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
import com.timevale.forward.service.copy.TrackEventCopier;
import com.timevale.forward.service.copy.TrackPropCopier;
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

    private int eventIndex = 0;
    private int headRow = 3;
    private final List<TrackEvent> trackEventList;

    public TrackListener(List<TrackEvent> trackEventList) {
        this.trackEventList = trackEventList;
    }

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();

        TrackEvent trackEvent = trackEventList.get(eventIndex);

        log.info("导入第{}行信息：{}", rowIndex, trackEvent);

        // 当前事件的合并行数
        Integer firstRowIndex = trackEvent.getFirstRowIndex();
        Integer lastRowIndex = trackEvent.getLastRowIndex();

        // 如果为首行则复制全部数据
        if(firstRowIndex.equals(rowIndex)) {
            BeanUtil.copyProperties(trackRow, trackEvent);
        }
        // 增加属性数据
        TrackProp trackProp = TrackPropCopier.INSTANCE.convert(trackRow);
        trackEvent.getTrackPropList().add(trackProp);

        if (lastRowIndex.equals(rowIndex)) {
            eventIndex++;
        }
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
