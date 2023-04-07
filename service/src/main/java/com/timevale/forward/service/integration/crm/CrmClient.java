package com.timevale.forward.service.integration.crm;

import cn.hutool.core.collection.CollUtil;
import com.timevale.crm.custom.provider.facade.api.CustomClient;
import com.timevale.crm.custom.provider.facade.model.CustomBaseModel;
import com.timevale.crm.custom.provider.facade.model.CustomModel;
import com.timevale.crm.custom.provider.facade.result.ResultObj;
import com.timevale.forward.service.utils.aop.LogPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@LogPoint
@Component
@RequiredArgsConstructor
public class CrmClient {

    private final CustomClient client;

    /**
     * 查询客户等级
     *
     * @param customName 客户名称
     * @return {@link String}
     */
    public String getPostGrade(String customName) {
        ResultObj<List<CustomModel>> resultObj = client.findCustomsByCustomName(customName);
        return Optional.ofNullable(resultObj)
                .map(ResultObj::getData)
                .map(CollUtil::getFirst)
                .map(CustomBaseModel::getPostGrade)
                .orElse("");
    }

}
