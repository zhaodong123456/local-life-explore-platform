<template>
  <div class="dashboard">
    <!-- 筛选栏：商铺选择 + 时间范围 -->
    <el-card shadow="never" class="filter-card">
      <div class="filter-bar">
        <div class="filter-item">
          <span class="filter-label">商铺</span>
          <el-select v-model="shopId" placeholder="请选择商铺" filterable style="width: 260px"
                     :loading="shopLoading">
            <el-option v-for="s in shops" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div class="filter-item">
          <span class="filter-label">时间范围</span>
          <el-radio-group v-model="days">
            <el-radio-button v-for="d in dayOptions" :key="d" :value="d">{{ d }} 天</el-radio-button>
          </el-radio-group>
        </div>
      </div>
    </el-card>

    <!-- 空态：名下无商铺 -->
    <el-empty v-if="!shopLoading && shops.length === 0"
              description="您名下暂无商铺，请先创建商铺并等待审核通过" />

    <!-- 图表区 -->
    <el-row v-show="shops.length > 0" :gutter="16">
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>访问量 / 核销量趋势</span>
          </template>
          <div ref="trafficChartRef" class="chart" v-loading="loading" />
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="12">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <span>点评数量 / 平均分趋势</span>
          </template>
          <div ref="reviewChartRef" class="chart" v-loading="loading" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref, shallowRef, watch, nextTick } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import request from '@/utils/request'

echarts.use([LineChart, GridComponent, TooltipComponent, LegendComponent, CanvasRenderer])

/** 商铺下拉数据（GET /api/merchant/shops，名下商铺分页） */
const shops = ref([])
const shopId = ref(null)
const shopLoading = ref(false)

/** 时间范围（后端限制 1~90，提供预设档位） */
const dayOptions = [7, 14, 30, 60, 90]
const days = ref(14)

const loading = ref(false)
const trafficChartRef = ref(null)
const reviewChartRef = ref(null)
const trafficChart = shallowRef(null)
const reviewChart = shallowRef(null)
let chartsReady = false

/** 加载商家名下商铺，默认选中第一个 */
async function fetchShops() {
  shopLoading.value = true
  try {
    const page = await request.get('/merchant/shops', { params: { pageNum: 1, pageSize: 100 } })
    shops.value = page?.records || []
    if (shops.value.length > 0) {
      shopId.value = shops.value[0].id // 触发 watch → 自动加载看板数据
    }
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    shopLoading.value = false
  }
}

/** 拉取看板统计数据（GET /api/merchant/dashboard/stats） */
async function fetchStats() {
  if (!shopId.value) return
  loading.value = true
  try {
    const data = await request.get('/merchant/dashboard/stats',
      { params: { shopId: shopId.value, days: days.value } })
    renderCharts(data)
  } catch (e) {
    /* 错误提示由 request.js 统一弹出 */
  } finally {
    loading.value = false
  }
}

/** 折线序列公共样式 */
function buildSeries(name, data, yAxisIndex, extra = {}) {
  return {
    name,
    type: 'line',
    data,
    yAxisIndex,
    smooth: true,
    symbolSize: 6,
    connectNulls: false, // 平均分无数据日（null）保留断点，不强行连线
    ...extra
  }
}

function buildOption(dates, seriesList, yAxes) {
  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: { top: 0 },
    grid: { top: 36, left: 16, right: 16, bottom: 8, containLabel: true },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: dates,
      axisLabel: { formatter: (v) => v.slice(5) } // 只展示 MM-DD，悬浮显示完整日期
    },
    yAxis: yAxes,
    series: seriesList
  }
}

/** 渲染两张图（DashboardVO：dates + visitCounts/writeoffCounts/reviewCounts/avgScores） */
function renderCharts(data) {
  if (!data || !chartsReady) return
  const dates = data.dates || []

  // 图A：访问量（左轴，面积渐变）+ 核销量（右轴）。双轴避免量级差异把小序列压平
  trafficChart.value.setOption({
    ...buildOption(
      dates,
      [
        buildSeries('访问量', data.visitCounts || [], 0, {
          areaStyle: { opacity: 0.15 },
          itemStyle: { color: '#409eff' }
        }),
        buildSeries('核销量', data.writeoffCounts || [], 1, {
          itemStyle: { color: '#67c23a' }
        })
      ],
      [
        { type: 'value', name: '访问量', minInterval: 1 },
        { type: 'value', name: '核销量', minInterval: 1, splitLine: { show: false } }
      ]
    ),
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' },
      formatter: (params) => {
        const lines = params.map(p => `${p.marker}${p.seriesName}：<b>${p.value ?? 0}</b>`)
        return [dates[params[0]?.dataIndex] || '', ...lines].join('<br/>')
      }
    }
  }, true) // notMerge=true：切换商铺/天数时整体替换，防止残留旧序列

  // 图B：点评数（左轴）+ 平均分（右轴 0~5，null 断点）
  reviewChart.value.setOption({
    ...buildOption(
      dates,
      [
        buildSeries('点评数', data.reviewCounts || [], 0, {
          itemStyle: { color: '#e6a23c' }
        }),
        buildSeries('平均分', data.avgScores || [], 1, {
          itemStyle: { color: '#f56c6c' }
        })
      ],
      [
        { type: 'value', name: '点评数', minInterval: 1 },
        { type: 'value', name: '平均分', min: 0, max: 5, splitLine: { show: false } }
      ]
    )
  }, true)
}

/** 初始化 ECharts 实例（容器渲染完成后调用一次） */
function initCharts() {
  if (chartsReady || !trafficChartRef.value || !reviewChartRef.value) return
  trafficChart.value = echarts.init(trafficChartRef.value)
  reviewChart.value = echarts.init(reviewChartRef.value)
  chartsReady = true
}

function handleResize() {
  trafficChart.value?.resize()
  reviewChart.value?.resize()
}

watch(shops, async (list) => {
  if (list.length > 0) {
    await nextTick() // 图表区 v-show 打开后容器才有尺寸
    initCharts()
  }
})

watch([shopId, days], () => fetchStats())

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  await fetchShops()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trafficChart.value?.dispose()
  reviewChart.value?.dispose()
  chartsReady = false
})
</script>

<style scoped>
.filter-card {
  margin-bottom: 16px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 32px;
  align-items: center;
}
.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.filter-label {
  color: #606266;
  font-size: 14px;
}
.chart-card {
  margin-bottom: 16px;
}
.chart {
  width: 100%;
  height: 340px;
}
/* 移动端适当降低图表高度 */
@media (max-width: 768px) {
  .chart {
    height: 260px;
  }
}
</style>
