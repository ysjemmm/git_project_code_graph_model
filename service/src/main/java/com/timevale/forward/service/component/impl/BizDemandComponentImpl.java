package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.BizDemandUpdateCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.SqlOrderComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.observer.event.BizDemandPlanReleaseDateMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import generator.domain.ProjectBizDemandDO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/04 10:29
 */
@Slf4j
@LogPoint
@Component
public class BizDemandComponentImpl implements BizDemandComponent {
    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private BizLabelMapper bizLabelMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private InnerGroupClient innerGroupClient;
    @Resource
    private BizLabelComponent bizLabelComponent;
    @Resource
    private SqlOrderComponent sqlOrderComponent;
    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private BizDemandLogComponent bizDemandLogComponent;
    @Resource
    private ProductBizDemandMapper productBizDemandMapper;
    @Resource
    private ProjectBizDemandMapper projectBizDemandMapper;

    @Override
    public void updateStatus(Long bdId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bdId);
        AssertUtil.notNull(bizDemandDO, "业务需求不存在");

        // 已驳回状态下不变化
        Integer oldBdStatus = bizDemandDO.getStatus();
        if (BizDemandStatusEnum.REJECT.getCode().equals(oldBdStatus)) {
            return;
        }

        // 判断状态是否发生变更
        Integer newBdStatus = getStatusByProduct(bdId);
        if (ObjectUtil.notEqual(oldBdStatus, newBdStatus)) {
            BizDemandDO updateDO = new BizDemandDO();
            updateDO.setId(bdId);
            updateDO.setStatus(newBdStatus);
            bizDemandMapper.update(updateDO);
        }
    }

    @Override
    public void updateStatusByProject(Long projectId) {
        if (projectId == null) {
            return;
        }
        ProjectDO project = projectMapper.get(projectId);
        if (project == null) {
            return;
        }
        List<ProjectBizDemandDO> pbdList = projectBizDemandMapper.selectByProjectId(projectId);
        if (pbdList.isEmpty()) {
            return;
        }
        BizDemandStatusEnum bdStatus = getBizDemandStatusByProjectStatus(project.getStatus());
        Set<Long> bizDemandIds = pbdList.stream().map(ProjectBizDemandDO::getBizDemandId)
                .collect(Collectors.toSet());
        List<BizDemandDO> bizDemands = bizDemandMapper.getByIds(bizDemandIds);
        if (bizDemands.isEmpty()) {
            return;
        }
        BizDemandDO demand = bizDemands.get(0);
        if (Objects.equals(demand.getStatus(), bdStatus.getCode())) {
            return;
        }
        bizDemandMapper.updateConditional(new BizDemandUpdateCondition().setIds(bizDemandIds)
                .setStatus(bdStatus.getCode()));
        bizDemandLogComponent.addLogsAsProjectStatusChange(demand.getStatus(), bdStatus.getCode(), bizDemandIds);
    }

    @Override
    public Map<Long, GroupResponse> getGroupListTreeMap(Collection<Long> queryDeptIds) {
        if (CollUtil.isEmpty(queryDeptIds)) {
            return new HashMap<>();
        }
        Map<Long, GroupResponse> deptMap = Maps.newHashMap();
        Set<Long> queryDeptIdSet = new HashSet<>(queryDeptIds);
        GroupResponse rootNode = innerGroupClient.getGroupListTree(true).get(0);
        for (GroupResponse childNode : rootNode.getChildNode()) {
            dfsGroupListTree(childNode, deptMap, queryDeptIdSet, StringUtils.EMPTY, false);
        }
        deptMap.put(Long.valueOf(rootNode.getGroupId()), rootNode);
        return deptMap;
    }

    public void dfsGroupListTree(GroupResponse node, Map<Long, GroupResponse> deptMap, Set<Long> queryDeptIdSet, String name, Boolean isInsert) {
        name = name + node.getGroupName();
        Long deptId = Long.valueOf(node.getGroupId());
        if (isInsert || queryDeptIdSet.contains(deptId)) {
            isInsert = true;
            node.setGroupName(name);
            deptMap.put(deptId, node);
        }
        // 如果为叶节点直接返回
        if (node.getChildNode() == null) {
            return;
        }

        name = name + CommonConstant.JOIN_LINE;
        for (GroupResponse childNode : node.getChildNode()) {
            dfsGroupListTree(childNode, deptMap, queryDeptIdSet, name, isInsert);
        }
    }

    @Override
    public String getDeptChainName(Long deptId) {
        if (deptId == null) {
            return StringUtils.EMPTY;
        }
        StringBuilder deptName = new StringBuilder();
        List<GroupResponse> groupChain = innerGroupClient.getGroupChain(deptId);
        groupChain.remove(groupChain.size() - 1);
        Collections.reverse(groupChain);
        groupChain.forEach(e -> deptName.append(e.getGroupName()).append(CommonConstant.JOIN_LINE));
        return deptName.deleteCharAt(deptName.length() - 1).toString();
    }

    @Override
    public Date getProjectEndDate(Long bizDemandId) {
        // 获取该业务需求所关联的产品需求
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBdId(bizDemandId);
        if (productBizDemandDOList.isEmpty()) {
            return null;
        }

        // 获取关联的产品需求相关的项目
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());
        List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
        if (projectDOList.isEmpty()) {
            return null;
        }

        //最小的产品需求状态小于列入项目中,无需计算发布时间
        Integer minStatus = productBizDemandDOList.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(0);
        if (minStatus < ProductDemandStatusEnum.INCLUDED.getCode()) {
            log.info("业务需求关联的产品需求{}:", productBizDemandDOList);
            return null;
        }

        Date result = null;
        for (ProjectDO projectDO : projectDOList) {
            Date projectEndDate = projectDO.getActualEndDate() == null ? projectDO.getPlanEndDate() : projectDO.getActualEndDate();
            if (result == null) {
                result = projectEndDate;
            } else {
                result = result.after(projectEndDate) ? result : projectEndDate;
            }
        }
        return result;
    }

    @Override
    public QueryResultVO<BizDemandVO> page(BizDemandListCondition condition) {
        Map<Long, GroupResponse> deptNodeMap = new HashMap<>();
        Set<Long> queryDeptIdSet = Sets.newHashSet(condition.getDeptIdList());
        if (queryDeptIdSet == null) {
            queryDeptIdSet = new HashSet<>();
        }

        // 如果查询条件有部门id，收集子部门id及所需部门的完整名
        if (CollUtil.isNotEmpty(queryDeptIdSet)) {
            deptNodeMap = getGroupListTreeMap(Lists.newArrayList(queryDeptIdSet));
            // 替换查询部门id条件
            condition.setDeptIdList(Lists.newArrayList(deptNodeMap.keySet()));
        }

        //是否打标
        List<BizLabelDO> bizLabelDOList;
        if (CollectionUtils.isNotEmpty(condition.getLabelIds())) {
            bizLabelDOList = bizLabelMapper.getByLabelIdInType(condition.getLabelIds(), BizTypeEnum.BIZ_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
            Boolean containLabel = condition.getContainLabel();
            if (containLabel) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return ResultUtil.queryResultEmpty();
                }
                condition.setContainIds(bizIds);
            } else {
                condition.setExclusiveIds(bizIds);
            }
        }

        // 产品线分析信息
        List<BizDemandListDO> allBizDemandListDOList = bizDemandMapper.selectList(condition);
        Map<Long, List<BizDemandListDO>> bizDemandListDOMap = allBizDemandListDOList.stream().collect(Collectors.groupingBy(BizDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        bizDemandListDOMap.forEach((k, v) -> {
            ProductLineAnalyseVO bizDemandProductLineVO = new ProductLineAnalyseVO();
            Optional<BizDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                bizDemandProductLineVO.setBizDomainId(e.getBizDomainId());
                bizDemandProductLineVO.setCount(v.size());
                bizDemandProductLineVO.setProductLineId(e.getProductLineId());
                bizDemandProductLineVO.setProductLineName(e.getProductLineName());
                analyseVOList.add(bizDemandProductLineVO);
            });
        });
        // 根据数量，逆序排序
        analyseVOList.sort((a, b) -> b.getCount().compareTo(a.getCount()));

        // 产品线排查
        List<Long> conditionSubProductLineIdList = condition.getSubProductLineIdList();
        if (CollUtil.isNotEmpty(conditionSubProductLineIdList)) {
            Set<Long> resultProductLineIdSet = analyseVOList.stream().map(ProductLineAnalyseVO::getProductLineId).collect(Collectors.toSet());
            List<Long> queryProductLineIdList = conditionSubProductLineIdList.stream().filter(resultProductLineIdSet::contains).collect(Collectors.toList());
            if (CollUtil.isEmpty(queryProductLineIdList)) {
                QueryResultVO<BizDemandVO> queryResultVO = new QueryResultVO<>();
                queryResultVO.setAnalyseVOList(analyseVOList);
                queryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return queryResultVO;
            } else {
                condition.setProductLineIdList(queryProductLineIdList);
            }
        }

        // 开始分页,查询并转换
        String collation = sqlOrderComponent.build(condition.getOrderFiled(), condition.getOrderCollation());
        PageHelper.startPage(condition.getPageNum(), condition.getPageSize(), collation);
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(condition);

        List<Long> bizDemandIds = bizDemandListDOList.stream().map(BizDemandListDO::getId).collect(Collectors.toList());
        List<Long> customerDevBizDemandIds = bizDemandListDOList.stream()
                .filter(BizDemandListDO::getCustomerDevDemand)
                .map(BizDemandListDO::getId).collect(Collectors.toList());
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);
        if (CollUtil.isEmpty(bizDemandVOList)) {
            return ResultUtil.queryResultEmpty();
        }

        //标签信息
        Map<Long, List<BizLabelSimpleVO>> labelMap = bizLabelComponent
                .getBizLabelMap(bizDemandIds, BizTypeEnum.BIZ_DEMAND.getCode());

        // 如果查询条件没有部门id，收集完整名
        if (CollUtil.isEmpty(queryDeptIdSet)) {
            queryDeptIdSet = bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toSet());
            deptNodeMap = getGroupListTreeMap(queryDeptIdSet);
        }

        // 信息填充
        for (BizDemandVO bizDemandVO : bizDemandVOList) {
            GroupResponse response = deptNodeMap.get(bizDemandVO.getDeptId());
            if (response == null) {
                log.info("没有找到部门,id为:{}", bizDemandVO.getDeptId());
            } else {
                bizDemandVO.setDeptName(response.getGroupName());
                bizDemandVO.setDeptDeleteFlag(response.getDeleteFlag());
            }

            List<BizLabelSimpleVO> list = labelMap.get(bizDemandVO.getId());
            if (CollUtil.isNotEmpty(list)) {
                bizDemandVO.setLabelNames(list);
            }
        }

        // 填充交付项目来源数据产研项目信息
        if (!customerDevBizDemandIds.isEmpty()) {
            List<ProjectBizDemandDO> pbdList = projectBizDemandMapper.selectByBizDemandIds(customerDevBizDemandIds);
            if (!pbdList.isEmpty()) {
                List<Long> projectIds = pbdList.stream().map(ProjectBizDemandDO::getProjectId).distinct()
                        .collect(Collectors.toList());
                List<ProjectDO> projects = projectMapper.getByIds(projectIds);
                Map<Long, ProjectDO> projectById = Maps.uniqueIndex(projects, ProjectDO::getId);
                Map<Long, BizDemandVO> voById = Maps.uniqueIndex(bizDemandVOList, BizDemandVO::getId);
                for (ProjectBizDemandDO pbd : pbdList) {
                    BizDemandVO vo = voById.get(pbd.getBizDemandId());
                    ProjectDO project = projectById.get(pbd.getProjectId());
                    if (vo != null && project != null) {
                        vo.setProjectId(project.getId());
                        vo.setProjectName(project.getName());
                        vo.setProjectCreateDate(project.getCreateDate());
                    }
                }
            }
        }

        // 分页数据
        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>(bizDemandListDOList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        QueryResultVO<BizDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setAnalyseVOList(analyseVOList);
        queryResultVO.setPageQueryResult(pageQueryResult);
        return queryResultVO;
    }

    @Override
    public Boolean transfer(Long id, String newReceiveMan, String newReceiveManId) {
        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.get(id);
        if (bizDemandDO == null) {
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 新旧接收人
        String oldReceiveMan = bizDemandDO.getReceiveMan();

        // 新旧接受人是否相同
        if (!Objects.equals(oldReceiveMan, newReceiveMan)) {
            // 数据变更
            BizDemandDO newBizDemandDO = new BizDemandDO();
            newBizDemandDO.setId(bizDemandDO.getId());
            newBizDemandDO.setReceiveMan(newReceiveMan);
            newBizDemandDO.setReceiveManId(newReceiveManId);
            bizDemandMapper.update(newBizDemandDO);

            // 日志记录
            bizDemandLogComponent.addLogWhenModifyData(
                    oldReceiveMan,
                    newReceiveMan,
                    bizDemandDO.getId(),
                    BizChangeLogFieldEnum.RECEIVE_MAN.getText(),
                    true);

            // 转交人通知
            new BizDemandToReceiveMsgEvent(
                    this,
                    bizDemandDO.getId(),
                    bizDemandDO.getSubmitMan(),
                    newReceiveManId,
                    bizDemandDO.getName()
            ).send();
        }

        return true;
    }

    @Override
    public void updateProjectEndDate(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();

        Date oldProjectEndDate = bizDemandDO.getProjectEndDate();
        Date newProjectEndDate = getProjectEndDate(bizDemandId);

        if (!Objects.equals(newProjectEndDate, oldProjectEndDate)) {
            if (newProjectEndDate != null && !bizDemandDO.getCustomerDevDemand()) {
                // 部分断开或关联业务需求 非客开需求才更新计划上线日期
                int month = DateUtil.getMonth(newProjectEndDate);
                bizDemandDO.setPlanReleaseDate(month - 1);
            }
            bizDemandDO.setProjectEndDate(newProjectEndDate);
            bizDemandMapper.fullUpdate(bizDemandDO);
            log.info("业务需求id:{},发布时间,更新前:{},更新后:{}", bizDemandId, oldProjectEndDate, newProjectEndDate);
            if (!Objects.equals(oldPlanReleaseDate, bizDemandDO.getPlanReleaseDate())) {
                List<ProductBizDemandDO> productBizDemandDos = productBizDemandMapper.getByBdId(bizDemandId);
                //link biz
                Integer minStatus = productBizDemandDos.stream().map(ProductBizDemandDO::getStatus).min(Comparator.comparingInt(o -> o)).orElse(null);
                Integer newStatus = getBizDemandStatus(minStatus);
                if (!BizDemandStatusEnum.statusNoNeedTodo(bizDemandDO.getStatus())) {
                    messageEventPublisher.publish(new BizDemandPlanReleaseDateMsgEvent(
                            this,
                            bizDemandDO.getId(),
                            bizDemandDO.getSubmitManId(),
                            bizDemandDO.getName(),
                            BizDemandStatusEnum.getTextByCode(newStatus),
                            PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate())
                    ));
                }

                bizDemandLogComponent.addLogWhenModifyData(
                        PlanReleaseDateEnum.getTextByCode(oldPlanReleaseDate),
                        PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate()),
                        bizDemandId,
                        BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                        false);
            }
        }
    }

    @Override
    public Integer getBizDemandStatus(Integer pdStatus) {
        log.info("产品需求状态 :{}", pdStatus);
        if (Objects.equals(ProductDemandStatusEnum.WAITING.getCode(), pdStatus)) {
            return BizDemandStatusEnum.PD_LINKED.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.SUSPEND.getCode(), pdStatus)) {
            return BizDemandStatusEnum.PD_SUSPEND.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.INCLUDED.getCode(), pdStatus)) {
            return BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.PROGRESS.getCode(), pdStatus)) {
            return BizDemandStatusEnum.PROJECTING.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.ONLINE.getCode(), pdStatus)) {
            return BizDemandStatusEnum.AVAILABLE.getCode();
        }

        return BizDemandStatusEnum.RECEIVED.getCode();
    }

    @Override
    public List<ProductDemandDO> getPdDO(Long bdId) {
        if (bdId == null) {
            return new ArrayList<>();
        }

        // 查询关联关系
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBdId(bdId);
        List<Long> pdIdList = productBizDemandDOList.stream()
                .map(ProductBizDemandDO::getProductDemandId)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(pdIdList)) {
            return new ArrayList<>();
        }

        return productDemandMapper.selectByIdList(pdIdList);
    }

    @Override
    public Integer getStatusByProduct(Long bdId) {
        // 查询关联的产品需求
        List<ProductDemandDO> pdDOList = getPdDO(bdId);

        // 过滤掉已作废的产品需求, 取出最小状态
        Integer minPdStatus = pdDOList.stream()
                .map(ProductDemandDO::getStatus)
                .filter(Objects::nonNull)
                .filter(e -> !ProductDemandStatusEnum.INVALID.getCode().equals(e))
                .min(Integer::compareTo)
                .orElse(null);

        // 根据产品需求状态判断业务需求状态
        int bdStatus;
        if (ProductDemandStatusEnum.INCLUDED.getCode().equals(minPdStatus)) {
            bdStatus = BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        } else if (ProductDemandStatusEnum.PROGRESS.getCode().equals(minPdStatus)) {
            bdStatus = BizDemandStatusEnum.PROJECTING.getCode();
        } else if (ProductDemandStatusEnum.ONLINE.getCode().equals(minPdStatus)) {
            bdStatus = BizDemandStatusEnum.AVAILABLE.getCode();
        } else if (ProductDemandStatusEnum.WAITING.getCode().equals(minPdStatus)
                || ProductDemandStatusEnum.SUSPEND.getCode().equals(minPdStatus)) {
            bdStatus = BizDemandStatusEnum.PD_LINKED.getCode();
        } else {
            bdStatus = BizDemandStatusEnum.RECEIVED.getCode();
        }
        return bdStatus;
    }

    @Override
    public BizDemandStatusEnum getBizDemandStatusByProjectStatus(Integer projectStatus) {
        ProjectStatusEnum status = ProjectStatusEnum.getByCode(projectStatus);
        if (status == ProjectStatusEnum.WAITING || status == ProjectStatusEnum.SUSPEND) {
            return BizDemandStatusEnum.INCLUDE_PROJECT;
        }
        if (status == ProjectStatusEnum.CONCLUSION ||
                status == ProjectStatusEnum.RELEASED) {
            return BizDemandStatusEnum.AVAILABLE;
        }
        return BizDemandStatusEnum.PROJECTING;
    }

}
