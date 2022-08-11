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
@ApiModel("埋点导入模板")
public class TrackImportTemplateVO extends ToString {

    @ApiModelProperty("文件id")
    private String fileId;
}
