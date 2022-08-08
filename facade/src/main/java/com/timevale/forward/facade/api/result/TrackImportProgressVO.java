package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2022/08/08 16:19
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点导入进度")
public class TrackImportProgressVO extends ToString {
    @ApiModelProperty("进度范围：0~100")
    private Integer progress;

    @ApiModelProperty("导入状态：0导入中，1导入成功，2导入失败")
    private Integer status;

    @ApiModelProperty("导入总数量")
    private Integer importCount;

    @ApiModelProperty("导入失败数量")
    private Integer importFailCount;
}
