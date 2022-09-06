package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.SearchConditionMapper;
import com.timevale.forward.dal.entity.SearchConditionDO;
import com.timevale.forward.facade.api.query.SearchConditionQueryList;
import com.timevale.forward.facade.api.request.SearchConditionAddReq;
import com.timevale.forward.facade.api.request.SearchConditionDefaultReq;
import com.timevale.forward.facade.api.request.SearchConditionDeleteReq;
import com.timevale.forward.facade.api.request.SearchConditionModifyReq;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author by YangXu
 * @date 2022/06/10 17:43
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class SearchConditionServiceImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    SearchConditionServiceImpl searchConditionService;

    @Mock
    SearchConditionMapper searchConditionMapper;

    @Test
    public void testList(){
        SearchConditionQueryList req = new SearchConditionQueryList();
        req.setModel(1);
        req.setTabType(1);

        SearchConditionDO conditionDO = new SearchConditionDO();
        conditionDO.setIsDefault(false);
        when(searchConditionMapper.select(any(), any(),any())).thenReturn(Lists.newArrayList(conditionDO));

        assert searchConditionService.list(req).ifSuccess();
    }

    @Test
    public void testAdd(){
        SearchConditionAddReq req = new SearchConditionAddReq();
        req.setModel(1);
        req.setTabType(1);
        req.setName("1");

        SearchConditionDO conditionDO = new SearchConditionDO();
        conditionDO.setName("2");
        when(searchConditionMapper.select(any(), any(), any())).thenReturn(Lists.newArrayList(conditionDO));

        when(searchConditionMapper.insert(any())).thenReturn(1);

        assert searchConditionService.add(req).ifSuccess();
    }

    @Test
    public void testUpdate(){
        SearchConditionModifyReq req = new SearchConditionModifyReq();
        when(searchConditionMapper.update(any())).thenReturn(1);

        assert searchConditionService.update(req).ifSuccess();
    }

    @Test
    public void testDelete(){
        SearchConditionDeleteReq req = new SearchConditionDeleteReq();
        req.setId(1L);

        when(searchConditionMapper.update(any())).thenReturn(1);

        assert searchConditionService.delete(req).ifSuccess();
    }

    @Test
    public void setDefault(){
        SearchConditionDefaultReq req = new SearchConditionDefaultReq();
        req.setId(1L);
        req.setModel(1);
        req.setTabType(1);

        SearchConditionDO conditionDO = new SearchConditionDO();
        conditionDO.setId(1L);
        conditionDO.setIsDefault(false);
        when(searchConditionMapper.select(any(),any(),any())).thenReturn(Lists.newArrayList(conditionDO));

        assert searchConditionService.setDefault(req).ifSuccess();

    }
}
