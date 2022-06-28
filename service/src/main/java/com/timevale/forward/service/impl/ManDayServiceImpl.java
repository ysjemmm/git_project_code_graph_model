package com.timevale.forward.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.*;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.ManDayMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.facade.api.client.ManDayService;
import com.timevale.forward.facade.api.query.ManDayQueryList;
import com.timevale.forward.facade.api.query.ProjectManDayQueryList;
import com.timevale.forward.facade.api.request.ManDayModifyReq;
import com.timevale.forward.facade.api.result.ManDayListVO;
import com.timevale.forward.facade.api.result.ManDayVO;
import com.timevale.forward.facade.api.result.ProjectManDayVO;
import com.timevale.forward.facade.api.result.ProjectTotalManDayVO;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ManDayCopier;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 人天相关接口
 *
 * @author jingchun
 * create on 2022/6/24
 */
@Slf4j
@RestService
public class ManDayServiceImpl implements ManDayService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BizChangeLogMapper bizChangeLogMapper;
    @Resource
    private PersonMapper personMapper;
    @Resource
    private ManDayMapper manDayMapper;

    @Override
    public BaseResult<List<ManDayListVO>> list(ManDayQueryList manDayQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        Long projectId = manDayQueryList.getProjectId();
        Set<Long> projectIds = new HashSet<>();
        Pair<Date, Date> dateRange = parseAndCheckDateRange(manDayQueryList.getWeekDateRange());
        Date startDate = dateRange.getLeft();
        Date endDate = dateRange.getRight();
        List<ManDayListVO> res = new ArrayList<>();
        List<ManDayDO> myManDays = manDayMapper.getByMemberIdAndDateRange(userInfo.getId(),
                startDate, endDate);
        Map<Long, ManDayDO> manDayByProjectId = Maps.uniqueIndex(myManDays, ManDayDO::getProjectId);
        if (projectId == null) {
            // 登陆人有人天录入的项目
            projectIds.addAll(myManDays.stream().map(ManDayDO::getProjectId).collect(Collectors.toSet()));
            // 登陆人参与的项目
            projectIds.addAll(personMapper.getMainIds(
                    Collections.singletonList(userInfo.getId()), null, PersonTypeEnum.PROJECT_MEMBER.getCode()));
        } else {
            projectIds.add(projectId);
            myManDays.removeIf(manDay -> !manDay.getProjectId().equals(projectId));
        }
        if (projectIds.isEmpty()) {
            return BaseResult.success(res);
        }
        List<ProjectDO> projects = projectMapper.getByIds(projectIds);
        List<BizChangeLogDO> changes = bizChangeLogMapper.listAllByActions(projectIds,
                BizChangeLogTypeEnum.PROJECT.getCode(),
                Lists.newArrayList(ButtonActionEnum.SUSPEND.getText(), ButtonActionEnum.INVALID.getText()));
        ListMultimap<Long, BizChangeLogDO> logsById = Multimaps.index(changes, BizChangeLogDO::getMainId);
        for (ProjectDO project : projects) {
            // 开始时间排除
            if (project.getActualStartDate() == null) {
                project.setActualStartDate(project.getPlanStartDate());
            }
            if (project.getActualStartDate().after(endDate)) {
                continue;
            }
            // 结束时间排除
            if (project.getActualEndDate() == null) {
                project.setActualEndDate(project.getPlanEndDate());
            }
            if (project.getStatus() < 0) {
                // 项目已暂停或者作废，则拿暂停、作废时间作为完成时间
                List<BizChangeLogDO> logs = logsById.get(project.getId());
                if (logs.isEmpty()) {
                    // 处于暂停作废状态却找不到记录时
                    continue;
                }
                // 最新一次暂停或者作废记录的时间
                logs.stream().map(BizChangeLogDO::getCreateDate)
                        .max(Date::compareTo).ifPresent(project::setActualEndDate);
            }
            if (project.getActualEndDate().before(startDate)) {
                continue;
            }
            ManDayListVO manDayListVO = new ManDayListVO()
                    .setProjectId(project.getId())
                    .setProjectName(project.getName())
                    .setProjectCreateDate(project.getCreateDate());
            // 组装数据
            if (project.getPmId().equals(userInfo.getId())) {
                // 项目经理
                manDayListVO.setPm(true);
                List<ManDayVO> resManDays = new ArrayList<>();
                List<ManDayDO> projectManDays =
                        manDayMapper.getByProjectIdAndStartDates(project.getId(), Collections.singleton(startDate),
                                null);
                Set<String> existsMemberIds = projectManDays.stream().map(ManDayDO::getMemberId)
                        .collect(Collectors.toSet());
                for (ManDayDO projectManDay : projectManDays) {
                    ManDayVO manDayVO = ManDayCopier.INSTANCE.convert(projectManDay);
                    resManDays.add(manDayVO);
                }
                List<PersonDO> persons = personMapper.get(Collections.singletonList(project.getId()),
                        PersonTypeEnum.PROJECT_MEMBER.getCode());
                persons.removeIf(person -> existsMemberIds.contains(person.getUserId()));
                for (PersonDO person : persons) {
                    resManDays.add(new ManDayVO()
                            .setMemberId(person.getUserId())
                            .setMemberName(person.getUserName())
                            .setProjectId(project.getId())
                            .setWeekStartDate(startDate)
                            .setWeekEndDate(endDate)
                            .setWeekDateRange(DateUtil.formDateRange(startDate, endDate)));
                }
                for (ManDayVO resManDay : resManDays) {
                    resManDay.setEditable(true);
                    if (resManDay.getMemberId().equals(userInfo.getId())) {
                        resManDay.setPm(true);
                    }
                }
                resManDays.sort((o1, o2) ->
                        Boolean.compare(o2.isPm(), o1.isPm()));
                manDayListVO.setManDays(resManDays);
            } else {
                // 非项目经理
                ManDayDO manday = manDayByProjectId.get(project.getId());
                if (manday != null) {
                    manDayListVO.setManDays(Collections.singletonList(ManDayCopier.INSTANCE.convert(manday)));
                } else {
                    manDayListVO.setManDays(Collections.singletonList(
                            new ManDayVO()
                                    .setMemberId(userInfo.getId())
                                    .setMemberName(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName())
                                    .setProjectId(project.getId())
                                    .setWeekStartDate(startDate)
                                    .setWeekEndDate(endDate)
                                    .setWeekDateRange(DateUtil.formDateRange(startDate, endDate))));
                }
            }
            res.add(manDayListVO);
        }
        res.sort(Comparator.comparing(ManDayListVO::isPm)
                .reversed()
                .thenComparing(ManDayListVO::getProjectCreateDate));

        return BaseResult.success(res);
    }

    @Override
    public BaseResult<ProjectTotalManDayVO> listProjectManDays(ProjectManDayQueryList projectManDayQueryList) {
        Long projectId = projectManDayQueryList.getProjectId();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "您查询的项目不存在，无法查询人天数据");
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        boolean pm = userInfo.getId().equals(project.getPmId());
        List<Date> startDates = null;
        if (projectManDayQueryList.getWeekDateRanges() != null) {
            startDates = projectManDayQueryList.getWeekDateRanges().stream()
                    .map(ManDayServiceImpl::parseAndCheckDateRange)
                    .map(Pair::getLeft).collect(Collectors.toList());
        }
        List<ManDayDO> manDays = manDayMapper.getByProjectIdAndStartDates(projectId,
                startDates, projectManDayQueryList.getUserIds());
        ProjectTotalManDayVO res = new ProjectTotalManDayVO();
        if (manDays.isEmpty()) {
            res.setProjectActualManDay(BigDecimal.ZERO);
            res.setProjectManDays(Collections.emptyList());
            return BaseResult.success(res);
        }
        res.setProjectActualManDay(manDayMapper.sumProjectActualDays(projectId));
        res.setProjectManDays(new ArrayList<>());

        List<ManDayVO> manDayVOList = ManDayCopier.INSTANCE.convert(manDays);
        for (ManDayVO manDayVO : manDayVOList) {
            if (pm) {
                manDayVO.setEditable(true);
            }
            if (project.getPmId().equals(manDayVO.getMemberId())) {
                manDayVO.setPm(true);
            }
        }
        ListMultimap<String, ManDayVO> manDayVOListByMemberId = Multimaps.index(manDayVOList, ManDayVO::getMemberId);
        for (String memberId : manDayVOListByMemberId.keySet()) {
            List<ManDayVO> memberManDays = manDayVOListByMemberId.get(memberId);
            memberManDays.sort(Comparator.comparing(ManDayVO::getWeekStartDate).reversed());
            ManDayVO firstManDay = memberManDays.get(0);
            ProjectManDayVO projectManDayVO = new ProjectManDayVO();
            projectManDayVO.setManDays(memberManDays);
            projectManDayVO.setTotalActualManDay(memberManDays.stream().map(ManDayVO::getActualManDay)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
            projectManDayVO.setMemberId(firstManDay.getMemberId());
            projectManDayVO.setMemberName(firstManDay.getMemberName());
            projectManDayVO.setPm(firstManDay.isPm());
            res.getProjectManDays().add(projectManDayVO);
        }

        res.getProjectManDays().sort(Comparator.comparing(ProjectManDayVO::isPm).reversed()
                .thenComparing(ProjectManDayVO::getMemberId));
        return BaseResult.success(res);
    }

    @Override
    public BaseResult<Boolean> modify(ManDayModifyReq manDayModifyReq) {
        Long projectId = manDayModifyReq.getProjectId();
        String memberId = manDayModifyReq.getMemberId();
        ProjectDO project = projectMapper.get(projectId);
        AssertUtil.notNull(project, "更改的项目id不存在");
        AssertUtil.checkState(project.getPmId().equals(LocalSessionUtils.getUserInfo().getId()),
                "您不是项目的项目经理，无权修改人天数据");
        BigDecimal actualManDay = manDayModifyReq.getActualManDay();
        Pair<Date, Date> dateRange = parseAndCheckDateRange(manDayModifyReq.getWeekDateRange());
        Date startDate = dateRange.getLeft();
        Date endDate = dateRange.getRight();
        List<ManDayDO> oldManDays = manDayMapper.getByProjectIdAndStartDates(project.getId(),
                Collections.singleton(startDate), null);
        Optional<ManDayDO> modifiedManDay = oldManDays.stream().filter(manDay -> manDay.getMemberId().equals(memberId))
                .findFirst();
        if (modifiedManDay.isPresent()) {
            // 原本已经存在的数据直接更新或者删除
            ManDayDO oldManDay = modifiedManDay.get();
            if (actualManDay == null || actualManDay.compareTo(BigDecimal.ZERO) == 0) {
                manDayMapper.delete(oldManDay);
            } else {
                oldManDay.setActualManDay(actualManDay);
                manDayMapper.updateActualManDay(oldManDay);
            }
            return BaseResult.success(true);
        }
        if (actualManDay == null || actualManDay.compareTo(BigDecimal.ZERO) == 0) {
            // 不存在且入参为空或者0不做处理
            return BaseResult.success(true);
        }
        Optional<PersonDO> member = personMapper.get(Collections.singletonList(project.getId()),
                        PersonTypeEnum.PROJECT_MEMBER.getCode()).stream()
                .filter(person -> person.getUserId().equals(memberId))
                .findFirst();
        // 校验项目时间
        setProjectActualStartAndEndDate(project);
        AssertUtil.checkState(!project.getActualEndDate().before(startDate) &&
                !project.getActualStartDate().after(endDate), "您提供的开始截至时间不在项目时间范围内，请修改");
        // 校验项目成员
        AssertUtil.checkState(member.isPresent(), "您提交的用户id不是该项目成员，请核对");
        // 原本不存在则新增
        manDayMapper.insert(new ManDayDO()
                .setProjectId(projectId)
                .setMemberId(memberId)
                .setMemberName(member.get().getUserName())
                .setActualManDay(actualManDay)
                .setWeekStartDate(startDate)
                .setWeekEndDate(endDate));
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Set<String>> queryManDayDateRanges(Long projectId) {
        Set<String> res = new HashSet<>();
        List<ManDayDO> manDays = manDayMapper.getByProjectId(projectId);
        for (ManDayDO manDay : manDays) {
            res.add(DateUtil.formDateRange(manDay.getWeekStartDate(), manDay.getWeekEndDate()));
        }
        return BaseResult.success(res);
    }

    private static Pair<Date, Date> parseAndCheckDateRange(String dateRange) {
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

    private void setProjectActualStartAndEndDate(ProjectDO project) {
        if (project.getActualStartDate() == null) {
            project.setActualStartDate(project.getPlanStartDate());
        }
        if (project.getActualEndDate() == null) {
            project.setActualEndDate(project.getPlanEndDate());
        }
        if (project.getStatus() < 0) {
            // 项目已暂停或者作废，则拿暂停、作废时间作为完成时间
            List<BizChangeLogDO> logs = bizChangeLogMapper.listAllByActions(Collections.singletonList(project.getId()),
                    BizChangeLogTypeEnum.PROJECT.getCode(),
                    Lists.newArrayList(ButtonActionEnum.SUSPEND.getText(), ButtonActionEnum.INVALID.getText()));
            // 最新一次暂停或者作废记录的时间
            logs.stream().map(BizChangeLogDO::getCreateDate)
                    .max(Date::compareTo).ifPresent(project::setActualEndDate);
        }
    }

}
