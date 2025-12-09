package com.timevale.forward.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Lists;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.CustomDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.condition.ProductCustomDemandCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandTrackEventCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.BizLabelMapper;
import com.timevale.forward.dal.dao.CustomDemandMapper;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductCustomDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandTrackEventMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.BizDemandDO;
import com.timevale.forward.dal.entity.BizDemandListDO;
import com.timevale.forward.dal.entity.BizLabelDO;
import com.timevale.forward.dal.entity.CustomDemandDO;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProductCustomDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.dal.entity.ProductDemandTrackEventDO;
import com.timevale.forward.dal.entity.ProjectDO;
import com.timevale.forward.dal.entity.ProjectProductDemandDO;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.client.ProductDemandGroupService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.client.UseCasePlatFormCallService;
import com.timevale.forward.facade.api.query.ProductBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductCustomDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkTrackEventQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandTrackEventQueryList;
import com.timevale.forward.facade.api.query.ProductLinkCustomDemandQueryList;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.CustomDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandStatusVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProductLineAnalyseVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.facade.api.result.QueryResultVO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.model.enums.ButtonActionEnum;
import com.timevale.forward.model.enums.CustomerGradeEnum;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.ForwardFlowStatusEnum;
import com.timevale.forward.model.enums.LinkOrUnLinkEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.PriorityEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizLabelComponent;
import com.timevale.forward.service.component.CustomDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.LabelComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductBizDemandComponent;
import com.timevale.forward.service.component.ProductCustomDemandComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.component.ProductDemandLogComponent;
import com.timevale.forward.service.component.ProductDemandTrackEventComponent;
import com.timevale.forward.service.component.ProjectComponent;
import com.timevale.forward.service.component.ProjectLogComponent;
import com.timevale.forward.service.component.ProjectProductDemandComponent;
import com.timevale.forward.service.component.TaskProductDemandComponent;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.component.impl.ProductDemandDescFlowComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.CustomDemandCopier;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.integration.crm.CrmClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.observer.event.ProductDemandBatchTransferMsgEvent;
import com.timevale.forward.service.observer.event.ProductDemandToCopiedMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.duplicate.GroupDuplicateUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.base.util.AssertUtil;
import com.timevale.mandarin.base.util.StringUtils;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class ProductDemandServiceImpl implements ProductDemandService {

    @Resource
    private FileComponent fileComponent;

    @Resource
    private PersonComponent personComponent;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProductDemandComponent productDemandComponent;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private ProductBizDemandComponent productBizDemandComponent;

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;

    @Resource
    private ProductDemandLogComponent productDemandLogComponent;

    @Resource
    private ProjectLogComponent projectLogComponent;

    @Resource
    private ProductDemandTrackEventMapper productDemandTrackEventMapper;

    @Resource
    private TrackEventComponent trackEventComponent;

    @Resource
    private ProductDemandTrackEventComponent productDemandTrackEventComponent;

    @Resource
    private TrackEventMapper trackEventMapper;

    @Resource
    private ProductDemandDescFlowComponent productDemandDescFlowComponent;

    @Resource
    private CustomDemandComponent customDemandComponent;

    @Resource
    private CustomDemandMapper customDemandMapper;

    @Resource
    private ProductCustomDemandMapper productCustomDemandMapper;

    @Resource
    private ProductCustomDemandComponent productCustomDemandComponent;

    @Resource
    private LabelComponent labelComponent;

    @Resource
    private BizLabelMapper bizLabelMapper;

    @Resource
    private BizLabelComponent bizLabelComponent;

    @Resource
    private ProjectComponent projectComponent;

    @Resource
    private GroupDuplicateUtil groupDuplicateUtil;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private CrmClient crmClient;

    @Resource
    private UseCasePlatFormCallService useCasePlatFormCallService;

    @Resource
    private ProductDemandGroupService productDemandGroupService;

    private static final String ONE_HUNDRED_PERCENT = "100.00%";

    @Override
    public BaseResult<QueryResultVO<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求接收参数:{}", productDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        if (groupDuplicateUtil.setOwnerIdByAscription(productDemandQueryList, userInfo, condition, innerUserPersonClient)) {
            return BaseResult.success(ResultUtil.queryResultEmpty());
        }

        //是否打标
        if (CollectionUtils.isNotEmpty(productDemandQueryList.getLabelIds()) || CollectionUtils.isNotEmpty(productDemandQueryList.getLabelCategoryIds())) {
            Boolean containLabel = productDemandQueryList.getContainLabel();

            List<Long> newLabelIds = labelComponent.getLabelIds(productDemandQueryList.getLabelIds(), productDemandQueryList.getLabelCategoryIds());

            // 查询包含且类别下没有标签
            if (CollectionUtils.isEmpty(newLabelIds) && containLabel) {
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }

            // 查询使用这些标签的需求id
            List<BizLabelDO> bizLabelDOList = bizLabelMapper.getByLabelIdInType(newLabelIds, BizTypeEnum.PRODUCT_DEMAND.getCode());
            List<Long> bizIds = bizLabelDOList.stream().map(BizLabelDO::getBizId).collect(Collectors.toList());

            if (containLabel) {
                if (CollectionUtils.isEmpty(bizIds)) {
                    return BaseResult.success(ResultUtil.queryResultEmpty());
                }
                condition.setInProductDemandIds(bizIds);
            } else {
                condition.setNotInProductDemandIds(bizIds);
            }

        }

        // 完整查询
        List<ProductDemandListDO> allProductDemandListDO = productDemandComponent.list(ProductDemandCopier.INSTANCE.convert(condition));
        Map<Long, List<ProductDemandListDO>> bizDemandListDOMap = allProductDemandListDO.stream().collect(Collectors.groupingBy(ProductDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        bizDemandListDOMap.forEach((k, v) -> {
            ProductLineAnalyseVO analyseVO = new ProductLineAnalyseVO();
            Optional<ProductDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                analyseVO.setCount(v.size());
                analyseVO.setProductLineId(e.getProductLineId());
                analyseVO.setProductLineName(e.getProductLineName());
                analyseVOList.add(analyseVO);
            });
        });
        //逆序排序
        analyseVOList.sort((a, b) -> b.getCount().compareTo(a.getCount()));

        List<Long> conditionSubProductLineIdList = productDemandQueryList.getSubProductLineIds();
        if (CollectionUtils.isNotEmpty(conditionSubProductLineIdList)) {
            Set<Long> resultProductLineIdSet = analyseVOList.stream().map(ProductLineAnalyseVO::getProductLineId).collect(Collectors.toSet());
            List<Long> queryProductLineIdList = conditionSubProductLineIdList.stream().filter(resultProductLineIdSet::contains).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(queryProductLineIdList)) {
                QueryResultVO<ProductDemandVO> queryResultVO = new QueryResultVO<>();
                queryResultVO.setAnalyseVOList(analyseVOList);
                queryResultVO.setPageQueryResult(ResultUtil.pageEmpty());
                return BaseResult.success(queryResultVO);
            } else {
                condition.setProductLineIds(queryProductLineIdList);
            }
        }

        // 分页查询
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);

        PageQueryResult<ProductDemandVO> pageQueryResult = groupDuplicateUtil.getDemandVOQueryResultVO(productDemandListDO);

        QueryResultVO<ProductDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setPageQueryResult(pageQueryResult);
        queryResultVO.setAnalyseVOList(analyseVOList);

        return BaseResult.success(queryResultVO);
    }

    @Override
    public BaseResult<QueryResultVO<ProductDemandVO>> simpleList(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求对接metersphere平台-接收参数:{}", productDemandQueryList);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        condition.setName(productDemandQueryList.getName());
        condition.setId(productDemandQueryList.getId());

        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize());
        List<ProductDemandListDO> productDemandListDO = CollUtil.defaultIfEmpty(productDemandMapper.simpleList(condition), Collections.emptyList());

        List<ProductDemandVO> productDemandVOList = ProductDemandCopier.INSTANCE.convert(productDemandListDO);

        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        QueryResultVO<ProductDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setPageQueryResult(pageQueryResult);
        queryResultVO.setAnalyseVOList(Collections.emptyList());

        return BaseResult.success(queryResultVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long productDemandId, Integer type) {
        log.info("产品需求暂停,作废接收参数:{},{}", productDemandId, type);

        if (!ProductDemandStatusEnum.SUSPEND.getCode().equals(type)
                && !ProductDemandStatusEnum.INVALID.getCode().equals(type)) {
            throw new BaseBizRuntimeException("操作类型不是暂停或作废,请重试输入");
        }
        ProductDemandDO productDemand = productDemandMapper.get(productDemandId);
        if (productDemand == null) {
            throw new BaseBizRuntimeException("找不到该产品需求");
        }
        if (ProductDemandStatusEnum.INVALID.getCode().equals(productDemand.getStatus())
                || ProductDemandStatusEnum.ONLINE.getCode().equals(productDemand.getStatus())) {
            throw new BaseBizRuntimeException("产品需求状态为已作废或已完成上线时,不能修改状态");
        }

        Integer oldStatus = productDemand.getStatus();
        // 更新需求状态
        productDemand.setStatus(type);
        productDemandComponent.update(productDemand);

        ProjectProductDemandDO relation = projectProductDemandMapper.getByProductDemandId(productDemandId);
        if (relation != null) {
            ProjectDO projectDO = projectMapper.get(relation.getProjectId());
            Map<Long, String> pdNameMap = new HashMap<>();
            pdNameMap.put(productDemand.getId(), productDemand.getName());
            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, null);

            // 暂停or作废解除项目关联
            projectProductDemandComponent.update(null, productDemandId);
        }
        if (ProductDemandStatusEnum.SUSPEND.getCode().equals(type)) {
            // 暂停,更新业务需求状态
            productDemandComponent.updateDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), false);
        } else {
            productDemandComponent.updateDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), true);

            ProductBizDemandCondition c = ProductBizDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
            List<Long> bizDemandIds = productBizDemandMapper.select(c).stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bizDemandIds)) {
                Map<Long, String> bdNameMap = bizDemandMapper.getByIds(bizDemandIds).stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));
                productDemandLogComponent.addLogWhenLinkOrUnlink(productDemand.getName(), productDemand.getId(), bdNameMap, null);
                // 作废解业务需求关联
                productBizDemandComponent.update(productDemandId, null, relation == null);
            }

            ProductCustomDemandCondition cc = ProductCustomDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
            List<Long> customDemandIds = productCustomDemandMapper.select(cc).stream().map(ProductCustomDemandDO::getCustomDemandId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(customDemandIds)) {
                Map<Long, String> cdNameMap = customDemandMapper.selectByIds(customDemandIds).stream().collect(Collectors.toMap(CustomDemandDO::getId, CustomDemandDO::getName, (v1, v2) -> v2));
                productDemandLogComponent.addLogWhenLinkOrUnlinkCustomDemand(productDemand.getName(), productDemand.getId(), cdNameMap, null);
                productCustomDemandComponent.update(productDemandId, null, relation == null);
            }

            bizLabelComponent.deleteLabel(productDemandId, BizTypeEnum.PRODUCT_DEMAND.getCode());
        }
        String action = ProductDemandStatusEnum.SUSPEND.getCode().equals(type) ? ButtonActionEnum.SUSPEND.getText() : ButtonActionEnum.INVALID.getText();
        productDemandLogComponent.addLogWhenStatusChange(oldStatus, type, productDemandId, action);
        //解除任务关联
        taskProductDemandComponent.update(null, productDemandId);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> updatePriority(ProductDemandPriorityUpdateReq productDemandPriorityUpdateReq) {
        Integer priority = productDemandPriorityUpdateReq.getPriority();
        if (priority == null || priority < 0) {
            throw new BaseBizRuntimeException("非法优先级");
        }
        if (StrUtil.isBlank(PriorityEnum.getTextByCode(priority))) {
            throw new BaseBizRuntimeException("未知优先级");
        }
        ProductDemandDO pdo = new ProductDemandDO().setPriority(productDemandPriorityUpdateReq.getPriority());
        pdo.setId(productDemandPriorityUpdateReq.getId());
        productDemandComponent.update(pdo);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long productDemandId) {
        log.info("产品需求开启接收参数:{}", productDemandId);
        ProductDemandDO productDemandDO = productDemandMapper.get(productDemandId);
        if (productDemandDO == null) {
            throw new BaseBizRuntimeException("找不到该产品需求");
        }
        if (!ProductDemandStatusEnum.SUSPEND.getCode().equals(productDemandDO.getStatus())) {
            throw new BaseBizRuntimeException("产品需求状态不是暂停,不能开启");
        }
        productDemandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        productDemandComponent.update(productDemandDO);
        productDemandComponent.updateDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), false);

        productDemandLogComponent.addLogWhenStatusChange(ProductDemandStatusEnum.SUSPEND.getCode(), ProductDemandStatusEnum.WAITING.getCode()
                , productDemandId, ButtonActionEnum.ENABLE.getText());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProductDemandAddReq productDemandAddReq) {
        log.info("产品需求新增接收参数:{}", productDemandAddReq);

        ProductDemandDO productDemandDO = productDemandMapper.getByName(productDemandAddReq.getName());
        if (productDemandDO != null) {
            throw new BaseBizRuntimeException("该产品需求名称已存在,请修改后重试");
        }

        if (StringUtils.isNotBlank(productDemandAddReq.getTargetCustomer())
                && productDemandAddReq.getTargetCustomer().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("目标客户/用户/项目中请勿包含空格");
        }
        if (StrUtil.isNotEmpty(productDemandAddReq.getTargetCustomer())) {
            String postGrade = crmClient.getPostGrade(productDemandAddReq.getTargetCustomer());
            productDemandAddReq.setCustomerGrade(postGrade);
        }
        checkDescLength(productDemandAddReq.getDesc());
        ProductDemandDO productDemand = ProductDemandCopier.INSTANCE.convert(productDemandAddReq);
        productDemand.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        productDemand.setType(JSON.toJSONString(productDemandAddReq.getTypes()));
        productDemandMapper.insert(productDemand);

        fileComponent.add(productDemandAddReq.getFiles(), productDemand.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());

        List<PersonAddReq> recipients = productDemandAddReq.getRecipients();
        personComponent.add(productDemandAddReq.getRecipients(), productDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        if (!CollectionUtils.isEmpty(recipients)) {
            List<String> copiers = recipients.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            messageEventPublisher.publish(new ProductDemandToCopiedMsgEvent(
                    this,
                    productDemand.getId(),
                    productDemand.getCreateMan(),
                    copiers,
                    productDemand.getName()
            ));
        }
        boolean hasProject = productDemandAddReq.getProjectId() != null;

        List<Long> bizDemandIds = productDemandAddReq.getBizDemandIds();
        if (CollectionUtils.isNotEmpty(bizDemandIds)) {
            //只关联产品或客户需求时,不关联项目,此处计算项目发布时间
            productBizDemandComponent.batchInsert(productDemand.getId(), bizDemandIds, !hasProject);
            Map<Long, String> bdNameMap = bizDemandMapper.getByIds(bizDemandIds).stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));
            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemand.getName(), productDemand.getId(), bdNameMap, ButtonActionEnum.LINK.getText());
        }

        List<Long> customDemandIds = productDemandAddReq.getCustomDemandIds();
        if (CollectionUtils.isNotEmpty(customDemandIds)) {
            productCustomDemandComponent.batchInsert(productDemand.getId(), customDemandIds, !hasProject);
            Map<Long, String> bdNameMap = customDemandMapper.selectByIds(customDemandIds).stream().collect(Collectors.toMap(CustomDemandDO::getId, CustomDemandDO::getName, (v1, v2) -> v2));
            productDemandLogComponent.addLogWhenLinkOrUnlinkCustomDemand(productDemand.getName(), productDemand.getId(), bdNameMap, ButtonActionEnum.LINK.getText());

        }

        if (!hasProject) {
            //如果没关联项目,需要计算业务,客户需求状态
            productDemandComponent.updateDemandStatusAsProductStatusChange(Lists.newArrayList(productDemand.getId()), false);
        } else {
            ProjectDO projectDO = projectMapper.get(productDemandAddReq.getProjectId());
            if (projectDO == null) {
                throw new BaseBizRuntimeException("找不到该项目");
            }
            List<ProductBizDemandDO> productBizDemandDOList = productBizDemandMapper.getByProductDemandIds(Lists.newArrayList(productDemand.getId()));
            Map<Long, Integer> bizIdMap = productBizDemandDOList.stream().collect(Collectors.toMap(ProductBizDemandDO::getBizDemandId, ProductBizDemandDO::getStatus, (v1, v2) -> v2));

            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus(), Lists.newArrayList(productDemand.getId()));

            projectProductDemandComponent.batchInsert(productDemandAddReq.getProjectId(), Lists.newArrayList(productDemand.getId()));
            //
            bizIdMap.forEach((k, v) -> {
                BizDemandDO bizDemandDO = bizDemandMapper.get(k);
                productDemandComponent.sendDingMsg(v, bizDemandDO.getStatus(), k);
            });

            Map<Long, String> pdNameMap = new HashMap<>();
            pdNameMap.put(productDemand.getId(), productDemand.getName());
            projectLogComponent.addLogWhenLinkOrUnlink(projectDO.getName(), projectDO.getId(), pdNameMap, ButtonActionEnum.LINK.getText());
        }
        productDemandLogComponent.addLogWhenStatusChange(productDemand.getStatus(), productDemand.getStatus(), productDemand.getId(), ButtonActionEnum.SUBMIT.getText());

        if (CollectionUtils.isNotEmpty(productDemandAddReq.getTrackEventIds())) {
            List<Long> trackEventIds = productDemandAddReq.getTrackEventIds();
            productDemandTrackEventComponent.batchInsert(productDemand.getId(), trackEventIds);
            List<String> eventNames = trackEventMapper.selectByIds(trackEventIds).stream().map(TrackEventDO::getFullCnName).collect(Collectors.toList());
            productDemandLogComponent.addLogWhenLinkOrUnlinkTrackEvent(productDemand.getId(), eventNames, ButtonActionEnum.LINK.getText());
        }

        //标签
        if (CollectionUtils.isNotEmpty(productDemandAddReq.getLabelIds())) {
            bizLabelComponent.addLabel(productDemand.getId(), productDemandAddReq.getLabelIds(), BizTypeEnum.PRODUCT_DEMAND.getCode());
            bizLabelComponent.addLog(productDemand.getId(), productDemandAddReq.getLabelIds(), BizTypeEnum.PRODUCT_DEMAND.getCode(), true);
        }

        // 资源计划
        onlyUpdateResourcePlan(productDemandAddReq, productDemand);

        return BaseResult.success(true);
    }

    private void onlyUpdateResourcePlan(ProductDemandAddReq productDemandAddReq, ProductDemandDO productDemand) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandGroupResourcePlanReq resourcePlanReq = new ProductDemandGroupResourcePlanReq();
        List<ResourcePlanProductDemandAddReq> planAddReqs = new ArrayList<>();
        ResourcePlanProductDemandAddReq planAddReq = new ResourcePlanProductDemandAddReq();
        planAddReq.setProductDemandId(productDemand.getId());
        planAddReq.setProductDemandOwners(productDemandAddReq.getProductDemandOwners());
        planAddReqs.add(planAddReq);
        resourcePlanReq.setProductDemands(planAddReqs);
        resourcePlanReq.setOperatorId(userInfo.getId());
        resourcePlanReq.setOperator(userInfo.getAlias());
        productDemandGroupService.upsertResourcePlan(resourcePlanReq, true, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProductDemandModifyReq productDemandModifyReq) {
        log.info("产品需求修改接收参数:{}", productDemandModifyReq);
        ProductDemandDO oldProductDemand = productDemandMapper.getByName(productDemandModifyReq.getName());
        if (oldProductDemand != null && !oldProductDemand.getId().equals(productDemandModifyReq.getId())) {
            throw new BaseBizRuntimeException("该产品需求名称已存在,请修改后重试");
        }
        if (oldProductDemand == null) {
            oldProductDemand = productDemandMapper.get(productDemandModifyReq.getId());
        }
        if (productDemandModifyReq.getDescChangeReq() != null) {
            // 发起变更记录时不直接修改产品需求描述
            productDemandModifyReq.setDesc(oldProductDemand.getDesc());
        }

        // 如果有客户名称但是没有客户等级则尝试填入
        if (StrUtil.isNotEmpty(productDemandModifyReq.getTargetCustomer())) {
            Optional.ofNullable(crmClient.getPostGrade(productDemandModifyReq.getTargetCustomer()))
                    .ifPresent(productDemandModifyReq::setCustomerGrade);
        }

        ProductDemandDO newProductDemand = ProductDemandCopier.INSTANCE.convert(productDemandModifyReq);
        newProductDemand.setType(JSON.toJSONString(productDemandModifyReq.getTypes()));
        productDemandMapper.update(newProductDemand);
        // 附件
        fileComponent.update(productDemandModifyReq.getFiles(), newProductDemand.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        // 抄送人
        List<PersonDO> personDO = personComponent.select(newProductDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        List<PersonAddReq> originalRecipients = PersonCopier.INSTANCE.do2req(personDO);

        List<PersonAddReq> recipients = productDemandModifyReq.getRecipients();
        // 创建新集合来存储需要新增的抄送人
        List<PersonAddReq> newRecipients = recipients.stream()
                .filter(r -> originalRecipients.stream()
                        .noneMatch(or -> or.getUserId().equals(r.getUserId())))
                .collect(Collectors.toList());
        // 更新抄送人
        personComponent.update(recipients, newProductDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        // 发送消息
        if (CollUtil.isNotEmpty(newRecipients)) {
            // 发送消息
            List<String> copiers = newRecipients.stream().map(PersonAddReq::getUserId).collect(Collectors.toList());
            messageEventPublisher.publish(new ProductDemandToCopiedMsgEvent(
                    this,
                    newProductDemand.getId(),
                    newProductDemand.getModifyMan(),
                    copiers,
                    newProductDemand.getName()
            ));
        }

        // 资源计划
        onlyUpdateResourcePlan(productDemandModifyReq, newProductDemand);

        // 日志
        productDemandLogComponent.addLogWhenModifyData(oldProductDemand, newProductDemand);

        productDemandDescFlowComponent.startProductDemandDescChangeFlow(productDemandModifyReq);

        updateTargetCustomer(newProductDemand, productDemandModifyReq.getId());

        return BaseResult.success(true);
    }

    private void updateTargetCustomer(ProductDemandDO newProductDemand, Long id) {
        List<BizDemandListDO> bizDemandList = bizDemandMapper.linkBizDemandList(id);
        if (CollUtil.isNotEmpty(bizDemandList)) {
            // 得到业务需求的客户等级最大值
            // 使用(v1, v2) -> v2作为合并函数，当出现重复的customerGrade时，保留后来的targetCustomer值
            Map<String, String> customerGradeMap = bizDemandList.stream()
                    .collect(Collectors.toMap(
                            BizDemandListDO::getCustomerGrade,
                            BizDemandListDO::getTargetCustomer,
                            (v1, v2) -> v2
                    ));

            // 根据CustomerGradeEnum的score进行排序，获取具有最高优先级的客户等级
            Optional<Map.Entry<String, String>> highestGradeEntry = customerGradeMap.entrySet()
                    .stream()
                    .filter(entry -> entry.getKey() != null)
                    .sorted((entry1, entry2) -> {
                        // 获取CustomerGradeEnum中对应的枚举值
                        CustomerGradeEnum grade1 = CustomerGradeEnum.getByText(entry1.getKey());
                        CustomerGradeEnum grade2 = CustomerGradeEnum.getByText(entry2.getKey());

                        // 如果枚举值存在，则按score降序排序(优先级从高到低: S, A, B, C, D)
                        if (grade1 != null && grade2 != null) {
                            return Integer.compare(grade2.getScore(), grade1.getScore());
                        }

                        // 如果其中一个枚举值不存在，将其排在后面
                        if (grade1 != null) return -1;
                        if (grade2 != null) return 1;

                        // 如果都不存在，保持原有顺序
                        return 0;
                    })
                    .findFirst();

            // 如果找到了最高优先级的客户等级，则可以获取对应的targetCustomer值
            if (highestGradeEntry.isPresent()) {
                String highestGrade = highestGradeEntry.get().getKey();
                String targetCustomer = highestGradeEntry.get().getValue();
                // 在这里可以使用highestGrade和targetCustomer进行后续处理
                newProductDemand.setCustomerGrade(highestGrade);
                newProductDemand.setTargetCustomer(targetCustomer);
                // 更新产品需求
                productDemandMapper.update(newProductDemand);
            }
        } else {
            newProductDemand.setCustomerGrade("");
            newProductDemand.setTargetCustomer("");
            // 删除产品需求
            productDemandMapper.update(newProductDemand);
        }
    }

    @Override
    public BaseResult<ProductDemandDetailVO> get(Long productDemandId) {
        log.info("产品需求查看接收参数:{}", productDemandId);
        ProductDemandDetailVO productDemandDetailVO = productDemandComponent.get(productDemandId);
        return BaseResult.success(productDemandDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> matchProjectList(ProductDemandLinkProjectQueryList query) {
        log.info("产品需求-项目匹配接收参数:{}", query);

        // 校验当前产品需求是否已经关联项目
        if (query.getProductDemandId() != null) {
            ProjectProductDemandDO related = projectProductDemandMapper.getByProductDemandId(query.getProductDemandId());
            AssertUtil.checkState(related == null, "该产品需求已被关联,请解除后重试");
        }

        // 如果查询状态条件为空,默认选择下列状态
        List<Integer> status = query.getStatus();
        if (CollUtil.isEmpty(status)) {
            query.setStatus(Lists.newArrayList(
                    ProjectStatusEnum.DEVING.getCode(),
                    ProjectStatusEnum.WAITING.getCode(),
                    ProjectStatusEnum.PLANING.getCode(),
                    ProjectStatusEnum.TESTING.getCode()));
        }

        // 转换类型
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(query);

        // 如果标签条件不为空
        if (CollUtil.isNotEmpty(query.getLabelIds()) || CollUtil.isNotEmpty(query.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(query.getLabelIds(), query.getLabelCategoryIds());
            if (CollUtil.isEmpty(labelIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setLabelIds(labelIds);
        }

        PageQueryResult<ProjectVO> pageQueryResult = projectComponent.page(condition, Lists.newArrayList()).getPageQueryResult();
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(ProductDemandLinkBizDemandQueryList query) {
        log.info("产品需求-业务需求匹配接收参数:{}", query);
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(query);

        List<Integer> status = query.getStatusList();
        if (CollUtil.isEmpty(status)) {
            condition.setStatusList(Lists.newArrayList(
                    BizDemandStatusEnum.RECEIVED.getCode()
                    , BizDemandStatusEnum.PJ_SUSPEND.getCode()
                    , BizDemandStatusEnum.INCLUDE_PROJECT.getCode()
                    , BizDemandStatusEnum.PROJECTING.getCode()
                    , BizDemandStatusEnum.PD_LINKED.getCode()
                    , BizDemandStatusEnum.AVAILABLE.getCode()));
        }
        // 过滤掉已经关联的业务需求
        if (condition.getProductDemandId() != null) {
            List<ProductBizDemandDO> productBizDemand = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                    .productDemandId(condition.getProductDemandId())
                    .isDeleted(false)
                    .build());
            List<Long> bizDemandIds = productBizDemand.stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
            condition.setBizDemandIds(bizDemandIds);
        }
        condition.setCollation(CommonConstant.DEFAULT_ORDER_BY);
        if (CollUtil.isNotEmpty(query.getLabelIds()) || CollUtil.isNotEmpty(query.getLabelCategoryIds())) {
            List<Long> labelIds = labelComponent.getLabelIds(query.getLabelIds(), query.getLabelCategoryIds());
            if (CollUtil.isEmpty(labelIds)) {
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setLabelIds(labelIds);
        }
        return BaseResult.success(bizDemandComponent.page(condition).getPageQueryResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkBizDemand(ProductBizDemandLinkReq bizDemandLinkReq) {
        log.info("关联or取消关联业务需求,参数:{}", bizDemandLinkReq);
        List<Long> bizDemandIds = bizDemandLinkReq.getBizDemandIds();
        List<Long> productDemandIds = Lists.newArrayList(bizDemandLinkReq.getProductDemandId());
        ProductDemandDO productDemandDO = productDemandMapper.selectById(bizDemandLinkReq.getProductDemandId());
        List<BizDemandDO> bizDemandDOS = bizDemandMapper.getByIds(bizDemandIds);
        Map<Long, String> bdNameMap = bizDemandDOS.stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(bizDemandLinkReq.getType())) {
            productBizDemandComponent.batchInsert(bizDemandLinkReq.getProductDemandId(), bizDemandIds, true);

            productDemandComponent.updateBizDemandStatus(productDemandIds, false);

            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            //当前业务需求下的所有产品需求
            Long bizDemandId = bizDemandIds.get(0);

            productBizDemandComponent.update(bizDemandLinkReq.getProductDemandId(), bizDemandId, true);

            productDemandComponent.updateDemandStatusWhenUnlink(bizDemandId, productDemandDO.getId(), true);

            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.UN_LINK.getText());

        }

        updateTargetCustomer(productDemandDO, productDemandDO.getId());

        return BaseResult.success(true);
    }

    @Override
    public ProjectVO linkProjectList(Long productDemandId) {
        log.info("产品需求-项目清单接收参数:{}", productDemandId);
        ProjectDO projectDO = projectMapper.getByProductDemandId(productDemandId);
        if (projectDO == null) {
            return null;
        }
        return ProjectCopier.INSTANCE.transform(projectDO);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> linkBizDemandList(ProductBizDemandQueryList query) {
        log.info("产品需求-业务需求清单接收参数:{}", query);
        PageHelper.startPage(query.getPageNum(), query.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<BizDemandListDO> bizDemandList = bizDemandMapper.linkBizDemandList(query.getProductDemandId());

        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandList);
        Map<Long, GroupResponse> deptMap = bizDemandComponent.getGroupListTreeMap(bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList()));
        bizDemandVOList.forEach(p -> {
            GroupResponse response = deptMap.get(p.getDeptId());
            if (response == null) {
                log.info("没有找到部门,id为:{}", p.getDeptId());
            } else {
                p.setDeptName(response.getGroupName());
                p.setDeptDeleteFlag(response.getDeleteFlag());
            }
            p.setPriorityText(PriorityEnum.getTextChineseByCode(p.getPriority()));
        });

        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>(bizDemandList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> productDemandBatchTransferReceiveMan(BatchTransferReq batchTransferReq) {
        // 参数
        List<Long> idList = batchTransferReq.getIdList();
        String owner = batchTransferReq.getReceiveMan();
        String ownerId = batchTransferReq.getReceiveManId();

        // 批量更新
        if (CollectionUtils.isNotEmpty(idList)) {
            // 日志
            List<BizChangeLogDO> bizChangeLogDOList = new ArrayList<>();
            List<ProductDemandDO> productDemandDOList = productDemandMapper.selectByIdList(idList);
            for (ProductDemandDO e : productDemandDOList) {
                if (Objects.equals(e.getOwnerId(), ownerId)) {
                    continue;
                }
                BizChangeLogDO log = productDemandLogComponent.getLog(e.getOwner(), owner, e.getId(), BizChangeLogFieldEnum.OWNER.getText(), true);
                bizChangeLogDOList.add(log);
            }

            if (CollUtil.isNotEmpty(bizChangeLogDOList)) {
                // 实体
                productDemandMapper.updateOwner(idList, owner, ownerId);
                // 日志
                productDemandLogComponent.batchAddLog(bizChangeLogDOList);
                // 通知
                int count = bizChangeLogDOList.size();
                String receiveManId = batchTransferReq.getReceiveManId();
                String initiator = LocalSessionUtils.getUserInfo().getFullAlias();
                new ProductDemandBatchTransferMsgEvent(this, initiator, receiveManId, count).send();
            }

        }

        return BaseResult.success(true);
    }

    public BaseResult<Boolean> batchUpdateTargetCustomer() {
        List<ProductDemandDO> productDemandDOS = productDemandMapper.selectList();
        AtomicInteger updateCount = new AtomicInteger(0);

        if (!CollUtil.isEmpty(productDemandDOS)) {
            // 使用CompletableFuture并行处理
            List<CompletableFuture<Void>> futures = productDemandDOS.stream()
                    .map(productDemandDO -> CompletableFuture.runAsync(() -> {
                        List<BizDemandListDO> bizDemandList = bizDemandMapper.linkBizDemandList(productDemandDO.getId());
                        long count = bizDemandList.stream()
                                .map(BizDemandListDO::getCustomerGrade)
                                .filter(StrUtil::isNotEmpty)
                                .count();
                        if (count > 0) {
                            updateTargetCustomer(productDemandDO, productDemandDO.getId());
                            updateCount.getAndIncrement();
                        }
                    }))
                    .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        }

        log.info("批量更新产品需求客户成功,更新数量:{}", updateCount.get());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> updateResourcePlan(ResourcePlanProductDemandAddReq resourcePlanProductDemandAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandGroupResourcePlanReq planReq = new ProductDemandGroupResourcePlanReq();
        List<ResourcePlanProductDemandAddReq> reqList = new ArrayList<>(1);
        reqList.add(resourcePlanProductDemandAddReq);
        planReq.setProductDemands(reqList);
        planReq.setOperatorId(userInfo.getId());
        planReq.setOperator(userInfo.getAlias());
        return productDemandGroupService.upsertResourcePlan(planReq, true, true);
    }

    @Override
    public BaseResult<Boolean> updateDemandStatus(ProductDemandStatusUpdateReq productDemandStatusUpdateReq) {
        ProductDemandDO productDemandDO = productDemandMapper.get(productDemandStatusUpdateReq.getId());
        AssertUtil.notNull(productDemandDO, "产品需求不存在");
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        productDemandDO.setModifyManId(userInfo.getId());
        productDemandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productDemandDO.setModifyDate(new Date());
        Integer status = productDemandStatusUpdateReq.getStatus();
        if (ProductDemandStatusEnum.SUSPEND.getCode().equals(status) || ProductDemandStatusEnum.INVALID.getCode().equals(status)) {
            return updateStatus(productDemandDO.getId(), status);
        } else if (ProductDemandStatusEnum.DEVELOPING.getCode().equals(status)
                || ProductDemandStatusEnum.DEV_COMPLETED.getCode().equals(status)
                || ProductDemandStatusEnum.ONLINE.getCode().equals(status)) {
            productDemandDO.setStatus(status);
            productDemandMapper.updateStatus(productDemandDO);
            // 更新关联的业务需求状态
            productDemandComponent.updateDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandDO.getId()), false);
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<ProductDemandStatusVO>> queryNextDemandStatus(Long id) {
        ProductDemandDO productDemandDO = productDemandMapper.get(id);
        AssertUtil.notNull(productDemandDO, "产品需求不存在");
        List<ProductDemandStatusVO> statusVOS = new ArrayList<>();
        if (ProductDemandStatusEnum.SUSPEND.getCode().equals(productDemandDO.getStatus())) {
            ProductDemandStatusVO suspendStatusVO = new ProductDemandStatusVO();
            suspendStatusVO.setStatus(ProductDemandStatusEnum.INVALID.getCode());
            suspendStatusVO.setStatusText(ProductDemandStatusEnum.INVALID.getText());
            statusVOS.add(suspendStatusVO);
            return BaseResult.success(statusVOS);
        }

        if (ProductDemandStatusEnum.INCLUDED.getCode().equals(productDemandDO.getStatus())
                || ProductDemandStatusEnum.WAITING.getCode().equals(productDemandDO.getStatus())) {
            ProductDemandStatusVO devStatusVO = new ProductDemandStatusVO();
            devStatusVO.setStatus(ProductDemandStatusEnum.DEVELOPING.getCode());
            devStatusVO.setStatusText(ProductDemandStatusEnum.DEVELOPING.getText());
            statusVOS.add(devStatusVO);
        } else if (ProductDemandStatusEnum.DEVELOPING.getCode().equals(productDemandDO.getStatus())
        || ProductDemandStatusEnum.PROGRESS.getCode().equals(productDemandDO.getStatus())) {
            ProductDemandStatusVO devCompleteStatusVO = new ProductDemandStatusVO();
            devCompleteStatusVO.setStatus(ProductDemandStatusEnum.DEV_COMPLETED.getCode());
            devCompleteStatusVO.setStatusText(ProductDemandStatusEnum.DEV_COMPLETED.getText());
            statusVOS.add(devCompleteStatusVO);
        } else if (ProductDemandStatusEnum.DEV_COMPLETED.getCode().equals(productDemandDO.getStatus())) {
            ProductDemandStatusVO onlineStatusVO = new ProductDemandStatusVO();
            onlineStatusVO.setStatus(ProductDemandStatusEnum.ONLINE.getCode());
            onlineStatusVO.setStatusText(ProductDemandStatusEnum.ONLINE.getText());
            statusVOS.add(onlineStatusVO);
        } else {
            return BaseResult.success(new ArrayList<>());
        }

        // 暂停
        ProductDemandStatusVO pauseStatusVO = new ProductDemandStatusVO();
        pauseStatusVO.setStatus(ProductDemandStatusEnum.SUSPEND.getCode());
        pauseStatusVO.setStatusText(ProductDemandStatusEnum.SUSPEND.getText());
        statusVOS.add(pauseStatusVO);

        // 作废
        ProductDemandStatusVO cancelStatusVO = new ProductDemandStatusVO();
        cancelStatusVO.setStatus(ProductDemandStatusEnum.INVALID.getCode());
        cancelStatusVO.setStatusText(ProductDemandStatusEnum.INVALID.getText());
        statusVOS.add(cancelStatusVO);
        return BaseResult.success(statusVOS);
    }

    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> matchTrackEventList(ProductDemandLinkTrackEventQueryList trackEventQueryList) {
        log.info("产品需求-事件匹配,参数:{}", trackEventQueryList);
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        if (trackEventQueryList.getProductDemandId() != null) {
            ProductDemandTrackEventCondition c = ProductDemandTrackEventCondition.builder().productDemandId(trackEventQueryList.getProductDemandId()).isDeleted(false).build();
            List<Long> trackEventIds = productDemandTrackEventMapper.select(c).stream().map(ProductDemandTrackEventDO::getTrackEventId).collect(Collectors.toList());
            condition.setFilterTrackEventIds(trackEventIds);
        }
        condition.setStatus(Lists.newArrayList(ForwardFlowStatusEnum.COMPLETE.getCode()));
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackEventComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkTrackEvent(ProductDemandTrackEventLinkReq trackEventLinkReq) {
        log.info("关联or取消关联事件,参数:{}", trackEventLinkReq);
        List<Long> trackEventIds = trackEventLinkReq.getTrackEventIds();
        Long productDemandId = trackEventLinkReq.getProductDemandId();
        List<String> eventNames = trackEventMapper.selectByIds(trackEventIds).stream().map(TrackEventDO::getFullCnName).collect(Collectors.toList());

        if (LinkOrUnLinkEnum.LINK.getCode().equals(trackEventLinkReq.getType())) {

            productDemandTrackEventComponent.batchInsert(trackEventLinkReq.getProductDemandId(), trackEventIds);

            productDemandLogComponent.addLogWhenLinkOrUnlinkTrackEvent(productDemandId, eventNames, ButtonActionEnum.LINK.getText());

        } else {
            Long trackEventId = trackEventIds.get(0);
            productDemandTrackEventComponent.update(trackEventLinkReq.getProductDemandId(), trackEventId);

            productDemandLogComponent.addLogWhenLinkOrUnlinkTrackEvent(productDemandId, eventNames, ButtonActionEnum.UN_LINK.getText());

        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> linkTrackEventList(ProductDemandTrackEventQueryList trackEventQueryList) {
        log.info("产品需求-事件清单,参数:{}", trackEventQueryList);
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<TrackEventDO> list = trackEventMapper.linkTrackEventList(trackEventQueryList.getProductDemandId());
        List<TrackEventVO> trackEventVOList = TrackEventCopier.INSTANCE.convert(list);
        trackEventVOList.forEach(a -> {
            a.setEnvNames(EnvEnum.getTextByCode(JSONObject.parseArray(a.getEnv(), Integer.class)));
            a.setPlatformNames(PlatformTypeEnum.getTextByCode(JSONObject.parseArray(a.getPlatform(), Integer.class)));
        });
        PageInfo<TrackEventDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<TrackEventVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(trackEventVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> matchCustomDemandList(ProductLinkCustomDemandQueryList customDemandQueryList) {
        log.info("产品需求-客户需求匹配,参数:{}", customDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<String> receiveManIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        log.info("我和我的下属:receiveManIdList={}", receiveManIdList);
        CustomDemandListCondition condition = CustomDemandCopier.INSTANCE.convert(customDemandQueryList);
        condition.setReceiveManIds(receiveManIdList);
        List<Integer> status = customDemandQueryList.getStatus();
        if (CollectionUtils.isEmpty(status)) {
            condition.setStatus(Lists.newArrayList(
                    BizDemandStatusEnum.RECEIVED.getCode()
                    , BizDemandStatusEnum.PJ_SUSPEND.getCode()
                    , BizDemandStatusEnum.INCLUDE_PROJECT.getCode()
                    , BizDemandStatusEnum.PROJECTING.getCode()
                    , BizDemandStatusEnum.PD_LINKED.getCode()
                    , BizDemandStatusEnum.AVAILABLE.getCode()));
        }
        // 过滤掉已经关联的业务需求
        if (customDemandQueryList.getProductDemandId() != null) {
            ProductCustomDemandCondition c = ProductCustomDemandCondition.builder().productDemandId(customDemandQueryList.getProductDemandId()).isDeleted(false).build();
            List<ProductCustomDemandDO> productBizDemand = productCustomDemandMapper.select(c);
            List<Long> customDemandIds = productBizDemand.stream().map(ProductCustomDemandDO::getCustomDemandId).collect(Collectors.toList());
            condition.setCustomDemandIds(customDemandIds);
        }
        PageHelper.startPage(customDemandQueryList.getPageNum(), customDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return customDemandComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkCustomDemand(ProductCustomDemandLinkReq customDemandLinkReq) {
        log.info("关联or取消关联客户需求,参数:{}", customDemandLinkReq);
        List<Long> customDemandIds = customDemandLinkReq.getCustomDemandIds();
        List<Long> productDemandIds = Lists.newArrayList(customDemandLinkReq.getProductDemandId());
        ProductDemandDO productDemandDO = productDemandMapper.selectById(customDemandLinkReq.getProductDemandId());
        Map<Long, String> bdNameMap = customDemandMapper.selectByIds(customDemandIds).stream().collect(Collectors.toMap(CustomDemandDO::getId, CustomDemandDO::getName, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(customDemandLinkReq.getType())) {
            productCustomDemandComponent.batchInsert(customDemandLinkReq.getProductDemandId(), customDemandIds, true);

            productDemandComponent.updateCustomDemandStatus(productDemandIds, false);
//
            productDemandLogComponent.addLogWhenLinkOrUnlinkCustomDemand(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            Long customDemandId = customDemandIds.get(0);

            productCustomDemandComponent.update(customDemandLinkReq.getProductDemandId(), customDemandId, true);

            productDemandComponent.updateDemandStatusWhenUnlink(customDemandId, productDemandDO.getId(), false);

            productDemandLogComponent.addLogWhenLinkOrUnlinkCustomDemand(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.UN_LINK.getText());
//
        }
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<CustomDemandVO>> linkCustomDemandList(ProductCustomDemandQueryList customDemandQueryList) {
        log.info("产品需求-客户需求清单,参数:{}", customDemandQueryList);
        PageHelper.startPage(customDemandQueryList.getPageNum(), customDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        CustomDemandListCondition condition = CustomDemandListCondition.builder()
                .productDemandId(customDemandQueryList.getProductDemandId())
                .linkCustomList(true)
                .build();
        return customDemandComponent.list(condition);
    }

    private void checkDescLength(String desc) {
        Integer descLength = StrUtil.length(desc);
        AssertUtil.checkState(CommonConstant.DESC_MAX_LENGTH.compareTo(descLength) >= 0, "需求描述字数过大,请重新输入");
    }

}
