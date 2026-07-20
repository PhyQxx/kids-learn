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
      <el-button size="small" type="primary" plain @click="showAiImageDialog">
        ✨ AI图片
      </el-button>
      <input
        ref="fileInput"
        class="file-input"
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @change="handleFileChange"
      >
    </div>

    <!-- AI图片生成对话框 -->
    <el-dialog
      v-model="aiImageDialogVisible"
      title="AI生成图片"
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form @submit.prevent="handleAiGenerateImage">
        <el-form-item label="图片描述">
          <el-input
            v-model="aiImagePrompt"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请描述你想生成的图片内容，例如：一只可爱的卡通小猫在花园里玩耍"
          />
        </el-form-item>
        <el-form-item label="图片尺寸">
          <el-select v-model="aiImageSize" style="width: 100%">
            <el-option label="正方形 1:1" value="1024x1024" />
            <el-option label="横版 4:3" value="1024x768" />
            <el-option label="竖版 3:4" value="768x1024" />
            <el-option label="宽屏 16:9" value="1920x1080" />
            <el-option label="竖屏 9:16" value="1080x1920" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiImageDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="aiImageGenerating"
          :disabled="!aiImagePrompt.trim()"
          @click="handleAiGenerateImage"
        >
          生成图片
        </el-button>
      </template>
    </el-dialog>

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
import { uploadQuestionImage, aiGenerateImage } from '@/api/request'
import { parseRichContent, serializeRichContent, type RichBlock, type RichSpeech } from '@/utils/richContent'

export interface RichEditorContext {
  subjectName?: string
  gradeName?: string
  questionType?: string
  questionContent?: string
  knowledgePoint?: string
  /** 用途标识：'question' 表示题目内容，'option' 表示选项内容 */
  usage?: 'question' | 'option'
}

const props = withDefaults(defineProps<{
  modelValue?: string
  compact?: boolean
  context?: RichEditorContext
}>(), {
  modelValue: '',
  compact: false,
  context: undefined,
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const blocks = ref<RichBlock[]>([])
const speech = ref<RichSpeech>({})
const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)
const lastLocalValue = ref('')

// AI图片生成相关
const aiImageDialogVisible = ref(false)
const aiImagePrompt = ref('')
const aiImageSize = ref('1024x1024')
const aiImageGenerating = ref(false)

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

function getEditorText(): string {
  return blocks.value
    .filter(b => b.type === 'paragraph')
    .map(b => (b as any).text || '')
    .join(' ')
    .trim()
    .substring(0, 100)
}

/** 从富文本字符串中提取纯文本 */
function extractTextFromRichContent(content?: string): string {
  if (!content) return ''
  try {
    const parsed = JSON.parse(content)
    if (parsed.blocks && Array.isArray(parsed.blocks)) {
      return parsed.blocks
        .filter((b: any) => b.type === 'paragraph')
        .map((b: any) => b.text || '')
        .join(' ')
        .trim()
    }
  } catch {
    // 不是JSON格式，尝试去掉HTML标签
  }
  return content.replace(/<[^>]+>/g, '').trim()
}

function showAiImageDialog() {
  // 根据上下文自动生成提示词
  const ctx = props.context
  if (ctx) {
    const parts: string[] = []
    if (ctx.usage === 'option') {
      // 选项图片：以选项内容为主，题目为辅
      const optionText = getEditorText()
      if (optionText) {
        parts.push(`一张"${optionText}"的卡通插图`)
      } else {
        parts.push('为儿童教育题目选项生成一张配图')
      }
      // 简要说明题目背景
      if (ctx.questionContent) {
        const text = extractTextFromRichContent(ctx.questionContent).substring(0, 50)
        if (text) parts.push(`题目背景：${text}`)
      }
    } else {
      // 题目图片：基于题目信息生成图片
      const questionText = getEditorText()
      if (questionText) {
        parts.push(`一张"${questionText}"的卡通插图`)
      } else {
        parts.push('为儿童教育题目生成一张配图')
      }
      if (ctx.subjectName) parts.push(`学科：${ctx.subjectName}`)
      if (ctx.knowledgePoint) parts.push(`知识点：${ctx.knowledgePoint}`)
    }
    parts.push('风格：卡通可爱，适合儿童，清晰简洁')
    aiImagePrompt.value = parts.join('，')
  } else {
    aiImagePrompt.value = '为儿童教育题目生成一张卡通配图，风格可爱，清晰简洁'
  }
  aiImageSize.value = '1024x1024'
  aiImageDialogVisible.value = true
}


async function handleAiGenerateImage() {
  if (!aiImagePrompt.value.trim()) {
    ElMessage.warning('请输入图片描述')
    return
  }
  aiImageGenerating.value = true
  try {
    const res = await aiGenerateImage(aiImagePrompt.value.trim(), aiImageSize.value)
    if (res.code === 200 && res.data?.imageUrl) {
      blocks.value.push({ type: 'image', url: res.data.imageUrl, alt: aiImagePrompt.value.trim() })
      emitChange()
      aiImageDialogVisible.value = false
      ElMessage.success('AI图片已生成')
    } else {
      ElMessage.error(res.msg || 'AI图片生成失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || 'AI图片生成失败')
  } finally {
    aiImageGenerating.value = false
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
