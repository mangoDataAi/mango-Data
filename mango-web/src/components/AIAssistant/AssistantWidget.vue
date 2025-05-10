<template>
  <div class="ai-assistant-container" :class="{ 'is-minimized': isMinimized }">
    <!-- 悬浮按钮 -->
    <div 
      class="ai-assistant-toggle" 
      :class="{ 'is-dragging': isButtonDragging }"
      :style="isMinimized ? {
        left: buttonPosition.x + 'px',
        top: buttonPosition.y + 'px'
      } : {}"
      @mousedown="startButtonDrag"
      @click="handleButtonClick"
    >
      <div class="ai-assistant-toggle-icon">
        <div class="ai-assistant-pulse-ring"></div>
        <el-icon v-if="isMinimized"><ChatDotRound /></el-icon>
        <el-icon v-else><Close /></el-icon>
      </div>
    </div>

    <!-- 主体内容 -->
    <div 
      v-if="!isMinimized" 
      class="ai-assistant-wrapper"
      :class="{ 'is-expanded': isExpanded, 'is-dragging': isDragging }"
      :style="isExpanded ? {
        left: position.x + 'px',
        top: position.y + 'px',
        width: size.width + 'px',
        height: size.height + 'px'
      } : {}"
    >
      <div class="ai-assistant-body">
        <!-- 头部 -->
        <div class="ai-assistant-header" @mousedown="startDrag">
          <div class="drag-handle">
            <div class="drag-indicator">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
          <div class="ai-assistant-title">
            <el-icon><ChatDotRound /></el-icon>
            <span>AI助手</span>
            <span v-if="isDragging" class="debug-info">拖动中: {{ position.x }},{{ position.y }}</span>
          </div>
          <div class="ai-assistant-controls">
            <el-tooltip content="最小化" placement="top">
              <el-button type="text" @click.stop="minimizeAssistant">
                <el-icon><Minus /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="历史记录" placement="top">
              <el-button type="text" @click.stop="showHistory = !showHistory">
                <el-icon><Clock /></el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>

        <!-- 聊天区域 -->
        <div class="ai-assistant-content" v-if="!showHistory">
          <div class="ai-chat-container" ref="chatContainer">
            <div class="ai-chat-messages">
              <div v-for="(message, index) in chatHistory" :key="index" 
                  :class="['ai-message', message.type === 'user' ? 'ai-message-user' : 'ai-message-assistant']">
                <div class="ai-message-avatar">
                  <el-avatar :size="28" :src="message.type === 'user' ? userAvatar : assistantAvatar">
                    {{ message.type === 'user' ? 'U' : 'A' }}
                  </el-avatar>
                </div>
                <div class="ai-message-content">
                  <div class="ai-message-text">{{ message.content }}</div>
                  <div class="ai-message-time">{{ formatTime(message.timestamp) }}</div>
                </div>
              </div>
              <div v-if="isTyping" class="ai-message ai-message-assistant">
                <div class="ai-message-avatar">
                  <el-avatar :size="28" :src="assistantAvatar">A</el-avatar>
                </div>
                <div class="ai-message-content">
                  <div class="ai-typing-indicator">
                    <span></span>
                    <span></span>
                    <span></span>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 输入区域 -->
          <div class="ai-chat-input">
            <el-input
              v-model="userInput"
              type="textarea"
              :rows="1"
              placeholder="有什么可以帮你的？"
              resize="none"
              @keyup.enter.native="sendMessage"
            ></el-input>
            <el-button type="primary" :icon="Promotion" circle @click="sendMessage"></el-button>
          </div>
        </div>

        <!-- 历史记录 -->
        <div class="ai-assistant-history" v-if="showHistory">
          <div class="ai-history-header">
            <h3>对话历史</h3>
            <el-button type="text" @click="clearHistory">清空历史</el-button>
          </div>
          <el-timeline>
            <el-timeline-item
              v-for="(session, index) in chatSessions"
              :key="index"
              :timestamp="formatDate(session.date)"
              placement="top"
              :type="index === 0 ? 'primary' : ''"
            >
              <el-card class="ai-history-card" @click="loadSession(session)">
                <div class="ai-history-title">{{ session.title || '对话 ' + (chatSessions.length - index) }}</div>
                <div class="ai-history-preview">{{ session.preview }}</div>
              </el-card>
            </el-timeline-item>
          </el-timeline>
        </div>
      </div>
      
      <!-- 调整大小的手柄 -->
      <div class="resize-handle resize-se" @mousedown.stop="startResize"></div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { ChatDotRound, Close, Minus, Clock, Promotion } from '@element-plus/icons-vue';

