package com.timevale.forward.facade.api.request;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;


/**
 * @author by YangXu
 * @date 2022/08/08 16:09
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("埋点导入请求")
public class TrackImportReq extends BaseReq {

    @ApiModelProperty("导入文件id")
    @NotBlank(message = "文件id不能为空")
    private String fileId;
}
