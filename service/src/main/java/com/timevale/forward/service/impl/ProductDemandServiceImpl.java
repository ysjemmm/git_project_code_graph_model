package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.ProductDemandQueryList;
import com.timevale.forward.facade.api.request.ProductDemandAddReq;
import com.timevale.forward.facade.api.request.ProductDemandModifyReq;
import com.timevale.forward.facade.api.result.BizDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.facade.api.result.ProjectVO;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.model.enums.ProductDemandStatusEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.copy.ProductDemandCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
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


    @Override
    public BaseResult<PageQueryResult<ProductDemandVO>> list(ProductDemandQueryList productDemandQueryList) {
        log.info("产品需求接收参数:{}", productDemandQueryList);
//        PageHelper.startPage(projectQueryList.getPageNum(), projectQueryList.getPageSize());
        PageQueryResult<ProductDemandVO> result = new PageQueryResult<>();
        result.setResultList(Lists.newArrayList(new ProductDemandVO()));
        return BaseResult.success(result);
    }

    @Override
    public BaseResult<Boolean> updateStatus(Long productDemandId, Integer type) {
        log.info("产品需求暂停或开启收参数:productDemandId={},type={}", productDemandId, type);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> add(ProductDemandAddReq productDemandAddReq) {
        log.info("产品需求新增接收参数:{}", productDemandAddReq);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        ProductDemandDO demandDO = ProductDemandCopier.INSTANCE.convert(productDemandAddReq);
        demandDO.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
        demandDO.setCreateManId(userInfo.getId());
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
    public BaseResult<Boolean> modify(ProductDemandModifyReq productDemandModifyReq) {
        log.info("产品需求修改接收参数:{}", productDemandModifyReq);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<ProductDemandDetailVO> get(Long productDemandId) {
        ProductDemandDetailVO productDemandDetailVO = new ProductDemandDetailVO();
        log.info("产品需求查看接收参数:productDemandId={}", productDemandId);
        List<FileDO> fileDO = fileComponent.select(productDemandId, FileTypeEnum.PRODUCT_DEMAND.getCode());
        productDemandDetailVO.setFiles(FileCopier.INSTANCE.transform(fileDO));
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
