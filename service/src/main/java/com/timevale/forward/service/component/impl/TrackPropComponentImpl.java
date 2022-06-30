package com.timevale.forward.service.component.impl;

import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventPropCondition;
import com.timevale.forward.dal.condition.TrackPropCondition;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.dal.entity.TrackEventPropDO;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.model.enums.TrackPropTypeEnum;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class TrackPropComponentImpl implements TrackPropComponent {

    @Resource
    private TrackPropMapper trackPropMapper;

    @Resource
    private TrackEvenPropMapper trackEvenPropMapper;

    @Override
    public BaseResult<PageQueryResult<TrackPropVO>> list(TrackPropListCondition condition) {
        log.info("埋点事件列表,参数:{}", condition);
        List<TrackPropDO> list = trackPropMapper.list(condition);
        List<TrackPropVO> trackEventVOList = TrackPropCopier.INSTANCE.convert(list);
        trackEventVOList.forEach(a -> {
            a.setStatusName(TrackStatusEnum.getTextByCode(a.getStatus()));
        });
        PageInfo<TrackPropDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<TrackPropVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(trackEventVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }

    @Override
    public BaseResult<Boolean> add(List<TrackPropDO> trackPropDOList, Long trackEventId) {

        checkBeforeInsert(trackPropDOList);

        addRelation(trackPropDOList, trackEventId);

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> modify(List<TrackPropDO> trackPropDOList, Long trackEventId) {

        checkBeforeInsert(trackPropDOList);

        delRelation(trackPropDOList, trackEventId);

        return BaseResult.success(true);
    }

    @Override
    public List<TrackPropVO> get(Long trackEventId) {
        TrackEventPropCondition c = TrackEventPropCondition.builder().trackEventId(trackEventId).isDeleted(false).build();
        List<TrackEventPropDO> oldTrackEventPropDOList = trackEvenPropMapper.select(c);
        List<Long> oldPropIds = oldTrackEventPropDOList.stream().map(TrackEventPropDO::getTrackPropId).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(oldPropIds)) {
            return Lists.emptyList();
        }
        List<TrackPropDO> list = trackPropMapper.selectByIds(oldPropIds);
        List<TrackPropDO> filter = list.stream().filter(a -> !TrackPropTypeEnum.DEFAULT.getCode().equals(a.getType())).collect(Collectors.toList());
        List<TrackPropVO> trackEventVOList = TrackPropCopier.INSTANCE.convert(filter);

        trackEventVOList.forEach(a -> {
            a.setStatusName(TrackStatusEnum.getTextByCode(a.getStatus()));
        });
        return trackEventVOList;
    }

    private List<Long> addRelation(List<TrackPropDO> trackPropDOList, Long trackEventId) {
        List<Long> newPropIds = trackPropDOList.stream().filter(a -> !TrackPropTypeEnum.DEFAULT.getCode().equals(a.getType())).map(TrackPropDO::getId).collect(Collectors.toList());

        TrackEventPropCondition c = TrackEventPropCondition.builder().trackEventId(trackEventId).isDeleted(false).build();
        List<TrackEventPropDO> oldTrackEventPropDOList = trackEvenPropMapper.select(c);
        List<Long> oldPropIds = oldTrackEventPropDOList.stream().map(TrackEventPropDO::getTrackPropId).collect(Collectors.toList());

        newPropIds.removeAll(oldPropIds);
        List<TrackEventPropDO> list = newPropIds.stream().map(t -> {
            TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
            trackEventPropDO.setTrackEventId(trackEventId);
            trackEventPropDO.setTrackPropId(t);
            return trackEventPropDO;
        }).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(list)) {
            trackEvenPropMapper.batchInsert(list);
        }
        return oldPropIds;
    }

    private void delRelation(List<TrackPropDO> trackPropDOList, Long trackEventId) {
        if (CollectionUtils.isEmpty(trackPropDOList)) {
            TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
            trackEventPropDO.setTrackEventId(trackEventId);
            trackEventPropDO.setIsDeleted(true);
            trackEvenPropMapper.update(trackEventPropDO);
            return;
        }

        List<Long> newPropIds = trackPropDOList.stream().map(TrackPropDO::getId).collect(Collectors.toList());
        //新增
        List<Long> oldPropIds = addRelation(trackPropDOList, trackEventId);
        //删除
        oldPropIds.forEach(a -> {
            if (!newPropIds.contains(a)) {
                TrackEventPropDO trackEventPropDO = new TrackEventPropDO();
                trackEventPropDO.setTrackEventId(trackEventId);
                trackEventPropDO.setTrackPropId(a);
                trackEventPropDO.setIsDeleted(true);
                trackEvenPropMapper.update(trackEventPropDO);
            }
        });
    }

    private void checkBeforeInsert(List<TrackPropDO> trackPropDOList) {

        if (CollectionUtils.isEmpty(trackPropDOList)) {
            return;
        }

        //新加的属性,或不通过的属性
        List<TrackPropDO> filter = trackPropDOList.stream().filter(a -> a.getId() == null
                || TrackStatusEnum.REVIEW_FAIL.getCode().equals(a.getStatus())).collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filter)) {
            return;
        }
        List<Integer> status = Lists.newArrayList(TrackStatusEnum.REVIEWING.getCode(), TrackStatusEnum.REVIEWED.getCode());

        List<String> newCnNames = filter.stream().map(TrackPropDO::getCnName).collect(Collectors.toList());
        //sql大小写不敏感,程序判断
        TrackPropCondition c = TrackPropCondition.builder().cnNames(newCnNames).status(status).build();
        List<TrackPropDO> trackPropDos = trackPropMapper.select(c);
        List<String> oldCnName = trackPropDos.stream().map(TrackPropDO::getCnName).collect(Collectors.toList());
        newCnNames.retainAll(oldCnName);
        if (!CollectionUtils.isEmpty(newCnNames)) {
            throw new BaseBizRuntimeException("属性中文名 " + newCnNames + " 已存在,请修改后重试");
        }

        List<String> newEgNames = filter.stream().map(TrackPropDO::getEgName).collect(Collectors.toList());

        c = TrackPropCondition.builder().egNames(newEgNames).status(status).build();
        trackPropDos = trackPropMapper.select(c);
        List<String> oldEgName = trackPropDos.stream().map(TrackPropDO::getEgName).collect(Collectors.toList());
        newEgNames.retainAll(oldEgName);
        if (!CollectionUtils.isEmpty(newEgNames)) {
            throw new BaseBizRuntimeException("属性英文名 " + newEgNames + " 已存在,请修改后重试");
        }

        filter = trackPropDOList.stream().filter(a -> a.getId() == null).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(filter)) {
            trackPropMapper.batchInsert(filter);
        }
    }
}
