<template>
  <div class="personal-info-container">
    <!-- 个人资料卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="profile-card" shadow="hover">
          <div class="profile-header">
            <div class="avatar-wrapper">
              <el-avatar
                :size="120"
                :src="userInfo.avatar"
                @error="handleAvatarError"
              >
                {{ userInfo.nickname?.substring(0, 1)?.toUpperCase() }}
              </el-avatar>
              <div class="avatar-mask" @click="handleChangeAvatar">
                <el-icon :size="24"><Camera /></el-icon>
                <span>更换头像</span>
              </div>
            </div>
            <h2 class="nickname">{{ userInfo.nickname }}</h2>
            <div class="role-tag">
              <el-tag effect="dark" type="success">{{ userInfo.role || '系统管理员' }}</el-tag>
            </div>
          </div>
          <div class="profile-stats">
            <div class="stat-item">
              <div class="stat-value">{{ userInfo.loginCount || 0 }}</div>
              <div class="stat-label">登录次数</div>
            </div>
            <div class="stat-divider"></div>
            <div class="stat-item">
              <div class="stat-value">{{ userInfo.operationCount || 0 }}</div>
              <div class="stat-label">操作次数</div>
            </div>
          </div>
          <div class="profile-info">
            <div class="info-item">
              <el-icon><User /></el-icon>
              <span>{{ userInfo.username }}</span>
            </div>
            <div class="info-item">
              <el-icon><Message /></el-icon>
              <span>{{ userInfo.email || '未设置邮箱' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Iphone /></el-icon>
              <span>{{ userInfo.phone || '未绑定手机' }}</span>
            </div>
            <div class="info-item">
              <el-icon><Clock /></el-icon>
              <span>{{ userInfo.createTime || '2024-01-01' }}</span>
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="18">
        <el-card class="tab-card">
          <template #header>
            <el-tabs v-model="activeTab">
              <el-tab-pane label="基本资料" name="basic">
                <template #label>
                  <div class="custom-tab">
                    <el-icon><UserFilled /></el-icon>
                    <span>基本资料</span>
                  </div>
                </template>
              </el-tab-pane>
              <el-tab-pane label="安全设置" name="security">
                <template #label>
                  <div class="custom-tab">
                    <el-icon><Lock /></el-icon>
                    <span>安全设置</span>
                  </div>
                </template>
              </el-tab-pane>
              <el-tab-pane label="操作日志" name="logs">
                <template #label>
                  <div class="custom-tab">
                    <el-icon><List /></el-icon>
                    <span>操作日志</span>
                  </div>
                </template>
              </el-tab-pane>
            </el-tabs>
          </template>

          <!-- 基本资料表单 -->
          <el-form
            v-if="activeTab === 'basic'"
            ref="basicFormRef"
            :model="basicForm"
            :rules="basicRules"
            label-width="100px"
            class="basic-form"
          >
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="用户昵称" prop="nickname">
                  <el-input v-model="basicForm.nickname" placeholder="请输入用户昵称" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="用户性别" prop="gender">
                  <el-radio-group v-model="basicForm.gender">
                    <el-radio label="male">男</el-radio>
                    <el-radio label="female">女</el-radio>
                    <el-radio label="secret">保密</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号码" prop="phone">
                  <el-input v-model="basicForm.phone" placeholder="请输入手机号码">
                    <template #append>
                      <el-button type="primary" link @click="handleBindPhone">
                        {{ basicForm.phone ? '更换' : '绑定' }}
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="邮箱" prop="email">
                  <el-input v-model="basicForm.email" placeholder="请输入邮箱">
                    <template #append>
                      <el-button type="primary" link @click="handleBindEmail">
                        {{ basicForm.email ? '更换' : '绑定' }}
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="个人简介" prop="bio">
              <el-input
                v-model="basicForm.bio"
                type="textarea"
                :rows="4"
                placeholder="请输入个人简介"
                show-word-limit
                maxlength="200"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleUpdateBasic">保存修改</el-button>
              <el-button @click="resetBasicForm">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 安全设置 -->
          <div v-if="activeTab === 'security'" class="security-container">
            <!-- 安全评分 -->
            <div class="security-score">
              <el-row :gutter="20">
                <el-col :span="8">
                  <div class="score-circle">
                    <el-progress
                      type="dashboard"
                      :percentage="securityScore"
                      :color="securityScoreColors"
                    >
                      <template #default="{ percentage }">
                        <div class="score-content">
                          <div class="score-value">{{ percentage }}</div>
                          <div class="score-label">安全评分</div>
                        </div>
                      </template>
                    </el-progress>
                  </div>
                </el-col>
                <el-col :span="16">
                  <div class="score-tips">
                    <h3>安全建议</h3>
                    <div class="tips-list">
                      <div v-for="(tip, index) in securityTips" :key="index" class="tip-item">
                        <el-icon :class="tip.level"><component :is="tip.icon" /></el-icon>
                        <span>{{ tip.content }}</span>
                      </div>
                    </div>
                  </div>
                </el-col>
              </el-row>
            </div>

            <!-- 安全项目列表 -->
            <div class="security-list">
              <!-- 登录密码 -->
              <div class="security-item">
                <div class="item-info">
                  <div class="item-icon">
                    <el-icon :size="24"><Key /></el-icon>
                  </div>
                  <div class="item-main">
                    <div class="item-title">登录密码</div>
                    <div class="item-desc">建议您定期更换密码，设置安全性高的密码可以使账号更安全</div>
                  </div>
                </div>
                <div class="item-action">
                  <el-button type="primary" @click="handleChangePassword">修改密码</el-button>
                </div>
              </div>

              <!-- 手机绑定 -->
              <div class="security-item">
                <div class="item-info">
                  <div class="item-icon">
                    <el-icon :size="24"><Iphone /></el-icon>
                  </div>
                  <div class="item-main">
                    <div class="item-title">手机绑定</div>
                    <div class="item-desc">
                      已绑定手机：{{ userInfo.phone ? userInfo.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2') : '未绑定' }}
                    </div>
                  </div>
                </div>
                <div class="item-action">
                  <el-button type="primary" @click="handleBindPhone">
                    {{ userInfo.phone ? '更换手机' : '立即绑定' }}
                  </el-button>
                </div>
              </div>

              <!-- 邮箱绑定 -->
              <div class="security-item">
                <div class="item-info">
                  <div class="item-icon">
                    <el-icon :size="24"><Message /></el-icon>
                  </div>
                  <div class="item-main">
                    <div class="item-title">邮箱绑定</div>
                    <div class="item-desc">
                      已绑定邮箱：{{ userInfo.email ? userInfo.email.replace(/(.{2}).+(@.+)/, '$1****$2') : '未绑定' }}
                    </div>
                  </div>
                </div>
                <div class="item-action">
                  <el-button type="primary" @click="handleBindEmail">
                    {{ userInfo.email ? '更换邮箱' : '立即绑定' }}
                  </el-button>
                </div>
              </div>

              <!-- 登录设备 -->
              <div class="security-item">
                <div class="item-info">
                  <div class="item-icon">
                    <el-icon :size="24"><Monitor /></el-icon>
                  </div>
                  <div class="item-main">
                    <div class="item-title">登录设备</div>
                    <div class="item-desc">已登录 {{ loginDevices.length }} 台设备，建议定期检查登录设备</div>
                  </div>
                </div>
                <div class="item-action">
                  <el-button type="primary" @click="handleViewDevices">查看设备</el-button>
                </div>
              </div>
            </div>

            <!-- 修改密码弹窗 -->
            <el-dialog
              v-model="passwordDialog"
              title="修改密码"
              width="500px"
              :destroy-on-close="true"
            >
              <el-form
                ref="passwordFormRef"
                :model="passwordForm"
                :rules="passwordRules"
                label-width="100px"
              >
                <el-form-item label="当前密码" prop="oldPassword">
                  <el-input
                    v-model="passwordForm.oldPassword"
                    type="password"
                    show-password
                    placeholder="请输入当前密码"
                  />
                </el-form-item>
                <el-form-item label="新密码" prop="newPassword">
                  <el-input
                    v-model="passwordForm.newPassword"
                    type="password"
                    show-password
                    placeholder="请输入新密码"
                  />
                </el-form-item>
                <el-form-item label="确认新密码" prop="confirmPassword">
                  <el-input
                    v-model="passwordForm.confirmPassword"
                    type="password"
                    show-password
                    placeholder="请再次输入新密码"
                  />
                </el-form-item>
              </el-form>
              <template #footer>
                <div class="dialog-footer">
                  <el-button @click="passwordDialog = false">取消</el-button>
                  <el-button type="primary" @click="handlePasswordSubmit">
                    确认修改
                  </el-button>
                </div>
              </template>
            </el-dialog>

            <!-- 登录设备弹窗 -->
            <el-dialog
              v-model="devicesDialog"
              title="登录设备"
              width="700px"
              :destroy-on-close="true"
            >
              <el-table :data="loginDevices" style="width: 100%">
                <el-table-column prop="device" label="设备信息" min-width="200">
                  <template #default="{ row }">
                    <div class="device-info">
                      <el-icon :size="20"><component :is="row.icon" /></el-icon>
                      <span>{{ row.device }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="location" label="登录地点" />
                <el-table-column prop="time" label="登录时间" width="180" />
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 'current' ? 'success' : 'info'">
                      {{ row.status === 'current' ? '当前设备' : '其他设备' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.status !== 'current'"
                      type="danger"
                      link
                      @click="handleLogoutDevice(row)"
                    >
                      退出登录
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-dialog>
          </div>

          <!-- 操作日志 -->
          <div v-if="activeTab === 'logs'" class="logs-container">
            <!-- 日志统计 -->
            <el-row :gutter="20" class="log-stats">
              <el-col :span="6" v-for="(stat, index) in logStats" :key="index">
                <el-card shadow="hover" class="stat-card">
                  <div class="stat-content">
                    <div class="stat-icon" :class="stat.type">
                      <el-icon :size="32"><component :is="stat.icon" /></el-icon>
                    </div>
                    <div class="stat-info">
                      <div class="stat-value">{{ stat.value }}</div>
                      <div class="stat-label">{{ stat.label }}</div>
                    </div>
                  </div>
                </el-card>
              </el-col>
            </el-row>

            <!-- 日志图表 -->
            <el-card class="log-chart-card">
              <template #header>
                <div class="card-header">
                  <span>操作趋势</span>
                  <el-radio-group v-model="logChartRange" size="small" @change="handleLogChartRangeChange">
                    <el-radio-button label="week">近一周</el-radio-button>
                    <el-radio-button label="month">近一月</el-radio-button>
                  </el-radio-group>
                </div>
              </template>
              <div class="log-chart" ref="logChartRef"></div>
            </el-card>

            <!-- 日志列表 -->
            <el-card class="log-list-card">
              <template #header>
                <div class="card-header">
                  <div class="header-left">
                    <span>操作记录</span>
                    <el-tag type="info" effect="plain">
                      共 {{ total }} 条记录
                    </el-tag>
                  </div>
                  <div class="header-right">
                    <el-button :icon="Refresh" circle @click="refreshLogs" />
                  </div>
                </div>
              </template>

              <!-- 搜索表单 -->
              <el-form :model="logSearchForm" class="search-form">
                <el-row :gutter="16">
                  <el-col :span="8">
                    <el-form-item label="操作类型">
                      <el-select v-model="logSearchForm.type" placeholder="请选择操作类型" clearable>
                        <el-option label="登录" value="LOGIN" />
                        <el-option label="查询" value="SELECT" />
                        <el-option label="新增" value="INSERT" />
                        <el-option label="修改" value="UPDATE" />
                        <el-option label="删除" value="DELETE" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="操作状态">
                      <el-select v-model="logSearchForm.status" placeholder="请选择操作状态" clearable>
                        <el-option label="成功" value="1" />
                        <el-option label="失败" value="0" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="操作时间">
                      <el-date-picker
                        v-model="logSearchForm.timeRange"
                        type="datetimerange"
                        range-separator="至"
                        start-placeholder="开始时间"
                        end-placeholder="结束时间"
                        value-format="YYYY-MM-DD HH:mm:ss"
                        :shortcuts="dateShortcuts"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
                <el-row>
                  <el-col :span="24" class="search-buttons">
                    <el-button type="primary" @click="handleLogSearch">
                      <el-icon><Search /></el-icon> 搜索
                    </el-button>
                    <el-button @click="resetLogSearch">
                      <el-icon><Refresh /></el-icon> 重置
                    </el-button>
                  </el-col>
                </el-row>
              </el-form>

              <!-- 日志表格 -->
              <el-table
                ref="logTableRef"
                v-loading="logLoading"
                :data="logList"
                style="width: 100%"
                :max-height="400"
              >
                <el-table-column type="index" label="序号" width="60" />
                <el-table-column prop="type" label="操作类型" width="100">
                  <template #default="{ row }">
                    <el-tag :type="getOperationTypeTag(row.type)">
                      {{ getOperationTypeLabel(row.type) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="module" label="操作模块" width="120" />
                <el-table-column prop="description" label="操作描述" min-width="200" show-overflow-tooltip />
                <el-table-column prop="ip" label="操作IP" width="120" show-overflow-tooltip />
                <el-table-column prop="location" label="操作地点" width="150" show-overflow-tooltip />
                <el-table-column prop="browser" label="浏览器" width="120" show-overflow-tooltip />
                <el-table-column prop="os" label="操作系统" width="120" show-overflow-tooltip />
                <el-table-column prop="status" label="状态" width="80" align="center">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
                      {{ row.status === 1 ? '成功' : '失败' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="duration" label="耗时" width="100" align="right">
                  <template #default="{ row }">
                    <span :class="getDurationClass(row.duration)">
                      {{ row.duration }}ms
                    </span>
                  </template>
                </el-table-column>
                <el-table-column prop="createTime" label="操作时间" width="180" show-overflow-tooltip />
                <el-table-column label="操作" width="80" fixed="right">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="handleViewLog(row)">
                      详情
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 分页 -->
              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="logPage.current"
                  v-model:page-size="logPage.size"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="total"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleLogSizeChange"
                  @current-change="handleLogCurrentChange"
                />
              </div>
            </el-card>

            <!-- 日志详情抽屉 -->
            <el-drawer
              v-model="logDetailDrawer"
              title="操作日志详情"
              size="800px"
              :destroy-on-close="true"
            >
              <div class="log-detail-container">
                <!-- 基本信息 -->
                <el-descriptions :column="2" border>
                  <el-descriptions-item label="操作类型">
                    <el-tag :type="getOperationTypeTag(currentLog?.type)">
                      {{ getOperationTypeLabel(currentLog?.type) }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="操作状态">
                    <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'">
                      {{ currentLog?.status === 1 ? '成功' : '失败' }}
                    </el-tag>
                  </el-descriptions-item>
                  <el-descriptions-item label="操作模块">
                    {{ currentLog?.module }}
                  </el-descriptions-item>
                  <el-descriptions-item label="操作描述">
                    {{ currentLog?.description }}
                  </el-descriptions-item>
                  <el-descriptions-item label="操作IP">
                    {{ currentLog?.ip }}
                  </el-descriptions-item>
                  <el-descriptions-item label="操作地点">
                    <el-link type="primary" :underline="false" @click="handleViewLogMap">
                      <el-icon><Location /></el-icon>
                      {{ currentLog?.location }}
                    </el-link>
                  </el-descriptions-item>
                  <el-descriptions-item label="浏览器">
                    {{ currentLog?.browser }}
                  </el-descriptions-item>
                  <el-descriptions-item label="操作系统">
                    {{ currentLog?.os }}
                  </el-descriptions-item>
                  <el-descriptions-item label="操作时间">
                    {{ currentLog?.createTime }}
                  </el-descriptions-item>
                  <el-descriptions-item label="耗时">
                    <span :class="getDurationClass(currentLog?.duration)">
                      {{ currentLog?.duration }}ms
                    </span>
                  </el-descriptions-item>
                </el-descriptions>

                <!-- 请求信息 -->
                <el-card class="detail-card">
                  <template #header>
                    <div class="card-header">
                      <span>请求信息</span>
                      <el-tag type="info">{{ currentLog?.method || 'GET' }}</el-tag>
                    </div>
                  </template>
                  <div class="code-block">
                    <div class="code-header">
                      <span>Request URL</span>
                      <el-button type="primary" link @click="copyCode(currentLog?.url)">
                        复制
                      </el-button>
                    </div>
                    <pre class="code-content">{{ currentLog?.url }}</pre>
                  </div>
                  <div class="code-block">
                    <div class="code-header">
                      <span>Request Data</span>
                      <el-button type="primary" link @click="copyCode(currentLog?.params)">
                        复制
                      </el-button>
                    </div>
                    <pre class="code-content">{{ formatJson(currentLog?.params) }}</pre>
                  </div>
                </el-card>

                <!-- 响应信息 -->
                <el-card class="detail-card">
                  <template #header>
                    <div class="card-header">
                      <span>响应信息</span>
                      <el-tag :type="currentLog?.status === 1 ? 'success' : 'danger'">
                        {{ currentLog?.status === 1 ? '200 OK' : '500 Error' }}
                      </el-tag>
                    </div>
                  </template>
                  <div class="code-block">
                    <div class="code-header">
                      <span>Response Data</span>
                      <el-button type="primary" link @click="copyCode(currentLog?.result)">
                        复制
                      </el-button>
                    </div>
                    <pre class="code-content" :class="{ 'error-content': currentLog?.status === 0 }">
                      {{ formatJson(currentLog?.result) }}
                    </pre>
                  </div>
                </el-card>

                <!-- 异常信息 -->
                <el-card v-if="currentLog?.status === 0" class="detail-card">
                  <template #header>
                    <div class="card-header">
                      <span>异常信息</span>
                      <el-button type="danger" link @click="copyCode(currentLog?.errorInfo)">
                        复制堆栈
                      </el-button>
                    </div>
                  </template>
                  <div class="error-stack">
                    <pre>{{ currentLog?.errorInfo }}</pre>
                  </div>
                </el-card>
              </div>
            </el-drawer>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 头像裁剪弹窗 -->
    <el-dialog
      v-model="avatarDialog"
      title="更换头像"
      width="600px"
      :destroy-on-close="true"
    >
      <div class="avatar-upload">
        <el-upload
          class="upload-container"
          action="#"
          :auto-upload="false"
          :show-file-list="false"
          accept="image/*"
          @change="handleAvatarUpload"
        >
          <template #trigger>
            <el-button type="primary">选择图片</el-button>
          </template>
          <template #tip>
            <div class="el-upload__tip">
              只能上传 jpg/png 文件，且不超过 2MB
            </div>
          </template>
        </el-upload>
        <div class="preview-container" v-if="previewUrl">
          <!-- 这里可以集成图片裁剪组件 -->
          <img :src="previewUrl" alt="预览" />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="avatarDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSaveAvatar" :loading="uploading">
            确认更换
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import {
  User, UserFilled, Lock, List, Message,
  Iphone, Clock, Camera, Key, Monitor,
  Search, Warning, CircleCheck, InfoFilled, Location
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'

// 用户信息
const userInfo = reactive({
  username: 'admin',
  nickname: '管理员',
  avatar: '',
  role: '系统管理员',
  email: 'admin@example.com',
  phone: '13800138000',
  createTime: '2024-01-01',
  loginCount: 128,
  operationCount: 1024
})

// 头像相关
const avatarDialog = ref(false)
const previewUrl = ref('')
const uploading = ref(false)

const handleAvatarError = () => {
  // 头像加载失败时的处理
}

const handleChangeAvatar = () => {
  avatarDialog.value = true
}

const handleAvatarUpload = (file) => {
  const isImage = file.raw.type.startsWith('image/')
  const isLt2M = file.raw.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
    return
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB！')
    return
  }

  // 预览图片
  previewUrl.value = URL.createObjectURL(file.raw)
}

const handleSaveAvatar = async () => {
  uploading.value = true
  try {
    // 调用上传接口
    await new Promise(resolve => setTimeout(resolve, 1000))
    ElMessage.success('头像更新成功')
    avatarDialog.value = false
  } catch (error) {
    ElMessage.error('头像更新失败')
  } finally {
    uploading.value = false
  }
}

// 标签页
const activeTab = ref('basic')

// 基本资料表单
const basicFormRef = ref(null)
const basicForm = reactive({
  nickname: userInfo.nickname,
  gender: 'secret',
  phone: userInfo.phone,
  email: userInfo.email,
  bio: ''
})

// 表单校验规则
const basicRules = {
  nickname: [
    { required: true, message: '请输入用户昵称', trigger: 'blur' },
    { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 绑定手机号
const handleBindPhone = () => {
  // TODO: 实现手机号绑定逻辑
}

// 绑定邮箱
const handleBindEmail = () => {
  // TODO: 实现邮箱绑定逻辑
}

// 更新基本资料
const handleUpdateBasic = () => {
  basicFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        // 调用更新接口
        await new Promise(resolve => setTimeout(resolve, 1000))
        ElMessage.success('基本资料更新成功')
      } catch (error) {
        ElMessage.error('基本资料更新失败')
      }
    }
  })
}

// 重置表单
const resetBasicForm = () => {
  basicFormRef.value?.resetFields()
}

// 安全评分相关
const securityScore = ref(85)
const securityScoreColors = [
  { color: '#f56c6c', percentage: 20 },
  { color: '#e6a23c', percentage: 40 },
  { color: '#5cb87a', percentage: 60 },
  { color: '#1989fa', percentage: 80 },
  { color: '#67c23a', percentage: 100 }
]

const securityTips = ref([
  {
    icon: 'Warning',
    level: 'warning',
    content: '建议开启两步验证，提高账号安全性'
  },
  {
    icon: 'CircleCheck',
    level: 'success',
    content: '已绑定手机号，可用于安全验证'
  },
  {
    icon: 'InfoFilled',
    level: 'info',
    content: '密码强度良好，建议定期更换密码'
  }
])

// 密码修改相关
const passwordDialog = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validatePass = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请输入密码'))
  } else {
    if (passwordForm.confirmPassword !== '') {
      passwordFormRef.value?.validateField('confirmPassword')
    }
    callback()
  }
}

const validatePass2 = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, validator: validatePass, trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validatePass2, trigger: 'blur' }
  ]
}

// 登录设备相关
const devicesDialog = ref(false)
const loginDevices = ref([
  {
    id: 1,
    device: 'Chrome 120.0.0 / Windows 10',
    icon: 'Monitor',
    location: '广东省深圳市',
    time: '2024-01-15 10:00:00',
    status: 'current'
  },
  {
    id: 2,
    device: 'Safari / iOS 17.2',
    icon: 'Iphone',
    location: '广东省广州市',
    time: '2024-01-14 14:30:00',
    status: 'other'
  }
])

const handleChangePassword = () => {
  passwordDialog.value = true
}

const handlePasswordSubmit = () => {
  passwordFormRef.value?.validate(async (valid) => {
    if (valid) {
      try {
        // 调用修改密码接口
        await new Promise(resolve => setTimeout(resolve, 1000))
        ElMessage.success('密码修改成功')
        passwordDialog.value = false
      } catch (error) {
        ElMessage.error('密码修改失败')
      }
    }
  })
}

const handleViewDevices = () => {
  devicesDialog.value = true
}

const handleLogoutDevice = (device) => {
  ElMessageBox.confirm(
    '确定要退出该设备的登录状态吗？',
    '退出确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      // 调用退出登录接口
      await new Promise(resolve => setTimeout(resolve, 1000))
      ElMessage.success('设备已退出登录')
      loginDevices.value = loginDevices.value.filter(item => item.id !== device.id)
    } catch (error) {
      ElMessage.error('退出失败')
    }
  })
}

// 日志统计数据
const logStats = ref([
  {
    label: '总操作次数',
    value: '1,234',
    icon: 'Histogram',
    type: 'primary'
  },
  {
    label: '成功操作',
    value: '1,180',
    icon: 'CircleCheck',
    type: 'success'
  },
  {
    label: '异常操作',
    value: '54',
    icon: 'Warning',
    type: 'danger'
  },
  {
    label: '平均耗时',
    value: '128ms',
    icon: 'Timer',
    type: 'warning'
  }
])

// 日志图表相关
const logChartRef = ref(null)
const logChartRange = ref('week')
let logChart = null

// 日志搜索相关
const logSearchForm = ref({
  type: '',
  status: '',
  timeRange: []
})

// 日志列表相关
const logTableRef = ref(null)
const logLoading = ref(false)
const logList = ref([])
const logPage = ref({
  current: 1,
  size: 10
})
const total = ref(0)

// 日志详情相关
const logDetailDrawer = ref(false)
const currentLog = ref(null)

// 初始化日志图表
const initLogChart = () => {
  if (!logChartRef.value) return
  
  logChart = echarts.init(logChartRef.value)
  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: ['成功操作', '异常操作']
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    },
    yAxis: {
      type: 'value'
    },
    series: [
      {
        name: '成功操作',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#67c23a'
        },
        data: [120, 132, 101, 134, 90, 230, 210]
      },
      {
        name: '异常操作',
        type: 'bar',
        stack: 'total',
        emphasis: {
          focus: 'series'
        },
        itemStyle: {
          color: '#f56c6c'
        },
        data: [12, 13, 10, 13, 9, 23, 21]
      }
    ]
  }
  logChart.setOption(option)
}

// 图表范围变更
const handleLogChartRangeChange = () => {
  // 更新图表数据
  updateLogChart()
}

// 更新图表数据
const updateLogChart = () => {
  // 根据时间范围获取数据
  const data = logChartRange.value === 'week'
    ? {
        xAxis: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
        success: [120, 132, 101, 134, 90, 230, 210],
        error: [12, 13, 10, 13, 9, 23, 21]
      }
    : {
        xAxis: ['1日', '5日', '10日', '15日', '20日', '25日', '30日'],
        success: [320, 332, 301, 334, 390, 330, 320],
        error: [32, 33, 30, 33, 39, 33, 32]
      }

  logChart?.setOption({
    xAxis: {
      data: data.xAxis
    },
    series: [
      {
        name: '成功操作',
        data: data.success
      },
      {
        name: '异常操作',
        data: data.error
      }
    ]
  })
}

// 搜索方法
const handleLogSearch = () => {
  logPage.value.current = 1
  fetchLogList()
}

const resetLogSearch = () => {
  logSearchForm.value = {
    type: '',
    status: '',
    timeRange: []
  }
  handleLogSearch()
}

// 分页方法
const handleLogSizeChange = (val) => {
  logPage.value.size = val
  fetchLogList()
}

const handleLogCurrentChange = (val) => {
  logPage.value.current = val
  fetchLogList()
}

// 获取日志列表
const fetchLogList = () => {
  logLoading.value = true
  // 模拟接口请求
  setTimeout(() => {
    logList.value = [
      {
        id: 1,
        type: 'SELECT',
        module: '用户管理',
        description: '查询用户列表',
        ip: '192.168.1.1',
        location: '广东省深圳市',
        browser: 'Chrome',
        os: 'Windows 10',
        status: 1,
        duration: 56,
        createTime: '2024-01-15 10:00:00',
        method: 'GET',
        url: '/api/system/user/list',
        params: {
          pageNum: 1,
          pageSize: 10
        },
        result: {
          code: 200,
          msg: '操作成功',
          data: null
        }
      }
    ]
    total.value = 100
    logLoading.value = false
  }, 500)
}

// 查看日志详情
const handleViewLog = (row) => {
  currentLog.value = row
  logDetailDrawer.value = true
}

// 查看地图
const handleViewLogMap = () => {
  if (!currentLog.value?.location) return
  mapDialog.value = true
  // 这里复用之前的地图逻辑
}

// 刷新日志列表
const refreshLogs = () => {
  fetchLogList()
}

// 监听图表容器大小变化
window.addEventListener('resize', () => {
  logChart?.resize()
})

// 在 onMounted 中添加
onMounted(() => {

  initLogChart()
  fetchLogList()
})
</script>

<style scoped>
.personal-info-container {
  padding: 20px;
}

.profile-card {
  text-align: center;
}

.profile-header {
  margin-bottom: 24px;
}

.avatar-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}

