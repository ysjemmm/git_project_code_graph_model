package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectListDO;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.ProjectQueryList;
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectNodeAddReq;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectDetailVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.AscriptionEnum;
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
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
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


    @Override
    public BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(projectQueryList);
        if (CollectionUtils.isEmpty(projectQueryList.getTeamMembers())) {
            condition.setTeamMembers(new ArrayList<>());
        }
        if (AscriptionEnum.CURRENT_USER.name().equals(projectQueryList.getAscription())) {
            condition.getTeamMembers().add(currentUser);

        } else if (AscriptionEnum.TEAM.name().equals(projectQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            condition.getTeamMembers().addAll(allMyStaffWithSelf);
        }
        int count = projectMapper.count(condition);
        PageQueryResult<ProjectVO> pageQueryResult = new PageQueryResult<>();
        if (count == 0) {
            log.info("没有查询到项目信息");
            return BaseResult.success(pageQueryResult);
        }
        condition.setOffset((condition.getPageNum() - 1) * condition.getPageSize());
        condition.setSize(condition.getPageSize());
        List<ProjectListDO> projectListDO = projectMapper.list(condition);
        log.info("查询到项目信息:={}", projectListDO);
        
        Map<Long, List<ProjectListDO>> listMap = projectListDO.stream().collect(Collectors.groupingBy(ProjectListDO::getId));
        log.info("分组后项目信息:={}", listMap);
        
        List<ProjectVO> result = new ArrayList<>();
        listMap.forEach((k, v) -> {
            ProjectVO projectVO = ProjectCopier.INSTANCE.convert(v.get(0));
            String pdName = v.stream().map(ProjectListDO::getPdName).distinct().collect(Collectors.joining(","));
            String teamMember = v.stream().map(ProjectListDO::getTeamMember).distinct().collect(Collectors.joining(","));
            String productLineName = v.stream().map(ProjectListDO::getProductLineName).distinct().collect(Collectors.joining(","));
            String bizDomainName = v.stream().map(ProjectListDO::getBizDomainName).distinct().collect(Collectors.joining(","));
            projectVO.setPdName(pdName);
            projectVO.setTeamMember(teamMember);
            projectVO.setProductLineName(productLineName);
            projectVO.setBizDomainName(bizDomainName);
            result.add(projectVO);
        });
        pageQueryResult.setCurrentPage(condition.getPageNum());
        pageQueryResult.setItemsPerPage(condition.getPageSize());
        pageQueryResult.setTotalItems(count);
        pageQueryResult.setResultList(result);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long projectId, Byte type) {
        log.info("项目暂停或作废接收参数:projectId={},type={}", projectId, type);
        ProjectDO projectDO=new ProjectDO();
        projectDO.setId(projectId);
        projectMapper.update(projectDO);
//        if(ProjectStatusEnum.INVALID.getCode().equals(type)){
//
//        }

        projectDO.setStatus(type);
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
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectModifyReq);
        projectDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        projectDO.setModifyManId(userInfo.getId());
        projectDO.setPmName(projectModifyReq.getPm().getUserName());
        projectDO.setPmId(projectModifyReq.getPm().getUserId());
        fillInfo(projectModifyReq, projectDO);
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

    private void fillInfo(ProjectModifyReq req, ProjectDO projectDO) {
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
                if (node.getActualDate() != null) {
                    projectDO.setStatus(ProjectStatusEnum.PLANING.getCode());
                    projectDO.setActualStartDate(node.getActualDate());
                } else {
                    projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
                }

            }
        }
    }
}
