<template>
  <div class="login-container">
    <!-- 动态背景 -->
    <div class="background-animation">
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
    </div>
    
    <!-- 网格线背景 -->
    <div class="grid-background"></div>
    
    <!-- 动态圆圈效果 -->
    <div class="circles">
      <div v-for="i in 10" :key="i" class="circle"></div>
    </div>
    
    <div class="login-box">
      <div class="login-content">
        <div class="login-header">
          <div >
            <div class="logo-circle"></div>
            <h2 class="login-title">芒果数据中台</h2>
          </div>
          <p class="login-subtitle">MANGO DATA PLATFORM</p>
        </div>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          autocomplete="on"
          label-position="left"
        >
          <el-form-item prop="username">
            <el-input
              ref="usernameRef"
              v-model="loginForm.username"
              placeholder="用户名"
              type="text"
              tabindex="1"
              autocomplete="on"
              class="custom-input"
            >
              <template #prefix>
                <el-icon><User /></el-icon>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              ref="passwordRef"
              v-model="loginForm.password"
              :type="passwordVisible ? 'text' : 'password'"
              placeholder="密码"
              tabindex="2"
              autocomplete="on"
              @keyup.enter="handleLogin"
              class="custom-input"
            >
              <template #prefix>
                <el-icon><Lock /></el-icon>
              </template>
              <template #suffix>
                <el-icon class="cursor-pointer hover-effect" @click="passwordVisible = !passwordVisible">
                  <View v-if="passwordVisible" />
                  <Hide v-else />
                </el-icon>
              </template>
            </el-input>
          </el-form-item>

          <div class="remember-password">
            <el-checkbox v-model="loginForm.remember">记住密码</el-checkbox>
          </div>

          <el-button
            :loading="loading"
            type="primary"
            class="login-button"
            @click="handleLogin"
          >
            <span>登 录</span>
            <div class="button-effect"></div>
          </el-button>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {User, Lock, View, Hide} from '@element-plus/icons-vue'
import {login} from '@/api/user'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)
const passwordVisible = ref(false)

const loginForm = reactive({
  username: '',
  password: '',
  remember: false
})

const loginRules = {
  username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
  password: [{required: true, message: '请输入密码', trigger: 'blur'}]
}

// MD5加密函数实现
async function md5(message) {
  // 使用TextEncoder将字符串转换为Uint8Array
  const msgUint8 = new TextEncoder().encode(message);
  // 使用SubtleCrypto API计算哈希
  const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
  // 转换为十六进制字符串
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
  return hashHex;
}

const handleLogin = async () => {
  if (!loginFormRef.value) return

  try {
    await loginFormRef.value.validate()

    loading.value = true
    
    // 使用内置加密API加密密码
    const encryptedPassword = await md5(loginForm.password)
    
    // 创建新的登录参数，包含加密后的密码
    const loginParams = {
      username: loginForm.username,
      password: encryptedPassword
    }
    
    const res = await login(loginParams)
    
    // 保存令牌和用户信息
    localStorage.setItem('token', res.data.token)
    
    // 保存用户信息
    if (res.data && res.data.user) {
      localStorage.setItem('userInfo', JSON.stringify(res.data.user))
      console.log('保存用户信息:', res.data.user)
    }
    
    // 保存菜单和权限数据
    if (res.data && res.data.menus) {
      localStorage.setItem('menus', JSON.stringify(res.data.menus))
      console.log('保存菜单数据:', res.data.menus)
    }
    
    // 保存权限列表
    if (res.data && res.data.permissions) {
      localStorage.setItem('permissions', JSON.stringify(res.data.permissions))
      console.log('保存权限数据:', res.data.permissions)
    }
    
    // 处理记住密码
    if (loginForm.remember) {
      localStorage.setItem('rememberedUsername', loginForm.username)
      // 注意：出于安全考虑，不直接存储原始密码，而是存储加密后的密码
      localStorage.setItem('rememberedPassword', encryptedPassword)
      localStorage.setItem('rememberedLogin', 'true')
    } else {
      localStorage.removeItem('rememberedUsername')
      localStorage.removeItem('rememberedPassword')
      localStorage.removeItem('rememberedLogin')
    }
    
    router.push('/')
    ElMessage.success('登录成功')
  } catch (error) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}

