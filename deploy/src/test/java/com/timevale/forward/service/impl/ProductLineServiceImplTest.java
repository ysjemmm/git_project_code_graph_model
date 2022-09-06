package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.BizDomainMapper;
import com.timevale.forward.dal.dao.ModelMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BizDomainDO;
import com.timevale.forward.dal.entity.ModelDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;
import org.testng.collections.Lists;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @Date 2022/2/18 14:15
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProductLineServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private ProductLineServiceImpl productLineService;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private BizDomainMapper bizDomainMapper;

    @Mock
    ModelMapper modelMapper;

    @Test
    public void testProductLineList() {
        BizDomainDO bizDomainDO = new BizDomainDO();
        bizDomainDO.setId(1L);
        when(bizDomainMapper.selectAllBizDomain()).thenReturn(Collections.singletonList(bizDomainDO));

        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setBizDomainId(1L);
        when(productLineMapper.selectAllProductLine()).thenReturn(Collections.singletonList(productLineDO));

        assert productLineService.productLineList().ifSuccess();
    }

    @Test
    public void testGetProductLines() {
        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setName("www");
        when(productLineMapper.get(any())).thenReturn(Collections.singletonList(productLineDO));

        assert productLineService.getProductLines(1L).ifSuccess();
    }

    @Test
    public void testListProductLineModes() {
        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setName("www");
        when(productLineMapper.selectAllProductLine()).thenReturn(Lists.newArrayList(new ProductLineDO()));
        when(modelMapper.selectAllModel()).thenReturn(Lists.newArrayList(new ModelDO(){{setProductLineId(1L);}}));

        assert productLineService.listProductLineModes().ifSuccess();
    }


}