package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectNodeRecordMapper;
import com.timevale.forward.facade.api.client.ProjectNodeRecordService;
import com.timevale.forward.facade.api.query.ProjectNodeRecordQuery;
import com.timevale.forward.facade.api.result.ProjectNodeRecordCompareVO;
import com.timevale.forward.facade.api.result.ProjectNodeRecordVO;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;

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
//        List<ProjectNodeRecordDO> list = projectNodeRecordMapper.list(projectId);
//        List<ProjectNodeRecordVO> projectNodeRecordVOList = ProjectNodeRecordCopier.INSTANCE.convert(list);
        ProjectNodeRecordVO vo=new ProjectNodeRecordVO();
        return BaseResult.success(Lists.newArrayList(vo));
    }

    @Override
    public BaseResult<List<ProjectNodeRecordCompareVO>> list(ProjectNodeRecordQuery projectNodeRecordQuery) {
        ProjectNodeRecordCompareVO vo=new ProjectNodeRecordCompareVO();
        log.info("节点版本比较,参数:{}", projectNodeRecordQuery);
        return BaseResult.success(Lists.newArrayList(vo));
    }
}
