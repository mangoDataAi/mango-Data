<template>
  <div class="assistant-widget-container" :class="{ 'is-expanded': isExpanded, 'is-minimized': isMinimized }">
    <!-- 悬浮按钮 -->
    <div class="assistant-toggle" @click="toggleAssistant">
      <div class="assistant-toggle-icon">
        <div class="pulse-ring"></div>
        <el-icon v-if="isMinimized"><ChatDotRound /></el-icon>
        <el-icon v-else><Close /></el-icon>
      </div>
    </div>

    <!-- 主体内容 -->
    <div class="assistant-body">
      <!-- 头部 -->
      <div class="assistant-header">
        <div class="assistant-title">
          <el-icon><ChatDotRound /></el-icon>
          <span>智能助手</span>
        </div>
        <div class="assistant-controls">
          <el-tooltip content="最小化" placement="top">
            <el-button type="text" @click="minimizeAssistant">
              <el-icon><Minus /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="历史记录" placement="top">
            <el-button type="text" @click="showHistory = !showHistory">
              <el-icon><Clock /></el-icon>
            </el-button>
          </el-tooltip>
          <el-tooltip content="设置" placement="top">
            <el-button type="text" @click="showSettings = !showSettings">
              <el-icon><Setting /></el-icon>
            </el-button>
          </el-tooltip>
        </div>
      </div>

      <!-- 聊天区域 -->
      <div class="assistant-content" v-if="!showHistory && !showSettings">
        <div class="chat-container" ref="chatContainer">
          <div class="chat-messages">
            <div v-for="(message, index) in chatHistory" :key="index" 
                 :class="['message', message.type === 'user' ? 'message-user' : 'message-assistant']">
              <div class="message-avatar">
                <el-avatar :size="28" :src="message.type === 'user' ? userAvatar : assistantAvatar">
                  {{ message.type === 'user' ? 'U' : 'A' }}
                </el-avatar>
              </div>
              <div class="message-content">
                <div class="message-text">{{ message.content }}</div>
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>
            <div v-if="isTyping" class="message message-assistant">
              <div class="message-avatar">
                <el-avatar :size="28" :src="assistantAvatar">A</el-avatar>
              </div>
              <div class="message-content">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 输入区域 -->
        <div class="chat-input">
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
      <div class="assistant-history" v-if="showHistory">
        <div class="history-header">
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
            <el-card class="history-card" @click="loadSession(session)">
              <div class="history-title">{{ session.title || '对话 ' + (chatSessions.length - index) }}</div>
              <div class="history-preview">{{ session.preview }}</div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>

      <!-- 设置面板 -->
      <div class="assistant-settings" v-if="showSettings">
        <h3>助手设置</h3>
        <el-form label-position="top">
          <el-form-item label="助手名称">
            <el-input v-model="settings.assistantName" placeholder="智能助手"></el-input>
          </el-form-item>
          <el-form-item label="界面主题">
            <el-select v-model="settings.theme" style="width: 100%">
              <el-option label="明亮" value="light"></el-option>
              <el-option label="暗黑" value="dark"></el-option>
              <el-option label="自动" value="auto"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="通知提醒">
            <el-switch v-model="settings.notifications"></el-switch>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveSettings">保存设置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, watch } from 'vue';
import { ChatDotRound, Close, Minus, Clock, Setting, Promotion } from '@element-plus/icons-vue';

// 状态变量
const isExpanded = ref(false);
const isMinimized = ref(true);
const showHistory = ref(false);
const showSettings = ref(false);
const isTyping = ref(false);
const userInput = ref('');
const chatContainer = ref(null);

// 用户和助手头像
const userAvatar = ref('');
const assistantAvatar = ref('https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png');

