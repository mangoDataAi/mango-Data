<template>
  <div class="json-editor" :style="{ height: height }">
    <div ref="editorContainer" class="editor-container"></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onBeforeUnmount, defineProps, defineEmits } from 'vue'
import JSONEditor from 'jsoneditor'
import 'jsoneditor/dist/jsoneditor.min.css'

const props = defineProps({
  modelValue: {
    type: [String, Object, Array],
    default: '{}'
  },
  readOnly: {
    type: Boolean,
    default: false
  },
  height: {
    type: String,
    default: '400px'
  },
  mode: {
    type: String,
    default: 'tree', // tree, code, form, text, view
    validator: (value) => ['tree', 'code', 'form', 'text', 'view'].includes(value)
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'error'])

const editorContainer = ref(null)
const editor = ref(null)

const initEditor = () => {
  const options = {
    mode: props.readOnly ? 'view' : props.mode,
    modes: ['tree', 'code', 'form', 'text', 'view'],
    onChangeJSON: onChangeJSON,
    onChangeText: onChangeText,
    onValidationError: onValidationError
  }

  if (editorContainer.value) {
    editor.value = new JSONEditor(editorContainer.value, options)
    setContent()
  }
}

const setContent = () => {
  if (!editor.value) return

  try {
    let content = props.modelValue
    
    if (typeof content === 'string') {
      try {
        // 如果是字符串，尝试解析为JSON对象
        content = JSON.parse(content)
      } catch (e) {
        // 如果解析失败，保持为字符串，编辑器会自行处理
      }
    }
    
    editor.value.set(content)
  } catch (e) {
    console.error('设置JSON内容失败:', e)
  }
}

const onChangeJSON = (json) => {
  try {
    emit('update:modelValue', JSON.stringify(json, null, 2))
    emit('change', json)
  } catch (e) {
    console.error('JSON变更处理失败:', e)
  }
}

const onChangeText = (jsonText) => {
  try {
    emit('update:modelValue', jsonText)
    emit('change', jsonText)
  } catch (e) {
    console.error('文本变更处理失败:', e)
  }
}

const onValidationError = (errors) => {
  emit('error', errors)
}

// 监听属性变化
watch(() => props.modelValue, (newValue) => {
  if (editor.value) {
    // 获取当前编辑器的内容
    let currentValue
    try {
      currentValue = JSON.stringify(editor.value.get())
    } catch (e) {
      // 如果无法获取JSON，可能是无效的JSON
      currentValue = ''
    }

    // 只有当值发生变化时才更新编辑器
    let newValueStr = typeof newValue === 'string' ? newValue : JSON.stringify(newValue)
    if (currentValue !== newValueStr) {
      setContent()
    }
  }
}, { deep: true })

watch(() => props.readOnly, (newValue) => {
  if (editor.value) {
    editor.value.setMode(newValue ? 'view' : props.mode)
  }
})

watch(() => props.mode, (newValue) => {
  if (editor.value && !props.readOnly) {
    editor.value.setMode(newValue)
  }
})

onMounted(() => {
  initEditor()
})

onBeforeUnmount(() => {
  if (editor.value) {
    editor.value.destroy()
    editor.value = null
  }
})
</script>

<style scoped>
.json-editor {
  width: 100%;
  overflow: hidden;
}

.editor-container {
  width: 100%;
  height: 100%;
}

/* 确保jsoneditor的内部容器正确显示 */
:deep(.jsoneditor) {
  height: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

:deep(.jsoneditor-menu) {
  background-color: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
}

:deep(.jsoneditor-search input) {
  height: 24px;
}
</style> 