<template>
  <div class="help-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><QuestionFilled /></el-icon>
        <h2>帮助中心</h2>
      </div>
      <div class="header-search">
        <el-input
          v-model="searchQuery"
          placeholder="搜索帮助文档..."
          :prefix-icon="Search"
          clearable
          @keyup.enter="handleSearch"
        >
          <template #append>
            <el-button :icon="Search" @click="handleSearch">搜索</el-button>
          </template>
        </el-input>
      </div>
    </div>

    <!-- 快速入门 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="quick-start" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Guide /></el-icon> 快速入门
              </span>
              <el-button type="primary" @click="startGuide">
                <el-icon><VideoPlay /></el-icon> 开始引导
              </el-button>
            </div>
          </template>
          <el-steps :active="activeStep" finish-status="success">
            <el-step
              v-for="step in guideSteps"
              :key="step.title"
              :title="step.title"
              :description="step.description"
            />
          </el-steps>
          <div class="guide-content">
            <div class="step-detail">
              <h3>{{ currentStep.title }}</h3>
              <p>{{ currentStep.content }}</p>
              <div class="step-image">
                <el-image
                  :src="currentStep.image"
                  fit="contain"
                  :preview-src-list="[currentStep.image]"
                >
                  <template #placeholder>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
              </div>
              <div class="step-actions">
                <el-button
                  :disabled="activeStep === 0"
                  @click="prevStep"
                >
                  <el-icon><ArrowLeft /></el-icon> 上一步
                </el-button>
                <el-button
                  type="primary"
                  :disabled="activeStep === guideSteps.length - 1"
                  @click="nextStep"
                >
                  下一步 <el-icon><ArrowRight /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 常见问题 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <div class="section-title">
          <el-icon><ChatDotSquare /></el-icon>
          <span>常见问题</span>
        </div>
      </el-col>
      <el-col
        v-for="category in faqCategories"
        :key="category.id"
        :span="8"
      >
        <el-card class="faq-card" shadow="hover">
          <div class="category-header">
            <el-icon class="category-icon" :class="category.type">
              <component :is="category.icon" />
            </el-icon>
            <span class="category-title">{{ category.title }}</span>
          </div>
          <el-divider />
          <ul class="faq-list">
            <li
              v-for="item in category.items"
              :key="item.id"
              class="faq-item"
              @click="showFaqDetail(item)"
            >
              <el-icon><QuestionFilled /></el-icon>
              <span class="faq-question">{{ item.question }}</span>
              <el-icon class="arrow-icon"><ArrowRight /></el-icon>
            </li>
          </ul>
          <div class="category-footer">
            <el-button
              type="primary"
              link
              @click="viewMore(category)"
            >
              查看更多 <el-icon><ArrowRight /></el-icon>
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 视频教程与文档分布 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="video-tutorials" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><VideoCamera /></el-icon> 视频教程
              </span>
              <el-radio-group v-model="currentCategory" size="small">
                <el-radio-button v-for="cat in videoCategories" :key="cat.value" :label="cat.value">
                  {{ cat.label }}
                </el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="video-grid">
            <div
              v-for="video in filteredVideos"
              :key="video.id"
              class="video-card"
              @click="playVideo(video)"
            >
              <div class="video-thumbnail">
                <el-image :src="video.thumbnail" fit="cover">
                  <template #placeholder>
                    <div class="image-placeholder">
                      <el-icon><Picture /></el-icon>
                    </div>
                  </template>
                </el-image>
                <div class="video-duration">{{ video.duration }}</div>
                <div class="video-play">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="video-info">
                <h4>{{ video.title }}</h4>
                <p>{{ video.description }}</p>
                <div class="video-meta">
                  <span class="views">
                    <el-icon><View /></el-icon> {{ video.views }}次播放
                  </span>
                  <span class="date">{{ video.date }}</span>
                </div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      
      <el-col :span="8">
        <el-card class="doc-distribution" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Document /></el-icon> 文档分布
              </span>
              <el-tooltip content="文档更新时间：2024-01-15" placement="top">
                <el-icon class="header-tip"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>
          <div class="distribution-tree">
            <el-tree
              :data="docTree"
              :props="defaultProps"
              node-key="id"
              :expand-on-click-node="false"
              highlight-current
              @node-click="handleNodeClick"
            >
              <template #default="{ node, data }">
                <div class="tree-node">
                  <el-icon v-if="data.icon"><component :is="data.icon" /></el-icon>
                  <span>{{ node.label }}</span>
                  <el-tag v-if="data.count" size="small" :type="data.type">{{ data.count }}</el-tag>
                </div>
              </template>
            </el-tree>
          </div>
          <div class="distribution-stats">
            <div class="stat-item">
              <div class="stat-value">{{ totalDocs }}</div>
              <div class="stat-label">文档总数</div>
            </div>
            <el-divider direction="vertical" />
            <div class="stat-item">
              <div class="stat-value">{{ recentUpdates }}</div>
              <div class="stat-label">最近更新</div>
            </div>
            <el-divider direction="vertical" />
            <div class="stat-item">
              <div class="stat-value">{{ popularDocs }}</div>
              <div class="stat-label">热门文档</div>
            </div>
          </div>
        </el-card>

        <!-- 热门搜索 -->
        <el-card class="hot-searches" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><TrendCharts /></el-icon> 热门搜索
              </span>
              <el-tag type="danger" size="small">HOT</el-tag>
            </div>
          </template>
          <div class="search-tags">
            <el-tag
              v-for="tag in hotSearches"
              :key="tag.keyword"
              :type="tag.type"
              class="search-tag"
              @click="handleTagSearch(tag)"
            >
              {{ tag.keyword }}
              <span class="search-count">{{ tag.count }}</span>
            </el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 在线咨询与反馈 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="feedback-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><ChatLineRound /></el-icon> 问题反馈
              </span>
              <el-button-group>
                <el-button type="primary" @click="createFeedback">
                  <el-icon><Plus /></el-icon> 新建反馈
                </el-button>
                <el-button @click="refreshFeedback">
                  <el-icon><Refresh /></el-icon> 刷新
                </el-button>
              </el-button-group>
            </div>
          </template>

          <!-- 反馈列表 -->
          <el-tabs v-model="feedbackTab" class="feedback-tabs">
            <el-tab-pane label="我的反馈" name="my">
              <el-table
                :data="myFeedbackList"
                style="width: 100%"
                v-loading="loading"
              >
                <el-table-column prop="id" label="编号" width="80" />
                <el-table-column prop="title" label="标题" show-overflow-tooltip />
                <el-table-column prop="type" label="类型" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.type.type">{{ row.type.label }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="status" label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status.type" effect="dark">
                      {{ row.status.label }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="createTime" label="创建时间" width="160" />
                <el-table-column label="操作" width="120" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="viewFeedback(row)">
                      查看
                    </el-button>
                    <el-button
                      v-if="row.status.value === 'pending'"
                      link
                      type="danger"
                      @click="cancelFeedback(row)"
                    >
                      撤销
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 分页 -->
              <div class="pagination-container">
                <el-pagination
                  v-model:current-page="currentPage"
                  v-model:page-size="pageSize"
                  :page-sizes="[10, 20, 50]"
                  :total="total"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleSizeChange"
                  @current-change="handleCurrentChange"
                />
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 在线咨询 -->
        <el-card class="contact-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Service /></el-icon> 在线咨询
              </span>
              <el-tag type="success">在线</el-tag>
            </div>
          </template>
          
          <div class="contact-info">
            <div class="contact-item">
              <el-icon><Clock /></el-icon>
              <div class="contact-detail">
                <div class="contact-label">服务时间</div>
                <div class="contact-value">工作日 9:00-18:00</div>
              </div>
            </div>
            <div class="contact-item">
              <el-icon><Phone /></el-icon>
              <div class="contact-detail">
                <div class="contact-label">服务热线</div>
                <div class="contact-value">400-123-4567</div>
              </div>
            </div>
            <div class="contact-item">
              <el-icon><Message /></el-icon>
              <div class="contact-detail">
                <div class="contact-label">邮箱支持</div>
                <div class="contact-value">support@example.com</div>
              </div>
            </div>
          </div>

          <div class="contact-actions">
            <el-button type="primary" @click="startChat">
              <el-icon><ChatDotRound /></el-icon> 开始对话
            </el-button>
            <el-button @click="scheduleCall">
              <el-icon><AlarmClock /></el-icon> 预约通话
            </el-button>
          </div>
        </el-card>

        <!-- 常用工具 -->
        <el-card class="tools-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Tools /></el-icon> 常用工具
              </span>
            </div>
          </template>
          
          <div class="tools-grid">
            <div
              v-for="tool in commonTools"
              :key="tool.id"
              class="tool-item"
              @click="openTool(tool)"
            >
              <el-icon><component :is="tool.icon" /></el-icon>
              <span>{{ tool.name }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 视频播放对话框 -->
    <el-dialog
      v-model="videoDialog.visible"
      :title="videoDialog.title"
      width="800px"
      destroy-on-close
    >
      <div class="video-player">
        <div class="player-placeholder">
          <el-icon><VideoCamera /></el-icon>
          <span>视频播放区域</span>
        </div>
      </div>
      <div class="video-description">
        {{ videoDialog.description }}
      </div>
    </el-dialog>

    <!-- 新建反馈对话框 -->
    <el-dialog
      v-model="feedbackDialog.visible"
      title="新建反馈"
      width="600px"
      destroy-on-close
    >
      <el-form
        ref="feedbackFormRef"
        :model="feedbackForm"
        :rules="feedbackRules"
        label-width="80px"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="feedbackForm.title" placeholder="请输入反馈标题" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="feedbackForm.type" placeholder="请选择反馈类型" style="width: 100%">
            <el-option
              v-for="type in feedbackTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="content">
          <el-input
            v-model="feedbackForm.content"
            type="textarea"
            :rows="4"
            placeholder="请详细描述您遇到的问题"
          />
        </el-form-item>
        <el-form-item label="附件">
          <el-upload
            action="#"
            list-type="picture-card"
            :auto-upload="false"
            :on-change="handleFileChange"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedback">提交</el-button>
      </template>
    </el-dialog>

    <!-- 预约通话对话框 -->
    <el-dialog
      v-model="callDialog.visible"
      title="预约通话"
      width="400px"
      destroy-on-close
    >
      <el-form
        ref="callFormRef"
        :model="callForm"
        :rules="callRules"
        label-width="80px"
      >
        <el-form-item label="称呼" prop="name">
          <el-input v-model="callForm.name" placeholder="请输入您的称呼" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="callForm.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="日期" prop="date">
          <el-date-picker
            v-model="callForm.date"
            type="date"
            placeholder="选择预约日期"
            style="width: 100%"
            :disabled-date="disabledDate"
          />
        </el-form-item>
        <el-form-item label="时间" prop="time">
          <el-time-select
            v-model="callForm.time"
            start="09:00"
            step="00:30"
            end="18:00"
            placeholder="选择预约时间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="callForm.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="callDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitCall">确认预约</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import {
  QuestionFilled,
  Search,
  Guide,
  VideoPlay,
  Picture,
  ArrowLeft,
  ArrowRight,
  ChatDotSquare,
  DataLine,
  Setting,
  Key,
  VideoCamera,
  Document,
  TrendCharts,
  InfoFilled,
  View,
  Folder,
  Collection,
  Histogram,
  ChatLineRound,
  Plus,
  Refresh,
  Service,
  Clock,
  Phone,
  Message,
  ChatDotRound,
  AlarmClock,
  Tools
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 搜索
const searchQuery = ref('')
const handleSearch = () => {
  if (searchQuery.value) {
    ElMessage.success(`正在搜索: ${searchQuery.value}`)
  }
}

// 快速入门指南
const activeStep = ref(0)
const guideSteps = [
  {
    title: '系统概览',
    description: '了解系统基本功能',
    content: '数据中台系统提供强大的数据管理、分析和可视化功能。通过直观的界面，您可以轻松处理和探索数据。',
    image: '/images/guide/overview.png'
  },
  {
    title: '数据接入',
    description: '配置数据源',
    content: '支持多种数据源接入，包括数据库、文件、API等。通过简单的配置步骤，即可完成数据接入。',
    image: '/images/guide/data-source.png'
  },
  {
    title: '数据分析',
    description: '创建分析任务',
    content: '使用可视化的分析工具，对数据进行深入分析。支持多种分析模型和算法。',
    image: '/images/guide/analysis.png'
  },
  {
    title: '可视化展示',
    description: '生成数据报表',
    content: '通过丰富的图表组件，将分析结果转化为直观的可视化展示。支持自定义布局和主题。',
    image: '/images/guide/visualization.png'
  }
]

const currentStep = computed(() => guideSteps[activeStep.value])

const startGuide = () => {
  activeStep.value = 0
  ElMessage.success('开始引导教程')
}

const prevStep = () => {
  if (activeStep.value > 0) {
    activeStep.value--
  }
}

const nextStep = () => {
  if (activeStep.value < guideSteps.length - 1) {
    activeStep.value++
  }
}

// 常见问题分类
const faqCategories = [
  {
    id: 1,
    title: '数据分析',
    icon: 'DataLine',
    type: 'primary',
    items: [
      {
        id: 11,
        question: '如何创建数据分析任务？',
        answer: '在数据分析页面，点击"新建任务"按钮，选择分析模型并配置参数即可创建任务。'
      },
      {
        id: 12,
        question: '支持哪些分析模型？',
        answer: '系统内置多种分析模型，包括统计分析、回归分析、分类分析等。'
      },
      {
        id: 13,
        question: '如何导出分析结果？',
        answer: '在分析结果页面，点击"导出"按钮，选择导出格式即可下载结果文件。'
      }
    ]
  },
  {
    id: 2,
    title: '系统配置',
    icon: 'Setting',
    type: 'warning',
    items: [
      {
        id: 21,
        question: '如何修改系统主题？',
        answer: '在个性化设置中，可以选择不同的主题模式和颜色方案。'
      },
      {
        id: 22,
        question: '如何配置数据源？',
        answer: '在系统配置的数据源管理中，可以添加和管理各类数据源。'
      },
      {
        id: 23,
        question: '如何设置定时任务？',
        answer: '在任务管理中，可以创建和配置定时执行的分析任务。'
      }
    ]
  },
  {
    id: 3,
    title: '权限管理',
    icon: 'Key',
    type: 'danger',
    items: [
      {
        id: 31,
        question: '如何申请数据权限？',
        answer: '在权限管理页面，可以申请查看或编辑特定数据的权限。'
      },
      {
        id: 32,
        question: '如何管理用户角色？',
        answer: '管理员可以在用户管理中分配和调整用户角色。'
      },
      {
        id: 33,
        question: '如何设置数据可见范围？',
        answer: '在数据管理中，可以设置数据的访问权限和可见范围。'
      }
    ]
  }
]

// 显示问题详情
const showFaqDetail = (item) => {
  ElMessage({
    message: item.answer,
    type: 'info',
    duration: 5000
  })
}

// 查看更多
const viewMore = (category) => {
  ElMessage.success(`查看更多${category.title}相关问题`)
}

// 视频教程
const currentCategory = ref('all')
const videoCategories = [
  { label: '全部', value: 'all' },
  { label: '入门指南', value: 'guide' },
  { label: '数据分析', value: 'analysis' },
  { label: '高级功能', value: 'advanced' }
]

const videos = [
  {
    id: 1,
    title: '数据中台系统入门',
    description: '快速了解系统核心功能和基本操作流程',
    thumbnail: '/images/videos/intro.png',
    duration: '05:30',
    views: 1234,
    date: '2024-01-15',
    category: 'guide'
  },
  {
    id: 2,
    title: '数据分析模型详解',
    description: '深入讲解各类数据分析模型的应用场景',
    thumbnail: '/images/videos/analysis.png',
    duration: '08:45',
    views: 856,
    date: '2024-01-14',
    category: 'analysis'
  },
  {
    id: 3,
    title: '自定义图表开发',
    description: '学习如何开发和配置自定义数据图表',
    thumbnail: '/images/videos/chart.png',
    duration: '12:20',
    views: 567,
    date: '2024-01-13',
    category: 'advanced'
  },
  {
    id: 4,
    title: '数据权限管理',
    description: '详细介绍数据权限体系和配置方法',
    thumbnail: '/images/videos/permission.png',
    duration: '07:15',
    views: 789,
    date: '2024-01-12',
    category: 'guide'
  }
]

const filteredVideos = computed(() => {
  if (currentCategory.value === 'all') {
    return videos
  }
  return videos.filter(video => video.category === currentCategory.value)
})

// 视频播放对话框
const videoDialog = reactive({
  visible: false,
  title: '',
  description: ''
})

const playVideo = (video) => {
  videoDialog.visible = true
  videoDialog.title = video.title
  videoDialog.description = video.description
}

// 文档树
const defaultProps = {
  children: 'children',
  label: 'label'
}

const docTree = [
  {
    id: 1,
    label: '快速入门',
    icon: 'Guide',
    count: 5,
    type: 'success',
    children: [
      { id: 11, label: '系统概述', icon: 'Document' },
      { id: 12, label: '基础操作指南', icon: 'Document' },
      { id: 13, label: '常见问题解答', icon: 'Document' }
    ]
  },
  {
    id: 2,
    label: '数据管理',
    icon: 'Folder',
    count: 8,
    type: 'warning',
    children: [
      { id: 21, label: '数据源配置', icon: 'Document' },
      { id: 22, label: '数据模型设计', icon: 'Document' },
      { id: 23, label: '数据质量控制', icon: 'Document' }
    ]
  },
  {
    id: 3,
    label: '分析工具',
    icon: 'Collection',
    count: 6,
    type: 'primary',
    children: [
      { id: 31, label: '分析模型库', icon: 'Document' },
      { id: 32, label: '可视化图表', icon: 'Document' },
      { id: 33, label: '分析报告模板', icon: 'Document' }
    ]
  },
  {
    id: 4,
    label: '系统配置',
    icon: 'Setting',
    count: 4,
    type: 'info',
    children: [
      { id: 41, label: '用户权限', icon: 'Document' },
      { id: 42, label: '系统参数', icon: 'Document' },
      { id: 43, label: '日志管理', icon: 'Document' }
    ]
  }
]

// 文档统计
const totalDocs = ref(23)
const recentUpdates = ref(5)
const popularDocs = ref(8)

const handleNodeClick = (data) => {
  if (!data.children) {
    ElMessage.success(`正在打开文档：${data.label}`)
  }
}

// 热门搜索
const hotSearches = [
  { keyword: '数据分析模型', count: 128, type: 'danger' },
  { keyword: '权限配置', count: 98, type: 'warning' },
  { keyword: '图表定制', count: 85, type: '' },
  { keyword: '数据源连接', count: 76, type: 'info' },
  { keyword: '报表模板', count: 65, type: 'success' },
  { keyword: '定时任务', count: 52, type: '' }
]

const handleTagSearch = (tag) => {
  searchQuery.value = tag.keyword
  handleSearch()
}

// 问题反馈相关
const loading = ref(false)
const feedbackTab = ref('my')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 模拟反馈数据
const myFeedbackList = ref([
  {
    id: 'FB001',
    title: '数据分析模型加载异常',
    type: { label: '功能异常', value: 'bug', type: 'danger' },
    status: { label: '处理中', value: 'processing', type: 'warning' },
    createTime: '2024-01-15 10:30:00'
  },
  {
    id: 'FB002',
    title: '建议增加数据导出格式',
    type: { label: '功能建议', value: 'suggestion', type: 'success' },
    status: { label: '待处理', value: 'pending', type: 'info' },
    createTime: '2024-01-14 15:20:00'
  }
])

// 反馈对话框
const feedbackDialog = reactive({
  visible: false
})

const feedbackForm = reactive({
  title: '',
  type: '',
  content: '',
  files: []
})

const feedbackTypes = [
  { label: '功能异常', value: 'bug' },
  { label: '功能建议', value: 'suggestion' },
  { label: '使用咨询', value: 'question' },
  { label: '其他', value: 'other' }
]

const feedbackRules = {
  title: [{ required: true, message: '请输入反馈标题', trigger: 'blur' }],
  type: [{ required: true, message: '请选择反馈类型', trigger: 'change' }],
  content: [{ required: true, message: '请输入反馈内容', trigger: 'blur' }]
}

// 预约通话对话框
const callDialog = reactive({
  visible: false
})

const callForm = reactive({
  name: '',
  phone: '',
  date: '',
  time: '',
  remark: ''
})

const callRules = {
  name: [{ required: true, message: '请输入您的称呼', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  date: [{ required: true, message: '请选择预约日期', trigger: 'change' }],
  time: [{ required: true, message: '请选择预约时间', trigger: 'change' }]
}

// 常用工具
const commonTools = [
  { id: 1, name: '问题诊断', icon: 'Monitor' },
  { id: 2, name: '性能检测', icon: 'Histogram' },
  { id: 3, name: '数据校验', icon: 'Check' },
  { id: 4, name: '配置检查', icon: 'Setting' },
  { id: 5, name: '日志分析', icon: 'Document' },
  { id: 6, name: '帮助文档', icon: 'Notebook' }
]

// 方法定义
const createFeedback = () => {
  feedbackDialog.visible = true
}

const refreshFeedback = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('刷新成功')
  }, 1000)
}

const handleSizeChange = (val) => {
  pageSize.value = val
  // 重新加载数据
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  // 重新加载数据
}

const viewFeedback = (row) => {
  ElMessage.success(`查看反馈：${row.title}`)
}

const cancelFeedback = (row) => {
  ElMessageBox.confirm(
    '确定要撤销该反馈吗？',
    '撤销确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    ElMessage.success('反馈已撤销')
  })
}

const handleFileChange = (file) => {
  feedbackForm.files.push(file)
}

const submitFeedback = () => {
  ElMessage.success('反馈提交成功')
  feedbackDialog.visible = false
}

const startChat = () => {
  ElMessage.success('正在接入在线客服...')
}

const scheduleCall = () => {
  callDialog.visible = true
}

const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7
}

