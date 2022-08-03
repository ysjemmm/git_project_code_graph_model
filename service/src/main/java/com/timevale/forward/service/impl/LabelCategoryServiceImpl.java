package com.timevale.forward.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.LabelCategoryBizDomainMapper;
import com.timevale.forward.dal.dao.LabelCategoryMapper;
import com.timevale.forward.dal.entity.LabelCategoryBizDomainDO;
import com.timevale.forward.dal.entity.LabelCategoryDO;
import com.timevale.forward.facade.api.client.LabelCategoryService;
import com.timevale.forward.facade.api.query.LabelCategoryQueryList;
import com.timevale.forward.facade.api.query.LabelInCategoryQueryList;
import com.timevale.forward.facade.api.request.LabelCategoryAddReq;
import com.timevale.forward.facade.api.request.LabelCategoryModifyReq;
import com.timevale.forward.facade.api.result.LabelCategoryDetailVO;
import com.timevale.forward.facade.api.result.LabelCategorySimpleVO;
import com.timevale.forward.facade.api.result.LabelCategoryVO;
import com.timevale.forward.model.enums.BizTypeEnum;
import com.timevale.forward.service.copy.LabelCategoryCopier;
import com.timevale.forward.service.utils.ResultUtil;
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
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class LabelCategoryServiceImpl implements LabelCategoryService {

    @Resource
    private LabelCategoryMapper labelCategoryMapper;

    @Resource
    private LabelCategoryBizDomainMapper labelCategoryBizDomainMapper;


    @Override
    public BaseResult<PageQueryResult<LabelCategoryVO>> list(LabelCategoryQueryList labelCategoryQueryList) {
        log.info("类别列表,参数:{}", labelCategoryQueryList);
        List<LabelCategoryVO>labelCategoryVOList=new ArrayList<>();
        List<LabelCategoryDO>labelCategoryDOList=new ArrayList<>();

        PageInfo<LabelCategoryDO> pageInfo = new PageInfo<>(labelCategoryDOList);
        PageQueryResult<LabelCategoryVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(labelCategoryVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getAll() {
        List<LabelCategorySimpleVO> labelSimpleVOList = Lists.newArrayList(new LabelCategorySimpleVO());
        return BaseResult.success(labelSimpleVOList);
    }

    @Override
    public BaseResult<List<LabelCategorySimpleVO>> getLabelInCategory(LabelInCategoryQueryList labelInCategoryQueryList) {
        log.info("类别下的标签,参数:{}", labelInCategoryQueryList);
        List<LabelCategorySimpleVO> labelSimpleVOList = Lists.newArrayList(new LabelCategorySimpleVO());
        return BaseResult.success(labelSimpleVOList);
    }

    @Override
    public BaseResult<LabelCategoryDetailVO> get(Long categoryId) {
        log.info("类别查看,参数:{}", categoryId);
        LabelCategoryDO labelCategoryDO = labelCategoryMapper.get(categoryId);
        if(labelCategoryDO==null){
            throw new BaseBizRuntimeException("找不到该标签类别");
        }
        LabelCategoryDetailVO labelCategoryDetailVO = LabelCategoryCopier.INSTANCE.convert(labelCategoryDO);
        List<LabelCategoryBizDomainDO> labelCategoryBizDomainDos = labelCategoryBizDomainMapper.get(categoryId);
        List<Long> bizDomainIds = labelCategoryBizDomainDos.stream().map(LabelCategoryBizDomainDO::getBizDomainId).collect(Collectors.toList());
        labelCategoryDetailVO.setBizDomainIds(bizDomainIds);
        return BaseResult.success(new LabelCategoryDetailVO());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> delete(Long categoryId) {
        log.info("类别删除,参数:{}", categoryId);
        return BaseResult.success(true);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> add(LabelCategoryAddReq labelCategoryAddReq) {
        log.info("类别新增,参数:{}", labelCategoryAddReq);
        //同模块下,类别唯一
        LabelCategoryDO labelCategoryDO = LabelCategoryCopier.INSTANCE.convert(labelCategoryAddReq);
        checkBeforeInsert(labelCategoryDO);

        labelCategoryMapper.insert(labelCategoryDO);

        addRelation(labelCategoryAddReq.getBizDomainIds(),labelCategoryDO.getId());


        return BaseResult.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseResult<Boolean> modify(LabelCategoryModifyReq labelCategoryModifyReq) {
        log.info("类别修改,参数:{}", labelCategoryModifyReq);
        return BaseResult.success(true);
    }

    private void checkBeforeInsert(LabelCategoryDO labelCategoryDO){
        List<Integer> newType = JSONObject.parseArray(labelCategoryDO.getType(), Integer.class);
        List<LabelCategoryDO> categoryDOList = labelCategoryMapper.getByName(labelCategoryDO.getName());
        List<LabelCategoryDO> filter = categoryDOList.stream().filter(a -> !Objects.equals(labelCategoryDO.getId(), a.getId())).collect(Collectors.toList());
        filter.forEach(a->{
            List<Integer> oldType = JSONObject.parseArray(a.getType(), Integer.class);
            oldType.retainAll(newType);
            if(CollectionUtils.isNotEmpty(oldType)){
                throw new BaseBizRuntimeException("类别名称:"+labelCategoryDO.getName()+",在应用模块"+ BizTypeEnum.getTextByCode(oldType)+"中已存在,请修改后重试");
            }
        });
    }

    private void addRelation(List<Long> bizDomainIds, Long labelCategoryId) {
        List<LabelCategoryBizDomainDO> lcbd = bizDomainIds.stream().map(a -> {
            LabelCategoryBizDomainDO o = new LabelCategoryBizDomainDO();
            o.setBizDomainId(a);
            o.setLabelCategoryId(labelCategoryId);
            return o;
        }).collect(Collectors.toList());
        labelCategoryBizDomainMapper.batchInsert(lcbd);
    }

//    private void delRelation(List<TrackPropDO> trackPropDOList, Long trackEventId) {
//        if (org.springframework.util.CollectionUtils.isEmpty(trackPropDOList)) {
//            TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
//            trackEventPropDO.setTrackEventId(trackEventId);
//            trackEventPropDO.setIsDeleted(true);
//            trackEvenPropMapper.update(trackEventPropDO);
//            return;
//        }
//
//        List<Long> newPropIds = trackPropDOList.stream().map(TrackPropDO::getId).collect(Collectors.toList());
//        //新增
//        List<Long> oldPropIds = addRelation(trackPropDOList, trackEventId);
//        //删除
//        oldPropIds.forEach(a -> {
//            if (!newPropIds.contains(a)) {
//                TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
//                trackEventPropDO.setTrackEventId(trackEventId);
//                trackEventPropDO.setTrackPropId(a);
//                trackEventPropDO.setIsDeleted(true);
//                trackEvenPropMapper.update(trackEventPropDO);
//            }
//        });
//    }

}
