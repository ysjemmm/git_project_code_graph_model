package com.timevale.forward.service.excel.track.map;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * 行分类
 *
 * @author yangxu
 * @date 2022/08/18
 */
@Data
public class ClassifyRow {
    @ExcelProperty(index = 0)
    private String firstClass;

    @ExcelProperty(index = 1)
    private String secondClass;

    @ExcelProperty(index = 2)
    private String thirdClass;

    @ExcelProperty(index = 3)
    private String fourthClass;

    @ExcelProperty(index = 4)
    private String fifthClass;

    private Integer size = 1;
}
