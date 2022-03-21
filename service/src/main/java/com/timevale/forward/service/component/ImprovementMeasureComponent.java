package com.timevale.forward.service.component;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.entity.ImprovementMeasureDO;
import com.timevale.forward.facade.api.request.ImprovementMeasureAddReq;
import com.timevale.forward.model.enums.ImprovementMeasureStatusEnum;
import com.timevale.forward.service.copy.ImprovementMeasureCopier;
import com.timevale.forward.service.integration.erp.model.CreateTodoTaskMsg;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.mandarin.base.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.util.Lists;

import java.util.List;
import java.util.Map;

/**
 * @author by YangXu
 * @date 2022/03/19 12:01
 */
public interface ImprovementMeasureComponent {

    /**
     * 更新钉钉待办状态状态
     *
     * @param improvementMeasureDOList 改进措施DO List
     */
    void updateTodoStatus(List<ImprovementMeasureDO> improvementMeasureDOList);

    /**
     * 添加
     *
     * @param improvementMeasureAddReq 改进措施增加
     */
    void add(ImprovementMeasureAddReq improvementMeasureAddReq);

}
