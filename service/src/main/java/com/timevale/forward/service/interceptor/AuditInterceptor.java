package com.timevale.forward.service.interceptor;

import com.timevale.forward.service.constant.CommonConstant;
import com.timevale.forward.service.utils.envoy.LocalSessionUtils;
import com.timevale.forward.service.utils.envoy.UserInfo;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * @author by YangXu
 * @date 2022/01/12 10:49
 */
@Intercepts({
        @Signature(
                type = Executor.class,
                method = "update",
                args = {MappedStatement.class, Object.class}
        )
})
@Slf4j
public class AuditInterceptor implements Interceptor {

    private static final Set<String> FILTER_METHOD = new HashSet<>();

    static {
        FILTER_METHOD.add("com.timevale.forward.dal.dao.BizChangeLogMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.BizChangeLogMapper.batchInsert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProjectNodeFlowMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProjectNodeFlowMapper.update");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProjectNodeRecordMapper.batchInsert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProductDemandDescFlowMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProductDemandDescRecordMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.ProductDemandDescFlowMapper.update");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.TaskMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.TrackImportLogMapper.insert");
        FILTER_METHOD.add("com.timevale.forward.dal.dao.TrackEventMapper.batchInsert");
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        UserInfo userInfo = LocalSessionUtils.getUserInfo();

        if (invocation.getArgs().length == 1) {
            return invocation.proceed();
        }
        Object parameter = invocation.getArgs()[1];

        // 获取对应sql属性
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        String mappedStatementId = mappedStatement.getId();

        // 填充字段,日志操作人单独赋值为SYSTEM-SYSTEM
        if(FILTER_METHOD.contains(mappedStatementId)){
            return invocation.proceed();
        }

        // 根据sql类型进行审计填充
        String id = userInfo.getId();
        String name = userInfo.getAlias() + CommonConstant.JOIN_LINE + userInfo.getName();

        if (sqlCommandType == SqlCommandType.INSERT) {
            setProperty(parameter, AuditEnum.CREATE_MAN.getText(), name);
            setProperty(parameter, AuditEnum.CREATE_MAN_ID.getText(), id);
        } else {
            setProperty(parameter, AuditEnum.MODIFY_MAN.getText(), name);
            setProperty(parameter, AuditEnum.MODIFY_MAN_ID.getText(), id);
        }

        return invocation.proceed();
    }

    /**
     * 属性赋值
     *
     * @param bean  属性
     * @param name  名字
     * @param value 值
     */
    private void setProperty(Object bean, String name, Object value) {
        try {
            BeanUtils.setProperty(bean, name, value);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object plugin(Object target) {
        return Plugin.wrap(target, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    @Getter
    private enum AuditEnum {
        // 创建人
        CREATE_MAN("createMan"),
        // 创建人id
        CREATE_MAN_ID("createManId"),
        // 修改人
        MODIFY_MAN("modifyMan"),
        // 修改人Id
        MODIFY_MAN_ID("modifyManId");

        private final String text;

        AuditEnum(String text) {
            this.text = text;
        }
    }
}
