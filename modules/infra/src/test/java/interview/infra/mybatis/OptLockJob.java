package interview.infra.mybatis;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

@Data
@TableName("opt_lock_job")
public class OptLockJob {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long enterpriseId;

    private String title;

    @Version
    private Integer version;

    @TableLogic(value = "false", delval = "true")
    private Boolean isDeleted;
}