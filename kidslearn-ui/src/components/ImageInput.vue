<template>
  <div class="image-input">
    <div class="input-row">
      <el-input
        :model-value="modelValue"
        placeholder="粘贴图片URL，或上传/AI生成"
        clearable
        @update:model-value="emit('update:modelValue', $event || '')"
      />
      <el-button :loading="uploading" @click="openFilePicker">
        <el-icon><Upload /></el-icon>
      </el-button>
      <el-button type="primary" plain @click="showAiImageDialog">
        <el-icon><MagicStick /></el-icon>
      </el-button>
      <input
        ref="fileInput"
        class="file-input"
        type="file"
        accept="image/png,image/jpeg,image/gif,image/webp"
        @change="handleFileChange"
      >
    </div>

    <!-- 缩略图预览 -->
    <div v-if="modelValue" class="preview-box" @click="handlePreview">
      <img v-if="isImageUrl" :src="modelValue" class="preview-img" />
      <span v-else class="preview-emoji">{{ modelValue }}</span>
    </div>

    <!-- AI图片生成对话框 -->
    <el-dialog
      v-model="aiImageDialogVisible"
      title="AI生成图片"
      width="480px"
      :close-on-click-modal="false"
      append-to-body
    >
      <el-form @submit.prevent="handleAiGenerateImage">
        <el-form-item label="图片描述">
          <el-input
            v-model="aiImagePrompt"
            type="textarea"
            :autosize="{ minRows: 3, maxRows: 6 }"
            placeholder="请描述你想生成的图片内容，例如：一只可爱的卡通小猫"
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
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { MagicStick, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { aiGenerateImage, uploadQuestionImage } from '@/api/request'

const props = withDefaults(defineProps<{
  modelValue?: string
  /** 用途提示，用于自动拼接 AI 生图 prompt，如 "宠物基础形象" */
  hint?: string
}>(), {
  modelValue: '',
  hint: '',
})

const emit = defineEmits<{
  (event: 'update:modelValue', value: string): void
}>()

const fileInput = ref<HTMLInputElement>()
const uploading = ref(false)

// AI图片生成相关
const aiImageDialogVisible = ref(false)
const aiImagePrompt = ref('')
const aiImageSize = ref('1024x1024')
const aiImageGenerating = ref(false)

const URL_RE = /^(https?:|data:|\/\/|\/static\/)/
const isImageUrl = computed(() => URL_RE.test(props.modelValue || ''))

function openFilePicker() {
  fileInput.value?.click()
}

async function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return

  uploading.value = true
  try {
    const res = await uploadQuestionImage(file)
    const url = res?.data?.url
    if (!url) throw new Error('图片上传失败')
    emit('update:modelValue', url)
    ElMessage.success('图片已上传')
  } catch (error: any) {
    ElMessage.error(error?.message || '图片上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function showAiImageDialog() {
  const subject = props.hint ? `"${props.hint}"` : '儿童学习应用'
  aiImagePrompt.value = `一张${subject}的卡通形象图，风格：卡通可爱，适合儿童，清晰简洁，透明或纯色背景`
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
      emit('update:modelValue', res.data.imageUrl)
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

function handlePreview() {
  if (!props.modelValue || !isImageUrl.value) return
  const overlay = document.createElement('div')
  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.7);display:flex;align-items:center;justify-content:center;z-index:9999;cursor:pointer'
  overlay.onclick = () => document.body.removeChild(overlay)

  const img = document.createElement('img')
  img.src = props.modelValue
  img.style.cssText = 'max-width:90%;max-height:90%;object-fit:contain;border-radius:8px'
  img.onerror = () => document.body.removeChild(overlay)

  overlay.appendChild(img)
  document.body.appendChild(overlay)
}
</script>

<style scoped>
.image-input {
  width: 100%;
}

.input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}

.file-input {
  display: none;
}

.preview-box {
  margin-top: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 6px;
  background: #f7f8fa;
  cursor: pointer;
  overflow: hidden;
  transition: transform 0.2s;
}

.preview-box:hover {
  transform: scale(1.05);
}

.preview-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.preview-emoji {
  font-size: 40px;
  line-height: 1;
}
</style>
