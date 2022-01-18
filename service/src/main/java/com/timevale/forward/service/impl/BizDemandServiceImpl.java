package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.google.common.collect.Maps;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.query.BizDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandDetailVO;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.MessageComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/14 15:05
 */
@Slf4j
@RestService
public class BizDemandServiceImpl implements BizDemandService {

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductLineMapper productLineMapper;

    @Resource
    BizDomainMapper bizDomainMapper;

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    InnerUserPersonClient innerUserPersonClient;

    @Resource
    InnerGroupClient innerGroupClient;

    @Resource
    PersonComponent personComponent;

    @Resource
    FileComponent fileComponent;

    @Resource
    MessageComponent messageComponent;

    @Resource
    BizDemandComponent bizDemandComponent;

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> list(BizDemandQueryList bizDemandQueryList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BizDemandListCondition bizDemandListCondition = BizDemandCopier.INSTANCE.convert(bizDemandQueryList);

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bizDemandQueryList.getAscription();
        if(ascription.equals(AscriptionEnum.CURRENT_USER.toString())){
            bizDemandListCondition.setCreateManIdList(Lists.newArrayList(userInfo.getId()));
        }else if(ascription.equals(AscriptionEnum.RECEIVE.toString())){
            bizDemandListCondition.setReceiveManIdList(Lists.newArrayList(userInfo.getId()));
        }else if(ascription.equals(AscriptionEnum.COPIER.toString())){
            bizDemandListCondition.setCopier(userInfo.getId());
        }else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());
            if(ascription.equals(AscriptionEnum.TEAM_SUBMIT.toString())){
                Set<String> createIdSet = new HashSet<>(bizDemandListCondition.getCreateManIdList());
                if(!createIdSet.isEmpty()){
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                bizDemandListCondition.setCreateManIdList(teamMemberIdList);
            }else if(ascription.equals(AscriptionEnum.TEAM_RECEIVE.toString())){
                Set<String> receiveIdSet = new HashSet<>(bizDemandListCondition.getReceiveManIdList());
                if(!receiveIdSet.isEmpty()){
                    teamMemberIdList = teamMemberIdList.stream().filter(receiveIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                bizDemandListCondition.setReceiveManIdList(teamMemberIdList);
            }
        }
        if(resultIsEmpty){
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        // 开始分页
        PageHelper.startPage(bizDemandQueryList.pageNum, bizDemandQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        return bizDemandComponent.page(bizDemandListCondition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(BizDemandUpdateStatusReq bizDemandUpdateStatusReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 作废
        Long bizDemandId = bizDemandUpdateStatusReq.getBizDemandId();
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 修改业务需求状态
        bizDemandDO.setPlanReleaseDate(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.INVALID.getCode());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandMapper.update(bizDemandDO);

        // 取消产品关联
        productBizDemandMapper.deleteByBizDemandId(bizDemandId, userInfo.getAlias(), userInfo.getId());

        // 接收人通知
        messageComponent.bizDemandInvalidMsg(bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        );

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(BizDemandAddReq bizDemandAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 判断主题是否唯一
        BizDemandDO checkBizDemandDO = bizDemandMapper.selectByName(bizDemandAddReq.getName());
        if(checkBizDemandDO != null){
            throw new BaseBizRuntimeException("该业务需求名称已存在,请修改后重试");
        }

        // 新增业务需求
        BizDemandDO bizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandAddReq);
        bizDemandDO.setStatus(BizDemandStatusEnum.EVALUATE.getCode());
        bizDemandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        bizDemandDO.setCreateManId(userInfo.getId());
        bizDemandMapper.insert(bizDemandDO);

        List<FileAddReq> fileIdList = bizDemandAddReq.getFileList();
        if(!fileIdList.isEmpty()){
            fileComponent.add(fileIdList, bizDemandDO.getId(), FileTypeEnum.BIZ_DEMAND.getCode());
        }

        // 添加抄送人
        List<PersonAddReq> recipientInfoList = bizDemandAddReq.getRecipientInfoList();
        if(!recipientInfoList.isEmpty()){
            personComponent.add(recipientInfoList, bizDemandDO.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        messageComponent.bizDemandToReceiveMsg(bizDemandDO.getId(),
                bizDemandDO.getCreateMan(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<BizDemandDetailVO> getBizDemandById(Long bizDemandId) {
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        // 获取对应附件列表
        List<FileDO> fileDOList = fileComponent.select(bizDemandId, FileTypeEnum.BIZ_DEMAND.getCode());
        List<FileVO> fileVOList = FileCopier.INSTANCE.transform(fileDOList);

        // 获取对应抄送人
        List<PersonDO> personDOList = personComponent.select(bizDemandId, PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(personDOList);

        // 获取对应产品线，业务域
        ProductLineDO productLineDO = productLineMapper.selectById(bizDemandDO.getProductLineId());
        BizDomainDO bizDomainDO = bizDomainMapper.selectById(productLineDO.getBizDomainId());

        // 信息填充
        BizDemandDetailVO bizDemandDetailVO = BizDemandCopier.INSTANCE.convert(bizDemandDO);

        bizDemandDetailVO.setFileList(fileVOList);
        bizDemandDetailVO.setRecipientInfoList(personVOList);

        bizDemandDetailVO.setProductLineName(productLineDO.getName());
        bizDemandDetailVO.setReasonText(BizDemandReasonEnum.getTextByCode(bizDemandDetailVO.getReason()));
        bizDemandDetailVO.setStatusText(BizDemandStatusEnum.getTextByCode(bizDemandDetailVO.getStatus()));
        bizDemandDetailVO.setPriorityText(PriorityEnum.getTextChineseByCode(bizDemandDetailVO.getPriority()));
        bizDemandDetailVO.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(bizDemandDetailVO.getPlanReleaseDate()));
        if(BizDomainTypeEnum.KINGGRID.getCode().equals(bizDomainDO.getType())){
            bizDemandDetailVO.setOsText(OsEnum.getTextByCode(bizDemandDetailVO.getOs()));
            bizDemandDetailVO.setProcessorText(ProcessorEnum.getTextByCode(bizDemandDetailVO.getProcessor()));
        }
        Set<Long> deptIdSet =  Sets.newHashSet();
        deptIdSet.add(bizDemandDO.getDeptId());
        Map<Long, String> deptMap = Maps.newHashMap();
        GroupResponse rootNode = innerGroupClient.getGroupListTree(true);
        for (GroupResponse childNode : rootNode.getChildNode()){
            bizDemandComponent.dfsGroupListTree(childNode, deptMap, deptIdSet, "", false);
        }
        bizDemandDetailVO.setDeptName(deptMap.get(bizDemandDO.getDeptId()));

        //获取项目发布时间
        bizDemandDetailVO.setEndDate(bizDemandComponent.getProjectEndDate(bizDemandId));

        return BaseResult.success(bizDemandDetailVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(BizDemandModifyReq bizDemandModifyReq) {
        log.info("业务需求修改接收参数 bizDemandModifyReq = {}", bizDemandModifyReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求
        BizDemandDO oldBizDemandDO = bizDemandMapper.selectById(bizDemandModifyReq.getId());
        if(oldBizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        BizDemandDO newBizDemandDO = BizDemandCopier.INSTANCE.convert(bizDemandModifyReq);
        newBizDemandDO.setModifyMan(userInfo.getAlias());
        newBizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(newBizDemandDO);

        // 添加抄送人数据
        List<PersonAddReq> recipientInfoList = bizDemandModifyReq.getRecipientInfoList();
        if(!recipientInfoList.isEmpty()){
            personComponent.update(recipientInfoList, bizDemandModifyReq.getId(), PersonTypeEnum.BIZ_DEMAND_CC.getCode());
        }

        // 添加附件
        List<FileAddReq> fileIdList = bizDemandModifyReq.getFileList();
        if(!fileIdList.isEmpty()){
            fileComponent.update(fileIdList, bizDemandModifyReq.getId(), FileTypeEnum.BIZ_DEMAND.getCode());
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> agree(BizDemandAgreeReq bizDemandAgreeReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 接收，添加预期上线时间
        Long bizDemandId = bizDemandAgreeReq.getBizDemandId();
        Integer planReleaseDate = bizDemandAgreeReq.getPlanReleaseDate();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setStatus(BizDemandStatusEnum.RECEIVED.getCode());
        bizDemandDO.setPlanReleaseDate(planReleaseDate);
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 通知需求提交人
        messageComponent.bizDemandReceivedMsg(bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                PlanReleaseDateEnum.getTextByCode(bizDemandDO.getPlanReleaseDate())
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> reject(BizDemandRejectReq bizDemandRejectReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 修改业务需求状态 —— 驳回，添加驳回原因
        Long bizDemandId = bizDemandRejectReq.getBizDemandId();
        Integer reason = bizDemandRejectReq.getReason();

        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandId);
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setReason(reason);
        bizDemandDO.setPlanReleaseDate(null);
        bizDemandDO.setStatus(BizDemandStatusEnum.REJECT.getCode());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 驳回通知
        messageComponent.bizDemandRejectMsg(bizDemandDO.getId(),
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                bizDemandDO.getCreateManId(),
                bizDemandDO.getName(),
                BizDemandReasonEnum.getTextByCode(bizDemandDO.getReason())
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> transfer(BizDemandTransferReq bizDemandTransferReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转交：修改接收人
        BizDemandDO bizDemandDO = bizDemandMapper.selectById(bizDemandTransferReq.getId());
        if(bizDemandDO == null){
            throw new BaseBizRuntimeException("不存在该业务需求");
        }

        bizDemandDO.setReceiveMan(bizDemandTransferReq.getReceiveMan());
        bizDemandDO.setReceiveManId(bizDemandTransferReq.getReceiveManId());
        bizDemandDO.setModifyMan(userInfo.getAlias());
        bizDemandDO.setModifyManId(userInfo.getId());
        bizDemandMapper.update(bizDemandDO);

        // 转交人通知
        messageComponent.bizDemandToReceiveMsg(bizDemandDO.getId(),
                bizDemandDO.getCreateMan(),
                bizDemandDO.getReceiveManId(),
                bizDemandDO.getName()
        );

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> testNotice(Integer type) {
        if(type == 1){
            Date date = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyMMddE HH:mm:ss");
            System.out.println(sdf.format(DateUtil.getStartOfDay(date)));
            System.out.println(sdf.format(DateUtil.getEndOfDay(date)));
        }else{
            System.out.println(innerGroupClient.getAllSubSimpleGroupList(1L));
        }
        return BaseResult.success(true);
    }

}
