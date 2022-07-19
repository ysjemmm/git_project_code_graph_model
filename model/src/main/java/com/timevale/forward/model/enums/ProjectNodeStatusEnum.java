package com.timevale.forward.model.enums;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import lombok.Getter;

import java.util.*;

/**
 * @author by YangXu
 * @date 2022/04/26 19:54
 */
@Getter
public enum ProjectNodeStatusEnum {
    /**
     * 待启动、待内审、待串讲、待详设内审、待开发、开发中、待测试、测试中、已发布
     */
    READY_START(0,"待启动"),
    READY_INTERNAL_AUDIT(10,"待内审"),
    READY_CONSTRUE(20,"待串讲"),
    READY_CONSTRUE_REVERSE(25,"待反串讲"),
    READY_UED_AUDIT(27,"待UED评审"),
    READY_TECHNICAL_DETAIL_REVIEW(30,"待详设内审"),
    READY_DEVELOP(40,"待开发"),
    DEVELOPING(50,"开发中"),
    READY_TEST(60,"待测试"),
    TESTING(70,"测试中"),
    PUBLISHED(80,"已发布");

    private final Integer code;
    private final String text;
    public final static Map<String, Integer> nodeStatusMap = new LinkedHashMap<>();

    ProjectNodeStatusEnum(Integer code, String text) {
        this.code = code;
        this.text = text;
    }

    public static String getNameByCode(Integer code){
        for (ProjectNodeStatusEnum e : ProjectNodeStatusEnum.values()) {
            if(Objects.equals(e.code, code)){
                return e.text;
            }
        }
        return "";
    }

    static {
        // 配置节点和节点状态的映射关系
        nodeStatusMap.put(ProjectNodeEnum.START_PLAN.getText(),              ProjectNodeStatusEnum.READY_START.code);
        nodeStatusMap.put(ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText(),   ProjectNodeStatusEnum.READY_INTERNAL_AUDIT.code);
        nodeStatusMap.put(ProjectNodeEnum.DEMAND_CONSTRUE.getText(),         ProjectNodeStatusEnum.READY_CONSTRUE.code);
        nodeStatusMap.put(ProjectNodeEnum.DEMAND_CONSTRUE_REVERSE.getText(), ProjectNodeStatusEnum.READY_CONSTRUE_REVERSE.code);
        nodeStatusMap.put(ProjectNodeEnum.UED_AUDIT.getText(), ProjectNodeStatusEnum.READY_UED_AUDIT.code);
        nodeStatusMap.put(ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText(), ProjectNodeStatusEnum.READY_TECHNICAL_DETAIL_REVIEW.code);
        nodeStatusMap.put(ProjectNodeEnum.DEVELOP_START.getText(),           ProjectNodeStatusEnum.READY_DEVELOP.code);
        nodeStatusMap.put(ProjectNodeEnum.WRITE_TEST_CASES.getText(),        ProjectNodeStatusEnum.DEVELOPING.code);
        nodeStatusMap.put(ProjectNodeEnum.USE_CASE_REVIEW.getText(),         ProjectNodeStatusEnum.DEVELOPING.code);
        nodeStatusMap.put(ProjectNodeEnum.SUBMIT_TEST.getText(),             ProjectNodeStatusEnum.DEVELOPING.code);
        nodeStatusMap.put(ProjectNodeEnum.TEST_START.getText(),              ProjectNodeStatusEnum.READY_TEST.code);
        nodeStatusMap.put(ProjectNodeEnum.PUBLISH_SIMULATE.getText(),        ProjectNodeStatusEnum.TESTING.code);
        nodeStatusMap.put(ProjectNodeEnum.PUBLISH_OFFICIAL.getText(),        ProjectNodeStatusEnum.TESTING.code);
    }

}
