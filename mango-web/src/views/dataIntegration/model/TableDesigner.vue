<template>
  <div class="table-designer">
    <!-- 模式切换 -->
    <div class="mode-switch">
      <div class="mode-left">
        <el-radio-group v-model="designMode" size="large">
          <el-radio-button value="structure">表结构设计</el-radio-button>
          <el-radio-button value="relation">表关系设计</el-radio-button>
        </el-radio-group>
      </div>

      <div class="mode-right">
        <div class="model-name-input">
          <el-input
              v-model="tableForm.name"
              placeholder="请输入表名称"
              size="large"
              clearable
          >
            <template #prepend>表名称</template>
          </el-input>
        </div>
        <el-button type="primary" size="large" @click="handleSave">
          <el-icon>
            <CircleCheck/>
          </el-icon>
          保存表设计
        </el-button>
        <el-button type="success" size="large" @click="handleApplyTemplate">
          <el-icon>
            <Collection/>
          </el-icon>
          应用模板
        </el-button>
      </div>
    </div>

    <!-- 视图切换按钮组 -->
    <div class="view-mode-switch">
      <el-button-group>
        <el-button
            :type="viewMode === 'visual' ? 'primary' : 'default'"
            @click="viewMode = 'visual'"
            size="small">
          <i class="el-icon-picture"></i> 可视化视图
        </el-button>
        <el-button
            :type="viewMode === 'table' ? 'primary' : 'default'"
            @click="viewMode = 'table'"
            size="small">
          <i class="el-icon-document"></i> 表格视图
        </el-button>
      </el-button-group>
    </div>

    <!-- 表结构设计模式 -->
    <template v-if="designMode === 'structure'">
      <!-- 顶部工具栏 -->
      <div class="designer-toolbar">
        <div class="left-tools">
          <el-button-group>
            <el-button @click="handleUndo" :disabled="!canUndo">
              <el-icon>
                <Back/>
              </el-icon>
              撤销
            </el-button>
            <el-button @click="handleRedo" :disabled="!canRedo">
              <el-icon>
                <Right/>
              </el-icon>
              重做
            </el-button>
          </el-button-group>
          <el-divider direction="vertical"/>
          <el-button-group>
            <el-button @click="addField">
              <el-icon>
                <Plus/>
              </el-icon>
              添加字段
            </el-button>
            <el-button @click="removeSelectedFields" :disabled="!selectedFields.length">
              <el-icon>
                <Delete/>
              </el-icon>
              删除字段
            </el-button>
          </el-button-group>
        </div>
        <div class="right-tools">
          <el-button-group>
            <el-button @click="previewSQL">
              <el-icon>
                <Document/>
              </el-icon>
              SQL预览
            </el-button>
            <el-button @click="exportTable">
              <el-icon>
                <Download/>
              </el-icon>
              导出表结构
            </el-button>
            <el-button @click="importTable">
              <el-icon>
                <Upload/>
              </el-icon>
              导入表结构
            </el-button>
          </el-button-group>
        </div>
      </div>

      <!-- 主设计区域 -->
      <div class="designer-main">
        <!-- 左侧组件面板 -->
        <div class="component-panel" v-if="viewMode === 'visual'">
          <el-tabs>
            <el-tab-pane label="字段组件">
              <div class="component-group">
                <div class="group-title">基础字段</div>
                <div class="component-list">
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'string')">
                    <el-icon>
                      <Aim/>
                    </el-icon>
                    <span>字符串</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'number')">
                    <el-icon>
                      <AddLocation/>
                    </el-icon>
                    <span>数值</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'datetime')">
                    <el-icon>
                      <Timer/>
                    </el-icon>
                    <span>日期时间</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'boolean')">
                    <el-icon>
                      <Switch/>
                    </el-icon>
                    <span>布尔值</span>
                  </div>
                </div>

                <div class="group-title" style="margin-top: 15px;">高级字段</div>
                <div class="component-list">
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'json')">
                    <el-icon>
                      <Paperclip/>
                    </el-icon>
                    <span>JSON</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'array')">
                    <el-icon>
                      <List/>
                    </el-icon>
                    <span>数组</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'enum')">
                    <el-icon>
                      <SetUp/>
                    </el-icon>
                    <span>枚举</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'binary')">
                    <el-icon>
                      <Files/>
                    </el-icon>
                    <span>二进制</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
            <el-tab-pane label="约束">
              <div class="component-group">
                <div class="group-title">字段约束</div>
                <div class="component-list">
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'pk')">
                    <el-icon>
                      <Key/>
                    </el-icon>
                    <span>主键</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'unique')">
                    <el-icon>
                      <InfoFilled/>
                    </el-icon>
                    <span>唯一约束</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'not-null')">
                    <el-icon>
                      <NoSmoking/>
                    </el-icon>
                    <span>非空</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'index')">
                    <el-icon>
                      <Sort/>
                    </el-icon>
                    <span>索引</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <!-- 中间设计区域 - 表格视图 -->
        <div class="fields-container" :class="{ 'full-width': viewMode === 'table' }">
          <el-table
              :data="tableForm.fields"
              border
              @selection-change="handleSelectionChange"
              class="fields-table"
              size="default"
              v-loading="loading"
          >
            <el-table-column type="selection" width="50"/>
            <el-table-column label="字段名称" prop="name" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="字段名称"/>
              </template>
            </el-table-column>
            <el-table-column label="显示名称" prop="displayName" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.displayName" placeholder="显示名称"/>
              </template>
            </el-table-column>
            <el-table-column label="数据类型" prop="type" width="150">
              <template #default="{ row }">
                <el-select v-model="row.type" style="width: 100%">
                  <el-option
                      v-for="type in dataTypes"
                      :key="type.value"
                      :label="type.label"
                      :value="type.value"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="长度" prop="length" width="100">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.length"
                    :min="1"
                    :disabled="!needLength(row.type)"
                />
              </template>
            </el-table-column>
            <el-table-column label="精度" prop="precision" width="100">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.precision"
                    :min="0"
                    :disabled="!needPrecision(row.type)"
                />
              </template>
            </el-table-column>
            <el-table-column label="默认值" prop="defaultValue" min-width="120">
              <template #default="{ row }">
                <el-input v-model="row.defaultValue" placeholder="默认值"/>
              </template>
            </el-table-column>
            <el-table-column label="主键" prop="isPrimaryKey" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isPrimaryKey" @change="handlePrimaryKeyChange(row)"/>
              </template>
            </el-table-column>
            <el-table-column label="非空" prop="isNotNull" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isNotNull"/>
              </template>
            </el-table-column>
            <el-table-column label="唯一" prop="isUnique" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isUnique"/>
              </template>
            </el-table-column>
            <el-table-column label="索引" prop="isIndexed" width="70" align="center">
              <template #default="{ row }">
                <el-checkbox v-model="row.isIndexed"/>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template #default="{ row, $index }">
                <el-button-group>
                  <el-button type="primary" link @click="moveField($index, 'up')" :disabled="$index === 0">
                    <el-icon>
                      <ArrowUp/>
                    </el-icon>
                  </el-button>
                  <el-button type="primary" link
                             @click="moveField($index, 'down')"
                             :disabled="$index === tableForm.fields.length - 1">
                    <el-icon>
                      <ArrowDown/>
                    </el-icon>
                  </el-button>
                  <el-button type="danger" link @click="removeField($index)">
                    <el-icon>
                      <Delete/>
                    </el-icon>
                  </el-button>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 右侧属性面板 - 只在可视化视图下显示 -->
        <div class="properties-panel" v-if="viewMode === 'visual' && selectedField">
          <div class="panel-title">
            <span>字段属性</span>
            <el-button type="primary" link @click="closePropertiesPanel">
              <el-icon>
                <Close/>
              </el-icon>
            </el-button>
          </div>
          <el-form :model="selectedField" label-width="100px" size="default">
            <el-form-item label="字段名称">
              <el-input v-model="selectedField.name" placeholder="请输入字段名称"/>
            </el-form-item>
            <el-form-item label="显示名称">
              <el-input v-model="selectedField.displayName" placeholder="请输入显示名称"/>
            </el-form-item>
            <el-form-item label="数据类型">
              <el-select v-model="selectedField.type" style="width: 100%">
                <el-option
                    v-for="type in dataTypes"
                    :key="type.value"
                    :label="type.label"
                    :value="type.value"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="字段长度" v-if="needLength(selectedField.type)">
              <el-input-number v-model="selectedField.length" :min="1" style="width: 100%"/>
            </el-form-item>
            <el-form-item label="精度" v-if="needPrecision(selectedField.type)">
              <el-input-number v-model="selectedField.precision" :min="0" style="width: 100%"/>
            </el-form-item>
            <el-form-item label="默认值">
              <el-input v-model="selectedField.defaultValue" placeholder="请输入默认值"/>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="selectedField.description" type="textarea" :rows="3" placeholder="请输入字段描述"/>
            </el-form-item>
            <el-divider>约束设置</el-divider>
            <el-form-item>
              <el-checkbox v-model="selectedField.isPrimaryKey" @change="handlePrimaryKeyChange(selectedField)">主键
              </el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="selectedField.isNotNull">非空</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="selectedField.isUnique">唯一约束</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="selectedField.isIndexed">创建索引</el-checkbox>
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="selectedField.isAutoIncrement"
                           :disabled="!['int', 'bigint', 'integer'].includes(selectedField.type)">自增
              </el-checkbox>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </template>

    <!-- 表关系设计模式 -->
    <template v-else-if="designMode === 'relation'">
      <div class="designer-toolbar">
        <div class="left-tools">
          <el-button-group>
            <el-button @click="addRelation">
              <el-icon>
                <Plus/>
              </el-icon>
              添加关系
            </el-button>
            <el-button @click="removeSelectedRelations" :disabled="!selectedRelations.length">
              <el-icon>
                <Delete/>
              </el-icon>
              删除关系
            </el-button>
          </el-button-group>
        </div>
        <div class="right-tools">
          <el-button-group>
            <el-button @click="previewRelationSQL">
              <el-icon>
                <Document/>
              </el-icon>
              SQL预览
            </el-button>
            <el-button @click="exportRelations">
              <el-icon>
                <Download/>
              </el-icon>
              导出关系
            </el-button>
          </el-button-group>
        </div>
      </div>

      <div class="designer-main">
        <div class="component-panel" v-if="viewMode === 'visual'">
          <el-tabs>
            <el-tab-pane label="关系组件">
              <div class="component-group">
                <div class="group-title">关系类型</div>
                <div class="component-list">
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'one-to-one')">
                    <el-icon>
                      <Connection/>
                    </el-icon>
                    <span>一对一</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'one-to-many')">
                    <el-icon>
                      <Share/>
                    </el-icon>
                    <span>一对多</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'many-to-many')">
                    <el-icon>
                      <Grape/>
                    </el-icon>
                    <span>多对多</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'reference')">
                    <el-icon>
                      <Link/>
                    </el-icon>
                    <span>外键引用</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>

        <div class="fields-container" :class="{ 'full-width': viewMode === 'table' }">
          <el-table
              :data="tableForm.relations"
              border
              @selection-change="handleRelationSelectionChange"
              class="fields-table"
              size="default"
              v-loading="loading"
          >
            <el-table-column type="selection" width="50"/>
            <el-table-column label="关系名称" prop="name" min-width="150">
              <template #default="{ row }">
                <el-input v-model="row.name" placeholder="关系名称"/>
              </template>
            </el-table-column>
            <el-table-column label="关系类型" prop="type" width="150">
              <template #default="{ row }">
                <el-select v-model="row.type" style="width: 100%">
                  <el-option label="一对一" value="one-to-one"/>
                  <el-option label="一对多" value="one-to-many"/>
                  <el-option label="多对多" value="many-to-many"/>
                  <el-option label="外键引用" value="reference"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="源表" prop="sourceTable" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.sourceTable" style="width: 100%" filterable>
                  <el-option
                      v-for="table in availableTables"
                      :key="table.id"
                      :label="table.name"
                      :value="table.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="源字段" prop="sourceField" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.sourceField" style="width: 100%" filterable
                           :disabled="!row.sourceTable">
                  <el-option
                      v-for="field in getTableFields(row.sourceTable)"
                      :key="field.id"
                      :label="field.name"
                      :value="field.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标表" prop="targetTable" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.targetTable" style="width: 100%" filterable>
                  <el-option
                      v-for="table in availableTables"
                      :key="table.id"
                      :label="table.name"
                      :value="table.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标字段" prop="targetField" min-width="150">
              <template #default="{ row }">
                <el-select v-model="row.targetField" style="width: 100%" filterable
                           :disabled="!row.targetTable">
                  <el-option
                      v-for="field in getTableFields(row.targetTable)"
                      :key="field.id"
                      :label="field.name"
                      :value="field.id"
                  />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ $index }">
                <el-button type="danger" link @click="removeRelation($index)">
                  <el-icon>
                    <Delete/>
                  </el-icon>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div class="properties-panel" v-if="viewMode === 'visual' && selectedRelation">
          <div class="panel-title">
            <span>关系属性</span>
            <el-button type="primary" link @click="closeRelationPropertiesPanel">
              <el-icon>
                <Close/>
              </el-icon>
            </el-button>
          </div>
          <el-form :model="selectedRelation" label-width="100px" size="default">
            <el-form-item label="关系名称">
              <el-input v-model="selectedRelation.name" placeholder="请输入关系名称"/>
            </el-form-item>
            <el-form-item label="关系类型">
              <el-select v-model="selectedRelation.type" style="width: 100%">
                <el-option label="一对一" value="one-to-one"/>
                <el-option label="一对多" value="one-to-many"/>
                <el-option label="多对多" value="many-to-many"/>
                <el-option label="外键引用" value="reference"/>
              </el-select>
            </el-form-item>
            <el-form-item label="源表">
              <el-select v-model="selectedRelation.sourceTable" style="width: 100%" filterable>
                <el-option
                    v-for="table in availableTables"
                    :key="table.id"
                    :label="table.name"
                    :value="table.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="源字段">
              <el-select v-model="selectedRelation.sourceField" style="width: 100%" filterable
                         :disabled="!selectedRelation.sourceTable">
                <el-option
                    v-for="field in getTableFields(selectedRelation.sourceTable)"
                    :key="field.id"
                    :label="field.name"
                    :value="field.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="目标表">
              <el-select v-model="selectedRelation.targetTable" style="width: 100%" filterable>
                <el-option
                    v-for="table in availableTables"
                    :key="table.id"
                    :label="table.name"
                    :value="table.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="目标字段">
              <el-select v-model="selectedRelation.targetField" style="width: 100%" filterable
                         :disabled="!selectedRelation.targetTable">
                <el-option
                    v-for="field in getTableFields(selectedRelation.targetTable)"
                    :key="field.id"
                    :label="field.name"
                    :value="field.id"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="级联删除" v-if="selectedRelation.type !== 'many-to-many'">
              <el-switch v-model="selectedRelation.cascadeDelete"/>
            </el-form-item>
            <el-form-item label="级联更新" v-if="selectedRelation.type !== 'many-to-many'">
              <el-switch v-model="selectedRelation.cascadeUpdate"/>
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="selectedRelation.description" type="textarea" :rows="3"
                        placeholder="请输入关系描述"/>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </template>

    <!-- SQL预览对话框 -->
    <el-dialog v-model="sqlPreviewVisible" title="SQL预览" width="70%" append-to-body>
      <div class="sql-preview">
        <el-tabs v-model="sqlTabActive">
          <el-tab-pane label="表结构SQL" name="structure">
            <pre class="sql-code">{{ structureSQL }}</pre>
          </el-tab-pane>
          <el-tab-pane label="关系SQL" name="relation">
            <pre class="sql-code">{{ relationSQL }}</pre>
          </el-tab-pane>
          <el-tab-pane label="完整SQL" name="full">
            <pre class="sql-code">{{ fullSQL }}</pre>
          </el-tab-pane>
        </el-tabs>
        <div class="dialog-footer">
          <el-button @click="sqlPreviewVisible = false">关闭</el-button>
          <el-button type="primary" @click="copySQL">复制SQL</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 应用模板对话框 -->
    <el-dialog v-model="templateDialogVisible" title="应用表模板" width="50%" append-to-body>
      <el-form :model="templateForm" label-width="120px">
        <el-form-item label="模板类型">
          <el-select v-model="templateForm.type" style="width: 100%">
            <el-option label="通用基础表" value="basic"/>
            <el-option label="用户信息表" value="user"/>
            <el-option label="订单表" value="order"/>
            <el-option label="产品表" value="product"/>
            <el-option label="日志表" value="log"/>
          </el-select>
        </el-form-item>
        <el-form-item label="表前缀">
          <el-input v-model="templateForm.prefix" placeholder="请输入表前缀"></el-input>
        </el-form-item>
        <el-form-item label="覆盖现有字段">
          <el-switch v-model="templateForm.override"/>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="templateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="applyTemplate">应用</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 导入表结构对话框 -->
    <el-dialog v-model="importDialogVisible" title="导入表结构" width="50%">
      <el-tabs v-model="importTabActive">
        <el-tab-pane label="SQL导入" name="sql">
          <el-form :model="importForm" label-width="100px">
            <el-form-item label="SQL脚本">
              <el-input
                  v-model="importForm.sql"
                  type="textarea"
                  :rows="10"
                  placeholder="请输入建表SQL脚本"
              />
            </el-form-item>
            <el-form-item label="数据库类型">
              <el-select v-model="importForm.dbType" style="width: 100%">
                <el-option label="MySQL" value="mysql"/>
                <el-option label="PostgreSQL" value="postgresql"/>
                <el-option label="SQLite" value="sqlite"/>
                <el-option label="Oracle" value="oracle"/>
                <el-option label="SQL Server" value="sqlserver"/>
              </el-select>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="JSON导入" name="json">
          <el-upload
              class="upload-demo"
              action="#"
              :auto-upload="false"
              :on-change="handleFileChange"
              :limit="1"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">请上传JSON格式的表结构文件</div>
            </template>
          </el-upload>
          <div v-if="importForm.file" class="selected-file">
            已选择文件: {{ importForm.file.name }}
          </div>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="importDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmImport">导入</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Delete, Download, Upload, Document, CircleCheck, Collection,
  Search, Refresh, Grid, Setting, View, Edit, Back, Right, ZoomIn, ZoomOut,
  FullScreen, ArrowUp, ArrowDown, Close, Switch, Connection, Share, Link,
  Aim, AddLocation, Timer, Paperclip, List, SetUp, Files, Key, InfoFilled,
  NoSmoking, Sort, Grape
} from '@element-plus/icons-vue'

