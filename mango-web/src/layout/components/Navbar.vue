<template>
  <div class="navbar">
    <div class="hamburger-container">
      <svg
        :class="{'is-active': isCollapse}"
        class="hamburger"
        viewBox="0 0 1024 1024"
        xmlns="http://www.w3.org/2000/svg"
        width="24"
        height="24"
        @click="toggleSideBar"
      >
        <path d="M408 442h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8zm-8 204c0 4.4 3.6 8 8 8h480c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8H408c-4.4 0-8 3.6-8 8v56zm504-486H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8zm0 632H120c-4.4 0-8 3.6-8 8v56c0 4.4 3.6 8 8 8h784c4.4 0 8-3.6 8-8v-56c0-4.4-3.6-8-8-8z" />
      </svg>
    </div>

    <div class="logo-container">
      <span class="logo-text">芒果数据中台</span>
    </div>

    <div class="breadcrumb-container">
      <!-- 可以在这里添加面包屑导航 -->
    </div>

    <div class="right-menu">
      <div class="right-menu-item">
        <i class="el-icon-user"></i>
      </div>
      <div class="right-menu-item">
        <i class="el-icon-bell"></i>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const isCollapse = ref(false)

const toggleSideBar = () => {
  isCollapse.value = !isCollapse.value
  // 这里可以触发父组件的侧边栏折叠/展开事件
}
</script>

<style lang="scss" scoped>
.navbar {
  // ... 其他样式保持不变
  .hamburger-container {
    padding-left: 0;  // 移除汉堡菜单的左内边距
    position: relative;
    z-index: 1;
    transform-origin: center;
    
    svg {
      transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    
    &:hover svg {
      filter: drop-shadow(0 0 3px rgba(255, 255, 255, 0.9));
      transform: scale(1.15) rotate(6deg);
    }
    
    /* 添加点击波纹效果 */
    &:active::after {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      width: 5px;
      height: 5px;
      background: rgba(255, 255, 255, 0.7);
      opacity: 0.8;
      border-radius: 100%;
      transform: translate(-50%, -50%);
      animation: navRipple 0.5s cubic-bezier(0.23, 1, 0.32, 1);
    }
  }

  .logo-container {
    margin-left: -10px;  // logo 向左移动
    position: relative;
    overflow: hidden;
    
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: -100%;
      width: 100%;
      height: 100%;
      background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
      animation: logoScan 5s infinite;
    }
  }

  .breadcrumb-container {
    margin-left: -10px;  // 面包屑导航向左移动
    position: relative;
    z-index: 1;
    
    .no-redirect {
      color: #fff;
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
      transition: all 0.3s ease;
      
      &:hover {
        color: #fff;
        text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
        transform: translateY(-1px);
      }
    }
    
    .el-breadcrumb__separator, .el-breadcrumb__inner {
      color: rgba(255, 255, 255, 0.95);
      text-shadow: 0 1px 2px rgba(0, 0, 0, 0.3);
      transition: all 0.3s ease;
      
      &:hover {
        color: #fff;
        text-shadow: 0 0 5px rgba(255, 255, 255, 0.5);
        transform: translateY(-1px) scale(1.03);
      }
    }
  }

  .right-menu {
    position: relative;
    z-index: 1;
    
    &-item {
      position: relative;
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      overflow: hidden;
      
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(255, 255, 255, 0.1);
        transform: translateY(100%);
        transition: transform 0.3s ease;
      }
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.2);
        transform: translateY(-3px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
        
        &::before {
          transform: translateY(0);
        }
        
        i {
          animation: iconFloat 0.6s ease-in-out infinite alternate;
        }
      }
    }
  }

  // 添加渐变背景和悬浮光晕效果
  background: linear-gradient(90deg, rgba(24, 144, 255, 0.8) 0%, rgba(104, 174, 255, 0.8) 100%);
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: -100%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, transparent 70%);
    animation: rotate 15s linear infinite;
    pointer-events: none;
    z-index: 0;
  }
  
  /* 添加鼠标跟随光效 */
  &:hover::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: radial-gradient(
      circle at var(--mouse-x, 50%) var(--mouse-y, 50%),
      rgba(255, 255, 255, 0.15),
      transparent 150px
    );
    pointer-events: none;
    z-index: 0;
  }
}

@keyframes navRipple {
  0% {
    transform: translate(-50%, -50%) scale(0);
    opacity: 1;
  }
  100% {
    transform: translate(-50%, -50%) scale(15);
    opacity: 0;
  }
}

@keyframes logoScan {
  0%, 100% {
    left: -100%;
  }
  50% {
    left: 100%;
  }
}

@keyframes iconFloat {
  0% {
    transform: translateY(0);
  }
  100% {
    transform: translateY(-3px);
  }
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}
</style> 