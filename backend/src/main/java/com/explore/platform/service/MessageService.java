package com.explore.platform.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.vo.MessageVO;

/**
 * 站内消息服务
 *
 * @author explore-platform
 */
public interface MessageService {

    /**
     * 发送站内消息（持久化到 MySQL）
     *
     * @param receiverUserId 接收人
     * @param type           消息类型 1点赞提醒 2优惠券到期 3入驻审核结果 4点评审核结果
     * @param title          标题
     * @param content        内容
     * @param bizType        业务类型标识（幂等键）
     * @param bizId          关联业务ID（幂等键）
     * @return 消息ID，如果已存在则返回已有消息ID（幂等）
     */
    Long sendMessage(Long receiverUserId, Integer type, String title, String content,
                     String bizType, Long bizId);

    /**
     * 我的消息分页（时间倒序）
     */
    IPage<MessageVO> pageMyMessages(Long userId, Integer pageNum, Integer pageSize, Integer isRead);

    /**
     * 未读数（小红点）
     */
    Long unreadCount(Long userId);

    /**
     * 单条已读
     */
    void markRead(Long userId, Long messageId);

    /**
     * 全部已读
     */
    void markAllRead(Long userId);
}
