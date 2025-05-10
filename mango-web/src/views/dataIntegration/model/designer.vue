<template>
  <div class="model-designer">
    <!-- 模式切换 -->
    <div class="mode-switch">
      <div class="mode-left">
        <el-radio-group v-model="designMode" size="large">
          <el-radio-button value="entity">实体关系建模</el-radio-button>
          <el-radio-button value="dimension">维度模型设计</el-radio-button>
        </el-radio-group>
      </div>

      <div class="mode-right">
        <div class="model-name-input">
          <el-input
              v-model="modelForm.name"
              placeholder="请输入模型名称"
              size="large"
              clearable
          >
            <template #prepend>模型名称</template>
          </el-input>
        </div>
        <el-button type="primary" size="large" @click="handleSave">
          <el-icon>
            <CircleCheck/>
          </el-icon>
          保存模型
        </el-button>
        <el-button type="success" size="large" @click="standardsDialogVisible = true">
          <el-icon>
            <Collection/>
          </el-icon>
          应用标准
        </el-button>
      </div>
    </div>

    <!-- 视图切换按钮组 -->
    <div class="view-mode-switch">
      <el-button-group>
        <el-button
            :type="viewMode === 'canvas' ? 'primary' : 'default'"
            @click="handleSwitchToCanvas"
            size="small">
          <i class="el-icon-picture"></i> 画布视图
        </el-button>
        <el-button
            :type="viewMode === 'list' ? 'primary' : 'default'"
            @click="handleSwitchToList"
            size="small">
          <i class="el-icon-document"></i> 列表视图
        </el-button>
      </el-button-group>
    </div>

    <!-- 画布视图 -->
    <div v-if="viewMode === 'canvas'">
      <!-- 实体关系建模模式 -->
      <template v-if="designMode === 'entity'">
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
              <el-button @click="handleZoomIn">
                <el-icon>
                  <ZoomIn/>
                </el-icon>
                放大
              </el-button>
              <el-button @click="handleZoomOut">
                <el-icon>
                  <ZoomOut/>
                </el-icon>
                缩小
              </el-button>
              <el-button @click="handleFitContent">
                <el-icon>
                  <FullScreen/>
                </el-icon>
                适应内容
              </el-button>
            </el-button-group>
            <el-divider direction="vertical"/>
          </div>
          <div class="right-tools">
            <el-button-group>
              <el-button>SQL预览</el-button>
              <el-button @click="exportModel">
                <el-icon>
                  <Download/>
                </el-icon>
                导出模型
              </el-button>
              <el-button @click="showImportModel">
                <el-icon>
                  <Upload/>
                </el-icon>
                导入模型
              </el-button>
            </el-button-group>
          </div>
        </div>

        <!-- 主设计区域 -->
        <div class="designer-main">
          <!-- 左侧组件面板 -->
          <div class="component-panel">
            <el-tabs>
              <el-tab-pane label="模型组件">
                <div class="component-group">
                  <div class="group-title">实体组件</div>
                  <div class="component-list">
                    <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'entity-table')">
                      <el-icon>
                        <Grid/>
                      </el-icon>
                      <span>实体表</span>
                    </div>
                    <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'relation')">
                      <el-icon>
                        <Share/>
                      </el-icon>
                      <span>关联关系</span>
                    </div>
                  </div>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>

          <!-- 中间画布区域 -->
          <div class="canvas-container" ref="canvasRef"
               @dragover.prevent
               @drop="handleDrop">
            <div v-if="!nodes.length" class="canvas-placeholder">
              从左侧拖拽组件到此处开始设计
            </div>

            <!-- 列表视图 -->
            <div v-else-if="viewMode === 'list'" class="list-container">
              <div class="list-header">
                <h3>实体表列表</h3>
                <el-input
                    v-model="listSearchKeyword"
                    placeholder="搜索实体表"
                    prefix-icon="Search"
                    clearable
                    style="width: 250px;"
                />
              </div>

              <!-- 实体表列表 -->
              <el-table
                  :data="filteredEntityTables || entityTables"
                  style="width: 100%"
                  @row-click="handleEntityTableClick"
              >
                <el-table-column prop="id" label="ID" width="180"/>
                <el-table-column prop="name" label="表名" width="180"/>
                <el-table-column prop="type" label="类型">
                  <template #default="{ row }">
                    <el-tag :type="getEntityTypeTag(row.type)">{{ getEntityTypeName(row.type) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="fields" label="字段数量">
                  <template #default="{ row }">
                    {{ row.fields ? row.fields.length : 0 }}
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <el-button type="primary" link @click.stop="editEntityTable(row)">
                      <el-icon>
                        <Edit/>
                      </el-icon>
                      编辑
                    </el-button>
                    <el-button type="danger" link @click.stop="deleteEntityTable(row)">
                      <el-icon>
                        <Delete/>
                      </el-icon>
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <!-- 关系列表 -->
              <div class="list-header" style="margin-top: 20px;">
                <h3>关系列表</h3>
                <el-input
                    v-model="relationSearchKeyword"
                    placeholder="搜索关系"
                    prefix-icon="Search"
                    clearable
                    style="width: 250px;"
                />
              </div>

              <el-table
                  :data="filteredRelations || relations"
                  style="width: 100%"
              >
                <el-table-column prop="id" label="ID" width="180"/>
                <el-table-column prop="name" label="关系名称" width="180"/>
                <el-table-column prop="type" label="关系类型">
                  <template #default="{ row }">
                    <el-tag :type="getRelationTypeTag(row.type)">{{ getRelationTypeName(row.type) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="sourceTable" label="源表"/>
                <el-table-column prop="targetTable" label="目标表"/>
                <el-table-column label="操作" width="200">
                  <template #default="{ row }">
                    <el-button type="primary" link @click="editRelation(row)">
                      <el-icon>
                        <Edit/>
                      </el-icon>
                      编辑
                    </el-button>
                    <el-button type="danger" link @click="deleteRelation(row)">
                      <el-icon>
                        <Delete/>
                      </el-icon>
                      删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>


          </div>

          <!-- 右侧属性面板 - 使用抽屉组件 -->
          <el-drawer
              v-model="showPropertyPanel"
              direction="rtl"
              size="800px"
              :with-header="true"
              :modal="false"
              :show-close="true"
              custom-class="property-drawer"
              :close-on-click-modal="false"
              :close-on-press-escape="false"
              :title="selectedNode ? getNodeTypeName(selectedNode.type) : '属性面板'"
              @close="handlePropertyPanelClose"
          >
            <div class="property-panel">
              <template v-if="selectedNode">
                <div class="panel-content">
                  <!-- 基本属性 -->
                  <el-form label-position="top">
                    <el-form-item label="名称">
                      <el-input
                          v-model="selectedNode.name"
                          @input="handleNodeNameChange"
                          @change="handleNodeNameChange"
                      />
                    </el-form-item>
                    <!-- 实体表属性配置 -->
                    <template v-if="selectedNode.type === 'entity-table'">
                      <el-form-item label="表描述">
                        <el-input v-model="selectedNode.config.description" type="textarea" :rows="2"
                                  @change="handleNodePropertyChange"/>
                      </el-form-item>
                      <el-form-item label="表类型">
                        <el-select v-model="selectedNode.config.tableType" style="width: 100%"
                                   @change="handleNodePropertyChange">
                          <el-option label="业务实体" value="business"/>
                          <el-option label="维度表" value="dimension"/>
                          <el-option label="事实表" value="fact"/>
                          <el-option label="关联表" value="association"/>
                        </el-select>
                      </el-form-item>

                      <el-divider>字段列表</el-divider>

                      <el-table :data="selectedNode.config.fields" border style="width: 100%">
                        <el-table-column label="字段名" min-width="120">
                          <template #default="{ row }">
                            <el-input
                                v-model="row.name"
                                size="small"
                                @input="handleFieldChange"
                                @change="handleFieldChange"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column label="类型" min-width="120">
                          <template #default="{ row }">
                            <el-select
                                v-model="row.type"
                                size="small"
                                style="width: 100%"
                                @change="(val) => handleFieldTypeChange(row, val)"
                            >
                              <el-option v-for="type in dataTypes" :key="type.value" :label="type.label"
                                         :value="type.value"/>
                            </el-select>
                          </template>
                        </el-table-column>
                        <el-table-column label="长度" width="150" v-if="true">
                          <template #default="{ row }">
                            <el-input-number
                                v-model="row.length"
                                size="small"
                                :min="1"
                                :max="1000"
                                @input="handleFieldChange"
                                @change="handleFieldChange"
                                v-if="needLength(row.type)"
                            />
                            <span v-else>-</span>
                          </template>
                        </el-table-column>
                        <el-table-column label="注释" min-width="120">
                          <template #default="{ row }">
                            <el-input
                                v-model="row.comment"
                                size="small"
                                @input="handleFieldChange"
                                @change="handleFieldChange"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column label="主键" width="60" align="center">
                          <template #default="{ row }">
                            <el-checkbox
                                v-model="row.isPrimary"
                                @change="(val) => handlePrimaryKeyChange(row, val)"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column label="非空" width="60" align="center">
                          <template #default="{ row }">
                            <el-checkbox
                                v-model="row.notNull"
                                @change="handleFieldChange"
                            />
                          </template>
                        </el-table-column>
                        <el-table-column label="操作" width="60" align="center">
                          <template #default="{ $index }">
                            <el-button type="danger" link @click="removeField($index)">
                              <el-icon>
                                <Delete/>
                              </el-icon>
                            </el-button>
                          </template>
                        </el-table-column>
                      </el-table>
                      <div class="field-actions" style="margin-top: 10px;">
                        <el-button type="primary" @click="addEntityField">
                          <el-icon>
                            <Plus/>
                          </el-icon>
                          添加字段
                        </el-button>
                      </div>

                      <el-divider>索引配置</el-divider>

                      <div v-if="!selectedNode.config.indexes || selectedNode.config.indexes.length === 0"
                           class="empty-tip">
                        暂无索引，点击下方按钮添加
                      </div>

                      <div v-else v-for="(index, idx) in selectedNode.config.indexes" :key="idx" class="index-item">
                        <el-form-item label="索引名称">
                          <el-input v-model="index.name"/>
                        </el-form-item>
                        <el-form-item label="索引类型">
                          <el-select v-model="index.type" style="width: 100%">
                            <el-option label="普通索引" value="normal"/>
                            <el-option label="唯一索引" value="unique"/>
                          </el-select>
                        </el-form-item>
                        <el-form-item label="索引字段">
                          <el-select v-model="index.fields" multiple style="width: 100%">
                            <el-option
                                v-for="field in selectedNode.config.fields"
                                :key="field.name"
                                :label="field.name"
                                :value="field.name"
                            />
                          </el-select>
                        </el-form-item>
                        <el-button type="danger" link @click="removeIndex(idx)">
                          <el-icon>
                            <Delete/>
                          </el-icon>
                        </el-button>
                        <el-divider v-if="idx < selectedNode.config.indexes.length - 1"/>
                      </div>

                      <div class="index-actions">
                        <el-button type="primary" @click="addIndex">
                          <el-icon>
                            <Plus/>
                          </el-icon>
                          添加索引
                        </el-button>
                      </div>
                    </template>
                    <!-- 关联关系属性配置 -->
                    <template v-else-if="selectedNode.type === 'relation'">
                      <el-form-item label="关系名称">
                        <el-input v-model="selectedNode.name" @change="handleNodeNameChange"/>
                      </el-form-item>

                      <!-- 确保在使用v-model前初始化config对象 -->
                      <div v-if="!selectedNode.config" v-once>
                        {{ initNodeConfig() }}
                      </div>

                      <el-form-item label="关系类型">
                        <el-select v-model="selectedNode.config.relationType" @change="handleRelationTypeChange">
                          <el-option label="一对一" value="one-to-one"/>
                          <el-option label="一对多" value="one-to-many"/>
                          <el-option label="多对多" value="many-to-many"/>
                        </el-select>
                      </el-form-item>

                      <el-form-item label="源实体">
                        <el-select
                            v-model="selectedNode.config.sourceTable"
                            placeholder="选择源实体"
                            @change="handleSourceTableChange"
                            filterable
                        >
                          <el-option
                              v-for="(node, index) in getEntityTables()"
                              :key="node.id || `entity-${index}`"
                              :label="node.name || `实体表 ${index + 1}`"
                              :value="node.id"
                          />
                        </el-select>
                        <div v-if="getEntityTables().length === 0"
                             style="color: #F56C6C; font-size: 12px; margin-top: 5px;">
                          请先在画布上添加实体表
                        </div>
                      </el-form-item>

                      <el-form-item label="源字段">
                        <el-select
                            v-model="selectedNode.config.sourceField"
                            placeholder="选择源字段"
                            multiple
                            collapse-tags
                            filterable
                            :disabled="!selectedNode.config.sourceTable"
                        >
                          <el-option
                              v-for="(field, index) in getSourceTableFields()"
                              :key="field.name || `source-field-${index}`"
                              :label="field.name || `字段 ${index + 1}`"
                              :value="field.name || `field_${index}`"
                          />
                        </el-select>
                        <div v-if="selectedNode.config.sourceTable && getSourceTableFields().length === 0"
                             style="color: #F56C6C; font-size: 12px; margin-top: 5px;">
                          源表没有字段，请先添加字段
                        </div>
                        <div v-if="!selectedNode.config.sourceTable"
                             style="color: #909399; font-size: 12px; margin-top: 5px;">
                          请先选择源实体
                        </div>
                      </el-form-item>

                      <el-form-item label="目标实体">
                        <el-select
                            v-model="selectedNode.config.targetTable"
                            placeholder="选择目标实体"
                            @change="handleTargetTableChange"
                            filterable
                            :disabled="!selectedNode.config.sourceTable"
                        >
                          <el-option
                              v-for="(node, index) in getEntityTables()"
                              :key="node.id || `entity-${index}`"
                              :label="node.name || `实体表 ${index + 1}`"
                              :value="node.id"
                              :disabled="node.id === selectedNode.config.sourceTable"
                          />
                        </el-select>
                      </el-form-item>

                      <el-form-item label="目标字段">
                        <el-select
                            v-model="selectedNode.config.targetField"
                            placeholder="选择目标字段"
                            multiple
                            collapse-tags
                            filterable
                        >
                          <el-option
                              v-for="(field, index) in targetTableFields"
                              :key="field.name || index"
                              :label="field.name || `字段 ${index + 1}`"
                              :value="field.name || `字段_${index}`"
                          />
                        </el-select>
                      </el-form-item>

                      <el-form-item label="级联操作">
                        <el-select v-model="selectedNode.config.onDelete" placeholder="删除时">
                          <el-option label="级联删除" value="CASCADE"/>
                          <el-option label="设为空值" value="SET NULL"/>
                          <el-option label="不允许删除" value="RESTRICT"/>
                          <el-option label="不做操作" value="NO ACTION"/>
                        </el-select>
                      </el-form-item>

                      <el-form-item label="关系描述">
                        <el-input v-model="selectedNode.config.description" type="textarea" :rows="2"/>
                      </el-form-item>

                      <!-- 多对多关系的中间表配置 -->
                      <template v-if="selectedNode.config.relationType === 'many-to-many'">
                        <el-divider content-position="center">中间表配置</el-divider>

                        <!-- 初始化中间表对象，如果不存在 -->
                        <div v-if="!selectedNode.config.junctionTable" v-once>
                          {{ initJunctionTable() }}
                        </div>

                        <el-form-item label="中间表名称">
                          <el-input
                              v-model="selectedNode.config.junctionTable.name"
                              placeholder="请输入中间表名称"
                          />
                        </el-form-item>

                        <el-form-item label="中间表描述">
                          <el-input
                              v-model="selectedNode.config.junctionTable.description"
                              type="textarea"
                              :rows="2"
                          />
                        </el-form-item>

                        <el-form-item label="附加字段">
                          <div style="margin-bottom: 10px;">
                            <el-button type="primary" link @click="addJunctionField">
                              <el-icon>
                                <Plus/>
                              </el-icon>
                              添加字段
                            </el-button>
                          </div>

                          <el-table
                              v-if="selectedNode.config.junctionTable.fields && selectedNode.config.junctionTable.fields.length > 0"
                              :data="selectedNode.config.junctionTable.fields"
                              border
                              size="small"
                          >
                            <el-table-column label="字段名" prop="name" min-width="120">
                              <template #default="{ row }">
                                <el-input v-model="row.name" size="small"/>
                              </template>
                            </el-table-column>

                            <el-table-column label="类型" prop="type" width="120">
                              <template #default="{ row }">
                                <el-select v-model="row.type" size="small">
                                  <el-option v-for="type in dataTypes" :key="type.value" :label="type.label"
                                             :value="type.value"/>
                                </el-select>
                              </template>
                            </el-table-column>

                            <el-table-column label="描述" min-width="150">
                              <template #default="{ row }">
                                <el-input v-model="row.comment" size="small"/>
                              </template>
                            </el-table-column>

                            <el-table-column label="操作" width="80" align="center">
                              <template #default="{ $index }">
                                <el-button type="danger" link size="small" @click="removeJunctionField($index)">
                                  <el-icon>
                                    <Delete/>
                                  </el-icon>
                                </el-button>
                              </template>
                            </el-table-column>
                          </el-table>
                        </el-form-item>
                      </template>

                      <el-form-item>
                        <el-button type="primary" @click="applyRelation">应用关系</el-button>
                      </el-form-item>
                    </template>
                  </el-form>
                </div>
              </template>
              <div v-else-if="showEmptyPropertyPanel" class="panel-placeholder">
                <el-empty
                    description="请选择节点查看属性"
                    :image-size="120"
                >
                  <template #image>
                    <el-icon style="font-size: 80px; color: #909399;">
                      <Edit/>
                    </el-icon>
                  </template>
                  <template #description>
                    <p>请从画布中选择一个节点来查看和编辑其属性</p>
                  </template>
                  <el-button type="primary" size="large" @click="handleAddEntityTable">添加实体表</el-button>
                </el-empty>
              </div>
            </div>
          </el-drawer>
        </div>

        <!-- 预览对话框 -->
        <preview-dialog
            v-model:visible="previewVisible"
            :model-data="previewData || {}"
        />

        <!-- 应用数据标准对话框 -->
        <el-dialog
            v-model="standardsDialogVisible"
            title="应用数据标准"
            width="70%"
            destroy-on-close
        >
          <el-tabs v-model="standardsActiveTab">
            <!-- 新增：已应用标准标签页 -->
            <el-tab-pane label="已应用标准" name="applied">
              <div class="applied-standards-section">
                <h3>已应用的主题域</h3>

                <!-- 调试信息 -->
                <div style="margin-bottom: 10px; color: #666; font-size: 12px;">
                  已加载 {{ Array.isArray(appliedDomainIds) ? appliedDomainIds.length : 0 }} 个主题域
                </div>

                <div class="domain-selector">
                  <!-- 使用更简单的下拉菜单实现，确保数据是数组 -->
                  <el-select
                      v-model="selectedAppliedDomainId"
                      placeholder="请选择主题域查看标准"
                      style="width: 100%"
                      filterable
                  >
                    <el-option
                        v-for="domainId in Array.isArray(appliedDomainIds) ? appliedDomainIds : []"
                        :key="domainId"
                        :label="getDomainName(domainId)"
                        :value="domainId"
                    />
                  </el-select>

                  <!-- 显示所有主题域，便于调试，确保数据是数组 -->
                  <div style="margin-top: 10px; border: 1px dashed #ccc; padding: 10px; border-radius: 4px;">
                    <div style="margin-bottom: 5px; font-weight: bold;">所有已应用主题域:</div>
                    <el-tag
                        v-for="domainId in Array.isArray(appliedDomainIds) ? appliedDomainIds : []"
                        :key="domainId"
                        style="margin-right: 5px; margin-bottom: 5px;"
                    >
                      {{ getDomainName(domainId) }}
                    </el-tag>
                    <div v-if="!Array.isArray(appliedDomainIds) || appliedDomainIds.length === 0" style="color: #999;">
                      暂无已应用的主题域
                    </div>
                  </div>

                  <!-- 主题域操作按钮 -->
                  <div v-if="selectedAppliedDomainId" style="margin-top: 10px; text-align: right;">
                    <el-button
                        type="danger"
                        size="small"
                        @click="removeAppliedDomain(selectedAppliedDomainId)"
                    >
                      <el-icon>
                        <Delete/>
                      </el-icon>
                      移除当前主题域
                    </el-button>
                  </div>
                </div>

                <div v-if="selectedAppliedDomainId" class="domain-standards">
                  <el-divider>
                    <el-tag effect="dark" size="large">{{ getDomainName(selectedAppliedDomainId) }}</el-tag>
                  </el-divider>

                  <h3>必须执行的标准</h3>
                  <el-table
                      :data="Array.isArray(getAppliedStandardsByDomain(selectedAppliedDomainId, true)) ? getAppliedStandardsByDomain(selectedAppliedDomainId, true) : []"
                      style="width: 100%; margin-bottom: 20px;"
                  >
                    <el-table-column prop="name" label="标准名称" min-width="180"/>
                    <el-table-column prop="type" label="标准类型" width="120">
                      <template #default="{ row }">
                        <el-tag :type="getStandardTypeTag(row.type)">{{ getStandardTypeName(row.type) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column label="操作" width="100" fixed="right">
                      <template #default="{ row }">
                        <el-button
                            type="primary"
                            link
                            @click="handleViewStandard(row)"
                        >
                          <el-icon>
                            <View/>
                          </el-icon>
                          查看
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty
                      v-if="!Array.isArray(getAppliedStandardsByDomain(selectedAppliedDomainId, true)) || getAppliedStandardsByDomain(selectedAppliedDomainId, true).length === 0"
                      description="暂无必须执行的标准"
                  />

                  <h3>建议执行的标准</h3>
                  <el-table
                      :data="Array.isArray(getAppliedStandardsByDomain(selectedAppliedDomainId, false)) ? getAppliedStandardsByDomain(selectedAppliedDomainId, false) : []"
                      style="width: 100%"
                  >
                    <el-table-column prop="name" label="标准名称" min-width="180"/>
                    <el-table-column prop="type" label="标准类型" width="120">
                      <template #default="{ row }">
                        <el-tag :type="getStandardTypeTag(row.type)">{{ getStandardTypeName(row.type) }}</el-tag>
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column label="操作" width="120" fixed="right">
                      <template #default="{ row }">
                        <el-button
                            type="primary"
                            link
                            @click="handleViewStandard(row)"
                        >
                          <el-icon>
                            <View/>
                          </el-icon>
                          查看
                        </el-button>
                        <el-button
                            type="danger"
                            link
                            @click="removeAppliedStandard(row)"
                        >
                          <el-icon>
                            <Delete/>
                          </el-icon>
                          移除
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <el-empty
                      v-if="!Array.isArray(getAppliedStandardsByDomain(selectedAppliedDomainId, false)) || getAppliedStandardsByDomain(selectedAppliedDomainId, false).length === 0"
                      description="暂无建议执行的标准"
                  />
                </div>

                <el-empty
                    v-if="!selectedAppliedDomainId"
                    description="请选择主题域查看标准"
                />
              </div>
            </el-tab-pane>

            <!-- 原有的标签页 -->
            <el-tab-pane label="选择标准" name="select">
              <!-- 原有内容保持不变 -->
              <el-form :model="standardsForm" label-width="100px">
                <el-form-item label="应用目标">
                  <el-radio-group v-model="standardsForm.targetType">
                    <el-radio label="model">整个模型</el-radio>
                    <el-radio label="node">当前节点</el-radio>
                  </el-radio-group>
                  <div v-if="standardsForm.targetType === 'node'" class="target-node-info">
                    <el-tag>{{ standardsForm.targetNodeName }}</el-tag>
                  </div>
                </el-form-item>
                <el-form-item label="主题域">
                  <el-select
                      v-model="standardsForm.domainIds"
                      placeholder="请选择主题域"
                      @change="handleDomainChange"
                      multiple
                      collapse-tags
                      collapse-tags-tooltip
                      style="width: 100%"
                  >
                    <el-option
                        v-for="domain in domainOptions"
                        :key="domain.id"
                        :label="domain.name"
                        :value="domain.id"
                    />
                  </el-select>
                </el-form-item>
              </el-form>

              <div v-if="standardsForm.domainIds && standardsForm.domainIds.length > 0" class="standards-section">
                <h3>必须执行的标准</h3>
                <el-table
                    v-loading="standardsLoading"
                    :data="requiredStandards"
                    style="width: 100%; margin-bottom: 20px;"
                >
                  <el-table-column type="selection" width="55" disabled/>
                  <el-table-column prop="name" label="标准名称" min-width="180"/>
                  <el-table-column prop="type" label="标准类型" width="120">
                    <template #default="{ row }">
                      <el-tag :type="getStandardTypeTag(row.type)">{{ getStandardTypeName(row.type) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                  <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" link @click="handleViewStandard(row)">
                        <el-icon>
                          <View/>
                        </el-icon>
                        查看
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>

                <h3>建议执行的标准</h3>
                <el-table
                    v-loading="standardsLoading"
                    :data="suggestedStandards"
                    @selection-change="handleSuggestedStandardsSelectionChange"
                    style="width: 100%"
                >
                  <el-table-column type="selection" width="55"/>
                  <el-table-column prop="name" label="标准名称" min-width="180"/>
                  <el-table-column prop="domainName" label="所属主题域" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" effect="plain">{{ row.domainName }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="type" label="标准类型" width="120">
                    <template #default="{ row }">
                      <el-tag :type="getStandardTypeTag(row.type)">{{ getStandardTypeName(row.type) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                  <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row }">
                      <el-button type="primary" link @click="handleViewStandard(row)">
                        <el-icon>
                          <View/>
                        </el-icon>
                        查看
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>

              <div v-if="!standardsForm.domainIds || standardsForm.domainIds.length === 0" class="empty-domain">
                <el-empty description="请先选择主题域"/>
              </div>

              <div class="dialog-footer" style="margin-top: 20px; text-align: right;">
                <el-button @click="standardsDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="standardsActiveTab = 'config'">下一步</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="配置应用" name="config">
              <div v-if="!hasSelectedStandards" class="empty-standards">
                <el-empty description="没有可应用的标准"/>
                <el-button type="primary" @click="standardsActiveTab = 'select'">返回选择</el-button>
              </div>
              <div v-else class="has-standards">
                <h3>已选择的标准</h3>
                <el-table :data="allSelectedStandards" style="width: 100%; margin-bottom: 20px;">
                  <el-table-column prop="name" label="标准名称" min-width="180"/>
                  <el-table-column prop="domainName" label="所属主题域" width="120">
                    <template #default="{ row }">
                      <el-tag size="small" effect="plain">{{ row.domainName }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="type" label="标准类型" width="120">
                    <template #default="{ row }">
                      <el-tag :type="getStandardTypeTag(row.type)">{{ getStandardTypeName(row.type) }}</el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="required" label="执行类型" width="120">
                    <template #default="{ row }">
                      <el-tag :type="row.required ? 'danger' : 'warning'">
                        {{ row.required ? '必须执行' : '建议执行' }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column label="应用优先级" width="180">
                    <template #default="{ row, $index }">
                      <el-select v-model="row.priority" placeholder="请选择优先级" :disabled="row.required">
                        <el-option label="高" value="high"/>
                        <el-option label="中" value="medium"/>
                        <el-option label="低" value="low"/>
                      </el-select>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100" fixed="right">
                    <template #default="{ row, $index }">
                      <el-button
                          v-if="!row.required"
                          type="danger"
                          link
                          @click="removeSelectedStandard(row)"
                      >
                        <el-icon>
                          <Delete/>
                        </el-icon>
                        移除
                      </el-button>
                      <span v-else>-</span>
                    </template>
                  </el-table-column>
                </el-table>

                <h3>应用范围</h3>
                <el-form :model="standardsForm" label-width="100px">
                  <el-form-item label="应用范围">
                    <el-radio-group v-model="standardsForm.applyScope">
                      <el-radio label="all">应用到所有节点</el-radio>
                      <el-radio label="selected">应用到选中节点</el-radio>
                    </el-radio-group>
                  </el-form-item>
                  <el-form-item label="应用说明">
                    <el-input
                        v-model="standardsForm.description"
                        type="textarea"
                        :rows="3"
                        placeholder="请输入应用说明"
                    />
                  </el-form-item>
                </el-form>

                <div class="dialog-footer" style="margin-top: 20px; text-align: right;">
                  <el-button @click="standardsActiveTab = 'select'">上一步</el-button>
                  <el-button type="primary" @click="applyStandards">确认应用</el-button>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="应用结果" name="result">
              <div class="standards-result">
                <div v-if="standardsApplied" class="success-result">
                  <el-result
                      icon="success"
                      title="标准应用成功"
                      sub-title="所选数据标准已成功应用到模型中"
                  >
                    <template #extra>
                      <el-button type="primary" @click="standardsDialogVisible = false">完成</el-button>
                    </template>
                  </el-result>
                </div>
                <div v-else class="applying-result">
                  <el-result
                      icon="warning"
                      title="标准应用中"
                      sub-title="正在应用数据标准，请稍候..."
                  >
                    <template #extra>
                      <el-progress :percentage="applyProgress"/>
                    </template>
                  </el-result>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-dialog>
      </template>

      <!-- 维度模型设计模式 -->
      <template v-else-if="designMode === 'dimension'">
        <div class="component-panel">
          <el-tabs>
            <el-tab-pane label="模型组件">
              <div class="component-group">
                <div class="group-title">维度组件</div>
                <div class="component-list">
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'fact-table')">
                    <el-icon>
                      <DataBoard/>
                    </el-icon>
                    <span>事实表</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'dimension-table')">
                    <el-icon>
                      <Connection/>
                    </el-icon>
                    <span>维度表</span>
                  </div>
                  <div class="component-item" draggable="true" @dragstart="handleDragStart($event, 'bridge-table')">
                    <el-icon>
                      <Switch/>
                    </el-icon>
                    <span>桥接表</span>
                  </div>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </template>

      <!-- SQL模型编排模式 -->
      <template v-else-if="designMode === 'sql'">
        <div class="sql-orchestration">
          <!-- 操作按钮组 -->
          <div class="operation-bar">
            <div class="left-operations">
              <el-button @click="addField">
                <el-icon>
                  <Plus/>
                </el-icon>
                新增字段
              </el-button>
              <el-button @click="deleteSelectedFields" :disabled="!selectedFields.length">
                <el-icon>
                  <Delete/>
                </el-icon>
                删除
              </el-button>
            </div>
            <div class="right-operations">
              <el-input
                  v-model="searchKeyword"
                  placeholder="搜索模型名称/描述"
                  prefix-icon="Search"
                  clearable
                  @clear="handleSearch"
                  @input="handleSearch"
              />
              <el-button @click="showAdvancedFilter = true">
                <el-icon>
                  <Filter/>
                </el-icon>
                高级筛选
              </el-button>
            </div>
          </div>

          <!-- 模型列表 -->
          <div class="model-list">
            <el-table
                :data="tableForm.fields"
                border
                class="fields-table"
                @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="50"/>
              <el-table-column label="字段名称" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.name" placeholder="字段名称"/>
                </template>
              </el-table-column>
              <el-table-column label="显示名称" min-width="180">
                <template #default="{ row }">
                  <el-input v-model="row.displayName" placeholder="显示名称"/>
                </template>
              </el-table-column>
              <el-table-column label="数据类型" width="180">
                <template #default="{ row }">
                  <el-select v-model="row.type" class="full-width">
                    <el-option
                        v-for="type in dataTypes"
                        :key="type.value"
                        :label="type.label"
                        :value="type.value"
                    />
                  </el-select>
                </template>
              </el-table-column>
              <el-table-column label="长度" width="120">
                <template #default="{ row }">
                  <el-input-number
                      v-model="row.length"
                      :min="1"
                      :disabled="!needLength(row.type)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="小数位数" width="120">
                <template #default="{ row }">
                  <el-input-number
                      v-model="row.precision"
                      :min="0"
                      :disabled="!needPrecision(row.type)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="主键" width="80" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.isPrimary"/>
                </template>
              </el-table-column>
              <el-table-column label="不为空" width="80" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.notNull"/>
                </template>
              </el-table-column>
              <el-table-column label="自增" width="80" align="center">
                <template #default="{ row }">
                  <el-checkbox
                      v-model="row.autoIncrement"
                      :disabled="!canAutoIncrement(row.type)"
                  />
                </template>
              </el-table-column>
              <el-table-column label="默认值" min-width="150">
                <template #default="{ row }">
                  <el-input v-model="row.defaultValue" placeholder="默认值"/>
                </template>
              </el-table-column>
              <el-table-column label="备注" min-width="200">
                <template #default="{ row }">
                  <el-input v-model="row.comment" placeholder="备注"/>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 分页 -->
          <div class="pagination-container">
            <el-pagination
                v-model:current-page="currentPage"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="handleCurrentChange"
            />
          </div>
        </div>
      </template>
    </div>

    <!-- 列表视图 -->
    <div v-else-if="viewMode === 'list'" class="list-view-container">
      <div class="list-container">
        <div class="list-header">
          <h3>实体表列表</h3>
          <el-input
              v-model="listSearchKeyword"
              placeholder="搜索实体表"
              prefix-icon="Search"
              clearable
              style="width: 250px;"
          />
        </div>

        <!-- 实体表列表 -->
        <el-table
            :data="filteredEntityTables || entityTables"
            style="width: 100%"
            @row-click="handleEntityTableClick"
        >
          <el-table-column prop="id" label="ID" width="180"/>
          <el-table-column prop="name" label="表名" width="180"/>
          <el-table-column prop="type" label="类型">
            <template #default="{ row }">
              <el-tag :type="getEntityTypeTag(row.type)">{{ getEntityTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="fields" label="字段数量">
            <template #default="{ row }">
              {{ row.fields ? row.fields.length : 0 }}
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" link @click.stop="editEntityTable(row)">
                <el-icon>
                  <Edit/>
                </el-icon>
                编辑
              </el-button>
              <el-button type="danger" link @click.stop="deleteEntityTable(row)">
                <el-icon>
                  <Delete/>
                </el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <!-- 关系列表 -->
        <div class="list-header" style="margin-top: 20px;">
          <h3>关系列表</h3>
          <el-input
              v-model="relationSearchKeyword"
              placeholder="搜索关系"
              prefix-icon="Search"
              clearable
              style="width: 250px;"
          />
        </div>

        <el-table
            :data="filteredRelations || relations"
            style="width: 100%"
        >
          <el-table-column prop="id" label="ID" width="180"/>
          <el-table-column prop="name" label="关系名称" width="180"/>
          <el-table-column prop="type" label="关系类型">
            <template #default="{ row }">
              <el-tag :type="getRelationTypeTag(row.type)">{{ getRelationTypeName(row.type) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sourceTable" label="源表"/>
          <el-table-column prop="targetTable" label="目标表"/>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <el-button type="primary" link @click="editRelation(row)">
                <el-icon>
                  <Edit/>
                </el-icon>
                编辑
              </el-button>
              <el-button type="danger" link @click="deleteRelation(row)">
                <el-icon>
                  <Delete/>
                </el-icon>
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 添加实体表编辑对话框 -->
    <el-dialog
        v-model="entityFormVisible"
        :title="entityFormTitle"
        width="1200px"
        append-to-body
        destroy-on-close
    >
      <el-form ref="entityFormRef" :model="entityForm" :rules="entityFormRules" label-width="100px">
        <el-form-item label="表名称" prop="name">
          <el-input v-model="entityForm.name" placeholder="请输入实体表名称"/>
        </el-form-item>

        <el-form-item label="表描述">
          <el-input v-model="entityForm.description" type="textarea" placeholder="请输入表描述"/>
        </el-form-item>

        <el-form-item label="表类型">
          <el-select v-model="entityForm.tableType" placeholder="请选择表类型">
            <el-option label="业务实体" value="business"/>
            <el-option label="维度表" value="dimension"/>
            <el-option label="事实表" value="fact"/>
            <el-option label="关联表" value="relation"/>
          </el-select>
        </el-form-item>

        <el-divider content-position="left">字段设置</el-divider>

        <div class="fields-container">
          <div v-for="(field, index) in entityForm.fields" :key="field._id" class="field-item">
            <el-row :gutter="10">
              <el-col :span="5">
                <el-form-item :prop="`fields.${index}.name`" label-width="0">
                  <el-input v-model="field.name" placeholder="字段名称"/>
                </el-form-item>
              </el-col>
              <el-col :span="3">
                <el-form-item :prop="`fields.${index}.type`" label-width="0">
                  <el-select
                      v-model="field.type"
                      size="small"
                      style="width: 100%"
                      @change="(val) => handleFieldTypeChange(field, val)"
                  >
                    <el-option v-for="type in dataTypes" :key="type.value" :label="type.label" :value="type.value"/>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="3" v-if="needLength(field.type)">
                <el-form-item :prop="`fields.${index}.length`" label-width="0">
                  <el-input-number
                      v-model="field.length"
                      size="small"
                      :min="1"
                      :max="1000"
                      @input="handleFieldChange"
                      @change="handleFieldChange"
                      v-if="needLength(field.type)"
                  />
                  <span v-else>-</span>
                </el-form-item>
              </el-col>
              <el-col :span="needLength(field.type) ? 6 : 9">
                <el-form-item label-width="0">
                  <el-checkbox v-model="field.primary" @change="(val) => handlePrimaryKeyChange(field, val)">主键
                  </el-checkbox>
                  <el-checkbox v-model="field.notNull">非空</el-checkbox>
                  <el-checkbox v-model="field.isForeignKey">外键</el-checkbox>
                </el-form-item>
              </el-col>
              <el-col :span="4">
                <el-form-item :prop="`fields.${index}.comment`" label-width="0">
                  <el-input v-model="field.comment" placeholder="字段注释"/>
                </el-form-item>
              </el-col>
              <el-col :span="3">
                <el-form-item label-width="0">
                  <el-tooltip content="删除字段">
                    <el-button
                        type="danger"
                        circle
                        plain
                        @click="removeField(index)"
                        :disabled="field.primary && index === 0"
                    >
                      <el-icon>
                        <Delete/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                </el-form-item>
              </el-col>
            </el-row>
            <el-divider v-if="index < entityForm.fields.length - 1"/>
          </div>

          <div class="add-field-btn">
            <el-button type="primary" plain @click="addField">
              <el-icon>
                <Plus/>
              </el-icon>
              添加字段
            </el-button>
          </div>
        </div>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="entityFormVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEntityTable">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 实体表表单抽屉 -->
    <el-drawer
        v-model="entityFormDrawerVisible"
        :title="entityFormTitle"
        direction="rtl"
        size="800px"
        :with-header="true"
        :modal="true"
        :show-close="true"
        custom-class="entity-form-drawer"
        :close-on-click-modal="false"
        :close-on-press-escape="false"
        @close="closeEntityFormDrawer"
    >
      <el-form ref="entityFormRef" :model="entityForm" :rules="entityFormRules" label-width="100px">
        <el-form-item label="表名称" prop="name">
          <el-input v-model="entityForm.name" placeholder="请输入实体表名称"/>
        </el-form-item>

        <el-form-item label="表描述">
          <el-input v-model="entityForm.description" type="textarea" :rows="2" placeholder="请输入表描述"/>
        </el-form-item>

        <el-form-item label="表类型">
          <el-select v-model="entityForm.tableType" style="width: 100%">
            <el-option label="业务实体" value="business"/>
            <el-option label="维度表" value="dimension"/>
            <el-option label="事实表" value="fact"/>
            <el-option label="关联表" value="association"/>
          </el-select>
        </el-form-item>

        <el-divider>字段列表</el-divider>

        <el-table :data="entityForm.fields" border style="width: 100%">
          <el-table-column label="字段名" min-width="120">
            <template #default="{ row }">
              <el-input
                  v-model="row.name"
                  size="small"
                  @input="handleFieldChange"
                  @change="handleFieldChange"
              />
            </template>
          </el-table-column>
          <el-table-column label="类型" min-width="120">
            <template #default="{ row }">
              <el-select
                  v-model="row.type"
                  size="small"
                  style="width: 100%"
                  @change="(val) => handleFieldTypeChange(row, val)"
              >
                <el-option v-for="type in dataTypes" :key="type.value" :label="type.label" :value="type.value"/>
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="长度" width="150" v-if="true">
            <template #default="{ row }">
              <el-input-number
                  v-model="row.length"
                  size="small"
                  :min="1"
                  :max="1000"
                  @input="handleFieldChange"
                  @change="handleFieldChange"
                  v-if="needLength(row.type)"
              />
              <span v-else>-</span>
            </template>
          </el-table-column>
          <el-table-column label="注释" min-width="120">
            <template #default="{ row }">
              <el-input
                  v-model="row.comment"
                  size="small"
                  @input="handleFieldChange"
                  @change="handleFieldChange"
              />
            </template>
          </el-table-column>
          <el-table-column label="主键" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox
                  v-model="row.primary"
                  @change="(val) => handlePrimaryKeyChange(row, val)"
              />
            </template>
          </el-table-column>
          <el-table-column label="非空" width="60" align="center">
            <template #default="{ row }">
              <el-checkbox
                  v-model="row.notNull"
                  @change="handleFieldChange"
              />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60" align="center">
            <template #default="{ $index }">
              <el-button type="danger" link @click="removeField($index)">
                <el-icon>
                  <Delete/>
                </el-icon>
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <div class="field-actions" style="margin-top: 10px;">
          <el-button type="primary" @click="addField">
            <el-icon>
              <Plus/>
            </el-icon>
            添加字段
          </el-button>
        </div>

        <el-divider>索引配置</el-divider>

        <div v-if="!entityForm.indexes || entityForm.indexes.length === 0" class="empty-tip">
          暂无索引，点击下方按钮添加
        </div>

        <div v-else v-for="(index, idx) in entityForm.indexes" :key="idx" class="index-item">
          <el-form-item label="索引名称">
            <el-input v-model="index.name"/>
          </el-form-item>
          <el-form-item label="索引类型">
            <el-select v-model="index.type" style="width: 100%">
              <el-option label="普通索引" value="normal"/>
              <el-option label="唯一索引" value="unique"/>
            </el-select>
          </el-form-item>
          <el-form-item label="索引字段">
            <el-select v-model="index.fields" multiple style="width: 100%">
              <el-option
                  v-for="field in entityForm.fields"
                  :key="field.name"
                  :label="field.name"
                  :value="field.name"
              />
            </el-select>
          </el-form-item>
          <el-button type="danger" link @click="removeIndex(idx)">
            <el-icon>
              <Delete/>
            </el-icon>
          </el-button>
          <el-divider v-if="idx < entityForm.indexes.length - 1"/>
        </div>

        <div class="index-actions">
          <el-button type="primary" @click="addIndex">
            <el-icon>
              <Plus/>
            </el-icon>
            添加索引
          </el-button>
        </div>

        <div class="drawer-footer">
          <el-button @click="entityFormDrawerVisible = false">取消</el-button>
          <el-button type="primary" @click="saveEntityTable">保存</el-button>
        </div>
      </el-form>
    </el-drawer>

  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox} from 'element-plus'
import {
  CircleCheck, Back, Right, ZoomIn, ZoomOut, FullScreen,
  View, Upload, Close, Connection, DataBoard, Grid,
  DataLine, Filter, Share, Histogram, Plus, Delete,
  SetUp, Search, VideoPlay, VideoPause, Edit, Document,
  CopyDocument, Timer, List, Download, Switch, ArrowRight,
  Collection
} from '@element-plus/icons-vue'
import {Graph, Shape, NodeView, Node} from '@antv/x6'
import '@antv/x6-vue-shape'
import PreviewDialog from './components/PreviewDialog.vue'
import {getModelDesign, saveModelDesign} from '@/api/dataasset/model'


// 定义props
const props = defineProps({
  modelId: {
    type: String,
    default: ''
  },
  domainId: {
    type: String,
    default: ''
  }
})

// 定义emit
const emit = defineEmits(['save-success', 'cancel'])

// 画布相关
const canvasRef = ref(null)
const graph = ref(null)
const isCanvasInitialized = ref(false)
const designMode = ref('entity')
const nodes = ref([])
const selectedNode = ref(null)
const selectedFields = ref([])

// 模型信息
const modelName = ref('')
const modelDescription = ref('')

// 加载状态
const loading = ref(false)

// 画布初始化状态
const isGraphInitialized = ref(false)

// 待加载的模型ID
const pendingModelId = ref(null)

// 获取节点类型名称
const getNodeTypeName = (type) => {
  switch (type) {
    case 'entity-table':
      return '实体表'
    case 'relation':
      return '关联关系'
    default:
      return '未知类型'
  }
}

// 处理节点名称变更
const handleNodeNameChange = () => {
  console.log('节点名称变化:', selectedNode.value.name)

  if (selectedNode.value) {
    const cell = graph.value.getCellById(selectedNode.value.id)
    if (cell) {
      // 更新节点标题
      cell.attr('title/text', selectedNode.value.name || '未命名表')

      // 如果是实体表，更新字段显示
      if (selectedNode.value.type === 'entity-table') {

        // 直接更新nodes数组中的节点
        const nodeIndex = nodes.value.findIndex(n => n.id === selectedNode.value.id);
        if (nodeIndex !== -1) {
          console.log('直接更新nodes数组中的节点名称:', nodes.value[nodeIndex].name, '->', selectedNode.value.name);
          nodes.value[nodeIndex].name = selectedNode.value.name;

          // 创建新数组触发响应式更新
          nodes.value = [...nodes.value];
        }
        updateEntityTableNode(selectedNode.value)
      }
    }
  }
}


// 处理字段变化
const handleFieldChange = () => {
  console.log('字段属性变化')

  if (selectedNode.value && selectedNode.value.type === 'entity-table') {
    // 立即更新画布上的节点显示
    updateEntityTableNode(selectedNode.value)
  }
}

// 添加强制刷新字段信息的方法
const forceRefreshFields = () => {
  console.log('强制刷新字段信息');

  // 如果当前选中的是实体表节点
  if (selectedNode.value && selectedNode.value.type === 'entity-table') {
    // 获取实体表的字段
    const fields = selectedNode.value.config?.fields || [];
    console.log('当前实体表字段:', fields);

    // 刷新源表字段和目标表字段
    const sourceFields = getSourceTableFields();
    console.log('刷新后的源表字段:', sourceFields);

    const targetFields = getTargetTableFields();
    console.log('刷新后的目标表字段:', targetFields);
  }

  // 刷新所有关系节点的字段选择
  refreshRelationshipFields();
};

// 添加刷新关系节点字段选择的方法
const refreshRelationshipFields = () => {
  console.log('刷新关系节点字段选择');

  // 查找所有关系节点
  const relationshipNodes = nodes.value.filter(node => node.type === 'relationship');
  console.log('找到关系节点数量:', relationshipNodes.length);

  // 遍历关系节点
  relationshipNodes.forEach(node => {
    // 获取源表和目标表ID
    const sourceId = node.sourceId;
    const targetId = node.targetId;

    if (sourceId && targetId) {
      // 查找源表和目标表
      const sourceNode = nodes.value.find(n => n.id === sourceId);
      const targetNode = nodes.value.find(n => n.id === targetId);

      if (sourceNode && targetNode) {
        console.log('关系:', sourceNode.name, '->', targetNode.name);

        // 获取源表和目标表的字段
        const sourceFields = sourceNode.config?.fields || [];
        const targetFields = targetNode.config?.fields || [];

        console.log('源表字段数量:', sourceFields.length);
        console.log('目标表字段数量:', targetFields.length);
      }
    }
  });

  // 如果当前选中的是关系节点，更新其配置面板
  if (selectedNode.value && selectedNode.value.type === 'relationship') {
    console.log('当前选中的是关系节点，更新配置面板');

    // 这里可以添加更新关系配置面板的代码
    // 例如，重新获取源表和目标表的字段，更新下拉菜单等
  }
};

// 修改getSourceTableFields和getTargetTableFields方法，确保它们返回最新的字段信息
const getSourceTableFields = () => {
  console.log('获取字段实体表列表, nodes长度:', nodes.value?.length);
  console.log('当前nodes 字段数组:', JSON.stringify(nodes.value));

  const fields = sourceTableFields.value;
  console.log('getSourceTableFields被调用，返回字段数量:', fields.length);
  return fields;
};

const getTargetTableFields = () => {
  console.log('获取目标表字段');

  // 如果当前选中的是关系节点
  if (selectedNode.value && selectedNode.value.type === 'relationship') {
    const targetId = selectedNode.value.targetId;
    if (targetId) {
      // 查找目标表节点
      const targetNode = nodes.value.find(node => node.id === targetId);
      if (targetNode) {
        console.log('找到目标表:', targetNode.name);

        // 获取目标表的字段
        const fields = targetNode.config?.fields || [];
        console.log('目标表字段数量:', fields.length);

        // 返回字段列表
        return fields.map(field => ({
          id: field.name,
          name: `${field.name} (${field.type})`
        }));
      }
    }
  }

  console.log('未找到目标表或目标表字段');
  return [];
};

// 处理删除节点
const handleDeleteNode = () => {
  if (!selectedNode.value || !graph.value) return

  ElMessageBox.confirm(
      `确定要删除节点 "${selectedNode.value.name}" 吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    const cell = graph.value.getCellById(selectedNode.value.id)
    if (cell) {
      graph.value.removeCell(cell)

      // 从节点列表中移除
      const index = nodes.value.findIndex(n => n.id === selectedNode.value.id)
      if (index !== -1) {
        nodes.value.splice(index, 1)
      }

      selectedNode.value = null
      ElMessage.success('节点已删除')
    }
  }).catch(() => {
  })
}

// 添加实体字段
const addEntityField = () => {
  if (!selectedNode.value) return

  if (!selectedNode.value.config.fields) {
    selectedNode.value.config.fields = []
  }

  selectedNode.value.config.fields.push({
    name: `field_${selectedNode.value.config.fields.length + 1}`,
    type: 'varchar',
    length: 255,
    isPrimary: false,
    notNull: false,
    isForeignKey: false,
    comment: '新字段'
  })

  // 更新节点可视化
  updateEntityTableNode(selectedNode.value.id)
}


// 导出模型
const exportModel = () => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 验证模型
  if (!validateModel()) {
    return
  }

  // 构建模型数据
  const modelData = {
    name: modelName.value,
    description: modelDescription.value,
    type: 'entity',
    nodes: nodes.value.map(node => {
      // 获取节点位置
      const cell = graph.value.getCellById(node.id)
      const position = cell ? cell.getPosition() : {x: 0, y: 0}

      return {
        ...node,
        position
      }
    }),
    edges: graph.value.getEdges().map(edge => ({
      source: {
        node: edge.getSourceNode().getData(),
        port: edge.getSourcePortId()
      },
      target: {
        node: edge.getTargetNode().getData(),
        port: edge.getTargetPortId()
      }
    }))
  }

  // 转换为JSON字符串
  const jsonStr = JSON.stringify(modelData, null, 2)

  // 创建下载链接
  const blob = new Blob([jsonStr], {type: 'application/json'})
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${modelName.value || 'entity_model'}.json`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)

  ElMessage.success('模型导出成功')
}

// 验证模型
const validateModel = () => {
  // 检查是否有节点
  if (nodes.value.length === 0) {
    ElMessage.warning('模型中没有任何节点，请先添加节点')
    return false
  }

  // 检查模型名称
  if (!modelName.value) {
    ElMessage.warning('请输入模型名称')
    return false
  }

  // 检查实体表是否有字段
  const invalidTables = nodes.value.filter(node =>
      node.type === 'entity-table' &&
      (!node.config.fields || node.config.fields.length === 0)
  )

  if (invalidTables.length > 0) {
    ElMessage.warning(`以下实体表没有字段: ${invalidTables.map(t => t.name).join(', ')}`)
    return false
  }

  // 检查关系是否配置完整
  const invalidRelations = nodes.value.filter(node =>
      node.type === 'relation' &&
      (!node.config.sourceTable || !node.config.targetTable)
  )

  if (invalidRelations.length > 0) {
    ElMessage.warning(`以下关系配置不完整: ${invalidRelations.map(r => r.name).join(', ')}`)
    return false
  }

  return true
}

// 应用的标准列表
const appliedStandards = ref([])

// 模型表单数据
const modelForm = reactive({
  id: '',
  name: '',
  type: 'detail',
  domainId: '',
  description: '',
  owner: '',
  status: 'draft'
})

// 更新关系可视化
const updateRelationVisual = () => {
  if (!selectedNode.value || !graph.value) return

  const relationCell = graph.value.getCellById(selectedNode.value.id)
  if (!relationCell) return

  const sourceNode = selectedNode.value.config.sourceTable ?
      graph.value.getCellById(selectedNode.value.config.sourceTable) : null

  const targetNode = selectedNode.value.config.targetTable ?
      graph.value.getCellById(selectedNode.value.config.targetTable) : null

  if (!sourceNode || !targetNode) return

  // 更新连接线
  const edges = graph.value.getConnectedEdges(relationCell)
  edges.forEach(edge => {
    graph.value.removeEdge(edge)
  })

  // 创建从源实体到关系的连接
  graph.value.addEdge({
    source: {cell: sourceNode.id},
    target: {cell: relationCell.id},
    attrs: {
      line: {
        stroke: '#5F95FF',
        strokeWidth: 2,
        targetMarker: {
          name: 'classic',
          size: 8
        }
      }
    },
    labels: [
      {
        position: 0.5,
        attrs: {
          text: {
            text: getRelationSourceLabel(),
            fill: '#333',
            fontSize: 12,
            textAnchor: 'middle',
            textVerticalAnchor: 'middle',
            pointerEvents: 'none'
          },
          rect: {
            fill: '#fff',
            stroke: '#5F95FF',
            strokeWidth: 1,
            rx: 3,
            ry: 3,
            refWidth: '100%',
            refHeight: '100%',
            refX: -50,
            refY: -10
          }
        }
      }
    ]
  })

  // 创建从关系到目标实体的连接
  graph.value.addEdge({
    source: {cell: relationCell.id},
    target: {cell: targetNode.id},
    attrs: {
      line: {
        stroke: '#5F95FF',
        strokeWidth: 2,
        targetMarker: {
          name: 'classic',
          size: 8
        }
      }
    },
    labels: [
      {
        position: 0.5,
        attrs: {
          text: {
            text: getRelationTargetLabel(),
            fill: '#333',
            fontSize: 12,
            textAnchor: 'middle',
            textVerticalAnchor: 'middle',
            pointerEvents: 'none'
          },
          rect: {
            fill: '#fff',
            stroke: '#5F95FF',
            strokeWidth: 1,
            rx: 3,
            ry: 3,
            refWidth: '100%',
            refHeight: '100%',
            refX: -50,
            refY: -10
          }
        }
      }
    ]
  })
}

// 获取关系源标签
const getRelationSourceLabel = () => {
  if (!selectedNode.value) return ''

  switch (selectedNode.value.config.relationType) {
    case 'one-to-one':
      return '1'
    case 'one-to-many':
      return '1'
    case 'many-to-many':
      return 'n'
    default:
      return ''
  }
}

// 获取关系目标标签
const getRelationTargetLabel = () => {
  if (!selectedNode.value) return ''

  switch (selectedNode.value.config.relationType) {
    case 'one-to-one':
      return '1'
    case 'one-to-many':
      return 'n'
    case 'many-to-many':
      return 'n'
    default:
      return ''
  }
}

// 处理拖拽开始
const handleDragStart = (event, type) => {
  // 设置拖拽数据
  event.dataTransfer.setData('componentType', type)
  event.dataTransfer.effectAllowed = 'copy'
}

// 处理拖拽放置
const handleDrop = (event) => {
  event.preventDefault()

  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 获取拖拽的组件类型
  const componentType = event.dataTransfer.getData('componentType')
  if (!componentType) return

  // 获取放置位置
  const {clientX, clientY} = event
  const {left, top} = canvasRef.value.getBoundingClientRect()
  const x = clientX - left + canvasRef.value.scrollLeft
  const y = clientY - top + canvasRef.value.scrollTop

  // 根据组件类型创建不同的节点
  if (componentType === 'entity-table') {
    createEntityTableNode(x, y)
  } else if (componentType === 'relation') {
    createRelationNode(x, y)
  }
}

// 注册自定义实体表节点
class EntityTableNode extends Shape.Rect {
  getMarkup() {
    return [
      {
        tagName: 'rect',
        selector: 'body',
      },
      {
        tagName: 'rect',
        selector: 'header',
      },
      {
        tagName: 'text',
        selector: 'title',
      },
      {
        tagName: 'line',
        selector: 'divider',
      },
      {
        tagName: 'text',
        selector: 'field0',
      },
      {
        tagName: 'text',
        selector: 'field1',
      },
      {
        tagName: 'text',
        selector: 'field2',
      },
      {
        tagName: 'text',
        selector: 'field3',
      },
      {
        tagName: 'text',
        selector: 'field4',
      },
      {
        tagName: 'text',
        selector: 'field5',
      },
      {
        tagName: 'text',
        selector: 'field6',
      },
      {
        tagName: 'text',
        selector: 'field7',
      },
      {
        tagName: 'text',
        selector: 'field8',
      },
      {
        tagName: 'text',
        selector: 'field9',
      },
    ]
  }

  defaults() {
    return {
      ...super.defaults,
      attrs: {
        body: {
          fill: '#ffffff',
          stroke: '#5F95FF',
          strokeWidth: 1,
          rx: 6,
          ry: 6,
        },
        header: {
          fill: '#5F95FF',
          height: 30,
          refWidth: '100%',
          rx: 6,
          ry: 6,
        },
        title: {
          text: 'Entity Table',
          fill: '#ffffff',
          fontSize: 14,
          fontWeight: 'bold',
          refX: '50%',
          refY: 15,
          textAnchor: 'middle',
        },
        divider: {
          stroke: '#5F95FF',
          strokeWidth: 1,
          x1: 0,
          y1: 30,
          x2: '100%',
          y2: 30,
        },
      },
    }
  }
}

// 注册自定义关系节点
class RelationNode extends Shape.Ellipse {
  getMarkup() {
    return [
      {
        tagName: 'ellipse',
        selector: 'body',
      },
      {
        tagName: 'text',
        selector: 'label',
      },
      {
        tagName: 'text',
        selector: 'typeLabel',
      },
    ]
  }

  defaults() {
    return {
      ...super.defaults,
      attrs: {
        body: {
          fill: '#f5f5f5',
          stroke: '#5F95FF',
          strokeWidth: 1,
        },
        label: {
          text: 'Relation',
          fill: '#333',
          fontSize: 12,
          fontWeight: 'bold',
          refX: '50%',
          refY: '50%',
          textAnchor: 'middle',
          textVerticalAnchor: 'middle',
        },
        typeLabel: {
          text: '1:N',
          fill: '#5F95FF',
          fontSize: 10,
          fontWeight: 'bold',
          refX: '100%',
          refX2: -5,
          refY: 5,
        },
      },
    }
  }
}

// 注册自定义节点
Node.registry.register('entity-table-node', EntityTableNode, true)
Node.registry.register('relation-node', RelationNode, true)

// 创建实体表节点
const createEntityTableNode = (x, y, data = null) => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 生成唯一ID
  const id = 'entity-' + Date.now()

  // 创建节点数据
  const nodeData = data || {
    id,
    type: 'entity-table',
    name: '新建实体表',
    config: {
      description: '',
      tableType: 'business',
      fields: [
        {
          name: 'id',
          type: 'int',
          length: 11,
          isPrimary: true,
          notNull: true,
          isForeignKey: false,
          comment: '主键ID'
        }
      ],
      indexes: []
    }
  }

  // 如果提供了数据但没有ID，则生成一个
  if (data && !data.id) {
    nodeData.id = id
  }

  // 生成字段文本
  let fieldsText = ''
  if (nodeData.config && nodeData.config.fields) {
    nodeData.config.fields.forEach(field => {
      const primaryKey = field.isPrimary ? '🔑 ' : ''
      const foreignKey = field.isForeignKey ? '🔗 ' : ''
      const notNull = field.notNull ? 'NN ' : ''
      const comment = field.comment ? ` // ${field.comment}` : ''
      fieldsText += `${primaryKey}${foreignKey}${notNull}${field.name}: ${field.type}${comment}\n`
    })
  }

  // 计算初始高度
  const baseHeight = 50 // 基础高度
  const fieldHeight = 20 // 每个字段的高度
  const fieldsCount = nodeData.config && nodeData.config.fields ? nodeData.config.fields.length : 0
  const height = baseHeight + fieldsCount * fieldHeight

  // 创建节点
  const node = graph.value.addNode({
    id: nodeData.id,
    x,
    y,
    width: 200,
    height,
    shape: 'rect',
    markup: [
      {
        tagName: 'rect',
        selector: 'body',
      },
      {
        tagName: 'rect',
        selector: 'header',
      },
      {
        tagName: 'text',
        selector: 'title',
      },
      {
        tagName: 'text',
        selector: 'fields',
      },
    ],
    attrs: {
      body: {
        fill: '#ffffff',
        stroke: '#5F95FF',
        strokeWidth: 1,
        rx: 6,
        ry: 6,
      },
      header: {
        fill: '#5F95FF',
        height: 30,
        refWidth: '100%',
        rx: 6,
        ry: 6,
      },
      title: {
        text: nodeData.name,
        fill: '#ffffff',
        fontSize: 14,
        fontWeight: 'bold',
        refX: '50%',
        refY: 15,
        textAnchor: 'middle',
      },
      fields: {
        text: fieldsText,
        fill: '#333',
        fontSize: 12,
        refX: 10,
        refY: 45,
        textAnchor: 'start',
        textVerticalAnchor: 'top',
        lineHeight: 20,
      },
    },
    ports: {
      groups: {
        input: {
          position: 'left',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        },
        output: {
          position: 'right',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        }
      },
      items: [
        {id: 'port-in', group: 'input'},
        {id: 'port-out', group: 'output'}
      ]
    },
    data: nodeData
  })

  // 添加到节点列表
  nodes.value.push(nodeData)

  return node
}

// 修改 updateEntityTableNode 函数，添加更多错误处理和调试信息
const updateEntityTableNode = (nodeId) => {
  try {
    if (!graph.value) {
      console.error('图形实例不存在，无法更新节点')
      return
    }

    const node = graph.value.getCellById(nodeId)
    if (!node) {
      console.error(`找不到节点: ${nodeId}`)
      return
    }

    const nodeData = node.getData()
    console.log('更新节点显示:', nodeId, nodeData)

    if (!nodeData) {
      console.error(`节点数据为空: ${nodeId}`)
      return
    }

    // 检查字段数据 - 添加多种可能的路径
    const fields = nodeData.fields || nodeData.config?.fields || []
    console.log('字段数据:', fields)

    if (!fields || fields.length === 0) {
      console.warn(`节点没有字段数据: ${nodeId}`)
      // 设置一个默认的字段文本
      node.attr('fields/text', '无字段数据')
      return
    }

    // 生成字段文本
    let fieldsText = ''
    if (nodeData.config && nodeData.config.fields) {
      nodeData.config.fields.forEach(field => {
        const primaryKey = field.isPrimary ? '🔑 ' : ''
        const foreignKey = field.isForeignKey ? '🔗 ' : ''
        const notNull = field.notNull ? 'NN ' : ''
        const comment = field.comment ? ` // ${field.comment}` : ''
        fieldsText += `${primaryKey}${foreignKey}${notNull}${field.name}: ${field.type}${comment}\n`
      })

      // 直接更新nodes数组中的节点
      const nodeIndex = nodes.value.findIndex(n => n.id === selectedNode.value.id);
      if (nodeIndex !== -1) {
        console.log('直接更新nodes数组中的节点名称:', nodes.value[nodeIndex].name, '->', selectedNode.value.name);
        nodes.value[nodeIndex].config.fields = nodeData.config.fields;

        // 创建新数组触发响应式更新
        nodes.value = [...nodes.value];
      }

      // 创建新数组触发响应式更新
      nodes.value = [...nodes.value];
    }

    console.log('生成的字段文本:', fieldsText)

    // 计算节点高度，根据字段数量动态调整
    const baseHeight = 50 // 基础高度
    const fieldHeight = 20 // 每个字段的高度
    const height = baseHeight + fields.length * fieldHeight

    // 更新节点高度
    node.resize(200, height)
    console.log('调整节点高度:', height)

    // 更新节点标题和字段
    node.attr({
      title: {
        text: nodeData.name || '未命名实体'
      },
      fields: {
        text: fieldsText
      }
    })

    console.log('更新节点属性完成')
  } catch (error) {
    console.error('更新实体表节点时出错:', error)
    ElMessage.error('更新实体表节点失败: ' + (error.message || '未知错误'))
  }
}


// 获取关系类型文本
const getRelationTypeText = (relationType) => {
  switch (relationType) {
    case 'one-to-one':
      return '1:1'
    case 'one-to-many':
      return '1:N'
    case 'many-to-many':
      return 'N:N'
    default:
      return '?:?'
  }
}

// 处理主键变更
const handlePrimaryKeyChange = (field, isPrimary) => {
  console.log('主键变更:', field.name, isPrimary)
  // 如果设置为主键，自动设置为非空
  if (isPrimary) {
    field.notNull = true
  }
  handleFieldChange()
}

const relations = ref([])


// 添加监听器，当nodes变化时，记录日志
watch(nodes, (newNodes) => {
  console.log('节点列表变化，当前节点数:', newNodes.length);
}, {deep: true});

// 添加监听器，当selectedNode.config.sourceTable变化时，记录日志
watch(() => selectedNode.value?.config?.sourceTable, (newSourceTable) => {
  console.log('源表变化:', newSourceTable);
}, {immediate: true});

// 添加监听器，当selectedNode.config.targetTable变化时，记录日志
watch(() => selectedNode.value?.config?.targetTable, (newTargetTable) => {
  console.log('目标表变化:', newTargetTable);
}, {immediate: true});

// 添加索引
const addIndex = () => {
  if (!selectedNode.value) return

  if (!selectedNode.value.config.indexes) {
    selectedNode.value.config.indexes = []
  }

  selectedNode.value.config.indexes.push({
    name: `idx_${selectedNode.value.name.toLowerCase()}_${selectedNode.value.config.indexes.length + 1}`,
    type: 'normal',
    fields: []
  })
}

// 删除索引
const removeIndex = (index) => {
  entityForm.indexes.splice(index, 1)
}

// 判断字段类型是否需要长度
const needLength = (type) => {
  return ['string', 'varchar', 'char', 'integer', 'int', 'decimal'].includes(type)
}

// 判断字段类型是否需要精度
const needPrecision = (type) => {
  return ['decimal', 'float', 'double'].includes(type)
}

// 判断字段类型是否可以自增
const canAutoIncrement = (type) => {
  return ['int', 'bigint', 'smallint', 'tinyint'].includes(type)
}

// 处理源表变更
const handleSourceTableChange = () => {
  try {
    console.log('源表变更');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      selectedNode.value.config = {};
    }

    // 清空源字段选择
    selectedNode.value.config.sourceField = [];

    console.log('已清空源字段选择，新源表:', selectedNode.value.config.sourceTable);

    // 强制更新
    nextTick(() => {
      // 这里可以添加额外的逻辑，如果需要
    });
  } catch (error) {
    console.error('处理源表变更时出错:', error);
  }
}

// 处理目标表变更
const handleTargetTableChange = () => {
  try {
    console.log('目标表变更');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      selectedNode.value.config = {};
    }

    // 清空目标字段选择
    selectedNode.value.config.targetField = [];

    console.log('已清空目标字段选择，新目标表:', selectedNode.value.config.targetTable);

    // 强制更新
    nextTick(() => {
      // 这里可以添加额外的逻辑，如果需要
    });
  } catch (error) {
    console.error('处理目标表变更时出错:', error);
  }
}


// 在目标表添加外键
const addForeignKeyToTargetTable = () => {
  if (!selectedNode.value) return

  const sourceTable = nodes.value.find(node => node.id === selectedNode.value.config.sourceTable)
  const targetTable = nodes.value.find(node => node.id === selectedNode.value.config.targetTable)

  if (!sourceTable || !targetTable) return

  // 获取源表主键字段
  const sourcePrimaryKey = sourceTable.config.fields.find(field => field.isPrimary)
  if (!sourcePrimaryKey) return

  // 检查目标表是否已有该外键
  const existingForeignKey = targetTable.config.fields.find(field =>
      field.isForeignKey &&
      field.foreignKey &&
      field.foreignKey.targetTable === sourceTable.id
  )

  if (existingForeignKey) {
    // 更新现有外键
    existingForeignKey.foreignKey.relationType = selectedNode.value.config.relationType
    existingForeignKey.foreignKey.onDelete = selectedNode.value.config.onDelete
  } else {
    // 创建新外键字段
    const foreignKeyName = `${sourceTable.name.toLowerCase()}_id`

    targetTable.config.fields.push({
      name: foreignKeyName,
      type: sourcePrimaryKey.type,
      length: sourcePrimaryKey.length,
      isPrimary: false,
      notNull: selectedNode.value.config.relationType === 'one-to-one',
      isForeignKey: true,
      foreignKey: {
        targetTable: sourceTable.id,
        targetField: sourcePrimaryKey.name,
        relationType: selectedNode.value.config.relationType,
        onDelete: selectedNode.value.config.onDelete
      }
    })
  }

  // 更新目标表节点可视化
  updateEntityTableNode(targetTable.id)
}


// 初始化画布
onMounted(async () => {
  // 注册自定义节点类型
  if (!Node.registry.exist('entity-table-node')) {
    Node.registry.register('entity-table-node', EntityTableNode, true)
  }
  if (!Node.registry.exist('relation-node')) {
    Node.registry.register('relation-node', RelationNode, true)
  }

  const initGraph = async () => {
    try {
      // 等待DOM更新
      await nextTick()

      // 增加延迟确保容器完全挂载
      await new Promise(resolve => setTimeout(resolve, 300))

      // 检查容器是否存在
      const container = canvasRef.value
      if (!container) {
        throw new Error('画布容器元素未找到')
      }

      // 检查容器是否在文档中
      if (!document.body.contains(container)) {
        throw new Error('画布容器元素未正确挂载到文档中')
      }

      // 检查容器尺寸
      const {width, height} = container.getBoundingClientRect()
      if (width === 0 || height === 0) {
        throw new Error('画布容器尺寸异常，请检查容器样式')
      }

      // 如果已经初始化过，先销毁旧实例
      if (graph.value) {
        graph.value.dispose()
        graph.value = null
      }

      // 初始化图形
      graph.value = new Graph({
        container,
        width: width,
        height: height,
        grid: {
          type: 'mesh',
          size: 10,
          visible: true,
          color: '#ddd'
        },
        mousewheel: {
          enabled: true,
          modifiers: ['ctrl', 'meta'],
          minScale: 0.5,
          maxScale: 2
        },
        connecting: {
          snap: true,
          allowBlank: false,
          allowLoop: false,
          highlight: true,
          connector: 'rounded',
          connectionPoint: 'boundary',
          router: {
            name: 'manhattan'
          },
          validateConnection({sourceView, targetView, sourceMagnet, targetMagnet}) {
            if (!sourceView || !targetView || !sourceMagnet || !targetMagnet) {
              return false
            }

            // 禁止自连接
            if (sourceView === targetView) {
              return false
            }

            // 只允许输出端口连接到输入端口
            const sourcePort = sourceMagnet.getAttribute('port-group')
            const targetPort = targetMagnet.getAttribute('port-group')
            if (sourcePort === 'output' && targetPort === 'input') {
              return true
            }

            return false
          },
          createEdge() {
            return new Shape.Edge({
              attrs: {
                line: {
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  targetMarker: {
                    name: 'classic',
                    size: 8
                  }
                }
              }
            })
          }
        },
        selecting: {
          enabled: true,
          multiple: true,
          rubberband: true,
          movable: true,
          showNodeSelectionBox: true
        },
        history: {
          enabled: true,
          beforeAddCommand(event, args) {
            return true // 返回false可以阻止命令被添加到历史记录
          }
        },
        clipboard: {
          enabled: true
        },
        keyboard: {
          enabled: true
        }
      })

      // 监听画布事件
      graph.value.on('blank:click', () => {
        selectedNode.value = null
      })

      // 监听节点选择事件
      graph.value.on('node:click', ({node}) => {
        selectedNode.value = node.getData()
      })

      // 监听边连接事件
      graph.value.on('edge:connected', ({edge}) => {
        const sourceNode = edge.getSourceNode()
        const targetNode = edge.getTargetNode()

        if (sourceNode && targetNode) {
          const sourceData = sourceNode.getData()
          const targetData = targetNode.getData()

          // 如果连接的是两个实体表，自动创建关系节点
          if (sourceData.type === 'entity-table' && targetData.type === 'entity-table') {
            // 获取边的中点位置
            const edgeView = graph.value.findViewByCell(edge)
            const pathElement = edgeView.findOne('path')
            const pathLength = pathElement.getTotalLength()
            const middlePoint = pathElement.getPointAtLength(pathLength / 2)

            // 创建关系节点
            createRelationNode(middlePoint.x, middlePoint.y)

            // 移除原始边
            graph.value.removeEdge(edge)
          }
        }
      })

      isCanvasInitialized.value = true
      console.log('画布初始化成功')

    } catch (error) {
      console.error('画布初始化失败:', error)
      ElMessage.error(error.message || '画布初始化失败')
      isCanvasInitialized.value = false
    }
  }

  // 初始化画布
  await initGraph()

  // 绑定视图切换按钮的点击事件处理函数
  const canvasButton = document.querySelector('.canvas-view-btn')
  const listButton = document.querySelector('.list-view-btn')

  if (canvasButton) {
    canvasButton.addEventListener('click', () => {
      switchViewMode('canvas')
    })
  }

  if (listButton) {
    listButton.addEventListener('click', () => {
      switchViewMode('list')
    })
  }
})

// 修复 handleNodeClick 函数，移除对 drawerVisible 的引用
const handleNodeClick = (args) => {
  try {
    const {node} = args
    if (!node) {
      console.error('节点为空')
      return
    }

    console.log('点击节点:', node)

    // 获取节点数据
    const nodeData = node.getData()
    console.log('节点数据:', nodeData)

    if (!nodeData) {
      console.error('节点数据为空')
      return
    }

    // 添加空值检查，防止访问 undefined 的属性
    const fields = nodeData.fields || nodeData.config?.fields || []
    console.log('字段数据:', fields)

    // 处理字段数据
    if (nodeData.type === 'entity-table') {
      // 打开实体表详情面板
      console.log('打开实体表详情:', nodeData.name)

      // 如果存在 openEntityDetail 函数，则调用它
      if (typeof openEntityDetail === 'function') {
        openEntityDetail(nodeData)
      } else {
        console.log('实体表详情:', {
          id: nodeData.id,
          name: nodeData.name,
          fields: fields
        })
      }
    } else if (nodeData.type === 'relation') {
      // 处理关系节点
      console.log('打开关系详情:', nodeData.name)

      // 如果存在 openRelationDetail 函数，则调用它
      if (typeof openRelationDetail === 'function') {
        openRelationDetail(nodeData)
      } else {
        console.log('关系详情:', nodeData)
      }
    }
  } catch (error) {
    console.error('处理节点点击事件时出错:', error)
    ElMessage.error('处理节点点击事件失败: ' + (error.message || '未知错误'))
  }
}

// 数据类型选项
const dataTypes = ref([
  {label: '字符串', value: 'string'},
  {label: '文本', value: 'text'},
  {label: '整数', value: 'integer'},
  {label: '小数', value: 'decimal'},
  {label: '布尔值', value: 'boolean'},
  {label: '日期', value: 'date'},
  {label: '日期时间', value: 'datetime'},
  {label: '时间戳', value: 'timestamp'}
])

// 撤销/重做状态
const canUndo = computed(() => {
  if (!graph.value || !graph.value.history) return false
  return graph.value.history.canUndo()
})

const canRedo = computed(() => {
  if (!graph.value || !graph.value.history) return false
  return graph.value.history.canRedo()
})

// 处理撤销
const handleUndo = () => {
  if (graph.value && graph.value.history && graph.value.history.canUndo()) {
    graph.value.history.undo()
  }
}

// 处理重做
const handleRedo = () => {
  if (graph.value && graph.value.history && graph.value.history.canRedo()) {
    graph.value.history.redo()
  }
}

// 处理缩放
const handleZoomIn = () => {
  if (graph.value) {
    const zoom = graph.value.zoom()
    if (zoom < 2) {
      graph.value.zoom(zoom + 0.1)
    }
  }
}

const handleZoomOut = () => {
  if (graph.value) {
    const zoom = graph.value.zoom()
    if (zoom > 0.5) {
      graph.value.zoom(zoom - 0.5)
    }
  }
}

const handleFitContent = () => {
  if (!this.graph) return

  try {
    // 尝试使用不同的方法来适应内容
    if (typeof this.graph.zoomToFit === 'function') {
      this.graph.zoomToFit()
    } else if (typeof this.graph.centerContent === 'function') {
      this.graph.centerContent()
    } else if (typeof this.graph.center === 'function') {
      this.graph.center()
    } else if (typeof this.graph.zoom === 'function') {
      // 如果没有适应内容的方法，可以尝试重置缩放
      this.graph.zoom(1)
    } else {
      console.warn('无法找到合适的方法来适应内容')
    }
  } catch (error) {
    console.error('适应内容时出错:', error)
  }
}

// 处理保存
const handleSave = async () => {
  try {
    if (!modelForm.name || modelForm.name.trim() === '') {
      ElMessage.warning('请输入模型名称')
      return
    }



    // 准备保存的数据
    const saveData = {
      model: {
        id: props.modelId,
        name: modelForm.name,
        type: modelForm.type || 'detail',
        domainId: modelForm.domainId,
        description: modelForm.description,
        owner: modelForm.owner,
        status: 'draft' // 默认为草稿状态
      },
      design: {
        canvas: JSON.stringify(getCanvasData()),
        fields: JSON.stringify(getAllFields()),
        indexes: JSON.stringify(getAllIndexes()),
        relations: JSON.stringify(getAllRelations()),
        config: JSON.stringify(getDesignConfig())
      },
      // 添加关联组件信息
      standards: getModelStandards()
    }

    // 调用API保存模型设计
    const res = await saveModelDesign(saveData)
    if (res && res.code === 0) {
      ElMessage.success('保存成功')
      emit('save-success', res.data)
    } else {
      ElMessage.error(res?.message || '保存失败')
    }
  } catch (error) {
    console.error('保存模型出错:', error)
    ElMessage.error('保存模型出错')
  } finally {
    loading.value = false
  }
}


// 获取画布数据
const getCanvasData = () => {
  if (!graph.value) return {}

  // 获取画布中的所有节点和边
  const cells = graph.value.getCells()
  return {
    nodes: cells.filter(cell => cell.isNode()).map(node => ({
      id: node.id,
      type: node.getData()?.type,
      position: node.getPosition(),
      size: node.getSize(),
      attrs: node.getAttrs(),
      data: node.getData()
    })),
    edges: cells.filter(cell => cell.isEdge()).map(edge => ({
      id: edge.id,
      source: edge.getSource(),
      target: edge.getTarget(),
      attrs: edge.getAttrs(),
      data: edge.getData()
    }))
  }
}

// 获取所有字段
const getAllFields = () => {
  if (!graph.value) return []

  const nodes = graph.value.getNodes()
  const allFields = []

  nodes.forEach(node => {
    const nodeData = node.getData()
    if (nodeData && nodeData.type === 'entity-table' && nodeData.config && nodeData.config.fields) {
      nodeData.config.fields.forEach(field => {
        allFields.push({
          nodeId: node.id,
          tableName: nodeData.config.tableName,
          ...field
        })
      })
    }
  })

  return allFields
}

// 获取所有索引
const getAllIndexes = () => {
  if (!graph.value) return []

  const nodes = graph.value.getNodes()
  const allIndexes = []

  nodes.forEach(node => {
    const nodeData = node.getData()
    if (nodeData && nodeData.type === 'entity-table' && nodeData.config && nodeData.config.indexes) {
      nodeData.config.indexes.forEach(index => {
        allIndexes.push({
          nodeId: node.id,
          tableName: nodeData.config.tableName,
          ...index
        })
      })
    }
  })

  return allIndexes
}

// 获取所有关系
const getAllRelations = () => {
  console.log('开始获取所有关系...');

  if (!graph.value) {
    console.log('图形实例不存在，返回空数组');
    return [];
  }

  const edges = graph.value.getEdges();
  console.log(`找到 ${edges.length} 条边`);

  // 详细记录每条边的原始信息
  edges.forEach((edge, index) => {
    console.log(`边 ${index + 1} (${edge.id}) 原始信息:`, {
      sourceId: edge.getSourceCellId(),
      targetId: edge.getTargetCellId(),
      sourcePortId: edge.getSourcePortId(),
      targetPortId: edge.getTargetPortId(),
      data: edge.getData()
    });
  });

  const relation = [];

  const relations = edges.map(edge => {
    const sourceNode = graph.value.getCellById(edge.getSourceCellId());
    const targetNode = graph.value.getCellById(edge.getTargetCellId());
    if (sourceNode?.getData()?.type === 'relation'){
      relation.push({
        id: sourceNode?.id,
        type: sourceNode?.getData()?.type,
        tableName: sourceNode?.getData()?.config?.name,
        data: sourceNode?.getData()
      });
    }
    if (targetNode?.getData()?.type === 'relation'){
      relation.push({
        id: targetNode?.id,
        type: targetNode?.getData()?.type,
        tableName: targetNode?.getData()?.config?.name,
        data: targetNode?.getData()
      });
    }



    // 记录端口信息
    const sourcePortId = edge.getSourcePortId();
    const targetPortId = edge.getTargetPortId();
    console.log(`边 ${edge.id} 的端口信息:`, {
      sourcePortId,
      targetPortId,
      sourceFieldParsed: sourcePortId?.split('-').pop(),
      targetFieldParsed: targetPortId?.split('-').pop()
    });

    // 记录边的数据
    const edgeData = edge.getData() || {};
    console.log(`边 ${edge.id} 的数据:`, edgeData);

    // 获取标签信息
    const labels = edge.getLabels() || [];
    const labelTexts = labels.map(label => {
      if (label && label.attrs && label.attrs.text) {
        return label.attrs.text.text;
      }
      return null;
    }).filter(text => text !== null);

    console.log(`边 ${edge.id} 的标签:`, labelTexts);


    console.log(`处理后的关系 ${edge.id}:`, relation);
    return relation;
  });

  console.log(`总共处理了 ${relations.length} 个关系`);
  console.log('所有关系:', relations);
  return relations;
};


// 获取设计配置
const getDesignConfig = () => {
  return {
    designMode: designMode.value,
    viewMode: viewMode.value,
    // 其他配置信息
  }
}

// 获取应用的标准
const getAppliedStandards = () => {
  // 从标准应用对话框中获取应用的标准
  return appliedStandards.value.map(standard => ({
    standardId: standard.id,
    standardType: standard.type,
    fieldId: standard.fieldId
  }))
}

// 加载画布数据
const loadCanvasData = (canvasData) => {
  if (!graph.value || !canvasData) return

  // 清空画布
  graph.value.clearCells()

  // 加载节点
  if (canvasData.nodes && canvasData.nodes.length > 0) {
    canvasData.nodes.forEach(nodeData => {
      const node = createNodeFromData(nodeData)
      if (node) {
        graph.value.addNode(node)
      }
    })
  }

  // 加载边
  if (canvasData.edges && canvasData.edges.length > 0) {
    canvasData.edges.forEach(edgeData => {
      const edge = createEdgeFromData(edgeData)
      if (edge) {
        graph.value.addEdge(edge)
      }
    })
  }

  // 适应内容
  setTimeout(() => {
    handleFitContent()
  }, 100)
}

// 从数据创建节点
const createNodeFromData = (nodeData) => {
  // 根据节点类型创建不同的节点
  if (nodeData.type === 'entity-table') {
    return createEntityTableNodeFromData(nodeData)
  }
  // 可以添加其他类型的节点创建逻辑

  return null
}

// 从数据创建实体表节点
const createEntityTableNodeFromData = (nodeData) => {
  // 实现从保存的数据创建实体表节点的逻辑
  // 这里需要根据你的具体实现来完成
  // ...
}

// 从数据创建边
const createEdgeFromData = (edgeData) => {
  // 实现从保存的数据创建边的逻辑
  // 这里需要根据你的具体实现来完成
  // ...
}

// 监听模型ID变化，加载模型设计
watch(() => props.modelId, (newVal) => {
  console.log('modelId变化:', newVal)

  // 检查modelId是否有效
  if (newVal && newVal.trim() !== '') {
    console.log('检测到有效的modelId，开始加载模型设计:', newVal)

    // 确保画布已初始化
    nextTick(() => {
      // 直接调用API获取模型设计
      loading.value = true
      getModelDesign(newVal)
          .then(res => {
            console.log('获取模型设计API响应:', res)
            if (res && res.code === 0 && res.data) {
              const {model, design, standards} = res.data

              // 设置模型基本信息
              if (model) {
                console.log('设置模型基本信息:', model)
                Object.assign(modelForm, {
                  id: model.id || '',
                  name: model.name || '',
                  type: model.type || 'detail',
                  domainId: model.domainId || '',
                  description: model.description || '',
                  owner: model.owner || '',
                  status: model.status || 'draft'
                })
              }


              // 设置设计信息
              if (design) {
                console.log('开始处理设计信息:', design)

                // 解析画布数据
                if (design.canvas) {
                  console.log('开始解析画布数据')
                  try {
                    // 解析JSON字符串为对象
                    const canvasData = JSON.parse(design.canvas)
                    console.log('解析后的画布数据:', canvasData)

                    // 解析字段数据
                    let fieldsData = []
                    if (design.fields) {
                      try {
                        fieldsData = JSON.parse(design.fields)
                        console.log('解析后的字段数据:', fieldsData)
                      } catch (e) {
                        console.error('解析字段数据出错:', e)
                      }
                    }

                    // 确保画布已初始化
                    if (graph.value && isCanvasInitialized.value) {
                      console.log('画布已初始化，开始加载节点和边')

                      // 清空画布
                      console.log('清空画布')
                      graph.value.clearCells()

                      // 清空节点列表
                      nodes.value = []

                      // 加载节点
                      if (canvasData.nodes && canvasData.nodes.length > 0) {
                        console.log(`开始加载${canvasData.nodes.length}个节点`)

                        canvasData.nodes.forEach((nodeData, index) => {
                          console.log(`处理第${index + 1}个节点:`, nodeData)

                          try {
                            // 根据节点类型创建不同的节点
                            if (nodeData.type === 'entity-table') {
                              console.log('创建实体表节点')

                              // 检查必要属性
                              if (!nodeData.position) {
                                console.warn('节点缺少position属性，使用默认值')
                                nodeData.position = {x: 100, y: 100}
                              }

                              // 查找该节点的字段数据
                              const nodeFields = fieldsData.filter(field => field.nodeId === nodeData.id)
                              console.log('节点字段:', nodeFields)

                              // 准备节点数据
                              const entityData = {
                                id: nodeData.id,
                                type: 'entity-table',
                                name: nodeData.data?.name || '未命名实体',
                                config: {
                                  description: nodeData.data?.description || '',
                                  tableType: nodeData.data?.tableType || 'business',
                                  fields: [],
                                  indexes: []
                                }
                              }

                              // 添加字段数据
                              if (nodeFields && nodeFields.length > 0) {
                                entityData.config.fields = nodeFields.map(field => ({
                                  name: field.name,
                                  type: field.type,
                                  length: field.length || 0,
                                  isPrimary: field.isPrimary || false,
                                  notNull: field.notNull || false,
                                  isForeignKey: field.isForeignKey || false,
                                  comment: field.comment || ''
                                }))
                              } else {
                                // 添加默认字段
                                entityData.config.fields = [{
                                  name: 'id',
                                  type: 'int',
                                  length: 11,
                                  isPrimary: true,
                                  notNull: true,
                                  isForeignKey: false,
                                  comment: '主键ID'
                                }]
                              }

                              // 直接使用createEntityTableNode函数创建节点
                              const node = createEntityTableNode(
                                  nodeData.position.x,
                                  nodeData.position.y,
                                  entityData
                              )

                              // 确保节点被添加到画布
                              if (node) {
                                // 将节点添加到画布
                                graph.value.addNode(node)

                                // 将节点添加到节点列表
                                nodes.value.push({
                                  id: node.id,
                                  name: entityData.name,
                                  type: entityData.type,
                                  data: entityData
                                })

                                console.log('节点创建并添加到画布:', node.id)
                              } else {
                                console.error('节点创建失败:', entityData)
                              }
                            } else {
                              console.warn('未知的节点类型:', nodeData.type)
                            }
                          } catch (e) {
                            console.error('添加节点出错:', e, nodeData)
                          }
                        })
                      } else {
                        console.log('没有节点数据需要加载')
                      }

                      // 加载边
                      if (canvasData.edges && canvasData.edges.length > 0) {
                        console.log(`开始加载${canvasData.edges.length}个边`)

                        canvasData.edges.forEach((edgeData, index) => {
                          console.log(`处理第${index + 1}个边:`, edgeData)

                          try {
                            // 确保源节点和目标节点存在
                            const sourceNode = graph.value.getCellById(edgeData.source)
                            const targetNode = graph.value.getCellById(edgeData.target)

                            if (!sourceNode || !targetNode) {
                              console.warn(`边的源节点或目标节点不存在: source=${edgeData.source}, target=${edgeData.target}`)
                              return
                            }

                            const edge = graph.value.addEdge({
                              id: edgeData.id,
                              source: {
                                cell: edgeData.source,
                                port: 'output' // 确保使用正确的端口
                              },
                              target: {
                                cell: edgeData.target,
                                port: 'input' // 确保使用正确的端口
                              },
                              router: {
                                name: 'manhattan'
                              },
                              connector: {
                                name: 'rounded'
                              },
                              attrs: {
                                line: {
                                  stroke: '#5F95FF',
                                  strokeWidth: 2,
                                  targetMarker: {
                                    name: 'classic',
                                    size: 8
                                  }
                                },
                                label: {
                                  text: edgeData.data?.relationName || '',
                                  fill: '#333',
                                  fontSize: 12,
                                  refX: 0.5,
                                  refY: -10
                                }
                              },
                              data: edgeData.data || {}
                            })

                            console.log('边创建成功:', edge?.id)
                          } catch (e) {
                            console.error('添加边出错:', e, edgeData)
                          }
                        })
                      } else {
                        console.log('没有边数据需要加载')
                      }

                      // 适应画布内容
                      console.log('调整画布视图')
                      setTimeout(() => {
                        try {
                          graph.value.centerContent()
                          console.log('画布内容居中完成')
                        } catch (e) {
                          console.error('画布居中出错:', e)
                        }
                      }, 1000) // 增加延时，确保所有元素加载完成
                    } else {
                      console.error('画布未初始化，无法加载数据')
                      // 保存modelId，等画布初始化后再加载
                      pendingModelId.value = id
                    }
                  } catch (e) {
                    console.error('解析画布数据出错:', e)
                  }
                } else {
                  console.warn('设计信息中没有canvas数据')
                }

                // 记录原始关系数据，用于调试
                if (design.relations) {
                  console.log('原始关系数据:', design.relations)
                  try {
                    // 尝试解析关系数据
                    const parsedRelations = JSON.parse(design.relations)
                    console.log('解析后的关系数据:', parsedRelations)

                    // 存储解析后的关系数据，供后续使用
                    if (Array.isArray(parsedRelations) && parsedRelations.length > 0) {
                      // 处理嵌套数组结构
                      const flattenedRelations = Array.isArray(parsedRelations[0])
                          ? parsedRelations.flat()
                          : parsedRelations

                      console.log('扁平化后的关系数据:', flattenedRelations)

                      // 提取关系节点
                      const relationNodes = flattenedRelations.filter(item => item.type === 'relation')
                      console.log('提取的关系节点:', relationNodes)

                      // 存储关系节点数据
                      if (relationNodes.length > 0) {
                        relationNodes.forEach(node => {
                          if (node.data && node.data.config) {
                            // 将关系配置添加到nodes数组中，确保在getModelStandards中能够找到
                            const existingNode = nodes.value.find(n => n.id === node.id)
                            if (!existingNode) {
                              nodes.value.push({
                                id: node.id,
                                type: 'relationship',
                                config: node.data.config
                              })
                              console.log(`添加关系节点到nodes数组: ${node.id}`)
                            }
                          }
                        })
                      }
                    }
                  } catch (error) {
                    console.error('解析关系数据失败:', error)
                  }
                }

                // ... 保持原有的其他数据处理代码不变 ...
              } else {
                console.warn('返回数据中没有design信息')
              }

              // 设置标准关联
              if (standards) {
                console.log('设置标准关联:', standards)
                if (standards.length > 0) {
                  appliedStandards.value = standards.map(s => ({
                    id: s.standardId,
                    type: s.standardType,
                    fieldId: s.fieldId
                  }))
                } else {
                  appliedStandards.value = []
                }
              } else {
                console.warn('返回数据中没有standards信息')
              }

              console.log('模型设计数据加载完成1')
            } else {
              console.error('获取模型设计失败:', res?.message)
              ElMessage.error(res?.message || '获取模型设计失败')
            }
          })
          .catch(err => {
            console.error('获取模型设计出错:', err)
            ElMessage.error('获取模型设计出错: ' + (err.message || err))
          })
          .finally(() => {
            loading.value = false
          })
    })
  } else {
    console.log('modelId为空，重置表单和画布')
    // 重置表单
    Object.assign(modelForm, {
      id: '',
      name: '新建模型',
      type: 'detail',
      domainId: '',
      description: '',
      owner: '',
      status: 'draft'
    })

    // 重置标准
    appliedStandards.value = []

    // 重置画布
    if (graph.value) {
      graph.value.clearCells()
    }
  }
}, {immediate: true})

// 处理预览
const previewVisible = ref(false)
const previewData = ref(null)

const handlePreview = () => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 验证模型
  if (!validateModel()) {
    return
  }

  // 构建预览数据
  previewData.value = {
    name: modelName.value,
    description: modelDescription.value,
    type: 'entity',
    nodes: nodes.value.map(node => {
      // 获取节点位置
      const cell = graph.value.getCellById(node.id)
      const position = cell ? cell.getPosition() : {x: 0, y: 0}

      return {
        ...node,
        position
      }
    })
  }

  previewVisible.value = true
}

// 应用数据标准相关
const standardsDialogVisible = ref(false)
const standardsActiveTab = ref('select')
const standardsForm = reactive({
  targetType: 'model',
  targetNodeName: '',
  domainIds: [],
  standardType: '',
  applyScope: 'all',
  description: ''
})
const standardsLoading = ref(false)
const availableStandards = ref([])
const selectedStandards = ref([])
const standardsApplied = ref(false)
const applyProgress = ref(0)


// 加载可用标准
const loadAvailableStandards = () => {
  standardsLoading.value = true

  // 模拟加载数据
  setTimeout(() => {
    availableStandards.value = [
      {
        id: '1',
        name: '实体表命名规范',
        type: 'naming',
        description: '规定实体表命名必须使用下划线命名法，并以业务领域前缀开头'
      },
      {
        id: '2',
        name: '字段命名规范',
        type: 'naming',
        description: '规定字段命名必须使用下划线命名法，并且主键必须命名为id'
      },
      {
        id: '3',
        name: '数据完整性规则',
        type: 'quality',
        description: '确保实体表必须有主键，并且关键业务字段不允许为空'
      },
      {
        id: '4',
        name: '敏感数据处理规则',
        type: 'security',
        description: '对包含个人信息的字段进行标记和加密处理'
      }
    ]

    standardsLoading.value = false
  }, 1000)
}

// 处理主题域变更
const handleDomainChange = () => {
  // 重新加载标准
  loadAvailableStandards()
}

// 移除选中的标准
const removeSelectedStandard = (index) => {
  selectedStandards.value.splice(index, 1)
}

// 获取标准类型标签样式
const getStandardTypeTag = (type) => {
  switch (type) {
    case 'naming':
      return 'primary'
    case 'quality':
      return 'success'
    case 'security':
      return 'danger'
    default:
      return 'info'
  }
}

// 获取标准类型名称
const getStandardTypeName = (type) => {
  switch (type) {
    case 'naming':
      return '命名规范'
    case 'quality':
      return '数据质量'
    case 'security':
      return '安全规则'
    default:
      return '未知类型'
  }
}

// 查看标准详情
const handleViewStandard = (standard) => {
  // TODO: 显示标准详情
  console.log('查看标准:', standard)
}

// 应用标准
const applyStandards = () => {
  if (selectedStandards.value.length === 0) {
    ElMessage.warning('请先选择要应用的标准')
    return
  }

  standardsActiveTab.value = 'result'
  standardsApplied.value = false
  applyProgress.value = 0

  // 模拟应用进度
  const timer = setInterval(() => {
    applyProgress.value += 10

    if (applyProgress.value >= 100) {
      clearInterval(timer)
      standardsApplied.value = true

      // 应用标准逻辑
      applyStandardsToModel()
    }
  }, 300)
}

// 将标准应用到模型
const applyStandardsToModel = () => {
  // TODO: 实现标准应用逻辑
  console.log('应用标准:', selectedStandards.value)
}

// 导出SQL
const exportSQL = () => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 验证模型
  if (!validateModel()) {
    return
  }

  // TODO: 生成SQL
  const sql = generateSQL()

  // 显示SQL预览对话框
  sqlPreviewVisible.value = true
  sqlPreviewContent.value = sql
}

// 生成SQL
const generateSQL = () => {
  let sql = ''

  // 获取所有实体表
  const entityTables = nodes.value.filter(node => node.type === 'entity-table')

  // 生成表创建语句
  entityTables.forEach(table => {
    sql += `-- 创建表: ${table.name}\n`
    sql += `CREATE TABLE \`${table.name}\`
            (  `

    // 字段定义
    if (table.config.fields && table.config.fields.length > 0) {
      table.config.fields.forEach((field, index) => {
        sql += `  \`${field.name}\` ${getSQLType(field)}`

        // 非空约束
        if (field.notNull) {
          sql += ' NOT NULL'
        }

        // 自增
        if (field.autoIncrement) {
          sql += ' AUTO_INCREMENT'
        }

        // 默认值
        if (field.defaultValue) {
          sql += ` DEFAULT ${field.defaultValue}`
        }

        // 注释
        if (field.comment) {
          sql += ` COMMENT '${field.comment}'`
        }

        // 逗号
        if (index < table.config.fields.length - 1 || table.config.fields.some(f => f.isPrimary)) {
          sql += ',\n'
        } else {
          sql += '\n'
        }
      })

      // 主键约束
      const primaryKeys = table.config.fields.filter(field => field.isPrimary)
      if (primaryKeys.length > 0) {
        sql += `  PRIMARY KEY (${primaryKeys.map(pk => `\`${pk.name}\``).join(', ')})`

        // 如果有索引，添加逗号
        if (table.config.indexes && table.config.indexes.length > 0) {
          sql += ',\n'
        } else {
          sql += '\n'
        }
      }

      // 索引
      if (table.config.indexes && table.config.indexes.length > 0) {
        table.config.indexes.forEach((index, i) => {
          if (index.type === 'unique') {
            sql += `  UNIQUE INDEX \`${index.name}\` (${index.fields.map(f => `\`${f}\``).join(', ')})`
          } else {
            sql += `  INDEX \`${index.name}\` (${index.fields.map(f => `\`${f}\``).join(', ')})`
          }

          if (i < table.config.indexes.length - 1) {
            sql += ',\n'
          } else {
            sql += '\n'
          }
        })
      }
    }

    sql += `) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='${table.config.description || table.name}';\n\n`
  })

  // 生成外键约束
  entityTables.forEach(table => {
    if (table.config.fields) {
      const foreignKeys = table.config.fields.filter(field => field.isForeignKey && field.foreignKey)

      if (foreignKeys.length > 0) {
        foreignKeys.forEach(fk => {
          const targetTable = nodes.value.find(node => node.id === fk.foreignKey.targetTable)

          if (targetTable) {
            sql += `-- 添加外键约束: ${table.name}.${fk.name} -> ${targetTable.name}.${fk.foreignKey.targetField}\n`
            sql += `ALTER TABLE \`${table.name}\`
              ADD CONSTRAINT \`fk_${table.name}_${fk.name}\` FOREIGN KEY (\`${fk.name}\`) REFERENCES \`${targetTable.name}\` (\`${fk.foreignKey.targetField}\`) ON DELETE ${fk.foreignKey.onDelete};    `
          }
        })
      }
    }
  })

  return sql
}

