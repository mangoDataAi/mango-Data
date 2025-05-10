<template>
  <div 
    class="nosql-collections"
    v-loading.fullscreen.lock="fullscreenLoading"
    :element-loading-text="loadingText"
    element-loading-background="rgba(0, 0, 0, 0.8)"
  >
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-button link @click="router.back()">
              <el-icon><ArrowLeft /></el-icon>
              返回
            </el-button>
            <el-divider direction="vertical" />
            <span class="datasource-info">
              {{ route.query.name }} ({{ dataSourceType }})
            </span>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="refreshCollections">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
            <el-button type="primary" @click="openCreateCollectionDialog">
              <el-icon><Plus /></el-icon> 创建集合
            </el-button>
            <el-button type="danger" @click="showDeleteConfirm" :disabled="!selectedCollection">
              <el-icon><Delete /></el-icon> 删除集合
            </el-button>
          </div>
        </div>
      </template>

      <div class="content-wrapper">
        <!-- 左侧集合列表 -->
        <div class="left-area">
          <div class="search-box">
            <el-input
              v-model="searchText"
              placeholder="搜索集合"
              :prefix-icon="Search"
              clearable
              @clear="filterCollections"
              @input="filterCollections"
            />
          </div>
          <div class="collections-list">
            <el-scrollbar>
              <div 
                v-for="collection in filteredCollections" 
                :key="collection.name"
                class="collection-item"
                :class="{ 'active': selectedCollection && selectedCollection.name === collection.name }"
                @click="selectCollection(collection)"
              >
                <el-icon><Collection /></el-icon>
                <span class="collection-name">{{ collection.name }}</span>
                <span class="collection-count">{{ formatCount(collection.count) }}</span>
              </div>
            </el-scrollbar>
          </div>
        </div>

        <!-- 右侧内容区域 -->
        <div class="right-area">
          <template v-if="selectedCollection">
            <el-tabs v-model="activeTab">
              <el-tab-pane label="数据浏览" name="data">
                <div class="data-browser">
                  <!-- 操作工具栏 -->
                  <div class="toolbar">
                    <el-input
                      v-model="queryFilter"
                      placeholder="过滤条件 (JSON)"
                      style="width: 300px;"
                    />
                    <el-button type="primary" @click="executeQuery">查询</el-button>
                    <el-button @click="createNewDocument">新增文档</el-button>
                    <el-button 
                      type="danger" 
                      :disabled="!selectedDocuments.length"
                      @click="deleteSelectedDocuments"
                    >
                      删除选中 ({{ selectedDocuments.length }})
                    </el-button>
                  </div>

                  <!-- 数据表格 -->
                  <div class="data-table">
                    <!-- MongoDB数据库表格 -->
                    <template v-if="dataSourceType === 'mongodb'">
                      <el-table
                        :data="documents"
                        style="width: 100%"
                        border
                        @selection-change="handleSelectionChange"
                      >
                        <el-table-column type="selection" width="55" />
                        <el-table-column label="ID" width="100">
                          <template #default="{ row }">
                            <el-tooltip :content="row._id" placement="top">
                              <span>{{ row._id }}</span>
                            </el-tooltip>
                          </template>
                        </el-table-column>
                        
                        <!-- 动态字段列 -->
                        <el-table-column v-for="field in commonFields" :key="field" :prop="field" :label="field" />
                        
                        <el-table-column label="操作" width="180">
                          <template #default="{ row }">
                            <el-button link @click="viewDocument(row)">查看</el-button>
                            <el-button link @click="editDocument(row)">编辑</el-button>
                            <el-button link type="danger" @click="deleteDocument(row)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </template>

                    <!-- Redis键值对展示 -->
                    <template v-else-if="dataSourceType === 'redis'">
                      <el-table
                        :data="documents"
                        style="width: 100%"
                        border
                        @selection-change="handleSelectionChange"
                      >
                        <el-table-column type="selection" width="55" />
                        <el-table-column prop="key" label="键" width="220" />
                        <el-table-column prop="type" label="类型" width="100">
                          <template #default="{ row }">
                            <el-tag>{{ row.type || '字符串' }}</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="value" label="值" min-width="150">
                          <template #default="{ row }">
                            <span v-if="row.value !== undefined">{{ row.value }}</span>
                            <el-tag v-else type="info">点击查看值</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="ttl" label="过期时间" width="120">
                          <template #default="{ row }">
                            <span>{{ row.ttl === -1 ? '永不过期' : formatTTL(row.ttl) }}</span>
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="200">
                          <template #default="{ row }">
                            <el-button link @click="viewDocument(row)">查看详情</el-button>
                            <el-button link @click="editDocument(row)">编辑</el-button>
                            <el-button link type="danger" @click="deleteDocument(row)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </template>
                    
                    <!-- 默认表格展示（其他NoSQL数据库） -->
                    <template v-else>
                      <el-table
                        :data="documents"
                        style="width: 100%"
                        border
                        @selection-change="handleSelectionChange"
                      >
                        <el-table-column type="selection" width="55" />
                        <el-table-column label="ID" width="100">
                          <template #default="{ row }">
                            <el-tooltip :content="row._id" placement="top">
                              <span>{{ row._id }}</span>
                            </el-tooltip>
                          </template>
                        </el-table-column>
                        
                        <!-- 动态字段列 -->
                        <el-table-column v-for="field in commonFields" :key="field" :prop="field" :label="field" />
                        
                        <el-table-column label="操作" width="180">
                          <template #default="{ row }">
                            <el-button link @click="viewDocument(row)">查看</el-button>
                            <el-button link @click="editDocument(row)">编辑</el-button>
                            <el-button link type="danger" @click="deleteDocument(row)">删除</el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                    </template>
                  </div>

                  <!-- 分页 -->
                  <div class="pagination">
                    <el-pagination
                      v-model:current-page="currentPage"
                      v-model:page-size="pageSize"
                      :page-sizes="[10, 20, 50, 100]"
                      layout="total, sizes, prev, pager, next, jumper"
                      :total="totalDocuments"
                      @size-change="handleSizeChange"
                      @current-change="handleCurrentChange"
                    />
                  </div>
                </div>
              </el-tab-pane>

              <el-tab-pane label="结构信息" name="schema">
                <div v-if="selectedCollection" class="schema-info">
                  <h3>集合信息</h3>
                  <div class="collection-info">
                    <div class="info-item">
                      <span class="label">名称：</span>
                      <span>{{ selectedCollection.name }}</span>
                    </div>
                    <div class="info-item">
                      <span class="label">文档数量：</span>
                      <span>{{ selectedCollection.count || 0 }}</span>
                    </div>
                    <div v-if="selectedCollection.size" class="info-item">
                      <span class="label">大小：</span>
                      <span>{{ selectedCollection.size }}</span>
                    </div>
                  </div>
                  
                  <h3>字段映射</h3>
                  <div class="mappings-container">
                    <pre class="mappings-code">{{ JSON.stringify(selectedCollection.mappings || {}, null, 2) }}</pre>
                  </div>
                </div>
                <div v-else class="empty-state">
                  请先选择一个集合
                </div>
              </el-tab-pane>

              <el-tab-pane label="查询构建器" name="builder">
                <div v-if="selectedCollection" class="query-builder">
                  <div class="builder-header">
                    <h3>构建查询</h3>
                    <el-alert
                      title="使用查询构建器来创建和执行复杂查询"
                      type="info"
                      :closable="false"
                      show-icon
                      style="margin-bottom: 16px;"
                    />
                  </div>

                  <!-- MongoDB 查询构建器 -->
                  <template v-if="dataSourceType === 'mongodb'">
                    <div class="builder-section">
                      <div class="section-title">过滤条件 (Filter)</div>
                      <el-input
                        v-model="queryBuilder.filter"
                        type="textarea"
                        :rows="5"
                        placeholder="输入JSON格式的过滤条件，例如: {&quot;price&quot;: {&quot;$gt&quot;: 100}}"
                      />
                    </div>

                    <div class="builder-section">
                      <div class="section-title">字段选择 (Projection)</div>
                      <el-input
                        v-model="queryBuilder.projection"
                        type="textarea"
                        :rows="3"
                        placeholder="输入JSON格式的字段选择，例如: {&quot;name&quot;: 1, &quot;price&quot;: 1}"
                      />
                    </div>

                    <div class="builder-section">
                      <div class="section-title">排序 (Sort)</div>
                      <el-input
                        v-model="queryBuilder.sort"
                        type="textarea"
                        :rows="3"
                        placeholder="输入JSON格式的排序条件，例如: {&quot;price&quot;: -1}"
                      />
                    </div>

                    <div class="builder-example">
                      <div class="example-title">示例：</div>
                      <div class="example-item">
                        <div>价格大于100的商品：</div>
                        <code>{ "price": { "$gt": 100 } }</code>
                      </div>
                      <div class="example-item">
                        <div>包含特定标签的商品：</div>
                        <code>{ "tags": "electronics" }</code>
                      </div>
                      <div class="example-item">
                        <div>仅返回名称和价格字段：</div>
                        <code>{ "name": 1, "price": 1 }</code>
                      </div>
                      <div class="example-item">
                        <div>按价格降序排序：</div>
                        <code>{ "price": -1 }</code>
                      </div>
                    </div>
                  </template>

                  <!-- Elasticsearch 查询构建器 -->
                  <template v-else-if="dataSourceType === 'elasticsearch'">
                    <div class="builder-section">
                      <div class="section-title">查询 (Query)</div>
                      <el-input
                        v-model="queryBuilder.filter"
                        type="textarea"
                        :rows="5"
                        placeholder="输入JSON格式的查询，例如: {&quot;match&quot;: {&quot;title&quot;: &quot;搜索词&quot;}}"
                      />
                    </div>

                    <div class="builder-example">
                      <div class="example-title">示例：</div>
                      <div class="example-item">
                        <div>精确匹配字段：</div>
                        <code>{ "term": { "status": "active" } }</code>
                      </div>
                      <div class="example-item">
                        <div>全文搜索：</div>
                        <code>{ "match": { "title": "搜索词" } }</code>
                      </div>
                      <div class="example-item">
                        <div>范围查询：</div>
                        <code>{ "range": { "price": { "gte": 10, "lte": 100 } } }</code>
                      </div>
                    </div>
                  </template>

                  <!-- Redis 查询构建器 -->
                  <template v-else-if="dataSourceType === 'redis'">
                    <div class="builder-section">
                      <div class="section-title">键模式 (Pattern)</div>
                      <el-input
                        v-model="queryBuilder.pattern"
                        placeholder="输入键模式，例如: user*"
                      />
                    </div>

                    <div class="builder-section">
                      <div class="section-title">类型 (Type)</div>
                      <el-select v-model="queryBuilder.type" style="width: 100%">
                        <el-option label="全部类型" value="" />
                        <el-option label="字符串 (string)" value="string" />
                        <el-option label="哈希 (hash)" value="hash" />
                        <el-option label="列表 (list)" value="list" />
                        <el-option label="集合 (set)" value="set" />
                        <el-option label="有序集合 (zset)" value="zset" />
                      </el-select>
                    </div>

                    <div class="builder-example">
                      <div class="example-title">示例模式：</div>
                      <div class="example-item">
                        <div>所有键：</div>
                        <code>*</code>
                      </div>
                      <div class="example-item">
                        <div>用户相关的键：</div>
                        <code>user:*</code>
                      </div>
                      <div class="example-item">
                        <div>特定ID的数据：</div>
                        <code>data:123:*</code>
                      </div>
                    </div>
                  </template>

                  <!-- 默认查询构建器 -->
                  <template v-else>
                    <div class="builder-section">
                      <div class="section-title">查询条件</div>
                      <el-input
                        v-model="queryBuilder.filter"
                        type="textarea"
                        :rows="5"
                        placeholder="输入JSON格式的查询条件"
                      />
                    </div>
                  </template>

                  <div class="builder-actions">
                    <el-button @click="resetQueryBuilder">重置</el-button>
                    <el-button type="primary" @click="executeBuilderQuery">执行查询</el-button>
                  </div>
                </div>
                <div v-else class="empty-state">
                  请先选择一个集合
                </div>
              </el-tab-pane>
            </el-tabs>
          </template>
          <div v-else class="no-collection-selected">
            <el-empty description="请选择一个集合" />
          </div>
        </div>
      </div>
    </el-card>

    <!-- 创建集合对话框 -->
    <el-dialog
      v-model="createCollectionDialogVisible"
      title="创建新集合"
      width="500px"
    >
      <el-form ref="createCollectionFormRef" :model="createCollectionForm" :rules="createCollectionRules" label-width="120px">
        <el-form-item label="集合名称" prop="name">
          <el-input v-model="createCollectionForm.name" placeholder="请输入集合名称" />
        </el-form-item>
        <!-- MongoDB特有配置 -->
        <template v-if="dataSourceType === 'mongodb'">
          <el-form-item label="是否上限集合">
            <el-switch v-model="createCollectionForm.capped" />
          </el-form-item>
          <template v-if="createCollectionForm.capped">
            <el-form-item label="最大文档数" prop="maxDocuments">
              <el-input-number v-model="createCollectionForm.maxDocuments" :min="1" />
            </el-form-item>
            <el-form-item label="最大大小(字节)" prop="size">
              <el-input-number v-model="createCollectionForm.size" :min="1" />
            </el-form-item>
          </template>
        </template>
        
        <!-- Redis特有配置 -->
        <template v-if="dataSourceType === 'redis'">
          <el-form-item label="数据类型" prop="dataType">
            <el-select v-model="createCollectionForm.dataType" placeholder="请选择数据类型" style="width: 100%">
              <el-option label="字符串 (String)" value="string" />
              <el-option label="列表 (List)" value="list" />
              <el-option label="集合 (Set)" value="set" />
              <el-option label="有序集合 (ZSet)" value="zset" />
              <el-option label="哈希表 (Hash)" value="hash" />
            </el-select>
            <div class="el-form-item-helper">选择要创建的Redis数据类型</div>
          </el-form-item>
          
          <el-form-item label="键名" prop="name">
            <el-input v-model="createCollectionForm.name" placeholder="请输入键名" />
            <div class="el-form-item-helper" v-if="createCollectionForm.dataType === 'string'">
              建议命名: user:{id}, product:{id}, session:{sid}, count:{entity}
            </div>
            <div class="el-form-item-helper" v-else-if="createCollectionForm.dataType === 'list'">
              建议命名: timeline:{user}, messages:{channel}, queue:{name}, tasks:{type}
            </div>
            <div class="el-form-item-helper" v-else-if="createCollectionForm.dataType === 'set'">
              建议命名: tags:{item}, friends:{userid}, permissions:{role}
            </div>
            <div class="el-form-item-helper" v-else-if="createCollectionForm.dataType === 'zset'">
              建议命名: leaderboard:{game}, ranking:{contest}, score:{event}
            </div>
            <div class="el-form-item-helper" v-else-if="createCollectionForm.dataType === 'hash'">
              建议命名: user:{id}:info, product:{id}:details, config:{module}
            </div>
            <div class="form-example-actions" v-if="createCollectionForm.dataType">
              <el-button size="small" type="text" @click="fillExampleKeyName">使用示例键名</el-button>
            </div>
          </el-form-item>
          
          <!-- 字符串类型 -->
          <template v-if="createCollectionForm.dataType === 'string'">
            <div class="redis-form-section">
              <div class="redis-form-section-title">字符串数据</div>
              <div class="redis-form-tips">
                <p>字符串是Redis最基本的数据类型，可以存储文本、数字或二进制数据。</p>
                <p>常用场景：用户会话、缓存数据、计数器等</p>
              </div>
            </div>
            
            <el-form-item label="键值" prop="stringValue">
              <el-input v-model="createCollectionForm.stringValue" type="textarea" :rows="3" placeholder="请输入字符串值" />
              <div class="form-example-actions">
                <el-button size="small" @click="fillStringExample">填充示例</el-button>
              </div>
            </el-form-item>
          </template>
          
          <!-- 列表类型 -->
          <template v-if="createCollectionForm.dataType === 'list'">
            <div class="redis-form-section">
              <div class="redis-form-section-title">列表数据</div>
              <div class="redis-form-tips">
                <p>列表是简单的字符串列表，按照插入顺序排序。可以添加元素到头部或尾部。</p>
                <p>常用场景：消息队列、最新动态、任务列表等</p>
              </div>
            </div>
            
            <el-form-item label="列表元素" prop="listValues">
              <el-input v-model="createCollectionForm.listValues" type="textarea" :rows="4" placeholder="每行输入一个元素" />
              <div class="el-form-item-helper">每行一个元素，会按顺序添加到列表中</div>
              <div class="form-example-actions">
                <el-button size="small" @click="fillListExample">填充示例</el-button>
              </div>
            </el-form-item>
          </template>
          
          <!-- 集合类型 -->
          <template v-if="createCollectionForm.dataType === 'set'">
            <div class="redis-form-section">
              <div class="redis-form-section-title">集合数据</div>
              <div class="redis-form-tips">
                <p>集合是字符串的无序集合，成员是唯一的（不允许重复）。</p>
                <p>常用场景：标签系统、唯一性检查、共同好友等</p>
              </div>
            </div>
            
            <el-form-item label="集合元素" prop="setValues">
              <el-input v-model="createCollectionForm.setValues" type="textarea" :rows="4" placeholder="每行输入一个元素" />
              <div class="el-form-item-helper">每行一个元素，重复元素会被自动去除</div>
              <div class="form-example-actions">
                <el-button size="small" @click="fillSetExample">填充示例</el-button>
              </div>
            </el-form-item>
          </template>
          
          <!-- 有序集合类型 -->
          <template v-if="createCollectionForm.dataType === 'zset'">
            <div class="redis-form-section">
              <div class="redis-form-section-title">有序集合数据</div>
              <div class="redis-form-tips">
                <p>有序集合与普通集合类似，但每个成员关联一个分数，通过分数排序。</p>
                <p>常用场景：排行榜、权重队列、优先级任务等</p>
                <p>输入格式: <code>分数 元素名称</code> (例如: <code>100 用户A</code>)</p>
              </div>
            </div>
            
            <el-form-item label="有序集合元素" prop="zsetValues">
              <el-input v-model="createCollectionForm.zsetValues" type="textarea" :rows="5" placeholder="每行输入一个 分数 元素，以空格分隔" />
              <div class="el-form-item-helper">格式：分数 元素，例如: 1 元素1</div>
              <div class="form-example-actions">
                <el-button size="small" @click="fillZSetExample">填充示例</el-button>
              </div>
            </el-form-item>
          </template>
          
          <!-- 哈希表类型 -->
          <template v-if="createCollectionForm.dataType === 'hash'">
            <div class="redis-form-section">
              <div class="redis-form-section-title">哈希表数据</div>
              <div class="redis-form-tips">
                <p>哈希表是字段和值的映射表，类似于JSON对象或字典。</p>
                <p>常用场景：用户信息、商品属性、配置项等</p>
                <p>输入格式: <code>字段名 字段值</code> (例如: <code>username 张三</code>)</p>
              </div>
            </div>
            
            <el-form-item label="哈希表字段" prop="hashValues">
              <el-input v-model="createCollectionForm.hashValues" type="textarea" :rows="5" placeholder="每行输入一个 字段 值，以空格分隔" />
              <div class="el-form-item-helper">格式：字段 值，例如: field1 value1</div>
              <div class="form-example-actions">
                <el-button size="small" @click="fillHashExample">填充示例</el-button>
              </div>
            </el-form-item>
          </template>
          
          <el-form-item label="过期时间(秒)" prop="ttl">
            <el-input-number v-model="createCollectionForm.ttl" :min="-1" :step="60" />
            <div class="el-form-item-helper">-1表示永不过期，常用值: 300(5分钟), 3600(1小时), 86400(1天)</div>
            <div class="common-ttl-buttons">
              <el-button size="small" @click="createCollectionForm.ttl = 300">5分钟</el-button>
              <el-button size="small" @click="createCollectionForm.ttl = 3600">1小时</el-button>
              <el-button size="small" @click="createCollectionForm.ttl = 86400">1天</el-button>
              <el-button size="small" @click="createCollectionForm.ttl = -1">永不过期</el-button>
            </div>
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createCollectionDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCollection">创建</el-button>
      </template>
    </el-dialog>

    <!-- 文档操作对话框 -->
    <el-dialog
      v-model="documentDialogVisible"
      :title="documentDialogMode === 'create' ? '新增文档' : (documentDialogMode === 'edit' ? '编辑文档' : '查看文档')"
      width="600px"
    >
      <div class="document-editor-container">
        <div v-if="documentDialogMode === 'create'" class="document-help">
          <el-alert
            title="在下方编辑器中输入JSON格式的文档内容"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <div class="document-example">
            <div class="example-title">示例文档:</div>
            <pre class="example-code">{
  "name": "示例产品",
  "price": 99.99,
  "category": "电子产品",
  "inStock": true,
  "tags": ["热销", "新品"],
  "specs": {
    "color": "黑色",
    "weight": 150
  }
}</pre>
          </div>
        </div>
        
        <el-input
          v-model="currentDocument"
          type="textarea"
          :rows="15"
          :readonly="documentDialogMode === 'view'"
          placeholder="输入JSON格式的文档内容"
        ></el-input>
        
        <div v-if="documentDialogMode !== 'view'" class="document-tips">
          <p>提示:</p>
          <ul>
            <li>确保输入有效的JSON格式</li>
            <li>不需要手动指定_id字段，系统会自动生成</li>
            <li>支持嵌套对象和数组</li>
          </ul>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="documentDialogVisible = false">取消</el-button>
        <el-button v-if="documentDialogMode !== 'view'" type="primary" @click="saveDocument">
          {{ documentDialogMode === 'create' ? '创建' : '保存' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  ArrowLeft, Search, Refresh, Collection, Plus, Delete
} from '@element-plus/icons-vue'
import JsonEditor from '@/components/JsonEditor'
import { 
  getCollections, 
  getCollectionInfo, 
  getDocuments, 
  createCollection,
  deleteCollection,
  createDocument as insertDocument,
  updateDocument,
  deleteDocument as apiDeleteDocument,
  executeCommand,
  getDatabaseStats,
  getCollectionSchema
} from '@/api/nosql'
import { extractIdFromDocument } from '@/utils/mongodb'

const router = useRouter()
const route = useRoute()

// 加载状态
const fullscreenLoading = ref(false)
const loadingText = ref('加载中...')

// 集合列表
const collections = ref([])
const searchText = ref('')
const selectedCollection = ref(null)

// 文档数据
const documents = ref([])
const selectedDocuments = ref([])
const totalDocuments = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const queryFilter = ref('')

// 标签页
const activeTab = ref('data')

// 查询构建器
const queryBuilder = ref({
  filter: '{}',
  projection: '{}',
  sort: '{}',
  pattern: '*',
  type: ''
})

// 对话框
const createCollectionDialogVisible = ref(false)
const createCollectionForm = ref({
  name: '',
  // MongoDB相关字段
  capped: false,
  maxDocuments: 1000,
  size: 1048576, // 1MB
  // Redis相关字段
  dataType: 'string',
  stringValue: '',
  listValues: '',
  setValues: '',
  zsetValues: '',
  hashValues: '',
  ttl: -1 // -1表示永不过期
})
const createCollectionFormRef = ref(null)
const createCollectionRules = {
  name: [
    { required: true, message: '请输入集合名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_:]+$/, message: '集合名称只能包含字母、数字、下划线和冒号', trigger: 'blur' }
  ],
  dataType: [
    { required: true, message: '请选择数据类型', trigger: 'change' }
  ],
  stringValue: [
    { required: true, message: '请输入字符串值', trigger: 'blur', validator: (rule, value, callback) => {
      if (createCollectionForm.value.dataType === 'string' && !value) {
        callback(new Error('请输入字符串值'))
      } else {
        callback()
      }
    }}
  ],
  listValues: [
    { required: true, message: '请输入列表元素', trigger: 'blur', validator: (rule, value, callback) => {
      if (createCollectionForm.value.dataType === 'list' && !value) {
        callback(new Error('请输入至少一个列表元素'))
      } else {
        callback()
      }
    }}
  ],
  setValues: [
    { required: true, message: '请输入集合元素', trigger: 'blur', validator: (rule, value, callback) => {
      if (createCollectionForm.value.dataType === 'set' && !value) {
        callback(new Error('请输入至少一个集合元素'))
      } else {
        callback()
      }
    }}
  ],
  zsetValues: [
    { required: true, message: '请输入有序集合元素', trigger: 'blur', validator: (rule, value, callback) => {
      if (createCollectionForm.value.dataType === 'zset' && !value) {
        callback(new Error('请输入至少一个有序集合元素'))
      } else if (createCollectionForm.value.dataType === 'zset') {
        // 验证格式：每行应该是 分数 元素
        const lines = value.split('\n').filter(line => line.trim() !== '')
        for (const line of lines) {
          const parts = line.trim().split(/\s+/)
          if (parts.length < 2 || isNaN(Number(parts[0]))) {
            callback(new Error('格式错误，请确保每行的格式为: 分数 元素'))
            return
          }
        }
        callback()
      } else {
        callback()
      }
    }}
  ],
  hashValues: [
    { required: true, message: '请输入哈希表字段', trigger: 'blur', validator: (rule, value, callback) => {
      if (createCollectionForm.value.dataType === 'hash' && !value) {
        callback(new Error('请输入至少一个哈希表字段'))
      } else if (createCollectionForm.value.dataType === 'hash') {
        // 验证格式：每行应该是 字段 值
        const lines = value.split('\n').filter(line => line.trim() !== '')
        for (const line of lines) {
          const parts = line.trim().split(/\s+/)
          if (parts.length < 2) {
            callback(new Error('格式错误，请确保每行的格式为: 字段 值'))
            return
          }
        }
        callback()
      } else {
        callback()
      }
    }}
  ]
}

const documentDialogVisible = ref(false)
const documentDialogMode = ref('view') // view, edit, create
const currentDocument = ref('{}')
const documentOriginal = ref(null)

// 计算属性
const dataSourceType = computed(() => {
  const type = route.query.type?.toLowerCase() || ''
  console.log('数据源类型 (计算):', type)
  return type
})

const filteredCollections = computed(() => {
  if (!searchText.value) {
    return collections.value
  }
  const searchLower = searchText.value.toLowerCase()
  return collections.value.filter(c => 
    c.name.toLowerCase().includes(searchLower)
  )
})

// 计算文档的公共字段，用于表格列
const commonFields = computed(() => {
  if (!documents.value || documents.value.length === 0) {
    return []
  }
  
  // 收集所有文档中的所有字段
  const allFields = new Set()
  documents.value.forEach(doc => {
    Object.keys(doc).forEach(key => {
      // 排除_id字段，因为它已经单独显示
      if (key !== '_id') {
        allFields.add(key)
      }
    })
  })
  
  // 转换为数组并排序
  return Array.from(allFields).sort()
})

// 方法
const loadCollections = async () => {
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载集合列表...'
    
    const response = await getCollections(route.params.id)
    collections.value = response.data || []
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载集合列表失败:', error)
    ElMessage.error('加载集合列表失败')
    fullscreenLoading.value = false
  }
}

const refreshCollections = () => {
  loadCollections()
}

const filterCollections = () => {
  // 通过计算属性自动过滤
}

const selectCollection = async (collection) => {
  selectedCollection.value = collection
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载集合信息...'
    
    // 1. 获取集合详细信息
    const infoResponse = await getCollectionInfo(route.params.id, collection.name)
    
    // 2. 获取集合模式(schema)信息
    const schemaResponse = await getCollectionSchema(route.params.id, collection.name)
    
    // 合并信息
    selectedCollection.value = { 
      ...collection, 
      ...infoResponse.data,
      ...(schemaResponse.data || {})
    }
    
    console.log('加载的集合信息:', selectedCollection.value)
    
    // 重置分页和查询
    currentPage.value = 1
    queryFilter.value = ''
    
    // 确保显示数据标签页
    activeTab.value = 'data'
    
    // 加载文档数据
    loadDocuments()
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载集合信息失败:', error)
    ElMessage.error('加载集合信息失败: ' + (error?.message || '未知错误'))
    fullscreenLoading.value = false
  }
}

