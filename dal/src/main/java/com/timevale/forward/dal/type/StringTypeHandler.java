package com.timevale.forward.dal.type;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author by YangXu
 * @date 2022/01/05 14:56
 */
@MappedTypes(String.class)
public class StringTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement preparedStatement, int i, String s, JdbcType jdbcType) throws SQLException {
        s = toLikeStr(s);
        preparedStatement.setString(i, s);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, String s) throws SQLException {
        return resultSet.getString(s);
    }

    @Override
    public String getNullableResult(ResultSet resultSet, int i) throws SQLException {
        return resultSet.getString(i);
    }

    @Override
    public String getNullableResult(CallableStatement callableStatement, int i) throws SQLException {
        return callableStatement.getString(i);
    }

    private static String toLikeStr(String str){
        if(str == null || "".equals(str)){
            return str;
        }
        str = str.replaceAll("\\\\","\\\\\\\\");
        str = str.replaceAll("/", "\\\\/");
        str = str.replaceAll("%", "\\\\%");
        str = str.replaceAll("_", "\\\\_");
        return str;
    }
}
