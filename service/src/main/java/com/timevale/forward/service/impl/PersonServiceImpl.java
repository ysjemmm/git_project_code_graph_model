package com.timevale.forward.service.impl;

import com.timevale.footstone.base.model.response.BaseResult;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.dao.ProductDemandMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.facade.api.client.PersonService;
import com.timevale.forward.facade.api.request.RecipientAddReq;
import com.timevale.forward.facade.api.result.PersonVO;
import com.timevale.forward.facade.api.result.TeamMemberVO;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.integration.inneruser.InnerUserPersonClient;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import com.timevale.mandarin.common.annotation.RestService;
import com.timevale.security.facade.response.BaseInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Resource
    private ProductDemandMapper productDemandMapper;

    @Override
    public BaseResult<Boolean> addRecipients(RecipientAddReq recipientAddReq) {
        // 抄送人
        personComponent.update(recipientAddReq.getRecipients(), recipientAddReq.getMainId(), recipientAddReq.getType());
        return BaseResult.success(true);
    }

    @Override
    public BaseResult<List<TeamMemberVO>> getTeamMembers(Long projectId) {
        List<PersonDO> personDOList = personMapper.get(Lists.newArrayList(projectId), PersonTypeEnum.PROJECT_MEMBER.getCode());
        if (CollectionUtils.isEmpty(personDOList)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<String> accounts = personDOList.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        //在职员工
        List<BaseInfoResponse> personByAccountNew = innerUserPersonClient.getPersonByAccountNew(accounts);
        List<TeamMemberVO> list = personByAccountNew.stream().map(a -> {
            TeamMemberVO teamMemberVO = new TeamMemberVO();
            teamMemberVO.setUserId(a.getAccount());
            teamMemberVO.setUserName(a.getAlias() + CommonConstant.JOIN_LINE + a.getName());
            teamMemberVO.setQuited(Integer.valueOf(1).equals(a.getStatus()));
            return teamMemberVO;
        }).collect(Collectors.toList());
        return BaseResult.success(list);
    }

    @Override
    public BaseResult<List<PersonVO>> getLastCopior() {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<ProductDemandDO> productDemandDOList = productDemandMapper.getByOwnerId(userInfo.getId());
        if (CollectionUtils.isEmpty(productDemandDOList)) {
            return BaseResult.success(Lists.emptyList());
        }

        List<Long> pids = productDemandDOList.stream().map(ProductDemandDO::getId).collect(Collectors.toList());
        List<PersonDO> personDOList = personMapper.get(pids, PersonTypeEnum.PRODUCT_DEMAND_CC.getCode());
        if (CollectionUtils.isEmpty(personDOList)) {
            return BaseResult.success(Lists.emptyList());
        }
        List<String> accounts = personDOList.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        List<String> onJobAccounts = innerUserPersonClient.batchGetStaffs(accounts, false);
        personDOList.sort(Comparator.comparing(PersonDO::getCreateDate).reversed());
        List<PersonDO> resultList = new ArrayList<>();
        List<String> distinct = new ArrayList<>();
        personDOList.forEach(a -> {
            //返回前10个在职的抄送人
            if (resultList.size() < 10 && !distinct.contains(a.getUserId()) && onJobAccounts.contains(a.getUserId())) {
                resultList.add(a);
                distinct.add(a.getUserId());
            }
        });
        List<PersonVO> personVOList = PersonCopier.INSTANCE.transform(resultList);
        return BaseResult.success(personVOList);
    }

    @Override
    public BaseResult<Void> resignNotice(String account) {
        return null;
    }

}
