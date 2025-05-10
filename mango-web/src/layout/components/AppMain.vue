<template>
  <section class="app-main">
    <!-- 添加鼠标追踪效果 -->
    <div class="cursor-effect" ref="cursorEffect"></div>
    
    <router-view v-slot="{ Component }">
      <transition name="fade-transform" mode="out-in">
        <component :is="Component" />
      </transition>
    </router-view>
  </section>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

// 鼠标跟踪效果
const cursorEffect = ref(null);
let animationFrame = null;
let mouseX = 0;
let mouseY = 0;
let contentElements = [];

// 追踪鼠标位置
const trackMouse = (e) => {
  mouseX = e.clientX;
  mouseY = e.clientY;
  
  // 更新CSS变量，用于hover效果
  document.documentElement.style.setProperty('--mouse-x', `${mouseX}px`);
  document.documentElement.style.setProperty('--mouse-y', `${mouseY}px`);
  
  if (cursorEffect.value) {
    // 鼠标跟随效果平滑动画
    cursorEffect.value.style.left = `${mouseX}px`;
    cursorEffect.value.style.top = `${mouseY}px`;
  }
  
  // 为附近元素添加悬浮效果
  contentElements.forEach(el => {
    const rect = el.getBoundingClientRect();
    const elCenterX = rect.left + rect.width / 2;
    const elCenterY = rect.top + rect.height / 2;
    
    // 计算鼠标和元素中心的距离
    const distance = Math.sqrt(
      Math.pow(mouseX - elCenterX, 2) +
      Math.pow(mouseY - elCenterY, 2)
    );
    
    // 最大影响距离
    const maxDistance = 300;
    
    if (distance < maxDistance) {
      // 距离越近，效果越强
      const factor = 1 - distance / maxDistance;
      const scale = 1 + factor * 0.05;
      const translateY = -factor * 5;
      
      el.style.transform = `translateY(${translateY}px) scale(${scale})`;
      el.style.boxShadow = `0 ${5 + factor * 10}px ${10 + factor * 15}px rgba(0, 0, 0, ${0.1 + factor * 0.1})`;
      el.style.zIndex = '1';
    } else {
      el.style.transform = '';
      el.style.boxShadow = '';
      el.style.zIndex = '';
    }
  });
};

// 初始化互动元素
const initInteractiveElements = () => {
  // 获取所有卡片和按钮等可交互元素
  contentElements = Array.from(document.querySelectorAll('.el-card, .el-button:not(.is-text, .is-link), .interactive-item'));
  
  // 添加鼠标移动事件监听
  document.addEventListener('mousemove', trackMouse);
};

// 在DOM挂载后初始化
onMounted(() => {
  setTimeout(() => {
    initInteractiveElements();
  }, 1000);
});

// 清理事件监听
onUnmounted(() => {
  document.removeEventListener('mousemove', trackMouse);
  if (animationFrame) {
    cancelAnimationFrame(animationFrame);
  }
});
</script>

<style lang="scss" scoped>
/* 页面转场动画 */
.fade-transform-enter-active,
.fade-transform-leave-active {
  transition: all 0.5s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(-30px);
}

/* 鼠标跟随效果 */
.cursor-effect {
  position: fixed;
  top: 0;
  left: 0;
  width: 300px;
  height: 300px;
  pointer-events: none;
  z-index: -1;
  opacity: 0.5;
  transform: translate(-50%, -50%);
  background: radial-gradient(
    circle at center,
    rgba(64, 158, 255, 0.15),
    rgba(64, 158, 255, 0.05) 40%,
    transparent 60%
  );
  filter: blur(10px);
  transition: width 0.3s, height 0.3s, opacity 0.3s;
}

:deep(.el-card) {
  transition: all 0.4s cubic-bezier(0.215, 0.61, 0.355, 1);
  border-radius: 8px;
  overflow: hidden;
  border: none;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  position: relative;
  z-index: 0;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(
      800px circle at var(--mouse-x, 0) var(--mouse-y, 0),
      rgba(64, 158, 255, 0.1),
      transparent 40%
    );
    opacity: 0;
    z-index: -1;
    transition: opacity 0.3s ease;
  }
  
  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.1);
    
    &::before {
      opacity: 1;
    }
    
    .el-card__header {
      background: linear-gradient(to right, #409EFF, #1890ff);
      color: white;
      
      &::after {
        opacity: 1;
        animation: headerGlow 1.5s ease-in-out infinite alternate;
      }
    }
  }
  
  .el-card__header {
    transition: all 0.3s ease;
    border-bottom: none;
    position: relative;
    
    &::after {
      content: '';
      position: absolute;
      bottom: 0;
      left: 0;
      width: 100%;
      height: 1px;
      background: linear-gradient(to right, transparent, rgba(64, 158, 255, 0.5), transparent);
      opacity: 0.6;
      transition: opacity 0.3s;
    }
  }
}

@keyframes headerGlow {
  0% {
    opacity: 0.6;
    box-shadow: 0 3px 10px rgba(64, 158, 255, 0.2);
  }
  100% {
    opacity: 1;
    box-shadow: 0 3px 15px rgba(64, 158, 255, 0.4);
  }
}

/* 按钮悬浮效果 */
:deep(.el-button:not(.is-text, .is-link)) {
  overflow: hidden;
  position: relative;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  z-index: 1;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: linear-gradient(
      135deg,
      transparent 0%,
      rgba(255, 255, 255, 0.1) 50%,
      transparent 100%
    );
    transform: translateX(-100%) skewX(-25deg);
    transition: transform 0.5s ease;
    z-index: -1;
  }
  
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 0;
    height: 100%;
    background-color: rgba(255, 255, 255, 0.2);
    transition: all 0.3s ease;
    z-index: -1;
  }
  
  &:hover {
    transform: translateY(-3px) scale(1.02);
    box-shadow: 0 7px 14px rgba(0, 0, 0, 0.15),
                0 3px 6px rgba(0, 0, 0, 0.1);
    
    &::before {
      transform: translateX(100%) skewX(-25deg);
    }
    
    &::after {
      width: 100%;
    }
  }
  
  &:active {
    transform: translateY(-1px) scale(1.01);
    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
  }
}

/* 添加点击波纹 */
:deep(.el-button--primary:not(.is-text, .is-link)) {
  &::after {
    background-color: rgba(255, 255, 255, 0.3);
  }
  
  &:active::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 10px;
    height: 10px;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 50%;
    transform: translate(-50%, -50%) scale(1);
    opacity: 0.8;
    animation: buttonRipple 0.6s cubic-bezier(0.23, 1, 0.32, 1);
  }
}

@keyframes buttonRipple {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 0.8;
  }
  100% {
    transform: translate(-50%, -50%) scale(20);
    opacity: 0;
  }
}
</style> 