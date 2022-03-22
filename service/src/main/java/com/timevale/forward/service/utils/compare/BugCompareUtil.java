package com.timevale.forward.service.utils.compare;

import cn.hutool.json.JSONUtil;
import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.dal.dao.BugOnlineProductLineMapper;
import com.timevale.forward.dal.dao.ProductLineMapper;
import com.timevale.forward.dal.entity.BugLogDO;
import com.timevale.forward.dal.entity.BugOnlineDO;
import com.timevale.forward.dal.entity.ProductLineDO;
import com.timevale.forward.model.enums.BugFieldEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.middle.BusinessMD;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.assertj.core.util.Lists;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Date 2022/3/22 15:42
 * @Author 望轩
 */
@Slf4j
@Controller
public class BugCompareUtil {
    @Resource
    private BugOnlineProductLineMapper bugOnlineProductLineMapper;

    @Resource
    private ProductLineMapper productLineMapper;

    /**
     * 通用字段比较
     *
     * @param oldObj oldObj
     * @param newObj newObj
     * @return 结果
     */
    public List<BugLogDO> commonCompare(Object oldObj, Object newObj) {
        Field[] oldFields = oldObj.getClass().getDeclaredFields();
        Field[] newFields = newObj.getClass().getDeclaredFields();

        List<BugLogDO> result = Lists.newArrayList();
        Map<String, Field> newFieldMap = Arrays.stream(newFields)
                .filter(e -> {
                    e.setAccessible(true);
                    return e.getAnnotation(FieldCompare.class) != null;
                })
                .collect(Collectors.toMap(Field::getName, Function.identity()));

        try {
            for (Field oldField : oldFields) {
                Field newField = newFieldMap.get(oldField.getName());

                if (newField == null) {
                    continue;
                }

                oldField.setAccessible(true);
                newField.setAccessible(true);

                Object oldValue = oldField.get(oldObj);
                Object newValue = newField.get(newObj);

                if (!Objects.equals(oldValue, newValue)) {
                    FieldCompare annotation = oldField.getAnnotation(FieldCompare.class);

                    String fieldName = annotation.fieldName();
                    Class<?> fieldType = oldField.getType();

                    String oldString = "";
                    String newString = "";

                    if (fieldType == String.class) {
                        oldString = (String) oldField.get(oldObj);
                        newString = (String) newField.get(newObj);
                    } else if (fieldType == Integer.class) {
                        Method method = annotation.enumClass().getMethod("getTextByCode", Integer.class);
                        oldString = (String) method.invoke(null, oldValue);
                        newString = (String) method.invoke(null, newValue);
                    }

                    BugLogDO bugLogDO = new BugLogDO();
                    bugLogDO.setField(fieldName);
                    bugLogDO.setOldValue(oldString);
                    bugLogDO.setNewValue(newString);
                    result.add(bugLogDO);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("字段对比完成后,返回结果:{}", result);
        return result;
    }

    /**
     * 线上bug特殊字段比较
     *
     * @param oldObj 老的线上bug对象
     * @param newObj 新的线上bug对象
     * @return 返回结果集合
     * */
    public List<BugLogDO> compareExtraIfNecessary(BugOnlineDO oldObj, BugOnlineDO newObj) {
        List<BugLogDO> bugLogDOList = new ArrayList<>();

        List<Long> oldProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(oldObj.getId());
        List<Long> newProductLineIdList = bugOnlineProductLineMapper.selectProductLineIds(newObj.getId());
        boolean result = CollectionUtils.isEqualCollection(oldProductLineIdList, newProductLineIdList);
        //如果产品线变了记录一条bug内容变更日志
        if (result == false) {
            StringBuilder oldNames = new StringBuilder();
            StringBuilder newNames = new StringBuilder();
            List<ProductLineDO> oldProductLineDOList = productLineMapper.selectByIds(oldProductLineIdList);
            List<ProductLineDO> newProductLineDOList = productLineMapper.selectByIds(newProductLineIdList);
            if (CollectionUtils.isNotEmpty(oldProductLineDOList)) {
                Integer count = 0;
                for (ProductLineDO productLineDO : oldProductLineDOList) {
                    oldNames.append(productLineDO.getName());
                    count++;
                    if (!count.equals(oldProductLineDOList.size())) {
                        oldNames.append("&");
                    }
                }
            }
            if (CollectionUtils.isNotEmpty(newProductLineIdList)) {
                Integer tally = 0;
                for (ProductLineDO productLine : newProductLineDOList) {
                    newNames.append(productLine.getName());
                    tally++;
                    if (!tally.equals(newProductLineDOList.size())) {
                        newNames.append("&");
                    }
                }
            }

            BugLogDO bugLogDO = new BugLogDO();
            bugLogDO.setField(BugFieldEnum.PRODUCT_LINE.getText());
            bugLogDO.setOldValue(oldNames.toString());
            bugLogDO.setNewValue(newNames.toString());
            bugLogDO.setMainId(oldObj.getId());
            bugLogDO.setType(BugLogTypeEnum.ONLINE.getCode());
            bugLogDOList.add(bugLogDO);
        }

        String oldBusiness = oldObj.getBusiness();
        String newBusiness = newObj.getBusiness();
        //如果产品线业务这个json字符串变了，要记录一条或多条内容变更日志
        if (!oldBusiness.equals(newBusiness)) {
            BusinessMD oldBusinessMD = new BusinessMD();
            BusinessMD newBusinessMD = new BusinessMD();
            if(!oldBusiness.equals("''")){
                oldBusinessMD = JSONUtil.toBean(oldBusiness, BusinessMD.class);
            }
            if(!newBusiness.equals("''")){
                newBusinessMD = JSONUtil.toBean(newBusiness, BusinessMD.class);
            }
            List<BugLogDO> bugLogList = commonCompare(oldBusinessMD, newBusinessMD);
            bugLogDOList.addAll(bugLogList);
        }

        return bugLogDOList;
    }


}