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
import com.timevale.forward.facade.api.request.ProjectAddReq;
import com.timevale.forward.facade.api.request.ProjectModifyReq;
import com.timevale.forward.facade.api.request.ProjectProductDemandLinkReq;
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

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private TaskComponent taskComponent;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;

    @Resource
    private TaskProductDemandMapper taskProductDemandMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    protected BugLogMapper bugLogMapper;

    @Resource
    private BugOfflineComponent bugOfflineComponent;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Resource
    private ProductDemandLogComponent productDemandLogComponent;

    @Resource
    private ProjectFlowMapper projectFlowMapper;

    @Resource
    private ProjectPublishPlanComponent projectPublishPlanComponent;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> list(ProjectQueryList projectQueryList) {
        log.info("项目列表接收参数:{}", projectQueryList);
        String currentUser = LocalSessionUtils.getUserInfo().getId();
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(projectQueryList);
        condition.setPageNum(projectQueryList.getPageNum());
        condition.setPageSize(projectQueryList.getPageSize());
        List<Long> projectIds = new ArrayList<>();
        //1.查找我或我的团队所属项目id
        if (AscriptionEnum.CURRENT_USER.name().equals(projectQueryList.getAscription())) {
            projectIds = personMapper.getMainIds(Lists.newArrayList(currentUser), null, PersonTypeEnum.PROJECT_MEMBER.getCode());
            if (CollectionUtils.isEmpty(projectIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }

        } else if (AscriptionEnum.TEAM.name().equals(projectQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(currentUser, true);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            projectIds = personMapper.getMainIds(allMyStaffWithSelf, null, PersonTypeEnum.PROJECT_MEMBER.getCode());
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
        Integer oldStatus = projectDO.getStatus();
        if (ProjectStatusEnum.INVALID.getCode().equals(oldStatus) || ProjectStatusEnum.RELEASED.getCode().equals(oldStatus)) {
            throw new BaseBizRuntimeException("项目状态为已作废或已发布时,不能修改状态");
        }
        projectDO.setStatus(type);
        projectMapper.update(projectDO);
        //修改产品需求状态
        productDemandComponent.updateProductDemandStatus(projectId, type);
        if (ProjectStatusEnum.INVALID.getCode().equals(type)) {
            // 作废解除关联
            projectProductDemandComponent.update(projectId, null);
        }
        String action = ProjectStatusEnum.SUSPEND.getCode().equals(type) ? ButtonActionEnum.SUSPEND.getText() : ButtonActionEnum.INVALID.getText();
        projectLogComponent.addLogWhenStatusChange(oldStatus, type, projectId, action);
        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, type, false);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long projectId, Boolean enableTask) {
        log.info("项目开启接收参数:projectId={}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        if (!ProjectStatusEnum.SUSPEND.getCode().equals(projectDO.getStatus())) {
            throw new BaseBizRuntimeException("项目状态不是暂停,不能开启");
        }
        Integer oldStatus = projectDO.getStatus();
        List<ProjectNodeDO> projectNode = projectNodeComponent.get(projectId);
        log.info("项目开启,节点信息:projectNode={}", projectNode);
        if (CollectionUtils.isEmpty(projectNode)) {
            projectDO.setStatus(ProjectStatusEnum.WAITING.getCode());
            projectMapper.update(projectDO);
        } else {
            fillInfoWhenEnable(projectNode, projectDO);
        }
        // 更新任务状态
        taskComponent.updateStatusAsProjectStatusChange(projectId, projectDO.getStatus(), enableTask);

        projectLogComponent.addLogWhenStatusChange(oldStatus, projectDO.getStatus(), projectId, ButtonActionEnum.ENABLE.getText());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProjectAddReq projectAddReq) {
        log.info("项目新增接收参数:{}", projectAddReq);
        ProjectDO project = projectMapper.getByName(projectAddReq.getName());

        if (projectAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("项目名称中请勿包含空格");
        }
        if (project != null) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        ProjectDO projectDO = ProjectCopier.INSTANCE.convert(projectAddReq);
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
        //生成节点信息
        projectNodeComponent.buildDefaultNode(projectDO.getPlanStartDate(), projectDO.getPlanEndDate(), projectDO.getId());

        Integer status = ProjectStatusEnum.WAITING.getCode();
        projectLogComponent.addLogWhenStatusChange(status, status, projectDO.getId(), ButtonActionEnum.SUBMIT.getText());

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProjectModifyReq projectModifyReq) {
        log.info("项目修改接收参数:{}", projectModifyReq);
        ProjectDO oldProject = projectMapper.getByName(projectModifyReq.getName());
        if (oldProject != null && !oldProject.getId().equals(projectModifyReq.getId())) {
            throw new BaseBizRuntimeException("该项目名称已存在,请修改后重试");
        }
        if (oldProject == null) {
            oldProject = projectMapper.get(projectModifyReq.getId());
        }
        ProjectDO newProject = ProjectCopier.INSTANCE.convert(projectModifyReq);
        newProject.setPmName(projectModifyReq.getPm().getUserName());
        newProject.setPmId(projectModifyReq.getPm().getUserId());
        List<ProjectNodeDO> projectNodeDOList = ProjectNodeCopier.INSTANCE.convert(projectModifyReq.getProjectNodes());
        Integer status = projectMapper.get(projectModifyReq.getId()).getStatus();
        newProject.setStatus(status);

        fillInfoWhenModify(projectNodeDOList, newProject);

        taskComponent.containProductLineInTask(newProject.getId(), newProject.getProductLineIds());

        bugOfflineComponent.containProductLineInBugOffline(newProject.getId(), newProject.getProductLineIds());

        List<String> pdUserIds = projectModifyReq.getPds().stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
        // 团队成员
        List<PersonAddReq> teamMembers = projectModifyReq.getTeamMembers();
        //过滤掉重复选择的项目经理,产品经理
        teamMembers = teamMembers.stream().filter(a -> !a.getUserId().equals(newProject.getPmId()) && !pdUserIds.contains(a.getUserId()))
                .collect(Collectors.toList());
        teamMembers.addAll(projectModifyReq.getPds());
        if (!pdUserIds.contains(projectModifyReq.getPm().getUserId())) {
            //产品经理不包含项目经理时,将项目经理加入团队中
            teamMembers.add(projectModifyReq.getPm());
        }
        personComponent.update(teamMembers, newProject.getId(), PersonTypeEnum.PROJECT_MEMBER.getCode());

        // 节点信息
        if (CollectionUtils.isNotEmpty(projectNodeDOList)) {
            boolean match = projectNodeDOList.stream().anyMatch(e ->
                    ProjectNodeEnum.PUBLISH_OFFICIAL.getText().equals(e.getName()) && e.getActualDate() != null);
            if (match && !checkProductRelease(projectModifyReq.getId())) {
                throw new BaseBizRuntimeException("该项目还有bug未关闭，请关闭后再发布");
            }
            if (match && Integer.valueOf(1).equals(projectModifyReq.getIsPlatformPublish())) {
                if (!projectPublishPlanComponent.linkPublishPlan(projectModifyReq.getId())) {
                    throw new BaseBizRuntimeException("请关联发布计划");
                }
                if (projectPublishPlanComponent.anyMatchNotFinished(projectModifyReq.getId())) {
                    throw new BaseBizRuntimeException("您的发布计划还未结束，请前往发布平台处理");
                }
            }
            projectNodeComponent.add(projectNodeDOList, newProject.getId());
            // 更新节点状态
            projectComponent.updateNodeStatus(projectModifyReq.getId());
        }
        // log
        projectLogComponent.addLogWhenModifyData(oldProject, newProject);
        // 产品线
        projectProductLineComponent.update(newProject.getProductLineIds(), newProject.getId());
        // 产品经理
        personComponent.update(projectModifyReq.getPds(), newProject.getId(), PersonTypeEnum.PROJECT_PD.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProjectDetailVO> get(Long projectId) {
        log.info("项目查看接收参数:projectId={}", projectId);
        ProjectDO projectDO = projectMapper.get(projectId);
        if (projectDO == null) {
            throw new BaseBizRuntimeException("该项目不存在");
        }
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

        // 节点状态
        projectDetailVO.setNodeStatusName(ProjectNodeStatusEnum.getNameByCode(projectDetailVO.getNodeStatus()));

        List<ProjectFlowDO> projectFlowDos = projectFlowMapper.getByProjectId(projectId);
        if (CollectionUtils.isNotEmpty(projectFlowDos)) {
            projectFlowDos.sort(Comparator.comparing(ProjectFlowDO::getCreateDate).reversed());
            ProjectFlowDO oldFlowDo = projectFlowDos.get(0);
            projectDetailVO.setProjectFlowId(oldFlowDo.getId());
        }
        return BaseResult.success(projectDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(ProjectLinkProductDemandQueryList productDemandQueryList) {
        log.info("项目-产品需求匹配,接收参数:productDemandQueryList={}", productDemandQueryList);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        // 过滤掉已经关联的产品需求
        List<Long> productDemandIds = projectProductDemandMapper.getLinkedProductDemand(Lists.newArrayList())
                .stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
        condition.setFilterProductDemandIds(productDemandIds);
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
    public BaseResult<ProductDemandStatusVO> linkOrUnLinkProductDemand(ProjectProductDemandLinkReq productDemandLinkReq) {
        log.info("关联or取消关联接收参数:productDemandLinkReq={}", productDemandLinkReq);
        ProjectDO projectDO = projectMapper.get(productDemandLinkReq.getProjectId());
        if (projectDO == null) {
            throw new BaseBizRuntimeException("找不到该项目");
        }
        List<Long> productDemandIds = productDemandLinkReq.getProductDemandIds();
        List<ProductDemandDO> productDemands = productDemandMapper.selectByIdList(productDemandIds);
        Map<Long, String> pdNameMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getName, (v1, v2) -> v2));
        Map<Long, Integer> statusMap = productDemands.stream().collect(Collectors.toMap(ProductDemandDO::getId, ProductDemandDO::getStatus, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(productDemandLinkReq.getType())) {
            List<ProjectProductDemandDO> productDemand = projectProductDemandMapper.getLinkedProductDemand(productDemandIds);
            if (CollectionUtils.isNotEmpty(productDemand)) {
                List<Long> existedIds = productDemand.stream().map(ProjectProductDemandDO::getProductDemandId).collect(Collectors.toList());
                throw new BaseBizRuntimeException("产品需求id为" + existedIds + "已被项目关联,请刷后重试");
            }
            projectProductDemandComponent.batchInsert(projectDO.getId(), productDemandIds);

            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            projectProductDemandComponent.update(null, productDemandIds.get(0));

            ProductDemandDO productDemandDO = new ProductDemandDO();
            productDemandDO.setId(productDemandIds.get(0));
            productDemandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
            productDemandComponent.update(productDemandDO);

            // 一个产品需求下的业务需求
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(productDemandIds, false);

            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.UN_LINK.getText());
            productDemandLogComponent.addLogAsProjectStatusChange(statusMap, productDemandDO.getStatus());

            // 取消产品需求和任务的关联
            productDemandIds.forEach(a -> taskProductDemandComponent.update(null, a));
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
        Long projectId = productDemandQueryList.getProjectId();
        int pageSize = productDemandQueryList.getPageSize();
        int pageNum = productDemandQueryList.getPageNum();
        log.info("项目-产品需求清单:pageNum={},pageSize={},projectId={}", pageNum,pageSize,projectId);
        // 查询产品需求
        List<ProductDemandListDO> productDemandListDO = productDemandMapper.linkProductDemandList(projectId);
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        if (CollectionUtils.isNotEmpty(productDemandVOList)) {
            //查询产品需求关联任务
            List<Long> productDemandIdList = productDemandVOList.stream().map(ProductDemandVO::getId).collect(Collectors.toList());
            List<TaskProductDemandDO> taskProductDemandDOList = taskProductDemandMapper.selectByProductDemandId(productDemandIdList);

            // 根据产品id分类
            Map<Long, List<TaskProductDemandDO>> taskProductDemandMap =
                    taskProductDemandDOList.stream().collect(Collectors.groupingBy(TaskProductDemandDO::getProductDemandId));

            // 填充任务数
            for (ProductDemandVO e : productDemandVOList) {
                int taskCount = taskProductDemandMap.containsKey(e.getId()) ? taskProductDemandMap.get(e.getId()).size() : 0;
                e.setTaskCount(taskCount);

                e.setProjectId(projectId);
                e.setStatusName(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
                e.setPriorityName(PriorityEnum.getTextByCode(e.getPriority()));
            }
        }

        if (Integer.valueOf(0).equals(productDemandQueryList.getType())) {
            productDemandVOList = productDemandVOList.stream().filter(a -> a.getTaskCount() == 0).collect(Collectors.toList());
        } else if (Integer.valueOf(1).equals(productDemandQueryList.getType())) {
            productDemandVOList = productDemandVOList.stream().filter(a -> a.getTaskCount() >= 1).collect(Collectors.toList());
        }

        int count = productDemandVOList.size();
        if (count > pageSize) {
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, count);
            productDemandVOList = productDemandVOList.subList(fromIndex, toIndex);
        }

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        pageQueryResult.setTotalItems(count);
        pageQueryResult.setTotalPages(count % pageSize == 0 ? count / pageSize : (count / pageSize) + 1);
        pageQueryResult.setCurrentPage(pageNum);
        pageQueryResult.setItemsPerPage(pageSize);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<ProjectBaseVO>> getProjectByProductLine(Long productLineId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<ProjectDO> projectDOList = projectMapper.selectByProductLine(productLineId, userInfo.getId());
        List<ProjectBaseVO> projectBaseVOList = projectDOList.stream().map(ProjectCopier.INSTANCE::convertTo).collect(Collectors.toList());

        return BaseResult.success(projectBaseVOList);
    }

    private boolean checkProductRelease(Long projectId) {
        List<BugOfflineDO> bugOfflineDOList = bugOfflineMapper.selectByProjectId(projectId);

        List<BugOfflineDO> releaseList = bugOfflineDOList.stream()
                .filter(e -> BugStatusEnum.COMPLETE.getCode().equals(e.getStatus())
                        || BugStatusEnum.CLOSE.getCode().equals(e.getStatus())
                        || BugStatusEnum.POSTPONE_REPAIR.getCode().equals(e.getStatus()))
                .collect(Collectors.toList());
        // 如果不仅为完成、关闭、延期修复，返回报错
        if (releaseList.size() != bugOfflineDOList.size()) {
            return false;
        }

        List<BugOfflineDO> postponeList = releaseList.stream()
                .filter(e -> BugStatusEnum.POSTPONE_REPAIR.getCode().equals(e.getStatus()))
                .collect(Collectors.toList());

        // 断开关联关系，并且记录bug日志
        if (!CollectionUtils.isEmpty(postponeList)) {
            ProjectDO projectDO = projectMapper.get(projectId);

            List<BugLogDO> bugLogDOList = Lists.newArrayList();
            postponeList.forEach(e -> {
                BugLogDO bugLogDO = new BugLogDO();
                bugLogDO.setField(BugFieldEnum.PROJECTS.getText());
                bugLogDO.setOldValue(projectDO.getName());
                bugLogDO.setNewValue(CommonConstant.NULL);
                bugLogDO.setMainId(e.getId());
                bugLogDO.setType(BugLogTypeEnum.OFFLINE.getCode());
                bugLogDOList.add(bugLogDO);
            });

            bugLogMapper.batchInsert(bugLogDOList);
            bugOfflineMapper.unlinkBugOffline(postponeList);
        }

        return true;
    }

    private void fillInfoWhenModify(List<ProjectNodeDO> projectNodes, ProjectDO newProject) {
        Map<String, ProjectNodeDO> nodeMap = projectNodes
                .stream()
                .collect(Collectors.toMap(ProjectNodeDO::getName, p -> p, (v1, v2) -> v2));
        // 检查任务
        boolean checkTask = nodeMap.get(ProjectNodeEnum.START_PLAN.getText()) == null
                && nodeMap.get(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText()) == null
                && nodeMap.get(ProjectNodeEnum.DEMAND_CONSTRUE.getText()) == null;
        if (checkTask) {
            //删除需求规划阶段时需要校验是否有关联任务,若有关联待执行&进行中&已完成&已暂停的任务,不能删除
            List<TaskDO> taskDOList = taskMapper.getByProjectId(newProject.getId())
                    .stream().filter(a -> TaskStageEnum.DEMAND.getCode().equals(a.getStage())
                            && !TaskStatusEnum.INVALID.getCode().equals(a.getStatus())).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(taskDOList)) {
                throw new BaseBizRuntimeException("需求规划阶段已关联任务，不可删除");
            }
        }
        // 计算项目状态
        ProjectNodeDO node = null;
        Integer oldStatus = newProject.getStatus();
        if ((node = nodeMap.get(ProjectNodeEnum.PUBLISH_OFFICIAL.getText())) != null && node.getActualDate() != null) {
            if (ProjectStatusEnum.SUSPEND.getCode().equals(oldStatus)) {
                // 编辑项目
                throw new BaseBizRuntimeException("项目状态为暂停时,不能填写发布正式的实际时间");
            }
            List<Date> nullDate = projectNodes.stream().map(ProjectNodeDO::getActualDate)
                    .filter(Objects::isNull).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(nullDate)) {
                throw new BaseBizRuntimeException("请填写完其他节点的实际时间后,再填写发布正式的实际时间");
            }
        }
        projectComponent.fillInfo(projectNodes, newProject);
        if (ProjectStatusEnum.SUSPEND.getCode().equals(oldStatus)) {
            // 编辑项目时，当状态是暂停,不修改项目状态
            newProject.setStatus(oldStatus);
        }
        ProjectDO oldProject = projectMapper.get(newProject.getId());
        projectMapper.update(newProject);
        if (!Objects.equals(newProject.getStatus(), oldStatus)) {
            //状态不一致时,更新产品需求状态
            productDemandComponent.updateProductDemandStatus(newProject.getId(), newProject.getStatus());
            projectLogComponent.addLogWhenStatusChange(oldStatus, newProject.getStatus(), newProject.getId(), ButtonActionEnum.MODIFY.getText());
        }
        if (!Objects.equals(oldProject.getPlanEndDate(), newProject.getPlanEndDate())
                || !Objects.equals(oldProject.getActualEndDate(), newProject.getActualEndDate())) {
            List<Long> bizDemandIds = projectComponent.getLinkBizDemandIds(oldProject.getId());
            bizDemandIds.forEach(a -> {
                bizDemandComponent.updateProjectEndDate(a, true);
            });
        }
        log.info("更新项目信息完成");
    }

    private void fillInfoWhenEnable(List<ProjectNodeDO> projectNodes, ProjectDO projectDO) {
        projectComponent.fillInfo(projectNodes, projectDO);
        projectMapper.update(projectDO);
        productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());
    }

}