// 获取SQL类型
const getSQLType = (field) => {
  switch (field.type) {
    case 'int':
    case 'bigint':
    case 'smallint':
    case 'tinyint':
      return `${field.type}(${field.length || 11})`
    case 'varchar':
    case 'char':
      return `${field.type}(${field.length || 255})`
    case 'decimal':
    case 'float':
    case 'double':
      return `${field.type}(${field.length || 10}, ${field.precision || 2})`
    default:
      return field.type
  }
}

// SQL预览相关
const sqlPreviewVisible = ref(false)
const sqlPreviewContent = ref('')


// 添加实体表
const handleAddEntityTable = () => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 计算画布中心位置
  const {width, height} = graph.value.getGraphArea()
  const center = graph.value.clientToLocal(width / 2, height / 2)

  // 如果画布中心已有节点，则偏移一点位置
  const x = center.x || 200
  const y = center.y || 200

  // 创建实体表节点
  createEntityTableNode(x, y)

  ElMessage.success('已添加新实体表')
}

// 属性面板控制
const showPropertyPanel = computed(() => {
  return !!selectedNode.value || showEmptyPropertyPanel.value
})
const showEmptyPropertyPanel = ref(false)

// 监听节点选择，自动显示属性面板
watch(selectedNode, (newVal) => {
  if (newVal) {
    // 如果选中了节点，确保属性面板显示
    showEmptyPropertyPanel.value = false
  }
}, {immediate: true})

