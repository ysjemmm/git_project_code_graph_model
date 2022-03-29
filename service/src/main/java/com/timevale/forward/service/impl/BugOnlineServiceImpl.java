package com.timevale.forward.service.impl;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BugOnlineListCondition;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.BugOnlineService;
import com.timevale.forward.facade.api.query.BugOnlineQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BugOnlineMD;
import com.timevale.forward.model.middle.BusinessBeanMD;
import com.timevale.forward.model.middle.BusinessMD;
import com.timevale.forward.service.component.BugOnlineProductLineComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.*;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.*;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.compare.FieldCompareUtil;
import com.timevale.forward.service.utils.date.DateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.BusinessResult;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.request.AccountRequest;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.assertj.core.util.Sets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.function.Function;
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
    private BizDomainMapper bizDomainMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private FileMapper fileMapper;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private BugLogMapper bugLogMapper;

    @Resource
    private BugStatusOperatorMapper bugStatusOperatorMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private PersonComponent personComponent;

    @Value("${business}")
    private String business;

    @Resource
    private BugOnlineProductLineComponent bugOnlineProductLineComponent;

    @Override
    public BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        log.info("线上bug-从配置中心获取信息，接收参数：{}", bugOnlineGetFieldReq.getProductLineIdList());

        List<BusinessBeanMD> businessBeanMDList = JSON.parseArray(business, BusinessBeanMD.class);
        List<Long> productLineIdList = bugOnlineGetFieldReq.getProductLineIdList();
        Map<Integer, String> fieldMap = getFieldMap();
        ProductLineToFieldVO productLineToFieldVO = new ProductLineToFieldVO();
        List<String> fieldList = new ArrayList<>();
        productLineIdList.forEach(productLineId -> {
            for (int i = 0; i < businessBeanMDList.size(); i++) {
                BusinessBeanMD businessBeanMD = businessBeanMDList.get(i);
                if (businessBeanMD.getFieldValue().contains(productLineId)) {
                    fieldList.add(fieldMap.get(i));
                }
            }
        });

        //属性字段集合去重
        List<String> distinctFieldList = fieldList.stream().distinct().collect(Collectors.toList());

        BusinessResult<ProductLineToFieldVO> businessResult = new BusinessResult<>();
        productLineToFieldVO.setField(distinctFieldList);
        businessResult.setData(productLineToFieldVO);
        return businessResult;
    }

    //获取下标与字段属性之间的映射关系
    public Map<Integer, String> getFieldMap() {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "flowId");
        map.put(1, "mainOId");
        map.put(2, "templateId");
        map.put(3, "appId");
        map.put(4, "sealId");
        map.put(5, "operatorNameAccount");
        map.put(6, "loginAccount");
        return map;
    }

    @Override
    public BaseResult<PageQueryResult<BugOnlineVO>> list(BugOnlineQueryList bugOnlineQueryList) {
        log.info("线上bug-获取线上bug列表，接收参数：{}", bugOnlineQueryList);

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 转换查询条件
        BugOnlineListCondition condition = BugOnlineCopier.INSTANCE.convert(bugOnlineQueryList);
        // 时间处理
        condition.setCreateDateLeft(DateUtil.getStartOfDay(condition.getCreateDateLeft()));
        condition.setCreateDateRight(DateUtil.getEndOfDay(condition.getCreateDateRight()));
        condition.setModifyDateLeft(DateUtil.getStartOfDay(condition.getModifyDateLeft()));
        condition.setModifyDateRight(DateUtil.getEndOfDay(condition.getModifyDateRight()));

        // 标志是否有对应数据
        boolean resultIsEmpty = false;
        // 根据tabs添加不同的效果
        String ascription = bugOnlineQueryList.getAscription();
        if (AscriptionEnum.CURRENT_USER.toString().equals(ascription)) {
            condition.setProposerIdList(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.RECEIVE.toString().equals(ascription)) {
            condition.setOperatorIdList(Lists.newArrayList(userInfo.getId()));
        } else if (AscriptionEnum.COPIER.toString().equals(ascription)) {
            condition.setCopier(userInfo.getId());
        } else {
            List<String> teamMemberIdList = innerUserPersonClient.getAllMyStaffWithSelf(LocalSessionUtils.getUserInfo().getId(), true);
            if (AscriptionEnum.TEAM_SUBMIT.toString().equals(ascription)) {
                Set<String> createIdSet = Sets.newHashSet(condition.getProposerIdList());
                if (!createIdSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(createIdSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setProposerIdList(teamMemberIdList);
            } else if (AscriptionEnum.TEAM_RECEIVE.toString().equals(ascription)) {
                Set<String> operatorSet = Sets.newHashSet(condition.getOperatorIdList());
                if (!operatorSet.isEmpty()) {
                    teamMemberIdList = teamMemberIdList.stream().filter(operatorSet::contains).collect(Collectors.toList());
                    resultIsEmpty = teamMemberIdList.isEmpty();
                }
                condition.setOperatorIdList(teamMemberIdList);
            }
        }
        if (resultIsEmpty) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 开始分页
        PageHelper.startPage(bugOnlineQueryList.pageNum, bugOnlineQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);

        // 查询并转换
        List<BugOnlineListDO> bugOnlineDOList = bugOnlineMapper.selectListByCondition(condition);
        List<BugOnlineVO> bugOnlineVOList = bugOnlineDOList.stream().map(BugOnlineCopier.INSTANCE::convert).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bugOnlineVOList)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        // 查询对应产品线和业务域
        List<Long> bugOnlineIdList = bugOnlineVOList.stream().map(BugOnlineVO::getId).collect(Collectors.toList());
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = bugOnlineProductLineMapper.selectByBugOnlineIdList(bugOnlineIdList);

        List<Long> productLineIdList = bugOnlineProductLineDOList.stream().map(BugOnlineProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIdList);

        List<Long> bizDomainIdList = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toList());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(bizDomainIdList);

        Map<Long, ProductLineDO> productLineMap = productLineDOList.stream().collect(Collectors.toMap(ProductLineDO::getId, Function.identity()));
        Map<Long, BizDomainDO> bizDomainDOMap = bizDomainDOList.stream().collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));
        Map<Long, List<BugOnlineProductLineDO>> bugOnlineProductLineMap =
                bugOnlineProductLineDOList.stream().collect(Collectors.groupingBy(BugOnlineProductLineDO::getBugOnlineId));

        for (BugOnlineVO e : bugOnlineVOList) {
            // 关联的产品线id
            List<Long> eProductLineIdList = bugOnlineProductLineMap.get(e.getId())
                    .stream()
                    .map(BugOnlineProductLineDO::getProductLineId)
                    .collect(Collectors.toList());

            // 关联的产品线
            List<ProductLineDO> eProductLineDOList = eProductLineIdList
                    .stream()
                    .map(productLineMap::get)
                    .collect(Collectors.toList());

            // 关联的产品线名称
            List<String> eProductLineNameList = eProductLineDOList
                    .stream()
                    .map(ProductLineDO::getName)
                    .distinct()
                    .collect(Collectors.toList());

            // 关联的业务域
            List<Long> eBizDomainIdList = eProductLineDOList
                    .stream()
                    .map(ProductLineDO::getBizDomainId)
                    .collect(Collectors.toList());

            // 关联的业务域名称
            List<String> eBizDomainNameList = eBizDomainIdList
                    .stream()
                    .map(i -> bizDomainDOMap.get(i).getName())
                    .distinct()
                    .collect(Collectors.toList());

            e.setProductLineNameList(eProductLineNameList);
            e.setBizDomainNameList(eBizDomainNameList);
        }

        // 信息填充
        bugOnlineVOList.forEach(e -> {
            e.setEnvName(BugOnlineEnvEnum.getTextByCode(e.getEnv()));
            e.setStatusName(BugOnlineStatusEnum.getTextByCode(e.getStatus()));
            e.setBelongName(BugOnlineBeloneEnum.getTextByCode(e.getBelong()));
            e.setReasonName(BugOnlineReasonEnum.getTextByCode(e.getReason()));
            e.setPriorityName(BugOnlinePriorityEnum.getTextByCode(e.getPriority()));
        });

        // 返回分页数据
        PageInfo<BugOnlineListDO> pageInfo = new PageInfo<>(bugOnlineDOList);
        PageQueryResult<BugOnlineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bugOnlineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> add(BugOnlineAddReq bugOnlineAddReq) {
        log.info("线上bug-新增:接收参数{}", bugOnlineAddReq);

        //将BugOnlineAddReq转化为BugOnlineDO
        BugOnlineDO bugOnlineDO = BugOnlineCopier.INSTANCE.transfer(bugOnlineAddReq);

        //往线上bug表里面插入数据
        bugOnlineMapper.insert(bugOnlineDO);

        List<Long> productLineIdList = bugOnlineAddReq.getProductLineIdList();
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = new ArrayList<>();
        //如果产品线id不为空往线上bug和产品线的映射表中插入信息
        if (CollectionUtils.isNotEmpty(productLineIdList)) {
            productLineIdList.forEach(productLineId -> {
                BugOnlineProductLineDO bugOnlineProductLineDO = new BugOnlineProductLineDO();
                bugOnlineProductLineDO.setBugOnlineId(bugOnlineDO.getId());
                bugOnlineProductLineDO.setProductLineId(productLineId);
                bugOnlineProductLineDOList.add(bugOnlineProductLineDO);
            });
            bugOnlineProductLineMapper.batchInsert(bugOnlineProductLineDOList);
        }

        //如果有附件往附件表里存放数据
        List<FileAddReq> files = bugOnlineAddReq.getFiles();
        if (CollectionUtils.isNotEmpty(files)) {
            fileComponent.add(files, bugOnlineDO.getId(), FileTypeEnum.BUG_ONLINE.getCode());
        }

        //如果有抄送人往抄送人表里面添加数据
        List<PersonAddReq> recipients = bugOnlineAddReq.getRecipients();
        if (CollectionUtils.isNotEmpty(recipients)) {
            personComponent.add(recipients, bugOnlineDO.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());
        }

        //bug日志表记录一条新增数据
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setMainId(bugOnlineDO.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setOldValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        bugLogDO.setAction(ButtonActionEnum.SUBMIT.getText());
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineAddMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> delete(BugOnlineReq bugOnlineReq) {
        log.info("线上bug-删除,接收参数：{}", bugOnlineReq);

        BugOnlineDO bugOnlineDO = new BugOnlineDO();
        bugOnlineDO.setId(bugOnlineReq.getId());
        bugOnlineDO.setIsDeleted(true);

        //删除线上bug
        bugOnlineMapper.update(bugOnlineDO);

        //删除bug日志表中的数据
        bugLogMapper.deleteByBugId(bugOnlineReq.getId(), BugLogTypeEnum.ONLINE.getCode());

        BugOnlineProductLineDO bugOnlineProductLineDO = new BugOnlineProductLineDO();
        bugOnlineProductLineDO.setBugOnlineId(bugOnlineReq.getId());
        bugOnlineProductLineDO.setIsDeleted(true);
        //删除线上bug产品线映射表里面的数据
        bugOnlineProductLineMapper.update(bugOnlineProductLineDO);

        //删除抄送人表person中的数据
        PersonDO personDO = new PersonDO();
        personDO.setMainId(bugOnlineReq.getId());
        personDO.setType(PersonTypeEnum.BUG_ONLINE_CC.getCode());
        personDO.setIsDeleted(true);
        personMapper.update(personDO);

        //删除评论数据
        commentMapper.deleteByToIdAndType(bugOnlineReq.getId(), CommentTypeEnum.BUG_ONLINE.getCode());

        //删除附件数据
        FileDO fileDO = new FileDO();
        fileDO.setIsDeleted(true);
        fileDO.setAttacheId(bugOnlineReq.getId());
        fileDO.setType(FileTypeEnum.BUG_ONLINE.getCode());
        fileMapper.update(fileDO);

        //查询所有的状态变更id
        List<BugLogDO> bugLogDOList = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineDO.getId(), BugLogTypeEnum.ONLINE.getCode(), true);
        List<Long> bugLogStatusIdList = bugLogDOList.stream().map(BugLogDO::getId).collect(Collectors.toList());

        //删除bug状态人员处理表里面的数据
        if (CollectionUtils.isNotEmpty(bugLogStatusIdList)) {
            bugStatusOperatorMapper.deleteByBugLogId(bugLogStatusIdList);
        }

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> modify(BugOnlineModifyReq bugOnlineModifyReq) {
        log.info("线上bug-修改,接收参数：{}", bugOnlineModifyReq);

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineModifyReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //老的线上bug比较对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        //是否为经办人&提出人及其上级
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        if (!operatorResult && !proposerResult) {
            throw new BaseBizRuntimeException("您没有修改权限");
        }

        //BugOnlineModifyReq -->  BugOnlineDO
        BugOnlineDO bugOnlineConvert = BugOnlineCopier.INSTANCE.change(bugOnlineModifyReq);
        //更新线上bug
        bugOnlineMapper.update(bugOnlineConvert);

        //更新附件表
        List<FileAddReq> files = bugOnlineModifyReq.getFiles();
        fileComponent.update(files, bugOnlineModifyReq.getId(), FileTypeEnum.BUG_ONLINE.getCode());

        //更新抄送人表
        List<PersonAddReq> recipients = bugOnlineModifyReq.getRecipients();
        personComponent.update(recipients, bugOnlineModifyReq.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());

        List<Long> productLineIdList = bugOnlineModifyReq.getProductLineIdList();
        //更新线上bug和产品线映射表
        bugOnlineProductLineComponent.update(productLineIdList, bugOnlineModifyReq.getId());

        //新的线上bug比较对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.convert(bugOnlineModifyReq);
        BugOnlineDO newBugOnlineDO = BugOnlineCopier.INSTANCE.change(bugOnlineModifyReq);

        //比较编辑修改的一般字段，生成结果集合
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);
        //额外判断产品线和产品线业务
        bugLogDOList.addAll(compareExtraIfNecessary(bugOnlineDO, newBugOnlineDO));
        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //如果经办人变了，但是状态没有变化，需要往状态人员处理表中插入一条数据，并且需要发送钉钉消息
        if (!bugOnlineDO.getOperatorId().equals(newBugOnlineDO.getOperatorId())) {
            //往bug状态人员处理表中插入一条记录
            insertToBugStatusOperator(bugOnlineModifyReq.getId());

            //发送钉钉消息
            messageEventPublisher.publish(
                    new BugOnlineModifyMsgEvent(
                            this,
                            bugOnlineDO.getName(),
                            BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                            bugOnlineModifyReq.getOperatorId(),
                            bugOnlineDO.getId()
                    )
            );
        }

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq) {
        log.info("线上bug-得到线上bug详情，接收参数:{}", bugOnlineDetailReq);

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
        if (bugOnlineDO.getDismissCause() != null) {
            bugOnlineDetailVO.setDismissCauseName(BugOnlineDismissCauseEnum.getTextByCode(bugOnlineDO.getDismissCause()));
        }
        if (bugOnlineDO.getRepairFailReason() != null) {
            bugOnlineDetailVO.setRepairFailReason(bugOnlineDO.getRepairFailReason());
        }
        bugOnlineDetailVO.setEnvName(BugOnlineEnvEnum.getTextByCode(bugOnlineDO.getEnv()));
        bugOnlineDetailVO.setBelongName(BugOnlineBeloneEnum.getTextByCode(bugOnlineDO.getBelong()));
        bugOnlineDetailVO.setPriorityName(BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority()));
        if (bugOnlineDO.getReason() != null) {
            bugOnlineDetailVO.setReasonName(BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason()));
        }
        bugOnlineDetailVO.setRecurrentName(BugOnlineRecurrentEnum.getTextByCode(bugOnlineDO.getRecurrent()));

        BusinessResult<BugOnlineDetailVO> businessResult = new BusinessResult<>();
        businessResult.setData(bugOnlineDetailVO);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> confirm(BugOnlineReq bugOnlineReq) {
        log.info("线上bug待确认接收参数：{}", bugOnlineReq.getId());

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为上报状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.PROBLEM_REPORT.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击bug确认");
        }

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.CONFIRM.getText());
        bugLogDO.setOldValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_CONFIRM.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());

        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> startRepair(BugOnlineStartRepairReq bugOnlineStartRepairReq) {
        log.info("线上bug开始修复接收参数：{}", bugOnlineStartRepairReq.getId());

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineStartRepairReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题确认”，“挂起”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.HANG_UP.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击开始修复");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
        bugOnlineDO.setReason(bugOnlineStartRepairReq.getReason());
        bugOnlineDO.setProblemReason(bugOnlineStartRepairReq.getProblemReason());
        bugOnlineDO.setSolveScheme(bugOnlineStartRepairReq.getSolveScheme());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.START_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_REPAIR.getText());
        bugLogDO.setMainId(bugOnlineStartRepairReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> repairFinished(BugOnlineRepairFinishedReq bugOnlineRepairFinishedReq) {
        log.info("线上bug修复完毕接收参数：{}", bugOnlineRepairFinishedReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineRepairFinishedReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题修复”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击修复完毕");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        //保存当前经办人
        String operator = bugOnlineDO.getOperator();
        String operatorId = bugOnlineDO.getOperatorId();

        //保存老的修复失败原因
        String repairFailReason = bugOnlineDO.getRepairFailReason();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode());
        bugOnlineDO.setRepairFailReason(null);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setOperator(bugOnlineRepairFinishedReq.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineRepairFinishedReq.getOperatorId());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPAIR_FINISH.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.REPAIR_CONFIRM.getText());
        bugLogDO.setMainId(bugOnlineRepairFinishedReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //如果此时修复失败原因有值，则需要插入一条bug内容变更记录，因为需要把修复失败原因清空
        if (repairFailReason != null) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
            bugLog.setOldValue(bugOnlineDO.getRepairFailReason());
            bugLog.setMainId(bugOnlineRepairFinishedReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRepairFinishedMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineRepairFinishedReq.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> confirmRepair(BugOnlineConfirmRepairReq bugOnlineConfirmRepairReq) {
        log.info("线上bug确认修复接收参数：{}", bugOnlineConfirmRepairReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineConfirmRepairReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“QA修复确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击确认修复");
        }

        //校验当前操作人职能是否为测试
        Boolean result = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        if (!result) {
            throw new BaseBizRuntimeException("您的职能没有权限点击此按钮");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.ONLINE.getCode());
        if (bugOnlineConfirmRepairReq.getReason() != null) {
            bugOnlineDO.setReason(bugOnlineConfirmRepairReq.getReason());
        }
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.CONFIRM_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.ONLINE.getText());
        bugLogDO.setMainId(bugOnlineConfirmRepairReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> online(BugOnlineOnlineReq bugOnlineOnlineReq) {
        log.info("线上bug已上线接收参数：{}", bugOnlineOnlineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“待上线”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.ONLINE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击已上线");
        }

        //校验点击按钮的人是否为测试角色或者是经办人&提出人及其上级
        Boolean jobFunctionResult = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        if (jobFunctionResult && operatorResult && proposerResult) {
            throw new BaseBizRuntimeException("您没有权限点击此按钮");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.COMPLETE.getCode());
        if (bugOnlineOnlineReq.getReason() != null) {
            bugOnlineDO.setReason(bugOnlineOnlineReq.getReason());
        }
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.ONLINE.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.COMPLETE.getText());
        bugLogDO.setMainId(bugOnlineOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineOnlineMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        bugOnlineDO.getProposerId(),
                        bugOnlineOnlineReq.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> openAgain(BugOnlineOpenAgainReq bugOnlineOpenAgainReq) {
        log.info("线上bug-重新打开接收参数：{}", bugOnlineOpenAgainReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineOpenAgainReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“完成”或者“关闭”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.COMPLETE.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.CLOSE.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击重新打开");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存当前经办人和上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperator = bugOnlineDO.getLastOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();
        //保存老的驳回原因
        Integer oldDismissCause = bugOnlineDO.getDismissCause();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setDismissCause(null);
        bugOnlineDO.setOpenAgainReason(bugOnlineOpenAgainReq.getOpenAgainReason());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.OPEN_AGAIN.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setMainId(bugOnlineOpenAgainReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //如果原来的驳回原因不为空，要加入一条内容记录
        if (oldDismissCause != null) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.DISMISS_CAUSE.getText());
            bugLog.setOldValue(BugOnlineDismissCauseEnum.getTextByCode(oldDismissCause));
            bugLog.setMainId(bugOnlineOpenAgainReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //往bug日志表中插入一条线上bug内容变更数据
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineOpenAgainMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq) {
        log.info("线上bug-不用修复接收参数：{}", bugOnlineNoRepairReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineNoRepairReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题上报”或者“问题确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.PROBLEM_REPORT.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击不用修复");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.BE_CONFIRM.getCode());
        bugOnlineDO.setLastOperatorId(bugOnlineDO.getOperatorId());
        bugOnlineDO.setLastOperator(bugOnlineDO.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineDO.getProposerId());
        bugOnlineDO.setOperator(bugOnlineDO.getOperator());
        bugOnlineDO.setDismissCause(bugOnlineNoRepairReq.getDismissCause());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.NO_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.BE_CONFIRM.getText());
        bugLogDO.setMainId(bugOnlineNoRepairReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //因为新增了驳回原因所以这里需要加入一条内容变更记录
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.DISMISS_CAUSE.getText());
        bugLog.setNewValue(BugOnlineDismissCauseEnum.getTextByCode(bugOnlineDO.getDismissCause()));
        bugLog.setMainId(bugOnlineNoRepairReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineNoRepairMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> transfer(BugOnlineTransferReq bugOnlineTransferReq) {
        log.info("线上bug-转交接收参数：{}", bugOnlineTransferReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineTransferReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断操作人是否有点击权限
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        Boolean testJobFunctionResult = false;
        if (bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode())) {
            testJobFunctionResult = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        }
        if (!operatorResult && !proposerResult && !testJobFunctionResult) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的经办人
        String oldOperator = bugOnlineDO.getOperator();

        bugOnlineDO.setLastOperatorId(bugOnlineDO.getOperatorId());
        bugOnlineDO.setLastOperator(bugOnlineDO.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineTransferReq.getUserId());
        bugOnlineDO.setOperator(bugOnlineTransferReq.getUserName());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.OPERATOR.getText());
        bugLog.setOldValue(oldOperator);
        bugLog.setNewValue(bugOnlineTransferReq.getUserName());
        bugLog.setMainId(bugOnlineTransferReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //如果不是自己转交给自己，bug状态处理人员表插入数据
        if (!oldOperator.equals(bugOnlineTransferReq.getUserName())) {
            //查询当前线上bug对应的所有状态变更记录
            List<BugLogDO> bugLogDOS = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineDO.getId()
                    , BugLogTypeEnum.ONLINE.getCode(), true);

            //按创建时间逆序排列，筛选出最后一条状态变更记录
            List<BugLogDO> collect = bugLogDOS.stream()
                    .sorted(Comparator.comparing(BugLogDO::getCreateDate).reversed()).collect(Collectors.toList());
            BugLogDO lastStatusBugLogDO = collect.get(0);

            BugStatusOperatorDO bugStatusOperatorDO = new BugStatusOperatorDO();
            bugStatusOperatorDO.setBugLogId(lastStatusBugLogDO.getId());
            bugStatusOperatorDO.setOperator(bugOnlineTransferReq.getUserName());
            bugStatusOperatorDO.setOperatorId(bugOnlineTransferReq.getUserId());
            //往状态人员处理表里面插入一条数据记录
            bugStatusOperatorMapper.insert(bugStatusOperatorDO);
        }

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineTransferMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                        bugOnlineTransferReq.getUserId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    public BusinessResult<Boolean> agree(BugOnlineReq bugOnlineReq) {
        log.info("线上bug-同意接收参数：{}", bugOnlineReq.getId());

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题上报”或者“问题确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.BE_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击同意");
        }

        //判断操作人是否有点击权限
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        if (!operatorResult && !proposerResult) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的经办人
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.CLOSE.getCode());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.AGREE.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.CLOSE.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> reject(BugOnlineReq bugOnlineReq) {
        log.info("线上bug-拒绝,接收参数：{}", bugOnlineReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“待确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.BE_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击拒绝");
        }

        //判断操作人是否有点击权限
        Boolean operatorResult = isPermission(bugOnlineDO.getOperatorId());
        Boolean proposerResult = isPermission(bugOnlineDO.getProposerId());
        if (!operatorResult && !proposerResult) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存老的驳回原因
        String oldDismissCause = BugOnlineDismissCauseEnum.getTextByCode(bugOnlineDO.getDismissCause());
        //保存老的经办人和老的上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();
        String lastOperator = bugOnlineDO.getLastOperator();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setDismissCause(null);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REFUSED.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //因为清空了驳回原因所以这里需要加入一条内容变更记录
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.DISMISS_CAUSE.getText());
        bugLog.setOldValue(oldDismissCause);
        bugLog.setMainId(bugOnlineReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRejectMsgEvent(
                        this,
                        userInfo.getAlias() + "-" + userInfo.getName(),
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> reconfirm(BugOnlineReq bugOnlineReq) {
        log.info("线上bug-重新确认,接收参数:{}", bugOnlineReq.getId());

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题确认”或者“问题修复”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击重新确认");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        //判断是从哪个状态点击的重新确认按钮
        if(oldStatus.equals(BugOnlineStatusEnum.QUESTION_REPAIR.getText())){
            bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        } else {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        }
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPEAT_CONFIRM.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> temporaryNoRepair(BugOnlineReq bugOnlineReq) {
        log.info("线上bug-暂不修复,接收参数:{}", bugOnlineReq.getId());

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“问题确认”或者“问题修复”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode())
                && !bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.QUESTION_REPAIR.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击暂不修复");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());

        bugOnlineDO.setStatus(BugOnlineStatusEnum.HANG_UP.getCode());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.TEMPORARY_NO_REPAIR.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.HANG_UP.getText());
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<Boolean> repairFailed(BugOnlineRepairFailedReasonReq bugOnlineRepairFailedReasonReq) {
        log.info("线上bug-修复失败,接收参数：{}", bugOnlineRepairFailedReasonReq.getId());

        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineRepairFailedReasonReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //判断当前状态是否为“QA修复确认”状态
        if (!bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.REPAIR_CONFIRM.getCode())) {
            throw new BaseBizRuntimeException("当前状态不允许点击修复失败");
        }

        //判断操作人是否有点击权限
        Boolean result = jobFunctionMatch(userInfo.getId(), JobFunctionEnum.QA.getName());
        if (!result) {
            throw new BaseBizRuntimeException("您没有点击此按钮的权限");
        }

        //保存老的状态
        String oldStatus = BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus());
        //保存老的经办人和老的上一阶段经办人
        String operatorId = bugOnlineDO.getOperatorId();
        String operator = bugOnlineDO.getOperator();
        String lastOperatorId = bugOnlineDO.getLastOperatorId();
        String lastOperator = bugOnlineDO.getLastOperator();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setRepairFailReason(bugOnlineRepairFailedReasonReq.getRepairFailReason());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPAIR_FAIL.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_REPAIR.getText());
        bugLogDO.setMainId(bugOnlineRepairFailedReasonReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //因为增加了修复失败原因所以这里需要加入一条内容变更记录
        BugLogDO bugLog = new BugLogDO();
        bugLog.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
        bugLog.setNewValue(bugOnlineRepairFailedReasonReq.getRepairFailReason());
        bugLog.setMainId(bugOnlineRepairFailedReasonReq.getId());
        bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
        //往bug日志表中插入一条线上bug内容变更数据
        bugLogMapper.insert(bugLog);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineRepairFailedMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        bugOnlineDO.getOperatorId(),
                        bugOnlineDO.getId()
                )
        );

        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    /**
     * 判断当前操作人是否为personId或者personId的上级
     */
    Boolean isPermission(String personId) {
        //得到当前操作人账户
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        String account = userInfo.getId();

        //如果当前操作人是权限人员，直接返回true
        if (personId.equals(account)) {
            return true;
        }

        //如果当前操作人不是直接权限人，看看是不是直接权限人的上级
        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setAccount(personId);
        Set<String> higherLevels = innerUserPersonClient.getAllSuperiorByAccount(accountRequest).getData();

        //判断当前操作人账户是否有权限
        return higherLevels.contains(account);
    }

    /**
     * 判断用户是否为某个职能
     */
    Boolean jobFunctionMatch(String personId, String jobFunction) {
        ArrayList<String> operatorIds = Lists.newArrayList(personId);
        //校验当前经办人职能是否为测试
        List<BaseInfoResponse> personByAccountNew = innerUserPersonClient.getPersonByAccountNew(operatorIds);
        BaseInfoResponse baseInfoResponse = personByAccountNew.get(0);
        if (baseInfoResponse != null) {
            return jobFunction.equals(baseInfoResponse.getJobFunction());
        }
        return true;
    }

    /**
     * 根据线上bug的id，往bug状态人员处理表中插入一条数据
     */
    public void insertToBugStatusOperator(Long bugOnlineId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        //查询当前线上bug对应的所有状态变更记录
        List<BugLogDO> bugLogDOS = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineId
                , BugLogTypeEnum.ONLINE.getCode(), true);

        //按创建时间逆序排列，筛选出最后一条状态变更记录
        List<BugLogDO> collect = bugLogDOS.stream()
                .sorted(Comparator.comparing(BugLogDO::getCreateDate).reversed()).collect(Collectors.toList());
        BugLogDO lastStatusBugLogDO = collect.get(0);

        BugStatusOperatorDO bugStatusOperatorDO = new BugStatusOperatorDO();
        bugStatusOperatorDO.setBugLogId(lastStatusBugLogDO.getId());
        bugStatusOperatorDO.setOperator(userInfo.getAlias() + "-" + userInfo.getName());
        bugStatusOperatorDO.setOperatorId(userInfo.getId());
        //往状态人员处理表里面插入一条数据记录
        bugStatusOperatorMapper.insert(bugStatusOperatorDO);
    }

    /**
     * 线上bug特殊字段比较
     *
     * @param oldObj 老的线上bug对象
     * @param newObj 新的线上bug对象
     * @return 返回结果集合
     */
    private List<BugLogDO> compareExtraIfNecessary(BugOnlineDO oldObj, BugOnlineDO newObj) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();

        List<Long> oldProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(oldObj.getId());
        List<Long> newProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(newObj.getId());
        boolean result = CollectionUtils.isEqualCollection(oldProductLineIdList, newProductLineIdList);
        //如果产品线变了记录一条bug内容变更日志
        if (!result) {
            StringBuilder oldNames = new StringBuilder();
            StringBuilder newNames = new StringBuilder();
            List<ProductLineDO> oldProductLineDOList = productLineMapper.selectByIds(oldProductLineIdList);
            List<ProductLineDO> newProductLineDOList = productLineMapper.selectByIds(newProductLineIdList);
            if (CollectionUtils.isNotEmpty(oldProductLineDOList)) {
                Integer count = 0;
                for (ProductLineDO productLineDO : oldProductLineDOList) {
                    oldNames.append(productLineDO.getName());
                    count++;
                    if (!count.equals(oldProductLineDOList.size())) {
                        oldNames.append("&");
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(newProductLineIdList)) {
                Integer tally = 0;
                for (ProductLineDO productLine : newProductLineDOList) {
                    newNames.append(productLine.getName());
                    tally++;
                    if (!tally.equals(newProductLineDOList.size())) {
                        newNames.append("&");
                    }
                }
            }

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PRODUCT_LINE.getText());
            bugLogDO.setOldValue(oldNames.toString());
            bugLogDO.setNewValue(newNames.toString());
            bugLogDO.setMainId(oldObj.getId());
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }

        String oldBusiness = oldObj.getBusiness().replace("'", "");
        String newBusiness = newObj.getBusiness().replace("'", "");
        //如果产品线业务这个json字符串变了，要记录一条或多条内容变更日志
        if (!oldBusiness.equals(newBusiness)) {
            BusinessMD oldBusinessMD = new BusinessMD();
            BusinessMD newBusinessMD = new BusinessMD();
            if (!"".equals(oldBusiness)) {
                oldBusinessMD = JSONUtil.toBean(oldBusiness, BusinessMD.class);
            }
            if (!"".equals(newBusiness)) {
                newBusinessMD = JSONUtil.toBean(newBusiness, BusinessMD.class);
            }
            oldBusinessMD.setId(oldObj.getId());
            List<BugLogDO> bugLogList = FieldCompareUtil.commonCompare(oldBusinessMD, newBusinessMD, BugLogDO.class);
            bugLogDOList.addAll(bugLogList);
        }

        return bugLogDOList;
    }
}



















