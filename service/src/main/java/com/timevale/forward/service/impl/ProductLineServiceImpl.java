package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.facade.api.client.ProductLineService;
import com.timevale.forward.facade.api.result.ProductLineVO;
import com.timevale.forward.service.copy.ProductLineCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;

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

    @Override
    public BaseResult<List<ProductLineVO>> productLineList() {
        // 获取业务域及其负责人
        List<BizDomainDO> bizDomainDOList = bizDomainMapper.selectAllBizDomain();
        if(CollectionUtils.isEmpty(bizDomainDOList)){
            throw new BaseBizRuntimeException("业务域数据为空");
        }
        Map<Long, BizDomainDO> bizDomainIdMap = bizDomainDOList.stream()
                .collect(Collectors.toMap(BizDomainDO::getId, Function.identity()));

        // 获取产品线数据
        List<ProductLineDO> productLineDOList = productLineMapper.selectAllProductLine();
        if(CollectionUtils.isEmpty(productLineDOList)){
            throw new BaseBizRuntimeException("产品线数据为空");
        }
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDOList);

        // 填充产品线对应业务域负责人信息
        productLineVOList.forEach(e -> {
            BizDomainDO bizDomainDO = bizDomainIdMap.get(e.getBizDomainId());
            e.setBizDomainOwner(bizDomainDO.getOwner());
            e.setBizDomainOwnerId(bizDomainDO.getOwnerId());
        });

        return BaseResult.success(productLineVOList);
    }

    @Override
    public BaseResult<List<ProductLineVO>> getProductLines(Long projectId) {
        List<ProductLineDO> productLineDO = productLineMapper.get(projectId);
        List<ProductLineVO> productLineVOList = ProductLineCopier.INSTANCE.convert(productLineDO);
        return BaseResult.success(productLineVOList);
    }
}
