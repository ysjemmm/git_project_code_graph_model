package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.observer.event.BizDemandPlanReleaseDateMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
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
@Component
@Slf4j
@LogPoint
public class BizDemandComponentImpl implements BizDemandComponent {

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    InnerGroupClient innerGroupClient;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private BizDemandLogComponent bizDemandLogComponent;

    @Override
    public void updateBizDemandStatusByLinkedProductDemand(Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBizDemandId(bizDemandId);
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());

        List<ProductDemandDO> productDemandDOList = Lists.newArrayList();
        if (!productDemandIdList.isEmpty()) {
            productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);
        }

        // 筛出最小产品需求状态
        Integer status = null;
        for (ProductDemandDO productDemandDO : productDemandDOList) {
            Integer productDemandStatus = productDemandDO.getStatus();
            if (productDemandStatus.equals(ProductDemandStatusEnum.INVALID.getCode())) {
                continue;
            }
            status = status == null ? productDemandStatus : Math.min(status, productDemandStatus);
        }

        // 根据产品需求状态判断业务需求状态
        int newStatus;
        if (ProductDemandStatusEnum.INCLUDED.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        } else if (ProductDemandStatusEnum.PROGRESS.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.PROJECTING.getCode();
        } else if (ProductDemandStatusEnum.ONLINE.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.AVAILABLE.getCode();
        } else if (ProductDemandStatusEnum.WAITING.getCode().equals(status) || ProductDemandStatusEnum.SUSPEND.getCode().equals(status)) {
            newStatus = BizDemandStatusEnum.PD_LINKED.getCode();
        } else {
            newStatus = BizDemandStatusEnum.RECEIVED.getCode();
        }

        // 判断状态是否发生变更
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        Integer oldStatus = bizDemandDO.getStatus();
        if (!Objects.equals(oldStatus, newStatus) && !Objects.equals(BizDemandStatusEnum.REJECT.getCode(), oldStatus)) {
            // 状态更新
            BizDemandDO newBizDemandDO = new BizDemandDO();
            newBizDemandDO.setId(bizDemandId);
            newBizDemandDO.setStatus(newStatus);
            bizDemandMapper.update(newBizDemandDO);
        }
    }

    @Override
    public Map<Long, GroupResponse> getGroupListTreeMap(List<Long> queryDeptIdList) {
        Map<Long, GroupResponse> deptMap = Maps.newHashMap();
        Set<Long> queryDeptIdSet = Sets.newHashSet(queryDeptIdList);
        GroupResponse rootNode = innerGroupClient.getGroupListTree(true);
        for (GroupResponse childNode : rootNode.getChildNode()) {
            dfsGroupListTree(childNode, deptMap, queryDeptIdSet, StringUtils.EMPTY, false);
        }
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
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBizDemandId(bizDemandId);
        if (productBizDemandDOList.isEmpty()) {
            return null;
        }

        // 获取关联的产品需求相关的项目
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());
        List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
        if (projectDOList.isEmpty()) {
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
    public QueryResultVO<BizDemandVO> page(BizDemandListCondition bizDemandListCondition) {
        Map<Long, GroupResponse> deptNodeMap = new HashMap<>();
        Set<Long> queryDeptIdSet = Sets.newHashSet(bizDemandListCondition.getDeptIdList());

        // 如果查询条件有部门id，收集子部门id及所需部门的完整名
        if (!CollectionUtils.isEmpty(queryDeptIdSet)) {
            deptNodeMap = getGroupListTreeMap(Lists.newArrayList(queryDeptIdSet));
            // 替换查询部门id条件
            bizDemandListCondition.setDeptIdList(Lists.newArrayList(deptNodeMap.keySet()));
        }

        // 日期处理
        bizDemandListCondition.setCreateDateStart(DateUtil.getStartOfDay(bizDemandListCondition.getCreateDateStart()));
        bizDemandListCondition.setCreateDateEnd(DateUtil.getEndOfDay(bizDemandListCondition.getCreateDateEnd()));
        bizDemandListCondition.setProjectEndDateStart(DateUtil.getStartOfDay(bizDemandListCondition.getProjectEndDateStart()));
        bizDemandListCondition.setProjectEndDateEnd(DateUtil.getEndOfDay(bizDemandListCondition.getProjectEndDateEnd()));

        // 查询并转换
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(bizDemandListCondition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);

        // 如果查询条件没有部门id，收集完整名
        if (CollectionUtils.isEmpty(queryDeptIdSet)) {
            queryDeptIdSet.addAll(bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList()));
            deptNodeMap = getGroupListTreeMap(Lists.newArrayList(queryDeptIdSet));
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
            bizDemandVO.setStatusText(BizDemandStatusEnum.getTextByCode(bizDemandVO.getStatus()));
            bizDemandVO.setPriorityText(PriorityEnum.getTextChineseByCode(bizDemandVO.getPriority()));
            bizDemandVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(bizDemandVO.getPlanReleaseDate()));
        }

        bizDemandVOList.forEach(e -> {
            e.setStatusText(BizDemandStatusEnum.getTextByCode(e.getStatus()));
            e.setPriorityText(PriorityEnum.getTextChineseByCode(e.getPriority()));
            e.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(e.getPlanReleaseDate()));
        });

        // 分页数据
        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>(bizDemandListDOList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        // 产品线分析信息
        List<BizDemandListDO> allBizDemandListDOList = bizDemandMapper.selectList(bizDemandListCondition);
        Map<Long, List<BizDemandListDO>> bizDemandListDOMap = allBizDemandListDOList.stream().collect(Collectors.groupingBy(BizDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        bizDemandListDOMap.forEach((k,v) -> {
            ProductLineAnalyseVO bizDemandProductLineVO = new ProductLineAnalyseVO();
            Optional<BizDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                bizDemandProductLineVO.setCount(v.size());
                bizDemandProductLineVO.setProductLineId(e.getProductLineId());
                bizDemandProductLineVO.setProductLineName(e.getProductLineName());
                analyseVOList.add(bizDemandProductLineVO);
            });
        });

        // 逆序排序
        analyseVOList.sort((a,b) -> b.getCount().compareTo(a.getCount()));

        QueryResultVO<BizDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setAnalyseVOList(analyseVOList);
        queryResultVO.setPageQueryResult(pageQueryResult);
        return queryResultVO;
    }

    @Override
    public Boolean transfer(Long id, String newReceiveMan, String newReceiveManId) {
        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(id);
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
            messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
                    this,
                    bizDemandDO.getId(),
                    bizDemandDO.getSubmitMan(),
                    newReceiveManId,
                    bizDemandDO.getName()
            ));
        }

        return true;
    }

    @Override
    public void updateProjectEndDate(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        Integer oldPlanReleaseDate = bizDemandDO.getPlanReleaseDate();

        Date oldProjectEndDate = bizDemandDO.getProjectEndDate();
        Date newProjectEndDate = getProjectEndDate(bizDemandId);

        if (!Objects.equals(newProjectEndDate, oldProjectEndDate)) {
            if (newProjectEndDate != null) {
                //部分断开或关联业务需求
                int month = DateUtil.getMonth(newProjectEndDate);
                bizDemandDO.setPlanReleaseDate(month - 1);
            }
            bizDemandDO.setProjectEndDate(newProjectEndDate);
            bizDemandMapper.fullUpdate(bizDemandDO);
            log.info("业务需求id:{},更新前发布时间:{},更新后发布时间:{}", bizDemandId, oldProjectEndDate, newProjectEndDate);
            if (!Objects.equals(oldPlanReleaseDate, bizDemandDO.getPlanReleaseDate())) {
                List<ProductBizDemandDO> productBizDemandDos = productBizDemandMapper.getByBizDemandId(bizDemandId);
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
    public Integer getBizDemandStatus(Integer pdStauts) {
        if (Objects.equals(ProductDemandStatusEnum.WAITING.getCode(), pdStauts)
                || Objects.equals(ProductDemandStatusEnum.SUSPEND.getCode(), pdStauts)) {
            return BizDemandStatusEnum.PD_LINKED.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.INCLUDED.getCode(), pdStauts)) {
            return BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.PROGRESS.getCode(), pdStauts)) {
            return BizDemandStatusEnum.PROJECTING.getCode();
        }
        if (Objects.equals(ProductDemandStatusEnum.ONLINE.getCode(), pdStauts)) {
            return BizDemandStatusEnum.AVAILABLE.getCode();
        }
        log.info("产品需求状态 :{}", pdStauts);
        return BizDemandStatusEnum.RECEIVED.getCode();
    }
}
