package com.timevale.forward.service.excel.track;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.timevale.mandarin.base.util.AssertUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author by YangXu
 * @date 2022/08/09 16:25
 */
@Slf4j
public class TrackCheckListener extends AnalysisEventListener<TrackRow> {
    // 表头行
    private int headRow = 3;
    // 限制事件条数
    private int eventLimit = 3;

    @Override
    public void invoke(TrackRow trackRow, AnalysisContext analysisContext) {
        if(BeanUtil.isEmpty(trackRow)) {
            return;
        }
        String firstClassify = trackRow.getFirstClassify();
        if (StrUtil.isNotEmpty(firstClassify)) {
            eventLimit--;
        }

    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        log.info("埋点导入表头及事件数量校验完毕");
    }

    @Override
    public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
    }
}
