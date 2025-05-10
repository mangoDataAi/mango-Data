<template>
 <div class="dataset-database">
   <!-- 统计卡片区域 -->
   <div class="statistics-area" v-loading="statsLoading">
     <el-row :gutter="20">
       <el-col :span="6">
         <div class="stat-card total">
           <div class="stat-value">{{ statistics.totalSources }}<span class="stat-unit">个</span></div>
           <div class="stat-label">数据库总数</div>
           <div class="stat-trend">
             较上月 <span class="up">+{{ statistics.totalSourcesGrowth }}%</span>
           </div>
           <el-icon class="stat-icon"><Coin /></el-icon>
         </div>
       </el-col>
       <el-col :span="6">
         <div class="stat-card connect-rate">
           <div class="stat-value">{{ statistics.connectionRate }}<span class="stat-unit">%</span></div>
           <div class="stat-label">连接成功率</div>
           <div class="stat-trend">
             较上月 <span class="up">+{{ statistics.connectionRateGrowth }}%</span>
           </div>
           <el-icon class="stat-icon"><CircleCheckFilled /></el-icon>
         </div>
       </el-col>
       <el-col :span="6">
         <div class="stat-card running">
           <div class="stat-value">{{ statistics.dbTypeCount }}<span class="stat-unit">个</span></div>
           <div class="stat-label">数据源种类</div>
           <div class="stat-trend">
             支持 <span class="up">{{ statistics.supportedTypes }}种</span>
           </div>
           <el-icon class="stat-icon"><Menu /></el-icon>
         </div>
       </el-col>
       <el-col :span="6">
         <div class="stat-card error">
           <div class="stat-value">{{ statistics.failedSources }}<span class="stat-unit">个</span></div>
           <div class="stat-label">连接失败数</div>
           <div class="stat-trend down good">
             较上月 <span class="down">{{ statistics.failedSourcesChange }}%</span>
           </div>
           <el-icon class="stat-icon"><Warning /></el-icon>
         </div>
       </el-col>
     </el-row>
   </div>

   <el-card>
     <template #header>
       <div class="card-header">
         <div class="header-left">
           <el-input
               v-model="queryParams.name"
               placeholder="请输入数据源名称"
               style="width: 200px"
               clearable
               @keyup.enter="handleQuery"
           />
           <el-button type="primary" @click="handleQuery">查询</el-button>
           <el-button @click="resetQuery">重置</el-button>
         </div>
         <div class="header-right">
           <el-button type="primary" @click="showAdvancedQuery">
             <el-icon>
                <Search/>
              </el-icon>
             高级查询
           </el-button>
           <el-button type="primary" @click="handleAdd">新建连接</el-button>
           <el-button
               type="danger"
               :disabled="selectedIds.length === 0"
               @click="handleBatchDelete"
           >
             批量删除
           </el-button>
         </div>
       </div>
     </template>

     <el-table
         :data="tableData"
         style="width: 100%"
         v-loading="loading"
         @selection-change="handleSelectionChange"
     >
       <el-table-column type="selection" width="55"/>
       <el-table-column prop="name" label="连接名称"/>
       <el-table-column prop="type" label="数据库类型"/>
       <el-table-column prop="url" label="连接URL" show-overflow-tooltip/>
       <el-table-column prop="username" label="用户名"/>
       <el-table-column label="连接状态">
         <template #default="scope">
           <el-tag :type="scope.row.status ? 'success' : 'danger'">
             {{ scope.row.status ? '已启用' : '已禁用' }}
           </el-tag>
         </template>
       </el-table-column>
       <el-table-column label="操作" width="320">
         <template #default="scope">
           <el-button-group>
             <el-button type="primary" @click="handleViewTables(scope.row)">
               <el-icon>
                  <DataLine/>
                </el-icon>
                {{ getViewButtonText(scope.row.type) }}
             </el-button>
             <el-button type="success" @click="handleTest(scope.row)">
               <el-icon>
                  <Connection/>
                </el-icon>
                测试
             </el-button>
             <el-button type="warning" @click="handleEdit(scope.row)">
               <el-icon>
                  <Edit/>
                </el-icon>
                编辑
             </el-button>
             <el-button type="danger" @click="handleDelete(scope.row)">
               <el-icon>
                  <Delete/>
                </el-icon>
                删除
             </el-button>
           </el-button-group>
         </template>
       </el-table-column>
     </el-table>

     <div class="pagination-container">
       <el-pagination
           v-model:current-page="queryParams.current"
           v-model:page-size="queryParams.size"
           :page-sizes="[10, 20, 50, 100]"
           :total="total"
           layout="total, sizes, prev, pager, next, jumper"
           @size-change="handleSizeChange"
           @current-change="handleCurrentChange"
       />
     </div>

      <!-- 数据库类型选择对话框 -->
     <el-dialog
         v-model="dbSelectorVisible"
         title="选择数据库类型"
         width="85%"
         :fullscreen="false"
         :modal="true"
         top="5vh"
         :append-to-body="true"
         :destroy-on-close="true"
     >
       <div class="db-selector">
         <div
             v-for="(group, groupName) in groupedDatabases"
             :key="groupName"
             class="db-group"
         >
           <h3>{{ groupName }}</h3>
           <div class="db-list">
             <div
                 v-for="db in group"
                 :key="db.value"
                 class="db-item"
                 :class="{ active: selectedDb === db.value }"
                 @click="selectDatabase(db)"
             >
               <img :src="db.icon" :alt="db.label">
               <span>{{ db.label }}</span>
             </div>
           </div>
         </div>
       </div>
       <template #footer>
         <div class="dialog-footer">
           <el-button @click="dbSelectorVisible = false">取消</el-button>
           <el-button
               type="primary"
               @click="handleDbSelect"
               :disabled="!selectedDb"
           >
             下一步
           </el-button>
         </div>
       </template>
     </el-dialog>

     <!-- 数据源配置对话框 -->
      <el-dialog
          v-model="configDialogVisible"
          :title="editingConfig ? '编辑数据源' : '新建数据源'"
          width="600px"
      >
        <el-form
            ref="formRef"
            :model="configForm"
            :rules="rules"
            label-width="100px"
        >
          <el-form-item label="连接名称" prop="name">
            <el-input v-model="configForm.name" placeholder="请输入连接名称"/>
          </el-form-item>
          <!-- 文件系统表单项 -->
          <template v-if="isFileSystem">
            <template v-if="configForm.type === 'file'">
              <el-form-item label="文件路径" prop="path">
                <el-input v-model="configForm.path" placeholder="请输入本地文件路径"/>
              </el-form-item>
            </template>
            <template v-else>
              <el-form-item label="主机地址" prop="host">
                <el-input v-model="configForm.host" placeholder="请输入主机地址"/>
              </el-form-item>
              <el-form-item label="端口" prop="port">
                <el-input v-model="configForm.port" placeholder="请输入端口号"/>
              </el-form-item>
              <el-form-item label="用户名" prop="username">
                <el-input v-model="configForm.username" placeholder="请输入用户名"/>
              </el-form-item>
              <el-form-item label="密码" prop="password">
                <el-input
                    v-model="configForm.password"
                    type="password"
                    placeholder="请输入密码"
                    show-password
                />
              </el-form-item>
              <template v-if="['minio', 's3', 'oss'].includes(configForm.type)">
                <el-form-item label="存储桶" prop="bucket">
                  <el-input v-model="configForm.bucket" placeholder="请输入存储桶名称"/>
               </el-form-item>
                <el-form-item label="访问密钥" prop="accessKey">
                  <el-input v-model="configForm.accessKey" placeholder="请输入访问密钥"/>
                </el-form-item>
                <el-form-item label="密钥密码" prop="secretKey">
                  <el-input
                      v-model="configForm.secretKey"
                      type="password"
                      placeholder="请输入密钥密码"
                      show-password
                  />
                </el-form-item>
              </template>
              <template v-if="configForm.type === 'hdfs'">
                <el-form-item label="命名空间" prop="namenode">
                  <el-input v-model="configForm.namenode" placeholder="请输入HDFS命名空间"/>
               </el-form-item>
             </template>
           </template>
         </template>

         <!-- 数据库表单项 -->
         <template v-else>
           <el-form-item label="主机地址" prop="host">
             <el-input v-model="configForm.host" placeholder="请输入主机地址">
               <template #prepend>
                 <span>{{ urlPrefix }}</span>
               </template>
             </el-input>
           </el-form-item>

           <el-form-item label="端口" prop="port">
             <el-input v-model="configForm.port" placeholder="请输入端口号"/>
           </el-form-item>

           <el-form-item label="数据库名" prop="dbName">
             <el-input v-model="configForm.dbName" placeholder="请输入数据库名"/>
           </el-form-item>

           <el-form-item label="用户名" prop="username">
             <el-input v-model="configForm.username" placeholder="请输入用户名"/>
           </el-form-item>

           <el-form-item label="密码" prop="password">
             <el-input
                 v-model="configForm.password"
                 type="password"
                 placeholder="请输入密码"
                  show-password
              />
           </el-form-item>
            <el-form-item label="测试连接" prop="validationQuery">
              <el-input v-model="configForm.validationQuery" placeholder="请输入测试查询语句"/>
            </el-form-item>
            <el-form-item label="连接状态" prop="status">
              <el-switch
                  v-model="configForm.status"
                  active-text="启用"
                  inactive-text="禁用"
                  :active-value="true"
                  :inactive-value="false"
              />
            </el-form-item>
          </template>
        </el-form>
       <template #footer>
          <div class="dialog-footer">
            <el-button @click="configDialogVisible = false">取消</el-button>
            <el-button type="warning" @click="handleTestConfig">测试连接</el-button>
            <el-button type="primary" @click="handleSave">保存</el-button>
          </div>
        </template>
      </el-dialog>
     <!-- 连接监控对话框 -->
      <el-dialog
          v-model="monitoringDialogVisible"
          title="连接监控"
          width="80%"
      >
        <div class="monitoring-content">
          <el-row :gutter="20">
            <el-col :span="8">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>活跃连接数</span>
                  </div>
                </template>
                <div class="metric">
                  <span class="number">{{ monitoringData.activeConnections }}</span>
                  <span class="label">个</span>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>连接池使用率</span>
                  </div>
                </template>
                <div class="metric">
                  <span class="number">{{ monitoringData.poolUsage }}%</span>
                </div>
              </el-card>
            </el-col>
            <el-col :span="8">
              <el-card>
                <template #header>
                  <div class="card-header">
                    <span>等待连接数</span>
                  </div>
                </template>
                <div class="metric">
                  <span class="number">{{ monitoringData.waitingConnections }}</span>
                  <span class="label">个</span>
                </div>
              </el-card>
            </el-col>
          </el-row>
        </div>
      </el-dialog>
     <!-- 性能分析对话框 -->
      <el-dialog
          v-model="performanceDialogVisible"
          title="性能分析"
          width="80%"
      >
        <div class="performance-content">
          <el-tabs>
            <el-tab-pane label="SQL执行统计">
              <el-table :data="performanceData.sqlStats">
                <el-table-column prop="sql" label="SQL语句"/>
               <el-table-column prop="execCount" label="执行次数"/>
               <el-table-column prop="avgTime" label="平均执行时间(ms)"/>
               <el-table-column prop="maxTime" label="最大执行时间(ms)"/>
             </el-table>
           </el-tab-pane>
           <el-tab-pane label="连接池状态">
             <!-- 这里可以放置连接池状态的图表 -->
           </el-tab-pane>
           <el-tab-pane label="性能趋势">
             <!-- 这里可以放置性能趋势的图表 -->
           </el-tab-pane>
         </el-tabs>
       </div>
     </el-dialog>

     <!-- 高级查询对话框 -->
     <el-dialog
         v-model="advancedQueryVisible"
         title="高级查询"
         width="500px"
     >
       <el-form
           ref="advancedQueryFormRef"
           :model="advancedQueryForm"
           label-width="100px"
       >
         <el-form-item label="数据库类型">
           <el-select
               v-model="advancedQueryForm.dbTypes"
               multiple
               placeholder="请选择数据库类型"
               style="width: 100%"
           >
             <el-option-group
                 v-for="group in databaseTypes"
                 :key="group.label"
                 :label="group.label"
             >
               <el-option
                   v-for="item in group.options"
                   :key="item.value"
                   :label="item.label"
                   :value="item.value"
               />
             </el-option-group>
           </el-select>
         </el-form-item>

         <el-form-item label="连接状态">
           <el-select
               v-model="advancedQueryForm.status"
               placeholder="请选择连接状态"
               clearable
               style="width: 100%"
           >
             <el-option label="已启用" :value="true"/>
             <el-option label="已禁用" :value="false"/>
           </el-select>
         </el-form-item>

         <el-form-item label="创建时间">
           <el-date-picker
               v-model="advancedQueryForm.timeRange"
               type="daterange"
               range-separator="至"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
               style="width: 100%"
           />
         </el-form-item>
       </el-form>

       <template #footer>
         <span class="dialog-footer">
           <el-button @click="advancedQueryVisible = false">取消</el-button>
           <el-button @click="resetAdvancedQuery">重置</el-button>
           <el-button type="primary" @click="handleAdvancedQuery">查询</el-button>
         </span>
       </template>
     </el-dialog>

     <!-- 添加监控对话框 -->
     <DatabaseMonitor
         v-model:visible="monitorVisible"
         :datasource="currentDatasource"
     />
   </el-card>
 </div>

