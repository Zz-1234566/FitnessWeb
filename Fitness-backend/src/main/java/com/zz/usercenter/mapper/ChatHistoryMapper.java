package com.zz.usercenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zz.usercenter.model.domain.ChatHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话记录 Mapper
 * @Author zhouzhou
 * @Date 2026/3/25 19:20:27
 * @Description 针对表【chat_history】的数据库操作Mapper
 * @Entity generator.domain.ChatHistory
 */
@Mapper
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {
}
