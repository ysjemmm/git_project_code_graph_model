package com.timevale.forward.service.excel.track.sensor;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author by YangXu
 * @date 2023/01/09 16:47
 */
@Data
public class SensorTrackRow {

    @ExcelProperty(value = "事件编号" ,index = 0)
    private Long serialNumber;

    @ExcelProperty(value = "事件英文变量名（必填）", index = 1)
    private String eventNameEn;

    @ExcelProperty(value = "事件显示名（必填）", index = 2)
    private String eventNameCn;

    @ExcelProperty(value = "属性英文变量名（必填）", index = 3)
    private String propNameEn;

    @ExcelProperty(value = "属性显示名（必填）", index = 4)
    private String propNameCn;

    @ExcelProperty(value = "数据类型（必填）", index = 5)
    private String dataType;

    @ExcelProperty(value = "属性值示例或说明", index = 6)
    private String propExplain;

    @ExcelProperty(value = "应埋点平台（必填）", index = 7)
    private String platform;

    @ExcelProperty(value = "触发时机（必填）", index = 8)
    private String touchMoment;

    @ExcelProperty(value = "备注", index = 9)
    private String memo;
}
