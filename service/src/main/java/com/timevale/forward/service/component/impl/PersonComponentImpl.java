package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.model.enums.PersonTypeEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.copy.PersonCopier;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class PersonComponentImpl implements PersonComponent {

    @Resource
    private PersonMapper personMapper;

    @Override
    public void add(List<PersonAddReq> list, Long mainId, Byte type) {
        log.info("人员新增接收参数:list={},mainId={},type={}", list, mainId, type);
        List<PersonDO> existPersons = select(mainId, type);
        log.info("已存在人员:existPersons={}", existPersons);
        if (CollectionUtils.isEmpty(existPersons)) {
            List<PersonDO> personDO = PersonCopier.INSTANCE.convert(list);
            fillValue(mainId, type, personDO);
            personMapper.inserts(personDO);
        }
    }


    @Override
    public void update(List<PersonAddReq> list, Long mainId, Byte type) {
        log.info("人员编辑接收参数:list={},mainId={},type={}", list, mainId, type);
        if (CollectionUtils.isEmpty(list)) {
            delete(list, mainId, type);
            return;
        }
        List<PersonDO> personDO = PersonCopier.INSTANCE.convert(list);
        fillValue(mainId, type, personDO);
        List<PersonDO> existPersons = select(mainId, type);
        log.info("已存在人员:existPersons={}", existPersons);
        List<String> existUserIds = existPersons.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        List<PersonDO> needAddPersons = new ArrayList<>();
        personDO.forEach((p) -> {
            if (!existUserIds.contains(p.getUserId())) {
                needAddPersons.add(p);
            }
        });
        personMapper.inserts(needAddPersons);
        log.info("编辑时,新增人员:needAddPersons={},type={}", needAddPersons, type);

        List<String> reqPersonIds = personDO.stream().map(PersonDO::getUserId).collect(Collectors.toList());
        existPersons.forEach((p) -> {
            if (!reqPersonIds.contains(p.getUserId())) {
                p.setIsDeleted(true);
                //删除
                personMapper.update(p);
            }
        });
    }

    @Override
    public List<PersonDO> select(Long mainId, Byte type) {
        PersonListCondition condition = PersonListCondition.builder().build();
        condition.setType(type);
        if (PersonTypeEnum.PROJECT_PD.getCode().equals(type)
                || PersonTypeEnum.PROJECT_MEMBER.getCode().equals(type)) {
            condition.setProjectId(mainId);
        } else if (PersonTypeEnum.PRODUCT_DEMAND_CC.getCode().equals(type)) {
            condition.setProductDemandId(mainId);
        } else if (PersonTypeEnum.BIZ_DEMAND_CC.getCode().equals(type)) {
            condition.setBizDemandId(mainId);
        }
        return personMapper.select(condition);
    }


    private void fillValue(Long mainId, Byte type, List<PersonDO> personDO) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        personDO.forEach(t -> {
            t.setProjectId(0L);
            t.setProductDemandId(0L);
            t.setBizDemandId(0L);
            t.setType(type);
            t.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            t.setCreateManId(userInfo.getId());
            if (PersonTypeEnum.PROJECT_PD.getCode().equals(type)
                    || PersonTypeEnum.PROJECT_MEMBER.getCode().equals(type)) {
                t.setProjectId(mainId);
            } else if (PersonTypeEnum.PRODUCT_DEMAND_CC.getCode().equals(type)) {
                t.setProductDemandId(mainId);
            } else if (PersonTypeEnum.BIZ_DEMAND_CC.getCode().equals(type)) {
                t.setBizDemandId(mainId);
            }
        });
    }

    private void delete(List<PersonAddReq> list, Long mainId, Byte type) {
        if (CollectionUtils.isEmpty(list)) {
            PersonDO personDO = new PersonDO();
            if (PersonTypeEnum.PROJECT_PD.getCode().equals(type)
                    || PersonTypeEnum.PROJECT_MEMBER.getCode().equals(type)) {
                personDO.setProjectId(mainId);
            } else if (PersonTypeEnum.PRODUCT_DEMAND_CC.getCode().equals(type)) {
                personDO.setProductDemandId(mainId);
            } else if (PersonTypeEnum.BIZ_DEMAND_CC.getCode().equals(type)) {
                personDO.setBizDemandId(mainId);
            }
            personDO.setType(type);
            personDO.setIsDeleted(true);
            personMapper.update(personDO);
        }
    }
}
