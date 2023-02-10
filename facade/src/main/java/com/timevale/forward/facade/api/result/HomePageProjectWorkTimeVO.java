package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModelProperty;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * @author jingchun
 * created on 2023/2/7
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ApiOperation("团队项目工时个人项目")
public class HomePageProjectWorkTimeVO extends ToString {

    @ApiModelProperty("日期")
    private Date date;

    private List<HomePageSingleProjectWorkTimeVO> projectInfo;

}