// 状态变量
const isExpanded = ref(false);
const isMinimized = ref(true);
const showHistory = ref(false);
const isTyping = ref(false);
const userInput = ref('');
const chatContainer = ref(null);

// 位置和大小状态
const position = ref({ x: window.innerWidth - 420, y: 100 });
const size = ref({ width: 350, height: 500 });
const isDragging = ref(false);
const isResizing = ref(false);
const dragOffset = ref({ x: 0, y: 0 });

// 悬浮球按钮位置
const buttonPosition = ref({ x: window.innerWidth - 70, y: window.innerHeight - 70 });
const isButtonDragging = ref(false);

// 计算样式，使用transform而不是直接修改position
const getButtonStyle = () => {
  if (!isMinimized.value && !isButtonDragging.value) return {};
  
  return {
    transform: `translate3d(${buttonPosition.value.x}px, ${buttonPosition.value.y}px, 0)`,
    willChange: isButtonDragging.value ? 'transform' : 'auto'
  };
};

const getWrapperStyle = () => {
  if (!isExpanded.value) return {};
  
  return {
    transform: `translate3d(${position.value.x}px, ${position.value.y}px, 0)`,
    width: `${size.value.width}px`,
    height: `${size.value.height}px`,
    willChange: isDragging.value || isResizing.value ? 'transform, width, height' : 'auto'
  };
};

// 用户和助手头像
const userAvatar = ref('');
const assistantAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png');

// 对话历史
const chatHistory = ref([
  {
    type: 'assistant',
    content: '你好！我是你的AI助手，有什么可以帮助你的吗？',
    timestamp: new Date()
  }
]);

// 历史会话
const chatSessions = ref([
  {
    id: 1,
    title: '如何使用系统功能',
    preview: '关于如何使用系统功能的对话...',
    date: new Date(Date.now() - 86400000),
    messages: []
  },
  {
    id: 2,
    title: '数据导出问题',
    preview: '关于数据导出功能的咨询...',
    date: new Date(Date.now() - 172800000),
    messages: []
  }
]);

// 拖拽相关方法
const startDrag = (event) => {
  if (!isExpanded.value || isDragging.value || isResizing.value) {
    console.log('无法开始拖动', { 
      isExpanded: isExpanded.value, 
      isDragging: isDragging.value, 
      isResizing: isResizing.value 
    });
    return;
  }
  
  console.log('开始拖动', event.clientX, event.clientY);
  isDragging.value = true;
  
  // 计算鼠标与窗口左上角的偏移
  dragOffset.value = {
    x: event.clientX - position.value.x,
    y: event.clientY - position.value.y
  };
  
  // 添加全局事件监听
  window.addEventListener('mousemove', onDrag, { passive: false });
  window.addEventListener('mouseup', stopDrag);
  
  // 防止文本选择
  document.body.style.userSelect = 'none';
  document.body.style.touchAction = 'none';
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
};

let dragAnimationFrame = null;
const onDrag = (event) => {
  if (!isDragging.value) return;
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
  
  // 取消之前未执行的动画帧
  if (dragAnimationFrame) {
    cancelAnimationFrame(dragAnimationFrame);
  }
  
  // 使用requestAnimationFrame优化更新
  dragAnimationFrame = requestAnimationFrame(() => {
    // 更新位置
    position.value = {
      x: Math.max(0, Math.min(window.innerWidth - size.value.width, event.clientX - dragOffset.value.x)),
      y: Math.max(0, Math.min(window.innerHeight - size.value.height, event.clientY - dragOffset.value.y))
    };
  });
};

const stopDrag = (event) => {
  if (!isDragging.value) return;
  
  console.log('停止拖动');
  
  // 取消之前未执行的动画帧
  if (dragAnimationFrame) {
    cancelAnimationFrame(dragAnimationFrame);
    dragAnimationFrame = null;
  }
  
  isDragging.value = false;
  
  // 移除全局事件监听
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
  
  // 恢复文本选择
  document.body.style.userSelect = '';
  document.body.style.touchAction = '';
  
  // 阻止事件传播和默认行为
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
};

