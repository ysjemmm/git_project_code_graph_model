package com.timevale.forward.facade.api.client;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.facade.api.MagicValue;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.TeamMemberVO;
import com.timevale.mandarin.common.annotation.RestClient;

import java.util.List;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@RestClient(serviceId = MagicValue.FORWARD_RPC_PREFIX)
public interface PersonService {
    /**
     * 修改
     *
     * @param recipientAddReq 抄送人信息
     * @return 数量
     */
    BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq);


    /**
     * 查看团队成员
     *
     * @param projectId 项目信息
     * @return 详情信息
     */
    BaseResult<List<TeamMemberVO>> getTeamMembers(Long projectId);

    /**
     * 查看团队成员
     *
     * @return 详情信息
     */
    BaseResult<List<TeamMemberVO>> getProjectMembers();

    /**
     * 获取抄送人
     * @return PersonVO
     */
    BaseResult<List<PersonVO>> getLastCopior();

    /**
     * 辞职通知
     *
     * @param account 花名拼音
     */
    BaseResult<Void> resignNotice(String account);
}