// 数据类型选项
const dataTypes = ref([
  { label: '字符串', value: 'varchar' },
  { label: '文本', value: 'text' },
  { label: '整数', value: 'int' },
  { label: '长整数', value: 'bigint' },
  { label: '小数', value: 'decimal' },
  { label: '浮点数', value: 'float' },
  { label: '双精度', value: 'double' },
  { label: '布尔值', value: 'boolean' },
  { label: '日期', value: 'date' },
  { label: '时间', value: 'time' },
  { label: '日期时间', value: 'datetime' },
  { label: '时间戳', value: 'timestamp' },
  { label: 'JSON', value: 'json' },
  { label: '二进制', value: 'blob' }
])

// 设计器模式
const designMode = ref('structure') // 'structure' | 'relation'
const viewMode = ref('visual') // 'visual' | 'table'
const loading = ref(false)

// 表单数据
const tableForm = reactive({
  id: '',
  name: '',
  displayName: '',
  description: '',
  fields: [],
  relations: []
})

// 撤销/重做历史记录
const history = reactive({
  past: [],
  future: []
})

// 计算是否可以撤销/重做
const canUndo = computed(() => history.past.length > 0)
const canRedo = computed(() => history.future.length > 0)

// 选中的字段和关系
const selectedFields = ref([])
const selectedRelations = ref([])
const selectedField = ref(null)
const selectedRelation = ref(null)

