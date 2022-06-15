package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventCondition;
import com.timevale.forward.dal.condition.TrackMapCondition;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.client.TrackMapService;
import com.timevale.forward.facade.api.request.TrackMapAddReq;
import com.timevale.forward.facade.api.request.TrackMapDeleteReq;
import com.timevale.forward.facade.api.result.TrackMapVO;
import com.timevale.forward.model.enums.TrackMapEnum;
import com.timevale.forward.service.copy.TrackMapCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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

    @Resource
    private TrackMapMapper trackMapMapper;

    @Resource
    private TrackEventMapper trackEventMapper;


    @Override
    public BaseResult<List<TrackMapVO>> trackMapList() {
        List<BizDomainDO> bizDomainDos = bizDomainMapper.selectAllBizDomain();
        List<ProductLineDO> productLineDos = productLineMapper.selectAllProductLine();
        List<ModelDO> modelDos = modelMapper.selectAllModel();

        List<TrackMapDO> trackMapDos = trackMapMapper.selectAllTrackMap();
        List<TrackMapDO> pages = trackMapDos.stream().filter(a -> TrackMapEnum.PAGE.getCode().equals(a.getLevel())).collect(Collectors.toList());
        List<TrackMapDO> elements = trackMapDos.stream().filter(a -> TrackMapEnum.ELEMENT.getCode().equals(a.getLevel())).collect(Collectors.toList());

        List<TrackMapVO> bizDomainVos = TrackMapCopier.INSTANCE.bizDomainConvert(bizDomainDos);
        List<TrackMapVO> productLineVos = TrackMapCopier.INSTANCE.productLineConvert(productLineDos);
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
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(TrackMapAddReq trackMapAddReq) {
        log.info("埋点地图新增,参数:{}", trackMapAddReq);
        TrackMapCondition c = TrackMapCondition.builder().parentId(trackMapAddReq.getParentId()).level(trackMapAddReq.getLevel()).build();
        List<TrackMapDO> trackMapDos = trackMapMapper.select(c);
        boolean match = trackMapDos.stream().anyMatch(a -> a.getName().equals(trackMapAddReq.getName()));
        if(match){
            throw new BaseBizRuntimeException("该菜单名称已存在,请修改后重试");
        }
        TrackMapDO trackMapDO = TrackMapCopier.INSTANCE.convert(trackMapAddReq);
        trackMapMapper.insert(trackMapDO);
        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(TrackMapDeleteReq trackMapDeleteReq) {
        TrackMapCondition c = TrackMapCondition.builder().parentId(trackMapDeleteReq.getId()).build();
        List<TrackMapDO> trackMapDos = trackMapMapper.select(c);
        if(!CollectionUtils.isEmpty(trackMapDos)){
            throw new BaseBizRuntimeException("该菜单下有子菜单不能删除");
        }

        TrackEventCondition cc = TrackEventCondition.builder().trackMapId(trackMapDeleteReq.getId()).build();
        List<TrackEventDO> trackEventDos = trackEventMapper.select(cc);
        if(!CollectionUtils.isEmpty(trackEventDos)){
            throw new BaseBizRuntimeException("请联系数据产品经理删除该分类下所有事件后再删除");
        }
        trackMapMapper.delete(trackMapDeleteReq.getId());
        return BaseResult.success(true);
    }
}
