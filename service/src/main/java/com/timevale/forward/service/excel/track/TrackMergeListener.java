package com.timevale.forward.service.excel.track;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.enums.CellExtraTypeEnum;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellExtra;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/09 16:25
 */
@Slf4j
public class TrackMergeListener extends AnalysisEventListener<TrackRow> {

    private List<TrackEvent> trackEventList;

    public TrackMergeListener(List<TrackEvent> trackEventList) {
        this.trackEventList = trackEventList;
    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        if (extra.getFirstColumnIndex() != 1 || extra.getFirstRowIndex() < 3) {
            return;
        }
        TrackEvent trackEvent = new TrackEvent();
        trackEvent.setFirstRowIndex(extra.getFirstRowIndex());
        trackEvent.setLastRowIndex(extra.getLastRowIndex());
        trackEventList.add(trackEvent);
    }

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext context) {
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        CollectionUtil.sort(trackEventList, Comparator.comparingInt(TrackEvent::getFirstRowIndex));
        log.info("导入文件扫描完成");
    }

}