</template>


<script setup>

import {ref, onMounted, computed, watch, onBeforeUnmount, nextTick, reactive} from 'vue'

import {ElMessage, ElMessageBox, ElLoading} from 'element-plus'

import {databaseIcons} from '@/assets/database-icons'

import {
 getDatabaseSources,
 getSourceTableStructure,
 createDs,
 updateDs,
 deleteDs,
 testDs,
 getDbDsPage,
 batchDeleteDbDs,
 getDataSourceStatistics

} from '@/api/datasource'

import {
 getConnectionMetrics,
 getPerformanceMetrics,
 getAllMetrics

} from '@/api/monitor'

import {useRouter} from 'vue-router'

import {
  Search, 
  Grid, 
  Monitor, 
  Edit, 
  DataLine, 
  Delete, 
  Refresh, 
  Connection, 
  Coin, 
  CircleCheckFilled, 
  Odometer, 
  Warning, 
  ArrowUp, 
  ArrowDown,
  Menu,
  Files,
  FolderOpened,
  Collection
} from '@element-plus/icons-vue'
import DatabaseMonitor from '@/components/monitor/DatabaseMonitor.vue'
const router = useRouter()


// 数据库分组
const groupedDatabases = {
  '关系型数据库': [
    {label: 'MySQL', value: 'mysql', icon: databaseIcons.mysql},
    {label: 'Oracle', value: 'oracle', icon: databaseIcons.oracle},
    {label: 'SQLServer', value: 'sqlserver', icon: databaseIcons.sqlserver},
  ],
 '国产数据库': [
    {label: 'DM7', value: 'dm7', icon: databaseIcons.dm7},
    {label: 'DM8', value: 'dm8', icon: databaseIcons.dm8},
  ],
 '大数据数据库': [
    {label: 'Hive', value: 'hive', icon: databaseIcons.hive},
    {label: 'ClickHouse', value: 'clickhouse', icon: databaseIcons.clickhouse},
  ],
 '时序数据库': [
   {label: 'InfluxDB', value: 'influxdb', icon: databaseIcons.influxdb},
  ],
 '图数据库': [
    {label: 'Neo4j', value: 'neo4j', icon: databaseIcons.neo4j},
  ],
 'NoSQL数据库': [
    {label: 'MongoDB', value: 'mongodb', icon: databaseIcons.mongodb},
  ],
 '文件服务器': [
    {label: 'SFTP服务器', value: 'sftp', icon: databaseIcons.sftp},
  ],
 '消息数据库': [
    {label: 'Kafka', value: 'kafka', icon: databaseIcons.kafka},
  ]

}