// 检查是否有记住的登录信息
onMounted(() => {
  // 检查是否记住密码
  const rememberedLogin = localStorage.getItem('rememberedLogin')
  if (rememberedLogin === 'true') {
    loginForm.username = localStorage.getItem('rememberedUsername') || ''
    loginForm.password = localStorage.getItem('rememberedPassword') || ''
    loginForm.remember = true
  }
  
  animateCircles()
})

// 动态圆圈效果函数
const animateCircles = () => {
  const circles = document.querySelectorAll('.circle')
  circles.forEach((circle, index) => {
    // 随机位置和大小
    const size = Math.random() * 150 + 50 // 50px至200px
    const posX = Math.random() * window.innerWidth
    const posY = Math.random() * window.innerHeight
    const duration = Math.random() * 10 + 5 // 5s至15s
    const delay = Math.random() * 5 // 0s至5s延迟

    circle.style.width = `${size}px`
    circle.style.height = `${size}px`
    circle.style.left = `${posX}px`
    circle.style.top = `${posY}px`
    circle.style.animationDuration = `${duration}s`
    circle.style.animationDelay = `${delay}s`
    circle.style.opacity = Math.random() * 0.2 + 0.05 // 0.05至0.25的不透明度
  })
}
</script>

<style lang="scss" scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #e6f7ff 0%, #c6e6ff 50%, #b8e2ff 100%);
  
  /* 动画处理 */
  animation: backgroundPulse 15s ease-in-out infinite;
}

