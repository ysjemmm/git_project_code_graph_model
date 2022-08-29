package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
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
import java.util.Set;
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
    public void add(List<PersonAddReq> list, Long mainId, Integer type) {
        log.info("人员新增接收参数:list={},mainId={},type={}", list, mainId, type);
        if (CollectionUtils.isEmpty(list)) {
            return;
        }
        List<PersonDO> existPersons = select(mainId, type);
        log.info("已存在人员:existPersons={}", existPersons);
        if (CollectionUtils.isEmpty(existPersons)) {
            List<PersonDO> personDO = PersonCopier.INSTANCE.convert(list);
            fillInfo(mainId, type, personDO);
            personMapper.inserts(personDO);
        }
    }


    @Override
    public void update(List<PersonAddReq> list, Long mainId, Integer type) {
        log.info("人员编辑接收参数:list={},mainId={},type={}", list, mainId, type);
        if (CollectionUtils.isEmpty(list)) {
            delete(list, mainId, type);
            return;
        }
        List<PersonDO> personDOList = PersonCopier.INSTANCE.convert(list);
        List<PersonDO> existPersons = addIfNotExisted(list, mainId, type);
        Set<String> reqPersonIds = personDOList.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        existPersons.forEach((p) -> {
            if (!reqPersonIds.contains(p.getUserId())) {
                UserInfo userInfo = LocalSessionUtils.getUserInfo();
                p.setIsDeleted(true);
                p.setModifyMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
                p.setModifyManId(userInfo.getId());
                //删除
                personMapper.update(p);
            }
        });
    }

    @Override
    public List<PersonDO> select(Long mainId, Integer type) {
        PersonListCondition condition = PersonListCondition.builder().build();
        condition.setType(type);
        condition.setMainId(mainId);
        return personMapper.select(condition);
    }

    @Override
    public List<PersonDO> addIfNotExisted(List<PersonAddReq> list, Long mainId, Integer type) {
        List<PersonDO> personDOList = PersonCopier.INSTANCE.convert(list);
        fillInfo(mainId, type, personDOList);
        List<PersonDO> existPersons = select(mainId, type);
        log.info("已存在人员:existPersons={}", existPersons);
        Set<String> existUserIds = existPersons.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        List<PersonDO> needAddPersons = new ArrayList<>();
        List<String> tmpList = new ArrayList<>();
        personDOList.forEach((p) -> {
            // personDOList去重
            if (!existUserIds.contains(p.getUserId()) && !tmpList.contains(p.getUserId())) {
                needAddPersons.add(p);
                tmpList.add(p.getUserId());
            }
        });
        if (CollectionUtils.isNotEmpty(needAddPersons)) {
            personMapper.inserts(needAddPersons);
            log.info("新增人员:needAddPersons={},type={}", needAddPersons, type);
        }
        return existPersons;
    }


    private void fillInfo(Long mainId, Integer type, List<PersonDO> personDOList) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        personDOList.forEach(t -> {
            t.setType(type);
            t.setMainId(mainId);
            t.setCreateMan(userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName());
            t.setCreateManId(userInfo.getId());
        });
    }

    private void delete(List<PersonAddReq> list, Long mainId, Integer type) {
        if (CollectionUtils.isEmpty(list)) {
            PersonDO personDO = new PersonDO();
            personDO.setMainId(mainId);
            personDO.setType(type);
            personDO.setIsDeleted(true);
            personMapper.update(personDO);
        }
    }
}
