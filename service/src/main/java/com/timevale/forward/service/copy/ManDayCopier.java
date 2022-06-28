package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.ManDayDO;
import com.timevale.forward.facade.api.result.ManDayVO;
import com.timevale.forward.service.utils.date.DateUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author jingchun
 * create on 2022/6/24
 */
@Mapper(imports = {DateUtil.class})
public interface ManDayCopier {

    ManDayCopier INSTANCE = Mappers.getMapper(ManDayCopier.class);

    @Mapping(target = "pm", ignore = true)
    @Mapping(target = "editable", ignore = true)
    @Mapping(target = "weekDateRange", expression = "java(DateUtil.formDateRange(manDayDO.getWeekStartDate(), manDayDO.getWeekEndDate()))")
    ManDayVO convert(ManDayDO manDayDO);

    List<ManDayVO> convert(List<ManDayDO> manDays);

}