const loadDocuments = async () => {
  if (!selectedCollection.value) {
    console.warn('没有选择集合，无法加载文档');
    return;
  }
  
  try {
    fullscreenLoading.value = true
    loadingText.value = '加载文档数据...'
    
    let filter = {}
    if (queryFilter.value) {
      try {
        filter = JSON.parse(queryFilter.value)
      } catch (e) {
        ElMessage.error('过滤条件格式错误，请使用有效的JSON')
        fullscreenLoading.value = false
        return
      }
    }
    
    const response = await getDocuments(
      route.params.id, 
      selectedCollection.value.name, 
      {
        page: currentPage.value,
        pageSize: pageSize.value,
        filter
      }
    )
    
    // 处理嵌套的data结构
    if (response.code === 0 && response.data && response.data.data) {
      documents.value = response.data.data || []
      totalDocuments.value = response.data.total || 0
    } else {
      // 兼容旧格式
      documents.value = response.data.documents || []
      totalDocuments.value = response.data.total || 0
    }
    
    console.log('加载的文档数据:', documents.value)
    console.log('数据类型:', typeof documents.value, Array.isArray(documents.value))
    console.log('数据字段:', commonFields.value)
    console.log('数据源类型:', dataSourceType.value)
    
    fullscreenLoading.value = false
  } catch (error) {
    console.error('加载文档数据失败:', error)
    ElMessage.error('加载文档数据失败')
    fullscreenLoading.value = false
  }
}

