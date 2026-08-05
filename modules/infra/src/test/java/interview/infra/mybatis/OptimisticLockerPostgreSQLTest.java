package interview.infra.mybatis;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 在真实 PostgreSQL 上实证 MP 3.5.15 OptimisticLockerInnerInterceptor 的语义，
 * 验证本仓库乐观锁改造所依赖的三个事实：
 * <ul>
 *   <li>updateById(entity) 实体携带旧 version 时，自动 SET version=old+1 且 WHERE version=old</li>
 *   <li>update(entity, wrapper) 实体携带旧 version 且 wrapper 不含 version 条件时，同样自动生效</li>
 *   <li>实体携带 version=old+1 且 wrapper 手写 eq(version, old) 时，恒 0 行（当前线上 bug 复现）</li>
 * </ul>
 */
@Testcontainers(disabledWithoutDocker = true)
class OptimisticLockerPostgreSQLTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static SqlSessionFactory factory;

    @BeforeAll
    static void setUp() throws Exception {
        DataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS opt_lock_job");
            statement.execute("""
                    CREATE TABLE opt_lock_job (
                        id BIGINT NOT NULL,
                        enterprise_id BIGINT NOT NULL,
                        title VARCHAR(100),
                        version INT NOT NULL DEFAULT 0,
                        is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
                        PRIMARY KEY (id)
                    )
                    """);
        }

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(StdOutImpl.class);

        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        MybatisSqlSessionFactoryBean bean = new MybatisSqlSessionFactoryBean();
        bean.setDataSource(dataSource);
        bean.setConfiguration(configuration);
        bean.setPlugins(interceptor);
        bean.afterPropertiesSet();
        factory = bean.getObject();
        factory.getConfiguration().addMapper(OptLockJobMapper.class);
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    }

    private void insertRow(long id, long enterpriseId, int version, String title) throws Exception {
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(
                     "INSERT INTO opt_lock_job (id, enterprise_id, title, version, is_deleted) VALUES (?, ?, ?, ?, FALSE)")) {
            ps.setLong(1, id);
            ps.setLong(2, enterpriseId);
            ps.setString(3, title);
            ps.setInt(4, version);
            ps.executeUpdate();
        }
    }

    private Map<String, Object> readRow(long id) throws Exception {
        try (Connection connection = connection();
             PreparedStatement ps = connection.prepareStatement(
                     "SELECT title, version, is_deleted FROM opt_lock_job WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return Map.of("title", rs.getString(1), "version", rs.getInt(2), "isDeleted", rs.getBoolean(3));
            }
        }
    }

    @Test
    void updateById_withOldVersion_bumpsVersionOnce() throws Exception {
        insertRow(1, 100, 0, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setId(1L);
        entity.setEnterpriseId(100L);
        entity.setTitle("viaUpdateById");
        entity.setVersion(0);

        int affected = mapper.updateById(entity);
        assertEquals(1, affected);
        Map<String, Object> row = readRow(1);
        assertEquals("viaUpdateById", row.get("title"));
        assertEquals(1, row.get("version"));
    }

    @Test
    void updateEntityWithWrapper_oldVersion_autoAppendsVersionToWrapper() throws Exception {
        insertRow(2, 100, 0, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setId(2L);
        entity.setTitle("viaWrapper");
        entity.setVersion(0);

        int affected = mapper.update(entity, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 2L)
                .eq(OptLockJob::getEnterpriseId, 100L));
        assertEquals(1, affected);
        Map<String, Object> row = readRow(2);
        assertEquals("viaWrapper", row.get("title"));
        assertEquals(1, row.get("version"));
    }

    @Test
    void updateEntityWithWrapper_staleVersion_rejectsUpdate() throws Exception {
        insertRow(3, 100, 5, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setId(3L);
        entity.setTitle("stale");
        entity.setVersion(4);

        int affected = mapper.update(entity, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 3L)
                .eq(OptLockJob::getEnterpriseId, 100L));
        assertEquals(0, affected);
        assertEquals(5, readRow(3).get("version"));
    }

    @Test
    void updateEntityWithWrapper_entityVersionPlusOne_alwaysZeroRows_reproducesCurrentBug() throws Exception {
        insertRow(4, 100, 7, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setId(4L);
        entity.setTitle("bug");
        entity.setVersion(8);

        int affected = mapper.update(entity, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 4L)
                .eq(OptLockJob::getVersion, 7));
        assertEquals(0, affected);

        OptLockJob fix = new OptLockJob();
        fix.setId(4L);
        fix.setTitle("fixed");
        fix.setVersion(7);
        int fixed = mapper.update(fix, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 4L)
                .eq(OptLockJob::getEnterpriseId, 100L));
        assertEquals(1, fixed);
        Map<String, Object> row = readRow(4);
        assertEquals("fixed", row.get("title"));
        assertEquals(8, row.get("version"));
    }

    @Test
    void softDeleteViaWrapperSet_isDeletedTrue_withEntityVersion_softDeletesAndBumpsVersion() throws Exception {
        insertRow(5, 100, 3, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setId(5L);
        entity.setEnterpriseId(100L);
        entity.setVersion(3);

        int affected = mapper.update(entity, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 5L)
                .eq(OptLockJob::getEnterpriseId, 100L)
                .set(OptLockJob::getIsDeleted, true));
        assertEquals(1, affected);
        Map<String, Object> row = readRow(5);
        assertEquals(Boolean.TRUE, row.get("isDeleted"));
        assertEquals(4, row.get("version"));

        OptLockJob stale = new OptLockJob();
        stale.setId(5L);
        stale.setEnterpriseId(100L);
        stale.setVersion(3);
        assertEquals(0, mapper.update(stale, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 5L)
                .eq(OptLockJob::getEnterpriseId, 100L)
                .set(OptLockJob::getIsDeleted, true)));
    }

    @Test
    void updateEntityOnlyVersion_wrapperCarriesSetAndBusinessConditions_works() throws Exception {
        insertRow(6, 100, 0, "orig");
        OptLockJobMapper mapper = factory.openSession(true).getMapper(OptLockJobMapper.class);

        OptLockJob entity = new OptLockJob();
        entity.setVersion(0);

        int affected = mapper.update(entity, Wrappers.<OptLockJob>lambdaUpdate()
                .eq(OptLockJob::getId, 6L)
                .eq(OptLockJob::getEnterpriseId, 100L)
                .eq(OptLockJob::getTitle, "orig")
                .set(OptLockJob::getTitle, "transitioned")
                .set(OptLockJob::getIsDeleted, false));
        assertEquals(1, affected);
        Map<String, Object> row = readRow(6);
        assertEquals("transitioned", row.get("title"));
        assertEquals(1, row.get("version"));
    }
}