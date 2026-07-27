<template>
  <el-card class="admin-crud-page">
    <template #header>
      <div style="display:flex;align-items:center;gap:12px">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="goTo('subject')" style="cursor:pointer">
            <span :style="{ color: state.level === 'subject' ? '#FF6B6B' : '', fontWeight: state.level === 'subject' ? '600' : '' }">闯关管理</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="state.level === 'level'" @click="goTo('level')" style="cursor:pointer">
            <span :style="{ color: state.level === 'level' ? '#FF6B6B' : '', fontWeight: state.level === 'level' ? '600' : '' }">{{ state.subject?.subjectName }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-button v-if="state.level === 'subject'" link type="primary" @click="goTo('grade')" style="margin-left:auto">年级管理</el-button>
        <el-button v-if="state.level === 'grade'" link type="primary" @click="goTo('subject')" style="margin-left:auto">返回闯关管理</el-button>
        <el-button v-if="state.level === 'level'" link type="primary" @click="goTo('subject')" style="margin-left:auto">返回学科列表</el-button>
      </div>
    </template>

    <SubjectPanel
      v-if="state.level === 'subject'"
      v-model:ageGroup="state.ageGroup"
      @select="onSubjectSelect"
    />
    <GradePanel
      v-else-if="state.level === 'grade'"
    />
    <LevelPanel
      v-else-if="state.level === 'level'"
      :subject="state.subject"
    />
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import SubjectPanel from './panels/SubjectPanel.vue'
import GradePanel from './panels/GradePanel.vue'
import LevelPanel from './panels/LevelPanel.vue'

type DrillLevel = 'subject' | 'grade' | 'level'

const state = reactive({
  level: 'subject' as DrillLevel,
  ageGroup: null as number | null,
  subject: null as any,
})

function onSubjectSelect(row: any) {
  state.subject = row
  state.level = 'level'
}

function goTo(target: DrillLevel) {
  if (target === 'subject') {
    state.subject = null
  } else if (target === 'grade') {
    state.subject = null
  }
  state.level = target
}
</script>
