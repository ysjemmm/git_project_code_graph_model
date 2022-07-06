package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectNodeRecordMapper;
import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import com.timevale.forward.facade.api.client.ProjectNodeRecordService;
import com.timevale.forward.facade.api.query.ProjectNodeRecordQuery;
import com.timevale.forward.facade.api.result.ProjectNodeRecordCompareVO;
import com.timevale.forward.facade.api.result.ProjectNodeRecordVO;
import com.timevale.forward.service.copy.ProjectNodeRecordCopier;
import com.timevale.forward.service.integration.http.ElapsedTimeClient;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectNodeRecordServiceImpl implements ProjectNodeRecordService {

    @Resource
    private ProjectNodeRecordMapper projectNodeRecordMapper;

    @Resource
    private ElapsedTimeClient elapsedTimeClient;

    @Override
    public BaseResult<List<ProjectNodeRecordVO>> list(Long projectId) {
        log.info("节点版本记录,参数:{}", projectId);
        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectId);
        Map<BigDecimal, List<ProjectNodeRecordDO>> map = list.stream().collect(Collectors.groupingBy(ProjectNodeRecordDO::getVersion));
        List<ProjectNodeRecordVO> projectNodeRecords=new ArrayList<>();
        map.forEach((k,v)->{
            ProjectNodeRecordVO projectNodeRecordVO = ProjectNodeRecordCopier.INSTANCE.convert(v.get(0));
            projectNodeRecords.add(projectNodeRecordVO);
        });
        projectNodeRecords.sort(Comparator.comparing(ProjectNodeRecordVO::getVersion).reversed());
        return BaseResult.success(projectNodeRecords);
    }

    @Override
    public BaseResult<List<ProjectNodeRecordCompareVO>> compare(ProjectNodeRecordQuery projectNodeRecordQuery) {
        log.info("节点版本比较,参数:{}", projectNodeRecordQuery);
        BigDecimal minVersion = projectNodeRecordQuery.getMinVersion();
        BigDecimal maxVersion = projectNodeRecordQuery.getMaxVersion();
        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectNodeRecordQuery.getProjectId());

        Map<String, ProjectNodeRecordDO> minMap = list.stream().filter(a -> minVersion.equals(a.getVersion()))
                .collect(Collectors.toMap(ProjectNodeRecordDO::getName, k -> k, (v1, v2) -> v2));

        Map<String, ProjectNodeRecordDO> maxMap = list.stream().filter(a -> maxVersion.equals(a.getVersion()))
                .collect(Collectors.toMap(ProjectNodeRecordDO::getName, k -> k, (v1, v2) -> v2));
        List<ProjectNodeRecordCompareVO> result = new ArrayList<>();
        minMap.forEach((k, v) -> {
            ProjectNodeRecordCompareVO vo = new ProjectNodeRecordCompareVO();
            if (v.getPlanDate() == null && (maxMap.get(k) == null || maxMap.get(k).getPlanDate() == null)) {
                return;
            }
            if (v.getPlanDate() != null && (maxMap.get(k) == null || maxMap.get(k).getPlanDate() == null)) {
                vo.setMinPlanDate(v.getPlanDate());
            }
            if (v.getPlanDate() == null && maxMap.get(k) != null && maxMap.get(k).getPlanDate() != null) {
                vo.setMaxPlanDate(maxMap.get(k).getPlanDate());
            }
            if (v.getPlanDate() != null && maxMap.get(k) != null && maxMap.get(k).getPlanDate() != null) {
                Date startDate = v.getPlanDate();
                Date endDate = maxMap.get(k).getPlanDate();
                Long seconds=0L;
                if (startDate.before(endDate)) {
                     seconds = elapsedTimeClient.getElapsedTime(DateUtil.getEndOfDay(startDate), DateUtil.getEndOfDay(endDate));
                }else if(startDate.after(endDate)){
                     seconds = -elapsedTimeClient.getElapsedTime(DateUtil.getEndOfDay(endDate), DateUtil.getEndOfDay(startDate));
                }
                BigDecimal elapsedTime = new BigDecimal(seconds.toString());
                elapsedTime = elapsedTime.divide(new BigDecimal(DateFormatConst.WORK_DAY / DateFormatConst.ONE_SECOND), 0, RoundingMode.UP);
                vo.setTimeDiff(elapsedTime);
                vo.setMinPlanDate(startDate);
                vo.setMaxPlanDate(endDate);
            }

            vo.setMinVersion(minVersion);
            vo.setMaxVersion(maxVersion);
            vo.setName(k);
            result.add(vo);
        });
        maxMap.forEach((k, v) -> {
            if (v.getPlanDate() != null && (minMap.get(k) == null)) {
                ProjectNodeRecordCompareVO vo = new ProjectNodeRecordCompareVO();
                vo.setMaxPlanDate(v.getPlanDate());
                vo.setMinVersion(minVersion);
                vo.setMaxVersion(maxVersion);
                vo.setName(k);
                result.add(vo);
            }
        });
        return BaseResult.success(result);
    }
}
