package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.TrackMapDO;
import com.timevale.forward.facade.api.client.TrackMapService;
import com.timevale.forward.facade.api.request.TrackMapAddReq;
import com.timevale.forward.facade.api.request.TrackMapDeleteReq;
import com.timevale.forward.facade.api.result.TrackMapVO;
import com.timevale.forward.service.copy.TrackMapCopier;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

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
public class TrackMapServiceImpl implements TrackMapService {

    @Resource
    private ProductLineMapper productLineMapper;

    @Resource
    private BizDomainMapper bizDomainMapper;

    @Resource
    private ModelMapper modelMapper;


    @Override
    public BaseResult<List<TrackMapVO>> trackMapList() {

        List<BizDomainDO> bizDomainDos = Lists.newArrayList(new BizDomainDO(){{setId(1L);setName("业务域");}});
        List<ProductLineDO> productLineos = Lists.newArrayList(new ProductLineDO(){{setId(2L);setBizDomainId(1L);setName("产品线");}});
        List<ModelDO> modelDos =Lists.newArrayList(new ModelDO(){{setId(3L);setProductLineId(2L);setName("模块");}});
        List<TrackMapDO> pages = Lists.newArrayList(new TrackMapDO(){{setId(4L);setParentId(3L);setName("页面");}});
        List<TrackMapDO> elements = Lists.newArrayList(new TrackMapDO(){{setId(5L);setParentId(4L);setName("元素");}});

        List<TrackMapVO> bizDomainVos = TrackMapCopier.INSTANCE.bizDomainConvert(bizDomainDos);
        List<TrackMapVO> productLineVos = TrackMapCopier.INSTANCE.productLineConvert(productLineos);
        List<TrackMapVO> modelVos = TrackMapCopier.INSTANCE.modelConvert(modelDos);
        List<TrackMapVO> pageVos = TrackMapCopier.INSTANCE.convert(pages);
        List<TrackMapVO> elementVos = TrackMapCopier.INSTANCE.convert(elements);

        elementVos.forEach(a -> {
            a.setLevel(5);
        });

        Map<Long, List<TrackMapVO>> elementMap = elementVos.stream().collect(Collectors.groupingBy(TrackMapVO::getParentId));
        pageVos.forEach(a -> {
            a.setChildren(elementMap.get(a.getId()));
            a.setLevel(4);
        });

        Map<Long, List<TrackMapVO>> pageMap = pageVos.stream().collect(Collectors.groupingBy(TrackMapVO::getParentId));
        modelVos.forEach(a -> {
            a.setChildren(pageMap.get(a.getId()));
            a.setLevel(3);
        });

        Map<Long, List<TrackMapVO>> modeMap = modelVos.stream().collect(Collectors.groupingBy(TrackMapVO::getParentId));
        productLineVos.forEach(a -> {
            a.setChildren(modeMap.get(a.getId()));
            a.setLevel(2);
        });

        Map<Long, List<TrackMapVO>> productLineMap = productLineVos.stream().collect(Collectors.groupingBy(TrackMapVO::getParentId));
        bizDomainVos.forEach(a -> {
            a.setChildren(productLineMap.get(a.getId()));
            a.setLevel(1);
        });

        return BaseResult.success(bizDomainVos);
    }

    @Override
    public BaseResult<Boolean> add(TrackMapAddReq trackMapAddReq) {
        return BaseResult.success();
    }

    @Override
    public BaseResult<Boolean> delete(TrackMapDeleteReq trackMapDeleteReq) {
        return BaseResult.success();
    }
}
