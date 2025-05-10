<template>
  <div class="json-editor-container" :style="{ height: height }">
    <div ref="jsonEditorRef" class="json-editor"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import JSONEditor from 'jsoneditor'
import 'jsoneditor/dist/jsoneditor.min.css'

const props = defineProps({
  modelValue: {
    type: [Object, Array, String],
    default: () => ({})
  },
  height: {
    type: String,
    default: '200px'
  },
  mode: {
    type: String,
    default: 'tree' // 'tree', 'view', 'form', 'code', 'text'
  },
  readOnly: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'change', 'error'])

const jsonEditorRef = ref(null)
let jsonEditor = null

// 初始化编辑器
onMounted(() => {
  if (!jsonEditorRef.value) return

  // 解析JSON字符串
  let json = props.modelValue
  if (typeof json === 'string') {
    try {
      json = JSON.parse(json)
    } catch (e) {
      // 如果解析失败，仍然使用原始字符串
      json = props.modelValue
    }
  }

  // JSONEditor配置
  const options = {
    mode: props.mode,
    modes: ['tree', 'view', 'form', 'code', 'text'],
    readOnly: props.readOnly,
    onChange: () => {
      try {
        const value = jsonEditor.get()
        emit('update:modelValue', value)
        emit('change', value)
      } catch (e) {
        emit('error', e)
      }
    },
    onModeChange: (newMode) => {
      console.log('JSONEditor mode changed:', newMode)
    },
    onError: (error) => {
      emit('error', error)
    }
  }

  // 创建编辑器实例
  jsonEditor = new JSONEditor(jsonEditorRef.value, options)
  
  // 设置初始值
  jsonEditor.set(json)
})

// 监听传入值变化
watch(
  () => props.modelValue,
  (newValue) => {
    if (!jsonEditor) return
    
    // 解析JSON字符串
    let json = newValue
    if (typeof json === 'string') {
      try {
        json = JSON.parse(json)
      } catch (e) {
        // 如果解析失败，仍然使用原始字符串
        json = newValue
      }
    }
    
    // 更新编辑器内容（如果与当前内容不同）
    const currentValue = jsonEditor.get()
    if (JSON.stringify(currentValue) !== JSON.stringify(json)) {
      jsonEditor.set(json)
    }
  }
)

// 清理编辑器实例
onBeforeUnmount(() => {
  if (jsonEditor) {
    jsonEditor.destroy()
    jsonEditor = null
  }
})
</script>

<style scoped>
.json-editor-container {
  position: relative;
  width: 100%;
  overflow: hidden;
}

.json-editor {
  width: 100%;
  height: 100%;
}

:deep(.jsoneditor) {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
}

:deep(.jsoneditor-menu) {
  background-color: #409eff;
  border-bottom: 1px solid #dcdfe6;
}
</style> 