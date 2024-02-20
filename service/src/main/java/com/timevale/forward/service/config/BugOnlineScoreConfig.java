package com.timevale.forward.service.config;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.HashBasedTable;
import com.timevale.forward.model.enums.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Slf4j
@Configuration
public class BugOnlineScoreConfig implements EnvironmentAware {

    @Override
    public void setEnvironment(Environment environment) {
        String property = environment.getProperty("bugOnlineScoreConfig");
        initialData(property);
    }

    private void initialData(String property) {
        if (StrUtil.isEmpty(property)) {
            return;
        }

        JSONObject configJson = JSONObject.parseObject(property);

        for (String factorEnumName : configJson.keySet()) {
            JSONObject factorJson = configJson.getJSONObject(factorEnumName);

            for (String bizDomainIdStr : factorJson.keySet()) {
                List<String> bizDomainIds = StrUtil.split(bizDomainIdStr, ",");
                JSONObject scoreConfig = factorJson.getJSONObject(bizDomainIdStr);

                HashBasedTable<Long, Integer, Integer> scoreTable = HashBasedTable.create();
                for (String bizDomainId : bizDomainIds) {
                    scoreConfig.getInnerMap().forEach((code, score) ->
                            scoreTable.put(Long.valueOf(bizDomainId), Integer.valueOf(code), (int) score));
                }

                fillToEnum(factorEnumName, scoreTable);
            }
        }
    }

    private void fillToEnum(String factorEnumName, HashBasedTable<Long, Integer, Integer> scoreTable) {
        if ("CustomerGradeEnum".equals(factorEnumName)) {
            CustomerGradeEnum.setScoreTable(scoreTable);
        }
        if ("CustomerCountEnum".equals(factorEnumName)) {
            CustomerCountEnum.setScoreTable(scoreTable);
        }
        if ("BugOnlineCategoryEnum".equals(factorEnumName)) {
            BugOnlineCategoryEnum.setScoreTable(scoreTable);
        }
        if ("BugOnlineEnvEnum".equals(factorEnumName)) {
            BugOnlineEnvEnum.setScoreTable(scoreTable);
        }
        if ("BugOnlineRecurrentEnum".equals(factorEnumName)) {
            BugOnlineRecurrentEnum.setScoreTable(scoreTable);
        }
        if ("UserCountEnum".equals(factorEnumName)) {
            UserCountEnum.setScoreTable(scoreTable);
        }
        if ("ProblemOccurredTimeEnum".equals(factorEnumName)) {
            ProblemOccurredTimeEnum.setScoreTable(scoreTable);
        }
        if ("ProduceLineLevelEnum".equals(factorEnumName)) {
            ProduceLineLevelEnum.setScoreTable(scoreTable);
        }
    }
}
