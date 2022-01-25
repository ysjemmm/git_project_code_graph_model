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
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.List;

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

    @Override
    public BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq) {
        // 抄送人
        personComponent.update(recipientAddReq.getRecipients(), recipientAddReq.getMainId(), recipientAddReq.getType());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<PersonVO>> getTeamMembers(Long projectId) {
        List<PersonDO> personDO = personMapper.get(Lists.newArrayList(projectId), PersonTypeEnum.PROJECT_MEMBER.getCode());
        List<PersonVO> personVO=PersonCopier.INSTANCE.transform(personDO);
        return BaseResult.success(personVO);
    }

}
