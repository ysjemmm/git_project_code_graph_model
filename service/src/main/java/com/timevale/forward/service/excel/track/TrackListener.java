package com.timevale.forward.service.excel.track;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/08/09 16:25
 */
public class TrackListener extends AnalysisEventListener<TrackRow> {

    private final List<TrackEvent> trackEventList = new ArrayList<>();

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext analysisContext) {
        if(BeanUtil.isEmpty(trackRow)) {
            return;
        }
        String firstClassify = trackRow.getFirstClassify();
        if (StrUtil.isNotEmpty(firstClassify)) {
            TrackEvent trackEvent = new TrackEvent();
            BeanUtil.copyProperties(trackRow, trackEvent);
            trackEventList.add(trackEvent);
        }
        List<TrackProp> trackPropList = CollectionUtil.getLast(trackEventList).getTrackPropList();
        TrackProp trackProp = new TrackProp();
        BeanUtil.copyProperties(trackRow, trackProp);
        trackPropList.add(trackProp);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
    }

    

}
