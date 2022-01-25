package com.timevale.forward.service.copy;

import com.timevale.forward.dal.dto.HomePageProjectOnlineLatelyDTO;
import com.timevale.forward.facade.api.result.HomePageProjectOnlineLatelyVO;
import com.timevale.mandarin.common.result.PageQueryResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/25 11:45
 */
@Mapper
public interface HomePageProjectOnlineLatelyCopier {
    HomePageProjectOnlineLatelyCopier INSTANCE = Mappers.getMapper(HomePageProjectOnlineLatelyCopier.class);

    /**
     * 转换
     *
     * @param homePageProjectOnlineLatelyDTO DTO
     * @return VO
     */
    HomePageProjectOnlineLatelyVO convert(HomePageProjectOnlineLatelyDTO homePageProjectOnlineLatelyDTO);

    /**
     * 转换
     *
     * @param homePageProjectOnlineLatelyDTOList DTOList
     * @return VOList
     */
    List<HomePageProjectOnlineLatelyVO> convert(List<HomePageProjectOnlineLatelyDTO> homePageProjectOnlineLatelyDTOList);

    /**
     * 转换
     *
     * @param list DTO分页数据
     * @return vo
     */
    PageQueryResult<HomePageProjectOnlineLatelyVO> convert(PageQueryResult<HomePageProjectOnlineLatelyDTO> list);
}
