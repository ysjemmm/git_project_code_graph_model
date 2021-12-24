package com.timevale.forward.service.impl;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductBizDemandCondition;
import com.timevale.forward.dal.dao.ProductBizDemandMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.BizDemandLinkProductDemandListDO;
import com.timevale.forward.dal.entity.ProductBizDemandDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.BizDemandProductDemandService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.query.BizDemandProductDemandQueryList;
import com.timevale.forward.facade.api.result.BizDemandLinkProductDemandVO;
import com.timevale.forward.facade.api.result.ProductDemandDetailVO;
import com.timevale.forward.facade.api.result.ProductDemandVO;
import com.timevale.forward.service.copy.BizDemandCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.query.QueryBase;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/23 18:03
 */
@Slf4j
@RestService
public class BizDemandProductDemandServiceImpl implements BizDemandProductDemandService {

    @Resource
    ProductBizDemandMapper productBizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    ProductDemandService productDemandService;

    @Override
    public BaseResult<PageQueryResult<BizDemandLinkProductDemandVO>> linkProductDemandList(BizDemandProductDemandQueryList bizDemandProductDemandQueryList) {
        // 开始分页
        PageHelper.startPage(bizDemandProductDemandQueryList.pageNum, bizDemandProductDemandQueryList.pageSize);

        List<BizDemandLinkProductDemandListDO> doList = productDemandMapper.selectByBizDemandId(bizDemandProductDemandQueryList.getBizDemandId());
        List<BizDemandLinkProductDemandVO> voList = BizDemandCopier.INSTANCE.transform(doList);

        return BaseResult.success(BizDemandCopier.INSTANCE.transform(ResultUtil.pageSuccess(new PageInfo<>(voList))));
    }

    @Override
    public BaseResult<ProductDemandDetailVO> getProductDemand(Long productDemandId) {
        return productDemandService.get(productDemandId);
    }

    @Override
    public BaseResult<Boolean> unlinkProductDemand(Long bizDemandId, Long productDemandId) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 查询对应数据
        List<ProductBizDemandDO> list = productBizDemandMapper.select(ProductBizDemandCondition.builder()
                .bizDemandId(bizDemandId)
                .productDemandId(productDemandId)
                .isDeleted(false)
                .build());

        if(list == null){
            throw new BaseBizRuntimeException("不存在对应的关联关系");
        }

        ProductBizDemandDO productBizDemandDO = list.get(0);
        productBizDemandDO.setModifyManId(userInfo.getAlias());
        productBizDemandDO.setModifyManId(userInfo.getId());

        productBizDemandMapper.delete(productBizDemandDO);


        return BaseResult.success(true);
    }
}
