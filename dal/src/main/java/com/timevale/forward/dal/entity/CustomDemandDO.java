package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * @author by YangXu
 * @date 2021/12/15 10:59
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CustomDemandDO extends BaseDO {

    /**
     * 客户需求主题
     */
    private String name;

    /**
     * 产品端
     */
    private String productEnd;

    /**
     * 客户名称
     */
    private String customName;

    /**
     * 客户所在页面
     */
    private String page;

    /**
     * 所在页面链接
     */
    private String path;


    /**
     * 需求描述
     */
    private String desc;

    /**
     * 需求解决状态
     */
    private Integer status;


    /**
     * 接收人
     */
    private String receiveMan;

    /**
     * 接收人id
     */
    private String receiveManId;


    /**
     * 驳回理由
     */
    private Integer reason;

    /**
     * 需求提交人
     */
    private String submitMan;

    /**
     * 需求提交人id
     */
    private String submitManId;

    /**
     * 解决方案
     */
    private String solvePlan;


    /**
     * 项目发布时间
     */
    private Date projectEndDate;


    /**
     * 联系方式
     */
    private String contact;

    /**
     * 问题类别
     */
    private Integer cause;

}
