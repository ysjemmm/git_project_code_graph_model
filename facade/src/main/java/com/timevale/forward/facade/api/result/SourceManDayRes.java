package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author jingchun
 * created on 2023/3/1
 */
@Getter
@Setter
public class SourceManDayRes extends ToString {

    // 产研项目id
    private String projectId;

    // 来源数据id
    private String sourceId;

    // 同项目、来源下人天列表
    private List<SourceManDayVO> manDays;

}
