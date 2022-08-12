package com.timevale.forward.dal.dao;

import com.timevale.forward.dal.entity.TrackImportLogDO;

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

}
