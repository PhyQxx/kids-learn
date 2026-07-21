<template>
  <div class="feedback-audio-page">
    <section class="page-bar">
      <div>
        <h2>反馈语音配置</h2>
        <p>配置答题时答对/答错的反馈语音，支持试听和 AI 生成。</p>
      </div>
      <div class="actions">
        <el-button :icon="Refresh" @click="fetchData" :loading="loading">刷新</el-button>
        <el-button type="primary" :icon="Check" @click="handleSave" :loading="saving">保存</el-button>
      </div>
    </section>

    <section class="config-card" v-loading="loading">
      <el-form label-width="120px" label-position="left">
        <el-form-item label="基础URL">
          <el-input v-model="form.baseUrl" placeholder="https://example.com/audio/feedback" />
          <div class="form-tip">音频文件的基础访问地址，文件名会自动拼接到此 URL 后</div>
        </el-form-item>

        <el-divider>
          <el-icon><CircleCheck /></el-icon> 答对反馈语音
        </el-divider>

        <el-form-item label="文件列表">
          <div class="audio-list-editor">
            <div v-for="(item, idx) in correctItems" :key="'c-' + idx" class="audio-item">
              <div class="audio-input-row">
                <el-input v-model="correctItems[idx]" placeholder="文件名（不含扩展名）" style="flex:1">
                  <template #prepend>{{ form.baseUrl }}/</template>
                  <template #append>.wav</template>
                </el-input>
                <el-button :icon="VideoPlay" circle @click="previewAudio(correctItems[idx])" :disabled="!correctItems[idx]?.trim()" title="试听" />
                <el-button type="danger" :icon="Delete" circle @click="removeItem('correct', idx)" :disabled="correctItems.length <= 1" />
              </div>
              <div v-if="generatingCorrect === idx" class="generating-hint">
                <el-icon class="is-loading"><Loading /></el-icon>
                AI 正在生成...
              </div>
            </div>
            <div class="audio-actions">
              <el-button type="primary" plain @click="addItem('correct')">+ 添加答对语音</el-button>
              <el-button type="success" plain @click="aiGenerate('correct')" :loading="generatingCorrect !== null">
                <el-icon><MagicStick /></el-icon> AI 生成
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider>
          <el-icon><CircleClose /></el-icon> 答错反馈语音
        </el-divider>

        <el-form-item label="文件列表">
          <div class="audio-list-editor">
            <div v-for="(item, idx) in wrongItems" :key="'w-' + idx" class="audio-item">
              <div class="audio-input-row">
                <el-input v-model="wrongItems[idx]" placeholder="文件名（不含扩展名）" style="flex:1">
                  <template #prepend>{{ form.baseUrl }}/</template>
                  <template #append>.wav</template>
                </el-input>
                <el-button :icon="VideoPlay" circle @click="previewAudio(wrongItems[idx])" :disabled="!wrongItems[idx]?.trim()" title="试听" />
                <el-button type="danger" :icon="Delete" circle @click="removeItem('wrong', idx)" :disabled="wrongItems.length <= 1" />
              </div>
              <div v-if="generatingWrong === idx" class="generating-hint">
                <el-icon class="is-loading"><Loading /></el-icon>
                AI 正在生成...
              </div>
            </div>
            <div class="audio-actions">
              <el-button type="primary" plain @click="addItem('wrong')">+ 添加答错语音</el-button>
              <el-button type="warning" plain @click="aiGenerate('wrong')" :loading="generatingWrong !== null">
                <el-icon><MagicStick /></el-icon> AI 生成
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-divider>预览</el-divider>

        <el-form-item label="答对语音">
          <div class="preview-list">
            <el-tag v-for="item in correctItems.filter(i => i.trim())" :key="item" type="success" class="preview-tag" @click="previewAudio(item)">
              <el-icon><VideoPlay /></el-icon> {{ item }}
            </el-tag>
            <span v-if="!correctItems.some(i => i.trim())" class="empty-hint">暂无配置</span>
          </div>
        </el-form-item>
        <el-form-item label="答错语音">
          <div class="preview-list">
            <el-tag v-for="item in wrongItems.filter(i => i.trim())" :key="item" type="danger" class="preview-tag" @click="previewAudio(item)">
              <el-icon><VideoPlay /></el-icon> {{ item }}
            </el-tag>
            <span v-if="!wrongItems.some(i => i.trim())" class="empty-hint">暂无配置</span>
          </div>
        </el-form-item>
      </el-form>
    </section>

    <!-- 音频播放器弹窗 -->
    <el-dialog v-model="playerVisible" :title="'试听: ' + playerTitle" width="400px" append-to-body>
      <div class="audio-player-wrapper">
        <audio ref="audioPlayerRef" :src="playerUrl" controls autoplay style="width:100%" />
      </div>
      <div class="player-url">{{ playerUrl }}</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Refresh, Check, Delete, VideoPlay, Loading, MagicStick, CircleCheck, CircleClose } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getFeedbackAudioConfig, saveFeedbackAudioConfig, generateFeedbackAudio } from '@/api/request'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  baseUrl: '',
  correctList: '',
  wrongList: ''
})

