package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.HomePageDataIndicatorDTO;
import com.timevale.forward.facade.api.result.HomePageDataIndicatorVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

/**
 * @author by YangXu
 * @date 2022/01/24 16:50
 */
@Mapper
public interface HomePageDataIndicatorCopier {
    HomePageDataIndicatorCopier INSTANCE = Mappers.getMapper(HomePageDataIndicatorCopier.class);

    /**
     * 转换
     *
     * @param homePageDataIndicatorDTO DTO
     * @return VO
     */
    HomePageDataIndicatorVO convert(HomePageDataIndicatorDTO homePageDataIndicatorDTO);

}
