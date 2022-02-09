package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class PersonServiceImpl implements PersonService {
    @Resource
    private PersonComponent personComponent;

    @Resource
    private PersonMapper personMapper;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq) {
        // 抄送人
        personComponent.update(recipientAddReq.getRecipients(), recipientAddReq.getMainId(), recipientAddReq.getType());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<PersonVO>> getTeamMembers(Long projectId) {
        List<PersonDO> personDO = personMapper.get(Lists.newArrayList(projectId), PersonTypeEnum.PROJECT_MEMBER.getCode());
        if(CollectionUtils.isEmpty(personDO)){
            return BaseResult.success(Lists.emptyList());
        }
        List<String> accounts = personDO.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        //在职员工
        List<String> employeeOnJob = innerUserPersonClient.getPersonByAccountNew(accounts).stream()
                .filter(a -> Integer.valueOf(0).equals(a.getStatus()))
                .map(BaseInfoResponse::getAccount)
                .collect(Collectors.toList());
        List<PersonDO> result = personDO.stream().filter(a -> employeeOnJob.contains(a.getUserId())).collect(Collectors.toList());
        List<PersonVO> personVO=PersonCopier.INSTANCE.transform(result);
        return BaseResult.success(personVO);
    }

}
