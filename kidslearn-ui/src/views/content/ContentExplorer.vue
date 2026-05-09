<template>
  <el-card>
    <template #header>
      <div style="display:flex;align-items:center;gap:12px">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item @click="goToLevel('subject')" style="cursor:pointer">
            <span :style="{ color: state.level === 'subject' ? '#FF6B6B' : '', fontWeight: state.level === 'subject' ? '600' : '' }">闯关管理</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="state.level !== 'subject' && state.level !== 'grade'" @click="goToLevel('course')" style="cursor:pointer">
            <span :style="{ color: state.level === 'course' ? '#FF6B6B' : '', fontWeight: state.level === 'course' ? '600' : '' }">{{ state.subject?.subjectName }}</span>
          </el-breadcrumb-item>
          <el-breadcrumb-item v-if="state.level === 'level'" @click="goToLevel('level')" style="cursor:pointer">
            <span :style="{ color: state.level === 'level' ? '#FF6B6B' : '', fontWeight: state.level === 'level' ? '600' : '' }">{{ state.course?.courseName }}</span>
          </el-breadcrumb-item>
        </el-breadcrumb>
        <el-button v-if="state.level === 'subject'" link type="primary" @click="goToLevel('grade')" style="margin-left:auto">年级管理</el-button>
        <el-button v-if="state.level === 'grade'" link type="primary" @click="goToLevel('subject')" style="margin-left:auto">返回闯关管理</el-button>
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
    <CoursePanel
      v-else-if="state.level === 'course'"
      :subject="state.subject"
      :ageGroup="state.ageGroup"
      @select="onCourseSelect"
    />
    <LevelPanel
      v-else-if="state.level === 'level'"
      :course="state.course"
    />
  </el-card>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import SubjectPanel from './panels/SubjectPanel.vue'
import GradePanel from './panels/GradePanel.vue'
import CoursePanel from './panels/CoursePanel.vue'
import LevelPanel from './panels/LevelPanel.vue'

type DrillLevel = 'subject' | 'grade' | 'course' | 'level'

const state = reactive({
  level: 'subject' as DrillLevel,
  ageGroup: null as number | null,
  subject: null as any,
  course: null as any,
  courseLevel: null as any,
})

function onSubjectSelect(row: any) {
  state.subject = row
  state.level = 'course'
}

function onCourseSelect(row: any) {
  state.course = row
  state.level = 'level'
}

function goToLevel(target: DrillLevel) {
  if (target === 'subject') {
    state.subject = null
    state.course = null
    state.courseLevel = null
  } else if (target === 'grade') {
    state.subject = null
    state.course = null
    state.courseLevel = null
  } else if (target === 'course') {
    state.course = null
    state.courseLevel = null
  } else if (target === 'level') {
    state.courseLevel = null
  }
  state.level = target
}
</script>