.avatar-mask {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: rgba(0, 0, 0, 0.6);
  border-radius: 50%;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.3s;
}

.avatar-wrapper:hover .avatar-mask {
  opacity: 1;
}

.avatar-mask span {
  margin-top: 8px;
  font-size: 14px;
}

.nickname {
  margin: 0 0 12px;
  font-size: 20px;
  line-height: 1.5;
}

.role-tag {
  display: flex;
  justify-content: center;
}

.profile-stats {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 24px;
  padding: 12px 0;
  border-top: 1px solid var(--el-border-color-lighter);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-divider {
  width: 1px;
  height: 40px;
  background: var(--el-border-color-lighter);
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-primary);
  line-height: 1.5;
}

.stat-label {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.profile-info {
  text-align: left;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--el-text-color-regular);
}

.info-item:last-child {
  margin-bottom: 0;
}

.tab-card {
  min-height: 500px;
}

.custom-tab {
  display: flex;
  align-items: center;
  gap: 6px;
}

.basic-form {
  max-width: 800px;
  margin: 0 auto;
  padding: 24px;
}

.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.preview-container {
  width: 300px;
  height: 300px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
}

.preview-container img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

:deep(.el-tabs__nav) {
  margin-bottom: -20px;
}

.security-container {
  padding: 24px;
}