// 表格数据
const loading = ref(false)
const tableData = ref([])
// 对话框显示控制
const dbSelectorVisible = ref(false)
const configDialogVisible = ref(false)
const selectedDb = ref('')
const editingConfig = ref(false)


// 表单相关
const formRef = ref(null)

const defaultConfig = {
  name: '',
  type: '',
  url: '',
  host: '127.0.0.1',
  port: '',
  dbName: '',
  username: '',
  password: '',
  status: true,  // 默认启用
  validationQuery: 'SELECT 1',
  initialSize: 5,
  maxActive: 20,
  minIdle: 5,
  maxWait: 60000,
  testOnBorrow: true,
  testOnReturn: false,
  testWhileIdle: true
}

const configForm = ref({...defaultConfig})


// 添加 URL 前缀映射表

const urlPrefixMap = {
 // 关系型数据库
 mysql: 'jdbc:mysql://',
 oracle: 'jdbc:oracle:thin:@',
 postgresql: 'jdbc:postgresql://',
 sqlserver: 'jdbc:sqlserver://',
 db2: 'jdbc:db2://',
 mariadb: 'jdbc:mariadb://',
 sybase: 'jdbc:sybase:Tds:',
 h2: 'jdbc:h2:',

 // 国产数据库
 dm7: 'jdbc:dm://',
 dm8: 'jdbc:dm://',
 gaussdb: 'jdbc:gaussdb://',
 opengauss: 'jdbc:opengauss://',
 kingbase: 'jdbc:kingbase8://',
 shentong: 'jdbc:oscar://',
 highgo: 'jdbc:highgo://',
 gbase: 'jdbc:gbase://',
 uxdb: 'jdbc:uxdb://',
 hydb: 'jdbc:hydb://',
 xugu: 'jdbc:xugu://',
 neudb: 'jdbc:neudb://',
 sequoiadb: 'jdbc:sequoiadb://',
 tbase: 'jdbc:tbase://',

 // 大数据数据库
 hive: 'jdbc:hive2://',
 hbase: 'jdbc:phoenix:',
 clickhouse: 'jdbc:clickhouse://',
 doris: 'jdbc:mysql://', // Doris 使用 MySQL 协议
 starrocks: 'jdbc:mysql://', // StarRocks 使用 MySQL 协议
 spark: 'jdbc:hive2://', // Spark SQL 使用 Hive 协议
 presto: 'jdbc:presto://',
 trino: 'jdbc:trino://',
 impala: 'jdbc:impala://',
 kylin: 'jdbc:kylin://',
 tidb: 'jdbc:mysql://', // TiDB 使用 MySQL 协议
 oceanbase: 'jdbc:oceanbase://',

 // 时序数据库
 influxdb: 'http://',
 tdengine: 'jdbc:TAOS://',
 opentsdb: 'http://',
 timescaledb: 'jdbc:postgresql://', // TimescaleDB 使用 PostgreSQL 协议

 // NoSQL数据库
 mongodb: 'mongodb://',
 redis: 'redis://',
 elasticsearch: 'http://',
 cassandra: 'cassandra://',
 couchdb: 'http://',
  couchbase: 'couchbase://',
  neo4j: 'bolt://',
  nebula: 'nebula://',
  hugegraph: 'http://',
  janusgraph: 'janusgraph://',
 // 消息队列
 kafka: 'kafka://',
  rabbitmq: 'amqp://',
  rocketmq: 'rocketmq://',
 // 文件服务器
 ftp: 'ftp://',
 sftp: 'sftp://',
 hdfs: 'hdfs://',
 minio: 'http://',
 s3: 'https://',
 oss: 'http://',
 csv: 'file://',
 excel: 'file://',
 json: 'file://'

}


