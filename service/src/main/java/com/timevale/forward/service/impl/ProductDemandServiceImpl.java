package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductDemandListCondition;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.dal.entity.ProductDemandListDO;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.component.ProductDemandComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.GroupModel;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

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

    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {

        log.info("产品需求接收参数:{}", productDemandQueryList);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        log.info("人员信息:{}", userInfo);
        PageHelper.startPage(productDemandQueryList.getPageNum(), productDemandQueryList.getPageSize(),CommonConstant.DEFAULT_ORDER_BY);
        ProductDemandListCondition condition = ProductDemandCopier.INSTANCE.convert(productDemandQueryList);
        if (CollectionUtils.isEmpty(productDemandQueryList.getOwnerIds())) {
            condition.setOwnerIds(new ArrayList<>());
        }
        if (AscriptionEnum.CURRENT_USER.name().equals(productDemandQueryList.getAscription())) {
            condition.getOwnerIds().add(userInfo.getId());

        }else if (AscriptionEnum.TEAM.name().equals(productDemandQueryList.getAscription())) {
            List<String> allMyStaffWithSelf = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId());
            log.info("我和我的下属:{}", allMyStaffWithSelf);
            condition.getOwnerIds().addAll(allMyStaffWithSelf);
        }else if (AscriptionEnum.DEPARTMENT.name().equals(productDemandQueryList.getAscription())) {
            GroupModel defaultGroup = userInfo.getDefaultGroup();
            List<String> accountIds = innerUserPersonClient.getAllByGroupId(defaultGroup.getGroupId());
            log.info("用户默认部门id:{},同部门人员:{}", defaultGroup.getGroupId(),accountIds);
            condition.getOwnerIds().addAll(accountIds);
        }else if (AscriptionEnum.COPIER.name().equals(productDemandQueryList.getAscription())) {
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
        log.info("产品需求暂停或开启收参数:productDemandId={},type={}", productDemandId, type);
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
        productDemandMapper.insert(demandDO);

        if(CollectionUtils.isNotEmpty(productDemandAddReq.getFiles())){
            fileComponent.add(productDemandAddReq.getFiles(),demandDO.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        }
        if(CollectionUtils.isNotEmpty(productDemandAddReq.getRecipients())){
            personComponent.add(productDemandAddReq.getRecipients(),demandDO.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
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
        productDemandMapper.update(demandDO);
        // 附件
        fileComponent.update(productDemandModifyReq.getFiles(),demandDO.getId(), FileTypeEnum.PRODUCT_DEMAND.getCode());
        // 抄送人
        personComponent.update(productDemandModifyReq.getRecipients(),demandDO.getId(), PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> get(Long productDemandId) {
        log.info("产品需求查看接收参数:productDemandId={}", productDemandId);
        ProductDemandDetailVO productDemandDetailVO =productDemandComponent.get(productDemandId);
        return BaseResult.success(productDemandDetailVO);
    }

    @Override
    public BaseResult<PageQueryResult<ProjectVO>> matchProjectList(Long productDemandId) {
        log.info("产品需求匹配接收参数:productDemandId={}", productDemandId);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProjectVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProjectVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<PageQueryResult<BizDemandVO>> matchBizDemandList(Long productDemandId) {
        log.info("业务需求匹配接收参数:productDemandId={}", productDemandId);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<BizDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new BizDemandVO()));
        return BaseResult.success(result);
    }

}
