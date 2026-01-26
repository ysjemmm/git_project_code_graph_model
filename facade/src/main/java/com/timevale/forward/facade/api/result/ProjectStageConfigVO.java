package com.timevale.forward.facade.api.result;

import lombok.Data;

import java.util.List;

@Data
public class ProjectStageConfigVO {

    private List<KindConfig> config;

    @Data
    public static class KindConfig {
        private Integer kind;
        private String kindName;
        private List<TypeConfig> typeConfig;
    }

    @Data
    public static class TypeConfig {
        private Integer type;
        private String typeName;
        private List<Stage> stage;
    }

    @Data
    public static class Stage {
        private String stageName;
        private String stageType;
        private Integer version;
        private List<StageItem> nodes;
    }

    @Data
    public static class StageItem {
        private Integer num;
        private String name;
        private Boolean isRequired;
    }
}