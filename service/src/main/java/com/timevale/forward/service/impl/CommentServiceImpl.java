package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.BizDemandMapper;
import com.timevale.forward.dal.dao.CommentMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.facade.api.client.BizDemandService;
import com.timevale.forward.facade.api.client.CommentService;
import com.timevale.forward.facade.api.client.ProductDemandService;
import com.timevale.forward.facade.api.client.ProjectService;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.MessageTitleEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.copy.CommentCopier;
import com.timevale.forward.service.integration.erp.ErpMessageClient;
import com.timevale.forward.service.integration.erp.model.ActionCardMsg;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import javax.jms.Message;
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
    ErpMessageClient erpMessageClient;

    @Resource
    BizDemandMapper bizDemandMapper;

    @Resource
    ProductDemandMapper productDemandMapper;

    @Resource
    ProjectMapper projectMapper;

    @Resource
    CommentMapper commentMapper;

    @Override
    public BaseResult<List<CommentVO>> list(CommentQueryList commentQueryList) {
        log.info("项目列表接收参数:{}", commentQueryList);

        Long toId = commentQueryList.getToId();
        Byte type = commentQueryList.getType();

        List<CommentDO> commentDOList = commentMapper.select(toId, type);
        List<CommentVO> commentVOList = CommentCopier.INSTANCE.convert(commentDOList);

        return BaseResult.success(commentVOList);
    }

    @Override
    public BaseResult<Boolean> add(CommentAddReq commentAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 添加评论
        CommentDO commentDO = CommentCopier.INSTANCE.convert(commentAddReq);
        commentDO.setCreateMan(userInfo.getAlias());
        commentDO.setCreateManId(userInfo.getId());
        commentMapper.insert(commentDO);

        // 查询对应业务需求/产品需求/项目名称
        String name = "";
        Long toId = commentAddReq.getToId();
        Byte type = commentAddReq.getType();
        if(type.equals(CommentTypeEnum.PROJECT.getCode())){
            name = projectMapper.get(toId).getName();
        }else if(type.equals(CommentTypeEnum.PRODUCT_DEMAND.getCode())){
            name = productDemandMapper.selectById(toId).getName();
        }else{
            name = bizDemandMapper.selectById(toId).getName();
        }

        // 通知被@人(待实现）
        /*String creatMan = userInfo.getAlias();
        String content = commentAddReq.getContent();
        String title = MessageTitleEnum.COMMENT.getText();
        String commentType = CommentTypeEnum.getTextByCode(commentAddReq.getType());
        List<String> receivers = commentAddReq.getReceiverInfoList().stream().map(PersonQuery::getUserId).collect(Collectors.toList());

        String markdown = String.format("%s评论了%s%s：%s，可进入产研项目管理系统查看",creatMan, commentType, name, content);

        ActionCardMsg actionCardMsg = ActionCardMsg.builder()
                .title(title)
                .markdown(markdown)
                .singleUrl("https://www.baidu.com/")
                .receivers(receivers)
                .build();
        erpMessageClient.sendActionCardMsg(actionCardMsg);
*/
        return BaseResult.success(true);
    }

}
