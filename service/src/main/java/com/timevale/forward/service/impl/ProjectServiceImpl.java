package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProjectStageEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProjectNodeComponent;
import com.timevale.forward.service.component.ProjectProductLineComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.ProjectNodeCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProjectServiceImpl implements ProjectService {

    @Resource
    private PersonComponent personComponent;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ProjectNodeComponent projectNodeComponent;

    @Resource
    private ProjectProductLineComponent projectProductLineComponent;

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProjectVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProjectVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long projectId, Byte type) {
        log.info("项目暂停或作废接收参数:projectId={},type={}", projectId, type);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> enable(Long projectId) {
        log.info("项目开启接收参数:projectId={}", projectId);
        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectAddReq projectAddReq) {
        log.info("项目新增接收参数:{}", projectAddReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectAddReq);
        projectDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setCreateManId(userInfo.getId());
        projectDO.setPmName(projectAddReq.getPm().getUserName());
        projectDO.setPmId(projectAddReq.getPm().getUserId());
        fillInfo(projectAddReq,projectDO);
        projectMapper.insert(projectDO);

        // 产品线
        projectProductLineComponent.add(projectDO.getProductLineIds(), projectDO.getId());

        // 产品经理
        if (CollectionUtils.isNotEmpty(projectAddReq.getPds())) {
            personComponent.add(projectAddReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());
        }
        // 团队成员
        if (CollectionUtils.isNotEmpty(projectAddReq.getTeamMembers())) {
            personComponent.add(projectAddReq.getTeamMembers(), projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());
        }
        // 节点信息
        if (CollectionUtils.isNotEmpty(projectAddReq.getProjectNodes())) {
            projectNodeComponent.add(projectAddReq.getProjectNodes(), projectDO.getId());
        }
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq) {
        log.info("项目修改接收参数:{}", projectModifyReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectModifyReq);
        projectDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setModifyManId(userInfo.getId());
        projectDO.setPmName(projectModifyReq.getPm().getUserName());
        projectDO.setPmId(projectModifyReq.getPm().getUserId());
        projectMapper.update(projectDO);

        // 产品线
        projectProductLineComponent.update(projectDO.getProductLineIds(), projectDO.getId());

        // 产品经理
        if (CollectionUtils.isNotEmpty(projectModifyReq.getPds())) {
            personComponent.update(projectModifyReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());
        }
        // 团队成员
        if (CollectionUtils.isNotEmpty(projectModifyReq.getTeamMembers())) {
            personComponent.update(projectModifyReq.getTeamMembers(), projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());
        }
        // 节点信息
        if (CollectionUtils.isNotEmpty(projectModifyReq.getProjectNodes())) {
            projectNodeComponent.add(projectModifyReq.getProjectNodes(), projectDO.getId());
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectDetailVO> get(Long projectId) {
        log.info("项目查看接收参数:projectId={}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        ProjectDetailVO projectDetailVO = ProjectCopier.INSTANCE.convert(projectDO);
        //产品线
        List<Long> productLineIds = projectProductLineComponent.get(projectId);
        projectDetailVO.setProductLineId(productLineIds);

        // 产品经理
        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        projectDetailVO.setPd(PersonCopier.INSTANCE.transform(pds));
        
        // 团队成员
        List<PersonDO> team = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        projectDetailVO.setTeamMember(PersonCopier.INSTANCE.transform(team));
        
        //节点
        List<ProjectNodeDO> projectNodeDO = projectNodeComponent.get(projectId);
        projectDetailVO.setProjectNodes(ProjectNodeCopier.INSTANCE.transform(projectNodeDO));

        //产品需求
        return BaseResult.success(projectDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(Long projectId) {
        log.info("产品需求匹配接收参数:projectId={}", projectId);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProductDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProductDemandVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> linkOrUnLinkProductDemand(Long projectId, List<Long> productDemandIds, Byte type) {
        log.info("关联or取消关联接收参数:projectId={},productDemandId={},type={}", projectId, productDemandIds, type);
        return BaseResult.success(true);
    }

    private void fillInfo(ProjectAddReq req, ProjectDO projectDO) {
        List<ProjectNodeAddReq> projectNodes = req.getProjectNodes();
        for (ProjectNodeAddReq node : projectNodes) {
            if (ProjectStageEnum.TEST_RELEASE.getText().equals(node.getName()) && node.getActualDate() != null) {
                projectDO.setStatus(ProjectStatusEnum.RELEASED.getCode());
                projectDO.setActualEndDate(node.getActualDate());
            }
            if (ProjectStageEnum.TEST_START.getText().equals(node.getName()) && node.getActualDate() != null) {
                projectDO.setStatus(ProjectStatusEnum.TESTING.getCode());
            }
            boolean dev = (ProjectStageEnum.DEV_REVIEW.getText().equals(node.getName())
                    || ProjectStageEnum.DEV_START.getText().equals(node.getName()))
                    && node.getActualDate() != null;
            if (dev) {
                projectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
            }
            if (ProjectStageEnum.DEMAND_START.getText().equals(node.getName())) {
                if(node.getActualDate() != null){
                    projectDO.setStatus(ProjectStatusEnum.PLANING.getCode());
                    projectDO.setActualStartDate(node.getActualDate());
                }else{
                    projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
                }

            }
        }
    }
}