// SQL预览相关
const sqlPreviewVisible = ref(false)
const sqlTabActive = ref('structure')
const structureSQL = ref('')
const relationSQL = ref('')
const fullSQL = ref('')

// 应用模板相关
const templateDialogVisible = ref(false)
const templateForm = reactive({
  type: 'basic',
  prefix: '',
  override: false
})

// 导入表结构相关
const importDialogVisible = ref(false)
const importTabActive = ref('sql')
const importForm = reactive({
  sql: '',
  dbType: 'mysql',
  file: null,
  jsonData: null
})

// 可用的表列表（用于关系设计）
const availableTables = ref([
  { id: '1', name: '用户表' },
  { id: '2', name: '订单表' },
  { id: '3', name: '产品表' }
])

// 初始化数据
onMounted(() => {
  // 如果有传入的表ID，则加载表数据
  if (props.tableId) {
    loadTableData()
  } else {
    // 初始化一个空表，添加一些默认字段
    initEmptyTable()
  }
})

// 属性
const props = defineProps({
  tableId: {
    type: String,
    default: ''
  },
  modelId: {
    type: String,
    default: ''
  }
})

// 事件
const emit = defineEmits(['save-success', 'cancel'])

// 初始化空表
const initEmptyTable = () => {
  tableForm.fields = [
    {
      id: generateId(),
      name: 'id',
      displayName: 'ID',
      type: 'int',
      length: 11,
      precision: 0,
      isPrimaryKey: true,
      isNotNull: true,
      isUnique: true,
      isIndexed: true,
      isAutoIncrement: true,
      defaultValue: '',
      description: '主键ID'
    },
    {
      id: generateId(),
      name: 'create_time',
      displayName: '创建时间',
      type: 'datetime',
      length: 0,
      precision: 0,
      isPrimaryKey: false,
      isNotNull: true,
      isUnique: false,
      isIndexed: true,
      isAutoIncrement: false,
      defaultValue: 'CURRENT_TIMESTAMP',
      description: '创建时间'
    },
    {
      id: generateId(),
      name: 'update_time',
      displayName: '更新时间',
      type: 'datetime',
      length: 0,
      precision: 0,
      isPrimaryKey: false,
      isNotNull: true,
      isUnique: false,
      isIndexed: false,
      isAutoIncrement: false,
      defaultValue: 'CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
      description: '最后更新时间'
    }
  ]
  
  // 保存初始状态到历史记录
  saveToHistory()
}