const submitCall = () => {
  ElMessage.success('预约成功，我们会尽快与您联系')
  callDialog.visible = false
}

const openTool = (tool) => {
  ElMessage.success(`正在打开${tool.name}...`)
}
</script>

<style scoped>
.help-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-icon {
  font-size: 24px;
  color: var(--el-color-primary);
}

.header-title h2 {
  margin: 0;
  font-size: 20px;
}

.header-search {
  width: 400px;
}

.quick-start {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.guide-content {
  margin-top: 24px;
  padding: 20px;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
}

.step-detail {
  text-align: center;
}

.step-detail h3 {
  margin: 0 0 12px;
  font-size: 18px;
}

.step-detail p {
  margin: 0 0 20px;
  color: var(--el-text-color-secondary);
}

.step-image {
  width: 100%;
  max-width: 600px;
  margin: 0 auto 20px;
  border-radius: 8px;
  overflow: hidden;
}

.image-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 300px;
  background-color: var(--el-bg-color);
  color: var(--el-text-color-secondary);
  font-size: 48px;
}

.step-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 18px;
  font-weight: bold;
}

.faq-card {
  height: 100%;
  transition: transform 0.3s;
}

.faq-card:hover {
  transform: translateY(-4px);
}

.category-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.category-icon {
  font-size: 24px;
  padding: 8px;
  border-radius: 8px;
}

