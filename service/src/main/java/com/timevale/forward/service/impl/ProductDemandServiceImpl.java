package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.*;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.*;
import com.timevale.forward.facade.api.request.*;
import com.timevale.forward.facade.api.result.*;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.*;
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
    private ProjectComponent projectCmponent;

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

    private static final Integer MAX_LENGTH = 20 * 1000;


    @Override
    public BaseResult<QueryResultVO<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求接收参数:{}", productDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());
        } else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                allMyStaffWithSelf.retainAll(productDemandQueryList.getOwnerIds());
                log.info("我和我的下属,过滤后:{}", allMyStaffWithSelf);
            }
            if (CollectionUtils.isEmpty(allMyStaffWithSelf)) {
                //所选人员不在我的团队中
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
            condition.setOwnerIds(allMyStaffWithSelf);
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandQueryList.getAscription())) {
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));

            String groupId = baseInfos.get(0).getDefaultGroup().getGroupId();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(groupId);
            log.info("用户默认部门id:{},同部门人员:{}", groupId, accountIds);
            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                accountIds.retainAll(productDemandQueryList.getOwnerIds());
                log.info("用户默认部门id:{},过滤后:{}", groupId, accountIds);
            }
            if (CollectionUtils.isEmpty(accountIds)) {
                //所选人员不在我的部门中
                return BaseResult.success(ResultUtil.queryResultEmpty());
            }
            condition.setOwnerIds(accountIds);
        } else if (AscriptionEnum.COPIER.name().equals(productDemandQueryList.getAscription())) {
            condition.setCopierId(userInfo.getId());
        }

        // 分页查询
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);

        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });

        // 分页数据
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);
        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        // 完整查询
        List<ProductDemandListDO> allProductDemandListDO = productDemandComponent.list(condition);
        Map<Long, List<ProductDemandListDO>> bizDemandListDOMap = allProductDemandListDO.stream().collect(Collectors.groupingBy(ProductDemandListDO::getProductLineId));
        log.info("业务查询产品线分析：{}", bizDemandListDOMap);

        List<ProductLineAnalyseVO> analyseVOList = new ArrayList<>();
        bizDemandListDOMap.forEach((k,v) -> {
            ProductLineAnalyseVO analyseVO = new ProductLineAnalyseVO();
            Optional<ProductDemandListDO> any = v.stream().findAny();
            any.ifPresent(e -> {
                analyseVO.setCount(v.size());
                analyseVO.setProductLineId(e.getProductLineId());
                analyseVO.setProductLineName(e.getProductLineName());
                analyseVOList.add(analyseVO);
            });
        });

        QueryResultVO<ProductDemandVO> queryResultVO = new QueryResultVO<>();
        queryResultVO.setPageQueryResult(pageQueryResult);
        queryResultVO.setAnalyseVOList(analyseVOList);

        return BaseResult.success(queryResultVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long productDemandId, Integer type) {
        log.info("产品需求暂停,作废接收参数:productDemandId={},type={}", productDemandId, type);
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
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), false);
        } else {
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), true);

            ProductBizDemandCondition c = ProductBizDemandCondition.builder().productDemandId(productDemandId).isDeleted(false).build();
            List<Long> bizDemandIds = productBizDemandMapper.select(c).stream().map(ProductBizDemandDO::getBizDemandId).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(bizDemandIds)) {
                Map<Long, String> bdNameMap = bizDemandMapper.selectByIds(bizDemandIds).stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));
                productDemandLogComponent.addLogWhenLinkOrUnlink(productDemand.getName(), productDemand.getId(), bdNameMap, null);
                // 作废解业务需求关联
                productBizDemandComponent.update(productDemandId, null);
            }
        }
        String action = ProductDemandStatusEnum.SUSPEND.getCode().equals(type) ? ButtonActionEnum.SUSPEND.getText() : ButtonActionEnum.INVALID.getText();
        productDemandLogComponent.addLogWhenStatusChange(oldStatus, type, productDemandId, action);
        //解除任务关联
        taskProductDemandComponent.update(null, productDemandId);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> enable(Long productDemandId) {
        log.info("产品需求开启接收参数:productDemandId={}", productDemandId);
        ProductDemandDO productDemandDO = productDemandMapper.get(productDemandId);
        if (productDemandDO == null) {
            throw new BaseBizRuntimeException("找不到该产品需求");
        }
        if (!ProductDemandStatusEnum.SUSPEND.getCode().equals(productDemandDO.getStatus())) {
            throw new BaseBizRuntimeException("产品需求状态不是暂停,不能开启");
        }
        productDemandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        productDemandComponent.update(productDemandDO);
        productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), false);

        productDemandLogComponent.addLogWhenStatusChange(ProductDemandStatusEnum.SUSPEND.getCode(), ProductDemandStatusEnum.WAITING.getCode()
                , productDemandId, ButtonActionEnum.ENABLE.getText());
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProductDemandAddReq productDemandAddReq) {
        log.info("产品需求新增接收参数:{}", productDemandAddReq);

        if (productDemandAddReq.getName().contains(CommonConstant.BLANK)) {
            throw new BaseBizRuntimeException("产业需求名称中请勿包含空格");
        }

        ProductDemandDO productDemandDO = productDemandMapper.getByName(productDemandAddReq.getName());
        if (productDemandDO != null) {
            throw new BaseBizRuntimeException("该产品需求名称已存在,请修改后重试");
        }
        checkDescLength(productDemandAddReq.getDesc());
        ProductDemandDO productDemand = ProductDemandCopier.INSTANCE.convert(productDemandAddReq);
        productDemand.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        productDemand.setType(JSON.toJSONString(productDemandAddReq.getTypes()));
        productDemandMapper.insert(productDemand);

        fileComponent.add(productDemandAddReq.getFiles(), productDemand.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());

        personComponent.add(productDemandAddReq.getRecipients(), productDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

        List<Long> bizDemandIds = productDemandAddReq.getBizDemandIds();
        if (CollectionUtils.isNotEmpty(bizDemandIds)) {
            productBizDemandComponent.batchInsert(productDemand.getId(), bizDemandIds);
            Map<Long, String> bdNameMap = bizDemandMapper.selectByIds(bizDemandIds).stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));
            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemand.getName(), productDemand.getId(), bdNameMap, ButtonActionEnum.LINK.getText());
            if (productDemandAddReq.getProjectId() == null) {
                //如果只关联业务需求,没关联项目,需要计算业务状态
                productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemand.getId()), false);
            }
        }
        if (productDemandAddReq.getProjectId() != null) {
            ProjectDO projectDO = projectMapper.get(productDemandAddReq.getProjectId());
            if (projectDO == null) {
                throw new BaseBizRuntimeException("找不到该项目");
            }
            projectProductDemandComponent.batchInsert(productDemandAddReq.getProjectId(), Lists.newArrayList(productDemand.getId()));
            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());

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
        return BaseResult.success(true);
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
        checkDescLength(productDemandModifyReq.getDesc());
        ProductDemandDO newProductDemand = ProductDemandCopier.INSTANCE.convert(productDemandModifyReq);
        newProductDemand.setType(JSON.toJSONString(productDemandModifyReq.getTypes()));
        productDemandMapper.update(newProductDemand);
        // 附件
        fileComponent.update(productDemandModifyReq.getFiles(), newProductDemand.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        // 抄送人
        personComponent.update(productDemandModifyReq.getRecipients(), newProductDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

        productDemandLogComponent.addLogWhenModifyData(oldProductDemand, newProductDemand);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> get(Long productDemandId) {
        log.info("产品需求查看接收参数:productDemandId={}", productDemandId);
        ProductDemandDetailVO productDemandDetailVO = productDemandComponent.get(productDemandId);
        return BaseResult.success(productDemandDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> matchProjectList(ProductDemandLinkProjectQueryList productDemandLinkProjectQueryList) {
        log.info("产品需求-项目匹配接收参数:projectQueryList={}", productDemandLinkProjectQueryList);
        if (productDemandLinkProjectQueryList.getProductDemandId() != null) {
            ProjectProductDemandDO productDemandDO = projectProductDemandMapper.getByProductDemandId(productDemandLinkProjectQueryList.getProductDemandId());
            if (productDemandDO != null) {
                throw new BaseBizRuntimeException("该产品需求已被关联,请解除后重试");
            }
        }
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(productDemandLinkProjectQueryList);
        condition.setPageNum(productDemandLinkProjectQueryList.getPageNum());
        condition.setPageSize(productDemandLinkProjectQueryList.getPageSize());
        List<Integer> status = productDemandLinkProjectQueryList.getStatus();
        if (CollectionUtils.isEmpty(status)) {
            // 空,默认选择下列状态
            condition.setStatus(Lists.newArrayList(ProjectStatusEnum.WAITING.getCode()
                    , ProjectStatusEnum.PLANING.getCode()
                    , ProjectStatusEnum.DEVING.getCode()
                    , ProjectStatusEnum.TESTING.getCode()));
        }
        PageQueryResult<ProjectVO> pageQueryResult = projectCmponent.page(condition, Lists.newArrayList()).getPageQueryResult();
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(ProductDemandLinkBizDemandQueryList productDemandLinkBizDemandQueryList) {
        log.info("产品需求-业务需求匹配接收参数:bizDemandQueryList={}", productDemandLinkBizDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> receiveManIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        log.info("我和我的下属:receiveManIdList={}", receiveManIdList);
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(productDemandLinkBizDemandQueryList);
        if (!CollectionUtils.isEmpty(condition.getReceiveManIdList())) {
            receiveManIdList.retainAll(condition.getReceiveManIdList());
            log.info("我和我的下属,过滤后,receiveManIdList={}", receiveManIdList);
        }
        if (CollectionUtils.isEmpty(receiveManIdList)) {
            //所选人员不在我和我的下属中
            return BaseResult.success(ResultUtil.pageEmpty());
        }

        condition.setReceiveManIdList(receiveManIdList);
        List<Integer> status = productDemandLinkBizDemandQueryList.getStatusList();
        if (CollectionUtils.isEmpty(status)) {
            condition.setStatusList(Lists.newArrayList(
                    BizDemandStatusEnum.RECEIVED.getCode()
                    , BizDemandStatusEnum.INCLUDE_PROJECT.getCode()
                    , BizDemandStatusEnum.PROJECTING.getCode()
                    , BizDemandStatusEnum.REJECT.getCode()
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
        PageHelper.startPage(productDemandLinkBizDemandQueryList.getPageNum(), productDemandLinkBizDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return BaseResult.success(bizDemandComponent.page(condition).getPageQueryResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkBizDemand(ProductBizDemandLinkReq bizDemandLinkReq) {
        log.info("关联or取消关联业务需求,参数:bizDemandLinkReq={}", bizDemandLinkReq);
        List<Long> bizDemandIds = bizDemandLinkReq.getBizDemandIds();
        List<Long> productDemandIds = Lists.newArrayList(bizDemandLinkReq.getProductDemandId());
        ProductDemandDO productDemandDO = productDemandMapper.selectById(bizDemandLinkReq.getProductDemandId());
        Map<Long, String> bdNameMap = bizDemandMapper.selectByIds(bizDemandIds).stream().collect(Collectors.toMap(BizDemandDO::getId, BizDemandDO::getName, (v1, v2) -> v2));

        if (LinkOrUnLinkEnum.LINK.getCode().equals(bizDemandLinkReq.getType())) {
            productBizDemandComponent.batchInsert(bizDemandLinkReq.getProductDemandId(), bizDemandIds);

            productDemandComponent.updateBizDemandStatusAsProductStatusChange(productDemandIds, false);

            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.LINK.getText());

        } else {
            //当前业务需求下的所有产品需求
            Long bizDemandId = bizDemandIds.get(0);

            productDemandComponent.updateBizDemandStatusWhenUnlink(bizDemandId, productDemandDO.getId());

            productBizDemandComponent.update(bizDemandLinkReq.getProductDemandId(), bizDemandId);

            productDemandLogComponent.addLogWhenLinkOrUnlink(productDemandDO.getName(), productDemandDO.getId(), bdNameMap, ButtonActionEnum.UN_LINK.getText());

        }
        return BaseResult.success(true);
    }

    @Override
    public ProjectVO linkProjectList(Long productDemandId) {
        log.info("产品需求-项目清单接收参数:productDemandId={}", productDemandId);
        ProjectDO projectDO = projectMapper.getByProductDemandId(productDemandId);
        if (projectDO == null) {
            return null;
        }
        ProjectVO projectVO = ProjectCopier.INSTANCE.transform(projectDO);
        projectVO.setStatusName(ProjectStatusEnum.getTextByCode(projectDO.getStatus()));
        projectVO.setPriorityName(PriorityEnum.getTextByCode(projectDO.getPriority()));
        return projectVO;
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> linkBizDemandList(ProductBizDemandQueryList productBizDemandQueryList) {
        log.info("产品需求-业务需求清单接收参数:productBizDemandQueryList={}", productBizDemandQueryList);
        PageHelper.startPage(productBizDemandQueryList.getPageNum(), productBizDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<BizDemandListDO> bizDemandList = bizDemandMapper.linkBizDemandList(productBizDemandQueryList.getProductDemandId());

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
                if (Objects.equals(e.getOwner(), owner)) {
                    continue;
                }
                BizChangeLogDO log = productDemandLogComponent.getLog(e.getOwner(), owner, e.getId(), BizChangeLogFieldEnum.OWNER.getText(), true);
                bizChangeLogDOList.add(log);
            }
            productDemandLogComponent.batchAddLog(bizChangeLogDOList);

            // 实体
            productDemandMapper.updateOwner(idList, owner, ownerId);
        }

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> matchTrackEventList(ProductDemandLinkTrackEventQueryList trackEventQueryList) {
        log.info("产品需求-事件匹配,参数:trackEventQueryList={}", trackEventQueryList);
        TrackEventListCondition condition = TrackEventCopier.INSTANCE.convert(trackEventQueryList);
        if(trackEventQueryList.getProductDemandId()!=null){
            ProductDemandTrackEventCondition c = ProductDemandTrackEventCondition.builder().productDemandId(trackEventQueryList.getProductDemandId()).isDeleted(false).build();
            List<Long> trackEventIds = productDemandTrackEventMapper.select(c).stream().map(ProductDemandTrackEventDO::getTrackEventId).collect(Collectors.toList());
            condition.setFilterTrackEventIds(trackEventIds);
        }
        condition.setStatus(Lists.newArrayList(TrackStatusEnum.REVIEWED.getCode()));
        PageHelper.startPage(trackEventQueryList.getPageNum(), trackEventQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackEventComponent.list(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkTrackEvent(ProductDemandTrackEventLinkReq trackEventLinkReq) {
        log.info("关联or取消关联事件,参数:trackEventLinkReq={}", trackEventLinkReq);
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
        log.info("产品需求-事件清单,参数:trackEventQueryList={}", trackEventQueryList);
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

    private void checkDescLength(String desc) {
        if (StringUtils.isNotEmpty(desc) && desc.getBytes().length > MAX_LENGTH) {
            throw new BaseBizRuntimeException("需求描述字数过大,请重新输入");
        }
    }
}
