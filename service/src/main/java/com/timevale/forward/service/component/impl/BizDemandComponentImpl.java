package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.result.BizDemandStatusVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.PlanReleaseDateEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.MessageComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.StringUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2022/01/04 10:29
 */
@Component
@Slf4j
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
    MessageComponent messageComponent;

    @Resource
    InnerGroupClient innerGroupClient;

    @Override
    public BizDemandStatusVO updateBizDemandStatusAsLinkProductDemand(Long bizDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBizDemandId(bizDemandId);
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());

        List<ProductDemandDO> productDemandDOList = Lists.newArrayList();
        if(!productDemandIdList.isEmpty()){
            productDemandDOList = productDemandMapper.selectByIdList(productDemandIdList);
        }

        // 筛出最小产品需求状态
        Integer status = null;
        for (ProductDemandDO productDemandDO : productDemandDOList) {
            Integer productDemandStatus = productDemandDO.getStatus();
            if(productDemandStatus.equals(ProductDemandStatusEnum.INVALID.getCode())){continue;}
            status = status == null ? productDemandStatus : Math.min(status, productDemandStatus);
        }

        // 根据产品需求状态判断业务需求状态
        int result;
        boolean notice = true;
        if(ProductDemandStatusEnum.INCLUDED.getCode().equals(status)){
            result = BizDemandStatusEnum.INCLUDE_PROJECT.getCode();
        }else if(ProductDemandStatusEnum.PROGRESS.getCode().equals(status)){
            result = BizDemandStatusEnum.PROJECTING.getCode();
        }else if(ProductDemandStatusEnum.ONLINE.getCode().equals(status)){
            result = BizDemandStatusEnum.AVAILABLE.getCode();
        }else{
            notice = false;
            result = BizDemandStatusEnum.RECEIVED.getCode();
        }

        // 获取项目发布时间
        Date date = null;
        if(!productDemandIdList.isEmpty()){
            List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
            log.info("产品需求id={},关联项目={}",productDemandIdList,projectDOList);
            for (ProjectDO projectDO : projectDOList) {
                Date projectEndDate = projectDO.getActualEndDate() == null? projectDO.getPlanEndDate(): projectDO.getActualEndDate();
                if(date == null){
                    date = projectEndDate;
                }else{
                    date = date.after(projectEndDate)? date: projectEndDate;
                }
            }
        }

        // 判断状态是否发生变更
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(!bizDemandDO.getStatus().equals(result)){
            // 状态更新
            bizDemandDO.setStatus(result);
            bizDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getId());
            bizDemandDO.setModifyManId(userInfo.getId());
            bizDemandMapper.update(bizDemandDO);

            if(notice){
                // 钉钉通知
                messageComponent.bizDemandStatusChangeMsg(bizDemandDO.getId(),
                        bizDemandDO.getCreateManId(),
                        bizDemandDO.getName(),
                        BizDemandStatusEnum.getTextByCode(bizDemandDO.getStatus()),
                        date);
            }
        }

        BizDemandStatusVO bizDemandStatusVO = new BizDemandStatusVO();
        bizDemandStatusVO.setEndDate(date);
        bizDemandStatusVO.setStatus(result);
        bizDemandStatusVO.setStatusText(BizDemandStatusEnum.getTextByCode(result));
        return bizDemandStatusVO;
    }

    @Override
    public void dfsGroupListTree(GroupResponse node, Map<Long, String> deptMap, Set<Long> queryDeptIdSet, String name, Boolean isInsert){
        name = name + node.getGroupName();
        Long deptId = Long.valueOf(node.getGroupId());
        if(isInsert || queryDeptIdSet.contains(deptId)){
            isInsert = true;
            deptMap.put(deptId, name);
        }
        // 如果为叶节点直接返回
        if(node.getChildNode() == null){return;}

        name = name + CommonConstant.JOIN_LINE;
        for (GroupResponse childNode : node.getChildNode()) {
            dfsGroupListTree(childNode, deptMap, queryDeptIdSet, name, isInsert);
        }
    }

    @Override
    public Date getProjectEndDate(Long bizDemandId){
        // 获取该业务需求所关联的产品需求
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByBizDemandId(bizDemandId);
        if(productBizDemandDOList.isEmpty()){return null;}

        // 获取关联的产品需求相关的项目
        List<Long> productDemandIdList = productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList());
        List<ProjectDO> projectDOList = projectMapper.selectByProductDemandIdList(productDemandIdList);
        if(projectDOList.isEmpty()){return null;}

        Date result = null;
        for (ProjectDO projectDO : projectDOList) {
            Date projectEndDate = projectDO.getActualEndDate() == null? projectDO.getPlanEndDate(): projectDO.getActualEndDate();
            if(result == null){
                result = projectEndDate;
            }else{
                result = result.after(projectEndDate)? result: projectEndDate;
            }
        }
        return result;
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> page(BizDemandListCondition bizDemandListCondition) {
        Map<Long, String> deptMap = Maps.newHashMap();
        Set<Long> queryDeptIdSet =  Sets.newHashSet(bizDemandListCondition.getDeptIdList());
        GroupResponse rootNode = innerGroupClient.getGroupListTree(true);

        // 如果查询条件有部门id，收集子部门id及所需部门的完整名
        if(!queryDeptIdSet.isEmpty()){
            for (GroupResponse childNode : rootNode.getChildNode()){
                dfsGroupListTree(childNode, deptMap, queryDeptIdSet, "", false);
            }
            // 替换查询部门id条件
            bizDemandListCondition.setDeptIdList(Lists.newArrayList(deptMap.keySet()));
        }

        // 通配符、日期处理
        bizDemandListCondition.setName(StringUtil.toLikeStr(bizDemandListCondition.getName()));
        bizDemandListCondition.setCreateDateStart(DateUtil.getStartOfDay(bizDemandListCondition.getCreateDateStart()));
        bizDemandListCondition.setCreateDateEnd(DateUtil.getEndOfDay(bizDemandListCondition.getCreateDateEnd()));

        // 查询并转换
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(bizDemandListCondition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);

        // 如果查询条件没有部门id，收集完整名
        if(queryDeptIdSet.isEmpty()){
            queryDeptIdSet.addAll(bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList()));
            for (GroupResponse childNode : rootNode.getChildNode()){
                dfsGroupListTree(childNode, deptMap, queryDeptIdSet, "", false);
            }
        }

        // 部门名称待修改 ，需要完整名称
        bizDemandVOList.forEach( e -> {
            e.setStatusText(BizDemandStatusEnum.getTextByCode(e.getStatus()));
            e.setPriorityText(PriorityEnum.getTextChineseByCode(e.getPriority()));
            e.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(e.getPlanReleaseDate()));
            e.setDeptName(deptMap.get(e.getDeptId()));
        });

        // 返回分页数据
        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>(bizDemandListDOList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }
}