const executeQuery = () => {
  loadDocuments()
}

const executeBuilderQuery = () => {
  if (dataSourceType.value === 'mongodb') {
    try {
      // 解析查询构建器中的JSON
      const filter = JSON.parse(queryBuilder.value.filter)
      const projection = JSON.parse(queryBuilder.value.projection)
      const sort = JSON.parse(queryBuilder.value.sort)
      
      // 构建完整的查询参数
      const queryParams = {
        filter,
        projection,
        sort,
        page: currentPage.value,
        pageSize: pageSize.value
      }
      
      // 更新UI上的过滤条件显示
      queryFilter.value = JSON.stringify(filter)
      
      // 切换到数据标签页并执行查询
      activeTab.value = 'data'
      
      fullscreenLoading.value = true
      loadingText.value = '执行查询...'
      
      getDocuments(route.params.id, selectedCollection.value.name, queryParams)
        .then(response => {
          if (response.code === 0 && response.data) {
            if (response.data.data) {
              documents.value = response.data.data
              totalDocuments.value = response.data.total || 0
            } else {
              documents.value = response.data.documents || []
              totalDocuments.value = response.data.total || 0
            }
            console.log('查询结果:', documents.value)
          }
          fullscreenLoading.value = false
        })
        .catch(error => {
          console.error('执行查询失败:', error)
          ElMessage.error('执行查询失败: ' + (error?.message || '未知错误'))
          fullscreenLoading.value = false
        })
    } catch (e) {
      ElMessage.error('查询参数格式错误，请检查JSON格式')
    }
  } else if (dataSourceType.value === 'redis') {
    // Redis查询
    // 这里使用pattern和type进行查询
    const params = {
      pattern: queryBuilder.value.pattern,
      type: queryBuilder.value.type
    }
    
    fullscreenLoading.value = true
    loadingText.value = '执行查询...'
    
    getDocuments(route.params.id, selectedCollection.value.name, params)
      .then(response => {
        if (response.code === 0 && response.data) {
          if (response.data.data) {
            // 处理Redis数据，确保包含所需字段
            documents.value = response.data.data.map(item => {
              // 确保每个项都有以下字段
              return {
                _id: item._id || item.key || item._key, // 使用可用的字段作为ID
                key: item.key || item._key || '',
                value: item.value !== undefined ? item.value : '',
                type: item.type || 'string',
                ttl: item.ttl !== undefined ? item.ttl : -1,
                ...item // 保留其他原始属性
              }
            })
            totalDocuments.value = response.data.total || 0
            
            console.log('Redis文档数据：', documents.value)
          } else {
            documents.value = response.data.documents || []
            totalDocuments.value = response.data.total || 0
          }
        }
        fullscreenLoading.value = false
      })
      .catch(error => {
        console.error('执行查询失败:', error)
        ElMessage.error('执行查询失败: ' + (error?.message || '未知错误'))
        fullscreenLoading.value = false
      })
  }
}

