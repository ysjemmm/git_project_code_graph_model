package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.base.Objects;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.condition.ProductCustomDemandCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.client.CustomDemandService;
import com.timevale.forward.facade.api.query.CustomDemandQueryList;
import com.timevale.forward.facade.api.query.CustomLinkProductDemandQueryList;
import com.timevale.forward.facade.api.query.CustomProductDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.CustomDemandStatusVO;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.model.bo.ProductEndBO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.CustomDemandCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.CustomDemandToReceiveMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by xingyun
 * @date 2021/12/14 15:05
 */
@Slf4j
@LogPoint
@RestService
public class CustomDemandServiceImpl implements CustomDemandService {

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private CustomDemandMapper customDemandMapper;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private CustomDemandLogComponent customDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Resource
    private CustomDemandComponent customDemandComponent;

    @Resource
    private SqlOrderComponent sqlOrderComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductCustomDemandComponent productCustomDemandComponent;

    @Value("${custom.demand.receiver}")
    private String receiver;

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandQueryList customDemandQueryList) {
        log.info("客户需求列表,参数:{}", customDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        // 转换查询条件
        CustomDemandListCondition condition = CustomDemandCopier.INSTANCE.convert(customDemandQueryList);

        String ascription = customDemandQueryList.getAscription();
        if (ascription.equals(AscriptionEnum.RECEIVE.toString())) {
            condition.setReceiveManIds(Lists.newArrayList(userInfo.getId()));
        }
        String collation = sqlOrderComponent.build(customDemandQueryList.getOrderFiled(), customDemandQueryList.getOrderCollation());
        // 开始分页
        PageHelper.startPage(customDemandQueryList.pageNum, customDemandQueryList.pageSize, collation);

        return customDemandComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(CustomDemandAddReq customDemandAddReq) {
        log.info("客户需求新增,参数:{}", customDemandAddReq);
        CustomDemandDO customDemandDO = CustomDemandCopier.INSTANCE.convert(customDemandAddReq);
        List<ProductEndBO> receivers = JSONObject.parseArray(receiver, ProductEndBO.class);
        Map<Integer, ProductEndBO> receiverMap = receivers.stream().collect(Collectors.toMap(ProductEndBO::getCode, k -> k, (v1, v2) -> v2));
        if (!receiverMap.containsKey(customDemandDO.getProductEnd())) {
            throw new BaseBizRuntimeException("产品端不在所给定的范围内,请修改后重试");
        }
        String cause = ProblemTypeEnum.getTextByCode(customDemandDO.getCause());
        if (StringUtils.isEmpty(cause)) {
            throw new BaseBizRuntimeException("问题类别不在所给定的范围内,请修改后重试");
        }
        ProductEndBO productEndBO = receiverMap.get(customDemandDO.getProductEnd());
        customDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        customDemandDO.setName(productEndBO.getName() + "-" + cause + "-" + customDemandDO.getCustomName());
        customDemandDO.setReceiveMan(productEndBO.getOwner());
        customDemandDO.setReceiveManId(productEndBO.getOwnerId());

        checkBeforeInsert(customDemandDO);

        customDemandMapper.insert(customDemandDO);

        List<FileAddReq> fileIdList = customDemandAddReq.getFiles();
        fileComponent.add(fileIdList, customDemandDO.getId(), FileTypeEnum.CUSTOM_DEMAND.getCode());

        // 通知需求接收人
        messageEventPublisher.publish(new CustomDemandToReceiveMsgEvent(
                this,
                customDemandDO.getId(),
                customDemandDO.getCustomName() + "-" + customDemandDO.getSubmitMan(),
                customDemandDO.getReceiveManId(),
                customDemandDO.getName()
        ));
        // 日志, 状态改为待评估
        customDemandLogComponent.addLogWhenModifyData(
                StringUtils.EMPTY,
                BizDemandStatusEnum.EVALUATE.getText(),
                customDemandDO.getId(),
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.SUBMIT.getText());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<CustomDemandVO> get(Long customDemandId) {
        log.info("客户需求查看,参数:{}", customDemandId);
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        if (customDemandDO == null) {
            throw new BaseBizRuntimeException("该客户需求不存在");
        }
        // 信息填充
        CustomDemandVO customDemandVO = CustomDemandCopier.INSTANCE.convert(customDemandDO);
        List<ProductEndBO> receivers = JSONObject.parseArray(receiver, ProductEndBO.class);
        Map<Integer, ProductEndBO> receiverMap = receivers.stream().collect(Collectors.toMap(ProductEndBO::getCode, k -> k, (v1, v2) -> v2));
        customDemandVO.setProductEndText(receiverMap.get(customDemandVO.getProductEnd()).getName());
        customDemandVO.setStatusText(BizDemandStatusEnum.getTextByCode(customDemandVO.getStatus()));
        customDemandVO.setCauseText(ProblemTypeEnum.getTextByCode(customDemandVO.getCause()));
        customDemandVO.setReasonText(CustomDemandReasonEnum.getTextByCode(customDemandVO.getReason()));

        return BaseResult.success(customDemandVO);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(Long customDemandId) {
        log.info("客户需求接收,参数:{}", customDemandId);
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        if (customDemandDO == null) {
            throw new BaseBizRuntimeException("该客户需求不存在");
        }

        // 保存旧状态
        Integer oldStatus = customDemandDO.getStatus();
        Integer oldReason = customDemandDO.getReason();

        customDemandDO.setReason(null);
        customDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        customDemandMapper.fullUpdate(customDemandDO);

        customDemandComponent.updateStatusBaseOnProductDemand(customDemandId);

        // 日志, 状态改为同意
        CustomDemandDO newCustomDemandDO = customDemandMapper.selectById(customDemandId);
        Integer newStatus = newCustomDemandDO.getStatus();

        customDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.getTextByCode(newStatus),
                customDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.RECEIVE.getText());

        String oldReasonText = CustomDemandReasonEnum.getTextByCode(oldReason);
        if (StringUtils.isNotEmpty(oldReasonText)) {
            customDemandLogComponent.addLogWhenModifyData(
                    oldReasonText,
                    StringUtils.EMPTY,
                    customDemandId,
                    BizChangeLogFieldEnum.REASON.getText(),
                    false
            );
        }

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> reject(CustomDemandRejectReq customDemandRejectReq) {
        log.info("客户需求驳回,参数:{}", customDemandRejectReq);
        Long customDemandId = customDemandRejectReq.getId();
        Integer reason = customDemandRejectReq.getReason();

        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        if (customDemandDO == null) {
            throw new BaseBizRuntimeException("该客户需求不存在");
        }

        // 保存旧状态
        Integer oldStatus = customDemandDO.getStatus();

        customDemandDO.setReason(reason);
        customDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        customDemandMapper.fullUpdate(customDemandDO);

        // 日志
        customDemandLogComponent.addLogWhenModifyData(
                BizDemandStatusEnum.getTextByCode(oldStatus),
                BizDemandStatusEnum.REJECT.getText(),
                customDemandId,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.REJECT.getText());

        customDemandLogComponent.addLogWhenModifyData(
                StringUtils.EMPTY,
                CustomDemandReasonEnum.getTextByCode(reason),
                customDemandDO.getId(),
                BizChangeLogFieldEnum.REASON.getText(),
                true
        );

        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> transfer(BatchTransferReq batchTransferReq) {
        log.info("客户需求转交,参数:{}", batchTransferReq);
        String newReceiveMan = batchTransferReq.getReceiveMan();
        String newReceiveManId = batchTransferReq.getReceiveManId();
        List<Long> idList = batchTransferReq.getIdList();

        // 判空
        if (CollectionUtils.isEmpty(idList)) {
            return BaseResult.success(true);
        }

        // 批处理存储
        List<BizChangeLogDO> logDOList = new ArrayList<>();

        // 获取相关业务需求
        List<CustomDemandDO> customDemandDOList = customDemandMapper.selectByIds(idList);
        for (CustomDemandDO e : customDemandDOList) {
            String oldReceiveMan = e.getReceiveMan();
            // 新旧接收人相同则不处理
            if (Objects.equal(oldReceiveMan, newReceiveMan)) {
                continue;
            }
            // 日志
            BizChangeLogDO logDO = customDemandLogComponent.getLogWhenModifyData(
                    oldReceiveMan,
                    newReceiveMan,
                    e.getId(),
                    BizChangeLogFieldEnum.RECEIVE_MAN.getText(),
                    true
            );
            logDOList.add(logDO);
        }
        if (CollectionUtils.isNotEmpty(logDOList)) {
            // 日志
            bizChangeLogMapper.batchInsert(logDOList);
            // 实体
            idList = logDOList.stream().map(BizChangeLogDO::getMainId).collect(Collectors.toList());
            customDemandMapper.updateReceiveMan(idList, newReceiveMan, newReceiveManId);

            if (Integer.valueOf(0).equals(batchTransferReq.getType())) {
                //单个转交,通知
                HashSet<Long> customDemandIdSet = new HashSet<>(idList);
                customDemandDOList = customDemandDOList.stream().filter(e -> customDemandIdSet.contains(e.getId())).collect(Collectors.toList());
                customDemandDOList.forEach(e -> messageEventPublisher.publish(new CustomDemandToReceiveMsgEvent(
                        this,
                        e.getId(),
                        e.getCustomName() + "-" + e.getSubmitMan(),
                        newReceiveManId,
                        e.getName()
                )));
            }
        }

        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> completed(CustomDemandCompletedReq customDemandCompletedReq) {
        log.info("客户需求完成无需开发,参数:{}", customDemandCompletedReq);
        Long id = customDemandCompletedReq.getId();
        String solvePlan = customDemandCompletedReq.getSolvePlan();
        CustomDemandDO customDemandDO = customDemandMapper.selectById(id);

        Integer oldStatus = customDemandDO.getStatus();
        String oldSolvePlan = customDemandDO.getSolvePlan();

        // 更新
        customDemandDO.setStatus(BizDemandStatusEnum.COMPLETED.getCode());
        customDemandDO.setSolvePlan(solvePlan);
        customDemandMapper.fullUpdate(customDemandDO);

        // 日志
        String oldValue = BizDemandStatusEnum.getTextByCode(oldStatus);
        String newValue = BizDemandStatusEnum.getTextByCode(BizDemandStatusEnum.COMPLETED.getCode());
        customDemandLogComponent.addLogWhenModifyData(
                oldValue,
                newValue,
                id,
                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                true,
                ButtonActionEnum.COMPLETED_NOT_DEV.getText());

        customDemandLogComponent.addLogWhenModifyData(
                oldSolvePlan,
                solvePlan,
                id,
                BizChangeLogFieldEnum.SOLVE_PLAN.getText(),
                true);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> matchProductDemandList(CustomLinkProductDemandQueryList customDemandQueryList) {
        log.info("客户需求-产品需求匹配,参数:{}", customDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(customDemandQueryList);

        if (customDemandQueryList.getCustomDemandId() != null) {
            ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().customDemandId(customDemandQueryList.getCustomDemandId()).isDeleted(false).build();
            List<ProductCustomDemandDO> productBizDemand = productCustomDemandMapper.select(c);
            List<Long> productDemandIds = productBizDemand.stream().map(ProductCustomDemandDO::getProductDemandId).collect(Collectors.toList());
            condition.setFilterProductDemandIds(productDemandIds);
        }
        List<String> ownerIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        log.info("我和我的下属:receiveManIdList={}", ownerIdList);
        if (!CollectionUtils.isEmpty(condition.getOwnerIds())) {
            ownerIdList.retainAll(condition.getOwnerIds());
            log.info("我和我的下属,过滤后,receiveManIdList={}", ownerIdList);
        }
        if (CollectionUtils.isEmpty(ownerIdList)) {
            //所选人员不在我和我的下属中
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        condition.setOwnerIds(ownerIdList);
        condition.setStatus(Lists.newArrayList(ProductDemandStatusEnum.WAITING.getCode()
                , ProductDemandStatusEnum.INCLUDED.getCode()
                , ProductDemandStatusEnum.PROGRESS.getCode()
                , ProductDemandStatusEnum.ONLINE.getCode()));

        condition.setCreateDateStart(DateUtil.getStartOfDay(condition.getCreateDateStart()));
        condition.setCreateDateEnd(DateUtil.getEndOfDay(condition.getCreateDateEnd()));
        // 开始分页
        PageHelper.startPage(customDemandQueryList.pageNum, customDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        // 查询符合条件的产品需求
        List<ProductDemandListDO> productDemandDOList = productDemandMapper.list(condition);
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandDOList);

        productDemandVOList.forEach(a -> {
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));
            a.setStatusName(ProductDemandStatusEnum.getTextByCode(a.getStatus()));
        });

        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandDOList);
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<CustomDemandStatusVO> linkOrUnLinkProductDemand(CustomProductDemandLinkReq customDemandLinkReq) {
        log.info("关联or取消关联产品需求,参数:{}", customDemandLinkReq);
        List<Long> productDemandIds = customDemandLinkReq.getProductDemandIds();
        Long customDemandId = customDemandLinkReq.getCustomDemandId();
        Integer oldStatus = customDemandMapper.selectById(customDemandId).getStatus();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(customDemandLinkReq.getType())) {
            productCustomDemandComponent.batchInsert(productDemandIds, customDemandId);

            customDemandLogComponent.addLogWhenCustomDemandLinkProductDemand(customDemandId, productDemandIds);
        } else {
            Long productDemandId = productDemandIds.get(0);

            productCustomDemandComponent.update(productDemandId, customDemandId, true);

            customDemandLogComponent.addLogWhenCustomDemandUnLinkProductDemand(customDemandId, productDemandId);
        }
        //更新客户需求状态
        customDemandComponent.updateStatusBaseOnProductDemand(customDemandId);

        return BaseResult.success(getLastedInfo(oldStatus, customDemandId));
    }

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> linkProductDemandList(CustomProductDemandQueryList customDemandQueryList) {
        log.info("客户需求-产品需求清单,参数:{}", customDemandQueryList);
        // 开始分页
        PageHelper.startPage(customDemandQueryList.pageNum, customDemandQueryList.pageSize);
        List<ProductDemandListDO> productDemandList = productDemandMapper.linkProductDemandInCustomDemand(customDemandQueryList.getCustomDemandId());
        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandList);

        productDemandVOList.forEach(a -> {
            a.setPriorityName(PriorityEnum.getTextByCode(a.getPriority()));
            a.setStatusName(ProductDemandStatusEnum.getTextByCode(a.getStatus()));
        });
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandList);
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    private CustomDemandStatusVO getLastedInfo(Integer oldStatus, Long customDemandId) {
        CustomDemandDO newCustomDemandDO = customDemandMapper.selectById(customDemandId);

        Date projectEndDate = newCustomDemandDO.getProjectEndDate();
        Integer newStatus = newCustomDemandDO.getStatus();
        String statusText = BizDemandStatusEnum.getTextByCode(newStatus);


        if (!oldStatus.equals(newStatus)) {
            customDemandLogComponent.addLogWhenModifyData(
                    BizDemandStatusEnum.getTextByCode(oldStatus),
                    BizDemandStatusEnum.getTextByCode(newStatus),
                    customDemandId,
                    BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
                    false
            );
        }
        CustomDemandStatusVO customDemandStatusVO = new CustomDemandStatusVO();
        customDemandStatusVO.setStatus(newStatus);
        customDemandStatusVO.setStatusText(statusText);
        customDemandStatusVO.setProjectEndDate(projectEndDate);
        return customDemandStatusVO;
    }

    public void checkBeforeInsert(CustomDemandDO customDemandDO) {
        String date = DateUtil.parseToString(DateUtil.addMinute(new Date(), -1));
        List<CustomDemandDO> customDemandDos = customDemandMapper.selectByDate(date);
        customDemandDos.forEach(a -> {
            if (commonCompare(a, customDemandDO)) {
                throw new BaseBizRuntimeException("请勿在1分钟内新增相同内容的客户需求");
            }
        });
    }

    public boolean commonCompare(CustomDemandDO oldObj, CustomDemandDO newObj) {
        Field[] oldFields = oldObj.getClass().getDeclaredFields();
        Field[] newFields = newObj.getClass().getDeclaredFields();
        List<String> filter = Arrays.asList("solvePlan", "projectEndDate");
        Map<String, Field> newFieldMap = Arrays.stream(newFields).collect(Collectors.toMap(Field::getName, k -> k, (v1, v2) -> v2));
        for (Field oldField : oldFields) {
            Field newField = newFieldMap.get(oldField.getName());
            if (newField == null || filter.contains(newField.getName())) {
                continue;
            }
            oldField.setAccessible(true);
            newField.setAccessible(true);
            try {
                Object oldValue = oldField.get(oldObj);
                Object newValue = newField.get(newObj);
                if (!Objects.equal(oldValue, newValue)) {
                    return false;
                }
            } catch (IllegalAccessException e) {
                log.error("字段比较异常", e);
                e.printStackTrace();
            }
        }
        log.info("重复数据,newObj:{},id:{}", newObj,oldObj.getId());
        return true;
    }
}