// 对话历史
const chatHistory = ref([
  {
    type: 'assistant',
    content: '你好！我是你的智能助手，有什么可以帮助你的吗？',
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

// 设置
const settings = ref({
  assistantName: '智能助手',
  theme: 'light',
  notifications: true
});

// 方法
const toggleAssistant = () => {
  if (isMinimized.value) {
    isMinimized.value = false;
    setTimeout(() => {
      isExpanded.value = true;
    }, 100);
  } else {
    isExpanded.value = !isExpanded.value;
  }
};

const minimizeAssistant = () => {
  isExpanded.value = false;
  setTimeout(() => {
    isMinimized.value = true;
    showHistory.value = false;
    showSettings.value = false;
  }, 300);
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

const saveSettings = () => {
  // 保存设置
  showSettings.value = false;
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

// 挂载时初始化
onMounted(() => {
  // 可以从用户信息中获取头像
  userAvatar.value = localStorage.getItem('userAvatar') || '';
  
  // 初始化其他配置
});
</script>

<style lang="scss" scoped>
.assistant-widget-container {
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
.assistant-toggle {
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
  position: relative;
  overflow: hidden;
  
  &:hover {
    transform: translateY(-3px);
    box-shadow: 0 5px 20px rgba(64, 158, 255, 0.5);
  }
  
  .assistant-toggle-icon {
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
  .pulse-ring {
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

// 主体部分
.assistant-body {
  width: 0;
  height: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
  margin-bottom: 10px;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  opacity: 0;
  transform: translateY(10px) scale(0.9);
  display: flex;
  flex-direction: column;
}

// 展开状态
.is-expanded {
  .assistant-body {
    width: 350px;
    height: 500px;
    opacity: 1;
    transform: translateY(0) scale(1);
  }
  
  .assistant-toggle {
    background: #f56c6c;
    transform: rotate(180deg);
    
    &:hover {
      background: #e64242;
    }
    
    .pulse-ring {
      animation: none;
    }
  }
}

// 头部样式
.assistant-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: linear-gradient(135deg, #409EFF, #36D1DC);
  color: white;
  border-top-left-radius: 12px;
  border-top-right-radius: 12px;
  
  .assistant-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-weight: bold;
    font-size: 16px;
    
    .el-icon {
      font-size: 20px;
    }
  }
  
  .assistant-controls {
    display: flex;
    gap: 8px;
    
    .el-button {
      color: white;
      
      &:hover {
        background: rgba(255, 255, 255, 0.2);
      }
    }
  }
}

// 聊天区域
.assistant-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chat-container {
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

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  display: flex;
  gap: 10px;
  max-width: 90%;
  
  .message-avatar {
    flex-shrink: 0;
  }
  
  .message-content {
    padding: 10px 15px;
    border-radius: 10px;
    position: relative;
    
    .message-text {
      font-size: 14px;
      line-height: 1.5;
      word-break: break-word;
    }
    
    .message-time {
      font-size: 11px;
      margin-top: 5px;
      opacity: 0.6;
    }
  }
  
  // 用户消息
  &.message-user {
    align-self: flex-end;
    
    .message-content {
      background: #ecf5ff;
      color: #333;
      border-bottom-right-radius: 2px;
    }
  }
  
  // 助手消息
  &.message-assistant {
    align-self: flex-start;
    
    .message-content {
      background: #f4f4f5;
      color: #333;
      border-bottom-left-radius: 2px;
    }
  }
}

// 输入框区域
.chat-input {
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
.assistant-history {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  
  .history-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    
    h3 {
      margin: 0;
    }
  }
  
  .history-card {
    cursor: pointer;
    transition: all 0.3s ease;
    
    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      transform: translateY(-2px);
    }
    
    .history-title {
      font-weight: bold;
      margin-bottom: 5px;
    }
    
    .history-preview {
      font-size: 13px;
      color: #606266;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

// 设置面板
.assistant-settings {
  flex: 1;
  padding: 15px;
  overflow-y: auto;
  
  h3 {
    margin-top: 0;
    margin-bottom: 20px;
  }
  
  .el-form-item {
    margin-bottom: 20px;
  }
}

// 打字指示器
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  
  span {
    display: inline-block;
    width: 8px;
    height: 8px;
    background-color: #999;
    border-radius: 50%;
    animation: typing 1.2s infinite ease-in-out;
    
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
    transform: scale(1.5);
    opacity: 0;
  }
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
  }
  30% {
    transform: translateY(-5px);
  }
}

// 响应式调整
@media (max-width: 768px) {
  .is-expanded .assistant-body {
    width: calc(100vw - 40px);
    max-width: 350px;
  }
}

// 暗黑模式支持
:root[data-theme='dark'] {
  .assistant-body {
    background: #1e1e1e;
  }
  
  .chat-container {
    &::-webkit-scrollbar-track {
      background: #2c2c2c;
    }
    
    &::-webkit-scrollbar-thumb {
      background: #4a4a4a;
    }
  }
  
  .message {
    &.message-user .message-content {
      background: #1a4b91;
      color: #e6e6e6;
    }
    
    &.message-assistant .message-content {
      background: #333333;
      color: #e6e6e6;
    }
  }
  
  .chat-input {
    border-top: 1px solid #3a3a3a;
  }
  
  .assistant-history .history-card .history-preview {
    color: #a0a0a0;
  }
}
</style> 