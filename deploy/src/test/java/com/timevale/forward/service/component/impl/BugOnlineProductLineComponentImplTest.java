package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineProductLineMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BugOnlineProductLineComponentImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private BugOnlineProductLineComponentImpl bugOnlineProductLineComponent;
    @Mock
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;


    @Test
    public void testUpdateEndDate() {
        bugOnlineProductLineComponent.update(Lists.newArrayList(1L),1L);
    }


}

































