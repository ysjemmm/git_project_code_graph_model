package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
@RestService
public class PersonServiceImpl implements PersonService {
    @Resource
    private PersonComponent personComponent;

    @Override
    public BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq) {
        // 抄送人
        personComponent.update(recipientAddReq.getRecipients(), recipientAddReq.getMainId(), recipientAddReq.getType());
        return BaseResult.success(true);
    }
}