const resetQueryBuilder = () => {
  queryBuilder.value = {
    filter: '{}',
    projection: '{}',
    sort: '{}',
    pattern: '*',
    type: ''
  }
}

const handleSelectionChange = (val) => {
  selectedDocuments.value = val
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadDocuments()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadDocuments()
}

const viewDocument = (doc) => {
  documentDialogMode.value = 'view'
  
  // 处理Redis数据特殊情况
  if (dataSourceType.value === 'redis') {
    // 构建更友好的显示结构
    const formattedDoc = {
      key: doc.key || doc._key,
      type: doc.type || 'string',
      value: doc.value !== undefined ? doc.value : '',
      ttl: doc.ttl,
      ttlFormatted: doc.ttl === -1 ? '永不过期' : formatTTL(doc.ttl)
    }
    
    // 添加数据类型特有的属性
    if (doc.type === 'hash' && doc._key) {
      // 移除内部属性
      const hashData = { ...doc }
      delete hashData._id
      delete hashData._key
      delete hashData.key
      delete hashData.type
      delete hashData.ttl
      formattedDoc.fields = hashData
    } else if (doc.type === 'list' || doc.type === 'set' || doc.type === 'zset') {
      if (doc.index !== undefined) {
        formattedDoc.index = doc.index
      }
      if (doc.score !== undefined) {
        formattedDoc.score = doc.score
      }
    }
    
    currentDocument.value = JSON.stringify(formattedDoc, null, 2)
  } else {
    // 普通文档展示
    currentDocument.value = JSON.stringify(doc, null, 2)
  }
  
  documentDialogVisible.value = true
}

