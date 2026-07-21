import { ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface UseFormOptions<T> {
  /** 初始表单数据 */
  defaultForm: T
  /** 保存函数 */
  saveFn: (data: T & { id?: number | null }) => Promise<{ code: number; msg?: string }>
  /** 删除函数（可选） */
  deleteFn?: (id: number) => Promise<{ code: number; msg?: string }>
  /** 保存成功后回调 */
  onSaveSuccess?: () => void
  /** 删除成功后回调 */
  onDeleteSuccess?: () => void
}

export function useForm<T extends Record<string, any>>(options: UseFormOptions<T>) {
  const { defaultForm, saveFn, deleteFn, onSaveSuccess, onDeleteSuccess } = options

  const dialogVisible = ref(false)
  const saving = ref(false)
  const editingId = ref<number | null>(null)
  const form = reactive<T>({ ...defaultForm })

  /** 打开新增弹窗 */
  function openAdd() {
    editingId.value = null
    Object.assign(form, defaultForm)
    dialogVisible.value = true
  }

  /** 打开编辑弹窗 */
  function openEdit(row: T & { id?: number }) {
    editingId.value = row.id ?? null
    Object.assign(form, row)
    dialogVisible.value = true
  }

  /** 打开弹窗（兼容新增/编辑） */
  function openDialog(row?: T & { id?: number }) {
    if (row) {
      openEdit(row)
    } else {
      openAdd()
    }
  }

  /** 关闭弹窗 */
  function closeDialog() {
    dialogVisible.value = false
  }

  /** 提交保存 */
  async function handleSave() {
    saving.value = true
    try {
      const res = await saveFn({ ...form, id: editingId.value } as any)
      if (res.code === 200) {
        ElMessage.success('保存成功')
        dialogVisible.value = false
        onSaveSuccess?.()
      } else {
        ElMessage.error(res.msg || '保存失败')
      }
    } finally {
      saving.value = false
    }
  }

  /** 删除确认 */
  async function handleDelete(id: number, confirmText = '确认删除？') {
    if (!deleteFn) {
      console.warn('未提供删除函数')
      return
    }

    await ElMessageBox.confirm(confirmText, '提示', { type: 'warning' })
    const res = await deleteFn(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      onDeleteSuccess?.()
    }
  }

  return {
    dialogVisible,
    saving,
    editingId,
    form,
    openAdd,
    openEdit,
    openDialog,
    closeDialog,
    handleSave,
    handleDelete,
  }
}
