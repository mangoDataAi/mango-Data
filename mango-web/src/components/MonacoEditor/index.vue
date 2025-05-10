<template>
  <div class="monaco-editor-container" :style="{ height }">
    <div ref="editorContainer" class="editor-instance"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, defineProps, defineEmits } from 'vue'
import * as monaco from 'monaco-editor'

const props = defineProps({
  code: {
    type: String,
    default: ''
  },
  language: {
    type: String,
    default: 'javascript'
  },
  theme: {
    type: String,
    default: 'vs-dark'
  },
  height: {
    type: String,
    default: '300px'
  },
  options: {
    type: Object,
    default: () => ({})
  }
})

const emit = defineEmits(['update:code', 'change', 'ready'])

const editorContainer = ref(null)
const editor = ref(null)

const initMonaco = () => {
  if (!editorContainer.value) return

  // 合并默认选项和用户提供的选项
  const defaultOptions = {
    value: props.code,
    language: props.language,
    theme: props.theme,
    automaticLayout: true,
    minimap: { enabled: true },
    scrollBeyondLastLine: false,
    lineNumbers: 'on',
    tabSize: 2,
    wordWrap: 'on'
  }

  const editorOptions = {
    ...defaultOptions,
    ...props.options
  }

  // 创建编辑器实例
  editor.value = monaco.editor.create(editorContainer.value, editorOptions)

  // 监听内容变更事件
  editor.value.onDidChangeModelContent(() => {
    const value = editor.value.getValue()
    emit('update:code', value)
    emit('change', value)
  })

  // 通知组件已准备就绪
  emit('ready', editor.value)
}

// 更新编辑器内容
const updateContent = () => {
  if (editor.value) {
    const currentValue = editor.value.getValue()
    if (props.code !== currentValue) {
      editor.value.setValue(props.code)
    }
  }
}

// 更新编辑器语言
const updateLanguage = () => {
  if (editor.value) {
    const model = editor.value.getModel()
    if (model) {
      monaco.editor.setModelLanguage(model, props.language)
    }
  }
}

// 更新编辑器主题
const updateTheme = () => {
  if (editor.value) {
    monaco.editor.setTheme(props.theme)
  }
}

// 更新编辑器选项
const updateOptions = () => {
  if (editor.value) {
    editor.value.updateOptions(props.options)
  }
}

// 监听属性变化
watch(() => props.code, updateContent)
watch(() => props.language, updateLanguage)
watch(() => props.theme, updateTheme)
watch(() => props.options, updateOptions, { deep: true })

// 组件挂载时初始化
onMounted(() => {
  initMonaco()
})

// 组件卸载前销毁编辑器实例
onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.dispose()
  }
})
</script>

<style scoped>
.monaco-editor-container {
  width: 100%;
  overflow: hidden;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

.editor-instance {
  width: 100%;
  height: 100%;
}
</style> 