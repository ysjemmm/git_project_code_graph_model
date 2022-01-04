package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.ProjectLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProductDemandLinkReq;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private PersonMapper personMapper;

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(projectQueryList);
        List<Long> projectIds = new ArrayList<>();
        //1.查找我或我的团队所属项目id
        if (AscriptionEnum.CURRENT_USER.name().equals(projectQueryList.getAscription())) {
            projectIds = personMapper.getProjectIds(Lists.newArrayList(currentUser), null, PersonTypeEnum.PROJECT_MEMBER.getCode());


        } else if (AscriptionEnum.TEAM.name().equals(projectQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            projectIds = personMapper.getProjectIds(allMyStaffWithSelf, null, PersonTypeEnum.PROJECT_MEMBER.getCode());
        }

        return projectComponent.page(condition, projectIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long projectId, Integer type) {
        log.info("项目暂停或作废接收参数:projectId={},type={}", projectId, type);
        if (!ProjectStatusEnum.SUSPEND.getCode().equals(type)
                && !ProjectStatusEnum.INVALID.getCode().equals(type)) {
            throw new BaseBizRuntimeException("操作类型不是暂停或作废,请重试输入");
        }
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        if (!ProjectStatusEnum.WAITING.getCode().equals(projectDO.getStatus())
                && !ProjectStatusEnum.PLANING.getCode().equals(projectDO.getStatus())
                && !ProjectStatusEnum.DEVING.getCode().equals(projectDO.getStatus())
                && !ProjectStatusEnum.TESTING.getCode().equals(projectDO.getStatus())) {
            throw new BaseBizRuntimeException("項目状态不是待启动、规划中、研发中、测试中,不能修改状态");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        projectDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setModifyManId(userInfo.getId());
        projectDO.setStatus(type);
        projectMapper.update(projectDO);
        //所有关联的产品需求
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectId);
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());

        //修改产品需求状态
        if (CollectionUtils.isNotEmpty(existProductDemandIds)) {
            if (ProjectStatusEnum.SUSPEND.getCode().equals(type)) {
                //暂停  更新产品需求状态
                productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.INCLUDED.getCode());
            } else {
                // 作废解除关联
                ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
                productDemandDO.setProjectId(projectId);
                productDemandDO.setIsDeleted(true);
                projectProductDemandComponent.update(productDemandDO);
                //作废  更新产品需求状态
                productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.WAITING.getCode());
            }
            //更新业务需求状态
