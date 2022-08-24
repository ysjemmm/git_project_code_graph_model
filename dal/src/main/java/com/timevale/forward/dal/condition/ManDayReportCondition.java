package com.timevale.forward.dal.condition;

import com.timevale.forward.dal.annotation.WildcardEscape;
import com.timevale.mandarin.common.query.QueryBase;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/19 16:44
 */
@Data
@Accessors(chain = true)
public class ManDayReportCondition extends QueryBase {

    @WildcardEscape
    private String projectName;

    @WildcardEscape
    private String rejectReason;

    private List<Integer> auditStatuses;

    private List<String> createMandIds;

    private List<String> pmIds;

    private Date createStartDate;

    private Date createEndDate;

    private Date weekStartDate;

    private Date weekEndDate;
}