// 加载表数据
const loadTableData = async () => {
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 500))
    
    // 这里应该是真实的API调用
    // const res = await getTableDetail(props.tableId)
    // tableForm.value = res.data
    
    // 模拟数据
    Object.assign(tableForm, {
      id: props.tableId,
      name: '示例表',
      displayName: '示例表',
      description: '这是一个示例表',
      fields: [
        {
          id: generateId(),
          name: 'id',
          displayName: 'ID',
          type: 'int',
          length: 11,
          precision: 0,
          isPrimaryKey: true,
          isNotNull: true,
          isUnique: true,
          isIndexed: true,
          isAutoIncrement: true,
          defaultValue: '',
          description: '主键ID'
        },
        {
          id: generateId(),
          name: 'name',
          displayName: '名称',
          type: 'varchar',
          length: 255,
          precision: 0,
          isPrimaryKey: false,
          isNotNull: true,
          isUnique: false,
          isIndexed: true,
          isAutoIncrement: false,
          defaultValue: '',
          description: '名称'
        }
      ],
      relations: []
    })
    
    // 保存初始状态到历史记录
    saveToHistory()
  } catch (error) {
    console.error('加载表数据失败', error)
    ElMessage.error('加载表数据失败')
  } finally {
    loading.value = false
  }
}

// 保存表设计
const handleSave = async () => {
  // 表单验证
  if (!tableForm.name) {
    ElMessage.warning('请输入表名称')
    return
  }
  
  if (!tableForm.fields.length) {
    ElMessage.warning('表至少需要一个字段')
    return
  }
  
  // 验证字段名称
  for (const field of tableForm.fields) {
    if (!field.name) {
      ElMessage.warning('字段名称不能为空')
      return
    }
  }
  
  loading.value = true
  try {
    // 模拟API调用
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    // 这里应该是真实的API调用
    // const res = await saveTableDesign(tableForm)
    
    ElMessage.success('保存成功')
    emit('save-success', tableForm)
  } catch (error) {
    console.error('保存失败', error)
    ElMessage.error('保存失败')
  } finally {
    loading.value = false
  }
}

// 应用模板
const handleApplyTemplate = () => {
  templateDialogVisible.value = true
}

