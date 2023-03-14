package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.dal.entity.FileDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.client.CommentService;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.facade.api.request.CommentBatchAddReq;
import com.timevale.forward.facade.api.request.FileAddReq;
import com.timevale.forward.facade.api.result.CommentVO;
import com.timevale.forward.facade.api.result.FileVO;
import com.timevale.forward.model.enums.CommentTypeEnum;
import com.timevale.forward.model.enums.FileTypeEnum;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.CommentCopier;
import com.timevale.forward.service.copy.FileCopier;
import com.timevale.forward.service.observer.event.CommentMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.aop.LogPoint;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@LogPoint
@RestService
public class CommentServiceImpl implements CommentService {

    @Resource
    private BizDemandMapper bizDemandMapper;

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private TaskMapper taskMapper;

    @Resource
    private BugOfflineMapper bugOfflineMapper;

    @Resource
    private BugOnlineMapper bugOnlineMapper;

    @Resource
    private TroubleTicketMapper troubleTicketMapper;

    @Resource
    private CommentMapper commentMapper;

    @Resource
    private MessageEventPublisher messageEventPublisher;

    @Resource
    private FileComponent fileComponent;

    @Resource
    private CustomDemandMapper customDemandMapper;

    private final Map<CommentTypeEnum, Function<Long, String>> commentMainNameFun =
            new HashMap<>();

    @PostConstruct
    public void initNameMap() {
        commentMainNameFun.put(CommentTypeEnum.PROJECT, id -> projectMapper.get(id).getName());
        commentMainNameFun.put(CommentTypeEnum.PRODUCT_DEMAND, id -> productDemandMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.BIZ_DEMAND, id -> bizDemandMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.TASK, id -> taskMapper.getById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.BUG_OFFLINE, id -> bugOfflineMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.BUG_ONLINE, id -> bugOnlineMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.TROUBLE_TICKET, id -> troubleTicketMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.CUSTOM_DEMAND, id -> customDemandMapper.selectById(id).getName());
        commentMainNameFun.put(CommentTypeEnum.INNER_PROJECT, id -> projectMapper.get(id).getName());
        commentMainNameFun.put(CommentTypeEnum.INNER_TASK, id -> taskMapper.getById(id).getName());
    }

    @Override
    public BaseResult<List<CommentVO>> list(CommentQueryList commentQueryList) {

        Long toId = commentQueryList.getToId();
        Integer type = commentQueryList.getType();

        List<CommentDO> commentDOList = commentMapper.select(toId, type);
        List<CommentVO> commentVOList = CommentCopier.INSTANCE.convert(commentDOList);

        // 查询评论对应附件
        List<Long> commentIdList = commentDOList.stream().map(CommentDO::getId).collect(Collectors.toList());
        List<FileDO> fileDOList = fileComponent.select(commentIdList, FileTypeEnum.COMMENT.getCode());
        Map<Long, List<FileDO>> fileMap = fileDOList.stream().collect(Collectors.groupingBy(FileDO::getAttacheId));

        for (CommentVO e : commentVOList) {
            List<FileVO> fileVOList = new ArrayList<>();
            if (fileMap.containsKey(e.getId())) {
                fileVOList = FileCopier.INSTANCE.transform(fileMap.get(e.getId()));
            }
            e.setFileList(fileVOList);
        }

        return BaseResult.success(commentVOList);
    }

    @Override
    public BaseResult<Boolean> add(CommentAddReq commentAddReq) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        // 添加评论
        CommentDO commentDO = CommentCopier.INSTANCE.convert(commentAddReq);
        commentMapper.insert(commentDO);

        // 添加附件
        List<FileAddReq> fileList = commentAddReq.getFileList();
        fileComponent.add(fileList, commentDO.getId(), FileTypeEnum.COMMENT.getCode());

        // 评论接收人
        List<String> receivers = commentAddReq.getReceiverInfoList().stream().map(PersonQuery::getUserId).collect(Collectors.toList());
        if (receivers.isEmpty()) {
            return BaseResult.success(true);
        }

        Long toId = commentAddReq.getToId();
        Integer type = commentAddReq.getType();
        // 查询对应业务需求/产品需求/项目名称
        CommentTypeEnum commentType = CommentTypeEnum.getByCode(type);
        String name = commentMainNameFun.getOrDefault(commentType, id -> "").apply(toId);

        // 任务特殊处理
        Long projectId = null;
        if (CommentTypeEnum.TASK.getCode().equals(type)) {
            TaskDO taskDO = taskMapper.getById(toId);
            projectId = taskDO.getProjectId();
        }

        // 发送通知
        messageEventPublisher.publish(new CommentMsgEvent(
                this,
                toId,
                userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName(),
                receivers,
                commentType,
                name,
                commentDO.getContent(),
                projectId
        ));

        return BaseResult.success(true);
    }

    @Override
    public BaseResult<Boolean> add(CommentBatchAddReq commentBatchAddReq) {
        List<Long> toIds = commentBatchAddReq.getToIds();
        toIds.forEach(a -> {
            CommentDO commentDO = new CommentDO();
            commentDO.setContent(commentBatchAddReq.getContent());
            commentDO.setToId(a);
            commentDO.setType(commentBatchAddReq.getType());
            commentMapper.insert(commentDO);
        });
        return BaseResult.success(true);
    }

}
