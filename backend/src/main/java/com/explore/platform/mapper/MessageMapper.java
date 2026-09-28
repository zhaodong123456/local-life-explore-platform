package com.explore.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.explore.platform.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/**
 * 站内消息 Mapper
 *
 * @author explore-platform
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