const editDocument = (doc) => {
  documentDialogMode.value = 'edit'
  
  // 在编辑前保存原始文档，确保保留正确的ID
  documentOriginal.value = {...doc}
  
  // 转换ObjectId为标准格式，确保客户端和服务器之间的一致性
  const processingDoc = {...doc}
  
  // 如果_id字段是ObjectId对象，转换为标准格式
  if (processingDoc._id && typeof processingDoc._id === 'object') {
    // 如果是MongoDB原生ObjectId
    if (processingDoc._id.$oid) {
      console.log('处理$oid格式的ID:', processingDoc._id.$oid);
      processingDoc._id = {
        $oid: processingDoc._id.$oid
      };
    } else {
      // 保存原始ID，避免丢失数据
      console.log('保留原始ID对象:', processingDoc._id);
    }
  }
  
  // 将处理后的文档转为JSON字符串并格式化（注意这也会将对象ID转为字符串表示）
  currentDocument.value = JSON.stringify(processingDoc, null, 2)
  documentDialogVisible.value = true
  
  console.log('编辑文档 - 原始ID:', documentOriginal.value._id);
  console.log('编辑文档 - 处理后:', currentDocument.value);
}

const createNewDocument = () => {
  documentDialogMode.value = 'create'
  currentDocument.value = '{}'
  documentDialogVisible.value = true
}

