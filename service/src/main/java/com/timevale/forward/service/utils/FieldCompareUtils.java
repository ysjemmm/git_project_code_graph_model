package com.timevale.forward.service.utils;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author xingyun
 * @date 2021-12-13 13:58
 * 用于比较两个对象的字段值
 **/
@Slf4j
public class FieldCompareUtils {
    public static List<String> compare(Object newObj, Object oldObj) {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();
        List<String>result=new ArrayList<>();
        String alias = userInfo.getName() + CommonConstant.JOIN_LINE + userInfo.getAlias();
        Field[] newFields = newObj.getClass().getDeclaredFields();
        Field[] oldFields = oldObj.getClass().getDeclaredFields();
        try {
            for (Field newField : newFields) {
                newField.setAccessible(true);
                FieldCompare newAnno = newField.getAnnotation(FieldCompare.class);
                for (Field oldField : oldFields) {
                    oldField.setAccessible(true);
                    FieldCompare oldAnno = oldField.getAnnotation(FieldCompare.class);
                    if (newAnno != null && oldAnno != null &&
                            Objects.equals(newAnno.fieldName(), oldAnno.fieldName()) && !Objects.equals(newField.get(newObj), oldField.get(oldObj))) {
                        if (StringUtils.isNotEmpty(newAnno.enumMapping())) {
                            Map<Integer, String> map = JSONObject.parseObject(newAnno.enumMapping(), new TypeReference<Map<Integer, String>>() {});
                            String newValue = map.get(newField.get(newObj));
                            String oldValue = map.get(oldField.get(oldObj));
                            result.add(alias + "把" + newAnno.fieldName() + "从" + oldValue + "改为" + newValue);
                        } else {
                            result.add(alias + "把" + newAnno.fieldName() + "从" + oldField.get(oldObj) + "改为" + newField.get(newObj));
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("字段对比完成后,返回结果:{}",result);
        return result;
    }
}
