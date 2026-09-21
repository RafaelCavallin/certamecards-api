package br.com.certamecards.common.persistence;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcType;
import org.postgresql.util.PGobject;

public class InetJdbcType implements JdbcType {

    private static final String PG_TYPE_NAME = "inet";

    public static final InetJdbcType INSTANCE = new InetJdbcType();

    @Override
    public int getJdbcTypeCode() {
        return SqlTypes.OTHER;
    }

    @Override
    public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
        return new BasicBinder<>(javaType, this) {
            @Override
            protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options)
                    throws SQLException {
                st.setObject(index, toPgObject(javaType.unwrap(value, String.class, options)));
            }

            @Override
            protected void doBind(CallableStatement st, X value, String name, WrapperOptions options)
                    throws SQLException {
                st.setObject(name, toPgObject(javaType.unwrap(value, String.class, options)));
            }
        };
    }

    @Override
    public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
        return VarcharJdbcType.INSTANCE.getExtractor(javaType);
    }

    private PGobject toPgObject(String value) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType(PG_TYPE_NAME);
        pgObject.setValue(value);
        return pgObject;
    }
}
