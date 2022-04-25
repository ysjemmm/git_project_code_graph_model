package com.timevale.forward.model.enums;

import com.timevale.forward.dal.entity.ProjectNodeDO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @date 2022/1/24 15:13
 * @author 望轩
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
    TECHNICAL_DETAIL_REVIEW(30,"技术详设评审"),
    DEVELOP_START(40,"开发开始"),
    WRITE_TEST_CASES(50,"编写测试用例"),
    USE_CASE_REVIEW(60,"用例评审"),
    SUBMIT_TEST(70,"提测"),
    TEST_START(80,"测试开始"),
    PUBLISH_SIMULATE(90,"发布模拟"),
    PUBLISH_OFFICIAL(100,"发布正式");

    private final Integer code;
    private final String projectNodeName;

    public static String getStage(List<ProjectNodeDO> projectNodeDOList){
        projectNodeDOList.sort(Comparator.comparing(a -> getCodeByName(a.getName())));
        for (ProjectNodeDO e : projectNodeDOList) {
            if(e.getActualDate() == null){
                return e.getName();
            }
        }
        return PUBLISH_OFFICIAL.projectNodeName;
    }

    public static Integer getStageCode(List<ProjectNodeDO> projectNodeDOList){
        projectNodeDOList.sort(Comparator.comparing(a -> getCodeByName(a.getName())));
        for (ProjectNodeDO e : projectNodeDOList) {
            if(e.getActualDate() == null){
                return getCodeByName(e.getName());
            }
        }
        return PUBLISH_OFFICIAL.code;
    }

    public static String getNameByCode(Integer code){
        for (ProjectNodeEnum e : ProjectNodeEnum.values()) {
            if(Objects.equals(e.code, code)){
                return e.projectNodeName;
            }
        }
        return "";
    }

    public static Integer getCodeByName(String name){
        for (ProjectNodeEnum e : ProjectNodeEnum.values()) {
            if(Objects.equals(e.projectNodeName, name)){
                return e.code;
            }
        }
        return -1;
    }

    public final static Map<Integer, String> DEFAULT_NODE = new HashMap<Integer, String>() {{
        put(1, ProjectNodeEnum.START_PLAN.getProjectNodeName());
        put(2, ProjectNodeEnum.DEMAND_INTERNAL_AUDIT.getProjectNodeName());
        put(3, ProjectNodeEnum.DEMAND_CONSTRUE.getProjectNodeName());
        put(4, ProjectNodeEnum.TECHNICAL_DETAIL_REVIEW.getProjectNodeName());
        put(5, ProjectNodeEnum.DEVELOP_START.getProjectNodeName());
        put(6, ProjectNodeEnum.WRITE_TEST_CASES.getProjectNodeName());
        put(7, ProjectNodeEnum.USE_CASE_REVIEW.getProjectNodeName());
        put(8, ProjectNodeEnum.SUBMIT_TEST.getProjectNodeName());
        put(9, ProjectNodeEnum.TEST_START.getProjectNodeName());
        put(10, ProjectNodeEnum.PUBLISH_SIMULATE.getProjectNodeName());
        put(11, ProjectNodeEnum.PUBLISH_OFFICIAL.getProjectNodeName());
    }};
}