// 查询参数

const queryParams = ref({
 current: 1,
 size: 10,
 name: ''

})


// 总数

const total = ref(0)


// 选中的行

const selectedIds = ref([])


// 数据库类型选项

const databaseTypes = [
 {
   label: '关系型数据库',
   options: [
     {label: 'MySQL', value: 'mysql'},
     {label: 'Oracle', value: 'oracle'},
     {label: 'PostgreSQL', value: 'postgresql'},
     {label: 'SQL Server', value: 'sqlserver'},
     {label: 'MariaDB', value: 'mariadb'},
     {label: 'DB2', value: 'db2'},
     {label: 'Informix', value: 'informix'},
     {label: 'SAP HANA', value: 'sap_hana'},
     {label: 'Sybase', value: 'sybase'},
     {label: 'H2', value: 'h2'}
   ]
 },
 {
   label: '国产数据库',
   options: [
     {label: '达梦数据库', value: 'dm'},
     {label: '人大金仓', value: 'kingbase'},
     {label: '南大通用', value: 'gbase'},
     {label: '神通数据库', value: 'oscar'},
     {label: '东软数据库', value: 'arterybase'},
     {label: '优炫数据库', value: 'uxdb'},
     {label: '瀚高数据库', value: 'highgo'},
     {label: '海量数据库', value: 'vastbase'},
     {label: '华为高斯', value: 'gaussdb'},
     {label: 'OpenGauss', value: 'opengauss'}
   ]
 },
 {
   label: 'NoSQL数据库',
   options: [
     {label: 'MongoDB', value: 'mongodb'},
     {label: 'Redis', value: 'redis'},
     {label: 'Elasticsearch', value: 'elasticsearch'},
     {label: 'Cassandra', value: 'cassandra'},
     {label: 'Neo4j', value: 'neo4j'}
   ]
 },
 {
   label: '文件数据库',
   options: [
     {label: 'CSV文件', value: 'csv'},
     {label: 'Excel文件', value: 'excel'},
     {label: 'JSON文件', value: 'json'},
     {label: 'FTP', value: 'ftp'},
     {label: 'SFTP', value: 'sftp'},
     {label: 'HDFS', value: 'hdfs'},
     {label: 'MinIO', value: 'minio'},
     {label: 'S3', value: 's3'},
     {label: 'OSS', value: 'oss'}
   ]
 },
 {   label: '消息数据库',
   options: [
     {label: 'Kafka', value: 'kafka'},
     {label: 'RabbitMQ', value: 'rabbitmq'},
     {label: 'RocketMQ', value: 'rocketmq'}
   ]
 }

]


// 新增统计数据对象
const statistics = reactive({
  totalSources: 0,
  totalSourcesGrowth: 0,
  connectionRate: 0,
  connectionRateGrowth: 0,
  failedSources: 0,
  failedSourcesChange: 0,
  dbTypeCount: 0,
  supportedTypes: 0,
  recentlyAdded: 0,
  weeklyGrowth: 0
})

// 控制统计卡片加载状态
const statsLoading = ref(false)

// 获取统计数据
const loadStatistics = async () => {
  statsLoading.value = true
  try {
    const res = await getDataSourceStatistics()
    if (res.code === 0 && res.data) {
      Object.assign(statistics, res.data)
    } else {
      console.error('获取统计数据失败:', res.message)
    }
  } catch (error) {
    console.error('获取统计数据失败:', error)
  } finally {
    statsLoading.value = false
  }
}


// 获取数据源列表

const loadData = async (params = {}) => {
 try {
   loading.value = true
   // 合并基础查询参数和高级查询参数
   const queryData = {
     ...queryParams.value,
     ...params,
     // 处理时间范围
     startTime: params.timeRange?.[0]?.toISOString(),
     endTime: params.timeRange?.[1]?.toISOString(),
     // 处理数据库类型数组
     dbTypes: params.dbTypes?.join(','),
   }

   const res = await getDbDsPage(queryData)

   if (res.code === 0) {
     tableData.value = res.data.records
     total.value = res.data.total
   }
 } catch (error) {
   ElMessage.error(error.message || '获取数据源列表失败')
 } finally {
   loading.value = false
 }
}


// 查询

const handleQuery = () => {
 queryParams.value.current = 1
 loadData()

}


// 重置查询

const resetQuery = () => {
 queryParams.value = {
   current: 1,
   size: 10,
   name: ''
 }
 loadData()

}


// 批量删除

const handleBatchDelete = () => {
 if (selectedIds.value.length === 0) {
   ElMessage.warning('请选择要删除的数据')
   return
 }

 ElMessageBox.confirm(
      `确定要删除选中的 ${selectedIds.value.length} 条数据吗？`,
     '警告',
     {
       confirmButtonText: '确定',
       cancelButtonText: '取消',
       type: 'warning'
     }
  ).then(async () => {
   try {
     const res = await batchDeleteDbDs(selectedIds.value)
     if (res.code === 0) {
       ElMessage.success('批量删除成功')
       loadData()
     }
   } catch (error) {
     console.error('批量删除失败：', error)
     ElMessage.error('批量删除失败')
   }
 }).catch(() => {
  })

}


// 选择变更

