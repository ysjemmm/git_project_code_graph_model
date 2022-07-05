package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectNodeRecordMapper;
import com.timevale.forward.dal.entity.ProjectNodeRecordDO;
import com.timevale.forward.facade.api.client.ProjectNodeRecordService;
import com.timevale.forward.facade.api.query.ProjectNodeRecordQuery;
import com.timevale.forward.facade.api.result.ProjectNodeRecordCompareVO;
import com.timevale.forward.facade.api.result.ProjectNodeRecordVO;
import com.timevale.forward.service.copy.ProjectNodeRecordCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
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

    @Override
    public BaseResult<List<ProjectNodeRecordVO>> list(Long projectId) {
        log.info("节点版本记录,参数:{}", projectId);
        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectId);
        List<ProjectNodeRecordVO> projectNodeRecords = ProjectNodeRecordCopier.INSTANCE.convert(list);
        return BaseResult.success(projectNodeRecords);
    }

    @Override
    public BaseResult<List<ProjectNodeRecordCompareVO>> compare(ProjectNodeRecordQuery projectNodeRecordQuery) {
        log.info("节点版本比较,参数:{}", projectNodeRecordQuery);
        BigDecimal maxVersion = projectNodeRecordQuery.getMaxVersion();
        BigDecimal minVersion = projectNodeRecordQuery.getMinVersion();
        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectNodeRecordQuery.getProjectId());
        List<ProjectNodeRecordDO> min = list.stream().filter(a -> maxVersion.equals(a.getVersion())).collect(Collectors.toList());
//        List<ProjectNodeRecordDO> min = list.stream().filter(a -> minVersion.equals(a.getVersion())).collect(Collectors.toList());

        ProjectNodeRecordCompareVO vo=new ProjectNodeRecordCompareVO();
        return BaseResult.success(Lists.newArrayList(vo));
    }
}
