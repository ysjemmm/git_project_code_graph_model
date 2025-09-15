package com.timevale.forward.service.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.timevale.mandarin.base.exception.BaseBizRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

/**
 * @auther: yuhua
 * @date: 2025/9/12 11:03
 * @description:
 */
public class JsonUtils {

    private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

    private static final String TIME_ZONE = "GMT+8";

    public static final ObjectMapper commonMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .setTimeZone(TimeZone.getTimeZone(TIME_ZONE))
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * object转Json, 底层采用默认mapper
     */
    public static String toJson(Object object) {
        try {
            return commonMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            LOG.error("parse object to json string error!", e);
            throw new BaseBizRuntimeException("parse object to json string error! ");
        }
    }

    /**
     * json转class, 底层采用默认mapper
     */
    public static <T> T fromJson(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }
        try {
            return commonMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            String clzName = clazz.getName();
            LOG.error("parse string to class error! class is:{}", clzName, e);
            throw new BaseBizRuntimeException("parse string to class error! class is:" + clzName);
        }
    }

    public static <T> T fromJsonWithoutError(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return null;
        }
        try {
            return commonMapper.readValue(jsonString, clazz);
        } catch (IOException e) {
            String clzName = clazz.getName();
            LOG.error("parse string to class error! class is:{}", clzName, e);
            return null;
        }
    }

    public static <T> List<T> fromJsonList(String jsonString, Class<T> clazz) {
        if (StringUtils.isEmpty(jsonString)) {
            return new ArrayList<>();
        }
        List<T> list;
        try {
            JavaType javaType = commonMapper.getTypeFactory().constructParametricType(
                    List.class, clazz);
            list = commonMapper.readValue(jsonString, javaType);
            return list;
        } catch (IOException e) {
            String clzName = clazz.getName();
            LOG.error("Parse string to List error! class is:{}", clzName, e);
            throw new BaseBizRuntimeException("Parse string to List error! class is:" + clzName);
        }
    }

    public static <K, V> Map<K, V> fromJsonMap(String jsonString, Class<K> key, Class<V> value) {
        if (StringUtils.isEmpty(jsonString)) {
            return new HashMap<>();
        }
        Map<K, V> map;
        try {
            JavaType javaType = commonMapper.getTypeFactory().constructParametricType(Map.class, key, value);
            map = commonMapper.readValue(jsonString, javaType);
            return map;
        } catch (IOException e) {
            LOG.error("Parse string to Map error!", e);
            throw new BaseBizRuntimeException("Parse string to Map error!");
        }
    }

    public static <T> T fromJson(Object value, TypeReference<T> typeReference) {
        if (value == null) {
            return null;
        }
        try {
            return commonMapper.convertValue(value, typeReference);
        } catch (Exception e) {
            String typeName = typeReference.getType().getTypeName();
            LOG.error("parse string to TypeReference error! TypeReference is:{}", typeName, e);
            throw new BaseBizRuntimeException("parse string to TypeReference error! TypeReference is" + typeName);
        }
    }

    public static <T> T jsonDeserialize(String content, Class<T> clazzType) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            return commonMapper.readValue(content, clazzType);
        } catch (Exception e) {
            LOG.error("parse string to class error! classType is:{}", clazzType, e);
            throw new BaseBizRuntimeException("parse string to class error! classType is" + clazzType);
        }
    }

    public static <T> T jsonDeserialize(String content, TypeReference<T> typeReference) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        try {
            return commonMapper.readValue(content, typeReference);
        } catch (Exception e) {
            String typeName = typeReference.getType().getTypeName();
            LOG.error("parse string to TypeReference error! TypeReference is:{}", typeName, e);
            throw new BaseBizRuntimeException("parse string to TypeReference error! TypeReference is" + typeName);
        }

    }

    /**
     * 将object转换为对应class类
     *
     * @param object
     * @param classType
     * @param <T>
     * @return
     */
    public static <T> T convertValue(Object object, Class<T> classType) {
        if (object == null) {
            return null;
        }
        try {
            return commonMapper.convertValue(object, classType);
        } catch (Exception e) {
            String typeName = classType.getName();
            LOG.error("convert object to class error! class is:{}", typeName, e);
            throw new BaseBizRuntimeException("convert object to class error! class is" + typeName);
        }

    }

    /**
     * 创建mapper
     */
    public static ObjectMapper createMapper() {
        return new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private JsonUtils() {
    }
}
