package com.timevale.forward.facade.api.request;

import com.timevale.mandarin.common.query.QueryBase;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author: xingyun
 * @create: 2021-12-13 17:44
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("文件上传")
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadReq extends BaseReq {

    @ApiModelProperty(value = "文件所属id")
    private Long attachId;

    @ApiModelProperty(value = "文件所属主体")
    private Byte type;

    @ApiModelProperty(value = "文件")
    private MultipartFile multipartFile;

}
