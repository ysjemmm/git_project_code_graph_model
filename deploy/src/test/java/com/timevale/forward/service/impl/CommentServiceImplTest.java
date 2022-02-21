package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.CommentMapper;
import com.timevale.forward.dal.dao.TaskMapper;
import com.timevale.forward.dal.entity.CommentDO;
import com.timevale.forward.dal.entity.TaskDO;
import com.timevale.forward.facade.api.query.CommentQueryList;
import com.timevale.forward.facade.api.query.PersonQuery;
import com.timevale.forward.facade.api.request.CommentAddReq;
import com.timevale.forward.service.observer.event.CommentMsgEvent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @Date 2022/2/17 19:45
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class CommentServiceImplTest extends AbstractTestNGSpringContextTests {
    @Mock
    TaskMapper taskMapper;
    @Mock
    CommentMapper commentMapper;
    @Mock
    MessageEventPublisher messageEventPublisher;
    @InjectMocks
    private CommentServiceImpl commentService;

    @Test
    public void testList() {

        CommentDO commentDO = new CommentDO();
        commentDO.setContent("www");
        when(commentMapper.select(any(), any())).thenReturn(Collections.singletonList(commentDO));

        CommentQueryList commentQueryList = new CommentQueryList();
        commentQueryList.setToId(1L);
        commentQueryList.setType(1);

        assert commentService.list(commentQueryList).ifSuccess();
    }

    @Test
    public void testAdd() {
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        when(commentMapper.insert(any())).thenReturn(1);

        TaskDO taskDO = new TaskDO();
        taskDO.setName("www");
        when(taskMapper.getById(any())).thenReturn(taskDO);

        MockedConstruction<CommentMsgEvent> commentMsgEventMockedConstruction = mockConstruction(CommentMsgEvent.class);
        commentMsgEventMockedConstruction.constructed();
        doNothing().when(messageEventPublisher).publish(any());

        CommentAddReq commentAddReq = new CommentAddReq();
        PersonQuery personQuery = new PersonQuery();
        personQuery.setUserId("www");
        commentAddReq.setReceiverInfoList(Collections.singletonList(personQuery));
        commentAddReq.setToId(1L);
        commentAddReq.setType(100);

        assert commentService.add(commentAddReq).ifSuccess();
        commentMsgEventMockedConstruction.close();
        localSessionUtilsMockedStatic.close();
    }
}

































