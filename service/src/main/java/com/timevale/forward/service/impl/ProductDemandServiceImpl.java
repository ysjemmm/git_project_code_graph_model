package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectProductDemandMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.BizDemandLinkReq;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.*;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.copy.ProjectCopier;
import com.timevale.forward.service.integration.inneruser.InnerGroupClient;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.GroupModel;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    private InnerGroupClient innerGroupClient;

    @Resource
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Resource
    private ProjectProductDemandComponent projectProductDemandComponent;

    @Resource
    private ProductBizDemandComponent productBizDemandComponent;


    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {

        log.info("产品需求接收参数:{}", productDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        if (CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
            condition.setOwnerIds(new ArrayList<>());
        }
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());

        } else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            condition.getOwnerIds().addAll(allMyStaffWithSelf);
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandQueryList.getAscription())) {
            GroupModel defaultGroup = userInfo.getDefaultGroup();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(defaultGroup.getGroupId());
            log.info("用户默认部门id:{},同部门人员:{}", defaultGroup.getGroupId(), accountIds);
            condition.getOwnerIds().addAll(accountIds);
        } else if (AscriptionEnum.COPIER.name().equals(productDemandQueryList.getAscription())) {
            condition.setCopierId(userInfo.getId());
        }
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });
        PageInfo<ProductDemandVO> pageInfo = new PageInfo<>(productDemandVO);

        PageQueryResult<ProductDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);

        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> updateStatus(Long productDemandId, Integer type) {
        log.info("产品需求暂停,作废接收参数:productDemandId={},type={}", productDemandId, type);
        if (!ProductDemandStatusEnum.SUSPEND.getCode().equals(type)
                && !ProductDemandStatusEnum.INVALID.getCode().equals(type)) {
            throw new BaseBizRuntimeException("操作类型不是暂停或作废,请重试输入");
        }
        ProductDemandDO demandDO = productDemandMapper.get(productDemandId);
        if (demandDO == null) {
            throw new BaseBizRuntimeException("找不到该产品需求");
        }
        if (!ProductDemandStatusEnum.WAITING.getCode().equals(demandDO.getStatus())
                && !ProductDemandStatusEnum.INCLUDED.getCode().equals(demandDO.getStatus())
                && !ProductDemandStatusEnum.PROGRESS.getCode().equals(demandDO.getStatus())) {
            throw new BaseBizRuntimeException("产品需求状态不是待排期、已列入项目、项目进行中,不能修改状态");
        }
        // 更新需求状态
        demandDO.setStatus(type);
        productDemandComponent.update(demandDO);

        // 暂停or作废解除项目关联
        ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
        productDemandDO.setProductDemandId(productDemandId);
        productDemandDO.setIsDeleted(true);
        projectProductDemandComponent.update(productDemandDO);

        if (ProductDemandStatusEnum.INVALID.getCode().equals(type)) {
            // 作废解业务需求关联
            ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
            productBizDemandDO.setProductDemandId(productDemandId);
            productBizDemandDO.setIsDeleted(true);
            productBizDemandComponent.update(productBizDemandDO);

        }
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

        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(ProductDemandAddReq productDemandAddReq) {
        log.info("产品需求新增接收参数:{}", productDemandAddReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandDO demandDO = ProductDemandCopier.INSTANCE.convert(productDemandAddReq);
        demandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        demandDO.setCreateManId(userInfo.getId());
        demandDO.setOwnerId(productDemandAddReq.getDemandOwner().getUserId());
        demandDO.setOwner(productDemandAddReq.getDemandOwner().getUserName());
        demandDO.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        demandDO.setType(JSON.toJSONString(productDemandAddReq.getTypes()));
        productDemandMapper.insert(demandDO);

        if (CollectionUtils.isNotEmpty(productDemandAddReq.getFiles())) {
            fileComponent.add(productDemandAddReq.getFiles(), demandDO.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        }
        if (CollectionUtils.isNotEmpty(productDemandAddReq.getRecipients())) {
            personComponent.add(productDemandAddReq.getRecipients(), demandDO.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        }
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProductDemandModifyReq productDemandModifyReq) {
        log.info("产品需求修改接收参数:{}", productDemandModifyReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandDO demandDO = ProductDemandCopier.INSTANCE.convert(productDemandModifyReq);
        demandDO.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        demandDO.setModifyManId(userInfo.getId());
        demandDO.setType(JSON.toJSONString(productDemandModifyReq.getTypes()));
        productDemandMapper.update(demandDO);
        // 附件
        fileComponent.update(productDemandModifyReq.getFiles(), demandDO.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        // 抄送人
        personComponent.update(productDemandModifyReq.getRecipients(), demandDO.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

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
        ProjectProductDemandDO productDemandDO = projectProductDemandMapper.getByProductDemandId(productDemandLinkProjectQueryList.getProductDemandId());
        if (productDemandDO != null) {
            throw new BaseBizRuntimeException("该产品需求已被关联,请解除后重试");
        }
        PageHelper.startPage(productDemandLinkProjectQueryList.getPageNum(), productDemandLinkProjectQueryList.getPageSize());
        ProjectListCondition condition = ProjectCopier.INSTANCE.convert(productDemandLinkProjectQueryList);
        condition.setStatus(Lists.newArrayList(ProjectStatusEnum.WAITING.getCode()
                , ProjectStatusEnum.PLANING.getCode()
                , ProjectStatusEnum.DEVING.getCode()
                , ProjectStatusEnum.TESTING.getCode()));
        return projectCmponent.page(condition);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(ProductDemandLinkBizDemandQueryList productDemandLinkBizDemandQueryList) {
        log.info("产品需求-业务需求匹配接收参数:bizDemandQueryList={}", productDemandLinkBizDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<String> receiveManIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());
        log.info("我和我的下属:receiveManIdList={}", receiveManIdList);
        PageHelper.startPage(productDemandLinkBizDemandQueryList.getPageNum(), productDemandLinkBizDemandQueryList.getPageSize());
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(productDemandLinkBizDemandQueryList);
        condition.setReceiveManIdList(receiveManIdList);
        condition.setStatusList(Lists.newArrayList(
                BizDemandStatusEnum.RECEIVED.getCode()
                , BizDemandStatusEnum.INCLUDE_PROJECT.getCode()
                , BizDemandStatusEnum.PROJECTING.getCode()
                , BizDemandStatusEnum.AVAILABLE.getCode()
                , BizDemandStatusEnum.REJECT.getCode()));
        List<BizDemandListDO> bizDemandListDOList = bizDemandMapper.selectList(condition);
        List<BizDemandVO> bizDemandVOList = BizDemandCopier.INSTANCE.convert(bizDemandListDOList);

        // 查询部门信息
        List<Long> deptIdList = bizDemandVOList.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList());
        Map<Long, String> groupInfo = innerGroupClient.batchGetSimpleGroupMap(deptIdList);

        bizDemandVOList.forEach(iter -> {
            iter.setPriorityText(PriorityEnum.getTextByCode(iter.getPriority()));
            iter.setStatusText(BizDemandStatusEnum.getTextByCode(iter.getStatus()));
            iter.setPlanReleaseDateText(PlanReleaseDateEnum.getTextByCode(iter.getPlanReleaseDate()));
            iter.setDeptName(groupInfo.get(iter.getDeptId()));
        });

        PageInfo<BizDemandVO> pageInfo = new PageInfo<>(bizDemandVOList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkBizDemand(BizDemandLinkReq bizDemandLinkReq) {
        log.info("关联or取消关联接收参数:bizDemandLinkReq={}", bizDemandLinkReq);
        List<Long> bizDemandIds = bizDemandLinkReq.getBizDemandIds();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(bizDemandLinkReq.getType())) {
            productBizDemandComponent.batchInsert(bizDemandLinkReq.getProductDemandId(), bizDemandIds);
        } else {
            ProductBizDemandDO productDemandDO = new ProductBizDemandDO();
            productDemandDO.setIsDeleted(true);
            productDemandDO.setProductDemandId(bizDemandLinkReq.getProductDemandId());
            productDemandDO.setBizDemandId(bizDemandIds.get(0));
            productBizDemandComponent.update(productDemandDO);
        }
        return BaseResult.success(true);
    }

    @Override
    public ProjectVO linkProjectList(Long productDemandId) {
        log.info("产品需求-项目清单接收参数:productDemandId={}", productDemandId);
        ProjectDO projectDO = projectMapper.getByProductDemandId(productDemandId);
        ProjectVO projectVO = ProjectCopier.INSTANCE.transform(projectDO);
        projectVO.setStatusName(ProductDemandStatusEnum.getTextByCode(projectDO.getStatus()));
        projectVO.setPriorityName(PriorityEnum.getTextByCode(projectDO.getPriority()));
        return projectVO;
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> linkBizDemandList(ProductBizDemandQueryList productBizDemandQueryList) {
        log.info("产品需求-业务需求清单接收参数:productBizDemandQueryList={}", productBizDemandQueryList);
        PageHelper.startPage(productBizDemandQueryList.getPageNum(), productBizDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<BizDemandListDO> bizDemandList = bizDemandMapper.productDemandBizDemandList(productBizDemandQueryList.getProductDemandId());

        List<BizDemandVO> bizDemandVO = BizDemandCopier.INSTANCE.convert(bizDemandList);
        List<Long> deptIdList = bizDemandVO.stream().map(BizDemandVO::getDeptId).collect(Collectors.toList());
        Map<Long, String> groupInfo = innerGroupClient.batchGetSimpleGroupMap(deptIdList);
        bizDemandVO.forEach(p -> {
            p.setPriorityText(ProductDemandStatusEnum.getTextByCode(p.getPriority()));
            p.setDeptName(groupInfo.get(p.getDeptId()));
        });

        PageInfo<BizDemandVO> pageInfo = new PageInfo<>(bizDemandVO);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVO);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

}
