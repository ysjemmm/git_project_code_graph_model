package com.timevale.forward.service.component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.security.facade.response.BaseInfoResponse;
import com.timevale.security.facade.response.GroupModelResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jingchun
 * created on 2023/2/13
 */
@Component
@RequiredArgsConstructor
public class UserComponent {
    private final InnerUserPersonClient personClient;

    @Value("${evalPmoGroup:557300580}")
    private String EVAL_PMO_GROUP;

    /**
     * 查询是否PMO或者PMO上级
     */
    public boolean isPmoOrPmoLeader() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BaseInfoResponse> users =
                personClient.getAllMyStaffWithSelfInfo(userInfo.getId(), false);
        for (BaseInfoResponse user : users) {
            if (CommonConstant.PMO.equalsIgnoreCase(user.getJobClassification())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 查询是否评价部门下的PMO或者PMO上级
     */
    public boolean isEvalPmo() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        BaseInfoResponse selfInfo = personClient.getSelfInfo(userInfo.getId(), false);

        GroupModelResponse evalGroup = CollUtil.findOne(selfInfo.getGroupList(), e -> EVAL_PMO_GROUP.equals(e.getGroupId()));
        return evalGroup != null && CommonConstant.PMO.equalsIgnoreCase(selfInfo.getJobClassification());
    }

    public boolean isPmo() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<BaseInfoResponse> personList = personClient.getPersonByAccountNew(Collections.singletonList(userInfo.getId()));
        for (BaseInfoResponse person : personList) {
            if (CommonConstant.PMO.equalsIgnoreCase(person.getJobClassification())) {
                return true;
            }
        }
        return false;
    }

    public List<String> getPmo(String groupId) {
        List<BaseInfoResponse> baseInfoList = personClient.getBaseInfoByGroupId(groupId);
        return baseInfoList.stream()
                .filter(e -> ObjectUtil.equal(CommonConstant.PMO, e.getJobClassification()))
                .map(BaseInfoResponse::getAccount)
                .collect(Collectors.toList());
    }


}