// 调整大小相关方法
const startResize = (event) => {
  if (!isExpanded.value || isDragging.value) return;
  
  console.log('开始调整大小');
  isResizing.value = true;
  
  // 记录当前鼠标位置
  dragOffset.value = {
    x: event.clientX,
    y: event.clientY
  };
  
  // 添加全局事件监听
  window.addEventListener('mousemove', onResize, { passive: false });
  window.addEventListener('mouseup', stopResize);
  
  // 防止文本选择
  document.body.style.userSelect = 'none';
  document.body.style.touchAction = 'none';
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
};

let resizeAnimationFrame = null;
const onResize = (event) => {
  if (!isResizing.value) return;
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
  
  // 取消之前未执行的动画帧
  if (resizeAnimationFrame) {
    cancelAnimationFrame(resizeAnimationFrame);
  }
  
  // 使用requestAnimationFrame优化更新
  resizeAnimationFrame = requestAnimationFrame(() => {
    // 计算大小差值
    const deltaX = event.clientX - dragOffset.value.x;
    const deltaY = event.clientY - dragOffset.value.y;
    
    // 更新大小，设置最小值
    size.value = {
      width: Math.max(300, size.value.width + deltaX),
      height: Math.max(400, size.value.height + deltaY)
    };
    
    // 更新鼠标位置
    dragOffset.value = {
      x: event.clientX,
      y: event.clientY
    };
  });
};

const stopResize = (event) => {
  if (!isResizing.value) return;
  
  console.log('停止调整大小');
  
  // 取消之前未执行的动画帧
  if (resizeAnimationFrame) {
    cancelAnimationFrame(resizeAnimationFrame);
    resizeAnimationFrame = null;
  }
  
  isResizing.value = false;
  
  // 移除全局事件监听
  window.removeEventListener('mousemove', onResize);
  window.removeEventListener('mouseup', stopResize);
  
  // 恢复文本选择
  document.body.style.userSelect = '';
  document.body.style.touchAction = '';
  
  // 阻止事件传播和默认行为
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
};

// 悬浮球拖拽相关方法
const startButtonDrag = (event) => {
  // 如果不是最小化状态，不处理
  if (!isMinimized.value || isButtonDragging.value) return;
  
  console.log('开始拖动悬浮球', event.clientX, event.clientY);
  isButtonDragging.value = true;
  
  // 计算鼠标与悬浮球左上角的偏移
  dragOffset.value = {
    x: event.clientX - buttonPosition.value.x,
    y: event.clientY - buttonPosition.value.y
  };
  
  // 添加全局事件监听
  window.addEventListener('mousemove', onButtonDrag, { passive: false });
  window.addEventListener('mouseup', stopButtonDrag);
  
  // 防止文本选择
  document.body.style.userSelect = 'none';
  document.body.style.touchAction = 'none'; // 移动端优化
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
};

let buttonAnimationFrame = null;
const onButtonDrag = (event) => {
  if (!isButtonDragging.value) return;
  
  // 阻止事件传播和默认行为
  event.stopPropagation();
  event.preventDefault();
  
  // 取消之前未执行的动画帧
  if (buttonAnimationFrame) {
    cancelAnimationFrame(buttonAnimationFrame);
  }
  
  // 使用requestAnimationFrame优化更新
  buttonAnimationFrame = requestAnimationFrame(() => {
    // 更新悬浮球位置
    const buttonSize = 50; // 悬浮球的尺寸
    buttonPosition.value = {
      x: Math.max(0, Math.min(window.innerWidth - buttonSize, event.clientX - dragOffset.value.x)),
      y: Math.max(0, Math.min(window.innerHeight - buttonSize, event.clientY - dragOffset.value.y))
    };
  });
};

const stopButtonDrag = (event) => {
  if (!isButtonDragging.value) return;
  
  console.log('停止拖动悬浮球');
  
  // 取消之前未执行的动画帧
  if (buttonAnimationFrame) {
    cancelAnimationFrame(buttonAnimationFrame);
    buttonAnimationFrame = null;
  }
  
  isButtonDragging.value = false;
  
  // 移除全局事件监听
  window.removeEventListener('mousemove', onButtonDrag);
  window.removeEventListener('mouseup', stopButtonDrag);
  
  // 恢复文本选择
  document.body.style.userSelect = '';
  document.body.style.touchAction = '';
  
  // 阻止事件传播和默认行为
  if (event) {
    event.stopPropagation();
    event.preventDefault();
  }
};

