package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineCustomMapper;
import com.timevale.forward.dal.entity.BugOnlineCustomDO;
import com.timevale.forward.facade.api.request.BugOnlineCustomAddReq;
import com.timevale.forward.service.component.BugOnlineCustomComponent;
import com.timevale.forward.service.copy.BugOnlineCustomCopier;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import javax.annotation.Resource;

import lombok.extern.slf4j.Slf4j;

/**
 * @author yexuan
 * @date 2022-09-26 11:01 yexuan
 */
@Component
@Slf4j
public class BugOnlineCustomComponentImpl implements BugOnlineCustomComponent {

    @Resource
    private BugOnlineCustomMapper bugOnlineCustomMapper;

    @Override
    public void add(List<BugOnlineCustomAddReq> addReqList, Long bugOnlineId) {
        log.info("新增时,线上bug和客户关联接收参数:list={},bugOnlineId={}", addReqList,bugOnlineId);
        if(CollectionUtils.isEmpty(addReqList)){
            return;
        }
        List<BugOnlineCustomDO> bugOnlineCustomDOList = BugOnlineCustomCopier.INSTANCE.convertList(addReqList);

        bugOnlineCustomDOList.forEach(b->{
            b.setBugOnlineId(bugOnlineId);
        });
        bugOnlineCustomMapper.batchInsert(bugOnlineCustomDOList);
    }

    @Override
    public void update(List<BugOnlineCustomAddReq> addReqList, Long bugOnlineId) {
        log.info("修改时,线上bug和客户关联接收参数:list={},bugOnlineId={}", addReqList,bugOnlineId);
        BugOnlineCustomDO bugOnlineCustomDO = new BugOnlineCustomDO();
        bugOnlineCustomDO.setBugOnlineId(bugOnlineId);
        bugOnlineCustomMapper.delete(bugOnlineCustomDO);
        add(addReqList,bugOnlineId);
    }

    @Override
    public List<BugOnlineCustomDO> selectByBugOnlineId(Long bugOnlineId) {
        return bugOnlineCustomMapper.selectByBugOnlineId(bugOnlineId);
    }

    @Override
    public void delete(Long bugOnlineId) {
        log.info("删除时,线上bug和客户关联接收参数:bugOnlineId={}", bugOnlineId);
        BugOnlineCustomDO bugOnlineCustomDO = new BugOnlineCustomDO();
        bugOnlineCustomDO.setBugOnlineId(bugOnlineId);
        bugOnlineCustomMapper.delete(bugOnlineCustomDO);
    }
}
