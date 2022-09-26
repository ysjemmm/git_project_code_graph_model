package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.ProductLineCondition;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.ProductLineService;
import com.timevale.forward.facade.api.query.ProductLineQueryList;
import com.timevale.forward.facade.api.request.ProductLineAddReq;
import com.timevale.forward.facade.api.request.ProductLineModifyReq;
import com.timevale.forward.facade.api.result.ModelVO;
import com.timevale.forward.facade.api.result.ProductLineModelVO;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.ModelCopier;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2021/12/13 17:07
 */
@Slf4j
@RestService
public class ProductLineServiceImpl implements ProductLineService {

    @Resource
    ProductLineMapper productLineMapper;

    @Resource
    BizDomainMapper bizDomainMapper;

    @Resource
    ModelMapper modelMapper;

    @Override
    public BaseResult<List<ProductLineVO>> productLineList() {

        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();

        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();

        return BaseResult.success(build(productLineDOList,bizDomainDOList));
    }

    @Override
    public BaseResult<PageQueryResult<ProductLineVO>> productLineList(ProductLineQueryList productLineQueryList) {

        ProductLineCondition condition = ProductLineCopier.INSTANCE.convert(productLineQueryList);
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();

        PageHelper.startPage(productLineQueryList.pageNum, productLineQueryList.pageSize, CommonConstant.DEFAULT_ORDER_BY);
        List<ProductLineDO> productLineDOList = productLineMapper.selectByCondition(condition);

        List<ProductLineVO> productLineVOList = build(productLineDOList, bizDomainDOList);
        PageInfo<ProductLineDO> pageInfo = new PageInfo<>(productLineDOList);
        PageQueryResult<ProductLineVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(productLineVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<ProductLineVO>> getProductLines(Long projectId) {
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDO);
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<List<ProductLineModelVO>> listProductLineModes() {

        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();

        List<ProductLineModelVO> productLineVOList = ProductLineCopier.INSTANCE.change(productLineDOList);

        List<ModelDO> modelDOList = modelMapper.selectAllModel();
        List<ModelVO> modelVOList = ModelCopier.INSTANCE.convert(modelDOList);
        Map<Long, List<ModelVO>> modelMap = modelVOList.stream().collect(Collectors.groupingBy(ModelVO::getProductLineId));

        productLineVOList.forEach(e -> {
            e.setModels(modelMap.get(e.getId()));
        });
        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<Boolean> add(ProductLineAddReq productLineAddReq) {
        ProductLineDO productLineDO = ProductLineCopier.INSTANCE.convert(productLineAddReq);
        productLineMapper.insert(productLineDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> update(ProductLineModifyReq productLineModifyReq) {
        ProductLineDO productLineDO = ProductLineCopier.INSTANCE.convert(productLineModifyReq);
        productLineMapper.update(productLineDO);
        return BaseResult.success(true);
    }

    private List<ProductLineVO> build(List<ProductLineDO> productLineDOList,List<BizDomainDO> bizDomainDOList){
        // 获取业务域及其负责人
        Map<Long, BizDomainDO> bizDomainIdMap = bizDomainDOList.stream()
                .collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));

        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDOList);

        // 填充产品线对应业务域负责人信息
        productLineVOList.forEach(e -> {
            BizDomainDO bizDomainDO = bizDomainIdMap.get(e.getBizDomainId());
            e.setBizDomainOwner(bizDomainDO.getOwner());
            e.setBizDomainOwnerId(bizDomainDO.getOwnerId());
            e.setBizDomainName(bizDomainDO.getName());
        });
        return productLineVOList;
    }
}