const handleSelectionChange = (selection) => {
 selectedIds.value = selection.map(item => item.id)

}


// 页码变更

const handleCurrentChange = (val) => {
 queryParams.value.current = val
 loadData()

}


// 每页条数变更

const handleSizeChange = (val) => {
 queryParams.value.size = val
 queryParams.value.current = 1
 loadData()

}


// 显示数据库选择器

const showDbSelector = () => {
 selectedDb.value = ''
 dbSelectorVisible.value = true
 editingConfig.value = false
}


// 选择数据库

const selectDatabase = (db) => {
 selectedDb.value = db.value
}


// 处理数据库选择

const handleDbSelect = () => {
 if (!selectedDb.value) {
   ElMessage.warning('请选择数据库类型')
   return
 }

 // 关闭数据库选择对话框
 dbSelectorVisible.value = false

 // 重置并预填充配置表单
 configForm.value = {
   ...defaultConfig,
   ...selectedDb.value,
   type: selectedDb.value, // 使用已选择的数据库类型
   host: '127.0.0.1',
   port: getDefaultPort(selectedDb.value), // 根据数据库类型获取默认端口
   dbName: '',
   username: getDefaultUsername(selectedDb.value), // 根据数据库类型获取默认用户名
   password: '',
   validationQuery: getDefaultValidationQuery(selectedDb.value), // 根据数据库类型获取默认验证查询
   status: true,
   initialSize: 5,
   maxActive: 20,
   minIdle: 5,
   maxWait: 60000,
   testOnBorrow: true,
   testOnReturn: false,
   testWhileIdle: true
 }

 // 打开配置对话框
 configDialogVisible.value = true
}


// 获取默认端口

const getDefaultPort = (dbType) => {
 const portMap = {
   // 关系型数据库
   mysql: '3306',
   postgresql: '5432',
   oracle: '1521',
   sqlserver: '1433',
   db2: '50000',
   mariadb: '3306',
   sybase: '5000',
   h2: '9092',

   // 国产数据库
   dm7: '5236',
   dm8: '5236',
   gaussdb: '25288',
   opengauss: '5432',
   kingbase: '54321',
   shentong: '2003',
   highgo: '5866',
   gbase: '5258',
   uxdb: '5432',
   hydb: '5432',
   xugu: '5138',
   neudb: '2003',
   sequoiadb: '11810',
   tbase: '5432',

   // 大数据数据库
   hive: '10000',
   hbase: '2181',
   clickhouse: '8123',
   doris: '9030',
   starrocks: '9030',
   spark: '10000',
   presto: '8080',
   trino: '8080',
   impala: '21050',
   kylin: '7070',
   tidb: '4000',
   oceanbase: '2881',

   // NoSQL数据库
   mongodb: '27017',
   redis: '6379',
   elasticsearch: '9200',
   cassandra: '9042',
   couchdb: '5984',
   couchbase: '8091',
   neo4j: '7687',
   nebula: '9669',
   hugegraph: '8080',
   janusgraph: '8182',

   // 消息队列
   kafka: '9092',
   rabbitmq: '5672',
   rocketmq: '9876',

   // 文件服务器
   ftp: '21',
   sftp: '22',
   hdfs: '9000',
   minio: '9000',
   s3: '443',
   oss: '80'
 }
 return portMap[dbType] || ''
}


// 获取默认用户名

const getDefaultUsername = (dbType) => {
 const usernameMap = {
   // 关系型数据库
   mysql: 'root',
   postgresql: 'postgres',
   oracle: 'system',
   sqlserver: 'sa',
   db2: 'db2admin',
   mariadb: 'root',
   sybase: 'sa',
   h2: 'sa',

   // 国产数据库
   dm7: 'SYSDBA',
   dm8: 'SYSDBA',
   gaussdb: 'gaussdb',
   opengauss: 'gaussdb',
   kingbase: 'SYSTEM',
   shentong: 'SYSDBA',
   highgo: 'highgo',
   gbase: 'gbase',
   uxdb: 'uxdb',
   hydb: 'SYSDBA',
   xugu: 'SYSDBA',
   neudb: 'SYSDBA',
   sequoiadb: 'admin',
   tbase: 'tbase',

   // 大数据数据库
   hive: 'hive',
   hbase: 'hbase',
   clickhouse: 'default',
   doris: 'root',
   starrocks: 'root',
   spark: 'spark',
   presto: 'presto',
   trino: 'admin',
   impala: 'impala',
   kylin: 'ADMIN',
   tidb: 'root',
   oceanbase: 'root',

   // NoSQL数据库
   mongodb: 'admin',
   redis: '',
   elasticsearch: 'elastic',
   cassandra: 'cassandra',
   couchdb: 'admin',
   couchbase: 'Administrator',
   neo4j: 'neo4j',
   nebula: 'root',
   hugegraph: 'admin',
   janusgraph: 'admin',

   // 文件服务器
   ftp: 'anonymous',
   sftp: 'root',
   minio: 'minioadmin',
   s3: 'admin',
   oss: 'admin'
 }
 return usernameMap[dbType] || ''
}


// 获取默认验证查询

const getDefaultValidationQuery = (dbType) => {
 const queryMap = {
   // 关系型数据库
   mysql: 'SELECT 1',
   postgresql: 'SELECT 1',
   oracle: 'SELECT 1 FROM DUAL',
   sqlserver: 'SELECT 1',
   db2: 'SELECT 1 FROM SYSIBM.SYSDUMMY1',
   mariadb: 'SELECT 1',
   sybase: 'SELECT 1',
   h2: 'SELECT 1',

   // 国产数据库
   dm7: 'SELECT 1',
   dm8: 'SELECT 1',
   gaussdb: 'SELECT 1',
   opengauss: 'SELECT 1',
   kingbase: 'SELECT 1',
   shentong: 'SELECT 1',
   highgo: 'SELECT 1',
   gbase: 'SELECT 1',
   uxdb: 'SELECT 1',
   hydb: 'SELECT 1',
   xugu: 'SELECT 1',
   neudb: 'SELECT 1',
   sequoiadb: 'SELECT 1',
   tbase: 'SELECT 1',

   // 大数据数据库
   hive: 'SELECT 1',
   clickhouse: 'SELECT 1',
   doris: 'SELECT 1',
    starrocks: 'SELECT 1',
    presto: 'SELECT 1',
    trino: 'SELECT 1',
    impala: 'SELECT 1',
    tidb: 'SELECT 1',
    oceanbase: 'SELECT 1',
    // NoSQL数据库
    mongodb: 'db.runCommand({ping: 1})',
    redis: 'PING',
    elasticsearch: 'GET /_cluster/health',
    cassandra: 'SELECT release_version FROM system.local',
    neo4j: 'RETURN 1',
    // 其他类型数据库可能不需要验证查询，返回空字符串
    kafka: '',
    rabbitmq: '',
    rocketmq: '',
    ftp: '',
    sftp: '',
    hdfs: '',
    minio: '',
    s3: '',
    oss: ''
  }
  return queryMap[dbType] || 'SELECT 1'
}

