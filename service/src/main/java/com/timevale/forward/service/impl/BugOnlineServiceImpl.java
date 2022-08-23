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
import com.timevale.forward.model.bo.BusinessBO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.model.middle.BugOnlineMD;
import com.timevale.forward.model.middle.BusinessMD;
import com.timevale.forward.service.component.*;
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
import org.apache.commons.lang3.StringUtils;
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
    private BugOnlineModelMapper bugOnlineModelMapper;

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

    @Resource
    private ModelMapper modelMapper;

    @Value("${business}")
    private String business;

    @Resource
    private SqlOrderComponent sqlOrderComponent;

    /**
     * 默认经办人,来自运营支撑提报bug
     */
    @Value("${default.operator:shifeng;释沣-余文杰}")
    private String defaultOperator;

    @Resource
    private BugOnlineProductLineComponent bugOnlineProductLineComponent;

    @Resource
    private BugOnlineModelComponent bugOnlineModelComponent;

    @Resource
    private LabelComponent labelComponent;

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private LabelMapper labelMapper;

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Override
    public BusinessResult<ProductLineToFieldVO> getAllDisplayField(BugOnlineGetFieldReq bugOnlineGetFieldReq) {
        log.info("线上bug-从配置中心获取信息，接收参数：{}", bugOnlineGetFieldReq.getProductLineIdList());

        List<BusinessBO> businessBeanList = JSON.parseArray(business, BusinessBO.class);
        List<Long> productLineIdList = bugOnlineGetFieldReq.getProductLineIdList();
        Map<Integer, String> fieldMap = getFieldMap();
        ProductLineToFieldVO productLineToFieldVO = new ProductLineToFieldVO();
        List<String> fieldList = new ArrayList<>();
        productLineIdList.forEach(productLineId -> {
            for (int i = 0; i < businessBeanList.size(); i++) {
                BusinessBO businessBO = businessBeanList.get(i);
                if (businessBO.getFieldValue().contains(productLineId)) {
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

        //是否打标
        List<BizLabelDO> bizLabelDOList;
        if (CollectionUtils.isNotEmpty(bugOnlineQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(bugOnlineQueryList.getLabelCategoryIds())) {
            List<Long> newLabelIds = labelComponent.getLabelIds(bugOnlineQueryList.getLabelIds(), bugOnlineQueryList.getLabelCategoryIds());
            if (CollectionUtils.isEmpty(newLabelIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.BUG_ONLINE.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(bizIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setContainIds(bizIds);
        }
        // 开始分页
        String collation = sqlOrderComponent.build(bugOnlineQueryList.getOrderFiled(), bugOnlineQueryList.getOrderCollation());
        PageHelper.startPage(bugOnlineQueryList.pageNum, bugOnlineQueryList.pageSize, collation);

        // 查询并转换
        List<BugOnlineListDO> bugOnlineDOList = bugOnlineMapper.selectListByCondition(condition);
        List<BugOnlineVO> bugOnlineVOList = bugOnlineDOList.stream().map(BugOnlineCopier.INSTANCE::convert).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(bugOnlineVOList)) {
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        //标签
        List<Long> bugOnlineIds = bugOnlineDOList.stream().map(BugOnlineListDO::getId).collect(Collectors.toList());
        bizLabelDOList = bizLabelMapper.getByBizIdInType(bugOnlineIds, BizTypeEnum.BUG_ONLINE.getCode());
        Map<Long, List<Long>> labelIdMap = bizLabelDOList.stream().collect(Collectors.groupingBy(BizLabelDO::getBizId
                , Collectors.mapping(BizLabelDO::getLabelId, Collectors.toList())));

        List<Long> labelIds = bizLabelDOList.stream().map(BizLabelDO::getLabelId).collect(Collectors.toList());
        Map<Long, String> labelNameMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(labelIds)) {
            List<LabelDO> labelDOList = labelMapper.getByIds(labelIds);
            labelNameMap = labelDOList.stream().collect(Collectors.toMap(LabelDO::getId, LabelDO::getName, (v1, v2) -> v2));
        }

        // 查询对应产品线和业务域
        List<Long> bugOnlineIdList = bugOnlineVOList.stream().map(BugOnlineVO::getId).collect(Collectors.toList());
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = bugOnlineProductLineMapper.selectByBugOnlineIdList(bugOnlineIdList);


        List<Long> productLineIdList = bugOnlineProductLineDOList.stream().map(BugOnlineProductLineDO::getProductLineId).collect(Collectors.toList());
        List<ProductLineDO> productLineDOList = productLineMapper.selectByIds(productLineIdList);

        List<Long> bizDomainIdList = productLineDOList.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toList());
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectByIdList(bizDomainIdList);

        List<BugOnlineModelDO> bugOnlineModelDOList = bugOnlineModelMapper.selectByBugOnlineIdList(bugOnlineIdList);
        List<Long> modelIdList = bugOnlineModelDOList.stream().map(BugOnlineModelDO::getModelId).collect(Collectors.toList());
        List<ModelDO> modelDOList = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(modelIdList)) {
            modelDOList = modelMapper.getByIds(modelIdList);
        }

        Map<Long, ProductLineDO> productLineMap = productLineDOList.stream().collect(Collectors.toMap(ProductLineDO::getId, Function.identity()));
        Map<Long, BizDomainDO> bizDomainDOMap = bizDomainDOList.stream().collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));
        Map<Long, List<BugOnlineProductLineDO>> bugOnlineProductLineMap =
                bugOnlineProductLineDOList.stream().collect(Collectors.groupingBy(BugOnlineProductLineDO::getBugOnlineId));

        Map<Long, List<BugOnlineModelDO>> bugOnlineModelMap =
                bugOnlineModelDOList.stream().collect(Collectors.groupingBy(BugOnlineModelDO::getBugOnlineId));
        Map<Long, String> modelNameMap = modelDOList.stream().collect(Collectors.toMap(ModelDO::getId, ModelDO::getName, (v1, v2) -> v2));

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

            // 关联的模块id
            if (bugOnlineModelMap.containsKey(e.getId())) {
                List<Long> eModelIdList = bugOnlineModelMap.get(e.getId())
                        .stream()
                        .map(BugOnlineModelDO::getModelId)
                        .collect(Collectors.toList());
                List<String> modelNames = eModelIdList.stream().filter(modelNameMap::containsKey).map(modelNameMap::get).collect(Collectors.toList());
                e.setModelNames(modelNames);

            }
            e.setProductLineNameList(eProductLineNameList);
            e.setBizDomainNameList(eBizDomainNameList);

            e.setEnvName(BugOnlineEnvEnum.getTextByCode(e.getEnv()));
            e.setStatusName(BugOnlineStatusEnum.getTextByCode(e.getStatus()));
            e.setBelongName(BugOnlineBeloneEnum.getTextByCode(e.getBelong()));
            e.setReasonName(BugOnlineReasonEnum.getTextByCode(e.getReason()));
            e.setSourceName(BugOnlineSourceEnum.getTextByCode(e.getSource()));
            e.setPriorityName(BugOnlinePriorityEnum.getTextByCode(e.getPriority()));
            e.setDismissCauseName(BugOnlineDismissCauseEnum.getTextByCode(e.getDismissCause()));

            if (labelIdMap.containsKey(e.getId())) {
                List<Long> labelIdList = labelIdMap.get(e.getId());
                List<String> labelNames = labelIdList.stream().filter(labelNameMap::containsKey).map(labelNameMap::get).collect(Collectors.toList());
                e.setLabelNames(labelNames);
            }
        }

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

        if (bugOnlineAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("线上bug名称中请勿包含空格");
        }

        if (Objects.equals(bugOnlineAddReq.getSource(), "support")) {
            log.info("默认经办人:{}", defaultOperator);
            String[] defaultOperators = defaultOperator.split(";");
            bugOnlineAddReq.setOperatorId(defaultOperators[0]);
            bugOnlineAddReq.setOperator(defaultOperators[1]);
        }
        //将BugOnlineAddReq转化为BugOnlineDO
        BugOnlineDO bugOnlineDO = BugOnlineCopier.INSTANCE.transfer(bugOnlineAddReq);

        bugOnlineMapper.insert(bugOnlineDO);

        bugOnlineProductLineComponent.add(bugOnlineAddReq.getProductLineIdList(), bugOnlineDO.getId());

        bugOnlineModelComponent.add(bugOnlineAddReq.getModelIds(), bugOnlineDO.getId());

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
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineAddMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority()),
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

        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }
        BugOnlineDO update = new BugOnlineDO();
        update.setId(bugOnlineReq.getId());
        update.setIsDeleted(true);

        //删除线上bug
        bugOnlineMapper.update(update);

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
        deleteLinkBug(bugOnlineDO.getId(),bugOnlineDO.getLinkBugId(),true);
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<String> modify(BugOnlineModifyReq bugOnlineModifyReq) {
        log.info("线上bug-修改,接收参数：{}", bugOnlineModifyReq);

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineModifyReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("线上bug不存在");
        }

        //老的线上bug比较对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        //保存老的产品线列表
        List<Long> oldProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(bugOnlineModifyReq.getId());

        List<Long> oldModelList = bugOnlineModelMapper.selectModelIds(bugOnlineModifyReq.getId());

        BugOnlineDO bugOnlineConvert = BugOnlineCopier.INSTANCE.change(bugOnlineModifyReq);
        //更新线上bug
        bugOnlineMapper.update(bugOnlineConvert);

        //更新附件表
        List<FileAddReq> files = bugOnlineModifyReq.getFiles();
        fileComponent.update(files, bugOnlineModifyReq.getId(), FileTypeEnum.BUG_ONLINE.getCode());

        //更新抄送人表
        List<PersonAddReq> recipients = bugOnlineModifyReq.getRecipients();
        personComponent.update(recipients, bugOnlineModifyReq.getId(), PersonTypeEnum.BUG_ONLINE_CC.getCode());

        bugOnlineProductLineComponent.update(bugOnlineModifyReq.getProductLineIdList(), bugOnlineModifyReq.getId());

        bugOnlineModelComponent.update(bugOnlineModifyReq.getModelIds(), bugOnlineModifyReq.getId());

        //新的线上bug比较对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.convert(bugOnlineModifyReq);
        BugOnlineDO newBugOnlineDO = BugOnlineCopier.INSTANCE.change(bugOnlineModifyReq);

        //比较编辑修改的一般字段，生成结果集合
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);

        //模块日志
        bugLogDOList.addAll(compareModel(oldModelList, bugOnlineModifyReq.getModelIds(), bugOnlineDO.getId()));
        bugLogDOList.addAll(compareProductLine(oldProductLineIdList, bugOnlineModifyReq.getProductLineIdList(), bugOnlineDO.getId()));
        bugLogDOList.addAll(compareExtField(bugOnlineDO, newBugOnlineDO));
        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //如果经办人变了，但是状态没有变化，需要往状态人员处理表中插入一条数据，并且需要发送钉钉消息
        if (!bugOnlineDO.getOperatorId().equals(newBugOnlineDO.getOperatorId())) {
            //往bug状态人员处理表中插入一条记录
            insertToBugStatusOperator(bugOnlineDO.getId(), newBugOnlineDO.getOperatorId(), newBugOnlineDO.getOperator());

            //发送钉钉消息
            messageEventPublisher.publish(
                    new BugOnlineModifyMsgEvent(
                            this,
                            bugOnlineModifyReq.getName(),
                            BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                            bugOnlineModifyReq.getOperatorId(),
                            bugOnlineDO.getId(),
                            BugOnlinePriorityEnum.getTextByCode(bugOnlineModifyReq.getPriority())
                    )
            );
        }
        String tips = updateLinkBug(bugOnlineModifyReq.getId(), bugOnlineModifyReq.getLinkBugId());
        BusinessResult<String> businessResult = new BusinessResult<>();
        businessResult.setData(tips);
        return businessResult;
    }

    @Override
    public BusinessResult<BugOnlineDetailVO> get(BugOnlineDetailReq bugOnlineDetailReq) {
        log.info("线上bug-得到线上bug详情，接收参数:{}", bugOnlineDetailReq);

        //查询线上bug
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(bugOnlineDetailReq.getId());
        if (bugOnlineDO == null) {
            throw new BaseBizRuntimeException("该线上bug不存在");
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
            BizDemandVO bizDemandVO = BizDemandCopier.INSTANCE.transfer(bizDemandDO);
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
        //模块名称
        List<Long> modelIdList = bugOnlineModelMapper.selectModelIds(bugOnlineDetailReq.getId());
        if (!CollectionUtils.isEmpty(modelIdList)) {
            String modelName = modelMapper.getByIds(modelIdList).stream().map(ModelDO::getName).collect(Collectors.joining(","));
            bugOnlineDetailVO.setModelName(modelName);
            bugOnlineDetailVO.setModelIds(modelIdList);
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
        bugOnlineDetailVO.setSourceName(BugOnlineSourceEnum.getTextByCode(bugOnlineDO.getSource()));
        bugOnlineDetailVO.setBelongName(BugOnlineBeloneEnum.getTextByCode(bugOnlineDO.getBelong()));
        bugOnlineDetailVO.setPriorityName(BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority()));
        if (bugOnlineDO.getReason() != null) {
            bugOnlineDetailVO.setReasonName(BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason()));
        }
        bugOnlineDetailVO.setRecurrentName(BugOnlineRecurrentEnum.getTextByCode(bugOnlineDO.getRecurrent()));

        //关联的bug/被关联的bug
        if(bugOnlineDO.getLinkBugId()!=null){
            BugOnlineDO linkBug = bugOnlineMapper.selectById(bugOnlineDO.getLinkBugId());
            BugOnlineLinkVO bugOnlineLinkVO=new BugOnlineLinkVO();
            bugOnlineLinkVO.setId(linkBug.getId());
            bugOnlineLinkVO.setName(linkBug.getName());
            bugOnlineDetailVO.setLinkBug(bugOnlineLinkVO);
        }else{
            List<BugOnlineDO> linkedBug = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
            List<BugOnlineLinkVO> linkedBugs = linkedBug.stream().map(a -> {
                BugOnlineLinkVO o = new BugOnlineLinkVO();
                o.setId(a.getId());
                o.setName(a.getName());
                return o;
            }).collect(Collectors.toList());
            bugOnlineDetailVO.setLinkedBugs(linkedBugs);
        }
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
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

        //得到老的对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_REPAIR.getCode());
        bugOnlineDO.setReason(bugOnlineStartRepairReq.getReason());
        bugOnlineDO.setProblemReason(bugOnlineStartRepairReq.getProblemReason());
        bugOnlineDO.setSolveScheme(bugOnlineStartRepairReq.getSolveScheme());
        bugOnlineDO.setExpectLaunchDate(bugOnlineStartRepairReq.getExpectLaunchDate());
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

        //得到新的对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        //比较内容是否变化,有变化则插入内容变更记录
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);

        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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
        bugOnlineDO.setRepairFailReason("");
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
        if (repairFailReason != null && !"".equals(repairFailReason)) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
            bugLog.setOldValue(repairFailReason);
            bugLog.setMainId(bugOnlineRepairFinishedReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.OPERATOR.getText());
            bugLog.setOldValue(operator);
            bugLog.setNewValue(bugOnlineDO.getOperator());
            bugLog.setMainId(bugOnlineRepairFinishedReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

        //如果前端传递的有bug原因，那么就存放一条内容记录
        if (bugOnlineConfirmRepairReq.getReason() != null) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.REASON.getText());
            bugLog.setNewValue(BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason()));
            bugLog.setMainId(bugOnlineConfirmRepairReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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
        if (!jobFunctionResult && !operatorResult && !proposerResult) {
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

        //如果前端传递的有bug原因，那么就存放一条内容记录
        if (bugOnlineOnlineReq.getReason() != null) {
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.REASON.getText());
            bugLog.setNewValue(BugOnlineReasonEnum.getTextByCode(bugOnlineDO.getReason()));
            bugLog.setMainId(bugOnlineOnlineReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bugLog);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

        //发送消息
        messageEventPublisher.publish(
                new BugOnlineOnlineMsgEvent(
                        this,
                        bugOnlineDO.getName(),
                        bugOnlineDO.getProposerId(),
                        bugOnlineOnlineReq.getId()
                )
        );

        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
        bugOnlineDOList.forEach(a->{
            messageEventPublisher.publish(
                    new BugOnlineResubmitOnlineMsgEvent(
                            this,
                            bugOnlineDO.getName(),
                            a.getProposerId(),
                            bugOnlineDO.getId()
                    )
            );
        });
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

        //得到老的对象
        BugOnlineMD oldBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        if (bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.CLOSE.getCode())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        }
        if (bugOnlineDO.getStatus().equals(BugOnlineStatusEnum.COMPLETE.getCode())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        }
        bugOnlineDO.setLastOperatorId(operatorId);
        bugOnlineDO.setLastOperator(operator);
        bugOnlineDO.setOperatorId(lastOperatorId);
        bugOnlineDO.setOperator(lastOperator);
        bugOnlineDO.setDismissCause(null);
        bugOnlineDO.setHangUp(false);
        bugOnlineDO.setOpenAgainReason(bugOnlineOpenAgainReq.getOpenAgainReason());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.OPEN_AGAIN.getText());
        bugLogDO.setOldValue(oldStatus);
        if (oldStatus.equals(BugOnlineStatusEnum.CLOSE.getText())) {
            bugLogDO.setNewValue(BugOnlineStatusEnum.PROBLEM_REPORT.getText());
        }
        if (oldStatus.equals(BugOnlineStatusEnum.COMPLETE.getText())) {
            bugLogDO.setNewValue(BugOnlineStatusEnum.QUESTION_CONFIRM.getText());
        }
        bugLogDO.setMainId(bugOnlineOpenAgainReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //得到新的对象
        BugOnlineMD newBugOnlineMD = BugOnlineCopier.INSTANCE.change(bugOnlineDO);

        //比较内容是否变化,有变化则插入内容变更记录
        List<BugLogDO> bugLogDOList = FieldCompareUtil.commonCompare(oldBugOnlineMD, newBugOnlineMD, BugLogDO.class);

        if (!CollectionUtils.isEmpty(bugLogDOList)) {
            bugLogMapper.batchInsert(bugLogDOList);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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
        deleteLinkBug(bugOnlineDO.getId(),bugOnlineDO.getLinkBugId(),false);
        BusinessResult<Boolean> businessResult = new BusinessResult<>();
        businessResult.setData(true);
        return businessResult;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BusinessResult<String> noRepair(BugOnlineNoRepairReq bugOnlineNoRepairReq) {
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
        //保存老的经办人
        String operator = bugOnlineDO.getOperator();
        //保存老的修复失败原因
        String oldRepairFailReason = bugOnlineDO.getRepairFailReason();

        bugOnlineDO.setStatus(BugOnlineStatusEnum.BE_CONFIRM.getCode());
        bugOnlineDO.setLastOperatorId(bugOnlineDO.getOperatorId());
        bugOnlineDO.setLastOperator(bugOnlineDO.getOperator());
        bugOnlineDO.setOperatorId(bugOnlineDO.getProposerId());
        bugOnlineDO.setOperator(bugOnlineDO.getProposer());
        bugOnlineDO.setDismissCause(bugOnlineNoRepairReq.getDismissCause());
        if (bugOnlineDO.getRepairFailReason() != null && !"".equals(bugOnlineDO.getRepairFailReason())) {
            bugOnlineDO.setRepairFailReason("");
        }
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

        //如果修复失败原因有值还需要记录一条日志内容记录
        if (oldRepairFailReason != null && !"".equals(oldRepairFailReason)) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.REPAIR_FAIL_REASON.getText());
            bug.setOldValue(oldRepairFailReason);
            bug.setMainId(bugOnlineNoRepairReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //往bug日志表中插入一条线上bug内容变更数据
            bugLogMapper.insert(bug);
        }

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.OPERATOR.getText());
            bug.setOldValue(operator);
            bug.setNewValue(bugOnlineDO.getOperator());
            bug.setMainId(bugOnlineNoRepairReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bug);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(bugOnlineDO.getId());
        bugOnlineDOList.forEach(a->{
            messageEventPublisher.publish(
                    new BugOnlineResubmitNoRepairMsgEvent(
                            this,
                            bugOnlineDO.getName(),
                            a.getProposerId(),
                            bugOnlineDO.getId()
                    )
            );
        });

        String tips = updateLinkBug(bugOnlineNoRepairReq.getId(), bugOnlineNoRepairReq.getLinkBugId());

        BusinessResult<String> businessResult = new BusinessResult<>();
        businessResult.setData(tips);
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

        bugOnlineDO.setOperatorId(bugOnlineTransferReq.getUserId());
        bugOnlineDO.setOperator(bugOnlineTransferReq.getUserName());
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        //如果不是自己转交给自己
        if (!oldOperator.equals(bugOnlineTransferReq.getUserName())) {
            insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

            //往bug日志表中插入一条线上bug内容变更数据
            BugLogDO bugLog = new BugLogDO();
            bugLog.setField(BugFieldEnum.OPERATOR.getText());
            bugLog.setOldValue(oldOperator);
            bugLog.setNewValue(bugOnlineTransferReq.getUserName());
            bugLog.setMainId(bugOnlineTransferReq.getId());
            bugLog.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogMapper.insert(bugLog);

            //发送消息
            messageEventPublisher.publish(
                    new BugOnlineTransferMsgEvent(
                            this,
                            userInfo.getAlias() + "-" + userInfo.getName(),
                            bugOnlineDO.getName(),
                            BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()),
                            bugOnlineTransferReq.getUserId(),
                            bugOnlineDO.getId(),
                            BugOnlinePriorityEnum.getTextByCode(bugOnlineDO.getPriority())
                    )
            );
        }

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

        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.OPERATOR.getText());
            bug.setOldValue(operator);
            bug.setNewValue(bugOnlineDO.getOperator());
            bug.setMainId(bugOnlineReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bug);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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
        deleteLinkBug(bugOnlineDO.getId(),bugOnlineDO.getLinkBugId(),false);
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
        if (oldStatus.equals(BugOnlineStatusEnum.QUESTION_REPAIR.getText())) {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.QUESTION_CONFIRM.getCode());
        } else {
            bugOnlineDO.setStatus(BugOnlineStatusEnum.PROBLEM_REPORT.getCode());
        }

        bugOnlineDO.setHangUp(false);
        //线上bug表更新
        bugOnlineMapper.update(bugOnlineDO);

        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(ButtonActionEnum.REPEAT_CONFIRM.getText());
        bugLogDO.setOldValue(oldStatus);
        bugLogDO.setNewValue(BugOnlineStatusEnum.getTextByCode(bugOnlineDO.getStatus()));
        bugLogDO.setMainId(bugOnlineReq.getId());
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setField(BugLogFieldEnum.STATUS.getText());
        //往bug日志表中插入一条线上bug状态变更数据
        bugLogMapper.insert(bugLogDO);

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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
        bugOnlineDO.setHangUp(true);
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
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

        //如果经办人变了，则添加一条内容变更记录
        if (!operator.equals(bugOnlineDO.getOperator())) {
            BugLogDO bug = new BugLogDO();
            bug.setField(BugFieldEnum.OPERATOR.getText());
            bug.setOldValue(operator);
            bug.setNewValue(bugOnlineDO.getOperator());
            bug.setMainId(bugOnlineRepairFailedReasonReq.getId());
            bug.setType(BugLogTypeEnum.ONLINE.getCode());
            //插入bug日志内容变更记录
            bugLogMapper.insert(bug);
        }

        //bug状态处理人员表插入数据
        insertToBugStatusOperator(bugOnlineDO.getId(), bugOnlineDO.getOperatorId(), bugOnlineDO.getOperator());

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

    @Override
    public BaseResult<List<BugOnlineVO>> getByName(BugOnlineGetReq bugOnlineGetReq) {
        List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByName(bugOnlineGetReq.getLinkBugName());
        List<BugOnlineDO> filter = bugOnlineDOList.stream()
                .filter(a -> !Objects.equals(a.getId(), bugOnlineGetReq.getId())&&!Objects.equals(a.getLinkBugId(), bugOnlineGetReq.getId()))
                .collect(Collectors.toList());
        if(CollectionUtils.isEmpty(filter)){
           return BaseResult.success(Lists.emptyList());
        }
        List<BugOnlineVO> result = filter.stream().map(BugOnlineCopier.INSTANCE::convertT).collect(Collectors.toList());
        return BaseResult.success(result);
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
    public void insertToBugStatusOperator(Long bugOnlineId, String userId, String userName) {
        //查询当前线上bug对应的所有状态变更记录
        List<BugLogDO> bugLogDOS = bugLogMapper.selectByBugOfflineIdAndType(bugOnlineId
                , BugLogTypeEnum.ONLINE.getCode(), true);

        //按创建时间逆序排列，筛选出最后一条状态变更记录
        List<BugLogDO> collect = bugLogDOS.stream().filter(a->BugLogFieldEnum.STATUS.getText().equals(a.getField()))
                .sorted(Comparator.comparing(BugLogDO::getCreateDate).reversed()).collect(Collectors.toList());
        BugLogDO lastStatusBugLogDO = collect.get(0);

        BugStatusOperatorDO bugStatusOperatorDO = new BugStatusOperatorDO();
        bugStatusOperatorDO.setBugLogId(lastStatusBugLogDO.getId());
        bugStatusOperatorDO.setOperator(userName);
        bugStatusOperatorDO.setOperatorId(userId);
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
    private List<BugLogDO> compareExtField(BugOnlineDO oldObj, BugOnlineDO newObj) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        String oldBusiness = oldObj.getBusiness();
        String newBusiness = newObj.getBusiness();
        //如果产品线业务这个json字符串变了，要记录一条或多条内容变更日志
        if (!Objects.equals(oldBusiness, newBusiness)) {
            BusinessMD oldBusinessMD = StringUtils.isEmpty(oldBusiness) ? new BusinessMD() : JSONUtil.toBean(oldBusiness, BusinessMD.class);
            BusinessMD newBusinessMD = StringUtils.isEmpty(newBusiness) ? new BusinessMD() : JSONUtil.toBean(newBusiness, BusinessMD.class);
            oldBusinessMD.setId(oldObj.getId());
            List<BugLogDO> bugLogList = FieldCompareUtil.commonCompare(oldBusinessMD, newBusinessMD, BugLogDO.class);
            bugLogDOList.addAll(bugLogList);
        }
        return bugLogDOList;
    }


    private List<BugLogDO> compareProductLine(List<Long> oldProductLineIdList, List<Long> newProductLineIdList, Long bugOnlineId) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        boolean result = CollectionUtils.isEqualCollection(oldProductLineIdList, newProductLineIdList);
        //如果产品线变了记录一条bug内容变更日志
        if (!result) {
            List<Long> mergeProductLineIds = new ArrayList<>();
            mergeProductLineIds.addAll(oldProductLineIdList);
            mergeProductLineIds.addAll(newProductLineIdList);
            Map<Long, String> mergeProductLines = productLineMapper.selectByIds(mergeProductLineIds).stream()
                    .collect(Collectors.toMap(ProductLineDO::getId, ProductLineDO::getName, (v1, v2) -> v2));
            String oldValue = oldProductLineIdList.stream().map(mergeProductLines::get).collect(Collectors.joining(","));
            String newValue = newProductLineIdList.stream().map(mergeProductLines::get).collect(Collectors.joining(","));
            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PRODUCT_LINE.getText());
            bugLogDO.setOldValue(oldValue);
            bugLogDO.setNewValue(newValue);
            bugLogDO.setMainId(bugOnlineId);
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }
        return bugLogDOList;
    }

    private List<BugLogDO> compareModel(List<Long> oldModelIds, List<Long> newModelIds, Long bugOnlineId) {
        oldModelIds = CollectionUtils.isEmpty(oldModelIds) ? new ArrayList<>() : oldModelIds;
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        boolean result = CollectionUtils.isEqualCollection(oldModelIds, newModelIds);
        if (!result) {
            List<Long> mergeModelIds = new ArrayList<>();
            mergeModelIds.addAll(oldModelIds);
            mergeModelIds.addAll(newModelIds);
            Map<Long, String> mergeModels = modelMapper.getByIds(mergeModelIds).stream()
                    .collect(Collectors.toMap(ModelDO::getId, ModelDO::getName, (v1, v2) -> v2));
            String oldValue = oldModelIds.stream().map(mergeModels::get).collect(Collectors.joining(","));
            String newValue = newModelIds.stream().map(mergeModels::get).collect(Collectors.joining(","));
            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.MODEL.getText());
            bugLogDO.setOldValue(oldValue);
            bugLogDO.setNewValue(newValue);
            bugLogDO.setMainId(bugOnlineId);
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }
        return bugLogDOList;
    }

    private String updateLinkBug(Long id,Long linkBugId){
        BugOnlineDO bugOnlineDO = bugOnlineMapper.selectById(id);
        Long oldLinkBugId = bugOnlineDO.getLinkBugId();
        Long finalBugId = null;
        String tips=StringUtils.EMPTY;
        if(linkBugId!=null){
            List<BugLogDO> bugLogDOList = new ArrayList<>();
            //查找哪些bug关联了当前bug,要将这些bug,重新关联到新的bug上
            List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(id);
            List<Long> updateIdA = bugOnlineDOList.stream().map(BugOnlineDO::getId).collect(Collectors.toList());
            updateIdA.add(id);

            BugOnlineDO linkBug = bugOnlineMapper.selectById(linkBugId);
            finalBugId=linkBug.getId();
            if (linkBug.getLinkBugId() != null) {
                //要关联的bug B可能有关联的bug C   最终取C
                finalBugId=linkBug.getLinkBugId();
                tips="若关联的bug已关联其他bug，会显示父级bug名称";
            }
            if(updateIdA.contains(finalBugId)){
                throw new BaseBizRuntimeException("关联的bug或其上级bug与当前bug相同,请修改后重试");
            }
            bugOnlineMapper.updateByIds(updateIdA,finalBugId);

            updateIdA.remove(id);
            //处理bug-a: a->关联了当前bugA,现关联finalBugId
            for (Long a : updateIdA) {
                //a删除当前bugA
                bugLogDOList.add(createBugLog(a,a,id,ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(id,a,id,ButtonActionEnum.UN_LINK.getText()));
                //a关联finalBugId
                bugLogDOList.add(createBugLog(a,a,finalBugId,ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId,a,finalBugId,ButtonActionEnum.LINK.getText()));
            }
            if(CollectionUtils.isNotEmpty(bugLogDOList)){
                bugLogMapper.batchInsert(bugLogDOList);
            }
        }else{
            bugOnlineMapper.updateByIds(Lists.newArrayList(id),null);
        }
        log.info("更新关联bug,id:{},oldLinkBugId:{},linkBugId:{},finalBugId:{}",id,oldLinkBugId,linkBugId,finalBugId);
        if (!Objects.equals(oldLinkBugId, finalBugId)) {
            //处理当前bug
            List<BugLogDO> bugLogDOList = new ArrayList<>();
            if(oldLinkBugId!=null&&finalBugId==null){
                //由A->B 变成 A->无
                bugLogDOList.add(createBugLog(id,id,oldLinkBugId,ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(oldLinkBugId,id,oldLinkBugId,ButtonActionEnum.UN_LINK.getText()));
            } else if(oldLinkBugId==null&&finalBugId!=null){
                //由A->无 变成 A->B
                bugLogDOList.add(createBugLog(id,id,finalBugId,ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId,id,finalBugId,ButtonActionEnum.LINK.getText()));
            }else if(oldLinkBugId!=null&&finalBugId!=null){
                //由A->B 变成 A->finalBugId
                //删除老的
                bugLogDOList.add(createBugLog(id,id,oldLinkBugId,ButtonActionEnum.UN_LINK.getText()));
                bugLogDOList.add(createBugLog(oldLinkBugId,id,oldLinkBugId,ButtonActionEnum.UN_LINK.getText()));

                bugLogDOList.add(createBugLog(id,id,finalBugId,ButtonActionEnum.LINK.getText()));
                bugLogDOList.add(createBugLog(finalBugId,id,finalBugId,ButtonActionEnum.LINK.getText()));
            }
            bugLogMapper.batchInsert(bugLogDOList);
        }
        return tips;
    }

    private void deleteLinkBug(Long id,Long linkBugId,boolean deleteLinked){
        //删除关联
        log.info("删除关联bug,id:{},linkBugId:{},deleteLinked:{},",id,linkBugId,deleteLinked);
        List<BugLogDO> bugLogDOList = new ArrayList<>();
        if(linkBugId!=null){
            bugOnlineMapper.updateByIds(Lists.newArrayList(id),null);
            bugLogDOList.add(createBugLog(id,id,linkBugId,ButtonActionEnum.UN_LINK.getText()));
            bugLogDOList.add(createBugLog(linkBugId,id,linkBugId,ButtonActionEnum.UN_LINK.getText()));
        }
        if(deleteLinked){
            List<BugOnlineDO> bugOnlineDOList = bugOnlineMapper.selectByLinkBugId(id);
            bugOnlineDOList.forEach(a->{
                bugLogDOList.add(createBugLog(a.getId(),a.getId(),id,ButtonActionEnum.UN_LINK.getText()));
            });
            List<Long> updateIds = bugOnlineDOList.stream().map(BugOnlineDO::getId).collect(Collectors.toList());
            if(CollectionUtils.isNotEmpty(updateIds)){
                bugOnlineMapper.updateByIds(updateIds,null);
            }
        }
        if(CollectionUtils.isNotEmpty(bugLogDOList)){
            bugLogMapper.batchInsert(bugLogDOList);
        }
    }

    private BugLogDO createBugLog(Long mainId,Long oldValue,Long newValue,String action){
        BugLogDO bugLogDO = new BugLogDO();
        bugLogDO.setAction(action);
        bugLogDO.setField(BugFieldEnum.LINK_BUG.getText());
        bugLogDO.setMainId(mainId);
        bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
        bugLogDO.setOldValue(String.valueOf(oldValue));
        bugLogDO.setNewValue(String.valueOf(newValue));
        return  bugLogDO;
    }
}





















