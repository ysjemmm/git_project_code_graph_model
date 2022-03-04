package com.timevale.forward.service.handler;

import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.dal.entity.BaseDO;
import com.timevale.forward.dal.entity.BugLogDO;
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
 * @author xingyun
 * @date 2021-12-13 13:58
 **/
@Slf4j
public abstract class AbstractFieldCompareHandler<T extends BaseDO> {

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
     * 特殊字段比较
     *
     * @param oldObj oldObj
     * @param newObj newObj
     * @return 结果
     */
    protected abstract List<BugLogDO> compareExtraIfNecessary(T oldObj,T newObj);

}
