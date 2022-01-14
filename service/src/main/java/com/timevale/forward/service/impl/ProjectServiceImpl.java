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
import com.timevale.forward.facade.api.request.PersonAddReq;
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
import java.util.*;
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
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

        } else if (AscriptionEnum.TEAM.name().equals(projectQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            projectIds = personMapper.getProjectIds(allMyStaffWithSelf, null, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
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
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(existProductDemandIds, false);

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
        fillInfo(projectNode, projectDO, true);
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
        personComponent.add(projectAddReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());
        List<String> pdUserIds = projectAddReq.getPds().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

        // 团队成员
        List<PersonAddReq> teamMembers = projectAddReq.getTeamMembers();
        //过滤掉重复选择的项目经理,产品经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(projectDO.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        teamMembers.addAll(projectAddReq.getPds());
        if (!pdUserIds.contains(projectAddReq.getPm().getUserId())) {
            //产品经理不包含项目经理时,将项目经理加入团队中
            teamMembers.add(projectAddReq.getPm());
        }
        personComponent.add(teamMembers, projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());
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
        Integer status = projectMapper.get(projectModifyReq.getId()).getStatus();
        projectDO.setStatus(status);
        fillInfo(projectNodeDO, projectDO, false);

        // 产品线
        projectProductLineComponent.update(projectDO.getProductLineIds(), projectDO.getId());

        // 产品经理
        personComponent.update(projectModifyReq.getPds(), projectDO.getId(), PersonTypeEnum.PROJECT_PD.getCode());
        List<String> pdUserIds = projectModifyReq.getPds().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());

        // 团队成员
        List<PersonAddReq> teamMembers = projectModifyReq.getTeamMembers();
        //过滤掉重复选择的项目经理,产品经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(projectDO.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        teamMembers.addAll(projectModifyReq.getPds());
        if (!pdUserIds.contains(projectModifyReq.getPm().getUserId())) {
            //产品经理不包含项目经理时,将项目经理加入团队中
            teamMembers.add(projectModifyReq.getPm());
        }
        personComponent.update(teamMembers, projectDO.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());

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
        projectDetailVO.setPriorityName(PriorityEnum.getTextByCode(projectDetailVO.getPriority()));
        projectDetailVO.setTypeName(ProjectTypeEnum.getTextByCode(projectDetailVO.getType()));

        //产品线
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVO = ProductLineCopier.INSTANCE.convert(productLineDO);
        projectDetailVO.setProductLineVO(productLineVO);

        // 产品经理
        List<PersonDO> pds = personComponent.select(projectId, PersonTypeEnum.PROJECT_PD.getCode());
        projectDetailVO.setPd(PersonCopier.INSTANCE.transform(pds));
        List<String> pdUserIds = pds.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        // 团队成员
        List<PersonDO> teamMembers = personComponent.select(projectId, PersonTypeEnum.PROJECT_MEMBER.getCode());
        //过滤掉产品经理和项目经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(projectDO.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        projectDetailVO.setTeamMember(PersonCopier.INSTANCE.transform(teamMembers));

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
        // 过滤掉已经关联的产品需求
        List<Long> productDemandIds = projectProductDemandMapper.getLinkedProductDemand(Lists.newArrayList())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        condition.setProductDemandIds(productDemandIds);
        condition.setStatus(Lists.newArrayList(ProductDemandStatusEnum.WAITING.getCode()
                , ProductDemandStatusEnum.INCLUDED.getCode()
                , ProductDemandStatusEnum.PROGRESS.getCode()
                , ProductDemandStatusEnum.ONLINE.getCode()));
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
    public BaseResult<ProductDemandStatusVO> linkOrUnLinkProductDemand(ProductDemandLinkReq productDemandLinkReq) {
        log.info("关联or取消关联接收参数:productDemandLinkReq={}", productDemandLinkReq);
        ProjectDO projectDO = projectMapper.get(productDemandLinkReq.getProjectId());
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        List<Long> productDemandIds = productDemandLinkReq.getProductDemandIds();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandLinkReq.getType())) {
            List<ProjectProductDemandDO> productDemand = projectProductDemandMapper.getLinkedProductDemand(productDemandIds);
            if (CollectionUtils.isNotEmpty(productDemand)) {
                List<Long> existedIds = productDemand.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
                throw new BaseBizRuntimeException("产品需求id为" + existedIds + "已被项目关联,请刷后重试");
            }
            projectProductDemandComponent.batchInsert(projectDO.getId(), productDemandIds);

            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());
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
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(productDemandIds, false);
        }
        //产品需求和项目关联或删除时,需要给前端刷新产品需求状态
        ProductDemandDO productDemandDO = productDemandMapper.selectById(productDemandIds.get(0));
        ProductDemandStatusVO vo = new ProductDemandStatusVO();
        vo.setStatus(productDemandDO.getStatus());
        vo.setStatusText(ProductDemandStatusEnum.getTextByCode(productDemandDO.getStatus()));
        return BaseResult.success(vo);
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

    private void fillInfo(List<ProjectNodeDO> projectNodes, ProjectDO projectDO, boolean enable) {
        Map<String, ProjectNodeDO> nodeMap = projectNodes
                .stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        ProjectNodeDO node = null;
        Integer oriStatus = projectDO.getStatus();
        if ((node = nodeMap.get(ProjectStageEnum.TEST_RELEASE.getText())) != null && node.getActualDate() != null) {
            if (!enable && ProjectStatusEnum.SUSPEND.getCode().equals(oriStatus)) {
                // 编辑项目时，当状态是暂停,不修改项目状态
                throw new BaseBizRuntimeException("项目状态为暂停时,不能填写发布正式的实际时间");
            }
            List<Date> nullDate = projectNodes.stream().map(ProjectNodeDO::getActualDate)
                    .filter(Objects::isNull).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(nullDate)) {
                throw new BaseBizRuntimeException("请填写完其他节点的实际时间后,再填写发布正式的实际时间");
            }
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
        if ((node = nodeMap.get(ProjectStageEnum.DEMAND_START.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_REVIEW.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        } else if ((node = nodeMap.get(ProjectStageEnum.DEV_START.getText())) != null) {
            projectDO.setActualStartDate(node.getActualDate());
        }
        if (!enable && ProjectStatusEnum.SUSPEND.getCode().equals(oriStatus)) {
            // 编辑项目时，当状态是暂停,不修改项目状态
            projectDO.setStatus(oriStatus);
        }
        log.info("更新项目信息:nodeMap={},,projectDO={},enable={}", nodeMap, projectDO, enable);
        projectMapper.update(projectDO);

        if (enable || !ProjectStatusEnum.SUSPEND.getCode().equals(oriStatus)) {
            // 启用项目时或当状态不是暂停,更新产品需求状态
            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());
        }
    }

}