// 处理属性面板关闭
const handlePropertyPanelClose = () => {
  showEmptyPropertyPanel.value = false
  // 临时存储当前选中节点的ID
  const currentNodeId = selectedNode.value ? selectedNode.value.id : null

  // 清空选中节点，这样再次点击同一节点时会触发选择事件
  selectedNode.value = null

  // 如果图形实例存在，取消节点的选中状态
  if (graph.value && currentNodeId) {
    // 使用X6正确的API取消选择
    if (graph.value.isSelected) {
      const cell = graph.value.getCellById(currentNodeId)
      if (cell && graph.value.isSelected(cell)) {
        graph.value.cleanSelection()
      }
    } else if (graph.value.selection) {
      // 另一种可能的API
      graph.value.selection.clean()
    }
  }
}

// 监听字段变更，更新节点显示
watch(() => selectedNode.value?.config?.fields, (newFields, oldFields) => {
  if (selectedNode.value && selectedNode.value.type === 'entity-table' && newFields) {
    // 检查是否有实际变化
    const hasChanged = JSON.stringify(newFields) !== JSON.stringify(oldFields)
    if (hasChanged) {
      updateEntityTableNode(selectedNode.value.id)
    }
  }
}, {deep: true})

// 处理字段类型变化
const handleFieldTypeChange = (field, newType) => {
  console.log('字段类型变化:', field.name, newType)

  // 根据类型设置默认长度
  if (needLength(newType)) {
    if (!field.length) {
      // 设置默认长度
      if (newType === 'varchar') {
        field.length = 255
      } else if (newType === 'char') {
        field.length = 50
      } else if (newType === 'int') {
        field.length = 11
      } else if (newType === 'bigint') {
        field.length = 20
      } else if (newType === 'decimal') {
        field.length = 10
      } else {
        field.length = 50
      }
    }
  } else {
    // 如果新类型不需要长度，清除长度值
    field.length = null
  }

  // 更新画布上的节点显示
  if (selectedNode.value && selectedNode.value.type === 'entity-table') {
    updateEntityTableNode(selectedNode.value)
  }
}


