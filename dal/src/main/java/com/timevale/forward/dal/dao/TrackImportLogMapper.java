package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TrackImportLogDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author by YangXu
 * @date 2022/08/12 13:46
 */
public interface TrackImportLogMapper {

    /**
     * 插入
     *
     * @param trackImportLogDO 跟踪进口日志DO
     */
    void insert(TrackImportLogDO trackImportLogDO);

    /**
     * 查询全部
     *
     * @return {@link List}<{@link TrackImportLogDO}>
     */
    List<TrackImportLogDO> selectAll();

    /**
     * 查询-通过创建人
     *
     * @param userId 用户id
     * @return {@link List}<{@link TrackImportLogDO}>
     */
    List<TrackImportLogDO> selectByCreateManId(@Param("userId")String userId);

    /**
     * 选择最新记录
     *
     * @param userId 用户id
     * @return {@link TrackImportLogDO}
     */
    TrackImportLogDO selectCreateLatest(@Param("userId") String userId);

}
