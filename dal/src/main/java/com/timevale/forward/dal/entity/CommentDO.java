package com.timevale.forward.dal.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author by YangXu
 * @date 2021/12/21 18:38
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class CommentDO extends BaseDO {

    /**
     * 主体Id
     */
    private Long toId;

    /**
     * 主体类型
     */
    private Integer type;

    /**
     * 评论内容
     */
    private String content;

}
