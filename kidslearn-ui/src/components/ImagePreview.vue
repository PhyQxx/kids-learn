<template>
  <div class="image-preview-wrapper" @click="handlePreview">
    <el-image
      v-if="src"
      :src="src"
      :size="size"
      fit="cover"
      class="preview-image"
      :style="{ width: size + 'px', height: size + 'px' }"
    >
      <template #error>
        <div class="image-error">
          <el-icon><Picture /></el-icon>
        </div>
      </template>
    </el-image>
    <span v-else class="no-image">-</span>
  </div>
</template>

<script setup lang="ts">
import { Picture } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  src?: string
  size?: number
}>(), {
  size: 40,
})

function handlePreview() {
  if (!props.src) return
  // 使用Element Plus的图片预览
  const overlay = document.createElement('div')
  overlay.style.cssText = 'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.7);display:flex;align-items:center;justify-content:center;z-index:9999;cursor:pointer'
  overlay.onclick = () => document.body.removeChild(overlay)

  const img = document.createElement('img')
  img.src = props.src
  img.style.cssText = 'max-width:90%;max-height:90%;object-fit:contain;border-radius:8px'
  img.onerror = () => {
    document.body.removeChild(overlay)
  }

  overlay.appendChild(img)
  document.body.appendChild(overlay)
}
</script>

<style scoped>
.image-preview-wrapper {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.preview-image {
  border-radius: 6px;
  transition: transform 0.2s;
}

.preview-image:hover {
  transform: scale(1.05);
}

.image-error {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  background: #f5f7fa;
  color: #c0c4cc;
}

.no-image {
  color: #c0c4cc;
}
</style>