const correctItems = ref<string[]>([''])
const wrongItems = ref<string[]>([''])

// AI 生成状态
const generatingCorrect = ref<number | null>(null)
const generatingWrong = ref<number | null>(null)

// 播放器
const playerVisible = ref(false)
const playerUrl = ref('')
const playerTitle = ref('')
const audioPlayerRef = ref<HTMLAudioElement | null>(null)

async function fetchData() {
  loading.value = true
  try {
    const res = await getFeedbackAudioConfig()
    if (res.code === 200 && res.data) {
      form.baseUrl = res.data.baseUrl || ''
      form.correctList = res.data.correctList || ''
      form.wrongList = res.data.wrongList || ''
      correctItems.value = form.correctList ? form.correctList.split(',').map(s => s.trim()) : ['']
      wrongItems.value = form.wrongList ? form.wrongList.split(',').map(s => s.trim()) : ['']
    }
  } finally {
    loading.value = false
  }
}

function addItem(type: 'correct' | 'wrong') {
  if (type === 'correct') correctItems.value.push('')
  else wrongItems.value.push('')
}

function removeItem(type: 'correct' | 'wrong', idx: number) {
  if (type === 'correct') correctItems.value.splice(idx, 1)
  else wrongItems.value.splice(idx, 1)
}

function previewAudio(name: string) {
  if (!name?.trim()) return
  const url = `${form.baseUrl.replace(/\/+$/, '')}/${name.trim()}.wav`
  playerUrl.value = url
  playerTitle.value = name.trim()
  playerVisible.value = true
}

async function aiGenerate(type: 'correct' | 'wrong') {
  const isCorrect = type === 'correct'
  const items = isCorrect ? correctItems : wrongItems
  const generatingRef = isCorrect ? generatingCorrect : generatingWrong

  // 找一个空位填入，或新增一行
  let targetIdx = items.value.findIndex(i => !i.trim())
  if (targetIdx === -1) {
    items.value.push('')
    targetIdx = items.value.length - 1
  }

  generatingRef.value = targetIdx

  try {
    const res = await generateFeedbackAudio({ type })
    if (res.code === 200 && res.data) {
      // 用生成的文字作为文件名（简化），实际用 audioUrl
      // 但我们存储的是文件名，需要从 URL 中提取
      const audioUrl = res.data.audioUrl
      const text = res.data.text

      // 从 URL 提取文件名（不含扩展名）
      const urlParts = audioUrl.split('/')
      const fileName = urlParts[urlParts.length - 1].replace('.wav', '')

      items.value[targetIdx] = fileName

      // 更新基础 URL（如果不同）
      const baseUrlFromUrl = urlParts.slice(0, -1).join('/')
      if (!form.baseUrl || form.baseUrl.includes('feedback')) {
        form.baseUrl = baseUrlFromUrl
      }

      ElMessage.success(`AI 生成成功: "${text}"`)

      // 自动试听
      setTimeout(() => previewAudio(fileName), 300)
    } else {
      ElMessage.error(res.msg || 'AI 生成失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || 'AI 生成失败')
  } finally {
    generatingRef.value = null
  }
}

async function handleSave() {
  saving.value = true
  try {
    const data = {
      baseUrl: form.baseUrl.trim(),
      correctList: correctItems.value.map(s => s.trim()).filter(Boolean).join(','),
      wrongList: wrongItems.value.map(s => s.trim()).filter(Boolean).join(','),
      aiMaxTokens: 800
    }
    const res = await saveFeedbackAudioConfig(data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      fetchData()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } finally {
    saving.value = false
  }
}

onMounted(fetchData)
</script>

<style scoped>
.feedback-audio-page {
  padding: 20px;
}
.page-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-bar h2 {
  margin: 0 0 4px 0;
  font-size: 18px;
}
.page-bar p {
  margin: 0;
  color: #909399;
  font-size: 13px;
}
.actions {
  display: flex;
  gap: 8px;
}
.config-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
}
.form-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.audio-list-editor {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.audio-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.audio-input-row {
  display: flex;
  gap: 8px;
  align-items: center;
}
.audio-actions {
  display: flex;
  gap: 8px;
  margin-top: 4px;
}
.generating-hint {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #e6a23c;
  font-size: 13px;
  padding-left: 12px;
}
.preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.preview-tag {
  cursor: pointer;
  transition: transform 0.2s;
}
.preview-tag:hover {
  transform: scale(1.05);
}
.empty-hint {
  color: #c0c4cc;
  font-size: 13px;
}
.audio-player-wrapper {
  margin-bottom: 12px;
}
.player-url {
  color: #909399;
  font-size: 12px;
  word-break: break-all;
}
</style>
