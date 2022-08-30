package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.FileDO;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/15 11:28
 */
public interface FileMapper {

    /**
     * 新增单条附件信息
     *
     * @param fileDO 附件DO
     * @return int
     */
    int insert(FileDO fileDO);

    /**
     * 批量新增
     *
     * @param list 附件信息列表
     * @return int
     */
    int inserts(@Param("list") List<FileDO> list);


    /**
     * 查询所属附件
     *
     * @param attacheId 附件所属id
     * @param type      附件所属类型
     * @return 列表
     */
    List<FileDO> select(@Param("attacheId") Long attacheId, @Param("type") Integer type);

    /**
     * 查询所属附件
     *
     * @param attacheIdList 附件所属id列表
     * @param type      附件所属类型
     * @return 列表
     */
    List<FileDO> selectByAttacheIdList(@Param("attacheIdList") Collection<Long> attacheIdList, @Param("type") Integer type);

    /**
     * 删除
     *
     * @param fileDO 附件信息
     * @return int
     */
    int update(FileDO fileDO);

    /**
     * 删除
     *
     * @param fileDO fileDO
     * @return int
     */
    int updateFileId(FileDO fileDO);

}
