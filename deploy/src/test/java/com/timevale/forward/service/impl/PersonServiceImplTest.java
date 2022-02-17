package com.timevale.forward.service.impl;

import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.security.facade.response.BaseInfoResponse;
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
 * @Date 2022/2/17 20:07
 * @Author 望轩
 */
@TestExecutionListeners(listeners = MockitoTestExecutionListener.class)
public class PersonServiceImplTest extends AbstractTestNGSpringContextTests {
    @InjectMocks
    private PersonServiceImpl personService;

    @Mock
    private PersonComponent personComponent;

    @Mock
    private PersonMapper personMapper;

    @Mock
    private InnerUserPersonClient innerUserPersonClient;

    @Test
    public void testAddRecipients() {

        RecipientAddReq recipientAddReq = new RecipientAddReq();
        PersonAddReq personAddReq = new PersonAddReq();
        personAddReq.setUserId("www");
        recipientAddReq.setRecipients(Collections.singletonList(personAddReq));

        doNothing().when(personComponent).update(any(), any(), any());

        assert personService.addRecipients(recipientAddReq).ifSuccess();
    }

    @Test
    public void testGetTeamMembers() {
        PersonDO personDO = new PersonDO();
        personDO.setUserId("www");
        when(personMapper.get(any(), any())).thenReturn(Collections.singletonList(personDO));

        BaseInfoResponse baseInfoResponse = new BaseInfoResponse();
        baseInfoResponse.setAccount("www");
        baseInfoResponse.setAlias("www");
        baseInfoResponse.setName("www");
        baseInfoResponse.setStatus(1);
        when(innerUserPersonClient.getPersonByAccountNew(any())).thenReturn(Collections.singletonList(baseInfoResponse));

        assert personService.getTeamMembers(1L).ifSuccess();
    }
}

































