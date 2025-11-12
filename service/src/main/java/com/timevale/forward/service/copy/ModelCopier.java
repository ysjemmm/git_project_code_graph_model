package com.timevale.forward.service.copy;

import com.alibaba.fastjson.JSON;
import com.timevale.forward.dal.condition.ModelCondition;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.facade.api.query.ModelQueryList;
import com.timevale.forward.facade.api.request.ModelAddReq;
import com.timevale.forward.facade.api.request.ModelModifyReq;
import com.timevale.forward.facade.api.result.ModelFormFieldVO;
import com.timevale.forward.facade.api.result.ModelVO;
import com.timevale.forward.model.enums.BugOnlineBeloneEnum;
import com.timevale.forward.model.enums.BugOnlineCategoryEnum;
import com.timevale.forward.model.enums.BugOnlineEnvEnum;
import com.timevale.forward.model.enums.BugOnlineGenerationStageEnum;
import com.timevale.forward.model.enums.BugOnlinePriorityEnum;
import com.timevale.forward.model.enums.BugOnlineReasonEnum;
import com.timevale.forward.model.enums.BugOnlineReasonStageEnum;
import com.timevale.forward.model.enums.BugOnlineRecurrentEnum;
import com.timevale.forward.model.enums.BugOnlineSourceEnum;
import com.timevale.forward.model.enums.BugOnlineStatusEnum;
import com.timevale.forward.model.enums.CustomerCountEnum;
import com.timevale.forward.model.enums.ProblemOccurredTimeEnum;
import com.timevale.forward.model.enums.UserCountEnum;
import com.timevale.forward.service.utils.date.DateUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper(
        imports = {
                JSON.class,
                ModelFormFieldVO.class
        }
)
public interface ModelCopier {

    ModelCopier INSTANCE = Mappers.getMapper(ModelCopier.class);


    /**
     * 批量处理
     *
     * @param modelDOList 产品线do 列表
     * @return 列表
     */
    List<ModelVO> convert(List<ModelDO> modelDOList);


    /**
     *
     * @param modelAddReq modelAddReq
     * @return return
     */
    @Mapping(target = "formField", expression = "java(JSON.toJSONString(modelAddReq.getDynamicFormFields()))")
    ModelDO convert(ModelAddReq modelAddReq);

    /**
     *
     * @param modelModifyReq modelModifyReq
     * @return return
     */
    @Mapping(target = "formField", expression = "java(JSON.toJSONString(modelModifyReq.getDynamicFormFields()))")
    ModelDO convert(ModelModifyReq modelModifyReq);

    /**
     * 转换
     *
     * @param modelQueryList 模型查询列表
     * @return return
     */
    ModelCondition convert(ModelQueryList modelQueryList);


}