// 更丰富的主题域选项
const domainOptions = ref([
  {id: 'customer', name: '客户域'},
  {id: 'product', name: '产品域'},
  {id: 'order', name: '订单域'},
  {id: 'finance', name: '财务域'},
  {id: 'marketing', name: '营销域'},
  {id: 'supply', name: '供应链域'},
  {id: 'hr', name: '人力资源域'},
  {id: 'service', name: '客户服务域'},
  // 新增主题域
  {id: 'sales', name: '销售域'},
  {id: 'logistics', name: '物流域'},
  {id: 'inventory', name: '库存域'},
  {id: 'production', name: '生产域'},
  {id: 'quality', name: '质量域'},
  {id: 'research', name: '研发域'},
  {id: 'legal', name: '法务域'},
  {id: 'procurement', name: '采购域'},
  {id: 'asset', name: '资产域'},
  {id: 'risk', name: '风控域'},
  {id: 'compliance', name: '合规域'},
  {id: 'it', name: 'IT域'}
])


const selectedAppliedDomainId = ref('customer') // 默认选中客户域

// 当打开标准对话框时，默认显示已应用标准标签页
const openStandardsDialog = () => {
  // 确保数组已初始化
  if (!standardsForm.value.domainIds) {
    standardsForm.value.domainIds = []
  }

  if (!modelForm.value.domainIds) {
    modelForm.value.domainIds = []
  }

  standardsForm.value.domainIds = [...modelForm.value.domainIds]
  standardsActiveTab.value = 'applied' // 默认显示已应用标准标签页

  // 如果有已应用的主题域，默认选择第一个
  if (appliedDomainIds.value && appliedDomainIds.value.length > 0) {
    selectedAppliedDomainId.value = appliedDomainIds.value[0]
  } else {
    selectedAppliedDomainId.value = ''
  }

  standardsDialogVisible.value = true
  loadDomainStandards()
}

