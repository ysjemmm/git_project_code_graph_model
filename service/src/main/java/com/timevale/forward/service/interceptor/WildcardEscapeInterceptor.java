package com.timevale.forward.service.interceptor;

import com.timevale.forward.dal.annotation.WildcardEscape;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.assertj.core.util.Strings;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Properties;

/**
 * @author by YangXu
 * @date 2022/01/12 18:29
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "query",
                args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
        )
})
public class WildcardEscapeInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        if(invocation.getArgs().length == 1){
            return invocation.proceed();
        }
        Object parameter = invocation.getArgs()[1];
        Field[] fields = parameter.getClass().getDeclaredFields();
        for (Field field : fields) {
            if(field.isAnnotationPresent(WildcardEscape.class)){
                field.setAccessible(true);
                String fieldName = field.getName();
                String fieldValue = wildcardEscape(BeanUtils.getProperty(parameter, fieldName));
                setProperty(parameter, fieldName, fieldValue);
            }
        }
        return invocation.proceed();
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    public static String wildcardEscape(String str){
        if(Strings.isNullOrEmpty(str)){
            return str;
        }
        str = str.replaceAll("\\\\","\\\\\\\\");
        str = str.replaceAll("/", "\\\\/");
        str = str.replaceAll("%", "\\\\%");
        str = str.replaceAll("_", "\\\\_");
        return str;
    }

    private void setProperty(Object bean, String name, Object value) {
        try {
            BeanUtils.setProperty(bean, name, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