// 应用模板
const applyTemplate = () => {
  const templates = {
    basic: [
      { name: 'id', displayName: 'ID', type: 'int', isPrimaryKey: true, isNotNull: true, isAutoIncrement: true },
      { name: 'create_time', displayName: '创建时间', type: 'datetime', isNotNull: true, defaultValue: 'CURRENT_TIMESTAMP' },
      { name: 'update_time', displayName: '更新时间', type: 'datetime', isNotNull: true },
      { name: 'remark', displayName: '备注', type: 'varchar', length: 255 }
    ],
    user: [
      { name: 'id', displayName: 'ID', type: 'int', isPrimaryKey: true, isNotNull: true, isAutoIncrement: true },
      { name: 'username', displayName: '用户名', type: 'varchar', length: 50, isNotNull: true, isUnique: true },
      { name: 'password', displayName: '密码', type: 'varchar', length: 100, isNotNull: true },
      { name: 'email', displayName: '邮箱', type: 'varchar', length: 100, isUnique: true },
      { name: 'mobile', displayName: '手机号', type: 'varchar', length: 20 },
      { name: 'status', displayName: '状态', type: 'tinyint', length: 1, defaultValue: '1' },
      { name: 'create_time', displayName: '创建时间', type: 'datetime', isNotNull: true, defaultValue: 'CURRENT_TIMESTAMP' },
      { name: 'update_time', displayName: '更新时间', type: 'datetime', isNotNull: true }
    ],
    // 更多模板...
  }
  
  const selectedTemplate = templates[templateForm.type]
  if (!selectedTemplate) {
    ElMessage.error('模板不存在')
    return
  }
  
  // 保存当前状态到历史记录
  saveToHistory()
  
  if (templateForm.override) {
    // 覆盖现有字段
    tableForm.fields = selectedTemplate.map(field => ({
      id: generateId(),
      ...field,
      name: templateForm.prefix ? `${templateForm.prefix}_${field.name}` : field.name
    }))
  } else {
    // 合并字段，避免重复
    const existingFieldNames = tableForm.fields.map(f => f.name)
    const newFields = selectedTemplate
      .filter(field => !existingFieldNames.includes(templateForm.prefix ? `${templateForm.prefix}_${field.name}` : field.name))
      .map(field => ({
        id: generateId(),
        ...field,
        name: templateForm.prefix ? `${templateForm.prefix}_${field.name}` : field.name
      }))
    
    tableForm.fields = [...tableForm.fields, ...newFields]
  }
  
  templateDialogVisible.value = false
  ElMessage.success('模板应用成功')
}

// 添加字段
const addField = () => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  const newField = {
    id: generateId(),
    name: `field_${tableForm.fields.length + 1}`,
    displayName: `字段${tableForm.fields.length + 1}`,
    type: 'varchar',
    length: 255,
    precision: 0,
    isPrimaryKey: false,
    isNotNull: false,
    isUnique: false,
    isIndexed: false,
    isAutoIncrement: false,
    defaultValue: '',
    description: ''
  }
  
  tableForm.fields.push(newField)
  selectedField.value = newField
}

// 移除字段
const removeField = (index) => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  tableForm.fields.splice(index, 1)
  
  if (selectedField.value && selectedField.value.id === tableForm.fields[index]?.id) {
    selectedField.value = null
  }
}

// 移除选中的字段
const removeSelectedFields = () => {
  if (!selectedFields.value.length) return
  
  // 保存当前状态到历史记录
  saveToHistory()
  
  const selectedIds = selectedFields.value.map(field => field.id)
  tableForm.fields = tableForm.fields.filter(field => !selectedIds.includes(field.id))
  selectedField.value = null
}

// 上下移动字段
const moveField = (index, direction) => {
  if (direction === 'up' && index > 0) {
    // 保存当前状态到历史记录
    saveToHistory()
    
    const temp = tableForm.fields[index]
    tableForm.fields[index] = tableForm.fields[index - 1]
    tableForm.fields[index - 1] = temp
  } else if (direction === 'down' && index < tableForm.fields.length - 1) {
    // 保存当前状态到历史记录
    saveToHistory()
    
    const temp = tableForm.fields[index]
    tableForm.fields[index] = tableForm.fields[index + 1]
    tableForm.fields[index + 1] = temp
  }
}

// 表格多选事件
const handleSelectionChange = (selection) => {
  selectedFields.value = selection
}

// 关系多选事件
const handleRelationSelectionChange = (selection) => {
  selectedRelations.value = selection
}

// 处理主键变更
const handlePrimaryKeyChange = (field) => {
  if (field.isPrimaryKey) {
    // 如果设置为主键，则自动设置为非空
    field.isNotNull = true
    
    // 如果是整数类型，默认设置为自增
    if (['int', 'bigint', 'integer'].includes(field.type)) {
      field.isAutoIncrement = true
    }
    
    // 可选：取消其他字段的主键标识
    tableForm.fields.forEach(f => {
      if (f.id !== field.id && f.isPrimaryKey) {
        f.isPrimaryKey = false
      }
    })
  }
}

// 添加关系
const addRelation = () => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  const newRelation = {
    id: generateId(),
    name: `relation_${tableForm.relations.length + 1}`,
    type: 'one-to-many',
    sourceTable: '',
    sourceField: '',
    targetTable: '',
    targetField: '',
    cascadeDelete: false,
    cascadeUpdate: false,
    description: ''
  }
  
  tableForm.relations.push(newRelation)
  selectedRelation.value = newRelation
}

// 移除关系
const removeRelation = (index) => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  tableForm.relations.splice(index, 1)
  
  if (selectedRelation.value && selectedRelation.value.id === tableForm.relations[index]?.id) {
    selectedRelation.value = null
  }
}

// 移除选中的关系
const removeSelectedRelations = () => {
  if (!selectedRelations.value.length) return
  
  // 保存当前状态到历史记录
  saveToHistory()
  
  const selectedIds = selectedRelations.value.map(relation => relation.id)
  tableForm.relations = tableForm.relations.filter(relation => !selectedIds.includes(relation.id))
  selectedRelation.value = null
}

