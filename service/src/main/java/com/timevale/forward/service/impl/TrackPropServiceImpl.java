package com.timevale.forward.service.impl;

import com.github.pagehelper.PageHelper;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventPropCondition;
import com.timevale.forward.dal.condition.TrackPropCondition;
import com.timevale.forward.dal.condition.TrackPropListCondition;
import com.timevale.forward.dal.dao.TrackEvenPropMapper;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.dao.TrackPropMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.dal.entity.TrackEventPropDO;
import com.timevale.forward.dal.entity.TrackPropDO;
import com.timevale.forward.facade.api.client.TrackPropService;
import com.timevale.forward.facade.api.query.TrackPropQueryList;
import com.timevale.forward.facade.api.request.TrackPropDeleteReq;
import com.timevale.forward.facade.api.request.TrackPropModifyReq;
import com.timevale.forward.facade.api.result.TrackPropVO;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackPropComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class TrackPropServiceImpl implements TrackPropService {

    /**
     * 流程审批人
     */
    @Value("${default.trackReviewer:chenran}")
    private String trackReviewer;


    @Resource
    private TrackPropComponent trackPropComponent;

    @Resource
    private TrackPropMapper trackPropMapper;

    @Resource
    private TrackEvenPropMapper trackEvenPropMapper;

    @Resource
    private TrackEventMapper trackEventMapper;


    @Override
    public BaseResult<PageQueryResult<TrackPropVO>> list(TrackPropQueryList trackPropQueryList) {
        log.info("埋点属性列表,参数:{}", trackPropQueryList);
        TrackPropListCondition condition = TrackPropCopier.INSTANCE.convert(trackPropQueryList);
        List<Integer> status = Lists.newArrayList(TrackStatusEnum.REVIEWING.getCode(), TrackStatusEnum.REVIEWED.getCode());
        condition.setStatus(status);
        PageHelper.startPage(trackPropQueryList.getPageNum(), trackPropQueryList.getPageSize(), CommonConstant.DEFAULT_ORDER_BY);
        return trackPropComponent.list(condition);
    }

    @Override
    public BaseResult<Boolean> modify(TrackPropModifyReq trackPropModifyReq) {
        log.info("埋点属性修改,参数:{}", trackPropModifyReq);
        TrackPropCondition c = TrackPropCondition.builder().id(trackPropModifyReq.getId()).build();
        TrackPropDO trackPropDO = trackPropMapper.select(c).get(0);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(userInfo.getId(), trackReviewer) || !TrackStatusEnum.REVIEWED.getCode().equals(trackPropDO.getStatus())) {
//            throw new BaseBizRuntimeException("状态为审核通过,且操作人为管理员才能编辑");
        }
        c = TrackPropCondition.builder().cnNames(Lists.newArrayList(trackPropModifyReq.getCnName())).build();
        List<TrackPropDO> trackPropDos = trackPropMapper.select(c);
        if(!CollectionUtils.isEmpty(trackPropDos)){
            throw new BaseBizRuntimeException("该属性中文名字已存在,不可保存");
        }
        trackPropDO.setCnName(trackPropModifyReq.getCnName());
        trackPropMapper.update(trackPropDO);
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> delete(TrackPropDeleteReq trackPropDeleteReq) {
        log.info("埋点属性删除,参数:{}", trackPropDeleteReq);
        TrackPropCondition c = TrackPropCondition.builder().id(trackPropDeleteReq.getId()).build();
        TrackPropDO trackPropDO = trackPropMapper.select(c).get(0);
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        if (!Objects.equals(userInfo.getId(), trackReviewer) || !TrackStatusEnum.REVIEWED.getCode().equals(trackPropDO.getStatus())) {
//            throw new BaseBizRuntimeException("状态为审核通过,且操作人为管理员才能删除");
        }
        TrackEventPropCondition cc = TrackEventPropCondition.builder().trackPropId(trackPropDeleteReq.getId()).isDeleted(false).build();
        List<Long> trackEventIds = trackEvenPropMapper.select(cc).stream().map(TrackEventPropDO::getTrackEventId).collect(Collectors.toList());
        if(!CollectionUtils.isEmpty(trackEventIds)){
            List<String> names = trackEventMapper.selectByIds(trackEventIds).stream().map(TrackEventDO::getCnName).collect(Collectors.toList());
//            throw new BaseBizRuntimeException("该属性被事件:"+names+"引用，请删除事件后再试。");

        }
        trackPropDO.setIsDeleted(true);
        trackPropMapper.update(trackPropDO);
        return BaseResult.success(true);
    }

}
