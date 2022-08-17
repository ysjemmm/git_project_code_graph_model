package com.timevale.forward.service.excel.track.map;

import lombok.Data;

import java.util.List;

/**
 * 分类数据
 *
 * @author yangxu
 * @date 2022/08/17
 */
@Data
public class ClassifyData {

    private Long id;

    private String name;

    private Integer size;

    private Integer level;

    private List<ClassifyData> subClassifyData;

}
