package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.facade.api.query.BizChangeLogQueryList;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author by YangXu
 * @date 2022/06/06 10:23
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BizChangeLogServiceImplTest  extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private BizChangeLogServiceImpl bizChangeLogService;

    @Mock
    private BizChangeLogMapper bizChangeLogMapper;

    @Test
    public void testList(){
        BizChangeLogDO bizChangeLogDO = new BizChangeLogDO();
        when(bizChangeLogMapper.list(any(),any())).thenReturn(Collections.singletonList(bizChangeLogDO));

        BizChangeLogQueryList bizChangeLogQueryList = new BizChangeLogQueryList();
        bizChangeLogQueryList.setPageNum(1);
        bizChangeLogQueryList.setPageNum(10);
        assert bizChangeLogService.list(bizChangeLogQueryList).ifSuccess();

    }

}
