package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.HomePageRiskWarningDTO;
import com.timevale.forward.facade.api.result.HomePageProjectNodeVO;
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
     * @param homePageRiskWarningDTOList DTOList
     * @return VO
     */
    List<HomePageProjectNodeVO> convert(List<HomePageRiskWarningDTO> homePageRiskWarningDTOList);
}
