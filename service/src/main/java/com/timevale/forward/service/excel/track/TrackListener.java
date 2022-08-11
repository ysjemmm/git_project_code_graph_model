package com.timevale.forward.service.excel.track;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/08/09 16:25
 */
@Slf4j
public class TrackListener extends AnalysisEventListener<TrackRow> {

    private int headRow = 3;
    private final List<TrackEvent> trackEventList;

    public TrackListener(List<TrackEvent> trackEventList) {
        this.trackEventList = trackEventList;
    }

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();

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
        log.info("导入文件读取完成");
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        if (headRow-- > 1) {
            return;
        }
        Field[] fields = ReflectUtil.getFields(TrackRow.class);
        for (Field field : fields) {
            ExcelProperty annotation = AnnotationUtil.getAnnotation(field, ExcelProperty.class);
            int index = annotation.index();
            String value = annotation.value()[0];
            String headMapValue = headMap.get(index);
            AssertUtil.checkState(ObjectUtil.equal(value, headMapValue), "导入文件列表格式错误，请勿修改模板格式");
        }
    }
}
