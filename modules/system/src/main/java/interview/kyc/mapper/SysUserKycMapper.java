package interview.kyc.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.kyc.model.entity.SysUserKyc;

/**
 * @author zhuxi
 * @apiNote 个人实名认证表 Mapper（简单查询一律 LambdaQuery，无手写 SQL）
 */
public interface SysUserKycMapper extends BaseMapper<SysUserKyc> {
}
