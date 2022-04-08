package com.timevale.forward.service.component.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.timevale.forward.dal.dao.BizChangeLogMapper;
import com.timevale.forward.dal.entity.BizChangeLogDO;
import com.timevale.forward.dal.entity.ProductDemandDO;
import com.timevale.forward.model.enums.BizChangeLogFieldEnum;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BizDemandStatusEnum;
import com.timevale.forward.model.enums.ProjectStatusEnum;
import com.timevale.forward.service.component.ProductDemandLogComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author xingyun
 * @date 2021-12-16 11:37
 **/
@Component
@Slf4j
public class ProductDemandLogComponentImpl implements ProductDemandLogComponent {

    @Resource
    private BizChangeLogMapper bizChangeLogMapper;

    /**
     * 编辑时,记录日志
     * @param oldObj oldObj
     * @param newObj newObj
     */
    @Override
    public void addLogWhenModifyData(ProductDemandDO oldObj, ProductDemandDO newObj) {

    }

    /**
     * 项目状态改变时引起的产品需求状态变化
     * @param statusMap statusMap
     * @param newStauts newStauts
     */
    @Override
    public void addLogAsProjectStatusChange(Map<Long, Integer> statusMap, Integer newStauts) {
        //系统 把{xx状态字段名称xx}从{原状态} 改为{新状态}
        statusMap.forEach((id,status) -> {
            if(!Objects.equals(status,newStauts)){
                BizChangeLogDO logDO = new BizChangeLogDO();
                logDO.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
                logDO.setMainId(id);
                logDO.setField(BizChangeLogFieldEnum.PRODUCT_DEMAND_STATUS.getText());
                logDO.setOldValue(BizDemandStatusEnum.getTextByCode(status));
                logDO.setNewValue(BizDemandStatusEnum.getTextByCode(newStauts));
            }
        });
    }

    /**
     * 按钮点击时引起的产品需求状态变化
     * @param oldStatus oldStatus
     * @param newStatus  newStatus
     * @param id  id
     * @param action action
     */
    @Override
    public void addLogWhenStatusChange(Integer oldStatus, Integer newStatus, Long id, String action) {
        //{操作人}点击 {按钮名称} ,状态改为{操作后状态},
        if(!Objects.equals(oldStatus,newStatus)){
            BizChangeLogDO logDO = new BizChangeLogDO();
            logDO.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            logDO.setMainId(id);
            logDO.setField(BizChangeLogFieldEnum.PRODUCT_DEMAND_STATUS.getText());
            logDO.setAction(action);
            logDO.setOldValue(ProjectStatusEnum.getTextByCode(oldStatus));
            logDO.setNewValue(ProjectStatusEnum.getTextByCode(newStatus));
//        bizChangeLogMapper.insert(logDO);
        }
    }

    /**
     * 关联/删除关联/作废产品需求时,双向记录日志
     * @param name name
     * @param id id
     * @param bdNameMap bdNameMap
     * @param linkOrUnlink linkOrUnlink
     */
    @Override
    public void addLogWhenLinkOrUnlink(String name, Long id,Map<Long, String> bdNameMap,String linkOrUnlink) {
        List<BizChangeLogDO> logs = new ArrayList<>();
        bdNameMap.forEach((bId, bName) -> {
            //1.产品需求记录日志:{操作人}添加/删除 {业务需求}:{业务需求A}
            BizChangeLogDO productLog = new BizChangeLogDO();
            productLog.setType(BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
            productLog.setMainId(id);
            productLog.setField(BizChangeLogTypeEnum.BIZ_DEMAND.getText());
            productLog.setAction(linkOrUnlink);
            productLog.setOldValue(bName);
            productLog.setNewValue(bName);
            logs.add(productLog);

            //1.业务需求记录日志:{操作人}添加/删除 {产品需求}:{产品需求A}
            BizChangeLogDO bizLog = new BizChangeLogDO();
            bizLog.setType(BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
            bizLog.setMainId(bId);
            bizLog.setField(BizChangeLogTypeEnum.PRODUCT_DEMAND.getText());
            bizLog.setAction(linkOrUnlink);
            bizLog.setOldValue(name);
            bizLog.setNewValue(name);
            logs.add(bizLog);

        });
        if (CollectionUtil.isNotEmpty(logs)) {
//            bizChangeLogMapper.batchInsert(logs);
        }
    }
}
