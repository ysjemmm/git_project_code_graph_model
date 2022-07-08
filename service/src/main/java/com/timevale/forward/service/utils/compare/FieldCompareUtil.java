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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author 望轩
 * @date 2022/3/22 15:42
 */
@Slf4j
public class FieldCompareUtil {

    private static final String TYPE = "type";
    private static final String FIELD = "field";
    private static final String MAIN_ID = "mainId";
    private static final String OLD_VALUE = "oldValue";
    private static final String NEW_VALUE = "newValue";
    private static final String METHOD = "getTextByCode";

    private static final Map<Class<?>, Integer> CLAZZ_MAP = Maps.newHashMap();
    static {
        CLAZZ_MAP.put(BusinessMD.class, BugLogTypeEnum.ONLINE.getCode());
        CLAZZ_MAP.put(BugOnlineMD.class, BugLogTypeEnum.ONLINE.getCode());
        CLAZZ_MAP.put(BugOfflineMD.class, BugLogTypeEnum.OFFLINE.getCode());
        CLAZZ_MAP.put(ProjectMD.class, BizChangeLogTypeEnum.PROJECT.getCode());
        CLAZZ_MAP.put(ProjectGoalMD.class, BizChangeLogTypeEnum.PROJECT.getCode());
        CLAZZ_MAP.put(BizDemandMD.class, BizChangeLogTypeEnum.BIZ_DEMAND.getCode());
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

                String oldString = convert(oldObj, oldField);
                String newString = convert(newObj, newField);

                // 判断结果是否相同
                if(Objects.equals(oldString, newString)){
                    continue;
                }

                // 字段名
                String fieldName = oldField.getAnnotation(FieldCompare.class).fieldName();

                // 构造
                Constructor<E> constructor = clazz.getConstructor();
                E e = constructor.newInstance();
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String name = field.getName();
                    if (MAIN_ID.equals(name)) {
                        field.set(e, oldObj.getId() == null ? newObj.getId() : oldObj.getId());
                    } else if (TYPE.equals(name)) {
                        field.set(e, CLAZZ_MAP.get(oldObj.getClass()));
                    } else if (FIELD.equals(name)) {
                        field.set(e, fieldName);
                    } else if (OLD_VALUE.equals(name)) {
                        field.set(e, oldString);
                    } else if (NEW_VALUE.equals(name)) {
                        field.set(e, newString);
                    }
                }
                result.add(e);
            }
        } catch (Exception e) {
            log.error("字段对比异常e:{},msg:{}", e, e.getMessage());
            return result;
        }
        log.info("字段对比完成后,返回结果:{}", result);
        return result;
    }

    private static String convert(Object object, Field field) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        field.setAccessible(true);
        Object value = field.get(object);

        // 为空则直接返回
        if(value == null){
            return "";
        }

        FieldCompare annotation = field.getAnnotation(FieldCompare.class);

        Class<?> type = field.getType();
        Class<?> enumClass = annotation.enumClass();

        // 是否为枚举类型
        Method method = null;
        if(enumClass != Enum.class){
            method = enumClass.getMethod(METHOD, type);
        }

        // 根据类型判断处理方法
        Object resultObj = "";
        if (type == String.class) {
            resultObj = value;
        } else if (type == BigDecimal.class) {
            int scale = annotation.scale();
            resultObj = ((BigDecimal) field.get(object)).setScale(scale, RoundingMode.DOWN);
        } else if (type == Date.class) {
            resultObj = DateUtil.parseToString((Date)value, DateFormatConst.DATE_FORMAT);
        } else if (type == Integer.class || type == Boolean.class) {
            resultObj = method == null ? value : method.invoke(null, value);
        }

        return String.valueOf(resultObj);
    }

}