// 当选择已应用的主题域时
const handleAppliedDomainChange = (domainId) => {
  selectedAppliedDomainId.value = domainId
  console.log('选择已应用的主题域:', domainId, getDomainName(domainId))
}

// 移除已应用的主题域
const removeAppliedDomain = (domainId) => {
  if (!appliedDomainIds.value) {
    appliedDomainIds.value = []
    return
  }

  // 移除主题域
  appliedDomainIds.value = appliedDomainIds.value.filter(id => id !== domainId)

  // 移除该主题域下的所有标准
  if (appliedStandards.value) {
    appliedStandards.value = appliedStandards.value.filter(std => std.domainId !== domainId)
  }

  // 如果移除的是当前选中的主题域，则清空选择
  if (selectedAppliedDomainId.value === domainId) {
    selectedAppliedDomainId.value = ''
  }

  ElMessage.success(`已移除 ${getDomainName(domainId)} 主题域及其标准`)
}

// 移除已应用的标准
const removeAppliedStandard = (standard) => {
  if (!appliedStandards.value) {
    return
  }

  // 如果是必须执行的标准，则不允许单独移除
  if (standard.required) {
    ElMessage.warning(`${standard.name} 是必须执行的标准，无法单独移除`)
    return
  }

  // 移除标准
  appliedStandards.value = appliedStandards.value.filter(std => std.id !== standard.id)

  ElMessage.success(`已移除标准: ${standard.name}`)
}


// 获取指定主题域下的已应用标准
const getAppliedStandardsByDomain = (domainId, isRequired) => {
  if (!appliedStandards.value || !domainId) {
    return []
  }

  return appliedStandards.value.filter(std =>
      std.domainId === domainId && std.required === isRequired
  ) || [] // 确保返回数组
}


// 获取主题域名称
const getDomainName = (domainId) => {
  if (!domainId) return '未知主题域'

  // 定义所有主题域的映射关系
  const domainMap = {
    'customer': '客户域',
    'product': '产品域',
    'finance': '财务域',
    'hr': '人力资源域',
    'order': '订单域',
    'marketing': '营销域',
    'supply': '供应链域',
    'service': '客户服务域',
    'sales': '销售域',
    'logistics': '物流域',
    'inventory': '库存域',
    'production': '生产域'
  }

  // 直接从映射中获取，如果不存在则返回原ID
  return domainMap[domainId] || `${domainId}域`
}

// 视图模式：canvas(画布视图) 或 list(列表视图)
const viewMode = ref('canvas')

// 列表视图搜索关键词
const listSearchKeyword = ref('')
const relationSearchKeyword = ref('')

// 切换视图模式
const switchViewMode = (mode) => {
  console.log('切换视图模式:', mode)

  // 更新视图模式
  this.viewMode = mode

  // 从列表视图切换到画布视图
  if (mode === 'canvas') {
    console.log('切换到画布视图')
    // 使用setTimeout代替$nextTick，避免this上下文问题
    setTimeout(() => {
      // 如果画布已初始化，则调整大小并居中内容
      if (this.graph) {
        // 调整画布大小
        this.graph.resize()
        // 居中显示内容
        this.graph.centerContent()
        console.log('画布已调整大小并居中内容')
      } else {
        console.warn('画布未初始化，无法调整大小')
      }
    }, 0)
  }

  // 从画布视图切换到列表视图
  if (mode === 'list') {
    console.log('切换到列表视图')
    // 更新列表数据
    this.updateListView()
  }
}


// 过滤后的实体表
const filteredEntityTables = computed(() => {
  if (!listSearchKeyword.value) return entityTables.value

  const keyword = listSearchKeyword.value.toLowerCase()
  return entityTables.value.filter(table =>
      (table.id && table.id.toLowerCase().includes(keyword)) ||
      (table.name && table.name.toLowerCase().includes(keyword)) ||
      (table.type && table.type.toLowerCase().includes(keyword))
  )
})

// 过滤后的关系
const filteredRelations = computed(() => {
  if (!relationSearchKeyword.value) return relations.value

  const keyword = relationSearchKeyword.value.toLowerCase()
  return relations.value.filter(relation =>
      (relation.id && relation.id.toLowerCase().includes(keyword)) ||
      (relation.name && relation.name.toLowerCase().includes(keyword)) ||
      (relation.sourceTable && relation.sourceTable.toLowerCase().includes(keyword)) ||
      (relation.targetTable && relation.targetTable.toLowerCase().includes(keyword)) ||
      (relation.type && relation.type.toLowerCase().includes(keyword))
  )
})


// 获取实体类型标签样式
const getEntityTypeTag = (type) => {
  const typeMap = {
    'entity': '',
    'fact': 'success',
    'dimension': 'warning',
    'lookup': 'info',
    'bridge': 'danger'
  }
  return typeMap[type] || ''
}

// 获取实体类型名称
const getEntityTypeName = (type) => {
  const typeMap = {
    'entity': '实体表',
    'fact': '事实表',
    'dimension': '维度表',
    'lookup': '查找表',
    'bridge': '桥接表'
  }
  return typeMap[type] || type
}

// 获取关系类型标签样式
const getRelationTypeTag = (type) => {
  const typeMap = {
    'oneToOne': '',
    'oneToMany': 'success',
    'manyToOne': 'warning',
    'manyToMany': 'danger'
  }
  return typeMap[type] || ''
}

// 获取关系类型名称
const getRelationTypeName = (type) => {
  const typeMap = {
    'oneToOne': '一对一',
    'oneToMany': '一对多',
    'manyToOne': '多对一',
    'manyToMany': '多对多'
  }
  return typeMap[type] || type
}

// 添加调试日志，查看实体表数据结构
const updateListView = () => {
  try {
    console.log('更新列表视图数据...')

    if (!graph.value) {
      console.error('图形实例不存在，无法更新列表数据')
      return
    }

    // 清空列表数据
    entityTables.value = []
    relations.value = []

    // 获取所有节点和边
    const nodes = graph.value.getNodes()
    const edges = graph.value.getEdges()

    console.log('画布中的节点:', nodes)
    console.log('画布中的边:', edges)

    // 提取实体表
    nodes.forEach(node => {
      const nodeData = node.getData()
      console.log('节点数据:', nodeData)

      if (nodeData && nodeData.type === 'entity-table') {
        // 提取字段数据
        const fields = nodeData.config?.fields || []
        console.log('字段数据:', fields)

        // 添加到实体表列表
        entityTables.value.push({
          id: nodeData.id,
          name: nodeData.name,
          type: 'entity',
          fields: fields,
          // 保留其他属性
          ...nodeData
        })
      } else if (nodeData && nodeData.type === 'relation') {
        // 添加到关系列表
        relations.value.push({
          id: nodeData.id,
          name: nodeData.name,
          type: nodeData.relationType || '1:N',
          source: nodeData.source,
          target: nodeData.target,
          // 保留其他属性
          ...nodeData
        })
      }
    })

    console.log('更新后的实体表列表:', entityTables.value)
    console.log('更新后的关系列表:', relations.value)
  } catch (error) {
    console.error('更新列表视图数据时出错:', error)
    ElMessage.error('更新列表视图数据失败: ' + (error.message || '未知错误'))
  }
}


// 添加 watch 来监听 viewMode 的变化
watch(viewMode, async (newMode, oldMode) => {
  console.log('视图模式变化:', oldMode, '->', newMode)

  // 从列表视图切换到画布视图
  if (newMode === 'canvas' && oldMode === 'list') {
    console.log('从列表视图切换到画布视图')

    // 确保画布已初始化
    if (graph.value) {
      try {
        // 等待DOM更新
        await nextTick()

        // 增加延迟确保容器完全挂载
        await new Promise(resolve => setTimeout(resolve, 300))

        // 检查容器是否存在
        const container = canvasRef.value
        if (!container) {
          throw new Error('画布容器元素未找到')
        }

        // 检查容器是否在文档中
        if (!document.body.contains(container)) {
          throw new Error('画布容器元素未正确挂载到文档中')
        }

        // 检查容器尺寸
        const {width, height} = container.getBoundingClientRect()
        if (width === 0 || height === 0) {
          throw new Error('画布容器尺寸异常，请检查容器样式')
        }

        // 如果已经初始化过，先销毁旧实例
        if (graph.value) {
          graph.value.dispose()
          graph.value = null
        }

        // 初始化图形
        graph.value = new Graph({
          container,
          width: width,
          height: height,
          grid: {
            type: 'mesh',
            size: 10,
            visible: true,
            color: '#ddd'
          },
          mousewheel: {
            enabled: true,
            modifiers: ['ctrl', 'meta'],
            minScale: 0.5,
            maxScale: 2
          },
          connecting: {
            snap: true,
            allowBlank: false,
            allowLoop: false,
            highlight: true,
            connector: 'rounded',
            connectionPoint: 'boundary',
            router: {
              name: 'manhattan'
            },
            validateConnection({sourceView, targetView, sourceMagnet, targetMagnet}) {
              if (!sourceView || !targetView || !sourceMagnet || !targetMagnet) {
                return false
              }

              // 禁止自连接
              if (sourceView === targetView) {
                return false
              }

              // 只允许输出端口连接到输入端口
              const sourcePort = sourceMagnet.getAttribute('port-group')
              const targetPort = targetMagnet.getAttribute('port-group')
              if (sourcePort === 'output' && targetPort === 'input') {
                return true
              }

              return false
            },
            createEdge() {
              return new Shape.Edge({
                attrs: {
                  line: {
                    stroke: '#5F95FF',
                    strokeWidth: 2,
                    targetMarker: {
                      name: 'classic',
                      size: 8
                    }
                  }
                }
              })
            }
          },
          selecting: {
            enabled: true,
            multiple: true,
            rubberband: true,
            movable: true,
            showNodeSelectionBox: true
          },
          history: {
            enabled: true,
            beforeAddCommand(event, args) {
              return true // 返回false可以阻止命令被添加到历史记录
            }
          },
          clipboard: {
            enabled: true
          },
          keyboard: {
            enabled: true
          }
        })

        // 监听画布事件
        graph.value.on('blank:click', () => {
          selectedNode.value = null
        })

        // 监听节点选择事件
        graph.value.on('node:click', ({node}) => {
          selectedNode.value = node.getData()
        })

        // 监听边连接事件
        graph.value.on('edge:connected', ({edge}) => {
          const sourceNode = edge.getSourceNode()
          const targetNode = edge.getTargetNode()

          if (sourceNode && targetNode) {
            const sourceData = sourceNode.getData()
            const targetData = targetNode.getData()

            // 如果连接的是两个实体表，自动创建关系节点
            if (sourceData.type === 'entity-table' && targetData.type === 'entity-table') {
              // 获取边的中点位置
              const edgeView = graph.value.findViewByCell(edge)
              const pathElement = edgeView.findOne('path')
              const pathLength = pathElement.getTotalLength()
              const middlePoint = pathElement.getPointAtLength(pathLength / 2)

              // 创建关系节点
              createRelationNode(middlePoint.x, middlePoint.y)

              // 移除原始边
              graph.value.removeEdge(edge)
            }
          }
        })

        isCanvasInitialized.value = true
        console.log('画布初始化成功')
      } catch (error) {
        console.error('切换到画布视图时出错:', error)
        ElMessage.error(error.message || '切换到画布视图失败')
      }
    } else {
      console.warn('图形未初始化，无法调整大小')

      // 如果图形未初始化，尝试重新初始化
      try {
        // 等待DOM更新
        await nextTick()

        // 增加延迟确保容器完全可见
        await new Promise(resolve => setTimeout(resolve, 300))

        // 初始化图形
        initGraph()
      } catch (error) {
        console.error('重新初始化图形时出错:', error)
        ElMessage.error(error.message || '重新初始化图形失败')
      }
    }
  }

  // 从画布视图切换到列表视图
  if (newMode === 'list' && oldMode === 'canvas') {
    console.log('从画布视图切换到列表视图')
    // 更新列表数据
    updateListView()
  }
}, {immediate: false})


// 切换到列表视图
const handleSwitchToList = () => {
  console.log('切换到列表视图')
  viewMode.value = 'list'
  updateListView()
}


// 在 setup 函数中定义 loadInitialData 函数
const loadInitialData = () => {
  try {
    console.log('加载初始数据')

    // 这里可以从API获取数据或使用模拟数据
    const mockData = getMockData()

    // 添加节点
    if (mockData.nodes && mockData.nodes.length > 0) {
      mockData.nodes.forEach(nodeData => {
        const node = graph.value.addNode({
          id: nodeData.id,
          shape: 'rect',
          x: nodeData.x,
          y: nodeData.y,
          width: 180,
          height: 120,
          attrs: {
            body: {
              fill: '#ffffff',
              stroke: '#5F95FF',
              strokeWidth: 1,
              rx: 4,
              ry: 4,
            },
            label: {
              text: nodeData.name,
              fill: '#333333',
              fontSize: 14,
              fontWeight: 'bold',
              refX: 0.5,
              refY: 20,
              textAnchor: 'middle',
            },
          },
          data: nodeData,
        })
      })
    }

    // 添加边
    if (mockData.edges && mockData.edges.length > 0) {
      mockData.edges.forEach(edgeData => {
        graph.value.addEdge({
          id: edgeData.id,
          source: edgeData.source,
          target: edgeData.target,
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 1,
              targetMarker: {
                name: 'classic',
                size: 8,
              },
            },
          },
          labels: [
            {
              attrs: {
                text: {
                  text: edgeData.name,
                },
              },
              position: {
                distance: 0.5,
              },
            },
          ],
          data: edgeData,
        })
      })
    }

    // 居中显示内容
    graph.value.centerContent()

    console.log('初始数据加载完成')
  } catch (error) {
    console.error('加载初始数据时出错:', error)
  }
}

// 获取模拟数据
const getMockData = () => {
  return {
    nodes: [
      {
        id: 'node1',
        name: '客户信息',
        type: 'entity',
        x: 100,
        y: 100,
        fields: [
          {name: 'id', type: 'string', primary: true},
          {name: 'name', type: 'string'},
          {name: 'age', type: 'number'},
          {name: 'email', type: 'string'},
        ],
      },
      {
        id: 'node2',
        name: '订单信息',
        type: 'entity',
        x: 400,
        y: 100,
        fields: [
          {name: 'id', type: 'string', primary: true},
          {name: 'order_no', type: 'string'},
          {name: 'customer_id', type: 'string', foreign: true},
          {name: 'amount', type: 'number'},
          {name: 'create_time', type: 'datetime'},
        ],
      },
      {
        id: 'node3',
        name: '产品信息',
        type: 'entity',
        x: 400,
        y: 300,
        fields: [
          {name: 'id', type: 'string', primary: true},
          {name: 'name', type: 'string'},
          {name: 'price', type: 'number'},
          {name: 'stock', type: 'number'},
        ],
      },
    ],
    edges: [
      {
        id: 'edge1',
        name: '下单',
        type: 'oneToMany',
        source: 'node1',
        target: 'node2',
      },
      {
        id: 'edge2',
        name: '包含',
        type: 'manyToMany',
        source: 'node2',
        target: 'node3',
      },
    ],
  }
}


// 修复 convertListToCanvas 函数，确保字段数据正确传递和渲染
const convertListToCanvas = () => {
  try {
    console.log('开始将列表数据转换为画布组件...')
    console.log('实体表数据:', entityTables.value)
    console.log('关系数据:', relations.value)

    if (!graph.value) {
      console.error('图形实例不存在，无法转换数据')
      return
    }

    // 清空画布
    graph.value.clearCells()

    // 转换实体表
    if (entityTables.value && entityTables.value.length > 0) {
      console.log(`转换 ${entityTables.value.length} 个实体表...`)

      // 计算布局位置
      const columns = Math.ceil(Math.sqrt(entityTables.value.length))
      const spacing = 250

      entityTables.value.forEach((entity, index) => {
        const row = Math.floor(index / columns)
        const col = index % columns

        const x = 100 + col * spacing
        const y = 100 + row * spacing

        console.log(`处理实体表: ${entity.name}`, entity)
        console.log('字段数据:', entity.fields)

        // 确保字段数据存在
        const fields = entity.fields || []

        // 使用现有的 createEntityTableNode 函数创建节点
        if (typeof createEntityTableNode === 'function') {
          // 准备节点数据
          const nodeData = {
            id: entity.id || `entity-${index}`,
            type: 'entity-table',
            name: entity.name,
            config: {
              fields: fields
            }
          }

          console.log('使用 createEntityTableNode 创建节点:', nodeData)
          createEntityTableNode(x, y, nodeData)
        } else {
          // 如果 createEntityTableNode 函数不存在，使用我们自己的逻辑
          console.log('createEntityTableNode 函数不存在，使用自定义逻辑创建节点')

          // 生成字段文本
          let fieldsText = ''
          if (fields.length > 0) {
            fields.forEach(field => {
              const primaryKey = field.primary || field.isPrimary ? '🔑 ' : ''
              const foreignKey = field.foreign || field.isForeignKey ? '🔗 ' : ''
              const notNull = field.notNull ? 'NN ' : ''
              fieldsText += `${primaryKey}${foreignKey}${field.name}: ${field.type || 'string'}\n`
            })
          } else {
            fieldsText = '无字段数据'
          }

          console.log('生成的字段文本:', fieldsText)

          // 计算节点高度
          const baseHeight = 50
          const fieldHeight = 20
          const height = baseHeight + Math.max(1, fields.length) * fieldHeight

          // 准备节点数据
          const nodeData = {
            id: entity.id || `entity-${index}`,
            name: entity.name,
            type: 'entity-table',
            fields: fields,
            config: {
              fields: fields
            }
          }

          // 创建节点
          const node = graph.value.addNode({
            id: nodeData.id,
            shape: 'entity-table-node',
            x: x,
            y: y,
            width: 200,
            height: height,
            attrs: {
              body: {
                fill: '#ffffff',
                stroke: '#5F95FF',
                strokeWidth: 1,
                rx: 6,
                ry: 6,
              },
              header: {
                fill: '#5F95FF',
                height: 30,
                refWidth: '100%',
                rx: 6,
                ry: 6,
              },
              title: {
                text: nodeData.name,
                fill: '#ffffff',
                fontSize: 14,
                fontWeight: 'bold',
                refX: '50%',
                refY: 15,
                textAnchor: 'middle',
              },
              fields: {
                text: fieldsText,
                fill: '#333',
                fontSize: 12,
                refX: 10,
                refY: 45,
                textAnchor: 'start',
                textVerticalAnchor: 'top',
                lineHeight: 20,
              },
            },
            ports: {
              groups: {
                input: {
                  position: 'left',
                  attrs: {
                    circle: {
                      r: 4,
                      magnet: true,
                      stroke: '#5F95FF',
                      strokeWidth: 1,
                      fill: '#fff',
                    },
                  },
                },
                output: {
                  position: 'right',
                  attrs: {
                    circle: {
                      r: 4,
                      magnet: true,
                      stroke: '#5F95FF',
                      strokeWidth: 1,
                      fill: '#fff',
                    },
                  },
                },
              },
              items: [
                {id: `port-in-${nodeData.id}`, group: 'input'},
                {id: `port-out-${nodeData.id}`, group: 'output'}
              ],
            },
            data: nodeData
          })

          console.log(`已创建实体节点: ${entity.name}`, node)
        }
      })
    }

    if (!relations.value || relations.value.length == 0) {

    }

    // 转换关系
    if (relations.value && relations.value.length > 0) {
      console.log(`转换 ${relations.value.length} 个关系...`)

      relations.value.forEach((relation, index) => {
        // 查找源节点和目标节点
        const sourceNode = graph.value.getCellById(relation.source)
        const targetNode = graph.value.getCellById(relation.target)

        if (sourceNode && targetNode) {
          // 计算关系节点的位置（源节点和目标节点的中点）
          const sourcePosition = sourceNode.getPosition()
          const targetPosition = targetNode.getPosition()

          const x = (sourcePosition.x + targetPosition.x) / 2
          const y = (sourcePosition.y + targetPosition.y) / 2

          // 获取关系类型文本
          const relationTypeText = getRelationTypeText(relation.type)

          // 准备节点数据
          const nodeData = {
            id: relation.id || `relation-${index}`,
            name: relation.name,
            type: 'relation',
            relationType: relation.type || '1:N',
            source: relation.source,
            target: relation.target
          }

          // 创建关系节点
          if (typeof createRelationNode === 'function') {
            createRelationNode(x, y, nodeData)
          } else {
            console.error('createRelationNode 函数不存在')
          }

          // 创建连接线
          if (typeof createEdge === 'function') {
            createEdge(sourceNode, targetNode, {
              relationType: relation.relationType || '1:N'
            })
          } else {
            // 如果 createEdge 函数不存在，使用默认的创建边的方法
            graph.value.addEdge({
              source: {cell: sourceId, port: `port-out-${sourceId}`},
              target: {cell: targetId, port: `port-in-${targetId}`},
              attrs: {
                line: {
                  stroke: '#5F95FF',
                  strokeWidth: 1,
                  targetMarker: {
                    name: 'classic',
                    size: 8,
                  },
                },
              },
              labels: [
                {
                  position: 0.5,
                  attrs: {
                    label: {
                      text: relation.relationType || '1:N',
                      fill: '#5F95FF',
                      fontSize: 12,
                    },
                  },
                },
              ],
              data: {
                relationType: relation.relationType || '1:N',
                sourceEntity: sourceId,
                targetEntity: targetId
              }
            })
          }

          // 使用自定义节点类型 'relation-node' 创建关系节点
          const relationNode = graph.value.addNode({
            id: nodeData.id,
            shape: 'relation-node', // 使用自定义节点类型
            x: x,
            y: y,
            width: 120,
            height: 60,
            attrs: {
              body: {
                fill: '#f5f5f5',
                stroke: '#5F95FF',
                strokeWidth: 1,
                rx: 6,
                ry: 6,
                strokeDasharray: '5 5',
              },
              label: {
                text: nodeData.name,
                fill: '#333333',
                fontSize: 12,
                fontWeight: 'bold',
                refX: '50%',
                refY: '50%',
                textAnchor: 'middle',
                textVerticalAnchor: 'middle',
              },
              typeLabel: {
                text: relationTypeText,
                fill: '#666666',
                fontSize: 10,
                refX: '50%',
                refY: '70%',
                textAnchor: 'middle',
                textVerticalAnchor: 'middle',
              },
            },
            ports: {
              groups: {
                input: {
                  position: 'left',
                  attrs: {
                    circle: {
                      r: 4,
                      magnet: true,
                      stroke: '#5F95FF',
                      strokeWidth: 1,
                      fill: '#fff',
                    },
                  },
                },
                output: {
                  position: 'right',
                  attrs: {
                    circle: {
                      r: 4,
                      magnet: true,
                      stroke: '#5F95FF',
                      strokeWidth: 1,
                      fill: '#fff',
                    },
                  },
                },
              },
              items: [
                {id: 'port-in', group: 'input'},
                {id: 'port-out', group: 'output'}
              ],
            },
            data: nodeData
          })

          console.log(`已创建关系节点: ${relation.name}`, relationNode)

          // 创建从源节点到关系节点的边
          const sourceEdge = graph.value.addEdge({
            source: {
              cell: sourceNode.id,
              port: 'port-out',
            },
            target: {
              cell: relationNode.id,
              port: 'port-in',
            },
            attrs: {
              line: {
                stroke: '#5F95FF',
                strokeWidth: 2,
                targetMarker: {
                  name: 'classic',
                  size: 8,
                },
              },
            },
            data: {
              type: 'relation-edge',
              relationId: relation.id,
            },
          })

          // 创建从关系节点到目标节点的边
          const targetEdge = graph.value.addEdge({
            source: {
              cell: relationNode.id,
              port: 'port-out',
            },
            target: {
              cell: targetNode.id,
              port: 'port-in',
            },
            attrs: {
              line: {
                stroke: '#5F95FF',
                strokeWidth: 2,
                targetMarker: {
                  name: 'classic',
                  size: 8,
                },
              },
            },
            data: {
              type: 'relation-edge',
              relationId: relation.id,
            },
          })

          console.log(`已创建关系边: ${relation.name}`, sourceEdge, targetEdge)
        } else {
          console.warn(`无法创建关系 ${relation.name}，源节点或目标节点不存在`)
        }
      })
    }

    // 居中显示内容
    graph.value.centerContent()

    console.log('列表数据转换为画布组件完成')
  } catch (error) {
    console.error('转换列表数据时出错:', error)
    ElMessage.error('转换列表数据失败: ' + (error.message || '未知错误'))
  }
}


// 添加 createEdge 函数（如果不存在）
const createEdge = (source, target, data = {}) => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  if (!source || !target) {
    console.error('源节点或目标节点为空')
    return
  }

  const sourceId = source.id
  const targetId = target.id

  console.log('创建边:', sourceId, targetId, data)

  // 创建边
  const edge = graph.value.addEdge({
    source: {cell: sourceId, port: `port-out-${sourceId}`},
    target: {cell: targetId, port: `port-in-${targetId}`},
    attrs: {
      line: {
        stroke: '#5F95FF',
        strokeWidth: 1,
        targetMarker: {
          name: 'classic',
          size: 8,
        },
      },
    },
    labels: [
      {
        position: 0.5,
        attrs: {
          label: {
            text: data.relationType || '1:N',
            fill: '#5F95FF',
            fontSize: 12,
          },
        },
      },
    ],
    data: {
      relationType: data.relationType || '1:N',
      sourceEntity: sourceId,
      targetEntity: targetId
    }
  })

  console.log('创建的边:', edge)
  return edge
}

