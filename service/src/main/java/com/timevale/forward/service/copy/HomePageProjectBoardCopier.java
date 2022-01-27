package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.HomePageProjectBoardDTO;
import com.timevale.forward.facade.api.result.HomePageProjectDateVO;
import com.timevale.forward.service.copy.convertor.DateConvertor;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/26 10:00
 */
@Mapper(uses = DateConvertor.class)
public interface HomePageProjectBoardCopier {
    HomePageProjectBoardCopier INSTANCE = Mappers.getMapper(HomePageProjectBoardCopier.class);

    /**
     * 转换
     *
     * @param homePageProjectBoardDTO DTO
     * @return VO
     */
    HomePageProjectDateVO convert(HomePageProjectBoardDTO homePageProjectBoardDTO);

    /**
     * 转换
     *
     * @param homePageProjectBoardDTOList DTOList
     * @return VOList
     */
    List<HomePageProjectDateVO> convert(List<HomePageProjectBoardDTO> homePageProjectBoardDTOList);
}
