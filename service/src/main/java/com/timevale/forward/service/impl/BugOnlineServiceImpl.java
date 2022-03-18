package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BugOfflineService;
import com.timevale.forward.facade.api.client.BugOnlineService;
import com.timevale.forward.facade.api.query.BugLogQueryList;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.copy.*;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Date 2022/3/17 16:59
 * @Author 望轩
 */
@Slf4j
@RestService
public class BugOnlineServiceImpl implements BugOnlineService {
    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private BugOfflineService bugOfflineService;

    @Override
    public BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        BusinessResult<ProductLineToFieldVO> businessResult = new BusinessResult<>();
        return businessResult;
    }

    @Override
    public BusinessResult<PageQueryResult<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList) {
        BusinessResult<PageQueryResult<BugOnlineVO>> businessResult = new BusinessResult<>();
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> add(BugOnlineAddReq bugOnlineAddReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> modify(BugOnlineModifyReq bugOnlineModifyReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq) {
        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineDetailReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //BugOnlineDO --> BugOnlineDetailVO
        BugOnlineDetailVO bugOnlineDetailVO = BugOnlineCopier.INSTANCE.convert(bugOnlineDO);

        //通过线上bug和产品线映射表查询所有的产品线id
        List<Long> productLineIdList = bugOnlineProductLineMapper.selectProductLineIds(bugOnlineDetailReq.getId());

        //如果产品线id不为空，批量查询产品线并进行类型转换
        if (CollectionUtils.isNotEmpty(productLineIdList)) {
            //批量查询产品线
            List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIdList);
            //转换 ProductLineDO --> ProductLineVO
            List<ProductLineVO> productLineVOList = productLineDOList.stream().map(ProductLineCopier.INSTANCE::convert).collect(Collectors.toList());
            //产品线信息存储到详情参数里面
            bugOnlineDetailVO.setProductLineVOList(productLineVOList);
        }

        Long bizDemandId = bugOnlineDO.getBizDemandId();
        //如果线上bug转化了业务需求，则查询并转化业务需求
        if (bizDemandId != null) {
            BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
            //bizDemandDO --> bizDemandVO
            BizDemandVO bizDemandVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);
            //业务需求信息存储到详情参数里面
            bugOnlineDetailVO.setBizDemandVO(bizDemandVO);
        }

        //查询附件
        List<FileDO> fileDOList = fileMapper.select(bugOnlineDetailReq.getId(), FileTypeEnum.BUG_ONLINE.getCode());
        //如果附件不为空，转化附件
        if (CollectionUtils.isNotEmpty(fileDOList)) {
            List<FileVO> fileVOList = fileDOList.stream().map(FileCopier.INSTANCE::change).collect(Collectors.toList());
            //附件信息存储到详情参数里面
            bugOnlineDetailVO.setFiles(fileVOList);
        }

        //查询抄送人
        List<PersonDO> personDOList = personMapper.select(PersonListCondition.builder()
                .mainId(bugOnlineDetailReq.getId())
                .type(PersonTypeEnum.BUG_ONLINE_CC.getCode())
                .build());
        //如果存在抄送人转化类型
        if (CollectionUtils.isNotEmpty(personDOList)) {
            List<PersonVO> personVOList = personDOList.stream()
                    .map(PersonCopier.INSTANCE::change).collect(Collectors.toList());
            //抄送人信息存储到详情参数里面
            bugOnlineDetailVO.setRecipientInfoList(personVOList);
        }

        //查询评论
        List<CommentDO> commentDOList = commentMapper
                .select(bugOnlineDetailReq.getId(), CommentTypeEnum.BUG_ONLINE.getCode());
        //如果评论表不为空，转化并添加到详情参数中
        if (CollectionUtils.isNotEmpty(commentDOList)) {
            List<CommentVO> commentVOList = commentDOList.stream().map(CommentCopier.INSTANCE::change)
                    .collect(Collectors.toList());
            bugOnlineDetailVO.setCommentVOList(commentVOList);
        }

        //信息填充
        bugOnlineDetailVO.setStatusName(BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()));
        bugOnlineDetailVO.setDismissCauseName(BugOnlineDismissCauseEnum.getTextByCode(bugOnlineDO.getDismissCause()));
        bugOnlineDetailVO.setEnvName(BugOnlineEnvStatus.getTextByCode(bugOnlineDO.getEnv()));
        bugOnlineDetailVO.setBelongName(BugOnlineBeloneEnum.getTextByCode(bugOnlineDO.getBelong()));
        bugOnlineDetailVO.setPriorityName(BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority()));
        bugOnlineDetailVO.setReasonName(BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason()));
        bugOnlineDetailVO.setRecurrentName(BugOnlineRecurrentEnum.getTextByCode(bugOnlineDO.getRecurrent()));

        BusinessResult<BugOnlineDetailVO> businessResult = new BusinessResult<>();
        businessResult.setData(bugOnlineDetailVO);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> startRepair(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq bugOnlineConfirmRepairReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> online(BugOnlineOnlineReq bugOnlineOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq) {
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }
}



