// 添加 createRelationNode 函数（如果不存在）
const createRelationNode = (x, y, data = null) => {
  if (!graph.value) {
    ElMessage.error('画布未初始化，请刷新页面重试')
    return
  }

  // 生成唯一ID
  const id = data?.id || ('relation-' + Date.now())

  // 创建节点数据
  const nodeData = data || {
    id,
    type: 'relation',
    name: '新建关系',
    config: {
      relationType: '1:N',
      sourceEntity: '',
      targetEntity: '',
      description: ''
    }
  }

  console.log('创建关系节点:', nodeData)

  // 创建节点
  const node = graph.value.addNode({
    id: nodeData.id,
    shape: 'relation-node',
    x,
    y,
    width: 120,
    height: 40,
    attrs: {
      body: {
        fill: '#ffffff',
        stroke: '#5F95FF',
        strokeWidth: 1,
        rx: 20,
        ry: 20,
      },
      label: {
        text: nodeData.name,
        fill: '#333333',
        fontSize: 12,
        fontWeight: 'bold',
        refX: '50%',
        refY: '50%',
        textAnchor: 'middle',
        textVerticalAnchor: 'middle',
      },
      typeLabel: {
        text: nodeData.config?.relationType || '1:N',
        fill: '#5F95FF',
        fontSize: 10,
        fontWeight: 'bold',
        refX: '100%',
        refX2: -5,
        refY: 5,
      },
    },
    ports: {
      groups: {
        input: {
          position: 'left',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
        output: {
          position: 'right',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff',
            },
          },
        },
      },
      items: [
        {id: `port-in-${nodeData.id}`, group: 'input'},
        {id: `port-out-${nodeData.id}`, group: 'output'}
      ],
    },
    data: nodeData
  })

  console.log('创建的关系节点:', node)
  return node
}

// 修改 watch 函数中的切换逻辑，确保在切换到画布视图时注册自定义节点类型
watch(viewMode, async (newMode, oldMode) => {
  console.log('视图模式变化:', oldMode, '->', newMode)

  // 从列表视图切换到画布视图
  if (newMode === 'canvas' && oldMode === 'list') {
    console.log('从列表视图切换到画布视图')

    try {
      // 确保自定义节点类型已注册
      if (!Node.registry.exist('entity-table-node')) {
        Node.registry.register('entity-table-node', EntityTableNode, true)
      }
      if (!Node.registry.exist('relation-node')) {
        Node.registry.register('relation-node', RelationNode, true)
      }

      // 等待DOM更新
      await nextTick()

      // 增加延迟确保容器完全挂载
      await new Promise(resolve => setTimeout(resolve, 300))

      // 检查容器是否存在
      const container = canvasRef.value
      if (!container) {
        throw new Error('画布容器元素未找到')
      }

      // 检查容器是否在文档中
      if (!document.body.contains(container)) {
        throw new Error('画布容器元素未正确挂载到文档中')
      }

      // 检查容器尺寸
      const {width, height} = container.getBoundingClientRect()
      if (width === 0 || height === 0) {
        throw new Error('画布容器尺寸异常，请检查容器样式')
      }

      // 如果已经初始化过，先销毁旧实例
      if (graph.value) {
        graph.value.dispose()
        graph.value = null
      }

      // 初始化图形，使用与初始化相同的配置
      graph.value = new Graph({
        container,
        width: width,
        height: height,
        grid: {
          type: 'mesh',
          size: 10,
          visible: true,
          color: '#ddd'
        },
        mousewheel: {
          enabled: true,
          modifiers: ['ctrl', 'meta'],
          minScale: 0.5,
          maxScale: 2
        },
        connecting: {
          snap: true,
          allowBlank: false,
          allowLoop: false,
          highlight: true,
          connector: 'rounded',
          connectionPoint: 'boundary',
          router: {
            name: 'manhattan'
          },
          validateConnection({sourceView, targetView, sourceMagnet, targetMagnet}) {
            if (!sourceView || !targetView || !sourceMagnet || !targetMagnet) {
              return false
            }

            // 禁止自连接
            if (sourceView === targetView) {
              return false
            }

            // 只允许输出端口连接到输入端口
            const sourcePort = sourceMagnet.getAttribute('port-group')
            const targetPort = targetMagnet.getAttribute('port-group')
            if (sourcePort === 'output' && targetPort === 'input') {
              return true
            }

            return false
          },
          createEdge() {
            return new Shape.Edge({
              attrs: {
                line: {
                  stroke: '#5F95FF',
                  strokeWidth: 2,
                  targetMarker: {
                    name: 'classic',
                    size: 8
                  }
                }
              }
            })
          }
        },
        selecting: {
          enabled: true,
          multiple: true,
          rubberband: true,
          movable: true,
          showNodeSelectionBox: true
        },
        history: {
          enabled: true,
          beforeAddCommand(event, args) {
            return true // 返回false可以阻止命令被添加到历史记录
          }
        },
        clipboard: {
          enabled: true
        },
        keyboard: {
          enabled: true
        }
      })

      // 监听画布事件
      graph.value.on('blank:click', () => {
        selectedNode.value = null
      })

      // 监听节点选择事件
      graph.value.on('node:click', ({node}) => {
        selectedNode.value = node.getData()
      })

      // 监听边连接事件
      graph.value.on('edge:connected', ({edge}) => {
        const sourceNode = edge.getSourceNode()
        const targetNode = edge.getTargetNode()

        if (sourceNode && targetNode) {
          const sourceData = sourceNode.getData()
          const targetData = targetNode.getData()

          // 如果连接的是两个实体表，自动创建关系节点
          if (sourceData.type === 'entity-table' && targetData.type === 'entity-table') {
            // 获取边的中点位置
            const edgeView = graph.value.findViewByCell(edge)
            const pathElement = edgeView.findOne('path')
            const pathLength = pathElement.getTotalLength()
            const middlePoint = pathElement.getPointAtLength(pathLength / 2)

            // 创建关系节点
            createRelationNode(middlePoint.x, middlePoint.y)

            // 移除原始边
            graph.value.removeEdge(edge)
          }
        }
      })

      // 将列表数据转换为画布组件
      convertListToCanvas()

      console.log('画布已重新初始化并更新内容')
    } catch (error) {
      console.error('切换到画布视图时出错:', error)
      ElMessage.error(error.message || '切换到画布视图失败')
    }
  }

  // 从画布视图切换到列表视图
  if (newMode === 'list' && oldMode === 'canvas') {
    console.log('从画布视图切换到列表视图')
    // 更新列表数据
    updateListView()
  }
}, {immediate: false})

// 修改 handleSwitchToCanvas 方法，确保在切换到画布视图时注册自定义节点类型
const handleSwitchToCanvas = async () => {
  console.log('切换到画布视图')
  viewMode.value = 'canvas'

  try {
    // 确保自定义节点类型已注册
    if (!Node.registry.exist('entity-table-node')) {
      Node.registry.register('entity-table-node', EntityTableNode, true)
    }
    if (!Node.registry.exist('relation-node')) {
      Node.registry.register('relation-node', RelationNode, true)
    }

    // 等待DOM更新
    await nextTick()

    // 增加延迟确保容器完全挂载
    await new Promise(resolve => setTimeout(resolve, 300))

    // 检查容器是否存在
    const container = canvasRef.value
    if (!container) {
      throw new Error('画布容器元素未找到')
    }

    // 检查容器是否在文档中
    if (!document.body.contains(container)) {
      throw new Error('画布容器元素未正确挂载到文档中')
    }

    // 检查容器尺寸
    const {width, height} = container.getBoundingClientRect()
    if (width === 0 || height === 0) {
      throw new Error('画布容器尺寸异常，请检查容器样式')
    }

    // 如果已经初始化过，先销毁旧实例
    if (graph.value) {
      graph.value.dispose()
      graph.value = null
    }

    // 初始化图形，使用与初始化相同的配置
    graph.value = new Graph({
      container,
      width: width,
      height: height,
      grid: {
        type: 'mesh',
        size: 10,
        visible: true,
        color: '#ddd'
      },
      mousewheel: {
        enabled: true,
        modifiers: ['ctrl', 'meta'],
        minScale: 0.5,
        maxScale: 2
      },
      connecting: {
        snap: true,
        allowBlank: false,
        allowLoop: false,
        highlight: true,
        connector: 'rounded',
        connectionPoint: 'boundary',
        router: {
          name: 'manhattan'
        },
        validateConnection({sourceView, targetView, sourceMagnet, targetMagnet}) {
          if (!sourceView || !targetView || !sourceMagnet || !targetMagnet) {
            return false
          }

          // 禁止自连接
          if (sourceView === targetView) {
            return false
          }

          // 只允许输出端口连接到输入端口
          const sourcePort = sourceMagnet.getAttribute('port-group')
          const targetPort = targetMagnet.getAttribute('port-group')
          if (sourcePort === 'output' && targetPort === 'input') {
            return true
          }

          return false
        },
        createEdge() {
          return new Shape.Edge({
            attrs: {
              line: {
                stroke: '#5F95FF',
                strokeWidth: 2,
                targetMarker: {
                  name: 'classic',
                  size: 8
                }
              }
            }
          })
        }
      },
      selecting: {
        enabled: true,
        multiple: true,
        rubberband: true,
        movable: true,
        showNodeSelectionBox: true
      },
      history: {
        enabled: true,
        beforeAddCommand(event, args) {
          return true // 返回false可以阻止命令被添加到历史记录
        }
      },
      clipboard: {
        enabled: true
      },
      keyboard: {
        enabled: true
      }
    })

    // 监听画布事件
    graph.value.on('blank:click', () => {
      selectedNode.value = null
    })

    // 监听节点选择事件
    graph.value.on('node:click', ({node}) => {
      selectedNode.value = node.getData()
    })

    // 监听边连接事件
    graph.value.on('edge:connected', ({edge}) => {
      const sourceNode = edge.getSourceNode()
      const targetNode = edge.getTargetNode()

      if (sourceNode && targetNode) {
        const sourceData = sourceNode.getData()
        const targetData = targetNode.getData()

        // 如果连接的是两个实体表，自动创建关系节点
        if (sourceData.type === 'entity-table' && targetData.type === 'entity-table') {
          // 获取边的中点位置
          const edgeView = graph.value.findViewByCell(edge)
          const pathElement = edgeView.findOne('path')
          const pathLength = pathElement.getTotalLength()
          const middlePoint = pathElement.getPointAtLength(pathLength / 2)

          // 创建关系节点
          createRelationNode(middlePoint.x, middlePoint.y)

          // 移除原始边
          graph.value.removeEdge(edge)
        }
      }
    })

    // 将列表数据转换为画布组件
    convertListToCanvas()

    console.log('画布已重新初始化并更新内容')
  } catch (error) {
    console.error('切换到画布视图时出错:', error)
    ElMessage.error(error.message || '切换到画布视图失败')
  }
}


// 添加模拟数据
const mockEntityTables = ref([
  {
    id: 'entity-1',
    name: '用户表',
    fields: [
      {name: 'id', type: 'string', primary: true, comment: '用户ID'},
      {name: 'username', type: 'string', notNull: true, comment: '用户名'},
      {name: 'email', type: 'string', notNull: true, comment: '邮箱'},
      {name: 'phone', type: 'string', comment: '手机号'},
      {name: 'created_at', type: 'datetime', notNull: true, comment: '创建时间'},
      {name: 'updated_at', type: 'datetime', comment: '更新时间'}
    ]
  },
  {
    id: 'entity-2',
    name: '订单表',
    fields: [
      {name: 'id', type: 'string', primary: true, comment: '订单ID'},
      {name: 'user_id', type: 'string', notNull: true, isForeignKey: true, comment: '用户ID'},
      {name: 'order_no', type: 'string', notNull: true, comment: '订单编号'},
      {name: 'total_amount', type: 'decimal', notNull: true, comment: '订单总金额'},
      {name: 'status', type: 'integer', notNull: true, comment: '订单状态'},
      {name: 'created_at', type: 'datetime', notNull: true, comment: '创建时间'}
    ]
  },
  {
    id: 'entity-3',
    name: '商品表',
    fields: [
      {name: 'id', type: 'string', primary: true, comment: '商品ID'},
      {name: 'name', type: 'string', notNull: true, comment: '商品名称'},
      {name: 'price', type: 'decimal', notNull: true, comment: '商品价格'},
      {name: 'stock', type: 'integer', notNull: true, comment: '库存数量'},
      {name: 'category', type: 'string', comment: '商品分类'},
      {name: 'description', type: 'text', comment: '商品描述'}
    ]
  },
  {
    id: 'entity-4',
    name: '订单商品表',
    fields: [
      {name: 'id', type: 'string', primary: true, comment: '主键ID'},
      {name: 'order_id', type: 'string', notNull: true, isForeignKey: true, comment: '订单ID'},
      {name: 'product_id', type: 'string', notNull: true, isForeignKey: true, comment: '商品ID'},
      {name: 'quantity', type: 'integer', notNull: true, comment: '购买数量'},
      {name: 'price', type: 'decimal', notNull: true, comment: '购买单价'}
    ]
  }
])

const mockRelations = ref([
  {
    id: 'relation-1',
    name: '用户下单',
    type: 'oneToMany',
    source: 'entity-1',
    target: 'entity-2'
  },
  {
    id: 'relation-2',
    name: '订单包含商品',
    type: 'manyToMany',
    source: 'entity-2',
    target: 'entity-3',
    through: 'entity-4'
  }
])

// 添加加载模拟数据的方法
const loadMockData = () => {
  entityTables.value = mockEntityTables.value
  relations.value = mockRelations.value
  ElMessage.success('已加载模拟数据')
}

// 添加应用配置效果的方法
const applyConfiguration = () => {
  try {
    ElMessageBox.confirm(
        '确定要应用当前配置吗？这将生成相应的数据模型。',
        '应用配置',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'info'
        }
    )
        .then(() => {
          // 模拟应用配置过程
          ElMessage({
            type: 'info',
            message: '正在应用配置...'
          })

          setTimeout(() => {
            // 模拟配置应用成功
            ElMessage({
              type: 'success',
              message: '配置应用成功！已生成数据模型',
              duration: 3000
            })

            // 显示应用结果
            showApplicationResult()
          }, 1500)
        })
        .catch(() => {
          ElMessage({
            type: 'info',
            message: '已取消应用配置'
          })
        })
  } catch (error) {
    console.error('应用配置时出错:', error)
    ElMessage.error('应用配置失败: ' + (error.message || '未知错误'))
  }
}

// 显示应用结果
const showApplicationResult = () => {
  const resultHtml = `
    <div style="text-align: left;">
      <h3>数据模型生成结果</h3>
      <div style="margin-top: 10px;">
        <div style="font-weight: bold; color: #409EFF;">✅ 已生成实体表</div>
        <ul style="margin-top: 5px;">
          <li>用户表 (users) - 6个字段</li>
          <li>订单表 (orders) - 6个字段</li>
          <li>商品表 (products) - 6个字段</li>
          <li>订单商品表 (order_items) - 5个字段</li>
        </ul>
      </div>
      <div style="margin-top: 10px;">
        <div style="font-weight: bold; color: #409EFF;">✅ 已建立关联关系</div>
        <ul style="margin-top: 5px;">
          <li>用户表 → 订单表 (一对多)</li>
          <li>订单表 ↔ 商品表 (多对多，通过订单商品表)</li>
        </ul>
      </div>
      <div style="margin-top: 10px;">
        <div style="font-weight: bold; color: #409EFF;">✅ 已生成SQL脚本</div>
        <div style="margin-top: 5px; color: #67C23A;">SQL脚本已保存到项目目录</div>
      </div>
    </div>
  `

  ElMessageBox({
    title: '应用结果',
    dangerouslyUseHTMLString: true,
    message: resultHtml,
    confirmButtonText: '确定'
  })
}

