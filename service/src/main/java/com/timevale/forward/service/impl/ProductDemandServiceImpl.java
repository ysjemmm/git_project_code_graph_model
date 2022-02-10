package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.BizDemandListCondition;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.condition.ProjectListCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkBizDemandQueryList;
import com.timevale.forward.facade.api.query.ProductDemandLinkProjectQueryList;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductBizDemandLinkReq;
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
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
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

    @Resource
    private BizDemandComponent bizDemandComponent;

    @Resource
    private ProductBizDemandMapper productBizDemandMapper;

    @Resource
    private TaskProductDemandComponent taskProductDemandComponent;


    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求接收参数:{}", productDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        List<String> filtered;
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());
        } else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                filtered = allMyStaffWithSelf.stream().filter(a -> productDemandQueryList.getOwnerIds().contains(a)).collect(Collectors.toList());
            } else {
                filtered = allMyStaffWithSelf;
            }
            log.info("我和我的下属:{},过滤后:{}", allMyStaffWithSelf, filtered);
            if (CollectionUtils.isEmpty(filtered)) {
                //所选人员不在我的团队中
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setOwnerIds(filtered);
        } else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandQueryList.getAscription())) {
            List<BaseInfoResponse> baseInfos = innerUserPersonClient.getPersonByAccountNew(Lists.newArrayList(userInfo.getId()));

            String groupId = baseInfos.get(0).getDefaultGroup().getGroupId();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(groupId);

            if (!CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
                filtered = accountIds.stream().filter(a -> productDemandQueryList.getOwnerIds().contains(a)).collect(Collectors.toList());
            } else {
                filtered = accountIds;
            }
            log.info("用户默认部门id:{},同部门人员:{},过滤后:{}", groupId, accountIds, filtered);
            if (CollectionUtils.isEmpty(filtered)) {
                //所选人员不在我的部门中
                return BaseResult.success(ResultUtil.pageEmpty());
            }
            condition.setOwnerIds(filtered);
        } else if (AscriptionEnum.COPIER.name().equals(productDemandQueryList.getAscription())) {
            condition.setCopierId(userInfo.getId());
        }
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        List<ProductDemandListDO> productDemandListDO = productDemandComponent.list(condition);
        List<ProductDemandVO> productDemandVO = ProductDemandCopier.INSTANCE.convert(productDemandListDO);
        productDemandVO.forEach(p -> {
            p.setStatusName(ProductDemandStatusEnum.getTextByCode(p.getStatus()));
            p.setPriorityName(PriorityEnum.getTextByCode(p.getPriority()));
        });
        PageInfo<ProductDemandListDO> pageInfo = new PageInfo<>(productDemandListDO);
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
        ProductDemandDO productDemand = productDemandMapper.get(productDemandId);
        if (productDemand == null) {
            throw new BaseBizRuntimeException("找不到该产品需求");
        }
        if (!ProductDemandStatusEnum.WAITING.getCode().equals(productDemand.getStatus())
                && !ProductDemandStatusEnum.INCLUDED.getCode().equals(productDemand.getStatus())
                && !ProductDemandStatusEnum.PROGRESS.getCode().equals(productDemand.getStatus())) {
            throw new BaseBizRuntimeException("产品需求状态不是待排期、已列入项目、项目进行中,不能修改状态");
        }
        // 更新需求状态
        productDemand.setStatus(type);
        productDemandComponent.update(productDemand);

        if (ProductDemandStatusEnum.SUSPEND.getCode().equals(type)) {
            // 暂停,更新业务需求状态
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), false);
        }

        // 暂停or作废解除项目关联
        ProjectProductDemandDO productDemandDO = new ProjectProductDemandDO();
        productDemandDO.setProductDemandId(productDemandId);
        productDemandDO.setIsDeleted(true);
        projectProductDemandComponent.update(productDemandDO);
        if (ProductDemandStatusEnum.INVALID.getCode().equals(type)) {
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(productDemandId), true);
            // 作废解业务需求关联
            ProductBizDemandDO productBizDemandDO = new ProductBizDemandDO();
            productBizDemandDO.setProductDemandId(productDemandId);
            productBizDemandDO.setIsDeleted(true);
            productBizDemandComponent.update(productBizDemandDO);

        }
        //解除任务关联
        taskProductDemandComponent.unLinkIfProductDemandStatusAllChange(productDemandId);
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
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandDO productDemand = ProductDemandCopier.INSTANCE.convert(productDemandAddReq);
        productDemand.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        productDemand.setCreateManId(userInfo.getId());
        productDemand.setStatus(ProductDemandStatusEnum.WAITING.getCode());
        productDemand.setType(JSON.toJSONString(productDemandAddReq.getTypes()));
        productDemandMapper.insert(productDemand);

        fileComponent.add(productDemandAddReq.getFiles(), productDemand.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());

        personComponent.add(productDemandAddReq.getRecipients(), productDemand.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

        productBizDemandComponent.batchInsert(productDemand.getId(), productDemandAddReq.getBizDemandIds());
        if (productDemandAddReq.getProjectId() != null) {
            ProjectDO projectDO = projectMapper.get(productDemandAddReq.getProjectId());
            if (projectDO == null) {
                throw new BaseBizRuntimeException("找不到该项目");
            }
            projectProductDemandComponent.batchInsert(productDemandAddReq.getProjectId(), Lists.newArrayList(productDemand.getId()));
            productDemandComponent.updateProductDemandStatus(projectDO.getId(), projectDO.getStatus());
        }
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(ProductDemandModifyReq productDemandModifyReq) {
        log.info("产品需求修改接收参数:{}", productDemandModifyReq);
        ProductDemandDO productDemandDO = productDemandMapper.getByName(productDemandModifyReq.getName());
        if (productDemandDO != null && !productDemandDO.getId().equals(productDemandModifyReq.getId())) {
            throw new BaseBizRuntimeException("该产品需求名称已存在,请修改后重试");
        }
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
        return projectCmponent.page(condition, Lists.newArrayList());
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(ProductDemandLinkBizDemandQueryList productDemandLinkBizDemandQueryList) {
        log.info("产品需求-业务需求匹配接收参数:bizDemandQueryList={}", productDemandLinkBizDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<String> receiveManIdList = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        log.info("我和我的下属:receiveManIdList={}", receiveManIdList);
        BizDemandListCondition condition = BizDemandCopier.INSTANCE.convert(productDemandLinkBizDemandQueryList);
        List<String> filtered;
        if (!CollectionUtils.isEmpty(condition.getReceiveManIdList())) {
            filtered = receiveManIdList.stream().filter(a -> condition.getReceiveManIdList().contains(a)).collect(Collectors.toList());
        } else {
            filtered = receiveManIdList;
        }
        if (CollectionUtils.isEmpty(filtered)) {
            //所选人员不在我和我的下属中
            return BaseResult.success(ResultUtil.pageEmpty());
        }
        condition.setReceiveManIdList(filtered);
        List<Integer> status = productDemandLinkBizDemandQueryList.getStatusList();
        if (CollectionUtils.isEmpty(status)) {
            condition.setStatusList(Lists.newArrayList(
                    BizDemandStatusEnum.RECEIVED.getCode()
                    , BizDemandStatusEnum.INCLUDE_PROJECT.getCode()
                    , BizDemandStatusEnum.PROJECTING.getCode()
                    , BizDemandStatusEnum.REJECT.getCode()
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
        return bizDemandComponent.page(condition);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> linkOrUnLinkBizDemand(ProductBizDemandLinkReq bizDemandLinkReq) {
        log.info("关联or取消关联接收参数:bizDemandLinkReq={}", bizDemandLinkReq);
        List<Long> bizDemandIds = bizDemandLinkReq.getBizDemandIds();
        if (LinkOrUnLinkEnum.LINK.getCode().equals(bizDemandLinkReq.getType())) {
            productBizDemandComponent.batchInsert(bizDemandLinkReq.getProductDemandId(), bizDemandIds);

            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(bizDemandLinkReq.getProductDemandId()), false);
        } else {
            productDemandComponent.updateBizDemandStatusAsProductStatusChange(Lists.newArrayList(bizDemandLinkReq.getProductDemandId()), true);

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
            p.setPriorityText(PriorityEnum.getTextChineseByCode(p.getPriority()));
            p.setDeptName(deptMap.get(p.getDeptId()).getGroupName());
        });

        PageInfo<BizDemandListDO> pageInfo = new PageInfo<>(bizDemandList);
        PageQueryResult<BizDemandVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(bizDemandVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

}
