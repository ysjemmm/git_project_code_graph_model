package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.BizRecordDO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2023/05/15 15:43
 */
public interface BizRecordMapper {

    void insert(BizRecordDO bizRecordDO);

    void batchInsert(@Param("records") Collection<BizRecordDO> records);

    @Select("SELECT * FROM biz_record WHERE main_id=#{mainId} AND main_type=#{mainType} AND is_deleted=false")
    List<BizRecordDO> get(@Param("mainId")Long mainId, @Param("mainType")Integer mainType);

    @Select("SELECT * FROM biz_record where main_type=#{mainType}")
    List<BizRecordDO> getByMainType(@Param("mainType")Integer mainType);

    @Select("SELECT * FROM biz_record WHERE main_id=#{mainId} AND main_type=#{mainType} AND is_deleted=false ORDER BY create_date DESC LIMIT 1")
    BizRecordDO getLast(@Param("mainId")Long mainId, @Param("mainType")Integer mainType);

    @Update("UPDATE biz_record SET record=#{record},modify_man=#{modifyMan},modify_man_id=#{modifyManId} WHERE id=#{id}")
    void updateRecord(@Param("id")Long id, @Param("record")String record);
}