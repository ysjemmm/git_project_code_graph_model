package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
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
public class BizDemandListCondition extends QueryBase {

    /**
     * 名称
     */
    @WildcardEscape
    private String name;

    /**
     * 主键Id
     */
    private Long id;

    /**
     * 目标客户/用户/项目
     */
    @WildcardEscape
    private String targetCustomer;

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
     * 子产品线id列表
     */
    private List<Long> subProductLineIdList;

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
     * 需求提交人id列表
     */
    private List<String> submitManIdList;

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

    /**
     * 项目发布时间-起始时间
     */
    private Date projectEndDateStart;

    /**
     * 项目发布时间-结束时间
     */
    private Date projectEndDateEnd;

    private String orderFiled;

    private Integer orderCollation;

    /**
     * 标签id
     */
    private List<Long> labelIds;

    /**
     *包含的id
     */
    private List<Long> containIds;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 排序
     */
    private String collation;

}
