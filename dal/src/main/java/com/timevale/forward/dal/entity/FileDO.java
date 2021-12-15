package com.timevale.forward.dal.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @Date 2021/12/15 11:18
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FileDO extends BaseDO {

    /**
     * 附件所属id
     */
    Long attacheId;

    /**
     * 附件所属类型
     */
    Integer type;

    /**
     * 附件名称
     */
    String name;

    /**
     * 文件id
     */
    String fileId;
}
