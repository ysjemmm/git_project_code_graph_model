package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 13:50
 */
@Data
@Builder
public class BizDemandListCondition {

    /**
     * 名称
     */
    private String name;

    /**
     * 主键Id
     */
    private Long id;

    /**
     * 起始时间
     */
    private Date createDateStart;

    /**
     * 结束时间
     */
    private Date createDateEnd;

    /**
     * 优先级列表
     */
    private List<Integer> priorityList;

    /**
     * 业务域id列表
     */
    private List<Long> bizDomainIdList;

    /**
     * 产品线id列表
     */
    private List<Long> productLineIdList;

    /**
     * 预期上线日期列表
     */
    private List<Integer> planReleaseDateList;

    /**
     * 业务状态列表
     */
    private List<Integer> statusList;

    /**
     * 提交人id列表
     */
    private List<String> createManIdList;

    /**
     * 接收人id列表
     */
    private List<String> receiveManIdList;

    /**
     * 部门id列表
     */
    private List<Long> deptIdList;

    /**
     * 用于判断是否为"抄送我的需求"tab
     */
    private String copier;

    /**
     * 产品需求id
     */
    private Long productDemandId;
    /**
     * 业务需求id
     */
    private List<Long> bizDemandIds;
}
