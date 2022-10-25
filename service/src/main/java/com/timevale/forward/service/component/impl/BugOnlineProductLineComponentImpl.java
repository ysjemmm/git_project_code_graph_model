package com.timevale.forward.service.component.impl;

import com.timevale.forward.dal.dao.BugOnlineProductLineMapper;
import com.timevale.forward.dal.entity.BugOnlineProductLineDO;
import com.timevale.forward.service.component.BugOnlineProductLineComponent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @Date 2022/3/22 10:32
 * @Author 望轩
 */
@Component
@Slf4j
public class BugOnlineProductLineComponentImpl implements BugOnlineProductLineComponent {
    @Resource
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;

    @Override
    public void update(List<Long> productLineIdList, Long id,Integer type) {
        if (CollectionUtils.isNotEmpty(productLineIdList)) {
            BugOnlineProductLineDO bugOnlineProductLineDO = new BugOnlineProductLineDO();
            bugOnlineProductLineDO.setBugOnlineId(id);
            bugOnlineProductLineDO.setType(type);
            bugOnlineProductLineDO.setIsDeleted(true);
            //删除之前的数据
            bugOnlineProductLineMapper.update(bugOnlineProductLineDO);

            List<BugOnlineProductLineDO> bugOnlineProductLineDOList = new ArrayList<>();
            productLineIdList.forEach(productLineId -> {
                BugOnlineProductLineDO bugOnlineProductLine = new BugOnlineProductLineDO();
                bugOnlineProductLine.setBugOnlineId(id);
                bugOnlineProductLine.setProductLineId(productLineId);
                bugOnlineProductLineDO.setType(type);
                bugOnlineProductLineDOList.add(bugOnlineProductLine);
            });
            //插入新的数据
            bugOnlineProductLineMapper.batchInsert(bugOnlineProductLineDOList);
        }
    }

    @Override
    public void add(List<Long> productLineIdList, Long id,Integer type) {
        List<BugOnlineProductLineDO> bugOnlineProductLineDOList = new ArrayList<>();
        //如果产品线id不为空往线上bug和产品线的映射表中插入信息
        if (CollectionUtils.isNotEmpty(productLineIdList)) {
            productLineIdList.forEach(productLineId -> {
                BugOnlineProductLineDO bugOnlineProductLineDO = new BugOnlineProductLineDO();
                bugOnlineProductLineDO.setBugOnlineId(id);
                bugOnlineProductLineDO.setProductLineId(productLineId);
                bugOnlineProductLineDO.setType(type);
                bugOnlineProductLineDOList.add(bugOnlineProductLineDO);
            });
            bugOnlineProductLineMapper.batchInsert(bugOnlineProductLineDOList);
        }
    }
}