package com.timevale.forward.facade.api.result;

import com.timevale.mandarin.common.result.ToString;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @Date 2022/3/17 17:52
 * @Author 望轩
 */
@EqualsAndHashCode(callSuper = true)
@Data
@ApiModel("产品线对应的字段")
public class ProductLineToFieldVO extends ToString {
    @ApiModelProperty("产品线要显示的所有字段属性名：{flowId->流程flow id,mainOId->实名主体 oid,templateId->模板id,appId->appid,sealId->sealid,operatorNameAccount->操作人姓名账号,loginAccount->登录账号}")
    private List<String> field;
}