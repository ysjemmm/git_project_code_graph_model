package com.timevale.forward.service.component.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.model.enums.ProjectTypeEnum;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Component
@Slf4j
public class ProjectComponentImpl implements ProjectComponent {

    @Resource
    private ProjectMapper projectMapper;

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> page(ProjectListCondition condition) {
        int count = projectMapper.count(condition);
        PageQueryResult<ProjectVO> pageQueryResult = new PageQueryResult<>();
        if (count == 0) {
            log.info("没有查询到项目信息");
            return BaseResult.success(pageQueryResult);
        }
        condition.setOffset((condition.getPageNum() - 1) * condition.getPageSize());
        condition.setSize(condition.getPageSize());
        List<ProjectListDO> projectListDO = projectMapper.list(condition);
        log.info("查询到项目信息:{}", projectListDO);

        List<ProjectVO> result = ProjectCopier.INSTANCE.convert(projectListDO);
        result.forEach(a->{
            a.setTypeName(ProjectTypeEnum.getTextByCode(a.getType()));
            a.setStatusName(ProjectStatusEnum.getTextByCode(a.getStatus()));
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));
        });
        pageQueryResult.setCurrentPage(condition.getPageNum());
        pageQueryResult.setItemsPerPage(condition.getPageSize());
        pageQueryResult.setTotalItems(count);
        pageQueryResult.setResultList(result);
        pageQueryResult.setTotalPages(count % condition.getPageSize() == 0
                ? count / condition.getPageSize() : count / condition.getPageSize() + 1);
        return BaseResult.success(pageQueryResult);
    }
}