// 根据表ID获取该表的字段列表
const getTableFields = (tableId) => {
  // 这里应该是从API获取，这里模拟一些数据
  const fieldMap = {
    '1': [
      { id: '101', name: 'id' },
      { id: '102', name: 'username' },
      { id: '103', name: 'email' }
    ],
    '2': [
      { id: '201', name: 'id' },
      { id: '202', name: 'order_no' },
      { id: '203', name: 'user_id' }
    ],
    '3': [
      { id: '301', name: 'id' },
      { id: '302', name: 'product_name' },
      { id: '303', name: 'price' }
    ]
  }
  
  return tableId ? (fieldMap[tableId] || []) : []
}

// 关闭属性面板
const closePropertiesPanel = () => {
  selectedField.value = null
}

// 关闭关系属性面板
const closeRelationPropertiesPanel = () => {
  selectedRelation.value = null
}

// 判断是否需要长度字段
const needLength = (type) => {
  return ['varchar', 'char', 'int', 'bigint', 'tinyint'].includes(type)
}

// 判断是否需要精度字段
const needPrecision = (type) => {
  return ['decimal', 'float', 'double'].includes(type)
}

// 生成SQL预览
const previewSQL = () => {
  // 生成表结构SQL
  generateStructureSQL()
  
  // 如果有关系，生成关系SQL
  if (tableForm.relations.length > 0) {
    generateRelationSQL()
  } else {
    relationSQL.value = '-- 没有定义表关系'
  }
  
  // 生成完整SQL
  fullSQL.value = structureSQL.value + '\n\n' + relationSQL.value
  
  // 显示SQL预览对话框
  sqlPreviewVisible.value = true
}

// 生成表结构SQL
const generateStructureSQL = () => {
  // 这里实现SQL生成逻辑
  // 这是一个简化的MySQL风格SQL生成
  let sql = `CREATE TABLE \`${tableForm.name}\` (\n`
  
  // 字段定义
  const fieldSQLs = tableForm.fields.map(field => {
    let fieldSQL = `  \`${field.name}\` ${field.type.toUpperCase()}`
    
    // 添加长度
    if (needLength(field.type) && field.length) {
      fieldSQL += `(${field.length})`
    } else if (needPrecision(field.type) && (field.length || field.precision)) {
      fieldSQL += `(${field.length || 10}, ${field.precision || 0})`
    }
    
    // 添加约束
    if (field.isNotNull) {
      fieldSQL += ' NOT NULL'
    }
    
    if (field.defaultValue) {
      fieldSQL += ` DEFAULT ${field.defaultValue}`
    }
    
    if (field.isAutoIncrement) {
      fieldSQL += ' AUTO_INCREMENT'
    }
    
    // 添加注释
    if (field.description) {
      fieldSQL += ` COMMENT '${field.description}'`
    }
    
    return fieldSQL
  })
  
  sql += fieldSQLs.join(',\n')
  
  // 主键
  const primaryKey = tableForm.fields.find(field => field.isPrimaryKey)
  if (primaryKey) {
    sql += `,\n  PRIMARY KEY (\`${primaryKey.name}\`)`
  }
  
  // 唯一约束
  const uniqueFields = tableForm.fields.filter(field => field.isUnique && !field.isPrimaryKey)
  if (uniqueFields.length) {
    uniqueFields.forEach(field => {
      sql += `,\n  UNIQUE KEY \`uk_${field.name}\` (\`${field.name}\`)`
    })
  }
  
  // 索引
  const indexedFields = tableForm.fields.filter(field => field.isIndexed && !field.isPrimaryKey && !field.isUnique)
  if (indexedFields.length) {
    indexedFields.forEach(field => {
      sql += `,\n  INDEX \`idx_${field.name}\` (\`${field.name}\`)`
    })
  }
  
  sql += '\n) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci'
  
  if (tableForm.description) {
    sql += ` COMMENT='${tableForm.description}'`
  }
  
  sql += ';'
  
  structureSQL.value = sql
}

// 生成关系SQL
const generateRelationSQL = () => {
  // 这里实现关系SQL生成逻辑
  let sql = ''
  
  tableForm.relations.forEach(relation => {
    if (relation.type === 'one-to-many' || relation.type === 'one-to-one' || relation.type === 'reference') {
      // 获取源表和目标表名称（这里应该从availableTables中获取）
      const sourceTable = availableTables.value.find(t => t.id === relation.sourceTable)?.name || relation.sourceTable
      const targetTable = availableTables.value.find(t => t.id === relation.targetTable)?.name || relation.targetTable
      
      // 获取源字段和目标字段名称
      const sourceField = getTableFields(relation.sourceTable)
                            .find(f => f.id === relation.sourceField)?.name || relation.sourceField
      const targetField = getTableFields(relation.targetTable)
                            .find(f => f.id === relation.targetField)?.name || relation.targetField
      
      sql += `-- ${relation.type === 'one-to-many' ? '一对多' : relation.type === 'one-to-one' ? '一对一' : '外键引用'} 关系: ${sourceTable}.${sourceField} -> ${targetTable}.${targetField}\n`
      sql += `ALTER TABLE \`${targetTable}\` ADD CONSTRAINT \`fk_${targetTable}_${targetField}\` FOREIGN KEY (\`${targetField}\`) REFERENCES \`${sourceTable}\`(\`${sourceField}\`)`
      
      // 添加级联选项
      if (relation.cascadeDelete && relation.cascadeUpdate) {
        sql += ' ON DELETE CASCADE ON UPDATE CASCADE'
      } else if (relation.cascadeDelete) {
        sql += ' ON DELETE CASCADE ON UPDATE RESTRICT'
      } else if (relation.cascadeUpdate) {
        sql += ' ON DELETE RESTRICT ON UPDATE CASCADE'
      } else {
        sql += ' ON DELETE RESTRICT ON UPDATE RESTRICT'
      }
      
      sql += ';\n\n'
    } else if (relation.type === 'many-to-many') {
      // 获取源表和目标表名称
      const sourceTable = availableTables.value.find(t => t.id === relation.sourceTable)?.name || relation.sourceTable
      const targetTable = availableTables.value.find(t => t.id === relation.targetTable)?.name || relation.targetTable
      
      // 生成中间表名
      const junctionTable = `${sourceTable}_${targetTable}`
      
      sql += `-- 多对多关系: ${sourceTable} <-> ${targetTable}\n`
      sql += `CREATE TABLE \`${junctionTable}\` (\n`
      sql += `  \`${sourceTable}_id\` INT NOT NULL,\n`
      sql += `  \`${targetTable}_id\` INT NOT NULL,\n`
      sql += `  PRIMARY KEY (\`${sourceTable}_id\`, \`${targetTable}_id\`),\n`
      sql += `  INDEX \`idx_${junctionTable}_${sourceTable}_id\` (\`${sourceTable}_id\`),\n`
      sql += `  INDEX \`idx_${junctionTable}_${targetTable}_id\` (\`${targetTable}_id\`),\n`
      sql += `  CONSTRAINT \`fk_${junctionTable}_${sourceTable}_id\` FOREIGN KEY (\`${sourceTable}_id\`) REFERENCES \`${sourceTable}\`(\`id\`) ON DELETE CASCADE ON UPDATE CASCADE,\n`
      sql += `  CONSTRAINT \`fk_${junctionTable}_${targetTable}_id\` FOREIGN KEY (\`${targetTable}_id\`) REFERENCES \`${targetTable}\`(\`id\`) ON DELETE CASCADE ON UPDATE CASCADE\n`
      sql += `) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`
      
      if (relation.description) {
        sql += ` COMMENT='${relation.description}'`
      }
      
      sql += ';\n\n'
    }
  })
  
  relationSQL.value = sql || '-- 没有定义表关系'
}

