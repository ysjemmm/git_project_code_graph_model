package com.timevale.forward.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.facade.api.client.BizDemandProductDemandService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandLinkProductDemandReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProductDemandReq;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.BizDemandStatusVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.ProductBizDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.BizDemandPlanReleaseDateMsgEvent;
import com.timevale.forward.service.observer.event.BizDemandStatusChangeMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.date.DateStyle;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/23 18:03
 */
@Slf4j
@LogPoint
@RestService
public class BizDemandProductDemandServiceImpl implements BizDemandProductDemandService {

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandService productDemandService;

    @Resource
    BizDemandComponent bizDemandComponent;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    MessageEventPublisher messageEventPublisher;

    @Resource
    BizDemandLogComponent bizDemandLogComponent;

    @Override
    public BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> linkedProductDemandList(BizDemandProductDemandQueryList bizDemandProductDemandQueryList) {
        // 开始分页
        PageHelper.startPage(bizDemandProductDemandQueryList.pageNum, bizDemandProductDemandQueryList.pageSize);

        List<BizDemandLinkProductDemandListDO> DOList = productDemandMapper.selectByBizDemandId(bizDemandProductDemandQueryList.getBizDemandId());
        List<BizDemandLinkProductDemandVO> VOList = BizDemandCopier.INSTANCE.transform(DOList);

        VOList.forEach( e -> {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
        });

        // 返回分页数据
        PageInfo<BizDemandLinkProductDemandListDO> pageInfo = new PageInfo<>(DOList);
        PageQueryResult<BizDemandLinkProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(VOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> getProductDemand(Long productDemandId) {
        return productDemandService.get(productDemandId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<BizDemandStatusVO> linkProductDemand(BizDemandLinkProductDemandReq bizDemandLinkProductDemandReq) {
        Long bizDemandId = bizDemandLinkProductDemandReq.getId();
        List<Long> productDemandIdList = bizDemandLinkProductDemandReq.getProductDemandIdList();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 获取当前关联数据
        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .build());

        // 数据转换为集合，判断交集补集
        Set<Long> newLinkData = new HashSet<>(productDemandIdList);
        Map<Long, ProductBizDemandDO> oldLinkDate = list.stream()
                .collect(Collectors.toMap(ProductBizDemandDO::getProductDemandId, Function.identity(), (a, b) -> a));

        // 更新和新增数据的集合
        List<ProductBizDemandDO> insertLinkData = Lists.newArrayList();
        List<Long> updateLinkData = Lists.newArrayList();

        // 判断旧数据是否存在新数据中，更新逻辑删除标识
        for (Map.Entry<Long, ProductBizDemandDO> entry : oldLinkDate.entrySet()) {
            if(newLinkData.contains(entry.getKey()) && entry.getValue().getIsDeleted()){
                updateLinkData.add(entry.getValue().getId());
            }
        }

        // 判断新数据是否在旧数据中，添加新增数据
        for (Long productDemandId : newLinkData) {
            if(!oldLinkDate.containsKey(productDemandId)){
                ProductBizDemandDO productBizDemandDO = ProductBizDemandCopier.INSTANCE.convert(bizDemandId, productDemandId);
                insertLinkData.add(productBizDemandDO);
            }
        }

        // 新增和更新非空数据
        if(!insertLinkData.isEmpty()){productBizDemandMapper.inserts(insertLinkData);}
        if(!updateLinkData.isEmpty()){productBizDemandMapper.updates(updateLinkData, false);}

        bizDemandComponent.updateBizDemandStatusByLinkedProductDemand(bizDemandId);

        BizDemandStatusVO bizDemandStatusVO = compareBizDemandStatus(bizDemandDO, true);

        // 日志
        bizDemandLogComponent.addLogWhenBizDemandLinkProductDemand(bizDemandId, productDemandIdList);

        return BaseResult.success(bizDemandStatusVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<BizDemandStatusVO> unlinkProductDemand(BizDemandUnlinkProductDemandReq bizDemandUnlinkProductDemandReq) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandUnlinkProductDemandReq.getBizDemandId());
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 查询对应数据
        Long bizDemandId = bizDemandUnlinkProductDemandReq.getBizDemandId();
        Long productDemandId = bizDemandUnlinkProductDemandReq.getProductDemandId();

        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .productDemandId(productDemandId)
                .isDeleted(false)
                .build());

        if(CollectionUtils.isEmpty(list)){
            throw new BaseBizRuntimeException("不存在对应的关联关系");
        }

        ProductBizDemandDO productBizDemandDO = list.get(0);

        productBizDemandMapper.delete(productBizDemandDO);

        bizDemandComponent.updateBizDemandStatusByLinkedProductDemand(bizDemandId);

        // 判断当前状态
        BizDemandStatusVO bizDemandStatusVO = compareBizDemandStatus(bizDemandDO, false);

        // 产品需求关联日志
        bizDemandLogComponent.addLogWhenBizDemandUnLinkProductDemand(bizDemandId, productDemandId);

        return BaseResult.success(bizDemandStatusVO);
    }

    private BizDemandStatusVO compareBizDemandStatus(BizDemandDO oldBizDemandDO, Boolean update){
        Long bizDemandId = oldBizDemandDO.getId();
        BizDemandDO newBizDemandDO = bizDemandMapper.selectById(bizDemandId);

        Integer oldStatus = oldBizDemandDO.getStatus();
        Integer newStatus = newBizDemandDO.getStatus();

        String statusText = BizDemandStatusEnum.getTextByCode(newStatus);
        Date newEndDate = bizDemandComponent.getProjectEndDate(bizDemandId);
        Integer planReleaseDate = newBizDemandDO.getPlanReleaseDate();

        // 如果新旧状态不同
        if(!oldStatus.equals(newStatus)){
            // 当前状态需要发送通知
            if(BizDemandStatusEnum.statusNeedNotice(newStatus)){
                messageEventPublisher.publish(new BizDemandStatusChangeMsgEvent(
                        this,
                        newBizDemandDO.getId(),
                        newBizDemandDO.getSubmitManId(),
                        newBizDemandDO.getName(),
                        statusText,
                        newEndDate
                ));
            }

            // 日志
            bizDemandLogComponent.addLogWhenModifyData(
                    BizDemandStatusEnum.getTextByCode(oldStatus),
                    BizDemandStatusEnum.getTextByCode(newStatus),
                    bizDemandId,
                    BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                    false
            );
        }

        Date oldEndDate = oldBizDemandDO.getProjectEndDate();
        if(!Objects.equals(oldEndDate, newEndDate)){
            //更新项目发布时间
            newBizDemandDO.setProjectEndDate(newEndDate);

            // 日志
            bizDemandLogComponent.addLogWhenModifyData(
                    oldEndDate == null ? StringUtils.EMPTY : DateUtil.parseToString(oldEndDate, DateStyle.YYYY_MM_DD),
                    newEndDate == null ? StringUtils.EMPTY : DateUtil.parseToString(newEndDate, DateStyle.YYYY_MM_DD),
                    bizDemandId,
                    BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText(),
                    false
            );


            // 更新预期上线时间
            if(newEndDate != null && update){
                planReleaseDate = DateUtil.getMonth(newEndDate) - 1;
                newBizDemandDO.setPlanReleaseDate(planReleaseDate);

                // 发送通知
                messageEventPublisher.publish(new BizDemandPlanReleaseDateMsgEvent(
                        this,
                        bizDemandId,
                        newBizDemandDO.getSubmitManId(),
                        newBizDemandDO.getName(),
                        BizDemandStatusEnum.getTextByCode(newBizDemandDO.getStatus()),
                        PlanReleaseDateEnum.getTextByCode(planReleaseDate)
                ));

                // 日志
                bizDemandLogComponent.addLogWhenModifyData(
                        PlanReleaseDateEnum.getTextByCode(oldBizDemandDO.getPlanReleaseDate()),
                        PlanReleaseDateEnum.getTextByCode(planReleaseDate),
                        bizDemandId,
                        BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                        false
                );
            }
            bizDemandMapper.fullUpdate(newBizDemandDO);
        }

        // 返回当前状态
        BizDemandStatusVO bizDemandStatusVO = new BizDemandStatusVO();
        bizDemandStatusVO.setStatus(newStatus);
        bizDemandStatusVO.setStatusText(statusText);
        bizDemandStatusVO.setProjectEndDate(newEndDate);
        bizDemandStatusVO.setPlanReleaseDate(planReleaseDate);
        bizDemandStatusVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(planReleaseDate));
        return bizDemandStatusVO;
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> matchProductDemandList(BizDemandLinkProductDemandQueryList bizDemandSubProductDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BizDemandLinkProductDemandListCondition condition = BizDemandCopier.INSTANCE.convert(bizDemandSubProductDemandQueryList);

        //通配符、日期处理处理
        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));

        // 过滤当前业务需求已经关联的产品需求
        List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandSubProductDemandQueryList.getBizDemandId())
                .isDeleted(false)
                .build());
        condition.setLinkedIdList(productBizDemandDOList.stream().map(ProductBizDemandDO::getProductDemandId).collect(Collectors.toList()));

        // 仅展示自己及其下属负责的产品需求
        List<String> allMyStaffWithSelfList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        Set<String> ownerIdSet = new HashSet<>(condition.getOwnerIdList());
        if(!CollectionUtils.isEmpty(ownerIdSet)){
            allMyStaffWithSelfList = allMyStaffWithSelfList.stream().filter(ownerIdSet::contains).collect(Collectors.toList());
            if(allMyStaffWithSelfList.isEmpty()){
                return BaseResult.success(ResultUtil.pageEmpty());
            }
        }
        condition.setOwnerIdList(allMyStaffWithSelfList);

        // 开始分页
        PageHelper.startPage(bizDemandSubProductDemandQueryList.pageNum, bizDemandSubProductDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        // 查询符合条件的产品需求
        List<BizDemandLinkProductDemandListDO> productDemandDOList = productDemandMapper.selectListOfBizDemandLink(condition);
        List<BizDemandLinkProductDemandVO> bizDemandLinkProductDemandVOList = BizDemandCopier.INSTANCE.transform(productDemandDOList);

        // 业务需求状态信息赋值
        bizDemandLinkProductDemandVOList.forEach(e -> {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(ProductDemandStatusEnum.getTextByCode(e.getStatus()));
        });

        PageInfo<BizDemandLinkProductDemandListDO> pageInfo = new PageInfo<>(productDemandDOList);
        PageQueryResult<BizDemandLinkProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandLinkProductDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<BizDemandStatusVO> getBizDemandStatus(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        BizDemandStatusVO statusVO = BizDemandCopier.INSTANCE.change(bizDemandDO);

        statusVO.setStatusText(BizDemandStatusEnum.getTextByCode(statusVO.getStatus()));
        statusVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(statusVO.getPlanReleaseDate()));

        return BaseResult.success(statusVO);
    }

}
