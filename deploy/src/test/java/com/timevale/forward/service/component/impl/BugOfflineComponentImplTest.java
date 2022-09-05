package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOfflineMapper;
import com.timevale.forward.dal.entity.BugOfflineDO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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


    @Test
    public void testUpdateEndDate() {
        BugOfflineDO bugOfflineDO = new BugOfflineDO();
        bugOfflineDO.setProductLineId(1L);
        List<BugOfflineDO> objects = Lists.newArrayList(bugOfflineDO);
        when(bugOfflineMapper.selectByProjectId(any())).thenReturn(objects);
        bugOfflineComponent.containProductLineInBugOffline(1L,Lists.newArrayList(1L));
    }


}

































