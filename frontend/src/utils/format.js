/**
 * 公共格式化工具
 */

/** ISO 时间串（yyyy-MM-ddTHH:mm:ss）→ yyyy-MM-dd HH:mm */
export function fmtTime(iso) {
  if (!iso) return '-'
  return String(iso).replace('T', ' ').slice(0, 16)
}

/** 仅日期部分 yyyy-MM-dd */
export function fmtDate(iso) {
  if (!iso) return '-'
  return String(iso).slice(0, 10)
}

/** 金额展示：null → '面议'，否则 ¥xx */
export function fmtPrice(v) {
  return v == null ? '面议' : `¥${v}`
}

/** 逗号分隔的图片串 → URL 数组（过滤空项） */
export function splitImages(images) {
  if (!images) return []
  return String(images).split(',').map((s) => s.trim()).filter(Boolean)
}

/** 图片加载失败时的渐变占位（内联 SVG data URI） */
export const IMG_PLACEHOLDER =
  'data:image/svg+xml;utf8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="300">' +
      '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">' +
      '<stop offset="0" stop-color="#e8f1fd"/><stop offset="1" stop-color="#f5f7fa"/>' +
      '</linearGradient></defs>' +
      '<rect width="400" height="300" fill="url(#g)"/>' +
      '<text x="200" y="150" text-anchor="middle" dominant-baseline="middle" fill="#a0cfff" font-size="16">暂无图片</text>' +
      '</svg>'
  )