// 处理按钮点击，区分拖动和点击
const handleButtonClick = (event) => {
  if (isButtonDragging.value) {
    // 如果是拖动结束，不触发点击事件
    return;
  }
  
  // 触发原来的切换助手状态功能
  toggleAssistant();
};

// 方法
const toggleAssistant = () => {
  console.log('切换助手状态', { isMinimized: isMinimized.value, isExpanded: isExpanded.value });
  
  if (isMinimized.value) {
    // 从最小化状态展开时，将对话框位置设置在悬浮球附近
    position.value = {
      x: Math.max(0, Math.min(window.innerWidth - size.value.width, buttonPosition.value.x - size.value.width / 2)),
      y: Math.max(0, Math.min(window.innerHeight - size.value.height, buttonPosition.value.y - 50))
    };
    
    // 先设置不最小化，再设置展开
    isMinimized.value = false;
    setTimeout(() => {
      isExpanded.value = true;
    }, 50);
  } else {
    isExpanded.value = false;
    setTimeout(() => {
      isMinimized.value = true;
      showHistory.value = false;
    }, 300);
  }
};

const minimizeAssistant = () => {
  console.log('最小化助手');
  
  // 先设置不展开，触发收起动画
  isExpanded.value = false;
  
  // 延迟设置最小化状态和重置历史视图，确保动画完成
  setTimeout(() => {
    isMinimized.value = true;
    // 在组件完全隐藏后再重置历史视图状态
    setTimeout(() => {
      showHistory.value = false;
    }, 50);
  }, 300); // 确保动画时间与CSS过渡时间一致
};

const sendMessage = async () => {
  if (!userInput.value.trim()) return;
  
  // 添加用户消息
  const userMessage = {
    type: 'user',
    content: userInput.value,
    timestamp: new Date()
  };
  chatHistory.value.push(userMessage);
  userInput.value = '';
  
  // 滚动到底部
  await nextTick();
  scrollToBottom();
  
  // 模拟助手思考
  isTyping.value = true;
  
  // 模拟助手回复
  setTimeout(() => {
    isTyping.value = false;
    chatHistory.value.push({
      type: 'assistant',
      content: getAssistantResponse(userMessage.content),
      timestamp: new Date()
    });
    
    // 滚动到底部
    nextTick(() => {
      scrollToBottom();
    });
  }, 1000 + Math.random() * 1000);
};

const scrollToBottom = () => {
  if (chatContainer.value) {
    chatContainer.value.scrollTop = chatContainer.value.scrollHeight;
  }
};

