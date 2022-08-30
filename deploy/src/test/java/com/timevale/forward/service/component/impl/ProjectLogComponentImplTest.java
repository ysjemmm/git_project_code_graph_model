package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.*;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.BizDemandLogComponent;
import com.timevale.forward.service.component.PersonComponent;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class ProjectLogComponentImplTest extends AbstractTestNGSpringContextTests {

    @InjectMocks
    private ProjectLogComponentImpl projectLogComponent;

    @Mock
    private BizChangeLogMapper bizChangeLogMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private ProjectProductLineMapper projectProductLineMapper;

    @Mock
    private ProjectProductDemandMapper projectProductDemandMapper;

    @Mock
    private ProductBizDemandMapper productBizDemandMapper;

    @Mock
    private BizDemandComponent bizDemandComponent;

    @Mock
    private BizDemandLogComponent bizDemandLogComponent;

    @Test
    public void testAddLogWhenModifyData() {
        ProjectDO oldObj=new ProjectDO();
        ProjectDO newObj=new ProjectDO();
        newObj.setProductLineIds(Lists.newArrayList(1L));
        newObj.setPds(Lists.newArrayList(new PersonDO(){{
            setUserId("1");
            setUserName("1");
        }}));
        Date date = new Date();
        newObj.setPlanEndDate(date);
        when(projectProductLineMapper.get(any())).thenReturn(Lists.newArrayList());
        when(personComponent.select(any(),any())).thenReturn(Lists.newArrayList());
        when(projectProductDemandMapper.getByProjectId(any())).thenReturn(Lists.newArrayList(new ProjectProductDemandDO(){{setProductDemandId(1L);}}));
        when(productBizDemandMapper.getByProductDemandIds(any())).thenReturn(Lists.newArrayList(new ProductBizDemandDO(){{setBizDemandId(1L);}}));
        when(bizDemandComponent.getProjectEndDate(any())).thenReturn(date);
        when(bizDemandLogComponent.buildLogWhenPublishDateChange(any(),any(),any())).thenReturn(new BizChangeLogDO());
        projectLogComponent.addLogWhenModifyData(oldObj,newObj);
    }

    @Test
    public void testAddLogWhenStatusChange() {
        when(bizChangeLogMapper.insert(any())).thenReturn(1);
        projectLogComponent.addLogWhenStatusChange(1,1,1L,"");
    }

    @Test
    public void testAddLogWhenLinkOrUnlink() {
        Map<Long, String> pdNameMap=new HashMap<>();
        pdNameMap.put(1L,"");
        when(bizChangeLogMapper.insert(any())).thenReturn(1);
        projectLogComponent.addLogWhenLinkOrUnlink("",1L,pdNameMap,"");
    }
}

































