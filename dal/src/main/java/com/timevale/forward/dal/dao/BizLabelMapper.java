package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizLabelDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by xingyun
 * @date 2021/12/15 10:41
 */
public interface BizLabelMapper {

    /**
     * 列表
     *
     * @param labelId labelId
     * @return LabelCategoryDO
     */
    List<BizLabelDO> get(@Param("labelId") Long labelId);

    /**
     * 插入业务-标签
     *
     * @param bizLabelDO 业务-标签
     * @return int
     */
    int insert(BizLabelDO bizLabelDO);

    /**
     *
     * @param bizLabelDOList bizLabelDOList
     * @return int
     */
    int batchInsert(List<BizLabelDO> bizLabelDOList);

    /**
     * 插入业务-标签
     *
     * @param bizLabelDO 业务-标签
     * @return int
     */
    int update(BizLabelDO bizLabelDO);

    /**
     * 列表
     *
     * @param bizId bizId
     * @param type type
     * @return LabelCategoryDO
     */
    List<BizLabelDO> list(@Param("bizId") Long bizId,@Param("type") Integer type);


    /**
     * 列表
     *
     * @param labelIds labelIds
     * @param type type
     * @return LabelCategoryDO
     */
    List<BizLabelDO> getByLabelIdInType(@Param("labelIds") List<Long> labelIds,@Param("type") Integer type);

    /**
     * 得到 by 标签id不在类型
     *
     * @param labelIds 标签id
     * @param type     类型
     * @return {@link List}<{@link BizLabelDO}>
     */
    List<BizLabelDO> getByLabelIdNotInType(@Param("labelIds") List<Long> labelIds,@Param("type") Integer type);

    /**
     * 列表
     *
     * @param bizIds bizIds
     * @param type type
     * @return LabelCategoryDO
     */
    List<BizLabelDO> getByBizIdInType(@Param("bizIds") List<Long> bizIds,@Param("type") Integer type);

}