//            productDemandComponent.updateBizDemandStatusAsProductStatusChange(existProductDemandIds);

        }
        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long projectId) {
        log.info("项目开启接收参数:projectId={}", projectId);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        if (!ProjectStatusEnum.SUSPEND.getCode().equals(projectDO.getStatus())) {
            throw new BaseBizRuntimeException("项目状态不是暂停,不能开启");
        }
        projectDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setModifyManId(userInfo.getId());
        List<ProjectNodeDO> projectNode = projectNodeComponent.get(projectId);
        log.info("项目开启,节点信息:projectNode={}", projectNode);
        if (CollectionUtils.isEmpty(projectNode)) {
            projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
            projectMapper.update(projectDO);
            return BaseResult.success(true);
        }
        fillInfo(projectNode, projectDO);
        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectAddReq projectAddReq) {
        log.info("项目新增接收参数:{}", projectAddReq);
        ProjectDO project = projectMapper.getByName(projectAddReq.getName());
        if (project != null) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectAddReq);
        projectDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setCreateManId(userInfo.getId());
        projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
        projectDO.setPmName(projectAddReq.getPm().getUserName());
        projectDO.setPmId(projectAddReq.getPm().getUserId());
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
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq) {
        log.info("项目修改接收参数:{}", projectModifyReq);
        ProjectDO project = projectMapper.getByName(projectModifyReq.getName());
        if (project != null && !project.getId().equals(projectModifyReq.getId())) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectModifyReq);
        projectDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setModifyManId(userInfo.getId());
        projectDO.setPmName(projectModifyReq.getPm().getUserName());
        projectDO.setPmId(projectModifyReq.getPm().getUserId());
        List<ProjectNodeDO> projectNodeDO = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());
        fillInfo(projectNodeDO, projectDO);

        // 产品线
        projectProductLineComponent.update(projectDO.getProductLineIds(), projectDO.getId());

        // 产品经理
        personComponent.update(projectModifyReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());

        // 团队成员
        personComponent.update(projectModifyReq.getTeamMembers(), projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());

        // 节点信息
        if (CollectionUtils.isNotEmpty(projectModifyReq.getProjectNodes())) {
            projectNodeComponent.add(projectNodeDO, projectDO.getId());
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectDetailVO> get(Long projectId) {
        log.info("项目查看接收参数:projectId={}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        ProjectDetailVO projectDetailVO = ProjectCopier.INSTANCE.convert(projectDO);
        projectDetailVO.setStatusName(ProjectStatusEnum.getTextByCode(projectDetailVO.getStatus()));

        //产品线
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
        projectDetailVO.setProductLineVO(productLineVO);

        // 产品经理
        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        projectDetailVO.setPd(PersonCopier.INSTANCE.transform(pds));

        // 团队成员
        List<PersonDO> team = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        projectDetailVO.setTeamMember(PersonCopier.INSTANCE.transform(team));

        //节点
        List<ProjectNodeDO> projectNodeDO = projectNodeComponent.get(projectId);
        List<ProjectNodeVO> projectNodeVO = ProjectNodeCopier.INSTANCE.transform(projectNodeDO);
        projectDetailVO.setProjectNodes(projectNodeVO);
        Date currentDate = new Date();
        projectDetailVO.setCurrentDate(currentDate);

        return BaseResult.success(projectDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(ProjectLinkProductDemandQueryList productDemandQueryList) {
        log.info("项目-产品需求匹配,接收参数:productDemandQueryList={}", productDemandQueryList);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        condition.setStatus(Lists.newArrayList(ProductDemandStatusEnum.WAITING.getCode()
                , ProductDemandStatusEnum.INCLUDED.getCode()
                , ProductDemandStatusEnum.PROGRESS.getCode()
                , ProductDemandStatusEnum.ONLINE.getCode()));
        condition.setMatchProductDemand(true);
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkProductDemand(ProductDemandLinkReq productDemandLinkReq) {
        log.info("关联or取消关联接收参数:productDemandLinkReq={}", productDemandLinkReq);
        ProjectDO projectDO = projectMapper.get(productDemandLinkReq.getProjectId());
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        List<Long> productDemandIds = productDemandLinkReq.getProductDemandIds();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandLinkReq.getType())) {
            projectProductDemandComponent.batchInsert(projectDO.getId(), productDemandIds);

            //updateProjectBizDemandStatus(projectDO);
        } else {
            ProjectProductDemandDO projectProductDemandDO = new ProjectProductDemandDO();
            projectProductDemandDO.setIsDeleted(true);
            projectProductDemandDO.setProductDemandId(productDemandIds.get(0));
            projectProductDemandComponent.update(projectProductDemandDO);

            ProductDemandDO productDemandDO = new ProductDemandDO();
            productDemandDO.setId(productDemandIds.get(0));
            productDemandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
            productDemandComponent.update(productDemandDO);

            // 一个产品需求下的业务需求
            //productDemandComponent.updateBizDemandStatusAsProductStatusChange(productDemandIds);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(ProjectProductDemandQueryList productDemandQueryList) {
        //产品需求
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandMapper.projectProductList(productDemandQueryList.getProjectId());
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });

        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    private void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO) {
        Map<String, ProjectNodeDO> nodeMap = projectNodes
                .stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        ProjectNodeDO node = null;
        if ((node = nodeMap.get(ProjectStageEnum.TEST_RELEASE.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.RELEASED.getCode());
            projectDO.setActualEndDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.TEST_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.TESTING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_REVIEW.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.DEVING.getCode());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEMAND_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setStatus(ProjectStatusEnum.PLANING.getCode());
        } else {
            projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
        }
        //优先取需求阶段实际时间作为项目实际开始时间,若无,则取开发阶段第一个节点实际时间做为作为项目实际开始时间
        if ((node = nodeMap.get(ProjectStageEnum.DEMAND_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_REVIEW.getText())) != null && node.getActualDate() != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_START.getText())) != null && node.getActualDate() != null) {
            projectDO.setActualStartDate(node.getActualDate());
        }
        log.info("更新项目信息:nodeMap={},,projectDO={}", nodeMap, projectDO);
        projectMapper.update(projectDO);

        //updateProjectBizDemandStatus(projectDO);

    }

    private void updateProjectBizDemandStatus(ProjectDO projectDO) {
        List<ProjectProductDemandDO> exists = projectProductDemandMapper.getByProjectId(projectDO.getId());
        List<Long> existProductDemandIds = exists.stream().map(ProjectProductDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        if (ProjectStatusEnum.WAITING.getCode().equals(projectDO.getStatus())
                || ProjectStatusEnum.SUSPEND.getCode().equals(projectDO.getStatus())) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.INCLUDED.getCode());
        } else if (ProjectStatusEnum.PLANING.getCode().equals(projectDO.getStatus())
                || ProjectStatusEnum.DEVING.getCode().equals(projectDO.getStatus())
                || ProjectStatusEnum.TESTING.getCode().equals(projectDO.getStatus())) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.PROGRESS.getCode());
        } else if (ProjectStatusEnum.RELEASED.getCode().equals(projectDO.getStatus())) {
            productDemandMapper.updateByIds(existProductDemandIds, ProductDemandStatusEnum.ONLINE.getCode());
        }
//        productDemandComponent.updateBizDemandStatusAsProductStatusChange(existProductDemandIds);
    }
}