// 编辑数据源
const handleEdit = (row) => {
  editingConfig.value = true
  configForm.value = {
    ...defaultConfig,
    ...row,
    status: row.status ?? true  // 确保状态值正确回显，默认为启用
  }
  configDialogVisible.value = true
}

// 删除数据源
const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除该数据源吗？', '提示', {
      type: 'warning'
    })
    const res = await deleteDs(row.id)
    if (res.code === 0) {
      ElMessage.success('删除成功')
      loadData()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 修改测试连接方法
const handleTestConfig = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()

    // 创建测试配置对象，不使用解构操作，防止字符串被拆分
    const testConfig = {};
    testConfig.name = configForm.value.name;
    testConfig.type = configForm.value.type;

    // 确保dm7类型能够正常使用，防止出现字符拆分
    if (typeof testConfig.type === 'object' && testConfig.type.length && !testConfig.type.toLowerCase) {
      // 如果type是字符数组，将其连接为字符串
      testConfig.type = Array.from(testConfig.type).join('');
      console.log('测试连接 - 修正后的数据库类型:', testConfig.type);
    }

    testConfig.host = configForm.value.host;
    testConfig.port = configForm.value.port;
    testConfig.dbName = configForm.value.dbName;
    testConfig.username = configForm.value.username;
    testConfig.password = configForm.value.password;

    // 获取正确的数据库类型
    const dbType = typeof testConfig.type === 'string' ? testConfig.type : (testConfig.type ? String(testConfig.type) : '');

    // 构建URL，避免使用configForm.value.type直接拼接
    const prefix = urlPrefixMap[dbType] || 'jdbc:dm://';
    testConfig.url = `${prefix}${configForm.value.host}:${configForm.value.port}/${configForm.value.dbName}`;

    console.log('测试连接配置:', testConfig);

    const res = await testDs(testConfig);
    if (res.code === 0) {
      ElMessage.success('连接测试成功');
    } else {
      ElMessage.error(res.msg || '连接测试失败');
    }
  } catch (error) {
    console.error('测试连接失败:', error);
    ElMessage.error('表单验证失败');
  }
}

// 修改保存方法
const handleSave = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()

    // 创建一个新的空对象，避免出现数字索引键问题
    let config = {};

    // 手动赋值所有字段，而不是使用解构，防止数据库类型出现字符拆分
    config.name = configForm.value.name;
    config.type = configForm.value.type;

    // 确保dm7类型能够正常使用，防止出现字符拆分
    if (typeof config.type === 'object' && config.type.length && !config.type.toLowerCase) {
      // 如果type是字符数组，将其连接为字符串
      config.type = Array.from(config.type).join('');
      console.log('修正后的数据库类型:', config.type);
    }

    config.host = configForm.value.host;
    config.port = configForm.value.port;
    config.dbName = configForm.value.dbName;
    config.username = configForm.value.username;
    config.password = configForm.value.password;
    config.status = configForm.value.status;
    config.validationQuery = configForm.value.validationQuery;
    config.initialSize = configForm.value.initialSize;
    config.maxActive = configForm.value.maxActive;
    config.minIdle = configForm.value.minIdle;
    config.maxWait = configForm.value.maxWait;
    config.testOnBorrow = configForm.value.testOnBorrow;
    config.testOnReturn = configForm.value.testOnReturn;
    config.testWhileIdle = configForm.value.testWhileIdle;

    // 如果是编辑模式，添加ID
    if (editingConfig.value && configForm.value.id) {
      config.id = configForm.value.id;
    }

    // 获取正确的数据库类型
    const dbType = typeof config.type === 'string' ? config.type : (config.type ? String(config.type) : '');

    // 构建 URL
    if (isFileSystem.value) {
      if (configForm.value.type === 'file') {
        config.url = `file://${configForm.value.path}`;
        config.path = configForm.value.path;
      } else {
        // 对于 MinIO/S3/OSS 等对象存储
        if (['minio', 's3', 'oss'].includes(configForm.value.type)) {
          config.url = `${urlPrefixMap[configForm.value.type]}${configForm.value.endpoint}`;
          config.endpoint = configForm.value.endpoint;
          config.bucket = configForm.value.bucket;
          config.accessKey = configForm.value.accessKey;
          config.secretKey = configForm.value.secretKey;
          if (configForm.value.type === 's3') {
            config.region = configForm.value.region;
          }
        } else {
          config.url = `${urlPrefixMap[configForm.value.type]}${configForm.value.host}:${configForm.value.port}`;
          if (configForm.value.type === 'hdfs') {
            config.namenode = configForm.value.namenode;
          }
        }
      }
    } else {
      // 特殊处理某些数据库的 URL 格式
      const host = configForm.value.host;
      const port = configForm.value.port;
      const dbName = configForm.value.dbName;

      switch (dbType) {
        case 'oracle':
          config.url = `${urlPrefixMap.oracle}${host}:${port}:${dbName}`;
          break;
        case 'sqlserver':
          config.url = `${urlPrefixMap.sqlserver}${host}:${port};databaseName=${dbName}`;
          break;
        case 'mongodb':
        case 'redis':
        case 'elasticsearch':
        case 'cassandra':
        case 'neo4j':
          config.url = `${urlPrefixMap[dbType]}${host}:${port}/${dbName}`;
          break;
        default:
          // 标准 JDBC URL 格式
          const prefix = urlPrefixMap[dbType] || 'jdbc:dm://';
          config.url = `${prefix}${host}:${port}/${dbName}`;
      }
    }
   // 输出配置对象用于检查
    console.log('保存数据源配置:', config);

    const saveApi = editingConfig.value ? updateDs : createDs;
    const res = await saveApi(config);
    if (res.code === 0) {
      ElMessage.success(editingConfig.value ? '更新成功' : '创建成功');
      configDialogVisible.value = false;
      loadData();
    } else {
      ElMessage.error(res.msg || (editingConfig.value ? '更新失败' : '创建失败'));
    }
  } catch (error) {
    console.error('保存失败:', error);
    ElMessage.error('表单验证失败');
  }
}


