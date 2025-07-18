package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.model.to.PdLineDomainTO;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author by YangXu
 * @date 2023/03/29 17:22
 */
@Slf4j
@LogPoint
@Component
@RequiredArgsConstructor
public class ProductLineComponent {
    private final BizDomainMapper bizDomainMapper;
    private final ProductLineMapper productLineMapper;

    /**
     * 通过产品线id查询 产品线和关联的业务域信息
     *
     * @param productLineIds 产品线id集合
     * @return 产品线和关联的业务域信息
     */
    public List<PdLineDomainTO> getByIds(Collection<Long> productLineIds) {
        List<PdLineDomainTO> result = new ArrayList<>();

        if (CollUtil.isEmpty(productLineIds)) {
            return result;
        }

        // 查询产品线
        List<ProductLineDO> productLineDOs = productLineMapper.getByIds(productLineIds);
        if (CollUtil.isEmpty(productLineDOs)) {
            return result;
        }

        // 查询对应的业务域
        Set<Long> bizDomainIds = productLineDOs.stream().map(ProductLineDO::getBizDomainId).collect(Collectors.toSet());
        List<BizDomainDO> bizDomainDOs = bizDomainMapper.getByIds(bizDomainIds);

        ImmutableMap<Long, BizDomainDO> bizDomainMap = Maps.uniqueIndex(bizDomainDOs, BaseDO::getId);
        for (ProductLineDO productLineDO : productLineDOs) {
            Long productLineId = productLineDO.getId();
            String productLineName = productLineDO.getName();
            Long bizDomainId = productLineDO.getBizDomainId();

            String bizDomainName = Optional.ofNullable(bizDomainMap.get(bizDomainId))
                    .flatMap(e -> Optional.ofNullable(e.getName()))
                    .orElse("");

            if (StrUtil.isEmpty(bizDomainName)) {
                log.error("[ProductLineComponent.getByIds]productLine {} can't find bizDomain", productLineId);
                continue;
            }

            result.add(new PdLineDomainTO(productLineId, productLineName, bizDomainId, bizDomainName));
        }

        return result;
    }

    /**
     * 通过产品线id查询 产品线和关联的业务域信息，封装为Map
     *
     * @param productLineIds 产品id
     * @return Key为产品线id，Value为产品线和关联的业务域信息
     */
    public Map<Long, PdLineDomainTO> getMapByIds(Collection<Long> productLineIds) {
        List<PdLineDomainTO> pdLineDomainTOs = getByIds(productLineIds);
        return pdLineDomainTOs.stream().collect(Collectors.toMap(PdLineDomainTO::getProductLineId, Function.identity()));
    }

    public List<String> getProductLineOwnersInBizDomain(Long bizDomainId) {
        Set<String> owners = new HashSet<>();
        BizDomainDO bizDomainDO = bizDomainMapper.selectById(bizDomainId);
        List<ProductLineDO> productLineDOS = productLineMapper.getBizDomainId(bizDomainId);
        owners.add(bizDomainDO.getOwnerId());
        productLineDOS.forEach(productLineDO -> owners.add(productLineDO.getOwnerId()));
        return owners.stream().collect(Collectors.toList());
    }

}
