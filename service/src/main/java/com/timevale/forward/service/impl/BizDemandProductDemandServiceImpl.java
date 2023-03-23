package com.timevale.forward.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandLinkProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.facade.api.client.BizDemandProductDemandService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.BizDemandLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandLinkProductDemandReq;
import com.timevale.forward.facade.api.request.BizDemandUnlinkProductDemandReq;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.BizDemandStatusVO;
import com.timevale.forward.facade.api.result.BizLabelSimpleVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
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
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.CollectionUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
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
    private ProductBizDemandMapper productBizDemandMapper;
    @Resource
    private ProductDemandMapper productDemandMapper;
    @Resource
    private BizDemandMapper bizDemandMapper;
    @Resource
    private ProductDemandService productDemandService;
    @Resource
    private BizDemandComponent bizDemandComponent;
    @Resource
    private InnerUserPersonClient innerUserPersonClient;
    @Resource
    private MessageEventPublisher messageEventPublisher;
    @Resource
    private BizDemandLogComponent bizDemandLogComponent;
    @Resource
    private LabelComponent labelComponent;
    @Resource
    private BizLabelMapper bizLabelMapper;
    @Resource
    private BizLabelComponent bizLabelComponent;
    @Resource
    private ProjectComponent projectComponent;

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
    public BaseResult<BizDemandStatusVO> linkProductDemand(BizDemandLinkProductDemandReq req) {
        final Long bdId = req.getId();
        BizDemandDO bdDO = bizDemandMapper.get(bdId);
        AssertUtil.notNull(bdDO, "不存在该业务需求");

        // 创建和产品需求的关联关系
        List<Long> willLinkPdIdList = req.getProductDemandIdList();
        List<ProductBizDemandDO> newLinkData = willLinkPdIdList.stream()
                .map(e -> ProductBizDemandCopier.INSTANCE.convert(bdId, e))
                .collect(Collectors.toList());
        productBizDemandMapper.batchInsert(newLinkData);

        // 更新业务需求状态
        bizDemandComponent.updateStatus(bdId);

        // 日志
        bizDemandLogComponent.linkPd(bdId, willLinkPdIdList);

        // 更新客开项目状态
        bizDemandComponent.updateCustomerPj(bdId);

        BizDemandStatusVO bizDemandStatusVO = compareBizDemandStatus(bdDO);

        return BaseResult.success(bizDemandStatusVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<BizDemandStatusVO> unlinkProductDemand(BizDemandUnlinkProductDemandReq bizDemandUnlinkProductDemandReq) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandUnlinkProductDemandReq.getBizDemandId());
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

        // 关联的项目
        List<Long> linkProjectIds = bizDemandComponent.getLinkProjectIds(bizDemandId);

        ProductBizDemandDO productBizDemandDO = list.get(0);

        productBizDemandMapper.delete(productBizDemandDO);

        bizDemandComponent.updateStatus(bizDemandId);

        // 判断当前状态
        BizDemandStatusVO bizDemandStatusVO = compareBizDemandStatus(bizDemandDO);

        // 产品需求关联日志
        bizDemandLogComponent.unlinkPd(bizDemandId, productDemandId);

        // 刷新客开
        for (Long projectId : linkProjectIds) {
            projectComponent.updateCustomDev(projectId);
        }

        return BaseResult.success(bizDemandStatusVO);
    }

    private BizDemandStatusVO compareBizDemandStatus(BizDemandDO oldBizDemandDO){
        final Long bdId = oldBizDemandDO.getId();
        BizDemandDO newBizDemandDO = bizDemandMapper.get(bdId);

        Integer oldStatus = oldBizDemandDO.getStatus();
        Integer newStatus = newBizDemandDO.getStatus();

        String statusText = BizDemandStatusEnum.getTextByCode(newStatus);
        Date newEndDate = bizDemandComponent.getProjectEndDate(bdId);

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
                    bdId,
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
                    oldEndDate == null ? "" : DateUtil.parseToString(oldEndDate, DateStyle.YYYY_MM_DD),
                    newEndDate == null ? "" : DateUtil.parseToString(newEndDate, DateStyle.YYYY_MM_DD),
                    bdId,
                    BizChangeLogFieldEnum.PROJECT_RELEASE_DATE.getText(),
                    false
            );
        }

        Integer oldPlanReleaseDate = oldBizDemandDO.getPlanReleaseDate();
        Integer newPlanReleaseDate = newBizDemandDO.getPlanReleaseDate();

        // 更新预期上线时间
        if(newEndDate != null){
            newPlanReleaseDate = DateUtil.getMonth(newEndDate) - 1;

            if(!Objects.equals(oldPlanReleaseDate, newPlanReleaseDate)){
                newBizDemandDO.setPlanReleaseDate(newPlanReleaseDate);

                // 发送通知
                messageEventPublisher.publish(new BizDemandPlanReleaseDateMsgEvent(
                        this,
                        bdId,
                        newBizDemandDO.getSubmitManId(),
                        newBizDemandDO.getName(),
                        BizDemandStatusEnum.getTextByCode(newBizDemandDO.getStatus()),
                        PlanReleaseDateEnum.getTextByCode(newPlanReleaseDate)
                ));

                // 日志
                bizDemandLogComponent.addLogWhenModifyData(
                        PlanReleaseDateEnum.getTextByCode(oldBizDemandDO.getPlanReleaseDate()),
                        PlanReleaseDateEnum.getTextByCode(newPlanReleaseDate),
                        bdId,
                        BizChangeLogFieldEnum.PLAN_RELEASE_DATE.getText(),
                        false
                );
            }
        }


        bizDemandMapper.updateCanNull(newBizDemandDO);

        // 返回当前状态
        return new BizDemandStatusVO()
                .setStatus(newStatus)
                .setStatusText(statusText)
                .setProjectEndDate(newEndDate)
                .setPlanReleaseDate(newPlanReleaseDate)
                .setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(newPlanReleaseDate));
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

        List<Integer> statusList = new ArrayList<>();
        Integer status = bizDemandSubProductDemandQueryList.getStatus();
        if (status != null) {
            statusList.add(status);
        } else {
            statusList = Arrays.stream(ProductDemandStatusEnum.values())
                    .filter(e -> !ProductDemandStatusEnum.INVALID.equals(e) && !ProductDemandStatusEnum.ONLINE.equals(e))
                    .map(ProductDemandStatusEnum::getCode)
                    .collect(Collectors.toList());
        }
        condition.setStatusList(statusList);

        //是否打标
        List<BizLabelDO> bizLabelDOList;
        if(CollectionUtils.isNotEmpty(bizDemandSubProductDemandQueryList.getLabelIds())||CollectionUtils.isNotEmpty(bizDemandSubProductDemandQueryList.getLabelCategoryIds())){
            List<Long> newLabelIds = labelComponent.getLabelIds(bizDemandSubProductDemandQueryList.getLabelIds(), bizDemandSubProductDemandQueryList.getLabelCategoryIds());
            if(CollectionUtils.isEmpty(newLabelIds)){
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setContainIds(bizIds);
        }

        // 开始分页
        PageHelper.startPage(bizDemandSubProductDemandQueryList.pageNum, bizDemandSubProductDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        // 查询符合条件的产品需求
        List<BizDemandLinkProductDemandListDO> productDemandDOList = productDemandMapper.selectListOfBizDemandLink(condition);
        List<BizDemandLinkProductDemandVO> bizDemandLinkProductDemandVOList = BizDemandCopier.INSTANCE.transform(productDemandDOList);

        if (CollectionUtils.isEmpty(bizDemandLinkProductDemandVOList)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        List<Long>bizDemandIds = productDemandDOList.stream().map(BizDemandLinkProductDemandListDO::getId).collect(Collectors.toList());

        Map<Long, List<BizLabelSimpleVO>> bizLabelMap = bizLabelComponent.getBizLabelMap(bizDemandIds, BizTypeEnum.PRODUCT_DEMAND.getCode());

        // 业务需求状态信息赋值
        for (BizDemandLinkProductDemandVO e : bizDemandLinkProductDemandVOList) {
            e.setPriorityText(PriorityEnum.getTextByCode(e.getPriority()));
            e.setStatusText(ProductDemandStatusEnum.getTextByCode(e.getStatus()));

            List<BizLabelSimpleVO> labelSimpleVOList = bizLabelMap.get(e.getId());
            if (CollectionUtils.isNotEmpty(labelSimpleVOList)) {
                e.setLabelNames(labelSimpleVOList);
            }
        }

        PageInfo<BizDemandLinkProductDemandListDO> pageInfo = new PageInfo<>(productDemandDOList);
        PageQueryResult<BizDemandLinkProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandLinkProductDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<BizDemandStatusVO> getBizDemandStatus(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.get(bizDemandId);
        BizDemandStatusVO statusVO = BizDemandCopier.INSTANCE.change(bizDemandDO);

        statusVO.setStatusText(BizDemandStatusEnum.getTextByCode(statusVO.getStatus()));
        statusVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(statusVO.getPlanReleaseDate()));

        return BaseResult.success(statusVO);
    }

}