// 添加文件系统类型判断

const isFileSystem = computed(() => {
 const fileTypes = ['ftp', 'sftp', 'hdfs', 'minio', 's3', 'oss', 'file']
 return fileTypes.includes(configForm.value.type)
})


// 修改表单验证规则

const rules = computed(() => {
 const baseRules = {
   name: [{required: true, message: '请输入连接名称', trigger: 'blur'}],
   status: [{required: true, message: '请选择连接状态', trigger: 'change'}]
 }

 // 根据数据源类型设置不同的验证规则
 switch (configForm.value.type) {
     // MongoDB - 主机必填，端口可选(默认27017)，数据库必填，用户名密码可选
   case 'mongodb':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }


     // Redis - 主机必填，端口可选(默认6379)，密码可选，数据库索引可选
   case 'redis':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库索引', trigger: 'blur'}]
     }


     // Elasticsearch - 主机必填，端口可选(默认9200)，用户名密码可选
   case 'elasticsearch':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }


     // Cassandra - 主机必填，端口可选(默认9042)，用户名密码可选，keyspace可选
   case 'cassandra':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入keyspace', trigger: 'blur'}]
     }


     // Hive - 主机必填，端口可选(默认10000)，用户名密码必填
   case 'hive':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: true, message: '请输入密码', trigger: 'blur'}]
     }


     // Impala - 主机必填，端口可选(默认21050)，用户名密码可选
   case 'impala':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }


     // StarRocks - 主机必填，端口可选(默认9030)，用户名密码必填
   case 'starrocks':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: true, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: true, message: '请输入密码', trigger: 'blur'}]
     }


     // ClickHouse - 主机必填，端口可选(默认8123)，用户名密码可选
   case 'clickhouse':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }


     // FTP/SFTP - 主机必填，端口可选，用户名密码可选
   case 'ftp':
   case 'sftp':
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }


     // HDFS - NameNode地址必填，端口可选(默认9000)
   case 'hdfs':
     return {
       ...baseRules,
       namenode: [{required: true, message: '请输入NameNode地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}]
     }


     // MinIO/S3/OSS - Endpoint必填，AccessKey/SecretKey必填，bucket必填，region可选(仅S3)
   case 'minio':
   case 's3':
   case 'oss':
     const cloudRules = {
       ...baseRules,
       endpoint: [{required: true, message: '请输入Endpoint', trigger: 'blur'}],
       accessKey: [{required: true, message: '请输入AccessKey', trigger: 'blur'}],
       secretKey: [{required: true, message: '请输入SecretKey', trigger: 'blur'}],
       bucket: [{required: true, message: '请输入存储桶名称', trigger: 'blur'}]
     }
     if (configForm.value.type === 's3') {
       cloudRules.region = [{required: false, message: '请输入区域', trigger: 'blur'}]
     }
     return cloudRules


     // 关系型数据库 - 主机必填，端口可选(使用默认端口)，数据库名必填，用户名密码必填
   default:
     return {
       ...baseRules,
       host: [{required: true, message: '请输入主机地址', trigger: 'blur'}],
       port: [{required: false, message: '请输入端口号', trigger: 'blur'}],
       dbName: [{required: false, message: '请输入数据库名', trigger: 'blur'}],
       username: [{required: false, message: '请输入用户名', trigger: 'blur'}],
       password: [{required: false, message: '请输入密码', trigger: 'blur'}]
     }
 }
})


// 查看库表

const handleViewTables = (row) => {
  if (!row || !row.id) {
    ElMessage.warning('无效的数据源')
    return
  }

  // 根据数据源类型确定要导航到的路由
  let routeName = 'DatabaseTables'; // 默认路由名称

  // 判断数据源类型并映射到相应的路由
  if (isFileSystemType(row.type)) {
    routeName = 'FileSystemBrowser';
  } else if (isMessageQueueType(row.type)) {
    routeName = 'MessageQueueTopics';
  } else if (isNoSQLType(row.type)) {
    routeName = 'NoSQLCollections';
  } else if (isGraphDBType(row.type)) {
    routeName = 'GraphDBVisualizer';
  } else if (isTimeSeriesType(row.type)) {
    routeName = 'TimeSeriesBrowser';
  }

  router.push({
    name: routeName,
    params: { id: row.id },
    query: {
      name: row.name,
      type: row.type
    }
  })
}

// 检查是否为文件系统类型
const isFileSystemType = (type) => {
  return ['ftp', 'sftp', 'hdfs', 'minio', 's3', 'oss', 'file'].includes(type);
}

// 检查是否为消息队列类型
const isMessageQueueType = (type) => {
  return ['kafka', 'rabbitmq', 'rocketmq'].includes(type);
}

// 检查是否为NoSQL类型
const isNoSQLType = (type) => {
  return ['mongodb', 'redis', 'elasticsearch', 'cassandra', 'couchdb', 'couchbase'].includes(type);
}

// 检查是否为图数据库类型
const isGraphDBType = (type) => {
  return ['neo4j', 'nebula', 'hugegraph', 'janusgraph'].includes(type);
}

// 检查是否为时序数据库类型
const isTimeSeriesType = (type) => {
  return ['influxdb', 'tdengine', 'opentsdb', 'timescaledb'].includes(type);
}

// 根据数据源类型获取查看按钮文本
const getViewButtonText = (type) => {
  if (isFileSystemType(type)) {
    return '浏览文件';
  } else if (isMessageQueueType(type)) {
    return '查看主题';
  } else if (isNoSQLType(type)) {
    if (type === 'mongodb') {
      return '查看集合';
    } else if (type === 'redis') {
      return '查看键空间';
    } else if (type === 'elasticsearch') {
      return '查看索引';
    } else {
      return '查看数据';
    }
  } else if (isGraphDBType(type)) {
    return '查看图';
  } else if (isTimeSeriesType(type)) {
    return '查看度量';
  } else {
    return '查看库表';
  }
}

