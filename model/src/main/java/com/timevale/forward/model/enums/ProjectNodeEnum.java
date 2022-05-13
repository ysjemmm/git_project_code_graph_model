package com.timevale.forward.model.enums;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;


/**
 * @author by YangXu
 * @date 2022/04/27 15:36
 */
@Getter
@AllArgsConstructor
public enum ProjectNodeEnum {
    /**
     * 节点阶段
     */
    START_PLAN(0,"开始规划"),
    DEMAND_INTERNAL_AUDIT(10,"需求内审"),
    DEMAND_CONSTRUE(20,"需求串讲"),
    DEMAND_CONSTRUE_REVERSE(25,"需求反串讲"),
    TECHNICAL_DETAIL_REVIEW(30,"技术详设评审"),
    DEVELOP_START(40,"开发开始"),
    WRITE_TEST_CASES(50,"编写测试用例"),
    USE_CASE_REVIEW(60,"用例评审"),
    SUBMIT_TEST(70,"提测"),
    TEST_START(80,"测试开始"),
    PUBLISH_SIMULATE(90,"发布模拟"),
    PUBLISH_OFFICIAL(100,"发布正式");

    private final Integer code;
    private final String text;

    public static Integer getCodeByName(String name){
        for (ProjectNodeEnum e : ProjectNodeEnum.values()) {
            if(Objects.equals(e.getText(), name)){
                return e.code;
            }
        }
        return -1;
    }

    public static void sort(List<ProjectNodeDO> nodeDOList){
        Map<String, Integer> nodeMap = Arrays.stream(ProjectNodeEnum.values())
                .collect(Collectors.toMap(ProjectNodeEnum::getText, ProjectNodeEnum::getCode, (a, b) -> a));
        nodeDOList.sort((a, b) -> {
            Integer aCode = nodeMap.get(a.getName());
            Integer bCode = nodeMap.get(b.getName());
            return aCode.compareTo(bCode);
        });
    }

    public final static Map<Integer, String> DEFAULT_NODE = new HashMap<Integer, String>() {{
        put(1, ProjectNodeEnum.START_PLAN.getText());
        put(2, ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getText());
        put(3, ProjectNodeEnum.DEMAND_CONSTRUE.getText());
        put(4, ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getText());
        put(5, ProjectNodeEnum.DEVELOP_START.getText());
        put(6, ProjectNodeEnum.WRITE_TEST_CASES.getText());
        put(7, ProjectNodeEnum.USE_CASE_REVIEW.getText());
        put(8, ProjectNodeEnum.SUBMIT_TEST.getText());
        put(9, ProjectNodeEnum.TEST_START.getText());
        put(10, ProjectNodeEnum.PUBLISH_SIMULATE.getText());
        put(11, ProjectNodeEnum.PUBLISH_OFFICIAL.getText());
    }};
}
