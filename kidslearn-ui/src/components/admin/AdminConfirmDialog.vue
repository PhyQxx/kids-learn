<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="480px"
    append-to-body
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div class="admin-confirm-dialog" :class="`is-${tone}`">
      <div class="admin-confirm-dialog__icon"><el-icon><WarningFilled /></el-icon></div>
      <div>
        <p class="admin-confirm-dialog__object">{{ objectName }}</p>
        <p class="admin-confirm-dialog__impact">{{ impact }}</p>
      </div>
    </div>
    <el-form-item v-if="requireInput" :label="`请输入“${objectName}”确认`" class="admin-confirm-dialog__verify">
      <el-input v-model="verification" autocomplete="off" />
    </el-form-item>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button :type="tone === 'danger' ? 'danger' : 'primary'" :loading="loading" :disabled="!canConfirm" @click="emit('confirm')">
        {{ confirmText }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'

const props = withDefaults(defineProps<{
  modelValue: boolean
  title: string
  objectName: string
  impact: string
  confirmText?: string
  tone?: 'warning' | 'danger'
  requireInput?: boolean
  loading?: boolean
}>(), {
  confirmText: '确认',
  tone: 'warning',
  requireInput: false,
  loading: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  confirm: []
}>()

const verification = ref('')
const canConfirm = computed(() => !props.requireInput || verification.value === props.objectName)

watch(() => props.modelValue, (open) => {
  if (!open) verification.value = ''
})
</script>

<style scoped>
.admin-confirm-dialog { display: flex; gap: var(--space-3); align-items: flex-start; }
.admin-confirm-dialog__icon { flex: 0 0 auto; color: var(--color-warning-700); font-size: 24px; }
.admin-confirm-dialog.is-danger .admin-confirm-dialog__icon { color: var(--color-danger-600); }
.admin-confirm-dialog__object { margin: 0; color: var(--admin-text); font-weight: 600; }
.admin-confirm-dialog__impact { margin: var(--space-1) 0 0; color: var(--admin-muted); }
.admin-confirm-dialog__verify { margin-top: var(--space-5); }
</style>

