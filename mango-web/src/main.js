import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import AIAssistant from './components/AIAssistant'

// ECharts 完整引入
import * as echarts from 'echarts'

// ECharts 按需引入
import {
  CanvasRenderer,
  SVGRenderer
} from 'echarts/renderers'
import {
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  HeatmapChart
} from 'echarts/charts'
import {
  TitleComponent,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkPointComponent,
  MarkLineComponent,
  MarkAreaComponent,
  VisualMapComponent
} from 'echarts/components'
import { use } from 'echarts/core'

// 注册必需的组件
use([
  CanvasRenderer,
  SVGRenderer,
  LineChart,
  BarChart,
  PieChart,
  ScatterChart,
  HeatmapChart,
  TitleComponent,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  DataZoomComponent,
  ToolboxComponent,
  MarkPointComponent,
  MarkLineComponent,
  MarkAreaComponent,
  VisualMapComponent
])

const pinia = createPinia()
const app = createApp(App)

// 全局错误处理
app.config.errorHandler = (err, vm, info) => {
  console.error('Vue错误:', err)
  console.error('错误信息:', info)
}

window.addEventListener('unhandledrejection', event => {
  console.error('未处理的Promise错误:', event.reason)
})

// 注册全局属性
app.config.globalProperties.$echarts = echarts

// 注册插件
app.use(pinia)
app.use(ElementPlus, {
  locale: zhCn
})
app.use(router)

// 注册AI助手组件
app.use(AIAssistant)

app.mount('#app')
