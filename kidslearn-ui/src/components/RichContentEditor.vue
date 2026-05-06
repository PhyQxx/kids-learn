<template>
  <div class="rich-editor" :class="{ compact }">
    <div class="rich-toolbar">
      <el-button size="small" @click="addParagraph">
        <el-icon><Plus /></el-icon>
        段落
      </el-button>
      <el-button size="small" :loading="uploading" @click="openFilePicker">
        <el-icon><Picture /></el-icon>
        图片
      </el-button>
      <input
        ref="fileInput"
        class="file-input"
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @change="handleFileChange"
      >
    </div>

    <div class="rich-blocks">
      <div v-for="(block, index) in blocks" :key="index" class="rich-block">
        <template v-if="block.type === 'paragraph'">
          <el-input
            v-model="block.text"
            type="textarea"
            :autosize="{ minRows: compact ? 1 : 2, maxRows: compact ? 3 : 6 }"
            placeholder="输入文字内容"
            @input="emitChange"
          />
        </template>

        <template v-else>
          <div class="image-block">
            <img :src="block.url" :alt="block.alt || 'image'">
            <el-input
              v-model="block.alt"
              size="small"
              placeholder="图片说明，朗读和摘要会使用"
              @input="emitChange"
            />
          </div>
        </template>

        <el-button
          class="remove-btn"
          link
          type="danger"
          :disabled="blocks.length === 1"
          @click="removeBlock(index)"
        >
          <el-icon><Delete /></el-icon>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Delete, Picture, Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { uploadQuestionImage } from '@/api/request'
import { parseRichContent, serializeRichContent, type RichBlock, type RichSpeech } from '@/utils/richContent'

const props = withDefaults(defineProps<{
  modelValue?: string
  compact?: boolean
}>(), {
  modelValue: '',
  compact: false,
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const blocks = ref<RichBlock[]>([])
const speech = ref<RichSpeech>({})
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const lastLocalValue = ref('')

watch(
  () => props.modelValue,
  (value) => {
    if (value === lastLocalValue.value) {
      return
    }
    const parsed = parseRichContent(value)
    blocks.value = parsed.blocks
    speech.value = parsed.speech || {}
  },
  { immediate: true }
)

function addParagraph() {
  blocks.value.push({ type: 'paragraph', text: '' })
  emitChange()
}

function removeBlock(index: number) {
  if (blocks.value.length === 1) {
    return
  }
  blocks.value.splice(index, 1)
  emitChange()
}

function openFilePicker() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }

  uploading.value = true
  try {
    const res = await uploadQuestionImage(file)
    const url = res?.data?.url
    if (!url) {
      throw new Error('图片上传失败')
    }
    blocks.value.push({ type: 'image', url, alt: file.name })
    emitChange()
  } catch (error: any) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function emitChange() {
  const serialized = serializeRichContent(blocks.value, speech.value)
  lastLocalValue.value = serialized
  emit('update:modelValue', serialized)
  nextTick(() => {
    lastLocalValue.value = ''
  })
}
</script>

<style scoped>
.rich-editor {
  width: 100%;
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 10px;
  background: #fff;
}

.rich-editor.compact {
  padding: 8px;
}

.rich-toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.file-input {
  display: none;
}

.rich-blocks {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rich-block {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 28px;
  gap: 8px;
  align-items: flex-start;
}

.image-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.image-block img {
  max-width: 220px;
  max-height: 160px;
  border-radius: 6px;
  border: 1px solid var(--el-border-color-lighter);
  object-fit: contain;
  background: #f7f8fa;
}

.remove-btn {
  min-height: 28px;
}
</style>