const formatTime = (date) => {
  return new Date(date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
};

const formatDate = (date) => {
  return new Date(date).toLocaleDateString();
};

const clearHistory = () => {
  chatSessions.value = [];
};

const loadSession = (session) => {
  // 加载历史会话
  showHistory.value = false;
};

// 获取助手回复（简单回复逻辑）
const getAssistantResponse = (message) => {
  if (message.includes('你好') || message.includes('嗨') || message.includes('Hi')) {
    return '你好！有什么我可以帮助你的吗？';
  } else if (message.includes('时间') || message.includes('日期')) {
    return `现在的时间是 ${new Date().toLocaleString()}`;
  } else if (message.includes('谢谢') || message.includes('感谢')) {
    return '不客气，随时为您服务！';
  } else if (message.includes('帮助') || message.includes('怎么用')) {
    return '您可以询问我关于系统功能、数据操作或其他问题，我会尽力帮助您解决问题。';
  } else {
    return '我理解您的问题，请稍等，我正在为您处理...';
  }
};

// 监听聊天记录变化，自动滚动到底部
watch(chatHistory, () => {
  nextTick(() => {
    scrollToBottom();
  });
});

// 监听窗口大小变化，确保窗口和悬浮球在视口内
const adjustPositionToViewport = () => {
  // 调整对话框位置
  if (position.value.x + size.value.width > window.innerWidth) {
    position.value.x = Math.max(0, window.innerWidth - size.value.width);
  }
  if (position.value.y + size.value.height > window.innerHeight) {
    position.value.y = Math.max(0, window.innerHeight - size.value.height);
  }
  
  // 调整悬浮球位置
  const buttonSize = 50;
  if (buttonPosition.value.x + buttonSize > window.innerWidth) {
    buttonPosition.value.x = window.innerWidth - buttonSize;
  }
  if (buttonPosition.value.y + buttonSize > window.innerHeight) {
    buttonPosition.value.y = window.innerHeight - buttonSize;
  }
};

// 清理事件监听器和动画
onUnmounted(() => {
  window.removeEventListener('mousemove', onDrag);
  window.removeEventListener('mouseup', stopDrag);
  window.removeEventListener('mousemove', onResize);
  window.removeEventListener('mouseup', stopResize);
  window.removeEventListener('mousemove', onButtonDrag);
  window.removeEventListener('mouseup', stopButtonDrag);
  window.removeEventListener('resize', adjustPositionToViewport);
  
  // 清理可能的动画帧
  if (buttonAnimationFrame) cancelAnimationFrame(buttonAnimationFrame);
  if (dragAnimationFrame) cancelAnimationFrame(dragAnimationFrame);
  if (resizeAnimationFrame) cancelAnimationFrame(resizeAnimationFrame);
});

// 挂载时初始化
onMounted(() => {
  // 可以从用户信息中获取头像
  userAvatar.value = localStorage.getItem('userAvatar') || '';
  
  // 添加窗口调整事件监听
  window.addEventListener('resize', adjustPositionToViewport);
  
  // 打印初始状态
  console.log('AI助手初始化', { 
    isMinimized: isMinimized.value, 
    isExpanded: isExpanded.value,
    position: position.value,
    size: size.value,
    buttonPosition: buttonPosition.value
  });
});
</script>

<style lang="scss" scoped>
.ai-assistant-container {
  position: fixed;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
}

// 悬浮按钮
.ai-assistant-toggle {
  position: fixed;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #36D1DC);
  box-shadow: 0 3px 15px rgba(64, 158, 255, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
  z-index: 10000;
  
  &.is-dragging {
    transition: none !important;
    box-shadow: 0 5px 25px rgba(64, 158, 255, 0.6);
    cursor: grabbing;
  }
  
  &:hover:not(.is-dragging) {
    transform: translateY(-3px);
    box-shadow: 0 5px 20px rgba(64, 158, 255, 0.5);
  }
  
  .ai-assistant-toggle-icon {
    color: white;
    font-size: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: transform 0.3s ease;
    position: relative;
    
    .el-icon {
      z-index: 2;
    }
  }
  
  // 脉冲动画
  .ai-assistant-pulse-ring {
    position: absolute;
    width: 100%;
    height: 100%;
    border-radius: 50%;
    background-color: rgba(255, 255, 255, 0.4);
    z-index: 1;
    opacity: 0;
    transform: scale(0.5);
    animation: pulse 2s infinite;
  }
}

// 重新实现的包装器
.ai-assistant-wrapper {
  position: fixed;
  width: 0;
  height: 0;
  opacity: 0;
  bottom: 20px;
  right: 20px;
  z-index: 9999;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 6px 30px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: opacity 0.3s ease, width 0.3s ease, height 0.3s ease;
  
  &.is-expanded {
    opacity: 1;
  }
  
  &.is-dragging {
    transition: none !important;
    box-shadow: 0 8px 40px rgba(0, 0, 0, 0.2);
    cursor: grabbing;
  }

  &:hover .resize-handle {
    opacity: 1;
  }
}

// 主体部分
.ai-assistant-body {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// 头部样式
.ai-assistant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: linear-gradient(135deg, #409EFF, #36D1DC);
  color: white;
  cursor: grab;
  user-select: none; /* 防止文本选择 */
  position: relative;
  
  &:active {
    cursor: grabbing;
  }
  
  &:hover .drag-handle {
    opacity: 1;
  }
  
  .drag-handle {
    position: absolute;
    left: 0;
    top: 0;
    width: 100%;
    height: 100%;
    opacity: 0.5;
    transition: opacity 0.2s;
    
    .drag-indicator {
      position: absolute;
      left: 10px;
      top: 50%;
      transform: translateY(-50%);
      display: flex;
      flex-direction: column;
      gap: 2px;
      
      span {
        display: block;
        width: 20px;
        height: 2px;
        background-color: rgba(255, 255, 255, 0.7);
        border-radius: 1px;
      }
    }
  }
  
  .ai-assistant-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-weight: 600;
    font-size: 16px;
    position: relative;
    z-index: 2;
    
    .debug-info {
      font-size: 11px;
      opacity: 0.7;
      margin-left: 8px;
    }
  }
  
  .ai-assistant-controls {
    display: flex;
    align-items: center;
    gap: 5px;
    z-index: 2; /* 确保控件在提示上方 */
    position: relative;
    
    .el-button {
      color: white;
      
      &:hover {
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }
}

// 调整大小的控件
.resize-handle {
  position: absolute;
  width: 16px;
  height: 16px;
  background-color: #409EFF;
  border-radius: 50%;
  opacity: 0.7;
  transition: opacity 0.2s ease, transform 0.2s ease;
  z-index: 10;
  box-shadow: 0 0 5px rgba(0, 0, 0, 0.2);
  
  &:hover {
    transform: scale(1.2);
    opacity: 1;
  }
  
  &.resize-se {
    bottom: 5px;
    right: 5px;
    cursor: se-resize;
    
    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 7px;
      height: 7px;
      border-right: 2px solid white;
      border-bottom: 2px solid white;
    }
  }
}

// 聊天区域
.ai-assistant-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.ai-chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 15px;
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: #f0f2f5;
    border-radius: 3px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: #d0d7de;
    border-radius: 3px;
  }
}

.ai-chat-messages {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.ai-message {
  display: flex;
  gap: 10px;
  max-width: 90%;
  
  .ai-message-avatar {
    flex-shrink: 0;
  }
  
  .ai-message-content {
    padding: 10px 15px;
    border-radius: 10px;
    position: relative;
    
    .ai-message-text {
      font-size: 14px;
      line-height: 1.5;
      word-break: break-word;
    }
    
    .ai-message-time {
      font-size: 11px;
      margin-top: 5px;
      opacity: 0.6;
    }
  }
  
  // 用户消息
  &.ai-message-user {
    align-self: flex-end;
    
    .ai-message-content {
      background: #ecf5ff;
      color: #333;
      border-bottom-right-radius: 2px;
    }
  }
  
  // 助手消息
  &.ai-message-assistant {
    align-self: flex-start;
    
    .ai-message-content {
      background: #f4f4f5;
      color: #333;
      border-bottom-left-radius: 2px;
    }
  }
}

// 输入框区域
.ai-chat-input {
  padding: 15px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  
  .el-input {
    flex: 1;
  }
  
  .el-button {
    flex-shrink: 0;
  }
}

// 历史记录
.ai-assistant-history {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  
  .ai-history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    
    h3 {
      margin: 0;
    }
  }
  
  .ai-history-card {
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }
    
    .ai-history-title {
      font-weight: bold;
      margin-bottom: 5px;
    }
    
    .ai-history-preview {
      font-size: 13px;
      color: #606266;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

// 打字指示器
.ai-typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  
  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    background-color: #999;
    border-radius: 50%;
    animation: ai-typing 1.2s infinite ease-in-out;
    
    &:nth-child(1) {
      animation-delay: 0s;
    }
    
    &:nth-child(2) {
      animation-delay: 0.2s;
    }
    
    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }
}

// 动画
@keyframes pulse {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  50% {
    opacity: 0.4;
  }
  100% {
    transform: scale(1.2);
    opacity: 0;
  }
}

@keyframes ai-typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-5px);
  }
}

// 响应式调整
@media (max-width: 768px) {
  .ai-assistant-wrapper {
    max-width: calc(100vw - 40px) !important;
  }
}

// 暗黑模式支持
:root[data-theme='dark'] {
  .ai-assistant-body,
  .ai-assistant-wrapper {
    background: #1e1e1e;
  }
  
  .ai-chat-container {
    &::-webkit-scrollbar-track {
      background: #2c2c2c;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #4a4a4a;
    }
  }
  
  .ai-message {
    &.ai-message-user .ai-message-content {
      background: #1a4b91;
      color: #e6e6e6;
    }
    
    &.ai-message-assistant .ai-message-content {
      background: #333333;
      color: #e6e6e6;
    }
  }
  
  .ai-chat-input {
    border-top: 1px solid #3a3a3a;
  }
  
  .ai-assistant-history .ai-history-card .ai-history-preview {
    color: #a0a0a0;
  }
}

/* 添加更丝滑的过渡动画 */
@keyframes smooth-appear {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
</style> 