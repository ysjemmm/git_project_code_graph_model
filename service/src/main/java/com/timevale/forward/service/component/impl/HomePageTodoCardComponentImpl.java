package com.timevale.forward.service.component.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.service.component.HomePageTodoCardComponent;
import com.timevale.forward.facade.api.result.HomePageTodoCardVO;
import com.timevale.forward.service.integration.superset.client.impl.BaseDistributeClientImpl;
import com.timevale.mandarin.common.annotation.RestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author by YangXu
 * @date 2022/01/24 10:10
 */
@Slf4j
@Component
public class HomePageTodoCardComponentImpl extends BaseDistributeClientImpl<HomePageTodoCardVO> implements HomePageTodoCardComponent {
    @Override
    public BaseResult<HomePageTodoCardVO> getTodoCard() {
        return null;
    }
}