// 添加实体表编辑相关变量和方法
const entityFormVisible = ref(false)
const entityFormTitle = ref('编辑实体表')
const entityForm = reactive({
  id: '',
  name: '',
  description: '',
  fields: []
})
const entityFormRules = {
  name: [
    {required: true, message: '请输入实体表名称', trigger: 'blur'},
    {min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur'}
  ]
}
const entityFormRef = ref(null)


// 编辑实体表
const editEntityTable = (row) => {
  console.log('编辑实体表', row)
  entityFormTitle.value = '编辑实体表'
  entityForm.id = row.id
  entityForm.name = row.name
  entityForm.description = row.description
  entityForm.tableType = row.tableType || 'business' // 默认为业务实体

  // 深拷贝字段数据，避免直接修改原数据
  entityForm.fields = JSON.parse(JSON.stringify(row.fields || []))

  // 确保每个字段都有_id属性
  entityForm.fields.forEach(field => {
    if (!field._id) {
      field._id = `temp-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
    }
    // 确保字段有长度属性（如果需要）
    if (needLength(field.type) && !field.length) {
      if (field.type === 'string' || field.type === 'varchar') {
        field.length = 255
      } else if (field.type === 'integer' || field.type === 'int') {
        field.length = 11
      } else if (field.type === 'decimal') {
        field.length = 10
      }
    }
  })

  // 初始化或复制索引数组
  entityForm.indexes = JSON.parse(JSON.stringify(row.indexes || []))

  // 打开抽屉
  console.log('打开抽屉前', entityFormDrawerVisible.value)
  entityFormDrawerVisible.value = true
  console.log('打开抽屉后', entityFormDrawerVisible.value)
}

// 删除实体表
const deleteEntityTable = (row) => {
  ElMessageBox.confirm(
      `确定要删除实体表 "${row.name}" 吗？此操作不可恢复。`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  )
      .then(() => {
        // 从实体表列表中移除
        const index = entityTables.value.findIndex(item => item.id === row.id)
        if (index !== -1) {
          entityTables.value.splice(index, 1)

          // 同时删除与该实体表相关的关系
          relations.value = relations.value.filter(
              relation => relation.source !== row.id && relation.target !== row.id
          )

          ElMessage.success(`已删除实体表 "${row.name}"`)
        }
      })
      .catch(() => {
        // 用户取消删除
      })
}


// 添加SQL预览功能
const previewSQL = () => {
  try {
    // 检查是否有实体表数据
    if (!entityTables.value || entityTables.value.length === 0) {
      ElMessage.warning('没有实体表数据，无法生成SQL预览')
      return
    }

    // 生成SQL预览内容
    const sqlContent = generateSQLPreview()

    // 显示SQL预览对话框
    ElMessageBox({
      title: 'SQL预览',
      dangerouslyUseHTMLString: true,
      message: sqlContent,
      showCancelButton: true,
      confirmButtonText: '复制SQL',
      cancelButtonText: '关闭',
      beforeClose: (action, instance, done) => {
        if (action === 'confirm') {
          // 复制SQL到剪贴板
          const tempTextarea = document.createElement('textarea')
          tempTextarea.value = sqlContent.replace(/<[^>]*>/g, '').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&nbsp;/g, ' ')
          document.body.appendChild(tempTextarea)
          tempTextarea.select()
          document.execCommand('copy')
          document.body.removeChild(tempTextarea)

          ElMessage.success('SQL已复制到剪贴板')
          done()
        } else {
          done()
        }
      }
    })
  } catch (error) {
    console.error('生成SQL预览时出错:', error)
    ElMessage.error('生成SQL预览失败: ' + (error.message || '未知错误'))
  }
}

// 生成SQL预览内容
const generateSQLPreview = () => {
  let sqlContent = '<div style="text-align: left; font-family: monospace; white-space: pre; overflow-x: auto; max-height: 500px;">'

  // 为每个实体表生成建表SQL
  entityTables.value.forEach(entity => {
    const tableName = entity.name.toLowerCase().replace(/\s+/g, '_')

    sqlContent += `<div style="margin-bottom: 20px; color: #333;">`
    sqlContent += `<div style="color: #888;">-- 创建${entity.name}表</div>`
    sqlContent += `<div style="color: #0033CC;">CREATE TABLE \`${tableName}\` (</div>`

    // 添加字段定义
    const fields = entity.fields || []
    fields.forEach((field, index) => {
      const isPrimary = field.primary || field.isPrimary
      const isNotNull = field.notNull
      const comment = field.comment ? ` COMMENT '${field.comment}'` : ''

      // 映射字段类型到SQL类型
      let sqlType = mapFieldTypeToSQLType(field.type)

      sqlContent += `  \`${field.name}\` ${sqlType}${isNotNull ? ' NOT NULL' : ''}${comment}`

      // 如果不是最后一个字段，添加逗号
      if (index < fields.length - 1 || isPrimary) {
        sqlContent += `,`
      }
      sqlContent += `</div>`
    })

    // 添加主键约束
    const primaryKeys = fields.filter(field => field.primary || field.isPrimary)
    if (primaryKeys.length > 0) {
      sqlContent += `  PRIMARY KEY (${primaryKeys.map(field => '`' + field.name + '`').join(', ')})</div>`
    }

    sqlContent += `<div style="color: #0033CC;">);</div>`
    sqlContent += `</div>`
  })

  // 为关系添加外键约束
  if (relations.value && relations.value.length > 0) {
    sqlContent += `<div style="margin-top: 20px; color: #888;">-- 添加外键约束</div>`

    relations.value.forEach(relation => {
      const sourceEntity = entityTables.value.find(entity => entity.id === relation.source)
      const targetEntity = entityTables.value.find(entity => entity.id === relation.target)

      if (sourceEntity && targetEntity) {
        const sourceTable = sourceEntity.name.toLowerCase().replace(/\s+/g, '_')
        const targetTable = targetEntity.name.toLowerCase().replace(/\s+/g, '_')

        // 根据关系类型生成不同的外键约束
        if (relation.type === 'oneToMany') {
          // 在多的一方添加外键
          const sourcePrimaryKey = sourceEntity.fields.find(field => field.primary || field.isPrimary)
          if (sourcePrimaryKey) {
            const foreignKeyName = `fk_${targetTable}_${sourceTable}`
            const foreignKeyField = `${sourceTable}_id`

            sqlContent += `<div style="color: #0033CC;">ALTER TABLE \`${targetTable}\` ADD CONSTRAINT \`${foreignKeyName}\` FOREIGN KEY (\`${foreignKeyField}\`) REFERENCES \`${sourceTable}\`(\`${sourcePrimaryKey.name}\`);</div>`
          }
        } else if (relation.type === 'manyToMany') {
          // 对于多对多关系，需要一个中间表
          const junctionTable = `${sourceTable}_${targetTable}`
          const sourcePrimaryKey = sourceEntity.fields.find(field => field.primary || field.isPrimary)
          const targetPrimaryKey = targetEntity.fields.find(field => field.primary || field.isPrimary)

          if (sourcePrimaryKey && targetPrimaryKey) {
            sqlContent += `<div style="color: #888;">-- 创建多对多关系的中间表</div>`
            sqlContent += `<div style="color: #0033CC;">CREATE TABLE \`${junctionTable}\` (</div>`
            sqlContent += `  \`id\` INT NOT NULL AUTO_INCREMENT,</div>`
            sqlContent += `  \`${sourceTable}_id\` ${mapFieldTypeToSQLType(sourcePrimaryKey.type)} NOT NULL,</div>`
            sqlContent += `  \`${targetTable}_id\` ${mapFieldTypeToSQLType(targetPrimaryKey.type)} NOT NULL,</div>`
            sqlContent += `  PRIMARY KEY (\`id\`),</div>`
            sqlContent += `  CONSTRAINT \`fk_${junctionTable}_${sourceTable}\` FOREIGN KEY (\`${sourceTable}_id\`) REFERENCES \`${sourceTable}\`(\`${sourcePrimaryKey.name}\`),</div>`
            sqlContent += `  CONSTRAINT \`fk_${junctionTable}_${targetTable}\` FOREIGN KEY (\`${targetTable}_id\`) REFERENCES \`${targetTable}\`(\`${targetPrimaryKey.name}\`)</div>`
            sqlContent += `<div style="color: #0033CC;">);</div>`
          }
        }
      }
    })
  }

  sqlContent += '</div>'
  return sqlContent
}

// 映射字段类型到SQL类型
const mapFieldTypeToSQLType = (fieldType) => {
  const typeMap = {
    'string': 'VARCHAR(255)',
    'text': 'TEXT',
    'integer': 'INT',
    'number': 'DECIMAL(10,2)',
    'decimal': 'DECIMAL(10,2)',
    'boolean': 'BOOLEAN',
    'date': 'DATE',
    'datetime': 'DATETIME',
    'timestamp': 'TIMESTAMP'
  }

  return typeMap[fieldType] || 'VARCHAR(255)'
}

// 更新工具栏按钮，添加SQL预览按钮
const toolbarButtons = ref([
  {name: '加载模拟数据', icon: 'DataLine', handler: loadMockData},
  {name: 'SQL预览', icon: 'Document', handler: previewSQL},
  {name: '应用配置', icon: 'Check', handler: applyConfiguration}
])

// 添加字段
const addField = () => {
  const newField = {
    _id: `temp-${Date.now()}`,
    name: '',
    type: 'string',
    length: 255, // 默认长度
    primary: false,
    notNull: false,
    isForeignKey: false,
    comment: ''
  }
  entityForm.fields.push(newField)
}

// 删除字段
const removeField = (index) => {
  entityForm.fields.splice(index, 1)
}

// 添加实体表
const addEntityTable = () => {
  entityFormTitle.value = '添加实体表'
  entityForm.id = ''
  entityForm.name = ''
  entityForm.description = ''
  entityForm.tableType = 'business' // 默认为业务实体
  entityForm.fields = [
    {
      _id: `temp-${Date.now()}`,
      name: 'id',
      type: 'integer',
      length: 11,
      primary: true,
      notNull: true,
      isForeignKey: false,
      comment: '主键ID'
    }
  ]
  entityForm.indexes = [] // 初始化索引数组
  entityFormDrawerVisible.value = true // 使用抽屉而不是对话框
}

// 保存实体表
const saveEntityTable = () => {
  entityFormRef.value.validate((valid) => {
    if (valid) {
      // 验证字段是否有名称
      const invalidFields = entityForm.fields.filter(field => !field.name.trim())
      if (invalidFields.length > 0) {
        ElMessage.warning('存在未命名的字段，请检查')
        return
      }

      // 验证是否有主键
      const hasPrimaryKey = entityForm.fields.some(field => field.primary)
      if (!hasPrimaryKey) {
        ElMessage.warning('请至少设置一个主键字段')
        return
      }

      // 准备保存的数据
      const saveData = {
        id: entityForm.id || `entity-${Date.now()}`,
        name: entityForm.name,
        description: entityForm.description,
        tableType: entityForm.tableType || 'business',
        fields: entityForm.fields.map(field => ({
          name: field.name,
          type: field.type,
          length: field.length,
          primary: field.primary,
          notNull: field.notNull,
          isForeignKey: field.isForeignKey,
          comment: field.comment
        })),
        indexes: entityForm.indexes
      }

      // 如果是编辑现有实体表
      if (entityForm.id) {
        const index = entityTables.value.findIndex(item => item.id === entityForm.id)
        if (index !== -1) {
          // 更新实体表
          entityTables.value[index] = saveData
          ElMessage.success(`已更新实体表 "${saveData.name}"`)
        }
      } else {
        // 添加新实体表
        entityTables.value.push(saveData)
        ElMessage.success(`已添加实体表 "${saveData.name}"`)
      }

      // 关闭抽屉
      entityFormDrawerVisible.value = false
    } else {
      ElMessage.warning('表单验证失败，请检查输入')
      return false
    }
  })
}

// 实体表表单抽屉是否可见
const entityFormDrawerVisible = ref(false)

// 关闭实体表表单抽屉
const closeEntityFormDrawer = () => {
  entityFormDrawerVisible.value = false
}

// 测试抽屉
const testDrawer = () => {
  console.log('测试抽屉')
  entityFormDrawerVisible.value = true
  console.log('抽屉状态:', entityFormDrawerVisible.value)
}


// 加载模型设计
const loadModelDesign = async (id) => {
  console.log('loadModelDesign被调用，modelId:', id)

  if (!id) {
    console.log('未提供modelId，跳过加载')
    return
  }

  // 检查画布是否已初始化
  if (!graph.value || !isCanvasInitialized.value) {
    console.error('画布未初始化，无法加载数据')
    pendingModelId.value = id
    return
  }

  try {
    loading.value = true

    // 调用后端API获取模型设计数据
    console.log('调用getModelDesign API, modelId:', id)
    const res = await getModelDesign(id)
    console.log('API返回数据:', res)

    if (res && res.code === 0 && res.data) {
      console.log('API调用成功，开始处理返回数据')
      const {model, design, standards} = res.data

      // 设置模型基本信息
      if (model) {
        console.log('设置模型基本信息:', model)
        modelForm.id = model.id
        modelForm.name = model.name
        modelForm.type = model.type || 'detail'
        modelForm.domainId = model.domainId
        modelForm.description = model.description
        modelForm.owner = model.owner
        modelForm.status = model.status || 'draft'
      } else {
        console.warn('返回数据中没有model信息')
      }

      // 设置设计信息
      if (design) {
        console.log('开始处理设计信息:', design)

        // 解析画布数据
        if (design.canvas) {
          console.log('开始解析画布数据')
          try {
            // 解析JSON字符串为对象
            const canvasData = JSON.parse(design.canvas)
            console.log('解析后的画布数据:', canvasData)

            // 解析字段数据
            let fieldsData = []
            if (design.fields) {
              try {
                fieldsData = JSON.parse(design.fields)
                console.log('解析后的字段数据:', fieldsData)
              } catch (e) {
                console.error('解析字段数据出错:', e)
              }
            }

            // 确保画布已初始化
            if (graph.value && isCanvasInitialized.value) {
              console.log('画布已初始化，开始加载节点和边')

              // 清空画布
              console.log('清空画布')
              graph.value.clearCells()

              // 清空节点列表
              nodes.value = []

              // 加载节点
              if (canvasData.nodes && canvasData.nodes.length > 0) {
                console.log(`开始加载${canvasData.nodes.length}个节点`)

                canvasData.nodes.forEach((nodeData, index) => {
                  console.log(`处理第${index + 1}个节点:`, nodeData)

                  try {
                    // 根据节点类型创建不同的节点
                    if (nodeData.type === 'entity-table') {
                      console.log('创建实体表节点')

                      // 检查必要属性
                      if (!nodeData.position) {
                        console.warn('节点缺少position属性，使用默认值')
                        nodeData.position = {x: 100, y: 100}
                      }

                      // 查找该节点的字段数据
                      const nodeFields = fieldsData.filter(field => field.nodeId === nodeData.id)
                      console.log('节点字段:', nodeFields)

                      // 准备节点数据
                      const entityData = {
                        id: nodeData.id,
                        type: 'entity-table',
                        name: nodeData.data?.name || '未命名实体',
                        config: {
                          description: nodeData.data?.description || '',
                          tableType: nodeData.data?.tableType || 'business',
                          fields: [],
                          indexes: []
                        }
                      }

                      // 添加字段数据
                      if (nodeFields && nodeFields.length > 0) {
                        entityData.config.fields = nodeFields.map(field => ({
                          name: field.name,
                          type: field.type,
                          length: field.length || 0,
                          isPrimary: field.isPrimary || false,
                          notNull: field.notNull || false,
                          isForeignKey: field.isForeignKey || false,
                          comment: field.comment || ''
                        }))
                      } else {
                        // 添加默认字段
                        entityData.config.fields = [{
                          name: 'id',
                          type: 'int',
                          length: 11,
                          isPrimary: true,
                          notNull: true,
                          isForeignKey: false,
                          comment: '主键ID'
                        }]
                      }

                      // 直接使用createEntityTableNode函数创建节点
                      const node = createEntityTableNode(
                          nodeData.position.x,
                          nodeData.position.y,
                          entityData
                      )

                      // 确保节点被添加到画布
                      if (node) {
                        // 将节点添加到画布
                        graph.value.addNode(node)

                        // 将节点添加到节点列表
                        nodes.value.push({
                          id: node.id,
                          name: entityData.name,
                          type: entityData.type,
                          data: entityData
                        })

                        console.log('节点创建并添加到画布:', node.id)
                      } else {
                        console.error('节点创建失败:', entityData)
                      }
                    } else {
                      console.warn('未知的节点类型:', nodeData.type)
                    }
                  } catch (e) {
                    console.error('添加节点出错:', e, nodeData)
                  }
                })
              } else {
                console.log('没有节点数据需要加载')
              }

              // 加载边
              if (canvasData.edges && canvasData.edges.length > 0) {
                console.log(`开始加载${canvasData.edges.length}个边`)

                canvasData.edges.forEach((edgeData, index) => {
                  console.log(`处理第${index + 1}个边:`, edgeData)

                  try {
                    // 确保源节点和目标节点存在
                    const sourceNode = graph.value.getCellById(edgeData.source)
                    const targetNode = graph.value.getCellById(edgeData.target)

                    if (!sourceNode || !targetNode) {
                      console.warn(`边的源节点或目标节点不存在: source=${edgeData.source}, target=${edgeData.target}`)
                      return
                    }

                    const edge = graph.value.addEdge({
                      id: edgeData.id,
                      source: {
                        cell: edgeData.source,
                        port: 'output' // 确保使用正确的端口
                      },
                      target: {
                        cell: edgeData.target,
                        port: 'input' // 确保使用正确的端口
                      },
                      router: {
                        name: 'manhattan'
                      },
                      connector: {
                        name: 'rounded'
                      },
                      attrs: {
                        line: {
                          stroke: '#5F95FF',
                          strokeWidth: 2,
                          targetMarker: {
                            name: 'classic',
                            size: 8
                          }
                        },
                        label: {
                          text: edgeData.data?.relationName || '',
                          fill: '#333',
                          fontSize: 12,
                          refX: 0.5,
                          refY: -10
                        }
                      },
                      data: edgeData.data || {}
                    })

                    console.log('边创建成功:', edge?.id)
                  } catch (e) {
                    console.error('添加边出错:', e, edgeData)
                  }
                })
              } else {
                console.log('没有边数据需要加载')
              }

              // 适应画布内容
              console.log('调整画布视图')
              setTimeout(() => {
                try {
                  graph.value.centerContent()
                  console.log('画布内容居中完成')
                } catch (e) {
                  console.error('画布居中出错:', e)
                }
              }, 1000) // 增加延时，确保所有元素加载完成
            } else {
              console.error('画布未初始化，无法加载数据')
              // 保存modelId，等画布初始化后再加载
              pendingModelId.value = id
            }
          } catch (e) {
            console.error('解析画布数据出错:', e)
          }
        } else {
          console.warn('设计信息中没有canvas数据')
        }

        // ... 保持原有的其他数据处理代码不变 ...
        // 记录原始关系数据，用于调试
        if (design.relations) {
          console.log('原始关系数据:', design.relations)
          try {
            // 尝试解析关系数据
            const parsedRelations = JSON.parse(design.relations)
            console.log('解析后的关系数据:', parsedRelations)

            // 存储解析后的关系数据，供后续使用
            if (Array.isArray(parsedRelations) && parsedRelations.length > 0) {
              // 处理嵌套数组结构
              const flattenedRelations = Array.isArray(parsedRelations[0])
                  ? parsedRelations.flat()
                  : parsedRelations

              console.log('扁平化后的关系数据:', flattenedRelations)

              // 提取关系节点
              const relationNodes = flattenedRelations.filter(item => item.type === 'relation')
              console.log('提取的关系节点:', relationNodes)

              // 存储关系节点数据
              if (relationNodes.length > 0) {
                relationNodes.forEach(node => {
                  if (node.data && node.data.config) {
                    // 将关系配置添加到nodes数组中，确保在getModelStandards中能够找到
                    const existingNode = nodes.value.find(n => n.id === node.id)
                    if (!existingNode) {
                      nodes.value.push({
                        id: node.id,
                        type: 'relationship',
                        config: node.data.config
                      })
                      console.log(`添加关系节点到nodes数组: ${node.id}`)
                    }
                  }
                })
              }
            }
          } catch (error) {
            console.error('解析关系数据失败:', error)
          }
        }


      } else {
        console.warn('返回数据中没有design信息')
      }

      // 设置标准关联
      if (standards) {
        console.log('设置标准关联:', standards)
        if (standards.length > 0) {
          appliedStandards.value = standards.map(s => ({
            id: s.standardId,
            type: s.standardType,
            fieldId: s.fieldId
          }))
        } else {
          appliedStandards.value = []
        }
      } else {
        console.warn('返回数据中没有standards信息')
      }

      console.log('模型设计数据加载完成')
    } else {
      console.error('API调用失败或返回数据格式不正确:', res)
      ElMessage.error(res?.message || '加载模型设计失败')
    }
  } catch (error) {
    console.error('加载模型设计出错:', error)
    ElMessage.error('加载模型设计出错: ' + (error.message || error))
  } finally {
    loading.value = false
    console.log('模型设计加载流程结束')
  }
}

// 监听模型ID变化，加载模型设计
watch(() => props.modelId, (newVal, oldVal) => {
  console.log('modelId变化:', oldVal, '->', newVal)
  if (newVal) {
    console.log('检测到有效的modelId，开始加载模型设计')
    loadModelDesign(newVal)
  } else {
    // 重置表单
    console.log('modelId为空，重置表单和画布')
    modelForm.id = ''
    modelForm.name = '新建模型'
    modelForm.type = 'detail'
    modelForm.domainId = ''
    modelForm.description = ''
    modelForm.owner = ''
    modelForm.status = 'draft'

    // 重置标准
    appliedStandards.value = []

    // 重置画布
    if (graph.value) {
      console.log('清空画布')
      graph.value.clearCells()
    }
  }
}, {immediate: true})  // 添加immediate: true确保组件初始化时就执行一次

// 组件挂载后初始化画布
onMounted(() => {
  console.log('组件挂载完成，初始modelId:', props.modelId, '初始domainId:', props.domainId)

  // 如果没有modelId但有domainId，说明是新建模型
  if (!props.modelId && props.domainId) {
    console.log('新建模型，设置domainId:', props.domainId)
    modelForm.domainId = props.domainId
  }

  // 使用nextTick确保DOM已完全渲染
  nextTick(() => {
    console.log('DOM已渲染，开始初始化画布')
    initGraph()
  })
})

// 初始化画布
const initGraph = () => {
  console.log('初始化画布')

  // 获取容器元素
  const container = document.getElementById('graph-container')
  if (!container) {
    console.error('找不到画布容器元素')
    return
  }

  // 获取容器尺寸
  const width = container.clientWidth
  const height = container.clientHeight

  // 初始化图形
  graph.value = new Graph({
    container,
    width: width,
    height: height,
    grid: {
      type: 'mesh',
      size: 10,
      visible: true,
      color: '#ddd'
    },
    mousewheel: {
      enabled: true,
      modifiers: ['ctrl', 'meta'],
      minScale: 0.5,
      maxScale: 2
    },
    connecting: {
      snap: true,
      allowBlank: false,
      allowLoop: false,
      highlight: true,
      connector: 'rounded',
      connectionPoint: 'boundary',
      router: {
        name: 'manhattan'
      },
      validateConnection({sourceView, targetView, sourceMagnet, targetMagnet}) {
        if (!sourceView || !targetView || !sourceMagnet || !targetMagnet) {
          return false
        }

        // 禁止自连接
        if (sourceView === targetView) {
          return false
        }

        // 只允许输出端口连接到输入端口
        const sourcePort = sourceMagnet.getAttribute('port-group')
        const targetPort = targetMagnet.getAttribute('port-group')
        if (sourcePort === 'output' && targetPort === 'input') {
          return true
        }

        return false
      },
      createEdge() {
        return new Shape.Edge({
          attrs: {
            line: {
              stroke: '#5F95FF',
              strokeWidth: 2,
              targetMarker: {
                name: 'classic',
                size: 8
              }
            }
          }
        })
      }
    },
    selecting: {
      enabled: true,
      multiple: true,
      rubberband: true,
      movable: true,
      showNodeSelectionBox: true
    },
    history: {
      enabled: true,
      beforeAddCommand(event, args) {
        return true // 返回false可以阻止命令被添加到历史记录
      }
    },
    clipboard: {
      enabled: true
    },
    keyboard: {
      enabled: true
    }
  })

  // 注册自定义节点
  registerCustomNodes()

  // 添加事件监听
  setupGraphEvents()

  // 如果画布初始化完成后，modelId已经存在，则加载数据
  if (props.modelId) {
    console.log('画布初始化完成，检测到modelId存在，加载数据')
    loadModelDesign(props.modelId)
  }
}

// 注册自定义节点
const registerCustomNodes = () => {
  if (!graph.value) return

  // 注册实体表节点
  Graph.registerNode('entity-table', {
    inherit: 'rect',
    width: 200,
    height: 200,
    attrs: {
      body: {
        fill: '#ffffff',
        stroke: '#5F95FF',
        strokeWidth: 1,
        rx: 5,
        ry: 5
      },
      // 表头
      '.header': {
        fill: '#f0f7ff',
        stroke: '#5F95FF',
        strokeWidth: 1,
        height: 30
      },
      // 表名
      '.title': {
        text: '实体表',
        fill: '#333333',
        fontSize: 14,
        fontWeight: 'bold',
        refX: 0.5,
        refY: 15,
        textAnchor: 'middle',
        textVerticalAnchor: 'middle'
      },
      // 字段容器
      '.fields-container': {
        refY: 30,
        fill: '#ffffff'
      },
      // 字段文本
      '.fields': {
        text: '',
        fill: '#666666',
        fontSize: 12,
        fontFamily: 'monospace',
        refX: 10,
        refY: 45,
        textAnchor: 'start',
        textVerticalAnchor: 'top',
        lineHeight: 1.2
      }
    },
    ports: {
      groups: {
        input: {
          position: 'left',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        },
        output: {
          position: 'right',
          attrs: {
            circle: {
              r: 4,
              magnet: true,
              stroke: '#5F95FF',
              strokeWidth: 1,
              fill: '#fff'
            }
          }
        }
      }
    }
  })
}

// 设置图形事件
const setupGraphEvents = () => {
  if (!graph.value) return

  // 节点选中事件
  graph.value.on('node:click', ({node}) => {
    console.log('节点点击:', node.id)
    // 显示节点属性面板
    currentNode.value = node
    showNodeProperties.value = true
  })

  // 边选中事件
  graph.value.on('edge:click', ({edge}) => {
    console.log('边点击:', edge.id)
    // 显示边属性面板
    currentEdge.value = edge
    showEdgeProperties.value = true
  })

  // 画布点击事件
  graph.value.on('blank:click', () => {
    // 取消选中
    currentNode.value = null
    currentEdge.value = null
    showNodeProperties.value = false
    showEdgeProperties.value = false
  })

  // 节点移动事件
  graph.value.on('node:moved', ({node}) => {
    console.log('节点移动:', node.id, node.getPosition())
    // 更新节点位置
    saveModelChanges()
  })

  // 连接创建事件
  graph.value.on('edge:connected', ({edge}) => {
    console.log('连接创建:', edge.id)
    // 更新关系
    saveModelChanges()
  })
}

// ... existing code ...

// 监听domainId变化
watch(() => props.domainId, (newVal, oldVal) => {
  console.log('domainId变化:', oldVal, '->', newVal)
  if (newVal) {
    // 更新domainId，无论是否是新建模型
    modelForm.domainId = newVal
    console.log('更新表单domainId:', modelForm.domainId)
  }
}, {immediate: true}) // 添加immediate: true，确保组件创建时立即执行一次

// 在第505行附近添加空值检查
const getRelationTypeLabel = (edge) => {
  // 添加空值检查，确保edge和edge.data存在
  if (!edge || !edge.data) {
    return '未知关系';
  }

  const relationType = edge.data.relationType;
  if (relationType === '1:1') {
    return '一对一';
  } else if (relationType === '1:n') {
    return '一对多';
  } else if (relationType === 'n:1') {
    return '多对一';
  } else if (relationType === 'n:m') {
    return '多对多';
  }
  return relationType || '未知关系';
}

// ... existing code ...

// 修复边缘创建时的数据初始化
const addEdge = (source, target, data = {}) => {
  // 创建默认边缘数据结构
  const defaultEdgeData = {
    relationType: '1:1', // 默认关系类型
    sourceField: '',
    targetField: '',
    // 其他必要的默认属性
  };

  // 合并默认数据和传入的数据
  const edgeData = {...defaultEdgeData, ...data};

  console.log('添加边缘，使用数据:', edgeData);

  // 使用合并后的数据创建边缘
  const edge = graph.value.addEdge({
    source,
    target,
    data: edgeData
  });

  return edge;
};

// ... existing code ...

// 修复打开抽屉前的检查
const openEdgeDrawer = (edge) => {
  if (!edge) {
    console.error('打开边缘抽屉失败: 边缘对象为空');
    return;
  }

  console.log('打开边缘抽屉，边缘数据:', edge.data);

  // 确保边缘有data对象
  if (!edge.data) {
    edge.data = {
      relationType: '1:1',
      sourceField: '',
      targetField: ''
    };
    console.warn('边缘缺少data对象，已创建默认data');
  }

  // 确保有relationType属性
  if (!edge.data.relationType) {
    edge.data.relationType = '1:1';
    console.warn('边缘缺少relationType属性，已设置默认值');
  }

  // 设置当前选中的边缘
  currentEdge.value = edge;

  // 初始化表单数据
  edgeForm.relationType = edge.data.relationType || '1:1';
  edgeForm.sourceField = edge.data.sourceField || '';
  edgeForm.targetField = edge.data.targetField || '';

  // 打开抽屉
  edgeDrawerVisible.value = true;
};

// ... existing code ...

// 修复处理边缘点击事件的函数
const handleEdgeClick = ({e, edge}) => {
  e.stopPropagation();

  // 记录日志，帮助调试
  console.log('边缘点击事件:', edge);

  // 确保edge存在且有data属性
  if (!edge || !edge.data) {
    console.error('边缘数据不完整:', edge);
    // 初始化边缘数据
    if (edge && !edge.data) {
      edge.data = {
        relationType: '1:1',
        sourceField: '',
        targetField: ''
      };
      console.warn('边缘缺少data对象，已创建默认data');
    } else {
      ElMessage.warning('无法编辑关系：数据不完整');
      return;
    }
  }

  // 打开边缘抽屉前确保数据完整
  openEdgeDrawer(edge);
};

// ... existing code ...

// 添加处理关系类型变更的方法
const handleRelationTypeChange = () => {
  try {
    console.log('关系类型变更');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      selectedNode.value.config = {};
    }

    // 如果是多对多关系，初始化中间表配置
    if (selectedNode.value.config.relationType === 'many-to-many') {
      if (!selectedNode.value.config.junctionTable) {
        selectedNode.value.config.junctionTable = {
          name: `junction_${Date.now()}`,
          description: '中间关联表',
          fields: []
        };

        console.log('已初始化中间表配置');
      }
    }

    console.log('关系类型已更新为:', selectedNode.value.config.relationType);
  } catch (error) {
    console.error('处理关系类型变更时出错:', error);
  }
}

// 修复添加中间表字段的方法
const addJunctionField = () => {
  try {
    console.log('添加中间表字段');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      selectedNode.value.config = {};
    }

    // 确保junctionTable存在
    if (!selectedNode.value.config.junctionTable) {
      console.warn('中间表配置为空，正在初始化');
      selectedNode.value.config.junctionTable = {
        name: `junction_${Date.now()}`,
        description: '中间关联表',
        fields: []
      };
    }

    // 确保fields数组存在
    if (!selectedNode.value.config.junctionTable.fields) {
      console.warn('中间表字段数组为空，正在初始化');
      selectedNode.value.config.junctionTable.fields = [];
    }

    // 添加新字段
    const newField = {
      name: `field_${selectedNode.value.config.junctionTable.fields.length + 1}`,
      type: 'varchar',
      length: 255,
      comment: '新字段'
    };

    selectedNode.value.config.junctionTable.fields.push(newField);

    console.log('已添加中间表字段:', newField);
  } catch (error) {
    console.error('添加中间表字段时出错:', error);
  }
}

// 修复移除中间表字段的方法
const removeJunctionField = (index) => {
  try {
    console.log('移除中间表字段, 索引:', index);

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      return;
    }

    // 确保junctionTable存在
    if (!selectedNode.value.config.junctionTable) {
      console.warn('中间表配置为空');
      return;
    }

    // 确保fields数组存在
    if (!selectedNode.value.config.junctionTable.fields) {
      console.warn('中间表字段数组为空');
      return;
    }

    // 检查索引是否有效
    if (index < 0 || index >= selectedNode.value.config.junctionTable.fields.length) {
      console.warn('无效的字段索引:', index);
      return;
    }

    // 移除字段
    selectedNode.value.config.junctionTable.fields.splice(index, 1);

    console.log('已移除中间表字段, 索引:', index);
  } catch (error) {
    console.error('移除中间表字段时出错:', error);
  }
}

// 修复应用关系的方法
const applyRelation = () => {
  try {
    console.log('应用关系');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      ElMessage.warning('没有选中的关系节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      ElMessage.warning('关系配置不完整');
      return;
    }

    // 验证关系配置
    if (!selectedNode.value.config.sourceTable) {
      ElMessage.warning('请选择源实体');
      return;
    }

    if (!selectedNode.value.config.targetTable) {
      ElMessage.warning('请选择目标实体');
      return;
    }

    if (!selectedNode.value.config.sourceField || selectedNode.value.config.sourceField.length === 0) {
      ElMessage.warning('请选择源字段');
      return;
    }

    if (!selectedNode.value.config.targetField || selectedNode.value.config.targetField.length === 0) {
      ElMessage.warning('请选择目标字段');
      return;
    }

    // 更新关系可视化
    updateRelationVisual();

    // 如果是一对一或一对多关系，自动在目标表添加外键
    if (selectedNode.value.config.relationType !== 'many-to-many') {
      addForeignKeyToTargetTable();
    }

    ElMessage.success('关系应用成功');
    console.log('关系已应用');
  } catch (error) {
    console.error('应用关系时出错:', error);
    ElMessage.error('应用关系失败: ' + error.message);
  }
}


// 初始化节点配置
const initNodeConfig = () => {
  return {
    relationType: '1:N',
    sourceTable: '',
    sourceField: [],
    targetTable: '',
    targetField: [],
    onDelete: 'NO ACTION',
    description: ''
  };
}

// 添加初始化中间表的方法
const initJunctionTable = () => {
  try {
    console.log('初始化中间表');

    // 确保selectedNode.value存在
    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return;
    }

    // 确保selectedNode.value.config存在
    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      initNodeConfig();
    }

    // 初始化中间表对象
    selectedNode.value.config.junctionTable = {
      name: `junction_${Date.now()}`,
      description: '中间关联表',
      fields: []
    };

    console.log('中间表已初始化');
    return selectedNode.value.config.junctionTable;
  } catch (error) {
    console.error('初始化中间表时出错:', error);
  }
}

// 添加调试日志，查看nodes数组的内容
const debugNodes = () => {
  console.log('当前nodes数组:', nodes.value);
  if (nodes.value && Array.isArray(nodes.value)) {
    console.log('nodes数组长度:', nodes.value.length);
    console.log('nodes中的实体表:');
    nodes.value.forEach((node, index) => {
      if (node && node.type === 'entity-table') {
        console.log(`实体表 ${index}:`, node.id, node.name, node.config?.fields?.length || 0);
      }
    });
  } else {
    console.warn('nodes不是数组或为空');
  }
};

