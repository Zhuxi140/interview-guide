package interview.ai.providerconfig.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.ai.providerconfig.model.entity.LlmProviderConfig;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmProviderConfigMapper extends BaseMapper<LlmProviderConfig> {
}
