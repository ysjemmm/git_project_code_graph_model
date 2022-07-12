package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BugOnlineMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.facade.api.client.CustomDemandService;
import com.timevale.forward.facade.api.query.CustomDemandQueryList;
import com.timevale.forward.facade.api.request.CustomDemandAddReq;
import com.timevale.forward.facade.api.request.CustomDemandCompletedReq;
import com.timevale.forward.facade.api.request.CustomDemandRejectReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.model.enums.AscriptionEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.CustomDemandCopier;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/14 15:05
 */
@Slf4j
@LogPoint
@RestService
public class CustomDemandServiceImpl implements CustomDemandService {

    @Resource
    private BizDemandMapper bizDemandMapper;

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
    private BizDemandLogComponent bizDemandLogComponent;

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> list(CustomDemandQueryList customDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        // 转换查询条件
        CustomDemandListCondition condition = CustomDemandCopier.INSTANCE.convert(customDemandQueryList);

        String ascription = customDemandQueryList.getAscription();
         if (ascription.equals(AscriptionEnum.RECEIVE.toString())) {
             condition.setReceiveManIds(Lists.newArrayList(userInfo.getId()));
        }
        // 开始分页
        PageHelper.startPage(customDemandQueryList.pageNum, customDemandQueryList.pageSize,  CommonConstant.DEFAULT_ORDER_BY);

        // 分页数据
//        List<CustomDemandDO> customDemandDOList = customDemandMapper.list(condition);
//        List<CustomDemandVO> customDemandVOList = CustomDemandCopier.INSTANCE.convert(customDemandDOList);
        List<CustomDemandVO> customDemandVOList=Lists.newArrayList(new CustomDemandVO());
        List<CustomDemandDO> customDemandDOList=new ArrayList<>();
        PageInfo<CustomDemandDO> pageInfo = new PageInfo<>(customDemandDOList);
        PageQueryResult<CustomDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(customDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(CustomDemandAddReq customDemandAddReq) {
        // 新增业务需求
        CustomDemandDO customDemandDO = CustomDemandCopier.INSTANCE.convert(customDemandAddReq);
        customDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        customDemandMapper.insert(customDemandDO);

        List<FileAddReq> fileIdList = customDemandAddReq.getFileList();
        fileComponent.add(fileIdList, customDemandDO.getId(), FileTypeEnum.CUSTOM_DEMAND.getCode());

//        // 通知需求接收人
//        messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
//                this,
//                bizDemandDO.getId(),
//                bizDemandDO.getSubmitMan(),
//                bizDemandDO.getReceiveManId(),
//                bizDemandDO.getName()
//        ));
//
//        // 日志, 状态改为待评估
//        bizDemandLogComponent.addLogWhenModifyData(
//                BizDemandStatusEnum.EVALUATE.getText(),
//                BizDemandStatusEnum.EVALUATE.getText(),
//                bizDemandDO.getId(),
//                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
//                true,
//                ButtonActionEnum.SUBMIT.getText());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<CustomDemandVO> get(Long customDemandId) {
        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
        if (customDemandDO == null) {
            throw new BaseBizRuntimeException("该客户需求不存在");
        }
        // 信息填充
        CustomDemandVO customDemandVO = CustomDemandCopier.INSTANCE.convert(customDemandDO);

        return BaseResult.success(customDemandVO);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> agree(Long customDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();


//        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandId);
//        if (customDemandDO == null) {
//            throw new BaseBizRuntimeException("该客户需求不存在");
//        }
//
//        // 保存旧状态
//        Integer oldStatus = customDemandDO.getStatus();
//        Integer oldReason = customDemandDO.getReason();
//
//        customDemandDO.setReason(null);
//        customDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
//        bizDemandMapper.fullUpdate(customDemandDO);
//
//        // 通知需求提交人
////        messageEventPublisher.publish(new BizDemandReceivedMsgEvent(
////                this,
////                customDemandDO.getId(),
////                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
////                customDemandDO.getSubmitManId(),
////                customDemandDO.getName(),
////                PlanReleaseDateEnum.getTextByCode(customDemandDO.getPlanReleaseDate())
////        ));
//
//        bizDemandComponent.updateBizDemandStatusByLinkedProductDemand(bizDemandId);
//
//        // 日志, 状态改为同意
//        BizDemandDO newBizDemandDO = bizDemandMapper.selectById(bizDemandId);
//        Integer newStatus = newBizDemandDO.getStatus();
//
//        bizDemandLogComponent.addLogWhenModifyData(
//                BizDemandStatusEnum.getTextByCode(oldStatus),
//                BizDemandStatusEnum.getTextByCode(newStatus),
//                customDemandId,
//                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
//                true,
//                ButtonActionEnum.RECEIVE.getText());
//
//        String oldReasonText = BizDemandReasonEnum.getTextByCode(oldReason);
//        if (StringUtils.isNotEmpty(oldReasonText)) {
//            bizDemandLogComponent.addLogWhenModifyData(
//                    oldReasonText,
//                    StringUtils.EMPTY,
//                    customDemandId,
//                    BizChangeLogFieldEnum.REASON.getText(),
//                    false
//            );
//        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(CustomDemandRejectReq customDemandRejectReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
//        Long bizDemandId = customDemandRejectReq.getCustomDemandId();
//        Integer reason = customDemandRejectReq.getReason();
//
//        CustomDemandDO customDemandDO = customDemandMapper.selectById(customDemandRejectReq.getCustomDemandId());
//        if (customDemandDO == null) {
//            throw new BaseBizRuntimeException("该客户需求不存在");
//        }
//
//        // 保存旧状态
//        Integer oldStatus = customDemandDO.getStatus();
//
//        customDemandDO.setReason(reason);
//        customDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
//        bizDemandMapper.fullUpdate(customDemandDO);
//
//        // 驳回通知
//        messageEventPublisher.publish(new BizDemandRejectMsgEvent(
//                this,
//                customDemandDO.getId(),
//                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
//                customDemandDO.getSubmitManId(),
//                customDemandDO.getName(),
//                BizDemandReasonEnum.getTextByCode(customDemandDO.getReason())
//        ));
//
//        // 日志
//        bizDemandLogComponent.addLogWhenModifyData(
//                BizDemandStatusEnum.getTextByCode(oldStatus),
//                BizDemandStatusEnum.REJECT.getText(),
//                bizDemandId,
//                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
//                true,
//                ButtonActionEnum.REJECT.getText());
//
//        bizDemandLogComponent.addLogWhenModifyData(
//                StringUtils.EMPTY,
//                BizDemandReasonEnum.getTextByCode(reason),
//                customDemandDO.getId(),
//                BizChangeLogFieldEnum.REASON.getText(),
//                true
//        );

        return BaseResult.success(true);
    }


//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public BaseResult<Boolean> transfer(BatchTransferReq batchTransferReq) {
//        // 参数
//        String newReceiveMan = batchTransferReq.getReceiveMan();
//        String newReceiveManId = batchTransferReq.getReceiveManId();
//        List<Long> bizDemandIdList = batchTransferReq.getIdList();
//
//        // 判空
//        if (CollectionUtils.isEmpty(bizDemandIdList)) {
//            return BaseResult.success(true);
//        }
//
//        // 批处理存储
//        List<BizChangeLogDO> logDOList = new ArrayList<>();
//
//        // 获取相关业务需求
//        List<BizDemandDO> bizDemandDOList = bizDemandMapper.selectByIds(bizDemandIdList);
//        for (BizDemandDO e : bizDemandDOList) {
//            String oldReceiveMan = e.getReceiveMan();
//
//            // 新旧接收人相同则不处理
//            if (Objects.equal(oldReceiveMan, newReceiveMan)) {
//                continue;
//            }
//
//            // 日志
//            BizChangeLogDO logDO = bizDemandLogComponent.getLogWhenModifyData(
//                    oldReceiveMan,
//                    newReceiveMan,
//                    e.getId(),
//                    BizChangeLogFieldEnum.RECEIVE_MAN.getText(),
//                    true
//            );
//            logDOList.add(logDO);
//        }
//
//        // 判空
//        if (CollectionUtils.isNotEmpty(logDOList)) {
//            // 日志
//            bizChangeLogMapper.batchInsert(logDOList);
//            // 实体
//            bizDemandIdList = logDOList.stream().map(BizChangeLogDO::getMainId).collect(Collectors.toList());
//            bizDemandMapper.updateReceiveMan(bizDemandIdList, newReceiveMan, newReceiveManId);
//
//            // 通知
//            HashSet<Long> bizDemandIdSet = new HashSet<>(bizDemandIdList);
//            bizDemandDOList = bizDemandDOList.stream().filter(e -> bizDemandIdSet.contains(e.getId())).collect(Collectors.toList());
//            bizDemandDOList.forEach(e -> messageEventPublisher.publish(new BizDemandToReceiveMsgEvent(
//                    this,
//                    e.getId(),
//                    e.getSubmitMan(),
//                    newReceiveMan,
//                    e.getName()
//            )));
//        }
//
//        return BaseResult.success(true);
//    }


    @Override
    public BaseResult<Boolean> completed(CustomDemandCompletedReq customDemandCompletedReq) {
        // 参数
//        Long id = customDemandCompletedReq.getId();
//        String solvePlan = customDemandCompletedReq.getSolvePlan() == null ? StringUtils.EMPTY : customDemandCompletedReq.getSolvePlan();
//
//        BizDemandDO bizDemandDO = bizDemandMapper.selectById(id);
//        Integer oldStatus = bizDemandDO.getStatus();
//        Integer newStatus = BizDemandStatusEnum.TO_CONFIRM.getCode();
//
//        // 旧数据
//        String oldSolvePlan = bizDemandDO.getSolvePlan();
//
//        // 更新
//        bizDemandDO.setRejectReason(StringUtils.EMPTY);
//        bizDemandDO.setStatus(newStatus);
//        bizDemandDO.setSolvePlan(solvePlan);
//        bizDemandMapper.fullUpdate(bizDemandDO);
//
//        // 日志
//        String oldValue = BizDemandStatusEnum.getTextByCode(oldStatus);
//        String newValue = BizDemandStatusEnum.getTextByCode(newStatus);
//        bizDemandLogComponent.addLogWhenModifyData(
//                oldValue,
//                newValue,
//                id,
//                BizChangeLogFieldEnum.BIZ_DEMAND_STATUS.getText(),
//                true,
//                ButtonActionEnum.COMPLETED_NOT_DEV.getText());
//
//        if(!Objects.equal(oldSolvePlan,customDemandCompletedReq.getSolvePlan())){
//            bizDemandLogComponent.addLogWhenModifyData(
//                    oldSolvePlan,
//                    customDemandCompletedReq.getSolvePlan(),
//                    id,
//                    BizChangeLogFieldEnum.SOLVE_PLAN.getText(),
//                    true);
//        }
        return BaseResult.success(true);
    }

}
