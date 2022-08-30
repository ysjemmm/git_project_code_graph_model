package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.ImprovementMeasureMapper;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.dao.TroubleTicketMapper;
import com.timevale.forward.dal.entity.*;
import com.timevale.forward.facade.api.query.TroubleTicketQueryList;
import com.timevale.forward.facade.api.request.TroubleTicketAddReq;
import com.timevale.forward.facade.api.request.TroubleTicketDeleteReq;
import com.timevale.forward.facade.api.request.TroubleTicketModifyReq;
import com.timevale.forward.model.enums.*;
import com.timevale.forward.service.component.BizDemandComponent;
import com.timevale.forward.service.component.FileComponent;
import com.timevale.forward.service.component.ImprovementMeasureComponent;
import com.timevale.forward.service.component.impl.PersonComponentImpl;
import org.assertj.core.util.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockitoTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * @author by YangXu
 * @date 2022/06/10 17:05
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class TroubleTicketServiceImplTest extends AbstractTestNGSpringContextTests {
    
    @InjectMocks
    TroubleTicketServiceImpl troubleTicketService;

    @Mock
    private TroubleTicketMapper troubleTicketMapper;

    @Mock
    private FileComponent fileComponent;

    @Mock
    private ImprovementMeasureMapper improvementMeasureMapper;

    @Mock
    private ImprovementMeasureComponent improvementMeasureComponent;

    @Mock
    private PersonComponentImpl personComponent;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private ProductLineMapper productLineMapper;

    @Mock
    private BizDemandComponent bizDemandComponent;
    
    @Test
    public void testAdd(){
        TroubleTicketAddReq req = new TroubleTicketAddReq();
        req.setName("1");
        req.setFileList(Lists.emptyList());
        req.setHandlerList(Lists.emptyList());
        req.setImprovementMeasureAddReqList(Lists.emptyList());

        when(troubleTicketMapper.insert(any())).thenReturn(1);

        doNothing().when(personComponent).add(any(),any(),any());
        doNothing().when(fileComponent).add(any(),any(),any());

        assert troubleTicketService.add(req).ifSuccess();
    }

    @Test
    public void testModify(){
        TroubleTicketModifyReq req = new TroubleTicketModifyReq();
        req.setId(1L);

        TroubleTicketDO troubleTicketDO = new TroubleTicketDO();
        when(troubleTicketMapper.selectById(any())).thenReturn(troubleTicketDO);

        when(troubleTicketMapper.update(any())).thenReturn(1);

        doNothing().when(personComponent).update(any(),any(),any());
        doNothing().when(fileComponent).update(any(),any(),any());

        assert troubleTicketService.modify(req).ifSuccess();
    }

    @Test
    public void testGet(){

        TroubleTicketDO troubleTicketDO = new TroubleTicketDO();
        troubleTicketDO.setProductLineId(1L);
        troubleTicketDO.setType(TroubleTicketTypeEnum.BIZ_TROUBLE.getCode());
        troubleTicketDO.setReason(TroubleTicketReasonEnum.FUNCTION_PROBLEM.getCode());
        troubleTicketDO.setTroubleRank(TroubleTicketRankEnum.P0.getCode());
        troubleTicketDO.setDuringTime(TroubleTicketDuringTimeEnum.FIVE_TO_TWENTY.getCode());
        troubleTicketDO.setInfluenceScope(TroubleTicketInfluenceScopeEnum.ALL.getCode());
        troubleTicketDO.setDutyTeam(1L);
        when(troubleTicketMapper.selectById(any())).thenReturn(troubleTicketDO);

        ProductLineDO productLineDO = new ProductLineDO();
        productLineDO.setName("1");
        when(productLineMapper.selectById(any())).thenReturn(productLineDO);

        when(bizDemandComponent.getDeptChainName(any())).thenReturn("1");
        when(personComponent.select(any(),any())).thenReturn(Lists.emptyList());
        when(fileComponent.select(1L ,1)).thenReturn(Lists.emptyList());

        assert troubleTicketService.get(1L).ifSuccess();
    }

    @Test
    public void listDelete(){
        TroubleTicketDeleteReq req = new TroubleTicketDeleteReq();
        req.setId(1L);

        TroubleTicketDO troubleTicketDO = new TroubleTicketDO();
        troubleTicketDO.setId(1L);
        when(troubleTicketMapper.selectById(any())).thenReturn(troubleTicketDO);

        ImprovementMeasureDO improvementMeasureDO = new ImprovementMeasureDO();
        improvementMeasureDO.setId(1L);
        when(improvementMeasureMapper.selectByTroubleTicketId(any())).thenReturn(Collections.singletonList(improvementMeasureDO));

        when(troubleTicketMapper.update(any())).thenReturn(1);

        doNothing().when(improvementMeasureComponent).delete(any());

        assert troubleTicketService.delete(req).ifSuccess();
    }

    @Test
    public void testList(){
        TroubleTicketQueryList req = new TroubleTicketQueryList();
        req.setHandlerIdList(Lists.newArrayList("1","2","3"));
        req.setTroubleRankList(Lists.emptyList());
        req.setAscription(AscriptionEnum.CURRENT_USER.getText());

        PersonDO personDO = new PersonDO();
        personDO.setUserId("1");
        personDO.setMainId(1L);
        when(personMapper.select(any())).thenReturn(Lists.newArrayList(personDO));

        TroubleTicketListDO troubleTicketListDO = new TroubleTicketListDO();
        troubleTicketListDO.setId(1L);
        when(troubleTicketMapper.selectList(any())).thenReturn(Lists.newArrayList(troubleTicketListDO));

        assert troubleTicketService.list(req).ifSuccess();
    }
}
