package com.timevale.forward.service.excel.track.event;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/09 16:29
 */
@Data
public class TrackRow {

    @ExcelProperty(value = "序号" ,index = 0)
    private String serialNumber;

    @ExcelProperty(value = "一级分类（必填）",index = 1)
    private String firstClassify;

    @ExcelProperty(value = "二级分类（必填）", index = 2)
    private String secondClassify;

    @ExcelProperty(value = "三级分类（必填）\n若需要添加请联系PMO", index = 3)
    private String thirdClassify;

    @ExcelProperty(value = "四级分类（必填）", index = 4)
    private String fourthClassify;

    @ExcelProperty(value = "五级分类", index = 5)
    private String fifthClassify;

    @ExcelProperty(value = "事件中文名（必填）", index = 6)
    private String eventNameCn;

    @ExcelProperty(value = "事件英文名（必填）", index = 7)
    private String eventNameEn;

    @ExcelProperty(value = "属性中文名（必填）", index = 8)
    private String propNameCn;

    @ExcelProperty(value = "属性英文名（必填）", index = 9)
    private String propNameEn;

    @ExcelProperty(value = "数据类型（必填）", index = 10)
    private String dataType;

    @ExcelProperty(value = "埋点平台（必填，可多选）", index = 11)
    private String platform;

    @ExcelProperty(value = "埋点位置说明\n（埋点平台含非服务端时，必填。可填写PRD或者wiki链接）", index = 12)
    private String explanation;

    @ExcelProperty(value = "接口名称\n（埋点平台包含服务端时，必填）", index = 13)
    private String apiName;

    @ExcelProperty(value = "触发时机（必填）", index = 14)
    private String touchMoment;

    @ExcelProperty(value = "埋点所属环境（必填）", index = 15)
    private String env;
}
