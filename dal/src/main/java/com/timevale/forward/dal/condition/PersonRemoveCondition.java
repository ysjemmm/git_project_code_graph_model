package com.timevale.forward.dal.condition;

import lombok.Builder;
import lombok.Data;
import org.springframework.data.util.Pair;

import java.util.List;

/**
 * @author by mayang
 * @date 2021/12/15 13:39
 */
@Data
@Builder
public class PersonRemoveCondition {

    /**
     * 主体 id -> 指定人 id
     */
    private List<Pair<Long, String>> mainId2PersonId;

    /**
     * 人员类型:0项目-产品经理,1项目-项目成员,20产品需求-抄送人,30业务需求-抄送人,40-任务执行人,50线下bug-抄送人，60线上bug-抄送人
     */
    private Integer type;
}
