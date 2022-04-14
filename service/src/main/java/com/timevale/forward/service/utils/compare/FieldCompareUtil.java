package com.timevale.forward.service.utils.compare;

import com.google.common.collect.Maps;
import com.timevale.forward.dal.annotation.FieldCompare;
import com.timevale.forward.model.enums.BizChangeLogTypeEnum;
import com.timevale.forward.model.enums.BugLogTypeEnum;
import com.timevale.forward.model.middle.*;
import com.timevale.forward.service.utils.date.DateFormatConst;
import com.timevale.forward.service.utils.date.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Lists;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 望轩
 * @date 2022/3/22 15:42
 */
@Slf4j
public class FieldCompareUtil {

    private static final String MAIN_ID = "mainId";
    private static final String TYPE = "type";
    private static final String OLD_VALUE = "oldValue";
    private static final String NEW_VALUE = "newValue";
    private static final String FIELD = "field";
    private static final String METHOD = "getTextByCode";

    private static final Map<Class<?>, Integer> CLAZZ_MAP = Maps.newHashMap();

    static {
        CLAZZ_MAP.put(BugOfflineMD.class, BugLogTypeEnum.OFFLINE.getCode());
        CLAZZ_MAP.put(BugOnlineMD.class, BugLogTypeEnum.ONLINE.getCode());
        CLAZZ_MAP.put(BusinessMD.class, BugLogTypeEnum.ONLINE.getCode());
        CLAZZ_MAP.put(BizDemandMD.class, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
        CLAZZ_MAP.put(ProjectMD.class, BizChangeLogTypeEnum.PROJECT.getCode());
        CLAZZ_MAP.put(ProductDemandMD.class, BizChangeLogTypeEnum.PRODUCT_DEMAND.getCode());
    }

    /**
     * 通用字段比较
     *
     * @param oldObj oldObj
     * @param newObj newObj
     * @return 结果
     */
    public static <T extends BaseMD, E> List<E> commonCompare(T oldObj, T newObj, Class<E> clazz) {
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
                    } else if (fieldType == Date.class) {
                        oldString = DateUtil.parseToString((Date) oldField.get(oldObj), DateFormatConst.DATE_FORMAT);
                        newString = DateUtil.parseToString((Date) newField.get(newObj), DateFormatConst.DATE_FORMAT);
                    } else if (fieldType == Integer.class) {
                        Method method = annotation.enumClass().getMethod(METHOD, Integer.class);
                        if (oldValue != null) {
                            oldString = (String) method.invoke(null, oldValue);
                        }
                        if (newValue != null) {
                            newString = (String) method.invoke(null, newValue);
                        }
                    } else if(fieldType == Boolean.class){
                        Method method = annotation.enumClass().getMethod(METHOD, Boolean.class);
                        if (oldValue != null) {
                            oldString = (String) method.invoke(null, oldValue);
                        }
                        if (newValue != null) {
                            newString = (String) method.invoke(null, newValue);
                        }
                    }
                    Constructor<E> constructor = clazz.getConstructor();
                    E e = constructor.newInstance();
                    for (Field field : clazz.getDeclaredFields()) {
                        field.setAccessible(true);
                        if (MAIN_ID.equals(field.getName())) {
                            field.set(e, oldObj.getId() == null ? newObj.getId() : oldObj.getId());
                        } else if (TYPE.equals(field.getName())) {
                            field.set(e, CLAZZ_MAP.get(oldObj.getClass()));
                        } else if (FIELD.equals(field.getName())) {
                            field.set(e, fieldName);
                        } else if (OLD_VALUE.equals(field.getName())) {
                            field.set(e, oldString);
                        } else if (NEW_VALUE.equals(field.getName())) {
                            field.set(e, newString);
                        }
                    }
                    result.add(e);
                }
            }
        } catch (Exception e) {
            log.error("字段对比异常e:{},msg:{}", e, e.getMessage());
            return result;
        }
        log.info("字段对比完成后,返回结果:{}", result);
        return result;
    }

}