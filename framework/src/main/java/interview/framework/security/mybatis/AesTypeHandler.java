package interview.framework.security.mybatis;

import interview.common.util.DataSecurityUtil;
import lombok.AllArgsConstructor;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.springframework.stereotype.Component;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author zhuxi
 * @apiNote mybatis 加密（AES）处理器
 * @since 2026/5/27 14:05
 */

public class AesTypeHandler extends BaseTypeHandler<String> {
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, DataSecurityUtil.INSTANCE.aesEncrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return DataSecurityUtil.INSTANCE.aesDecrypt(rs.getString(columnName));
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return DataSecurityUtil.INSTANCE.aesDecrypt(rs.getString(columnIndex));
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return DataSecurityUtil.INSTANCE.aesDecrypt(cs.getString(columnIndex));
    }
}
