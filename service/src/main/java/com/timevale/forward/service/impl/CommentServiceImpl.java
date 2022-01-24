package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.CommentMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.facade.api.client.CommentService;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.CommentCopier;
import com.timevale.forward.service.observer.event.CommentMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class CommentServiceImpl implements CommentService {

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    CommentMapper commentMapper;

    @Resource
    MessageEventPublisher messageEventPublisher;

    @Override
    public BaseResult<List<CommentVO>> list(CommentQueryList commentQueryList) {
        log.info("评论列表接收参数:{}", commentQueryList);

        Long toId = commentQueryList.getToId();
        Integer type = commentQueryList.getType();

        List<CommentDO> commentDOList = commentMapper.select(toId, type);
        List<CommentVO> commentVOList = CommentCopier.INSTANCE.convert(commentDOList);

        return BaseResult.success(commentVOList);
    }

    @Override
    public BaseResult<Boolean> add(CommentAddReq commentAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 添加评论
        CommentDO commentDO = CommentCopier.INSTANCE.convert(commentAddReq);
        commentMapper.insert(commentDO);

        // 评论接收人
        List<String> receivers = commentAddReq.getReceiverInfoList().stream().map(PersonQuery::getUserId).collect(Collectors.toList());
        if(receivers.isEmpty()){return BaseResult.success(true);}

        // 查询对应业务需求/产品需求/项目名称
        String name;
        Long toId = commentAddReq.getToId();
        Integer type = commentAddReq.getType();
        if(type.equals(CommentTypeEnum.PROJECT.getCode())){
            name = projectMapper.get(toId).getName();
        }else if(type.equals(CommentTypeEnum.PRODUCT_DEMAND.getCode())){
            name = productDemandMapper.selectById(toId).getName();
        }else{
            name = bizDemandMapper.selectById(toId).getName();
        }

        // 发送通知
        messageEventPublisher.publish(new CommentMsgEvent(
                this,
                toId,
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                receivers,
                CommentTypeEnum.getTextByCode(type),
                name,
                commentDO.getContent()
        ));

        return BaseResult.success(true);
    }

}
