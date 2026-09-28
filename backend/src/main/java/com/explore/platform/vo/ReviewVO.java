package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点评展示对象
 *
 * @author explore-platform
 */
@Data
@Schema(description = "点评信息")
public class ReviewVO implements Serializable {

    @Schema(description = "点评ID")
    private Long id;

    @Schema(description = "商铺ID")
    private Long shopId;

    @Schema(description = "发布用户ID")
    private Long userId;

    @Schema(description = "发布者昵称")
    private String nickname;

    @Schema(description = "发布者头像")
    private String avatar;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "正文")
    private String content;

    @Schema(description = "图片，逗号分隔")
    private String images;

    @Schema(description = "评分 1~5")
    private Integer score;

    @Schema(description = "状态 0待审核 1已通过 2已驳回")
    private Integer status;

    @Schema(description = "审核备注（我的点评返回）")
    private String auditRemark;

    @Schema(description = "点赞总数")
    private Long likeCount;

    @Schema(description = "当前查看者是否已点赞")
    private Boolean liked;

    @Schema(description = "发布时间")
    private LocalDateTime createTime;
}
