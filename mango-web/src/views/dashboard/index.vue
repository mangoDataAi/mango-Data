<template>
  <div class="dashboard-container">
    <div class="dashboard-header">
      <div class="welcome-section">
        <h1>欢迎使用芒果数据中台</h1>
        <p>{{ greeting }}</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="6" v-for="card in quickStats" :key="card.title">
        <el-card shadow="hover" class="quick-stat-card">
          <div class="stat-content">
            <el-icon class="stat-icon" :class="card.trend">
              <component :is="card.icon" />
            </el-icon>
            <div class="stat-info">
              <div class="stat-title">{{ card.title }}</div>
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-trend" :class="card.trend">
                {{ card.trend === 'up' ? '↑' : '↓' }} {{ card.change }}
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>数据资产趋势</span>
              <el-radio-group v-model="timeRange" size="small">
                <el-radio-button label="week">近一周</el-radio-button>
                <el-radio-button label="month">近一月</el-radio-button>
                <el-radio-button label="year">近一年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div class="chart-container" ref="trendChart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="chart-card">
          <template #header>
            <div class="chart-header">
              <span>数据分布</span>
            </div>
          </template>
          <div class="chart-container" ref="distributionChart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="activity-row">
      <el-col :span="12">
        <el-card shadow="hover" class="activity-card">
          <template #header>
            <div class="card-header">
              <span>最近活动</span>
              <el-button link>查看全部</el-button>
            </div>
          </template>
          <div class="activity-list">
            <div v-for="activity in recentActivities" :key="activity.id" class="activity-item">
              <el-icon class="activity-icon"><component :is="activity.icon" /></el-icon>
              <div class="activity-content">
                <div class="activity-title">{{ activity.title }}</div>
                <div class="activity-time">{{ activity.time }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="todo-card">
          <template #header>
            <div class="card-header">
              <span>待办事项</span>
              <el-button link>添加待办</el-button>
            </div>
          </template>
          <div class="todo-list">
            <el-checkbox-group v-model="checkedTodos">
              <div v-for="todo in todos" :key="todo.id" class="todo-item">
                <el-checkbox :label="todo.id">{{ todo.content }}</el-checkbox>
                <span class="todo-deadline">{{ todo.deadline }}</span>
              </div>
            </el-checkbox-group>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import {
  DataLine,
  Connection,
  Grid,
  SetUp,
  Upload,
  TrendCharts
} from '@element-plus/icons-vue'

// 问候语
const greeting = ref('欢迎回来！今天是 ' + new Date().toLocaleDateString())

// 快速统计数据
const quickStats = [
  {
    title: '数据源总数',
    value: '128',
    change: '12%',
    trend: 'up',
    icon: 'Connection'
  },
  {
    title: '数据表总数',
    value: '1,024',
    change: '8%',
    trend: 'up',
    icon: 'Grid'
  },
  {
    title: '数据量',
    value: '2.8TB',
    change: '5%',
    trend: 'up',
    icon: 'DataLine'
  },
  {
    title: '任务成功率',
    value: '99.9%',
    change: '0.1%',
    trend: 'down',
    icon: 'TrendCharts'
  }
]

// 时间范围选择
const timeRange = ref('month')

// 最近活动
const recentActivities = [
  {
    id: 1,
    title: '更新了销售数据源连接配置',
    time: '10分钟前',
    icon: 'SetUp'
  },
  {
    id: 2,
    title: '完成了用户行为数据同步任务',
    time: '30分钟前',
    icon: 'Upload'
  },
  {
    id: 3,
    title: '创建了新的数据模型',
    time: '2小时前',
    icon: 'Grid'
  },
  {
    id: 4,
    title: '修改了元数据采集规则',
    time: '4小时前',
    icon: 'SetUp'
  }
]

// 待办事项
const todos = [
  {
    id: 1,
    content: '审核数据源接入申请',
    deadline: '今天 14:00'
  },
  {
    id: 2,
    content: '更新数据质量检测规则',
    deadline: '今天 16:00'
  },
  {
    id: 3,
    content: '评审数据模型变更',
    deadline: '明天 10:00'
  },
  {
    id: 4,
    content: '处理数据同步告警',
    deadline: '明天 14:00'
  }
]
const checkedTodos = ref([])

// 图表相关
const trendChart = ref(null)
const distributionChart = ref(null)

onMounted(() => {
  nextTick(() => {
    // 初始化趋势图表
    const trend = echarts.init(trendChart.value)
    trend.setOption({
      tooltip: {
        trigger: 'axis'
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '数据量',
          type: 'line',
          smooth: true,
          data: [820, 932, 901, 934, 1290, 1330, 1320],
          areaStyle: {
            opacity: 0.1
          }
        }
      ]
    })

    // 初始化分布图表
    const distribution = echarts.init(distributionChart.value)
    distribution.setOption({
      tooltip: {
        trigger: 'item'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '数据分布',
          type: 'pie',
          radius: '50%',
          data: [
            { value: 1048, name: '结构化数据' },
            { value: 735, name: '半结构化数据' },
            { value: 580, name: '非结构化数据' }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    })

    // 监听窗口大小变化
    window.addEventListener('resize', () => {
      trend.resize()
      distribution.resize()
    })
  })
})
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 20px;
}

.dashboard-header {
  margin-bottom: 24px;
  
  .welcome-section {
    h1 {
      font-size: 24px;
      color: #2c3e50;
      margin-bottom: 8px;
    }
    
    p {
      color: #606266;
      font-size: 14px;
    }
  }
}

.quick-stat-card {
  margin-bottom: 20px;
  
  .stat-content {
    display: flex;
    align-items: center;
    
    .stat-icon {
      font-size: 48px;
      margin-right: 16px;
      color: #409EFF;
      
      &.up {
        color: #67C23A;
      }
      
      &.down {
        color: #F56C6C;
      }
    }
    
    .stat-info {
      flex: 1;
      
      .stat-title {
        font-size: 14px;
        color: #606266;
        margin-bottom: 8px;
      }
      
      .stat-value {
        font-size: 24px;
        font-weight: bold;
        color: #303133;
        margin-bottom: 4px;
      }
      
      .stat-trend {
        font-size: 12px;
        
        &.up {
          color: #67C23A;
        }
        
        &.down {
          color: #F56C6C;
        }
      }
    }
  }
}

.chart-row {
  margin-bottom: 20px;
  
  .chart-card {
    .chart-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .chart-container {
      height: 300px;
    }
  }
}

.activity-row {
  .activity-card, .todo-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }
  
  .activity-list {
    .activity-item {
      display: flex;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #EBEEF5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .activity-icon {
        font-size: 20px;
        color: #409EFF;
        margin-right: 12px;
      }
      
      .activity-content {
        flex: 1;
        
        .activity-title {
          font-size: 14px;
          color: #303133;
          margin-bottom: 4px;
        }
        
        .activity-time {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }
  
  .todo-list {
    .todo-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #EBEEF5;
      
      &:last-child {
        border-bottom: none;
      }
      
      .todo-deadline {
        font-size: 12px;
        color: #909399;
      }
    }
  }
}
</style> 