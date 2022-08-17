package com.timevale.forward.service.excel.track.event;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.timevale.forward.service.component.impl.TrackImportComponentImpl;
import com.timevale.forward.service.copy.TrackPropCopier;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
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
    private boolean checkHead = false;
    private final List<TrackEvent> trackEventList;
    private final String userId;

    public TrackListener(List<TrackEvent> trackEventList, String userId) {
        this.userId = userId;
        this.trackEventList = trackEventList;
    }

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext context) {
        if (!checkHead) {
            SpringUtil.getBean(TrackImportComponentImpl.class).
                    setExceptionMessage("导入文件列表格式错误，请勿修改模板格式", userId);
            throw new BaseBizRuntimeException("导入文件列表格式错误，请勿修改模板格式");
        }

        Integer rowIndex = context.readRowHolder().getRowIndex();

        TrackEvent trackEvent = trackEventList.get(eventIndex);
        // 当前事件的合并行数
        Integer firstRowIndex = trackEvent.getFirstRowIndex();
        Integer lastRowIndex = trackEvent.getLastRowIndex();

        // 判断是否到下一个
        while (lastRowIndex.compareTo(rowIndex) < 0) {
            eventIndex++;
            trackEvent = trackEventList.get(eventIndex);
            firstRowIndex = trackEvent.getFirstRowIndex();
            lastRowIndex = trackEvent.getLastRowIndex();
        }

        // 如果为首行则复制全部数据
        if(firstRowIndex.equals(rowIndex)) {
            BeanUtil.copyProperties(trackRow, trackEvent);
        }
        // 增加属性数据
        TrackProp trackProp = TrackPropCopier.INSTANCE.convert(trackRow);
        trackEvent.getTrackPropList().add(trackProp);

        log.info("导入第{}行信息：{}", rowIndex, trackEvent);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        log.info("导入文件读取完成");
    }

    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
        Integer rowIndex = context.readRowHolder().getRowIndex();
        if (rowIndex == 2) {
            log.info("表头信息对比开始");
            checkHead = true;
            Field[] fields = ReflectUtil.getFields(TrackRow.class);
            for (int i = 0; i < 16; i++) {
                Field field = fields[i];
                ExcelProperty annotation = AnnotationUtil.getAnnotation(field, ExcelProperty.class);
                int index = annotation.index();
                String value = annotation.value()[0];
                String headMapValue = headMap.get(index);
                if (ObjectUtil.notEqual(value, headMapValue)) {
                    SpringUtil.getBean(TrackImportComponentImpl.class).
                            setExceptionMessage("导入文件列表格式错误，请勿修改模板格式", userId);
                    throw new BaseBizRuntimeException("导入文件列表格式错误，请勿修改模板格式");
                }
            }
            log.info("表头信息对比结束");
        }
    }
}
