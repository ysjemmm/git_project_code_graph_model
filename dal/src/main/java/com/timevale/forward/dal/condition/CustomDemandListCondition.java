package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
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
public class CustomDemandListCondition {

    /**
     * 需求主题
     */
    @WildcardEscape
    private String name;
    /**
     * 客户名称
     */
    @WildcardEscape
    private String customName;
    /**
     * 客户所在页面
     */
    @WildcardEscape
    private String page;
    /**
     * 客户需求id
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
     * 需求解决状态
     */
    private List<Integer> status;
    /**
     * 问题类别
     */
    private List<Integer> causes;
    /**
     * 产品端
     */
    private List<Integer> productEnds;
    /**
     * 需求提交人id
     */
    @WildcardEscape
    private String submitMan;

    /**
     * 需求接收id
     */
    private List<String> receiveManIds;
    /**
     * 起始时间
     */
    private Date projectEndDateStart;
    /**
     * 结束时间
     */
    private Date projectEndDateEnd;

    /**
     * 客户需求id
     */
    private List<Long> customDemandIds;

    /**
     * 产品需求id
     */
    private Long productDemandId;
}
