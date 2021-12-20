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

    private String name;

    private Long id;

    private Date createDate;

    private List<Byte> priorityList;

    private List<Long> bizDomainIdList;

    private List<Long> productLineIdList;

    private List<Byte> planReleaseDateList;

    private List<Byte> statusList;

    private List<String> createManIdList;

    private List<String> receiveManIdList;

    private List<Long> deptIdList;

    /**
     * 用于判断是否为"抄送我的需求"tab
     */
    private String copier;

}
