package com.timevale.forward.dal.entity;

import lombok.Data;

import java.util.Date;

/**
 * @Description: 快捷搜索条件实体
 * @ClassName: FastSearchConditionDO
 * @Author: shaoye
 * @Date: 2023-08-22 14:39
 */
@Data
public class FastSearchConditionDO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 搜索用户ID
     */
    private String searchUserId;

    /**
     * 搜索条件内容
     */
    private String searchConditionContent;

    /**
     * 创建时间
     */
    private Date createDate;

    /**
     * 修改时间
     */
    private Date modifyDate;

}
