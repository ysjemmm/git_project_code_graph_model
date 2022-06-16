package com.timevale.forward.service.component.impl;

import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.PageInfo;
import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.condition.TrackEventListCondition;
import com.timevale.forward.dal.dao.TrackEventMapper;
import com.timevale.forward.dal.entity.TrackEventDO;
import com.timevale.forward.facade.api.result.TrackEventVO;
import com.timevale.forward.model.enums.EnvEnum;
import com.timevale.forward.model.enums.PlatformTypeEnum;
import com.timevale.forward.model.enums.TrackStatusEnum;
import com.timevale.forward.service.component.TrackEventComponent;
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.utils.ResultUtil;
import com.timevale.mandarin.common.result.PageQueryResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class TrackEventComponentImpl implements TrackEventComponent {

    @Resource
    private TrackEventMapper trackEventMapper;

    @Override
    public BaseResult<PageQueryResult<TrackEventVO>> list(TrackEventListCondition condition) {
        log.info("埋点事件列表,参数:{}", condition);
        List<TrackEventDO> list = trackEventMapper.list(condition);
        List<TrackEventVO> trackEventVOList = TrackEventCopier.INSTANCE.convert(list);
        trackEventVOList.forEach(a->{
            a.setStatusName(TrackStatusEnum.getTextByCode(a.getStatus()));
            a.setEnvNames(EnvEnum.getTextByCode(JSONObject.parseArray(a.getEnv(),Integer.class)));
            a.setPlatformNames(PlatformTypeEnum.getTextByCode(JSONObject.parseArray(a.getPlatform(),Integer.class)));
        });
        PageInfo<TrackEventDO> pageInfo = new PageInfo<>(list);
        PageQueryResult<TrackEventVO> pageQueryResult = new PageQueryResult<>();
        pageQueryResult.setResultList(trackEventVOList);
        ResultUtil.fillPageInfo(pageQueryResult, pageInfo);
        return BaseResult.success(pageQueryResult);
    }
}
