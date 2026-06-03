package interview.framework.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 */

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {



    @Override
    public void insertFill(MetaObject metaObject) {

        this.strictInsertFill(metaObject,"updatedBy", Long.class, AuthContext.getUserIdSafe());
        this.strictInsertFill(metaObject,"createdAt", OffsetDateTime.class, OffsetDateTime.now());
        this.strictInsertFill(metaObject,"traceId", String.class, TraceUtil.getTraceId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, AuthContext.getUserIdSafe());
        this.strictUpdateFill(metaObject, "updatedAt", OffsetDateTime.class, OffsetDateTime.now());
        this.strictUpdateFill(metaObject, "traceId", String.class, TraceUtil.getTraceId());
    }
}
