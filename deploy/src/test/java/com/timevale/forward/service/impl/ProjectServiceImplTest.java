package com.timevale.forward.service.impl;

import org.mockito.InjectMocks;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectServiceImpl projectService;

    @Test
    public void testUpdateStatus(){
    }


}