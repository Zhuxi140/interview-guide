package interview.infra.localMessage.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import interview.infra.localMessage.model.entity.LocalMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LocalMessageMapper extends BaseMapper<LocalMessage> {

}