const saveDocument = async () => {
  try {
    const docData = JSON.parse(currentDocument.value)
    
    if (documentDialogMode.value === 'edit') {
      let documentId = null;
      
      // 检查文档是否有带timestamp的特殊ID格式
      if (documentOriginal.value._id && typeof documentOriginal.value._id === 'object' && documentOriginal.value._id.timestamp) {
        documentId = String(documentOriginal.value._id.timestamp);
        console.log('使用timestamp作为ID:', documentId);
      } else {
        // 使用常规ID提取
        documentId = extractIdFromDocument(documentOriginal.value);
        console.log('使用常规ID提取:', documentId);
      }
      
      console.log('原始文档:', documentOriginal.value);
      console.log('解析的文档ID:', documentId);
      
      if (!documentId) {
        // 尝试从编辑后的文档中获取ID
        const editedDocId = extractIdFromDocument(docData);
        
        if (editedDocId) {
          console.log('从编辑后文档获取ID:', editedDocId);
          await updateDocument(
            route.params.id,
            selectedCollection.value.name,
            editedDocId,
            docData
          )
          ElMessage.success('文档更新成功')
          documentDialogVisible.value = false
          loadDocuments()
          return;
        }
        
        throw new Error('无法识别文档ID，请确保文档包含有效的_id字段');
      }
      
      // 克隆文档数据，移除复杂的_id对象
      const cleanData = { ...docData };
      if (cleanData._id && typeof cleanData._id === 'object') {
        delete cleanData._id;
      }
      
      await updateDocument(
        route.params.id,
        selectedCollection.value.name,
        documentId,
        cleanData
      )
      ElMessage.success('文档更新成功')
    } else if (documentDialogMode.value === 'create') {
      await insertDocument(
        route.params.id,
        selectedCollection.value.name,
        docData
      )
      ElMessage.success('文档创建成功')
    }
    
    documentDialogVisible.value = false
    loadDocuments()
  } catch (error) {
    console.error('保存文档失败:', error)
    ElMessage.error('保存文档失败: ' + error.message)
  }
}

const deleteDocument = async (doc) => {
  try {
    await ElMessageBox.confirm('确定要删除此文档吗？', '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    // 使用新的ID处理函数
    const documentId = extractIdFromDocument(doc);
    
    console.log('删除文档ID:', documentId, '原始ID:', doc._id);
    
    if (!documentId) {
      throw new Error('无法处理文档ID，请确保文档有有效的ID');
    }
    
    await apiDeleteDocument(
      route.params.id,
      selectedCollection.value.name,
      documentId
    )
    
    ElMessage.success('文档删除成功')
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除文档失败:', error)
      ElMessage.error('删除文档失败: ' + (error.message || '未知错误'))
    }
  }
}

