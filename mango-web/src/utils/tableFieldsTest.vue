<template>
  <div class="table-fields-test">
    <h2>表字段获取测试</h2>
    
    <el-card>
      <template #header>
        <div class="card-header">
          <span>配置信息</span>
        </div>
      </template>
      
      <el-form :model="form" label-width="120px">
        <el-form-item label="数据源ID">
          <el-input v-model="form.dataSourceId" placeholder="请输入数据源ID" />
        </el-form-item>
        
        <el-form-item label="表名">
          <el-input v-model="form.tableName" placeholder="请输入表名" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="fetchFields">获取字段</el-button>
          <el-button type="success" @click="fetchFieldsSync">同步获取字段</el-button>
          <el-button @click="useTestData">使用测试数据</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-divider />
    
    <el-card>
      <template #header>
        <div class="card-header">
          <span>表字段信息</span>
          <el-tag v-if="tableFields.length">共 {{ tableFields.length }} 个字段</el-tag>
        </div>
      </template>
      
      <div v-if="loading" class="loading-container">
        <el-skeleton :rows="5" animated />
      </div>
      
      <div v-else-if="error" class="error-message">
        <el-alert
          :title="error"
          type="error"
          :closable="false"
          show-icon
        />
      </div>
      
      <el-empty v-else-if="!tableFields.length" description="暂无数据" />
      
      <el-table v-else :data="tableFields" border stripe>
        <el-table-column prop="name" label="字段名" />
        <el-table-column prop="type" label="数据类型" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="主键">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="success">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="可空">
          <template #default="{ row }">
            <span>{{ row.nullable !== false ? '是' : '否' }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    
    <el-divider />
    
    <el-card>
      <template #header>
        <div class="card-header">
          <span>字段获取方式对比</span>
        </div>
      </template>
      
      <div class="comparison">
        <div class="comparison-item">
          <h3>同步获取方式 (新)</h3>
          <p>优点：</p>
          <ul>
            <li>直接调用API获取字段，不需等待延迟加载</li>
            <li>支持Promise/async-await方式调用</li>
            <li>包含字段缓存，避免重复请求</li>
          </ul>
          <pre>const fields = await getTableFieldsSync(targetTable);</pre>
        </div>
        
        <div class="comparison-item">
          <h3>异步获取方式 (原)</h3>
          <p>优点：</p>
          <ul>
            <li>兼容旧代码，保持API稳定性</li>
            <li>自动使用缓存中的数据</li>
          </ul>
          <pre>const fields = getMockTableFields(tableId);</pre>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { defineComponent, ref, reactive } from 'vue'
import { getTableFields2, fetchRealTableFields, getMockFieldsByTableName } from './tableFieldsHelper'
import { getTableFieldsSync, setTargetTable } from './tableBridge'

export default defineComponent({
  name: 'TableFieldsTest',
  
  setup() {
    const form = reactive({
      dataSourceId: '',
      tableName: ''
    })
    
    const tableFields = ref([])
    const loading = ref(false)
    const error = ref('')
    
    const fetchFields = async () => {
      if (!form.dataSourceId || !form.tableName) {
        error.value = '请输入数据源ID和表名'
        return
      }
      
      loading.value = true
      error.value = ''
      tableFields.value = []
      
      try {
        // 使用我们的辅助函数
        const result = await fetchRealTableFields(form.dataSourceId, form.tableName)
        
        if (result) {
          tableFields.value = result
        } else {
          error.value = '未能获取到表字段信息'
        }
      } catch (err) {
        console.error('获取表字段失败:', err)
        error.value = `获取字段失败: ${err.message || '未知错误'}`
      } finally {
        loading.value = false
      }
    }
    
    const fetchFieldsSync = async () => {
      if (!form.dataSourceId || !form.tableName) {
        error.value = '请输入数据源ID和表名'
        return
      }
      
      loading.value = true
      error.value = ''
      tableFields.value = []
      
      try {
        // 构造表信息对象
        const tableInfo = {
          dataSourceId: form.dataSourceId,
          name: form.tableName
        }
        
        // 使用同步获取字段函数
        console.log('使用同步方式获取表字段')
        const startTime = Date.now()
        
        const fields = await getTableFieldsSync(tableInfo)
        const endTime = Date.now()
        
        if (fields && fields.length > 0) {
          tableFields.value = fields
          console.log(`同步获取字段成功，耗时: ${endTime - startTime}ms，获取到 ${fields.length} 个字段`)
        } else {
          error.value = '未能获取到表字段信息'
        }
      } catch (err) {
        console.error('同步获取表字段失败:', err)
        error.value = `获取字段失败: ${err.message || '未知错误'}`
      } finally {
        loading.value = false
      }
    }
    
    const useTestData = () => {
      if (!form.tableName) {
        error.value = '请输入表名'
        return
      }
      
      loading.value = true
      error.value = ''
      
      try {
        // 使用模拟数据
        const mockFields = getMockFieldsByTableName(form.tableName)
        tableFields.value = mockFields || []
        
        if (!tableFields.value.length) {
          error.value = '未找到匹配的模拟数据'
        }
      } catch (err) {
        console.error('获取模拟数据失败:', err)
        error.value = `获取模拟数据失败: ${err.message || '未知错误'}`
      } finally {
        loading.value = false
      }
    }
    
    return {
      form,
      tableFields,
      loading,
      error,
      fetchFields,
      fetchFieldsSync,
      useTestData
    }
  }
})
</script>

<style scoped>
.table-fields-test {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.loading-container {
  padding: 20px 0;
}

.error-message {
  margin: 20px 0;
}

.comparison {
  display: flex;
  gap: 20px;
}

.comparison-item {
  flex: 1;
  padding: 15px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.comparison-item h3 {
  margin-top: 0;
  color: #409eff;
}

.comparison-item pre {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}
</style> 