package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 发布点评请求
 *
 * @author explore-platform
 */
@Data
@Schema(description = "发布点评请求")
public class ReviewPublishRequest implements Serializable {

    @Schema(description = "标题")
    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题过长")
    private String title;

    @Schema(description = "正文")
    @NotBlank(message = "正文不能为空")
    private String content;

    @Schema(description = "图片，逗号分隔")
    @Size(max = 1000, message = "图片地址过长")
    private String images;

    @Schema(description = "评分 1~5")
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低1分")
    @Max(value = 5, message = "评分最高5分")
    private Integer score;
}