// 新建连接

const handleAdd = () => {
 selectedDb.value = ''
 dbSelectorVisible.value = true
 editingConfig.value = false
}


// 连接监控

const monitoringDialogVisible = ref(false)

const monitoringData = ref({
 activeConnections: 0,
 poolUsage: 0,
 waitingConnections: 0

})


const handleConnMonitor = async (row) => {
 monitoringDialogVisible.value = true
 const metrics = await getConnectionMetrics(row.id)
 if (metrics) {
   monitoringData.value = metrics
 }

}


// 性能分析

const performanceDialogVisible = ref(false)

const performanceData = ref({
 sqlStats: []

})


const handlePerformance = async (row) => {
 performanceDialogVisible.value = true
 const metrics = await getPerformanceMetrics(row.id)
 if (metrics) {
   performanceData.value = metrics
 }

}


// 高级查询相关

const advancedQueryVisible = ref(false)

const advancedQueryForm = ref({
 dbTypes: [],
 status: '',
 timeRange: []

})


// 显示高级查询对话框

const showAdvancedQuery = () => {
 advancedQueryVisible.value = true
}


// 重置高级查询表单

const resetAdvancedQuery = () => {
 advancedQueryForm.value = {
   dbTypes: [],
   status: '',
   timeRange: []
 }
}


// 处理高级查询

const handleAdvancedQuery = () => {
 const params = {
   ...advancedQueryForm.value,
   current: 1, // 重置页码
 }
 loadData(params)
 advancedQueryVisible.value = false
}


// 监控相关

const monitorVisible = ref(false)

const currentDatasource = ref(null)


// 添加监控处理方法

const handleMonitor = (row) => {
 currentDatasource.value = row
 monitorVisible.value = true
}


// 修改测试连接方法
const handleTest = async (row) => {
  // 创建加载提示
  const loading = ElLoading.service({
    text: '正在测试连接...'
  })
 try {
    const res = await testDs(row)  // 使用 testDs 而不是 testConnection
    if (res.code === 0) {
      ElMessage.success('连接测试成功')
    } else {
      ElMessage.error(res.msg || '连接测试失败')
    }
  } catch (error) {
    console.error('测试连接失败:', error)
    ElMessage.error('表单验证失败')
  } finally {
    // 确保关闭加载提示
    loading.close()
  }
}


onMounted(() => {
 loadData()
 loadStatistics()
})

</script>


<style scoped>

.card-header {
 display: flex;
 justify-content: space-between;
 align-items: center;

}


.header-left {
 display: flex;
 gap: 10px;
 align-items: center;
 margin-left: -20px;

}


.header-right {
 display: flex;
 gap: 10px;

}


.monitor-info {
 display: flex;
 gap: 16px;

}


.db-selector {
 padding: 20px;
 max-height: 70vh;
 overflow-y: auto;
}


.db-group {
 margin-bottom: 24px;
}


.db-group h3 {
 margin: 0 0 16px 0;
 color: #606266;
 font-size: 16px;
 position: sticky;
 top: 0;
 background-color: white;
 padding: 8px 0;
 z-index: 1;
}


.db-list {
 display: grid;
 grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
 gap: 16px;
}


.db-item {
 display: flex;
 flex-direction: column;
 align-items: center;
 padding: 16px;
 border: 1px solid #dcdfe6;
 border-radius: 4px;
 cursor: pointer;
 transition: all 0.3s;
}


.db-item:hover {
 border-color: #409eff;
 box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}


.db-item.active {
 border-color: #409eff;
 background-color: #ecf5ff;
}


.db-item img {
 width: 48px;
 height: 48px;
 margin-bottom: 12px;
 object-fit: contain;
}


.db-item span {
 font-size: 14px;
 color: #606266;
 text-align: center;
 word-break: keep-all;
}


.dialog-footer {
 display: flex;
 justify-content: flex-end;
 gap: 12px;

}


.pagination-container {
 display: flex;
 justify-content: flex-end;
 margin-top: 20px;

}

.monitoring-content {
 .metric {
   text-align: center;
   padding: 20px;

   .number {
     font-size: 36px;
     font-weight: bold;
     color: #409EFF;
   }

   .label {
     margin-left: 8px;
     font-size: 16px;
     color: #909399;
   }
 }

}


.performance-content {
 min-height: 400px;

}


.monitor-header {
 display: flex;
 justify-content: space-between;
 align-items: center;
 margin-bottom: 20px;
 padding: 0 20px;

}


.monitor-info {
 display: flex;
 align-items: center;
 gap: 16px;

}


.monitor-chart {
 height: 300px;
 margin: 20px 0;

}


.el-descriptions {
 margin: 20px 0;

}

.dataset-database {
  .statistics-area {
    margin-bottom: 20px;
    
    .stat-card {
      position: relative;
      height: 120px;
      padding: 20px;
      overflow: hidden;
      border-radius: 8px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
      }
      
      .stat-value {
        font-size: 36px;
        font-weight: bold;
        color: white;
        margin-bottom: 8px;
        
        .stat-unit {
          font-size: 14px;
          font-weight: normal;
          margin-left: 2px;
        }
      }
      
      .stat-label {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.8);
      }
      
      .stat-trend {
        font-size: 12px;
        margin-top: 8px;
        color: rgba(255, 255, 255, 0.8);
        
        .up {
          color: #fff;
        }
        
        .down {
          color: #fff;
        }
      }
      
      .stat-icon {
        position: absolute;
        right: 20px;
        top: 20px;
        font-size: 48px;
        opacity: 0.2;
        color: white;
      }
      
      &.total {
        background: linear-gradient(135deg, #409EFF 0%, #85c5ff 100%);
      }
      
      &.success {
        background: linear-gradient(135deg, #67C23A 0%, #b3e19d 100%);
      }
      
      &.running {
        background: linear-gradient(135deg, #E6A23C 0%, #f3d19e 100%);
      }
      
      &.error {
        background: linear-gradient(135deg, #F56C6C 0%, #fab6b6 100%);
      }
    }
  }
}

</style>


