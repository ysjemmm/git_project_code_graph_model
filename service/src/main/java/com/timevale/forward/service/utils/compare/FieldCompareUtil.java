package com.timevale.forward.service.utils.compare;

import com.google.common.collect.Maps;
import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.dal.entity.LogDO;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.middle.BaseMD;
import com.timevale.forward.model.middle.BugOfflineMD;
import com.timevale.forward.model.middle.BugOnlineMD;
import com.timevale.forward.model.middle.BusinessMD;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 望轩
 * @date 2022/3/22 15:42
 */
@Slf4j
public class FieldCompareUtil {
    private static Map<Class<?>, Integer> clazzMap = Maps.newHashMap();

    static {
        clazzMap.put(BugOfflineMD.class, BugLogTypeEnum.OFFLINE.getCode());
        clazzMap.put(BugOnlineMD.class, BugLogTypeEnum.ONLINE.getCode());
        clazzMap.put(BusinessMD.class, BugLogTypeEnum.ONLINE.getCode());
    }

    /**
     * 通用字段比较
     *
     * @param oldObj oldObj
     * @param newObj newObj
     * @return 结果
     */
    public static <T extends BaseMD, E extends LogDO> List<E> commonCompare(T oldObj, T newObj) {
        Field[] oldFields = oldObj.getClass().getDeclaredFields();
        Field[] newFields = newObj.getClass().getDeclaredFields();

        List<E> result = Lists.newArrayList();
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
                    E logDO = (E) new LogDO();
                    logDO.setMainId(oldObj.getId());
                    logDO.setType(clazzMap.get(oldObj.getClass()));
                    logDO.setField(fieldName);
                    logDO.setOldValue(oldString);
                    logDO.setNewValue(newString);
                    result.add(logDO);
                }
            }
        } catch (Exception e) {
            log.error("字段对比异常e:{},msg:{}", e,e.getMessage());
            return result;
        }
        log.info("字段对比完成后,返回结果:{}", result);
        return result;
    }

}