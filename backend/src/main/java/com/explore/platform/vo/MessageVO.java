package com.explore.platform.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 站内消息视图对象
 *
 * @author explore-platform
 */
@Data
public class MessageVO implements Serializable {

    private String id;

    private Integer type;

    private String title;

    private String content;

    private String bizType;

    private Long bizId;

    private Integer isRead;

    private LocalDateTime createTime;

    private LocalDateTime readTime;
}