.security-score {
  margin-bottom: 40px;
  padding: 24px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
}

.score-circle {
  display: flex;
  justify-content: center;
}

.score-content {
  text-align: center;
}

.score-value {
  font-size: 28px;
  font-weight: bold;
  color: var(--el-color-primary);
}

.score-label {
  font-size: 14px;
  color: var(--el-text-color-secondary);
}

.score-tips {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.score-tips h3 {
  margin: 0 0 16px;
  font-size: 16px;
}

.tips-list {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip-item .warning {
  color: var(--el-color-warning);
}

.tip-item .success {
  color: var(--el-color-success);
}

.tip-item .info {
  color: var(--el-color-info);
}

.security-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: var(--el-bg-color-page);
  border-radius: 8px;
  transition: all 0.3s;
}

.security-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.item-info {
  display: flex;
  align-items: center;
  gap: 16px;
}

.item-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 8px;
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.item-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.item-title {
  font-size: 16px;
  font-weight: bold;
}

.item-desc {
  color: var(--el-text-color-secondary);
}

.device-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.logs-container {
  padding: 24px;
}

.log-stats {
  margin-bottom: 20px;
}

.stat-card {
  height: 100px;
}

.stat-content {
  height: 100%;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 64px;
  height: 64px;
  border-radius: 8px;
}

.stat-icon.primary {
  background: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.stat-icon.success {
  background: var(--el-color-success-light-9);
  color: var(--el-color-success);
}

.stat-icon.warning {
  background: var(--el-color-warning-light-9);
  color: var(--el-color-warning);
}

.stat-icon.danger {
  background: var(--el-color-danger-light-9);
  color: var(--el-color-danger);
}

.stat-info {
  flex: 1;
}

.log-chart-card {
  margin-bottom: 20px;
}

.log-chart {
  height: 300px;
}

.log-list-card {
  margin-bottom: 20px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
}

.search-buttons {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.log-detail-container {
  padding: 0 20px;
}

.detail-card {
  margin-top: 20px;
}

.code-block {
  margin-top: 12px;
  background: var(--el-bg-color-page);
  border-radius: 4px;
  overflow: hidden;
}

.code-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
}

.code-content {
  margin: 0;
  padding: 16px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
}

.error-content {
  color: var(--el-color-danger);
}

.error-stack {
  padding: 16px;
  background: var(--el-color-danger-light-9);
  border-radius: 4px;
  font-family: monospace;
  font-size: 14px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-all;
  color: var(--el-color-danger);
}
</style> 