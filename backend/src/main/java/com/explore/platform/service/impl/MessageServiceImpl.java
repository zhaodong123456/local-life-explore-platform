package com.explore.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.explore.platform.common.exception.BusinessException;
import com.explore.platform.entity.Message;
import com.explore.platform.mapper.MessageMapper;
import com.explore.platform.service.MessageService;
import com.explore.platform.vo.MessageVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 站内消息服务实现。
 * <p>
 * 幂等设计：消息通过 (receiver_user_id, biz_type, biz_id) 业务键唯一，
 * 同一事件重复触发/重复消费不会产生重复消息。
 *
 * @author explore-platform
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long sendMessage(Long receiverUserId, Integer type, String title, String content,
                            String bizType, Long bizId) {
        // 幂等校验：同一接收人+业务类型+业务ID 已存在则直接返回（重复触发/重复消费不重复发）
        Message exist = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverUserId, receiverUserId)
                .eq(Message::getBizType, bizType)
                .eq(Message::getBizId, bizId)
                .last("LIMIT 1"));
        if (exist != null) {
            log.info("消息已存在，幂等返回：receiver={}, bizType={}, bizId={}", receiverUserId, bizType, bizId);
            return exist.getId();
        }

        Message message = new Message();
        message.setReceiverUserId(receiverUserId);
        message.setType(type);
        message.setTitle(title);
        message.setContent(content);
        message.setBizType(bizType);
        message.setBizId(bizId);
        message.setIsRead(0);
        try {
            messageMapper.insert(message);
        } catch (DuplicateKeyException e) {
            // 并发兜底：唯一键冲突时查已有记录返回
            log.info("消息唯一键冲突，幂等返回：receiver={}, bizType={}, bizId={}", receiverUserId, bizType, bizId);
            Message dup = messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                    .eq(Message::getReceiverUserId, receiverUserId)
                    .eq(Message::getBizType, bizType)
                    .eq(Message::getBizId, bizId)
                    .last("LIMIT 1"));
            return dup != null ? dup.getId() : null;
        }
        return message.getId();
    }

    @Override
    public IPage<MessageVO> pageMyMessages(Long userId, Integer pageNum, Integer pageSize, Integer isRead) {
        Page<Message> page = messageMapper.selectPage(new Page<>(pageNum, Math.min(pageSize, 100)),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverUserId, userId)
                        .eq(isRead != null, Message::getIsRead, isRead)
                        .orderByDesc(Message::getCreateTime));
        return page.convert(this::toVO);
    }

    @Override
    public Long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long messageId) {
        Message message = messageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        if (!message.getReceiverUserId().equals(userId)) {
            throw new BusinessException("无权操作他人消息");
        }
        if (message.getIsRead() == 1) {
            return; // 幂等
        }
        message.setIsRead(1);
        message.setReadTime(LocalDateTime.now());
        messageMapper.updateById(message);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        Message update = new Message();
        update.setIsRead(1);
        update.setReadTime(LocalDateTime.now());
        messageMapper.update(update, new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverUserId, userId)
                .eq(Message::getIsRead, 0));
    }

    private MessageVO toVO(Message message) {
        MessageVO vo = new MessageVO();
        BeanUtils.copyProperties(message, vo);
        vo.setId(String.valueOf(message.getId()));
        return vo;
    }
}
