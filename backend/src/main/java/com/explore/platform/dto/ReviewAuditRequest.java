package com.explore.platform.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 点评审核请求（管理员）
 *
 * @author explore-platform
 */
@Data
@Schema(description = "点评审核请求")
public class ReviewAuditRequest implements Serializable {

    @Schema(description = "是否通过 true通过 false驳回")
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    @Schema(description = "审核备注（驳回时建议填写原因）")
    private String remark;
}
