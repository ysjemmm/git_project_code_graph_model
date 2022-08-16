package com.timevale.forward.service.excel.track;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.metadata.CellExtra;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author by YangXu
 * @date 2022/08/09 16:25
 */
@Slf4j
public class TrackMergeListener extends AnalysisEventListener<TrackRow> {

    private List<TrackEvent> trackEventList;
    private Map<Integer, Integer> indexMap = new HashMap<>();

    public TrackMergeListener(List<TrackEvent> trackEventList) {
        this.trackEventList = trackEventList;
    }

    @Override
    public void extra(CellExtra extra, AnalysisContext context) {
        if (extra.getFirstColumnIndex() != 1 || extra.getFirstRowIndex() < 3) {
            return;
        }
        Integer firstRowIndex = extra.getFirstRowIndex();
        Integer lastRowIndex = extra.getLastRowIndex();
        for (int i = firstRowIndex + 1; i <= lastRowIndex ; i++) {
            indexMap.remove(i);
        }
        indexMap.put(firstRowIndex, lastRowIndex);
    }

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();
        indexMap.put(rowIndex, rowIndex);

        TrackEvent trackEvent = new TrackEvent();
        trackEvent.setFirstRowIndex(rowIndex);
        trackEvent.setLastRowIndex(rowIndex);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        indexMap.forEach((k,v) -> {
            TrackEvent trackEvent = new TrackEvent();
            trackEvent.setFirstRowIndex(k);
            trackEvent.setLastRowIndex(v);
            trackEventList.add(trackEvent);
        });
        CollectionUtil.sort(trackEventList, Comparator.comparingInt(TrackEvent::getFirstRowIndex));
        log.info("导入文件扫描完成");
    }

}
