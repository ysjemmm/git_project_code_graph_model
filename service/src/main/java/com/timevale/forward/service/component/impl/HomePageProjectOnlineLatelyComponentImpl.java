package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.ProjectMapper;
import com.timevale.forward.dal.dto.HomePageProjectOnlineLatelyDTO;
import com.timevale.forward.facade.api.query.HomePageProjectOnlineLatelyQueryList;
import com.timevale.forward.model.enums.HomePageTabEnum;
import com.timevale.forward.service.component.HomePageProjectOnlineLatelyComponent;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.integration.superset.model.base.PageResult;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author by YangXu
 * @date 2022/01/24 10:05
 */
@Slf4j
@Component
public class HomePageProjectOnlineLatelyComponentImpl implements HomePageProjectOnlineLatelyComponent {

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private InnerUserPersonClient innerUserPersonClient;

    @Override
    public PageResult<HomePageProjectOnlineLatelyDTO> getProjectOnlineLately(HomePageProjectOnlineLatelyQueryList query) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        List<String> userIds;
        if (HomePageTabEnum.INDIVIDUAL.getCode().equals(query.getTabType())) {
            userIds = Lists.newArrayList(userInfo.getId());
        } else {
            userIds = innerUserPersonClient.getAllMyStaffWithSelf(userInfo.getId(), true);
        }

        int offset = (query.getPageNum() - 1) * query.getPageSize();
        List<HomePageProjectOnlineLatelyDTO> list = projectMapper.selectProjectOnlineLately(userIds, offset, query.getPageSize());
        int total = projectMapper.countProjectOnlineLately(userIds);

        PageResult<HomePageProjectOnlineLatelyDTO> pageResult = new PageResult<>();
        pageResult.setResult(list);
        pageResult.setTotal(total);
        return pageResult;
    }
}
