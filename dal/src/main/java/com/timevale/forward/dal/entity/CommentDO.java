package com.timevale.forward.dal.entity;

import com.timevale.mandarin.common.result.ToString;
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
    Long toId;

    /**
     * 主体类型
     */
    Integer type;

    /**
     * 评论内容
     */
    String content;

}