// 复制SQL代码
const copySQL = () => {
  // 根据当前激活的选项卡决定复制哪部分SQL
  let sqlToCopy = ''
  if (sqlTabActive.value === 'structure') {
    sqlToCopy = structureSQL.value
  } else if (sqlTabActive.value === 'relation') {
    sqlToCopy = relationSQL.value
  } else {
    sqlToCopy = fullSQL.value
  }
  
  // 复制到剪贴板
  navigator.clipboard.writeText(sqlToCopy)
    .then(() => {
      ElMessage.success('SQL已复制到剪贴板')
    })
    .catch(() => {
      ElMessage.error('复制失败')
    })
}

// 打开SQL预览
const previewRelationSQL = () => {
  // 生成关系SQL
  generateRelationSQL()
  
  // 显示SQL预览对话框
  sqlTabActive.value = 'relation'
  sqlPreviewVisible.value = true
}

// 导出表结构
const exportTable = () => {
  const exportData = {
    ...tableForm,
    exportTime: new Date().toISOString()
  }
  
  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${tableForm.name}_structure.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('表结构已导出')
}

// 导出关系
const exportRelations = () => {
  const exportData = {
    tableName: tableForm.name,
    relations: tableForm.relations,
    exportTime: new Date().toISOString()
  }
  
  const blob = new Blob([JSON.stringify(exportData, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = `${tableForm.name}_relations.json`
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
  
  ElMessage.success('表关系已导出')
}

// 导入表结构
const importTable = () => {
  importDialogVisible.value = true
}

// 文件变更处理
const handleFileChange = (file) => {
  importForm.file = file.raw
  
  // 读取文件内容
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const jsonData = JSON.parse(e.target.result)
      importForm.jsonData = jsonData
    } catch (error) {
      ElMessage.error('JSON格式错误，请检查文件内容')
    }
  }
  reader.readAsText(file.raw)
}

// 确认导入
const confirmImport = () => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  if (importTabActive.value === 'sql') {
    // SQL导入逻辑
    if (!importForm.sql.trim()) {
      ElMessage.warning('请输入SQL脚本')
      return
    }
    
    // TODO: 解析SQL脚本，提取表结构信息
    ElMessage.info('SQL解析功能开发中，敬请期待')
    
  } else if (importTabActive.value === 'json') {
    // JSON导入逻辑
    if (!importForm.jsonData) {
      ElMessage.warning('请选择JSON文件')
      return
    }
    
    try {
      // 验证导入的数据结构
      if (!importForm.jsonData.name || !Array.isArray(importForm.jsonData.fields)) {
        throw new Error('JSON格式不正确，缺少必要的字段')
      }
      
      // 更新表单数据
      tableForm.name = importForm.jsonData.name
      tableForm.displayName = importForm.jsonData.displayName || importForm.jsonData.name
      tableForm.description = importForm.jsonData.description || ''
      
      // 确保每个字段都有id
      tableForm.fields = importForm.jsonData.fields.map(field => ({
        ...field,
        id: field.id || generateId()
      }))
      
      // 如果有关系数据，也导入
      if (Array.isArray(importForm.jsonData.relations)) {
        tableForm.relations = importForm.jsonData.relations.map(relation => ({
          ...relation,
          id: relation.id || generateId()
        }))
      }
      
      ElMessage.success('导入成功')
      importDialogVisible.value = false
      
    } catch (error) {
      console.error('导入失败', error)
      ElMessage.error(`导入失败: ${error.message}`)
    }
  }
}

// 撤销操作
const handleUndo = () => {
  if (history.past.length === 0) return
  
  // 将当前状态保存到future
  const current = JSON.parse(JSON.stringify({
    fields: tableForm.fields,
    relations: tableForm.relations
  }))
  history.future.push(current)
  
  // 从past中取出最新的状态
  const previous = history.past.pop()
  
  // 更新表单数据
  tableForm.fields = previous.fields
  tableForm.relations = previous.relations
}

// 重做操作
const handleRedo = () => {
  if (history.future.length === 0) return
  
  // 将当前状态保存到past
  const current = JSON.parse(JSON.stringify({
    fields: tableForm.fields,
    relations: tableForm.relations
  }))
  history.past.push(current)
  
  // 从future中取出最新的状态
  const next = history.future.pop()
  
  // 更新表单数据
  tableForm.fields = next.fields
  tableForm.relations = next.relations
}

// 保存当前状态到历史记录
const saveToHistory = () => {
  const current = JSON.parse(JSON.stringify({
    fields: tableForm.fields,
    relations: tableForm.relations
  }))
  history.past.push(current)
  history.future = [] // 清空future
}

// 生成唯一ID
const generateId = () => {
  return 'id_' + Math.random().toString(36).substr(2, 9)
}

// 表格视图和可视化视图切换
const handleSwitchToCanvas = () => {
  viewMode.value = 'visual'
}

