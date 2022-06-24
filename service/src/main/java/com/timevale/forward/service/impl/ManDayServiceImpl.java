package com.timevale.forward.service.impl;

import com.google.common.base.Splitter;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimaps;
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
import com.timevale.forward.facade.api.request.ManDayModifyReq;
import com.timevale.forward.facade.api.result.ManDayListVO;
import com.timevale.forward.facade.api.result.ManDayVO;
import com.timevale.forward.facade.api.result.ProjectManDayVO;
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
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
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
            if (project.getActualStartDate().compareTo(endDate) > 0) {
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
            if (project.getActualEndDate().compareTo(startDate) < 0) {
                continue;
            }
            ManDayListVO manDayListVO = new ManDayListVO()
                    .setProjectId(project.getId())
                    .setProjectName(project.getName());
            // 组装数据
            if (project.getPmId().equals(userInfo.getId())) {
                // 项目经理
                List<ManDayVO> resManDays = new ArrayList<>();
                List<ManDayDO> projectManDays =
                        manDayMapper.getByProjectIdAndDateRange(project.getId(), startDate, endDate);
                Set<String> existsMemberIds = projectManDays.stream().map(ManDayDO::getMemberId).collect(Collectors.toSet());
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

        return BaseResult.success(res);
    }

    @Override
    public BaseResult<List<ProjectManDayVO>> listProjectManDays(Long projectId) {
        return null;
    }

    @Override
    public BaseResult<Boolean> modify(ManDayModifyReq manDayModifyReq) {
        return null;
    }

    @Override
    public BaseResult<List<String>> queryManDayDateRanges(Long projectId) {
        List<String> res = new ArrayList<>();
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