// 修改entityTables计算属性，添加更多调试信息
const entityTables = computed(() => {
  try {
    console.log('计算实体表列表');
    debugNodes();

    // 确保nodes.value存在且是数组
    if (!nodes.value || !Array.isArray(nodes.value)) {
      console.warn('节点列表为空或不是数组');
      return [];
    }

    // 过滤出实体表类型的节点，并确保每个节点都不为null
    const tables = nodes.value.filter(node => {
      if (!node) {
        console.warn('发现null节点');
        return false;
      }

      if (typeof node !== 'object') {
        console.warn('节点不是对象:', node);
        return false;
      }

      const isEntityTable = node.type === 'entity-table';
      if (isEntityTable) {
        console.log('找到实体表节点:', node.id, node.name);
      }

      return isEntityTable;
    }).map(node => {
      // 确保node有name属性
      if (!node.name) {
        console.warn('节点缺少name属性:', node.id);
        node.name = `实体表 ${node.id || '未知'}`;
      }

      // 确保node有config和fields属性
      if (!node.config) {
        console.warn('节点缺少config属性:', node.id);
        node.config = {fields: []};
      }

      if (!node.config.fields) {
        console.warn('节点缺少fields属性:', node.id);
        node.config.fields = [];
      }

      return node;
    });

    console.log('找到实体表:', tables.length, tables.map(t => ({id: t.id, name: t.name})));
    return tables;
  } catch (error) {
    console.error('计算实体表列表时出错:', error);
    return [];
  }
});


// 修改sourceTableFields计算属性，添加更多调试信息
const sourceTableFields = computed(() => {
  try {
    console.log('计算源表字段');

    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return [];
    }

    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      return [];
    }

    const sourceTableId = selectedNode.value.config.sourceTable;
    console.log('源表ID:', sourceTableId);

    if (!sourceTableId) {
      console.warn('未选择源表');
      return [];
    }

    // 查找源表节点
    const sourceTable = nodes.value.find(node =>
        node && node.id === sourceTableId
    );

    if (!sourceTable) {
      console.warn('找不到源表节点:', sourceTableId);
      return [];
    }

    console.log('找到源表节点:', sourceTable.id, sourceTable.name);

    if (!sourceTable.config) {
      console.warn('源表没有config属性:', sourceTableId);
      return [];
    }

    if (!sourceTable.config.fields) {
      console.warn('源表没有fields属性:', sourceTableId);
      return [];
    }

    // 确保每个字段都有name属性
    const fields = sourceTable.config.fields.map((field, index) => {
      if (!field) {
        console.warn('发现null字段');
        return {name: `未命名字段_${index}`, type: 'varchar'};
      }

      if (!field.name) {
        console.warn('字段缺少name属性');
        field.name = `字段_${index}`;
      }

      return field;
    });

    console.log('源表字段计算:', fields.length, fields.map(f => f.name));

    // 如果没有找到字段，返回一些测试字段
    if (fields.length === 0) {
      console.warn('没有找到字段，返回测试数据');
      return [
        {name: 'id', type: 'int', isPrimary: true},
        {name: 'name', type: 'varchar'},
        {name: 'description', type: 'text'}
      ];
    }

    return fields;
  } catch (error) {
    console.error('计算源表字段时出错:', error);
    return [];
  }
});


// 修改targetTableFields计算属性，添加更多调试信息
const targetTableFields = computed(() => {
  try {
    console.log('计算目标表字段');

    if (!selectedNode.value) {
      console.warn('当前没有选中节点');
      return [];
    }

    if (!selectedNode.value.config) {
      console.warn('节点配置为空');
      return [];
    }

    const targetTableId = selectedNode.value.config.targetTable;
    console.log('目标表ID:', targetTableId);

    if (!targetTableId) {
      console.warn('未选择目标表');
      return [];
    }

    // 查找目标表节点
    const targetTable = nodes.value.find(node =>
        node && node.id === targetTableId
    );

    if (!targetTable) {
      console.warn('找不到目标表节点:', targetTableId);
      return [];
    }

    console.log('找到目标表节点:', targetTable.id, targetTable.name);

    if (!targetTable.config) {
      console.warn('目标表没有config属性:', targetTableId);
      return [];
    }

    if (!targetTable.config.fields) {
      console.warn('目标表没有fields属性:', targetTableId);
      return [];
    }

    // 确保每个字段都有name属性
    const fields = targetTable.config.fields.map((field, index) => {
      if (!field) {
        console.warn('发现null字段');
        return {name: `未命名字段_${index}`, type: 'varchar'};
      }

      if (!field.name) {
        console.warn('字段缺少name属性');
        field.name = `字段_${index}`;
      }

      return field;
    });

    console.log('目标表字段计算:', fields.length, fields.map(f => f.name));

    // 如果没有找到字段，返回一些测试字段
    if (fields.length === 0) {
      console.warn('没有找到字段，返回测试数据');
      return [
        {name: 'id', type: 'int', isPrimary: true},
        {name: 'code', type: 'varchar'},
        {name: 'value', type: 'decimal'}
      ];
    }

    return fields;
  } catch (error) {
    console.error('计算目标表字段时出错:', error);
    return [];
  }
});

// 在组件挂载时调试nodes数组
onMounted(() => {
  // ... 现有代码 ...

  // 添加延迟调试，确保nodes数组已加载
  setTimeout(() => {
    console.log('组件挂载后调试nodes数组:');
    debugNodes();
  }, 1000);
});

// 添加监听器，当selectedNode变化时，记录日志
watch(selectedNode, (newNode) => {
  console.log('选中节点变化:', newNode?.id, newNode?.type);
  if (newNode && newNode.type === 'relation') {
    console.log('关系节点配置:', newNode.config);
  }
}, {deep: true});

// ... existing code ...

// 添加forceRefreshEntityTables方法
const forceRefreshEntityTables = () => {
  console.log('强制刷新entityTables');

  // 方法1: 使用nextTick
  nextTick(() => {
    console.log('nextTick后的entityTables:', entityTables.value);
  });

  // 方法2: 直接修改nodes数组触发响应式更新
  if (nodes.value && nodes.value.length > 0) {
    console.log('通过修改nodes数组触发更新');
    nodes.value = [...nodes.value];
  }

  // 方法3: 直接调用getEntityTables并打印结果
  const tables = getEntityTables();
  console.log('直接调用getEntityTables的结果:', tables);
};

// 添加refreshDropdowns方法
const refreshDropdowns = () => {
  console.log('刷新下拉菜单');

  // 强制重新计算entityTables
  const tables = getEntityTables();
  console.log('重新获取的实体表:', tables);

  // 检查是否有选中的节点，并且是关系类型
  const relationshipConfig = selectedNode.value && selectedNode.value.type === 'relationship'
      ? selectedNode.value
      : null;

  // 如果有关系节点被选中，刷新其下拉选项
  if (relationshipConfig) {
    console.log('刷新关系配置的下拉选项:', relationshipConfig);

    // 刷新源表字段
    if (relationshipConfig.sourceId) {
      const sourceFields = getSourceTableFields();
      console.log('刷新后的源表字段:', sourceFields);
    }

    // 刷新目标表字段
    if (relationshipConfig.targetId) {
      const targetFields = getTargetTableFields();
      console.log('刷新后的目标表字段:', targetFields);
    }
  } else {
    console.log('没有选中关系节点，跳过刷新关系下拉选项');
  }

  // 强制组件重新渲染
  nextTick(() => {
    console.log('下一个tick后的entityTables:', entityTables.value);

    // 再次打印nodes数组，确认名称是否已更新
    console.log('下一个tick后的nodes数组:', JSON.stringify(nodes.value));
  });
};

// 添加getEntityTables方法（如果不存在）
const getEntityTables = () => {
  console.log('获取实体表列表, nodes长度:', nodes.value?.length);

  // 过滤出实体表节点
  const tables = nodes.value
      ?.filter(node => {
        const isEntityTable = node && node.type === 'entity-table';
        if (isEntityTable) {
          console.log('找到实体表节点:', node.id, node.name);
        }
        return isEntityTable;
      })
      ?.map(node => ({
        id: node.id,
        name: node.name || '未命名表',
        fields: node.config?.fields || []
      })) || [];

  console.log('获取到的实体表:', tables);
  return tables;
};

// 获取实体表字段
const getEntityTableFields = () => {
  // 获取所有实体表节点
  const entityTables = nodes.value.filter(node => node.type === 'entity-table');

  // 提取字段信息
  const fieldsData = {};
  entityTables.forEach(table => {
    if (table.config && table.config.fields) {
      fieldsData[table.id] = table.config.fields;
    }
  });

  return fieldsData;
};

// 获取实体表索引
const getEntityTableIndexes = () => {
  // 获取所有实体表节点
  const entityTables = nodes.value.filter(node => node.type === 'entity-table');

  // 提取索引信息
  const indexesData = {};
  entityTables.forEach(table => {
    if (table.config && table.config.indexes) {
      indexesData[table.id] = table.config.indexes;
    }
  });

  return indexesData;
};

// 获取关系信息
const getRelationships = () => {
  // 获取所有关系节点
  const relationships = nodes.value.filter(node => node.type === 'relationship');

  // 提取关系信息
  const relationsData = {};
  relationships.forEach(relation => {
    if (relation.sourceId && relation.targetId) {
      relationsData[relation.id] = {
        sourceId: relation.sourceId,
        targetId: relation.targetId,
        sourceField: relation.sourceField || '',
        targetField: relation.targetField || '',
        relationType: relation.relationType || '1:N'
      };
    }
  });

  // 获取边缘（直接连接）的关系
  const edges = getEdges();
  edges.forEach(edge => {
    if (edge.source && edge.target && edge.data) {
      relationsData[edge.id] = {
        sourceId: edge.source,
        targetId: edge.target,
        sourceField: edge.data.sourceField || '',
        targetField: edge.data.targetField || '',
        relationType: edge.data.relationType || '1:N'
      };
    }
  });

  return relationsData;
};

// 获取边缘数据
const getEdges = () => {
  if (!graph.value) return [];

  // 获取所有边缘
  const edges = graph.value.getEdges();

  // 转换为简单对象
  return edges.map(edge => {
    const data = edge.getData() || {};
    return {
      id: edge.id,
      source: edge.getSourceCellId(),
      target: edge.getTargetCellId(),
      data: {
        sourceField: data.sourceField || '',
        targetField: data.targetField || '',
        relationType: data.relationType || '1:N'
      }
    };
  });
};

// 添加获取模型标准关联的方法
const getModelStandards = () => {
  console.log('获取模型标准关联');

  // 收集所有标准关联
  const standards = [];

  try {
    // 获取所有实体表节点
    const entityTables = nodes.value.filter(node => node.type === 'entity-table');
    console.log('找到实体表数量:', entityTables.length);

    // 处理实体表字段的标准关联
    entityTables.forEach(table => {
      if (table.config && table.config.fields && Array.isArray(table.config.fields)) {
        table.config.fields.forEach(field => {
          // 如果字段有关联的标准
          if (field.standards && Array.isArray(field.standards)) {
            field.standards.forEach(standard => {
              standards.push({
                standardId: standard.id,
                standardType: standard.type || 'field',
                fieldId: field.name,
                tableId: table.id
              });
            });
          }
        });
      }
    });

    // 获取所有关系节点
    const relationNodes = nodes.value.filter(node => node.type === 'relationship');
    console.log('找到关系节点数量:', relationNodes.length);

    // 详细记录每个关系节点的信息，帮助调试
    relationNodes.forEach((relation, index) => {
      console.log(`关系节点 ${index + 1}:`, {
        id: relation.id,
        config: relation.config,
        hasSourceTable: relation.config?.sourceTable ? true : false,
        hasTargetTable: relation.config?.targetTable ? true : false,
        hasStandards: relation.config?.standards ? true : false
      });
    });

    // 处理关系节点的标准关联
    relationNodes.forEach(relation => {
      if (relation.config) {
        const sourceId = relation.config.sourceTable;
        const targetId = relation.config.targetTable;

        if (sourceId && targetId) {
          // 如果关系配置有标准属性
          if (relation.config.standards && Array.isArray(relation.config.standards)) {
            relation.config.standards.forEach(standard => {
              standards.push({
                standardId: standard.id,
                standardType: 'relationship',
                relationId: relation.id,
                sourceId: sourceId,
                targetId: targetId
              });
            });
          } else {
            // 如果没有明确的standards属性，但有关系类型，创建一个隐含的标准关联
            // 这确保即使没有显式设置standards，关系信息也会被保存
            if (relation.config.relationType) {
              standards.push({
                standardId: `implicit-${relation.id}`,
                standardType: 'relationship',
                relationId: relation.id,
                sourceId: sourceId,
                targetId: targetId,
                relationType: relation.config.relationType,
                sourceField: relation.config.sourceField,
                targetField: relation.config.targetField,
                implicit: true // 标记为隐含的标准关联
              });
              console.log(`为关系 ${relation.id} 创建了隐含的标准关联`);
            }
          }
        }
      }
    });

    // 如果图形存在，获取所有边缘
    if (graph.value) {
      const edges = graph.value.getEdges();
      console.log('找到边缘数量:', edges.length);

      // 详细记录每个边的信息，帮助调试
      edges.forEach((edge, index) => {
        console.log(`边 ${index + 1}:`, {
          id: edge.id,
          sourceId: edge.getSourceCellId(),
          targetId: edge.getTargetCellId(),
          data: edge.getData()
        });
      });

      // 处理边缘的标准关联
      edges.forEach(edge => {
        const edgeData = edge.getData() || {};
        const sourceId = edge.getSourceCellId();
        const targetId = edge.getTargetCellId();

        if (sourceId && targetId) {
          // 首先检查是否有标准关联信息
          if (edgeData.standards && Array.isArray(edgeData.standards)) {
            edgeData.standards.forEach(standard => {
              standards.push({
                standardId: standard.id,
                standardType: 'edge',
                edgeId: edge.id,
                sourceId: sourceId,
                targetId: targetId
              });
            });
          } else {
            // 检查这条边是否连接了关系节点
            const sourceNode = nodes.value.find(node => node.id === sourceId);
            const targetNode = nodes.value.find(node => node.id === targetId);

            // 如果边的一端是关系节点，另一端是实体表，创建隐含的标准关联
            if ((sourceNode?.type === 'relationship' && targetNode?.type === 'entity-table') ||
                (sourceNode?.type === 'entity-table' && targetNode?.type === 'relationship')) {
              const relationNode = sourceNode?.type === 'relationship' ? sourceNode : targetNode;

              if (relationNode && relationNode.config) {
                standards.push({
                  standardId: `edge-implicit-${edge.id}`,
                  standardType: 'edge',
                  edgeId: edge.id,
                  sourceId: sourceId,
                  targetId: targetId,
                  relationId: relationNode.id,
                  implicit: true // 标记为隐含的标准关联
                });
                console.log(`为边 ${edge.id} 创建了隐含的标准关联`);
              }
            }
            // 如果边直接连接两个实体表（没有中间的关系节点）
            else if (sourceNode?.type === 'entity-table' && targetNode?.type === 'entity-table') {
              // 从边的数据中提取关系信息
              const relationType = edgeData.relationType || '1:N';
              const sourceField = edgeData.sourceField || '';
              const targetField = edgeData.targetField || '';

              // 如果边的标签中包含关系信息，也可以从标签中提取
              const labels = edge.getLabels() || [];
              let labelInfo = '';
              if (labels.length > 0) {
                const firstLabel = labels[0];
                if (firstLabel && firstLabel.attrs && firstLabel.attrs.text) {
                  labelInfo = firstLabel.attrs.text.text || '';
                }
              }

              // 创建一个表示直接关系的标准关联
              standards.push({
                standardId: `direct-relation-${edge.id}`,
                standardType: 'direct-relation',
                edgeId: edge.id,
                sourceId: sourceId,
                targetId: targetId,
                relationType: relationType,
                sourceField: sourceField,
                targetField: targetField,
                labelInfo: labelInfo,
                implicit: true // 标记为隐含的标准关联
              });
              console.log(`为直接连接的边 ${edge.id} 创建了关系标准关联`);
            }
          }
        }
      });

      // 如果没有找到任何关系节点，但有边连接实体表，从边中提取关系信息
      if (relationNodes.length === 0 && edges.length > 0) {
        console.log('没有找到关系节点，尝试从边中提取关系信息');

        // 获取所有直接连接实体表的边
        const entityConnections = [];

        edges.forEach(edge => {
          const sourceId = edge.getSourceCellId();
          const targetId = edge.getTargetCellId();
          const sourceNode = nodes.value.find(node => node.id === sourceId);
          const targetNode = nodes.value.find(node => node.id === targetId);

          // 如果边连接的是两个实体表
          if (sourceNode?.type === 'entity-table' && targetNode?.type === 'entity-table') {
            const edgeData = edge.getData() || {};

            // 从边的数据或标签中提取关系信息
            let relationInfo = {
              sourceId: sourceId,
              targetId: targetId,
              relationType: edgeData.relationType || '1:N',
              sourceField: edgeData.sourceField || '',
              targetField: edgeData.targetField || ''
            };

            // 如果边有标签，可能包含关系信息
            const labels = edge.getLabels() || [];
            if (labels.length > 0) {
              const firstLabel = labels[0];
              if (firstLabel && firstLabel.attrs && firstLabel.attrs.text) {
                relationInfo.labelInfo = firstLabel.attrs.text.text || '';
              }
            }

            entityConnections.push({
              edgeId: edge.id,
              ...relationInfo
            });
          }
        });

        console.log('从边中提取的实体关系:', entityConnections);

        // 为每个实体连接创建标准关联
        entityConnections.forEach(connection => {
          standards.push({
            standardId: `extracted-relation-${connection.edgeId}`,
            standardType: 'extracted-relation',
            edgeId: connection.edgeId,
            sourceId: connection.sourceId,
            targetId: connection.targetId,
            relationType: connection.relationType,
            sourceField: connection.sourceField,
            targetField: connection.targetField,
            labelInfo: connection.labelInfo,
            implicit: true // 标记为提取的关系
          });
        });
      }
    }
  } catch (error) {
    console.error('获取模型标准关联时出错:', error);
  }

  console.log('找到标准关联数量:', standards.length);
  console.log('标准关联详情:', standards);
  return standards;
};

// ... existing code ...
</script>

<style lang="scss" scoped>


// 调整样式
:deep(.property-drawer) {
  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #dcdfe6;

    .el-drawer__title {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
    }
  }

  .el-drawer__body {
    padding: 0;
    overflow: visible;
  }
}

.property-panel {
  width: 100%;
  height: 100%;
  background: #fff;
  overflow-y: auto;
  padding: 20px;

  .panel-content {
    padding: 0 0 20px 0;
  }

  .panel-placeholder {
    height: 100%;
    min-height: 400px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #909399;

    :deep(.el-empty__description) {
      margin-top: 16px;

      p {
        margin: 0;
        font-size: 16px;
        color: #606266;
        line-height: 1.5;
      }
    }

    :deep(.el-empty__bottom) {
      margin-top: 24px;
      display: flex;
      gap: 10px;
    }
  }
}

.model-designer {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
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
}

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

.canvas-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  min-height: 650px;

  .canvas-placeholder {
    position: absolute;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    color: #909399;
    font-size: 14px;
    text-align: center;
    width: 100%;
    max-width: 400px;
    padding: 20px;
    background: rgba(255, 255, 255, 0.8);
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .model-info-panel {
    position: absolute;
    top: 20px;
    right: 20px;
    width: 250px;
    background: rgba(255, 255, 255, 0.9);
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    padding: 16px;
    z-index: 100;

    .model-actions {
      display: flex;
      justify-content: space-between;
      margin-top: 10px;
    }
  }

  .property-panel-hint {
    position: absolute;
    top: 50%;
    right: 0;
    transform: translateY(-50%);
    background: rgba(64, 158, 255, 0.9);
    color: #fff;
    padding: 10px 15px;
    border-radius: 4px 0 0 4px;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 5px;
    box-shadow: -2px 0 8px rgba(0, 0, 0, 0.1);
    transition: all 0.3s;

    &:hover {
      background: rgba(64, 158, 255, 1);
      padding-right: 20px;
    }

    .el-icon {
      font-size: 16px;
    }

    span {
      font-size: 14px;
      white-space: nowrap;
    }
  }
}

:deep(.property-drawer) {
  .el-drawer__header {
    margin-bottom: 0;
    padding: 16px 20px;
    border-bottom: 1px solid #dcdfe6;

    .el-drawer__title {
      font-size: 16px;
      font-weight: 500;
      color: #303133;
    }
  }

  .el-drawer__body {
    padding: 0;
    overflow: visible;
  }
}

.property-panel {
  width: 100%;
  height: 100%;
  background: #fff;
  overflow-y: auto;
  padding: 20px;

  .panel-content {
    padding: 0 0 20px 0;
  }

  .panel-placeholder {
    height: 100%;
    min-height: 400px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #909399;

    :deep(.el-empty__description) {
      margin-top: 16px;

      p {
        margin: 0;
        font-size: 16px;
        color: #606266;
        line-height: 1.5;
      }
    }

    :deep(.el-empty__bottom) {
      margin-top: 24px;
      display: flex;
      gap: 10px;
    }
  }
}

:deep(.el-tabs__content) {
  padding: 10px;
}

.empty-standards {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.standards-result {
  padding: 20px 0;
}

.target-node-info {
  margin-top: 8px;
  padding: 8px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

:deep(.el-collapse-item__header) {
  padding: 0 8px;
}

:deep(.el-collapse-item__content) {
  padding: 10px;
}

:deep(.el-divider__text) {
  background-color: #fff;
  font-size: 14px;
  color: #606266;
}

.mode-switch {
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
  text-align: center;
}

.sql-preview-content {
  .sql-code {
    background: #f5f7fa;
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    padding: 16px;
    max-height: 400px;
    overflow: auto;
    font-family: 'Courier New', Courier, monospace;
    white-space: pre-wrap;
    word-break: break-all;
  }

  .table-structure-preview {
    height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
}

.upload-model {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    height: 200px;
  }
}

.mb-4 {
  margin-bottom: 16px;
}

.sql-orchestration {
  padding: 20px;
  background: #f5f7fa;
  min-height: calc(100vh - 120px);

  .operation-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding: 16px;
    background: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .left-operations {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .right-operations {
      display: flex;
      gap: 16px;
      align-items: center;

      .el-input {
        width: 300px;
      }
    }
  }

  .model-overview {
    margin-bottom: 20px;

    .overview-card {
      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
      }

      .card-value {
        font-size: 28px;
        font-weight: bold;
        margin: 16px 0;
      }

      .card-trend {
        font-size: 14px;
        color: #909399;

        .up {
          color: #67c23a;
        }

        .down {
          color: #f56c6c;
        }
      }

      &.total {
        border-left: 4px solid #409eff;
      }

      &.running {
        border-left: 4px solid #67c23a;
      }

      &.pending {
        border-left: 4px solid #e6a23c;
      }

      &.error {
        border-left: 4px solid #f56c6c;
      }
    }
  }

  .model-list {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

    .model-name {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        color: #409eff;
      }
    }

    .status-cell {
      display: flex;
      align-items: center;
      gap: 4px;
    }

    .model-detail {
      padding: 20px;
      background: #f5f7fa;
    }
  }

  .pagination-container {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

/* 实体表节点样式 */
:deep(.entity-table-node) {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  overflow: hidden;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);

  .node-header {
    background-color: #5F95FF;
    color: white;
    padding: 8px 10px;
    font-weight: bold;
    font-size: 14px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .node-content {
    background-color: white;
    padding: 8px;
    overflow-y: auto;
    max-height: calc(100% - 36px);
  }

  .node-field {
    padding: 3px 0;
    font-size: 12px;
    display: flex;
    align-items: center;

    .field-icon {
      margin-right: 4px;
    }

    .field-text {
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

/* 关系节点样式 */
:deep(.relation-node) {
  width: 100%;
  height: 100%;
  background-color: #f5f5f5;
  border: 1px solid #5F95FF;
  border-radius: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: #333;
  position: relative;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);

  .relation-type-icon {
    position: absolute;
    top: -10px;
    right: -10px;
    background-color: #5F95FF;
    color: white;
    border-radius: 50%;
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    font-weight: bold;
  }
}

/* 列表视图样式 */
.list-container {
  width: 100%;
  height: 100%;
  padding: 16px;
  overflow-y: auto;
  background-color: #f5f7fa;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list-header h3 {
  margin: 0;
  font-size: 18px;
  color: #303133;
  font-weight: 600;
}

/* 表格样式优化 */
:deep(.el-table) {
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 20px;
}

:deep(.el-table__header-wrapper) {
  background-color: #f2f6fc;
}

:deep(.el-table__row) {
  cursor: pointer;
  transition: background-color 0.3s;
}

:deep(.el-table__row:hover) {
  background-color: #ecf5ff !important;
}

/* 视图切换按钮样式 */
.designer-toolbar .el-button-group {
  margin-right: 16px;
}

/* 适应不同视图模式的容器样式 */
.canvas-container,
.list-container {
  width: 100%;
  height: 100%;
  transition: opacity 0.3s;
}

/* 空状态提示样式 */
:deep(.el-empty) {
  padding: 20px 0;
}

/* 标签样式 */
:deep(.el-tag) {
  margin-right: 5px;
}

/* 表格内操作按钮样式 */
:deep(.el-button--link) {
  padding: 4px 8px;
}

.view-mode-switch {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  background: #fff;
  border-bottom: 1px solid #dcdfe6;
}

.list-view-container {
  flex: 1;
  min-height: 0;
  display: flex;
  overflow: hidden;
}

.list-view-container .list-container {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

// 添加字段编辑相关样式
.fields-container {
  margin-bottom: 20px;

  .field-item {
    margin-bottom: 10px;
    padding: 10px;
    border-radius: 4px;
    background-color: #f8f9fa;
  }

  .add-field-btn {
    margin-top: 15px;
    text-align: center;
  }
}

// 确保对话框在最上层
:deep(.el-dialog) {
  display: flex;
  flex-direction: column;
  margin: 0 !important;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  max-height: calc(100% - 30px);
  max-width: calc(100% - 30px);
}

:deep(.el-dialog .el-dialog__body) {
  overflow-y: auto;
}

// 添加索引项样式
.index-item {
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  margin-bottom: 15px;
  position: relative;

  .el-button--danger {
    position: absolute;
    top: 10px;
    right: 10px;
  }
}

.empty-tip {
  color: #909399;
  text-align: center;
  padding: 20px 0;
  font-size: 14px;
}

.field-actions, .index-actions {
  display: flex;
  justify-content: center;
  margin-top: 15px;
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 10px 20px;
  background-color: #fff;
  border-top: 1px solid #e4e7ed;
  text-align: right;
  z-index: 1;
}

// 为了给底部按钮留出空间
:deep(.el-drawer__body) {
  padding-bottom: 60px;
}
</style> 