package com.timevale.forward.service.impl;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Splitter;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ManDayReportCondition;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.ManDayReportMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.ManDayReportDO;
import com.timevale.forward.dal.entity.ManDayReportListDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ManDayReportService;
import com.timevale.forward.facade.api.query.ManDayReportQueryList;
import com.timevale.forward.facade.api.request.ManDayReportBatchApproveReq;
import com.timevale.forward.facade.api.request.ManDayReportModifyReq;
import com.timevale.forward.facade.api.request.ManDayReportUrgeReq;
import com.timevale.forward.facade.api.result.ManDayReportListVO;
import com.timevale.forward.model.enums.AuditStatusEnum;
import com.timevale.forward.model.enums.ManDayReportTabEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ManDayReportCopier;
import com.timevale.forward.service.observer.event.ManDayReportApproveMsgEvent;
import com.timevale.forward.service.observer.event.ManDayReportUrgeMsgEvent;
import com.timevale.forward.service.observer.event.TrackEventApprovalMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import net.sf.cglib.core.Local;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/08/19 16:27
 */
@Slf4j
@RestService
public class ManDayReportServiceImpl implements ManDayReportService {

    @Resource
    private ManDayMapper manDayMapper;
    @Resource
    private ManDayReportMapper manDayReportMapper;
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Override
    public BaseResult<PageQueryResult<ManDayReportListVO>> page(ManDayReportQueryList manDayReportQueryList) {
        String userId = LocalSessionUtils.getUserInfo().getId();

        ManDayReportCondition condition = ManDayReportCopier.INSTANCE.convert(manDayReportQueryList);

        String tabTag = manDayReportQueryList.getTabTag();
        if (ManDayReportTabEnum.AUDIT.toString().equals(tabTag)) {
            condition.setPmIds(Collections.singletonList(userId));
            condition.setAuditStatuses(Collections.singletonList(AuditStatusEnum.AUDITING.getCode()));
        } else if (ManDayReportTabEnum.REPORT.toString().equals(tabTag)) {
            condition.setCreateManIds(Collections.singletonList(userId));
        }

        Pair<Date, Date> dateDatePair = parseAndCheckDateRange(manDayReportQueryList.getWeekDateRange());
        condition.setWeekStartDate(dateDatePair.getLeft());
        condition.setWeekEndDate(dateDatePair.getRight());

        PageHelper.startPage(manDayReportQueryList.getPageNum(), manDayReportQueryList.getPageSize(),
                "mdr.audit_status desc, mdr.create_date desc, mdr.id desc");
        List<ManDayReportListDO> reportDOList = manDayReportMapper.selectCondition(condition);
        List<ManDayReportListVO> reportVOList = reportDOList.stream().map(ManDayReportCopier.INSTANCE::convert).collect(Collectors.toList());

        for (ManDayReportListVO e : reportVOList) {
            e.setAuditStatusText(AuditStatusEnum.getTextByCode(e.getAuditStatus()));
            e.setWeekDateRange(DateUtil.formDateRange(e.getWeekStartDate(), e.getWeekEndDate()));
        }

        PageInfo<ManDayReportListDO> pageInfo = new PageInfo<>(reportDOList);
        PageQueryResult<ManDayReportListVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(reportVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ManDayReportModifyReq manDayReportModifyReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        Long id = manDayReportModifyReq.getId();

        ManDayReportDO manDayReportDO = manDayReportMapper.selectById(id);
        AssertUtil.notNull(manDayReportDO, "待提报不存在");

        Long manDayId = manDayReportDO.getManDayId();
        ManDayDO manDayDO = manDayMapper.getById(manDayId);
        AssertUtil.notNull(manDayReportDO, "对应人天不存在");

        Long projectId = manDayDO.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(projectDO,"对应项目不存在");

        String pmId = projectDO.getPmId();
        String userId = userInfo.getId();
        AssertUtil.checkState(ObjectUtil.equal(pmId, userId), "您不是提报对应项目的项目经理，无权审批");

        Integer auditStatus = manDayReportModifyReq.getAuditStatus();
        AssertUtil.checkState(!AuditStatusEnum.AUDITING.getCode().equals(auditStatus), "审批状态只能变为审批通过和已驳回");

        ManDayReportDO updateReportDO = new ManDayReportDO();
        updateReportDO.setId(id);
        updateReportDO.setAuditStatus(auditStatus);

        // 判断审批状态
        if (AuditStatusEnum.APPROVE.getCode().equals(manDayReportModifyReq.getAuditStatus())) {
            // 如果更新人天为0，则逻辑删除
            BigDecimal auditManDay = manDayReportDO.getAuditManDay();
            if (BigDecimal.ZERO.compareTo(auditManDay) == 0) {
                manDayMapper.delete(manDayDO);
            } else {
                // 否则更新实际人天
                manDayDO.setActualManDay(auditManDay);
                manDayMapper.updateActualManDay(manDayDO);
                // 重置当前审核状态
                manDayDO.setRejectReason("");
                manDayDO.setAuditManDay(BigDecimal.ZERO);
                manDayDO.setAuditStatus(AuditStatusEnum.APPROVE.getCode());
                manDayMapper.updateAudit(manDayDO);
                // 更新提报状态
                manDayReportMapper.updateById(updateReportDO);
            }
            messageEventPublisher.publish(new ManDayReportApproveMsgEvent(
                    this,
                    userInfo.getAlias() + "-" + userInfo.getName(),
                    manDayDO.getMemberId(),
                    DateUtil.formDateRange(manDayDO.getWeekStartDate(), manDayDO.getWeekEndDate()),
                    projectDO.getName(),
                    manDayDO.getAuditManDay().toString()
            ));
        } else {
            String rejectReason = manDayReportModifyReq.getRejectReason();
            // 更新提报状态，拒绝原因
            updateReportDO.setRejectReason(rejectReason);
            manDayReportMapper.updateById(updateReportDO);
            // 更新当前审核状态
            manDayDO.setRejectReason(rejectReason);
            manDayDO.setAuditStatus(AuditStatusEnum.REJECT.getCode());
            manDayMapper.updateAudit(manDayDO);
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> batchApprove(ManDayReportBatchApproveReq manDayReportBatchApproveReq) {
        String userId = LocalSessionUtils.getUserInfo().getId();

        List<Long> ids = manDayReportBatchApproveReq.getIds();
        List<ManDayReportDO> manDayReportDOS = manDayReportMapper.selectByIds(ids);

        // 对应人天数据
        List<Long> manDayIds = manDayReportDOS.stream().map(ManDayReportDO::getManDayId).collect(Collectors.toList());
        List<ManDayDO> manDayDOS = manDayMapper.getByIds(manDayIds);

        // 对应项目数据
        List<Long> projectIds = manDayDOS.stream().map(ManDayDO::getProjectId).distinct().collect(Collectors.toList());
        List<ProjectDO> projectDOS = projectMapper.getByIds(projectIds);
        projectDOS = projectDOS.stream().filter(e->!e.getIsDeleted()).collect(Collectors.toList());

        // 判断对应项目是否全为自己pm
        AssertUtil.checkState(projectDOS.stream().allMatch(e -> userId.equals(e.getPmId())), "您无法审批您不是项目经理的项目");

        Map<Long, BigDecimal> auditDayMap = manDayReportDOS.stream()
                .collect(Collectors.toMap(ManDayReportDO::getManDayId, ManDayReportDO::getAuditManDay, (a, b) -> a));

        log.info("[ManDayReportServiceImpl][batchApprove]批量更新人天{}",auditDayMap);
        for (Long id : manDayIds) {
            ManDayDO manDayDO = new ManDayDO();
            manDayDO.setId(id);

            // 对应审核人天天数
            BigDecimal auditManDay = auditDayMap.get(id);

            // 如果比较为0，删除人天
            if (BigDecimal.ZERO.compareTo(auditManDay) == 0) {
                manDayMapper.delete(manDayDO);
            }

            // 更新人天实际天数
            manDayDO.setActualManDay(auditManDay);
            manDayMapper.updateActualManDay(manDayDO);

            // 更新人天当前审核状态
            manDayDO.setRejectReason("");
            manDayDO.setAuditManDay(BigDecimal.ZERO);
            manDayDO.setAuditStatus(AuditStatusEnum.APPROVE.getCode());
            manDayMapper.updateAudit(manDayDO);
        }
        manDayReportMapper.updateStatus(ids, AuditStatusEnum.APPROVE.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> urge(ManDayReportUrgeReq manDayReportUrgeReq) {
        Long id = manDayReportUrgeReq.getId();

        ManDayReportDO manDayReportDO = manDayReportMapper.selectById(id);
        AssertUtil.notNull(manDayReportDO, "该提报不存在");

        AssertUtil.checkState(!AuditStatusEnum.APPROVE.getCode().equals(manDayReportDO.getAuditStatus()),
                "该提报已经审核通过，无需催办");

        Long manDayId = manDayReportDO.getManDayId();
        ManDayDO manDayDO = manDayMapper.getById(manDayId);
        AssertUtil.notNull(manDayDO, "该提报对应的人天不存在");

        Long projectId = manDayDO.getProjectId();
        ProjectDO projectDO = projectMapper.get(projectId);
        AssertUtil.notNull(manDayDO, "该提报对应的项目不存在");

        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        log.info("[ManDayReportServiceImpl][urge]{}发送提报{}催办信息",userInfo.getId(),manDayReportDO.getId());
        messageEventPublisher.publish(new ManDayReportUrgeMsgEvent(
                this,
                userInfo.getAlias() + "-" + userInfo.getName(),
                projectDO.getPmId()
        ));

        return BaseResult.success(true);
    }

    private static Pair<Date, Date> parseAndCheckDateRange(String dateRange) {
        if (StrUtil.isEmpty(dateRange)) {
            return Pair.of(null,null);
        }
        Pair<LocalDate, LocalDate> localDatePair = parseDateRange(dateRange);
        LocalDate startLocalDate = localDatePair.getLeft();
        LocalDate endLocalDate = localDatePair.getRight();
        AssertUtil.checkState(startLocalDate.getDayOfWeek() == DayOfWeek.MONDAY,
                "传入时间开始时间必须为周一");
        AssertUtil.checkState(endLocalDate.getDayOfWeek() == DayOfWeek.SUNDAY,
                "传入时间开始时间必须为周日");
        AssertUtil.checkState(ChronoUnit.DAYS.between(startLocalDate, endLocalDate) == 6L,
                "结束时间和开始时间需要在同一周");
        Date startDate = Date.from(startLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(endLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        return Pair.of(startDate, endDate);
    }

    private static Pair<LocalDate, LocalDate> parseDateRange(String dateRange) {
        List<String> dates = Splitter.on(CommonConstant.TILDE).trimResults().splitToList(dateRange);
        AssertUtil.checkState(dates.size() == 2, "时间间隔格式错误");
        return Pair.of(LocalDate.parse(dates.get(0)), LocalDate.parse(dates.get(1)));
    }
}
