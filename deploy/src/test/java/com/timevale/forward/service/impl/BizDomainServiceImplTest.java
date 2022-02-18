package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/17 19:41
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class BizDomainServiceImplTest extends AbstractTestNGSpringContextTests {
    @Mock
    BizDomainMapper bizDomainMapper;
    @InjectMocks
    private BizDomainServiceImpl bizDomainService;

    @Test
    public void testBizDomainList() {
        BizDomainDO bizDomainDO = new BizDomainDO();
        bizDomainDO.setType(1);
        when(bizDomainMapper.selectAllBizDomain()).thenReturn(Collections.singletonList(bizDomainDO));

        assert bizDomainService.bizDomainList().ifSuccess();
    }
}