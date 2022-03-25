package com.timevale.forward.model.middle;

import com.timevale.forward.dal.annotation.FieldCompare;
import lombok.Data;

/**
 * @Date 2022/3/22 14:22
 * @Author 望轩
 */
@Data
public class BusinessMD extends BaseMD{
    /**
     * 流程flow id
     */
    @FieldCompare(fieldName = "流程flow id")
    private String flowId;

    /**
     * 实名主体oid
     */
    @FieldCompare(fieldName = "实名主体oid")
    private String mainOid;

    /**
     * 模板id
     */
    @FieldCompare(fieldName = "模板id")
    private String templateId;

    /**
     * appId
     */
    @FieldCompare(fieldName = "appId")
    private String appId;

    /**
     * sealId
     */
    @FieldCompare(fieldName = "sealId")
    private String sealId;

    /**
     * 操作人姓名账号对应的属性
     */
    @FieldCompare(fieldName = "操作人姓名账号对应的属性")
    private String operatorNameAccount;

    /**
     * 登录账号
     */
    @FieldCompare(fieldName = "登录账号")
    private String loginAccount;
}