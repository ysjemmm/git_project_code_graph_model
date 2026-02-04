package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 13:50
 */
@Data
@Builder
public class BizDemandListCondition{

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
     * 包含标签
     */
    private Boolean containLabel;

    /**
     *包含的id
     */
    private List<Long> containIds;

    /**
     * 不包含的id
     */
    private List<Long> exclusiveIds;

    /**
     * 来源id
     */
    private String sourceId;

    /**
     * 排序
     */
    private String collation;

    /**
     * 需求描述
     */
    @WildcardEscape
    private String desc;

    /**
     * 期望上线日期开始
     */
    private Date hopeReleaseDayStart;

    /**
     * 期望上线日期结束
     */
    private Date hopeReleaseDayEnd;

    /**
     * 是否为客开需求
     */
    private Boolean customerDevDemand;

    /**
     * 产品方案
     */
    @WildcardEscape
    private String productSolution;

    /**
     * SR专家id集合
     */
    private Collection<String> srExpertIds;

    /**
     * 是否影响客户订单
     */
    private Boolean affectCustomerOrder;

    /**
     * 客户等级
     */
    private String customerGrade;

    /**
     * 卡单说明
     */
    @WildcardEscape
    private String stuckOrderInstructions;

    /**
     *  实现 id 与 name 双向查询
     */

    private List<Long> ids;

    /**
     * 需求负责人名称-模糊匹配
     */
    private String ownerName;

    /**
     * 产品线名称-模糊匹配
     */
    private String productLineName;

    private List<Long> bizDomainIds;


    public int pageNum = 1;
    public int pageSize = 20;
}