/* 背景动画效果 */
@keyframes backgroundPulse {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

/* 网格背景 */
.grid-background {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: 
    linear-gradient(rgba(64, 158, 255, 0.07) 1px, transparent 1px),
    linear-gradient(90deg, rgba(64, 158, 255, 0.07) 1px, transparent 1px);
  background-size: 50px 50px;
  perspective: 1000px;
  transform-style: preserve-3d;
  animation: gridMove 20s linear infinite;
}

@keyframes gridMove {
  0% {
    background-position: 0 0;
  }
  100% {
    background-position: 50px 50px;
  }
}

/* 动态浮动立方体 */
.background-animation {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.cube {
  position: absolute;
  background: rgba(64, 158, 255, 0.15);
  backdrop-filter: blur(5px);
  border: 1px solid rgba(64, 158, 255, 0.3);
  box-shadow: 0 0 30px rgba(64, 158, 255, 0.2);
  border-radius: 2px;
  animation: cubeFloat 25s ease-in-out infinite;
}

.cube:nth-child(1) {
  width: 150px;
  height: 150px;
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.cube:nth-child(2) {
  width: 100px;
  height: 100px;
  top: 70%;
  left: 80%;
  animation-delay: -5s;
}

.cube:nth-child(3) {
  width: 80px;
  height: 80px;
  top: 30%;
  left: 80%;
  animation-delay: -10s;
}

.cube:nth-child(4) {
  width: 120px;
  height: 120px;
  top: 90%;
  left: 30%;
  animation-delay: -15s;
}

.cube:nth-child(5) {
  width: 70px;
  height: 70px;
  top: 15%;
  left: 65%;
  animation-delay: -20s;
}

@keyframes cubeFloat {
  0%, 100% {
    transform: translateY(0) rotate(0deg) translateX(0);
  }
  25% {
    transform: translateY(-20px) rotate(5deg) translateX(10px);
  }
  50% {
    transform: translateY(10px) rotate(-5deg) translateX(-15px);
  }
  75% {
    transform: translateY(-15px) rotate(8deg) translateX(5px);
  }
}

/* 动态圆圈 */
.circles {
  position: absolute;
  width: 100%;
  height: 100%;
  top: 0;
  left: 0;
  overflow: hidden;
  z-index: 0;
}

.circle {
  position: absolute;
  border-radius: 50%;
  background: radial-gradient(
    circle at center,
    rgba(64, 158, 255, 0.3),
    rgba(64, 158, 255, 0) 70%
  );
  animation: pulseAndFloat 10s ease-in-out infinite;
}

@keyframes pulseAndFloat {
  0%, 100% {
    transform: translateY(0) scale(1);
  }
  50% {
    transform: translateY(-30px) scale(1.1);
    opacity: 0.2;
  }
}

/* 登录框 */
.login-box {
  position: relative;
  z-index: 10;
  width: 450px;
  overflow: hidden;
  border-radius: 12px;
  box-shadow: 
    0 8px 32px rgba(0, 91, 171, 0.2),
    0 0 0 1px rgba(64, 158, 255, 0.25);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(64, 158, 255, 0.3);
  transition: all 0.3s ease;
  
  &:hover {
    box-shadow: 
      0 15px 45px rgba(0, 91, 171, 0.25),
      0 0 0 1px rgba(64, 158, 255, 0.4);
  }
}

.login-content {
  padding: 40px;
}

.login-header {
  text-align: center;
  margin-bottom: 40px;
  background: none; /* 移除所有背景色 */
  padding: 0;
  border-radius: 0;
  box-shadow: none;
  border: none;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 15px;
  background: none; /* 移除背景 */
  padding: 0;
  border-radius: 0;
  border: none;
}

.logo-circle {
  width: 30px;
  height: 30px;
  background: linear-gradient(135deg, #56ccf2 0%, #2f80ed 100%); /* 明亮的蓝色渐变 */
  border-radius: 50%;
  margin-right: 10px;
  position: relative;
  box-shadow: 0 0 15px rgba(47, 128, 237, 0.5);
  animation: pulseLogo 2s ease-in-out infinite;
}

@keyframes pulseLogo {
  0%, 100% {
    transform: scale(1);
    box-shadow: 0 0 15px rgba(64, 158, 255, 0.5);
  }
  50% {
    transform: scale(1.05);
    box-shadow: 0 0 20px rgba(64, 158, 255, 0.8);
  }
}

.login-title {
  color: #0064c8; /* 深蓝色文字 */
  font-size: 30px;
  margin: 0;
  font-weight: 600;
  text-shadow: none;
}

.login-subtitle {
  color: #3498db; /* 明亮的蓝色 */
  font-size: 14px;
  letter-spacing: 2px;
  margin-top: 5px;
}

.login-form {
  margin-top: 30px;
}

.custom-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(64, 158, 255, 0.3);
  box-shadow: none;
  backdrop-filter: blur(5px);
  border-radius: 6px;
  padding: 12px 15px;
  transition: all 0.3s ease;
  
  &:hover, &:focus {
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid rgba(64, 158, 255, 0.5);
  }
  
  &.is-focus {
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  }
}

.custom-input :deep(input) {
  color: #333;
  height: 40px;
  
  &::placeholder {
    color: rgba(0, 91, 171, 0.5);
  }
}

.custom-input :deep(.el-input__prefix),
.custom-input :deep(.el-input__suffix) {
  color: rgba(0, 91, 171, 0.6);
}

.custom-input :deep(.el-input__prefix) {
  margin-right: 10px;
}

.login-button {
  width: 100%;
  margin-top: 30px;
  height: 50px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #409EFF 0%, #36CBCB 100%);
  border: none;
  border-radius: 8px;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 7px 20px rgba(64, 158, 255, 0.4);
    background: linear-gradient(135deg, #36CBCB 0%, #409EFF 100%);
  }
  
  &:active {
    transform: translateY(1px);
  }
}

.button-effect {
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255,255,255,0.3) 0%, rgba(255,255,255,0) 70%);
  opacity: 0;
  transition: all 0.5s ease-out;
}

.login-button:hover .button-effect {
  opacity: 0.5;
  animation: buttonGlow 2s ease-out infinite;
}

@keyframes buttonGlow {
  0% {
    transform: scale(0.5);
    opacity: 0;
  }
  50% {
    opacity: 0.3;
  }
  100% {
    transform: scale(2);
    opacity: 0;
  }
}

.hover-effect {
  transition: all 0.3s ease;
  
  &:hover {
    color: #fff;
    transform: scale(1.1);
  }
}

.remember-password {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 5px;
  margin-bottom: 5px;
  font-size: 14px;
  
  :deep(.el-checkbox__label) {
    color: #606266;
  }
  
  :deep(.el-checkbox__input.is-checked .el-checkbox__inner) {
    background-color: #409EFF;
    border-color: #409EFF;
  }
}

/* 响应式 */
@media (max-width: 576px) {
  .login-box {
    width: 90%;
    max-width: 450px;
  }
  
  .login-content {
    padding: 30px 20px;
  }
  
  .login-title {
    font-size: 24px;
  }
}
</style>
