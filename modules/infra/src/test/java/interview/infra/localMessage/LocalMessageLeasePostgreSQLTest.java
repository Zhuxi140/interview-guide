package interview.infra.localMessage;

import interview.framework.mybatis.JsonbStringTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class LocalMessageLeasePostgreSQLTest {

    @Container
    private static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"));

    private static final String CLAIM_SQL = """
            WITH candidates AS (
                SELECT id
                FROM local_message
                WHERE priority = 'LOW'
                  AND (
                    (status = 'PENDING' AND (next_retry_at IS NULL OR next_retry_at <= ?))
                    OR
                    (status = 'PROCESSING' AND (lease_until IS NULL OR lease_until <= ?))
                  )
                ORDER BY created_at
                LIMIT 1
                FOR UPDATE SKIP LOCKED
            )
            UPDATE local_message AS message
            SET status = 'PROCESSING', lease_owner = ?, lease_until = ?,
                lease_version = COALESCE(message.lease_version, 0) + 1, updated_at = ?
            FROM candidates
            WHERE message.id = candidates.id
            RETURNING message.id, message.lease_version
            """;

    @BeforeEach
    void resetSchema() throws Exception {
        // 每个用例都从同一条未领取消息开始。
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS local_message");
            statement.execute("""
                    CREATE TABLE local_message (
                        id BIGINT PRIMARY KEY,
                        priority VARCHAR(16) NOT NULL,
                        status VARCHAR(16) NOT NULL,
                        payload JSONB,
                        next_retry_at TIMESTAMPTZ,
                        lease_owner VARCHAR(128),
                        lease_until TIMESTAMPTZ,
                        lease_version BIGINT NOT NULL DEFAULT 0,
                        created_at TIMESTAMPTZ NOT NULL,
                        updated_at TIMESTAMPTZ
                    )
                    """);
            statement.execute("""
                    INSERT INTO local_message(id, priority, status, created_at)
                    VALUES (1, 'LOW', 'PENDING', CURRENT_TIMESTAMP)
                    """);
        }
    }

    @Test
    void claim_shouldSkipRowLockedByAnotherInstance() throws Exception {
        // 第一个事务未提交时，第二实例必须跳过同一行而不是重复领取。
        try (Connection first = connection(); Connection second = connection()) {
            first.setAutoCommit(false);
            second.setAutoCommit(false);

            Claim firstClaim = claim(first, "worker-a", OffsetDateTime.now());
            Claim secondClaim = claim(second, "worker-b", OffsetDateTime.now());

            assertNotNull(firstClaim);
            assertNull(secondClaim);
            first.commit();
            second.commit();
        }
    }

    @Test
    void expiredLease_shouldBeReclaimedAndFenceOldWorker() throws Exception {
        // 租约过期后版本递增，旧执行者不能再提交处理结果。
        Claim firstClaim;
        try (Connection first = connection()) {
            firstClaim = claim(first, "worker-a", OffsetDateTime.now());
        }
        try (Connection connection = connection(); Statement statement = connection.createStatement()) {
            statement.execute("UPDATE local_message SET lease_until = CURRENT_TIMESTAMP - INTERVAL '1 minute'");
        }

        Claim secondClaim;
        try (Connection second = connection()) {
            secondClaim = claim(second, "worker-b", OffsetDateTime.now());
        }

        assertNotNull(firstClaim);
        assertNotNull(secondClaim);
        assertEquals(firstClaim.leaseVersion() + 1, secondClaim.leaseVersion());
        assertEquals(0, finish("worker-a", firstClaim.leaseVersion()));
        assertEquals(1, finish("worker-b", secondClaim.leaseVersion()));
    }

    @Test
    void jsonbTypeHandler_shouldBindRawJsonInsteadOfVarchar() throws Exception {
        // JSON 字符串必须作为 JSONB 参数绑定，不能由 JDBC 按 VARCHAR 发送。
        String payload = "{\"resumeId\":1,\"objectKey\":\"resume/1.pdf\"}";
        JsonbStringTypeHandler handler = new JsonbStringTypeHandler();

        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement(
                     "UPDATE local_message SET payload = ? WHERE id = 1")) {
            handler.setNonNullParameter(statement, 1, payload, JdbcType.OTHER);
            assertEquals(1, statement.executeUpdate());
        }

        try (Connection connection = connection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT payload ->> 'objectKey' FROM local_message WHERE id = 1")) {
            assertTrue(result.next());
            assertEquals("resume/1.pdf", result.getString(1));
        }
    }

    private Claim claim(Connection connection, String worker, OffsetDateTime now) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(CLAIM_SQL)) {
            statement.setObject(1, now);
            statement.setObject(2, now);
            statement.setString(3, worker);
            statement.setObject(4, now.plusSeconds(120));
            statement.setObject(5, now);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? new Claim(result.getLong(1), result.getLong(2)) : null;
            }
        }
    }

    private int finish(String worker, long leaseVersion) throws Exception {
        try (Connection connection = connection(); PreparedStatement statement = connection.prepareStatement("""
                UPDATE local_message
                SET status = 'SUCCESS'
                WHERE id = 1 AND status = 'PROCESSING'
                  AND lease_owner = ? AND lease_version = ?
                """)) {
            statement.setString(1, worker);
            statement.setLong(2, leaseVersion);
            return statement.executeUpdate();
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        );
    }

    private record Claim(long id, long leaseVersion) {
    }
}
