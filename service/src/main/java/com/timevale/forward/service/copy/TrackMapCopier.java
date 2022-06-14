package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.dal.entity.TrackMapDO;
import com.timevale.forward.facade.api.result.TrackMapVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 10:30
 */
@Mapper
public interface TrackMapCopier {

    TrackMapCopier INSTANCE = Mappers.getMapper(TrackMapCopier.class);


    /**
     * 批量处理
     *
     * @param trackMapDOList  trackMapDOList
     * @return 列表
     */
    List<TrackMapVO> convert(List<TrackMapDO> trackMapDOList);

    /**
     *
     * @param modelDO modelDO
     * @return return
     */
    @Mapping(source = "productLineId", target = "parentId")
    TrackMapVO convert(ModelDO modelDO);


    /**
     *
     * @param productLineDO productLineDO
     * @return return
     */
    @Mapping(source = "bizDomainId", target = "parentId")
    TrackMapVO convert(ProductLineDO productLineDO);
    /**
     * 批量处理
     *
     * @param modelDOList  modelDOList
     * @return 列表
     */
    List<TrackMapVO> modelConvert(List<ModelDO> modelDOList);

    /**
     * 批量处理
     *
     * @param productLineDOList  productLineDOList
     * @return 列表
     */
    List<TrackMapVO> productLineConvert(List<ProductLineDO> productLineDOList);

    /**
     * 批量处理
     *
     * @param bizDomainDOList  bizDomainDOList
     * @return 列表
     */
    List<TrackMapVO> bizDomainConvert(List<BizDomainDO> bizDomainDOList);

}