const deleteSelectedDocuments = async () => {
  if (selectedDocuments.value.length === 0) {
    ElMessage.warning('请选择要删除的文档')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${selectedDocuments.value.length} 个文档吗？`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    fullscreenLoading.value = true
    loadingText.value = '删除文档中...'
    
    const promises = selectedDocuments.value.map(doc => {
      // 使用新的ID处理函数
      const documentId = extractIdFromDocument(doc);
      
      if (!documentId) {
        console.warn('跳过无效ID的文档:', doc);
        return Promise.resolve(); // 跳过无效ID的文档
      }
      
      return apiDeleteDocument(
        route.params.id,
        selectedCollection.value.name,
        documentId
      );
    }).filter(p => p); // 过滤掉undefined的Promise
    
    await Promise.all(promises)
    
    ElMessage.success(`成功删除文档`)
    loadDocuments()
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除文档失败:', error)
      ElMessage.error('批量删除文档失败: ' + (error.message || '未知错误'))
      fullscreenLoading.value = false
    }
  }
}

const handleCreateCollection = async () => {
  try {
    await createCollectionFormRef.value.validate()
    
    fullscreenLoading.value = true
    loadingText.value = '创建集合中...'
    
    // 根据不同数据源类型处理创建逻辑
    if (dataSourceType.value === 'redis') {
      // Redis特殊处理
      const formData = { ...createCollectionForm.value }
      
      // 根据数据类型构建请求参数
      const redisData = {
        name: formData.name,
        type: formData.dataType,
        ttl: formData.ttl
      }
      
      // 根据不同数据类型处理数据
      switch (formData.dataType) {
        case 'string':
          redisData.value = formData.stringValue
          break
        case 'list':
          // 处理列表元素，每行一个元素
          redisData.values = formData.listValues
            .split('\n')
            .filter(line => line.trim() !== '')
            .map(line => line.trim())
          break
        case 'set':
          // 处理集合元素，每行一个元素
          redisData.values = formData.setValues
            .split('\n')
            .filter(line => line.trim() !== '')
            .map(line => line.trim())
          break
        case 'zset':
          // 处理有序集合，每行 "分数 元素"
          redisData.values = formData.zsetValues
            .split('\n')
            .filter(line => line.trim() !== '')
            .map(line => {
              const parts = line.trim().split(/\s+/)
              const score = Number(parts[0])
              const member = parts.slice(1).join(' ')
              return { score, member }
            })
          break
        case 'hash':
          // 处理哈希表，每行 "字段 值"
          redisData.values = {}
          formData.hashValues
            .split('\n')
            .filter(line => line.trim() !== '')
            .forEach(line => {
              const parts = line.trim().split(/\s+/)
              const field = parts[0]
              const value = parts.slice(1).join(' ')
              redisData.values[field] = value
            })
          break
      }
      
      // 发送请求创建Redis键
      await createCollection(route.params.id, redisData)
    } else {
      // 其他数据库类型的处理保持不变
      await createCollection(route.params.id, createCollectionForm.value)
    }
    
    ElMessage.success('集合创建成功')
    createCollectionDialogVisible.value = false
    
    // 重置表单
    createCollectionForm.value = {
      name: '',
      capped: false,
      maxDocuments: 1000,
      size: 1048576,
      dataType: 'string',
      stringValue: '',
      listValues: '',
      setValues: '',
      zsetValues: '',
      hashValues: '',
      ttl: -1
    }
    
    // 刷新集合列表
    await loadCollections()
    fullscreenLoading.value = false
  } catch (error) {
    console.error('创建集合失败:', error)
    ElMessage.error('创建集合失败: ' + error.message)
    fullscreenLoading.value = false
  }
}

const showDeleteConfirm = async () => {
  if (!selectedCollection.value) {
    ElMessage.warning('请选择要删除的集合')
    return
  }
  
  try {
    await ElMessageBox.confirm(`确定要删除集合 "${selectedCollection.value.name}" 吗？此操作不可恢复！`, '警告', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    fullscreenLoading.value = true
    loadingText.value = '删除集合中...'
    
    await deleteCollection(route.params.id, selectedCollection.value.name)
    
    ElMessage.success('集合删除成功')
    selectedCollection.value = null
    
    // 刷新集合列表
    await loadCollections()
    fullscreenLoading.value = false
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除集合失败:', error)
      ElMessage.error('删除集合失败')
      fullscreenLoading.value = false
    }
  }
}

// 格式化函数
const formatCount = (count) => {
  if (count === undefined || count === null) return '-'
  return new Intl.NumberFormat().format(count)
}

const formatSize = (size) => {
  if (size === undefined || size === null) return '-'
  
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let index = 0
  let formattedSize = size
  
  while (formattedSize >= 1024 && index < units.length - 1) {
    formattedSize /= 1024
    index++
  }
  
  return `${formattedSize.toFixed(2)} ${units[index]}`
}

const formatTTL = (ttl) => {
  if (ttl <= 0) return '永不过期'
  
  // 转换为更友好的格式（天、小时、分钟、秒）
  const days = Math.floor(ttl / 86400)
  const hours = Math.floor((ttl % 86400) / 3600)
  const minutes = Math.floor((ttl % 3600) / 60)
  const seconds = ttl % 60
  
  const parts = []
  if (days > 0) parts.push(`${days}天`)
  if (hours > 0) parts.push(`${hours}小时`)
  if (minutes > 0) parts.push(`${minutes}分钟`)
  if (seconds > 0) parts.push(`${seconds}秒`)
  
  return parts.join(' ')
}

// 打开创建集合对话框并重置表单
const openCreateCollectionDialog = () => {
  // 根据数据源类型设置默认值
  if (dataSourceType.value === 'redis') {
    createCollectionForm.value = {
      name: '',
      capped: false,
      maxDocuments: 1000,
      size: 1048576,
      dataType: 'string',
      stringValue: '',
      listValues: '',
      setValues: '',
      zsetValues: '',
      hashValues: '',
      ttl: -1
    }
  } else if (dataSourceType.value === 'mongodb') {
    createCollectionForm.value = {
      name: '',
      capped: false,
      maxDocuments: 1000,
      size: 1048576
    }
  } else {
    createCollectionForm.value = {
      name: ''
    }
  }
  
  // 显示对话框
  createCollectionDialogVisible.value = true
  
  // 如果表单引用存在，重置验证
  if (createCollectionFormRef.value) {
    createCollectionFormRef.value.resetFields()
  }
}

// 示例填充方法
const fillStringExample = () => {
  // 示例字符串，使用JSON格式以便更好地展示各种数据类型的例子
  createCollectionForm.value.stringValue = JSON.stringify({
    "id": 1001,
    "name": "示例商品",
    "price": 99.99,
    "inStock": true,
    "tags": ["热销", "新品"],
    "lastUpdated": "2023-10-15T08:30:00Z"
  }, null, 2)
}

const fillListExample = () => {
  // 示例列表
  createCollectionForm.value.listValues = 
`最新消息1
最新消息2
最新消息3
任务A - 优先级高
任务B - 优先级中`
}

const fillSetExample = () => {
  // 示例集合
  createCollectionForm.value.setValues = 
`电子产品
家居用品
厨房用具
办公设备
电子产品`  // 重复项会被自动去除
}

const fillZSetExample = () => {
  // 示例有序集合
  createCollectionForm.value.zsetValues = 
`100 用户A
95 用户B
87 用户C
120 用户D
67 用户E`
}

const fillHashExample = () => {
  // 示例哈希表
  createCollectionForm.value.hashValues = 
`username 张三
age 28
email user@example.com
level VIP
points 1250
lastLogin 2023-10-25 14:30:00`
}

// 填充示例键名
const fillExampleKeyName = () => {
  const type = createCollectionForm.value.dataType
  switch (type) {
    case 'string':
      createCollectionForm.value.name = 'product:1001'
      break
    case 'list':
      createCollectionForm.value.name = 'timeline:user1'
      break
    case 'set':
      createCollectionForm.value.name = 'tags:product1001'
      break
    case 'zset':
      createCollectionForm.value.name = 'leaderboard:game1'
      break
    case 'hash':
      createCollectionForm.value.name = 'user:1001:info'
      break
  }
}

// 处理类型变化，清空其他类型的值
watch(() => createCollectionForm.value.dataType, (newType) => {
  // 当数据类型改变时，清空其他类型的输入值
  if (newType === 'string') {
    createCollectionForm.value.listValues = ''
    createCollectionForm.value.setValues = ''
    createCollectionForm.value.zsetValues = ''
    createCollectionForm.value.hashValues = ''
  } else if (newType === 'list') {
    createCollectionForm.value.stringValue = ''
    createCollectionForm.value.setValues = ''
    createCollectionForm.value.zsetValues = ''
    createCollectionForm.value.hashValues = ''
  } else if (newType === 'set') {
    createCollectionForm.value.stringValue = ''
    createCollectionForm.value.listValues = ''
    createCollectionForm.value.zsetValues = ''
    createCollectionForm.value.hashValues = ''
  } else if (newType === 'zset') {
    createCollectionForm.value.stringValue = ''
    createCollectionForm.value.listValues = ''
    createCollectionForm.value.setValues = ''
    createCollectionForm.value.hashValues = ''
  } else if (newType === 'hash') {
    createCollectionForm.value.stringValue = ''
    createCollectionForm.value.listValues = ''
    createCollectionForm.value.setValues = ''
    createCollectionForm.value.zsetValues = ''
  }
})

// 初始化
onMounted(() => {
  loadCollections()
})

// 监听
watch(() => route.params.id, (newId) => {
  if (newId) {
    selectedCollection.value = null
    loadCollections()
  }
})
</script>

<style scoped>
.nosql-collections {
  height: calc(100vh - 140px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.datasource-info {
  font-weight: bold;
}

.header-right {
  display: flex;
  gap: 8px;
}

.content-wrapper {
  display: flex;
  height: calc(100vh - 200px);
  overflow: hidden;
}

.left-area {
  width: 280px;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #ebeef5;
  padding-right: 16px;
}

.search-box {
  margin-bottom: 16px;
}

.collections-list {
  flex: 1;
  overflow: auto;
}

.collection-item {
  display: flex;
  align-items: center;
  padding: 10px;
  cursor: pointer;
  border-radius: 4px;
  margin-bottom: 4px;
}

.collection-item:hover {
  background-color: #f5f7fa;
}

.collection-item.active {
  background-color: #ecf5ff;
  color: #409eff;
}

.collection-name {
  margin-left: 8px;
  flex: 1;
}

.collection-count {
  color: #909399;
  font-size: 12px;
}

.right-area {
  flex: 1;
  padding-left: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.no-collection-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.data-browser {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.toolbar {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.data-table {
  flex: 1;
  overflow: hidden;
}

.pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

.schema-info {
  padding: 16px;
}

.collection-info {
  margin-bottom: 16px;
}

.info-item {
  margin-bottom: 8px;
}

.label {
  font-weight: bold;
}

.mappings-container {
  margin-top: 16px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
}

.mappings-code {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
}

.query-builder {
  padding: 16px;
}

.builder-header {
  margin-bottom: 16px;
}

.builder-section {
  margin-bottom: 16px;
}

.section-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.editor-content {
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  height: 120px;
}

.builder-example {
  margin-top: 16px;
}

.example-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.example-item {
  margin-bottom: 8px;
}

.builder-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.document-editor-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.document-help {
  margin-bottom: 16px;
}

.document-example {
  margin-bottom: 16px;
}

.example-code {
  background-color: #f5f7fa;
  padding: 16px;
  border-radius: 4px;
}

.document-tips {
  margin-top: 16px;
}

.document-tips p {
  margin-bottom: 8px;
}

.document-tips ul {
  list-style-type: disc;
  padding-left: 20px;
}

/* Redis表单样式 */
.el-form-item-helper {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.redis-form-section {
  margin-bottom: 16px;
}

.redis-form-section-title {
  font-weight: bold;
  margin-bottom: 8px;
}

.redis-form-tips {
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-size: 12px;
}

.redis-form-tips p {
  margin: 0 0 4px 0;
}

.redis-form-tips code {
  background-color: #e6e6e6;
  padding: 2px 4px;
  border-radius: 2px;
  font-family: monospace;
}

.form-example-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.common-ttl-buttons {
  margin-top: 8px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.common-ttl-buttons .el-button {
  margin-left: 0;
}
</style> 