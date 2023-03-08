package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollUtil;
import com.timevale.forward.dal.condition.PersonListCondition;
import com.timevale.forward.dal.dao.PersonMapper;
import com.timevale.forward.dal.entity.PersonDO;
import com.timevale.forward.facade.api.request.PersonAddReq;
import com.timevale.forward.model.enums.PersonLevelEnum;
import com.timevale.forward.service.component.PersonComponent;
import com.timevale.forward.service.copy.PersonCopier;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
        add(list, mainId, type, PersonLevelEnum.CORE.getCode());
    }

    @Override
    public void add(List<PersonAddReq> list, Long mainId, Integer type, Integer personLevel) {
        log.info("[PersonComponentImpl.add]人员新增接收参数:list={},mainId={},type={}, personLevel= {}", list, mainId, type, personLevel);
        if (CollUtil.isEmpty(list)) {
            return;
        }

        List<PersonDO> existPersons = select(mainId, type, personLevel);
        log.info("[PersonComponentImpl.add]已存在人员:existPersons={}", existPersons);

        if (CollUtil.isEmpty(existPersons)) {
            List<PersonDO> personDOList = list.stream()
                    .map(e -> PersonCopier.INSTANCE.req2do(e, mainId, type, personLevel))
                    .collect(Collectors.toList());
            personMapper.inserts(personDOList);
        }
    }

    @Override
    public void update(List<PersonAddReq> list, Long mainId, Integer type) {
        log.info("人员编辑接收参数:list={},mainId={},type={}", list, mainId, type);
        if (CollectionUtils.isEmpty(list)) {
            delete(mainId, type);
            return;
        }

        // 当前人员
        Set<String> updatePersonIdSet = list.stream().map(PersonAddReq::getUserId).collect(Collectors.toSet());
        // 新增人员
        List<PersonDO> existPersons = addIfNotExisted(list, mainId, type);
        // 去除人员
        existPersons.stream()
                .filter(e -> !updatePersonIdSet.contains(e.getUserId()))
                .forEach(e -> {
                    e.setIsDeleted(true);
                    personMapper.update(e);
                });
    }

    @Override
    public void update(List<PersonAddReq> list, Long mainId, Integer type, Integer personLevel) {
        log.info("人员编辑接收参数:list={},mainId={},type={}", list, mainId, type);
        if (CollectionUtils.isEmpty(list)) {
            delete(mainId, type, personLevel);
            return;
        }

        // 当前人员
        Set<String> updatePersonIdSet = list.stream().map(PersonAddReq::getUserId).collect(Collectors.toSet());
        // 新增人员
        List<PersonDO> existPersons = addIfNotExisted(list, mainId, type, personLevel);
        // 去除人员
        existPersons.stream()
                .filter(e -> !updatePersonIdSet.contains(e.getUserId()))
                .forEach(e -> {
                    e.setIsDeleted(true);
                    personMapper.update(e);
                });
    }

    @Override
    public List<PersonDO> select(Long mainId, Integer type) {
        PersonListCondition condition = PersonListCondition.builder()
                .type(type)
                .mainId(mainId)
                .build();
        return personMapper.select(condition);
    }

    @Override
    public List<PersonDO> select(Long mainId, Integer type, Integer personLevel) {
        PersonListCondition condition = PersonListCondition.builder()
                .type(type)
                .mainId(mainId)
                .personLevel(personLevel)
                .build();
        return personMapper.select(condition);
    }

    @Override
    public List<PersonDO> addIfNotExisted(List<PersonAddReq> list, Long mainId, Integer type) {
        List<PersonDO> existPersons = select(mainId, type);
        Set<String> existUserIdSet = existPersons.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        log.info("已存在人员:existUserIdSet={}", existUserIdSet);

        List<PersonDO> addPersonList = new ArrayList<>();

        if (CollUtil.isNotEmpty(list)) {
            // 过滤重复人员
            list.stream()
                    .filter(e -> !existUserIdSet.contains(e.getUserId()))
                    .forEach(e -> {
                        PersonDO personDO = PersonCopier.INSTANCE.req2do(e, mainId, type);
                        addPersonList.add(personDO);
                        existUserIdSet.add(e.getUserId());
                    });
        }

        // 如果人员不为空则新增
        if (CollUtil.isNotEmpty(addPersonList)) {
            personMapper.inserts(addPersonList);
            log.info("新增人员:addPersonList:{}, mainId:{}, type:{}", addPersonList, mainId, type);
        }
        return existPersons;
    }

    @Override
    public List<PersonDO> addIfNotExisted(List<PersonAddReq> list, Long mainId, Integer type, Integer personLevel) {
        // 已存在人员不区分核心扩展
        List<PersonDO> existPersons = select(mainId, type);
        Set<String> existUserIdSet = existPersons.stream().map(PersonDO::getUserId).collect(Collectors.toSet());
        log.info("已存在人员:existUserIdSet={}", existUserIdSet);

        List<PersonDO> addPersonList = new ArrayList<>();

        if (CollUtil.isNotEmpty(list)) {
            // 过滤重复人员
            list.stream()
                    .filter(e -> !existUserIdSet.contains(e.getUserId()))
                    .forEach(e -> {
                        PersonDO personDO = PersonCopier.INSTANCE.req2do(e, mainId, type, personLevel);
                        addPersonList.add(personDO);
                        existUserIdSet.add(e.getUserId());
                    });
        }

        // 如果人员不为空则新增
        if (CollUtil.isNotEmpty(addPersonList)) {
            personMapper.inserts(addPersonList);
            log.info("新增人员:addPersonList:{}, mainId:{}, type:{}, personLevel:{}", addPersonList, mainId, type, personLevel);
        }
        return existPersons.stream().filter(e -> Objects.equals(personLevel, e.getPersonLevel())).collect(Collectors.toList());
    }

    private void delete(Long mainId, Integer type) {
        log.info("[PersonComponentImpl.delete]删除人员, type:{},mainId:{}", type, mainId);

        PersonDO personDO = new PersonDO();
        personDO.setIsDeleted(true);
        personDO.setType(type);
        personDO.setMainId(mainId);
        personMapper.update(personDO);
    }

    private void delete(Long mainId, Integer type, Integer personLevel) {
        log.info("[PersonComponentImpl.delete]删除人员, type:{},mainId:{}", type, mainId);

        PersonDO personDO = new PersonDO();
        personDO.setIsDeleted(true);
        personDO.setType(type);
        personDO.setMainId(mainId);
        personDO.setPersonLevel(personLevel);
        personMapper.update(personDO);
    }

    @Override
    public void duplicateRemove(List<PersonAddReq> list, PersonAddReq duplicate) {
        list.removeIf(e -> Objects.equals(e.getUserId(), duplicate.getUserId()));
        list.add(duplicate);
    }
}
