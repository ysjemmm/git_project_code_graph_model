package com.timevale.forward.service.copy;

import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.CommentModifyReq;
import com.timevale.forward.facade.api.result.CommentVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author by YangXu
 * @date 2021/12/21 18:43
 */
@Mapper
public interface CommentCopier {

    CommentCopier INSTANCE = Mappers.getMapper(CommentCopier.class);

    /**
     * 转换
     *
     * @param commentAddReq 评论添加请求
     * @return DO
     */
    CommentDO convert(CommentAddReq commentAddReq);

    /**
     * 转换
     *
     * @param commentModifyReq 评论修改请求
     * @return DO
     */
    CommentDO convert(CommentModifyReq commentModifyReq);

    /**
     * 转换
     *
     * @param commentDOList DO列表
     * @return VO列表
     */
    List<CommentVO> convert(List<CommentDO> commentDOList);

    /**
     * CommentDO  -->  CommentVO
     *
     * @param commentDO 参数
     * @return CommentVO 返回值
     */
    CommentVO change(CommentDO commentDO);
}