.category-icon.primary {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.category-icon.warning {
  color: var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.category-icon.danger {
  color: var(--el-color-danger);
  background-color: var(--el-color-danger-light-9);
}

.category-title {
  font-size: 16px;
  font-weight: bold;
}

.faq-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.faq-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.faq-item:hover {
  background-color: var(--el-bg-color-page);
}

.faq-question {
  flex: 1;
  font-size: 14px;
}

.arrow-icon {
  color: var(--el-text-color-secondary);
  transition: transform 0.3s;
}

.faq-item:hover .arrow-icon {
  transform: translateX(4px);
  color: var(--el-color-primary);
}

.category-footer {
  margin-top: 12px;
  text-align: center;
}

/* 视频教程样式 */
.video-tutorials {
  margin-bottom: 20px;
}

.video-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
  margin-top: 16px;
}

.video-card {
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s;
  border: 1px solid var(--el-border-color-light);
}

.video-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.video-thumbnail {
  position: relative;
  height: 160px;
  overflow: hidden;
}

.video-thumbnail :deep(.el-image) {
  width: 100%;
  height: 100%;
}

.video-duration {
  position: absolute;
  bottom: 8px;
  right: 8px;
  padding: 2px 6px;
  background-color: rgba(0, 0, 0, 0.7);
  color: #fff;
  border-radius: 4px;
  font-size: 12px;
}

.video-play {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 48px;
  height: 48px;
  background-color: rgba(0, 0, 0, 0.7);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  opacity: 0;
  transition: opacity 0.3s;
}

.video-card:hover .video-play {
  opacity: 1;
}

.video-info {
  padding: 12px;
}

.video-info h4 {
  margin: 0 0 8px;
  font-size: 16px;
  line-height: 1.4;
}

.video-info p {
  margin: 0 0 8px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}

.video-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.views {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 文档分布样式 */
.doc-distribution {
  margin-bottom: 20px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;
}

.distribution-stats {
  display: flex;
  justify-content: space-around;
  align-items: center;
  margin-top: 20px;
  padding: 16px;
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
}

.stat-item {
  text-align: center;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: var(--el-color-primary);
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

/* 热门搜索样式 */
.hot-searches {
  margin-bottom: 20px;
}

.search-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.search-tag {
  cursor: pointer;
  transition: all 0.3s;
}

.search-tag:hover {
  transform: scale(1.05);
}

.search-count {
  margin-left: 4px;
  font-size: 12px;
  opacity: 0.8;
}

/* 视频播放对话框样式 */
.video-player {
  width: 100%;
  height: 400px;
  background-color: #000;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 16px;
}

.player-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #666;
  gap: 12px;
}

.player-placeholder .el-icon {
  font-size: 48px;
}

.video-description {
  color: var(--el-text-color-secondary);
  line-height: 1.6;
}

/* 问题反馈样式 */
.feedback-card {
  margin-bottom: 20px;
}

.feedback-tabs {
  margin-top: -12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在线咨询样式 */
.contact-card {
  margin-bottom: 20px;
}

.contact-info {
  margin-bottom: 20px;
}

.contact-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  transition: all 0.3s;
}

.contact-item:hover {
  background-color: var(--el-bg-color-page);
}

.contact-item .el-icon {
  font-size: 24px;
  color: var(--el-color-primary);
  padding: 8px;
  border-radius: 8px;
  background-color: var(--el-color-primary-light-9);
}

.contact-detail {
  flex: 1;
}

.contact-label {
  font-size: 14px;
  margin-bottom: 4px;
}

.contact-value {
  font-size: 16px;
  font-weight: bold;
}

.contact-actions {
  display: flex;
  gap: 12px;
}

.contact-actions .el-button {
  flex: 1;
}

/* 常用工具样式 */
.tools-card {
  margin-bottom: 20px;
}

.tools-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.tool-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.tool-item:hover {
  background-color: var(--el-color-primary-light-9);
  color: var(--el-color-primary);
}

.tool-item .el-icon {
  font-size: 24px;
}

.tool-item span {
  font-size: 13px;
}
</style> 