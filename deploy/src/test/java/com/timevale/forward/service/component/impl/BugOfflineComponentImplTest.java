package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOfflineMapper;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BugOfflineComponentImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private BugOfflineComponentImpl bugOfflineComponent;
    @Mock
    private BugOfflineMapper bugOfflineMapper;
}

































