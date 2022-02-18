package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.FileMapper;
import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dao.ProjectNodeMapper;
import com.timevale.forward.dal.dao.TestBillMapper;
import com.timevale.forward.dal.entity.ProjectNodeDO;
import com.timevale.forward.dal.entity.TestBillDO;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.observer.publisher.MessageEventPublisher;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;


/**
 * @Date 2022/2/16 14:52
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TestBillServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private TestBillServiceImpl testBillServiceImpl;

    @Mock
    private TestBillMapper testBillMapper;

    @Mock
    private FileMapper fileMapper;

    @Mock
    private ProjectNodeMapper projectNodeMapper;

    @Mock
    private ProjectMapper projectMapper;

    @Mock
    private MessageEventPublisher messageEventPublisher;

    @Mock
    private FileComponent fileComponent;

    @Test
    public void testAddTestBill(){
        UserInfo userInfo = new UserInfo();
        userInfo.setAlias("望轩");
        userInfo.setName("轩振营");
        MockedStatic<LocalSessionUtils> localSessionUtilsMockedStatic = mockStatic(LocalSessionUtils.class);
        localSessionUtilsMockedStatic.when(LocalSessionUtils::getUserInfo).thenReturn(userInfo);

        ProjectNodeDO projectNodeDO = new ProjectNodeDO();
        projectNodeDO.setName("提测");
        projectNodeDO.setPlanDate(new Date());
        when(projectNodeMapper.get(any())).thenReturn(Collections.singletonList(projectNodeDO));

        TestBillDO testBillDO = new TestBillDO();
        testBillDO.setCreateManId("wangxuan");
        when(testBillMapper.selectByProjectId(any())).thenReturn(testBillDO);

        assert testBillServiceImpl.addTestBill(3L).ifSuccess();
    }
}





























