package com.explore.platform.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 商家数据看板 VO
 * <p>
 * 结构为 ECharts 折线图量身定制：x 轴 dates 与三条 y 轴序列按下标一一对应，
 * 缺失日期已在 Service 层补齐（数值指标补 0，平均分补 null 断点），前端无需再加工。
 *
 * @author explore-platform
 */
@Data
@Schema(description = "商家数据看板")
public class DashboardVO implements Serializable {

    @Schema(description = "商铺ID")
    private Long shopId;

    @Schema(description = "商铺名称")
    private String shopName;

    @Schema(description = "日期序列（yyyy-MM-dd，升序，与下列各序列下标对应）")
    private List<String> dates;

    @Schema(description = "每日访问量")
    private List<Integer> visitCounts;

    @Schema(description = "每日核销量")
    private List<Integer> writeoffCounts;

    @Schema(description = "每日新增点评数")
    private List<Integer> reviewCounts;

    @Schema(description = "每日点评平均分（无点评的日子为 null，前端折线断点）")
    private List<BigDecimal> avgScores;
}
