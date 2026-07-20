import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'

/**
 * 自动计算 el-table 可用高度。
 * 同时用 JS 设置 tableBox 的 flex 布局，不依赖 CSS 选择器链路。
 *
 * <div ref="tableBox">
 *   <div class="filter-row">...</div>
 *   <el-table :max-height="tableMaxHeight" ... />
 *   <el-pagination ... />
 * </div>
 */
export function useTableHeight() {
  const tableBox = ref<HTMLElement | null>(null)
  const tableMaxHeight = ref(500)

  let observer: ResizeObserver | null = null

  function recalc() {
    const el = tableBox.value
    if (!el) return

    // 用 JS 强制设置 flex 布局，绕过 CSS 选择器匹配问题
    el.style.flex = '1'
    el.style.minHeight = '0'
    el.style.display = 'flex'
    el.style.flexDirection = 'column'

    const containerH = el.clientHeight
    if (containerH <= 0) return

    // 减去 tableBox 内非表格子元素（筛选行、分页器）
    let usedHeight = 0
    for (const child of Array.from(el.children)) {
      const htmlChild = child as HTMLElement
      if (htmlChild.classList.contains('el-table') || htmlChild.classList.contains('el-table__wrapper')) continue
      const style = window.getComputedStyle(htmlChild)
      if (style.display === 'none') continue
      usedHeight += htmlChild.offsetHeight
      usedHeight += parseFloat(style.marginTop) || 0
      usedHeight += parseFloat(style.marginBottom) || 0
    }

    // tableBox 自身 padding
    const boxStyle = window.getComputedStyle(el)
    usedHeight += parseFloat(boxStyle.paddingTop) || 0
    usedHeight += parseFloat(boxStyle.paddingBottom) || 0

    tableMaxHeight.value = Math.max(containerH - usedHeight, 200)
  }

  onMounted(() => {
    nextTick(() => {
      recalc()
      if (tableBox.value) {
        observer = new ResizeObserver(() => recalc())
        observer.observe(tableBox.value)
      }
    })
  })

  onBeforeUnmount(() => {
    observer?.disconnect()
  })

  return { tableBox, tableMaxHeight }
}
