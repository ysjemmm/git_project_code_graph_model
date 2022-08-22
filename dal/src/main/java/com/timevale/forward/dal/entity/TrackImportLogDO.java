package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * @author by YangXu
 * @date 2022/08/12 13:44
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TrackImportLogDO extends BaseDO {

    /**
     * 处理状态：0处理成功，1处理失败
     */
    private Integer status;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 导入事件数
     */
    private Integer importCount;

    /**
     * 导入事件错误数
     */
    private Integer importFailCount;

    /**
     * 导入结果：0导入成功，1导入失败
     */
    private Integer result;
}
