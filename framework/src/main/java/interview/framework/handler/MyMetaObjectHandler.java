package interview.framework.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import interview.common.util.TraceUtil;
import interview.framework.context.AuthContext;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author zhuxi
 * @since 2026-05-26
 */

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject,"updatedBy", Long.class, AuthContext.getUserId());
        this.strictInsertFill(metaObject,"createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject,"traceId", String.class, TraceUtil.getTraceId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedBy", Long.class, AuthContext.getUserId());
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        this.strictUpdateFill(metaObject, "traceId", String.class, TraceUtil.getTraceId());
    }
}
