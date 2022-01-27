package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningSubmitTestDTO;
import com.timevale.forward.dal.dto.HomePageRiskWarningTaskDTO;
import com.timevale.forward.facade.api.result.HomePageProjectNodeVO;
import com.timevale.forward.facade.api.result.HomePageSubmitTestVO;
import com.timevale.forward.facade.api.result.HomePageTaskVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 17:38
 */
@Mapper
public interface HomePageRiskWarningCopier {
    HomePageRiskWarningCopier INSTANCE = Mappers.getMapper(HomePageRiskWarningCopier.class);

    /**
     * 转换
     *
     * @param homePageRiskWarningDTO DTO
     * @return VO
     */
    HomePageProjectNodeVO convert(HomePageRiskWarningDTO homePageRiskWarningDTO);

    /**
     * 转换
     *
     * @param homePageRiskWarningTaskDTO DTO
     * @return VO
     */
    HomePageTaskVO convert(HomePageRiskWarningTaskDTO homePageRiskWarningTaskDTO);

    /**
     * 转换
     *
     * @param homePageRiskWarningTaskDTO DTO
     * @return VO
     */
    HomePageSubmitTestVO convert(HomePageRiskWarningSubmitTestDTO homePageRiskWarningTaskDTO);


}
