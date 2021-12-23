package com.timevale.forward.dal.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/15 11:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileDO extends BaseDO {

    /**
     * 附件所属id
     */
    private Long attacheId;

    /**
     * 附件所属类型
     */
    private Integer type;

    /**
     * 附件名称
     */
    private String name;

    /**
     * 文件id
     */
    private String fileId;
}
