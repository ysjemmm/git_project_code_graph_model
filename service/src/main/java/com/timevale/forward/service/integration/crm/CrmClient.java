package com.timevale.forward.service.integration.crm;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.timevale.crm.custom.provider.facade.api.CustomClient;
import com.timevale.crm.custom.provider.facade.model.CustomBaseModel;
import com.timevale.crm.custom.provider.facade.model.CustomModel;
import com.timevale.crm.custom.provider.facade.result.ResultObj;
import com.timevale.forward.service.utils.aop.LogPoint;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;

/**
 * @author by YangXu
 * @date 2023/04/07 15:39
 */
@LogPoint
@Component
public class CrmClient {

    @Resource
    private CustomClient customClient;

    /**
     * 获得客户等级
     *
     * @param customName 客户名称
     * @return 客户等级，没有则返回空字符串
     */
    public String getPostGrade(String customName) {
        if (StrUtil.isEmpty(customName)) {
            return "";
        }
        ResultObj<List<CustomModel>> resultObj = customClient.findCustomsByCustomName(customName);
        return Optional.ofNullable(resultObj)
                .map(ResultObj::getData)
                .map(CollUtil::getFirst)
                .map(CustomBaseModel::getPostGrade)
                .orElse("");
    }
}

