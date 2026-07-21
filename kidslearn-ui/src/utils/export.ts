/**
 * 数据导出工具
 * 支持导出为CSV格式
 */

interface ExportColumn {
  title: string
  key: string
  formatter?: (value: any, row: any) => string
}

interface ExportOptions<T> {
  data: T[]
  columns: ExportColumn[]
  filename?: string
}

/**
 * 导出数据为CSV文件
 */
export function exportToCsv<T extends Record<string, any>>(options: ExportOptions<T>) {
  const { data, columns, filename = 'export' } = options

  // 构建表头
  const header = columns.map(col => `"${col.title}"`).join(',')

  // 构建数据行
  const rows = data.map(row => {
    return columns.map(col => {
      let value = row[col.key]
      if (col.formatter) {
        value = col.formatter(value, row)
      }
      // 处理值中的引号和换行
      if (value === null || value === undefined) value = ''
      value = String(value).replace(/"/g, '""').replace(/\n/g, ' ')
      return `"${value}"`
    }).join(',')
  })

  // 组合CSV内容
  const csvContent = [header, ...rows].join('\n')

  // 添加BOM头以支持中文
  const bom = '\uFEFF'
  const blob = new Blob([bom + csvContent], { type: 'text/csv;charset=utf-8' })

  // 创建下载链接
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `${filename}_${formatDate(new Date())}.csv`
  link.click()

  // 释放URL
  URL.revokeObjectURL(link.href)
}

/**
 * 导出表格数据（通用方法）
 */
export function exportTableData<T extends Record<string, any>>(
  data: T[],
  columns: { label: string; prop: string }[],
  filename?: string
) {
  const exportColumns: ExportColumn[] = columns.map(col => ({
    title: col.label,
    key: col.prop,
  }))

  exportToCsv({ data, columns: exportColumns, filename })
}

function formatDate(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}${month}${day}_${hours}${minutes}`
}