const handleSwitchToList = () => {
  viewMode.value = 'table'
}

// 拖拽相关
const handleDragStart = (event, type) => {
  event.dataTransfer.setData('type', type)
}

const handleDrop = (event) => {
  event.preventDefault()
  const type = event.dataTransfer.getData('type')
  
  if (designMode.value === 'structure') {
    // 添加字段
    if (type === 'string') {
      addFieldWithType('varchar', 255)
    } else if (type === 'number') {
      addFieldWithType('int', 11)
    } else if (type === 'datetime') {
      addFieldWithType('datetime')
    } else if (type === 'boolean') {
      addFieldWithType('boolean')
    } else if (type === 'json') {
      addFieldWithType('json')
    } else if (type === 'array') {
      addFieldWithType('text') // 使用text来存储数组JSON
    } else if (type === 'enum') {
      addFieldWithType('enum', null, "('value1','value2','value3')")
    } else if (type === 'binary') {
      addFieldWithType('blob')
    }
    // 字段约束
    else if (type === 'pk') {
      if (selectedField.value) {
        selectedField.value.isPrimaryKey = true
        handlePrimaryKeyChange(selectedField.value)
      }
    } else if (type === 'unique') {
      if (selectedField.value) {
        selectedField.value.isUnique = true
      }
    } else if (type === 'not-null') {
      if (selectedField.value) {
        selectedField.value.isNotNull = true
      }
    } else if (type === 'index') {
      if (selectedField.value) {
        selectedField.value.isIndexed = true
      }
    }
  } else if (designMode.value === 'relation') {
    // 添加关系
    if (['one-to-one', 'one-to-many', 'many-to-many', 'reference'].includes(type)) {
      addRelationWithType(type)
    }
  }
}

// 添加特定类型的字段
const addFieldWithType = (type, length = null, extra = null) => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  const newField = {
    id: generateId(),
    name: `field_${tableForm.fields.length + 1}`,
    displayName: `字段${tableForm.fields.length + 1}`,
    type: type,
    length: length,
    precision: 0,
    isPrimaryKey: false,
    isNotNull: false,
    isUnique: false,
    isIndexed: false,
    isAutoIncrement: false,
    defaultValue: '',
    description: ''
  }
  
  if (extra) {
    newField.extra = extra
  }
  
  tableForm.fields.push(newField)
  selectedField.value = newField
}

// 添加特定类型的关系
const addRelationWithType = (type) => {
  // 保存当前状态到历史记录
  saveToHistory()
  
  const newRelation = {
    id: generateId(),
    name: `relation_${tableForm.relations.length + 1}`,
    type: type,
    sourceTable: '',
    sourceField: '',
    targetTable: '',
    targetField: '',
    cascadeDelete: false,
    cascadeUpdate: false,
    description: ''
  }
  
  tableForm.relations.push(newRelation)
  selectedRelation.value = newRelation
}
</script>

<style lang="scss" scoped>
.table-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  
  .mode-switch {
    padding: 16px 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
    
    .mode-left {
      flex: 1;
    }
    
    .mode-right {
      display: flex;
      align-items: center;
      gap: 10px;
      
      .model-name-input {
        width: 300px;
      }
    }
  }
  
  .view-mode-switch {
    padding: 8px 20px;
    background-color: #f0f2f5;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    align-items: center;
  }
  
  .designer-toolbar {
    height: 50px;
    padding: 0 20px;
    background: #fff;
    border-bottom: 1px solid #dcdfe6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    
    .left-tools, .right-tools {
      display: flex;
      align-items: center;
      gap: 10px;
    }
  }
  
  .designer-main {
    flex: 1;
    min-height: 0;
    display: flex;
    overflow: hidden;
    
    .component-panel {
      width: 250px;
      background: #fff;
      border-right: 1px solid #dcdfe6;
      overflow-y: auto;
      flex-shrink: 0;
      
      .component-group {
        padding: 10px;
        
        .group-title {
          font-size: 14px;
          color: #606266;
          margin-bottom: 10px;
        }
        
        .component-list {
          display: grid;
          grid-template-columns: repeat(2, 1fr);
          gap: 10px;
        }
        
        .component-item {
          height: 80px;
          background: #f5f7fa;
          border: 1px dashed #dcdfe6;
          border-radius: 4px;
          display: flex;
          flex-direction: column;
          align-items: center;
          justify-content: center;
          gap: 8px;
          cursor: move;
          transition: all 0.3s;
          
          &:hover {
            background: #ecf5ff;
            border-color: #409eff;
          }
          
          .el-icon {
            font-size: 24px;
            color: #409eff;
          }
          
          span {
            font-size: 12px;
            color: #606266;
          }
        }
      }
    }
    
    .fields-container {
      flex: 1;
      padding: 16px;
      background: #fff;
      overflow: auto;
      
      &.full-width {
        flex: 1;
      }
      
      .fields-table {
        width: 100%;
      }
    }
    
    .properties-panel {
      width: 320px;
      background: #fff;
      border-left: 1px solid #dcdfe6;
      overflow-y: auto;
      flex-shrink: 0;
      padding: 16px;
      
      .panel-title {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        font-size: 16px;
        font-weight: 500;
      }
    }
  }
  
  .sql-preview {
    .sql-code {
      background-color: #282c34;
      color: #abb2bf;
      padding: 16px;
      border-radius: 4px;
      font-family: 'Courier New', Courier, monospace;
      font-size: 14px;
      line-height: 1.5;
      overflow-x: auto;
      white-space: pre-wrap;
      height: 400px;
      overflow-y: auto;
    }
    
    .dialog-footer {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  }
  
  .selected-file {
    margin-top: 12px;
    padding: 8px;
    background-color: #f0f7ff;
    border-radius: 4px;
    border: 1px solid #409eff;
    color: #409eff;
  }
}

:deep(.el-checkbox) {
  margin-right: 0;
}

:deep(.el-tabs__nav-wrap) {
  padding: 0 12px;
}

:deep(.el-select) {
  width: 100%;
}

:deep(.el-input-number) {
  width: 100%;
}

:deep(.el-table .cell) {
  padding: 8px;
}

.form-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.canvas-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #909399;
  font-size: 16px;
  text-align: center;
}

.full-width {
  width: 100%;
}
</style> 