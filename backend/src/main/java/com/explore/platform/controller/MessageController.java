package com.explore.platform.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.explore.platform.common.result.Result;
import com.explore.platform.security.SecurityUtils;
import com.explore.platform.service.MessageService;
import com.explore.platform.vo.MessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户侧：站内消息接口
 *
 * @author explore-platform
 */
@Tag(name = "用户-站内消息")
@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "我的消息分页（可按已读过滤）")
    @GetMapping
    public Result<IPage<MessageVO>> myMessages(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer isRead) {
        return Result.ok(messageService.pageMyMessages(SecurityUtils.getCurrentUserId(), pageNum, pageSize, isRead));
    }

    @Operation(summary = "未读数（小红点）")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.unreadCount(SecurityUtils.getCurrentUserId()));
    }

    @Operation(summary = "单条已读")
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long messageId) {
        messageService.markRead(SecurityUtils.getCurrentUserId(), messageId);
        return Result.ok();
    }

    @Operation(summary = "全部已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        messageService.markAllRead(SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
