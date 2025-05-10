<template>
  <div class="model-list">
    <!-- 主内容区域 -->
    <div class="main-content">
      <!-- 左侧树形导航 -->
      <el-card class="tree-card">
        <template #header>
          <div class="tree-header">
            <span>模型列表</span>
            <div class="tree-tools">
              <el-tooltip content="回到首页" placement="top">
                <el-button link @click="goHome">
                  <el-icon>
                    <HomeFilled/>
                  </el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="刷新" placement="top">
                <el-button link @click="refreshTree">
                  <el-icon>
                    <Refresh/>
                  </el-icon>
                </el-button>
              </el-tooltip>
            </div>
          </div>
        </template>

        <el-input
            v-model="treeFilter"
            placeholder="输入关键字进行过滤"
            clearable
            class="tree-filter"
        >
          <template #prefix>
            <el-icon>
              <Search/>
            </el-icon>
          </template>
        </el-input>

        <el-tree
            ref="modelTreeRef"
            :data="treeData"
            :props="treeProps"
            :filter-node-method="filterNode"
            :expand-on-click-node="false"
            node-key="id"
            highlight-current
            @node-click="handleNodeClick"
        >
          <template #default="{ node, data }">
            <div class="custom-tree-node">
              <el-icon :class="getModelTypeIcon(data.type)">
                <component :is="getModelTypeIcon(data.type)"/>
              </el-icon>
              <span>{{ node.label }}</span>
              <el-tag v-if="data.status" size="small" :type="getModelStatusTag(data.status)">
                {{ getModelStatusName(data.status) }}
              </el-tag>
            </div>
          </template>
        </el-tree>
      </el-card>

      <!-- 右侧内容区域 -->
      <div class="content-area">
        <!-- 操作工具栏 -->
        <el-card class="search-card" shadow="hover">
          <div slot="header">
            <span>表搜索</span>
            <el-button
                style="float: right; padding: 3px 0"
                type="text"
                @click="showAdvanced = !showAdvanced">
              {{ showAdvanced ? '收起' : '展开' }}高级搜索
              <i :class="showAdvanced ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
            </el-button>
          </div>

          <!-- 基本搜索 -->
          <el-form :inline="true" :model="queryParams" size="small">
            <el-form-item label="表名">
              <el-input v-model="queryParams.name" placeholder="请输入表名" clearable/>
            </el-form-item>
            <el-form-item label="显示名称">
              <el-input v-model="queryParams.displayName" placeholder="请输入显示名称" clearable/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
              <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
            </el-form-item>
          </el-form>

          <!-- 高级搜索 -->
          <div v-show="showAdvanced" style="margin-top: 15px; border-top: 1px dashed #ccc; padding-top: 15px;">
            <el-form :model="queryParams" label-width="100px" size="small">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="创建时间">
                    <el-date-picker
                        v-model="dateRange.createTime"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        value-format="yyyy-MM-dd"
                        style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="更新时间">
                    <el-date-picker
                        v-model="dateRange.updateTime"
                        type="daterange"
                        range-separator="至"
                        start-placeholder="开始日期"
                        end-placeholder="结束日期"
                        value-format="yyyy-MM-dd"
                        style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="字段数量">
                    <el-col :span="11">
                      <el-input-number v-model="queryParams.minFieldCount" placeholder="最小值" :min="0"/>
                    </el-col>
                    <el-col :span="2" style="text-align: center;">-</el-col>
                    <el-col :span="11">
                      <el-input-number v-model="queryParams.maxFieldCount" placeholder="最大值" :min="0"/>
                    </el-col>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </div>
        </el-card>
        <div class="toolbar">
          <div class="left">
            <el-button type="primary" @click="handleCreateModel">
              <el-icon>
                <Plus/>
              </el-icon>
              新建模型
            </el-button>
            <el-dropdown split-button type="primary" @click="handleAdd" @command="handleCommand" v-if="showTableList">
              <template #default>
                <el-icon>
                  <Plus/>
                </el-icon>
                新建表
              </template>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="sqlCreate">
                    <el-icon>
                      <Document/>
                    </el-icon>
                    SQL新建表
                  </el-dropdown-item>
                  <el-dropdown-item command="refCreate">
                    <el-icon>
                      <Share/>
                    </el-icon>
                    引用已有表
                  </el-dropdown-item>
                  <el-dropdown-item command="fileCreate">
                    <el-icon>
                      <Upload/>
                    </el-icon>
                    文件新建表
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="success" @click="standardsDialogVisible = true" v-if="showTableList">
              <el-icon>
                <Collection/>
              </el-icon>
              应用标准
            </el-button>
            <el-button type="success" @click="handleImport">
              <el-icon>
                <Upload/>
              </el-icon>
              导入
            </el-button>
            <el-button type="warning" @click="handleExport">
              <el-icon>
                <Download/>
              </el-icon>
              导出
            </el-button>
          </div>
          <div class="right">
            <el-tooltip content="刷新列表" placement="top">
              <el-button circle @click="refreshList">
                <el-icon>
                  <Refresh/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="密度" placement="top">
              <el-button circle @click="toggleDensity">
                <el-icon>
                  <Grid/>
                </el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="列设置" placement="top">
              <el-button circle @click="showColumnSettings = true">
                <el-icon>
                  <Setting/>
                </el-icon>
              </el-button>
            </el-tooltip>
          </div>
        </div>


        <!-- 应用数据标准对话框 -->
        <el-dialog
            v-model="standardsDialogVisible"
            title="应用数据标准"
            width="70%"
            destroy-on-close
        >


          <el-tabs v-model="standardsActiveTab" @tab-change="handleTabChange">
            <!-- 新增：已应用标准标签页 -->
            <el-tab-pane label="已应用标准" name="applied">
              <div v-if="appliedRequiredStandards.length === 0 && appliedSuggestedStandards.length === 0"
                   class="empty-data">
                <el-empty description="暂无已应用的标准"/>
              </div>
              <div v-else>
                <!-- 必须执行的标准 -->
                <div v-if="appliedRequiredStandards.length > 0" class="standards-section">
                  <h3 class="section-title">必须执行的标准</h3>
                  <el-table :data="appliedRequiredStandards" border style="width: 100%">
                    <el-table-column prop="name" label="标准名称" min-width="150"/>
                    <el-table-column prop="code" label="标准编码" min-width="100"/>
                    <el-table-column prop="type" label="标准类型" min-width="100">
                      <template #default="scope">
                        {{ getStandardTypeText(scope.row.type) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column prop="applyTime" label="应用时间" min-width="150">
                      <template #default="scope">
                        {{ formatDate(scope.row.applyTime) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>

                <!-- 建议执行的标准 -->
                <div v-if="appliedSuggestedStandards.length > 0" class="standards-section">
                  <h3 class="section-title">建议执行的标准</h3>
                  <el-table :data="appliedSuggestedStandards" border style="width: 100%">
                    <el-table-column prop="name" label="标准名称" min-width="150"/>
                    <el-table-column prop="code" label="标准编码" min-width="100"/>
                    <el-table-column prop="type" label="标准类型" min-width="100">
                      <template #default="scope">
                        {{ getStandardTypeText(scope.row.type) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column prop="applyTime" label="应用时间" min-width="150">
                      <template #default="scope">
                        {{ formatDate(scope.row.applyTime) }}
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
              </div>
            </el-tab-pane>

            <!-- 原有的标签页 -->
            <el-tab-pane label="选择标准" name="select">
              <div class="standards-section">
                <!-- 必须执行的标准 -->
                <div class="standards-group">
                  <h4>必须执行的标准 ({{ totalRequiredStandards }})
                    <el-tag type="danger" size="small">必选</el-tag>
                    <el-button size="small" type="primary" @click="handleAddRequiredStandard"
                               style="margin-left: 10px;">
                      <el-icon>
                        <Plus/>
                      </el-icon>
                      新增
                    </el-button>
                  </h4>
                  <el-table
                      :data="requiredStandards"
                      style="width: 100%"
                      border
                  >
                    <el-table-column type="index" width="50" label="#"/>
                    <el-table-column prop="name" label="标准名称" min-width="150"/>
                    <el-table-column label="规则类型" min-width="120">
                      <template #default="{ row }">
                        {{ getStandardTypeText(row.ruleType || row.type) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column label="操作" width="120" align="center">
                      <template #default="{ row }">
                        <el-button
                            type="primary"
                            size="small"
                            @click="viewStandardDetails(row)"
                        >
                          查看详情
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div v-if="requiredStandards.length === 0 && !loading" class="empty-tip">
                    暂无必须执行的标准
                  </div>
                </div>

                <!-- 建议执行的标准 -->
                <div class="standards-group">
                  <h4>建议执行的标准 ({{ totalSuggestedStandards }})
                    <el-tag type="success" size="small">可选</el-tag>
                    <el-button size="small" type="primary" @click="handleAddSuggestedStandard"
                               style="margin-left: 10px;">
                      <el-icon>
                        <Plus/>
                      </el-icon>
                      新增
                    </el-button>
                  </h4>
                  <el-table
                      ref="suggestedStandardsTable"
                      :data="suggestedStandards"
                      style="width: 100%"
                      border
                      @selection-change="handleSuggestedStandardsSelectionChange"
                      row-key="id"
                  >
                    <el-table-column type="selection" width="55" :reserve-selection="true"/>
                    <el-table-column type="index" width="50" label="#"/>
                    <el-table-column prop="name" label="标准名称" min-width="150"/>
                    <el-table-column label="规则类型" min-width="120">
                      <template #default="{ row }">
                        {{ getStandardTypeText(row.ruleType || row.type) }}
                      </template>
                    </el-table-column>
                    <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                    <el-table-column label="操作" width="120" align="center">
                      <template #default="{ row }">
                        <el-button
                            type="primary"
                            size="small"
                            @click="viewStandardDetails(row)"
                        >
                          查看详情
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                  <div v-if="suggestedStandards.length === 0 && !loading" class="empty-tip">
                    暂无建议执行的标准
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="standards-actions">
                  <el-button
                      type="primary"
                      @click="goToNextStep"
                      :disabled="getTotalSelectedStandards() === 0"
                  >
                    下一步: 配置应用 ({{ getTotalSelectedStandards() }})
                  </el-button>
                  <el-button @click="clearSelectedSuggestedStandards">
                    清空选择
                  </el-button>
                </div>
              </div>

              <div class="dialog-footer" style="margin-top: 20px; text-align: right;">
                <el-button @click="standardsDialogVisible = false">取消</el-button>
                <el-button type="primary" @click="standardsActiveTab = 'configure'">下一步</el-button>
              </div>
            </el-tab-pane>

            <!-- 配置标准标签页 -->
            <el-tab-pane label="配置标准" name="configure">
              <div class="standards-section">
                <h3>配置选中的标准</h3>

                <!-- 显示选中的标准列表 -->
                <div class="selected-standards">
                  <h4>已选择的标准 ({{ getTotalSelectedStandards() }})</h4>

                  <!-- 必须执行的标准 -->
                  <div v-if="requiredStandards.length > 0">
                    <h5>必须执行的标准 ({{ requiredStandards.length }})
                      <el-tag type="danger" size="small">必选</el-tag>
                    </h5>
                    <el-table
                        :data="requiredStandards"
                        style="width: 100%; margin-bottom: 20px;"
                        border
                    >
                      <el-table-column type="index" width="50" label="#"/>
                      <el-table-column prop="name" label="标准名称" min-width="150"/>
                      <el-table-column label="规则类型" min-width="120">
                        <template #default="{ row }">
                          {{ getStandardTypeText(row.ruleType || row.type) }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                      <el-table-column label="操作" width="120" align="center">
                        <template #default="{ row }">
                          <el-button
                              type="primary"
                              size="small"
                              @click="viewStandardDetails(row)"
                          >
                            查看详情
                          </el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>

                  <!-- 选中的建议执行标准 -->
                  <div v-if="selectedSuggestedStandards.length > 0">
                    <h5>已选择的建议执行标准 ({{ selectedSuggestedStandards.length }})
                      <el-tag type="success" size="small">可选</el-tag>
                    </h5>
                    <el-table
                        :data="getSelectedSuggestedStandardsObjects()"
                        style="width: 100%"
                        border
                    >
                      <el-table-column type="index" width="50" label="#"/>
                      <el-table-column prop="name" label="标准名称" min-width="150"/>
                      <el-table-column label="规则类型" min-width="120">
                        <template #default="{ row }">
                          {{ getStandardTypeText(row.ruleType || row.type) }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
                      <el-table-column label="操作" width="120" align="center">
                        <template #default="{ row }">
                          <el-button
                              type="danger"
                              size="small"
                              @click="removeSelectedSuggestedStandard(row)"
                          >
                            移除
                          </el-button>
                        </template>
                      </el-table-column>
                    </el-table>
                  </div>

                  <div v-if="getTotalSelectedStandards() === 0" class="empty-tip">
                    暂未选择任何标准，请返回上一步选择
                  </div>
                </div>

                <!-- 标准应用配置 -->
                <div class="standards-config" v-if="getTotalSelectedStandards() > 0">
                  <h4>应用配置</h4>
                  <el-form label-width="120px">
                    <el-form-item label="应用范围">
                      <el-radio-group v-model="applyScope">
                        <el-radio label="all">应用到所有字段</el-radio>
                        <el-radio label="selected">仅应用到选中字段</el-radio>
                      </el-radio-group>
                      <div class="form-item-help">
                        <small>选择应用范围：应用到所有字段或仅应用到选中字段</small>
                      </div>
                    </el-form-item>

                    <el-form-item label="应用说明">
                      <el-input
                          v-model="applyDescription"
                          type="textarea"
                          :rows="3"
                          placeholder="请输入应用说明"
                      />
                      <div class="form-item-help">
                        <small>可选：添加关于此次应用标准的说明</small>
                      </div>
                    </el-form-item>
                  </el-form>

                  <!-- 显示当前选择的应用范围（调试用） -->
                  <div class="current-selection">
                    <p>当前选择的应用范围: <strong>{{
                        applyScope === 'all' ? '应用到所有字段' : '仅应用到选中字段'
                      }}</strong></p>
                  </div>
                </div>

                <!-- 操作按钮 -->
                <div class="standards-actions">
                  <el-button
                      type="primary"
                      @click="applySelectedStandards"
                      :disabled="getTotalSelectedStandards() === 0"
                  >
                    应用标准
                  </el-button>
                  <el-button @click="standardsActiveTab = 'select'">
                    返回选择标准
                  </el-button>
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

            <!-- 新增的标准验证标签页 -->
            <el-tab-pane label="标准验证" name="validation">
              <div class="validation-container">
                <div class="validation-header">
                  <h3>标准验证结果</h3>
                  <el-button type="primary" size="small" @click="validateAppliedStandards">
                    开始验证
                  </el-button>
                </div>

                <!-- 显示已应用的标准摘要 -->
                <div v-if="hasAppliedStandards && appliedStandards" class="applied-standards-summary">
                  <el-alert
                      title="已应用的标准"
                      type="info"
                      :closable="false"
                  >
                    <div class="applied-standards-count">
                      <span>已应用标准总数: {{ getAppliedStandardsCount() }}</span>
                      <el-button type="text" @click="showAppliedStandardsDetails = !showAppliedStandardsDetails">
                        {{ showAppliedStandardsDetails ? '隐藏详情' : '显示详情' }}
                      </el-button>
                    </div>
                  </el-alert>

                  <!-- 已应用标准详情 -->
                  <div v-if="showAppliedStandardsDetails" class="applied-standards-details">
                    <!-- 必须执行标准 -->
                    <div v-if="appliedStandards.required && appliedStandards.required.length > 0" class="domain-standards">
                      <h4>必须执行的标准</h4>
                      <el-table :data="appliedStandards.required" stripe border size="small">
                        <el-table-column prop="name" label="标准名称" min-width="120"/>
                        <el-table-column prop="type" label="标准类型" width="100">
                          <template #default="scope">
                            <el-tag :type="scope.row.type === 'NAMING' ? 'primary' : 'success'">
                              {{ scope.row.type }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="subType" label="子类型" width="150">
                          <template #default="scope">
                            <span>{{ getRuleSubTypeName(scope.row.subType) }}</span>
                          </template>
                        </el-table-column>
                        <el-table-column prop="standardType" label="执行类型" width="100">
                          <template #default="scope">
                            <el-tag type="danger" size="small">必须执行</el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                    
                    <!-- 建议执行标准 -->
                    <div v-if="appliedStandards.suggested && appliedStandards.suggested.length > 0" class="domain-standards">
                      <h4>建议执行的标准</h4>
                      <el-table :data="appliedStandards.suggested" stripe border size="small">
                        <el-table-column prop="name" label="标准名称" min-width="120"/>
                        <el-table-column prop="type" label="标准类型" width="100">
                          <template #default="scope">
                            <el-tag :type="scope.row.type === 'NAMING' ? 'primary' : 'success'">
                              {{ scope.row.type }}
                            </el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="subType" label="子类型" width="150">
                          <template #default="scope">
                            <span>{{ getRuleSubTypeName(scope.row.subType) }}</span>
                          </template>
                        </el-table-column>
                        <el-table-column prop="standardType" label="执行类型" width="100">
                          <template #default="scope">
                            <el-tag type="warning" size="small">建议执行</el-tag>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                    
                    <!-- 无已应用标准的情况 -->
                    <div v-if="(!appliedStandards.required || appliedStandards.required.length === 0) && 
                                (!appliedStandards.suggested || appliedStandards.suggested.length === 0)" 
                        class="empty-tip">
                      暂无已应用的标准
                    </div>
                  </div>
                </div>

                <div v-if="validationLoading" class="validation-loading">
                  <el-skeleton :rows="6" animated/>
                </div>

                <div v-else-if="validationResults.length === 0" class="validation-empty">
                  <el-empty description="暂无验证结果，请点击上方按钮进行标准验证"/>
                </div>

                <div v-else class="validation-results">
                  <el-card class="validation-summary">
                    <template #header>
                      <span>验证结果摘要</span>
                    </template>
                    <div class="summary-content">
                      <div class="summary-item">
                        <span class="label">验证标准总数:</span>
                        <span class="value">{{ validationResults.length }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="label">通过标准数:</span>
                        <span class="value success">{{ passedStandardsCount }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="label">未通过标准数:</span>
                        <span class="value danger">{{ failedStandardsCount }}</span>
                      </div>
                      <div class="summary-item">
                        <span class="label">发现问题总数:</span>
                        <span class="value warning">{{ totalIssuesCount }}</span>
                      </div>
                    </div>
                  </el-card>

                  <el-collapse v-model="activeValidations">
                    <el-collapse-item
                        v-for="(result, index) in validationResults"
                        :key="result.standardId || index"
                        :name="index"
                    >
                      <template #title>
                        <div class="validation-item-header">
                          <el-tag
                              :type="result.status ? 'success' : 'danger'"
                              size="medium"
                          >
                            {{ result.status ? '通过' : '未通过' }}
                          </el-tag>
                          <span class="standard-name">{{ result.standardName || '未命名标准' }}</span>
                          <span class="rule-name">{{ getRuleSubTypeName(result.subType) || '未知规则' }}</span>
                          <el-tag
                              v-if="result.standardType === 'required'"
                              type="danger"
                              size="small"
                          >
                            必须执行
                          </el-tag>
                          <el-tag
                              v-else-if="result.standardType === 'suggested'"
                              type="warning"
                              size="small"
                          >
                            建议执行
                          </el-tag>
                        </div>
                      </template>

                      <div class="validation-item-content">
                        <div class="validation-info">
                          <div class="info-item">
                            <span class="label">标准ID:</span>
                            <span class="value">{{ result.standardId || '未知' }}</span>
                          </div>
                          <div class="info-item">
                            <span class="label">标准代码:</span>
                            <span class="value">{{ result.standardCode || '未知' }}</span>
                          </div>
                          <div class="info-item">
                            <span class="label">规则类型:</span>
                            <span class="value">{{ getRuleTypeName(result.standardType) || '未知' }}</span>
                          </div>
                          <div class="info-item">
                            <span class="label">子类型:</span>
                            <span class="value">{{ getRuleSubTypeName(result.subType) }}</span>
                          </div>
                          <div class="info-item">
                            <span class="label">验证时间:</span>
                            <span class="value">{{ formatDate(result.validationTime) }}</span>
                          </div>
                        </div>

                        <div v-if="result.issues && result.issues.length > 0" class="validation-issues">
                          <div class="issues-header">
                            <span>发现的问题 ({{ result.issues.length }})</span>
                          </div>
                          <el-table
                              :data="result.issues"
                              stripe
                              border
                              style="width: 100%"
                          >
                            <el-table-column prop="objectName" label="字段/对象" min-width="150"/>
                            <el-table-column prop="message" label="问题描述" min-width="200"/>
                            <el-table-column prop="severity" label="严重程度" width="100">
                              <template #default="scope">
                                <el-tag
                                    :type="getSeverityType(scope.row.severity)"
                                    size="small"
                                >
                                  {{ scope.row.severity }}
                                </el-tag>
                              </template>
                            </el-table-column>
                            <el-table-column prop="details" label="建议" min-width="200"/>
                          </el-table>
                        </div>

                        <div v-else class="validation-success">
                          <i class="el-icon-success"></i>
                          <span>该标准验证通过，未发现问题</span>
                        </div>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>

                <!-- 添加底部操作区域 -->
                <div class="validation-footer" style="margin-top: 20px; text-align: right;">
                  <el-button type="primary" @click="standardsDialogVisible = false">完成</el-button>
                  <el-button @click="standardsActiveTab = 'configure'">返回配置</el-button>
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-dialog>

        <!-- 模型列表卡片 -->
        <el-card class="list-card" v-if="!showTableList">
          <el-table
              v-loading="loading"
              :data="modelList"
              :size="tableSize"
              border
              stripe
              style="width: 100%"
          >
            <el-table-column type="selection" width="55" align="center"/>
            <el-table-column type="index" label="序号" width="80" align="center"/>
            <el-table-column prop="name" label="模型名称" min-width="180" show-overflow-tooltip>
              <template #default="{ row }">
                <div class="model-name">
                  <el-icon :class="getModelTypeIcon(row.type)">
                    <component :is="getModelTypeIcon(row.type)"/>
                  </el-icon>
                  <span>{{ row.name }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="type" label="模型类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getModelTypeTag(row.type)">
                  {{ getModelTypeName(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="domain" label="所属主题域" width="150"/>
            <el-table-column prop="owner" label="负责人" width="120"/>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="getModelStatusTag(row.status)">
                  {{ getModelStatusName(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="updateTime" label="更新时间" width="180"/>
            <el-table-column label="操作" width="280" fixed="right">
              <template #default="{ row }">
                <el-button-group>
                  <el-tooltip content="查看详情" placement="top">
                    <el-button type="primary" link @click="handleView(row)">
                      <el-icon>
                        <View/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="设计模型" placement="top">
                    <el-button type="primary" link @click="handleDesign(row)">
                      <el-icon>
                        <Edit/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="发布" placement="top">
                    <el-button type="success" link @click="handlePublish(row)">
                      <el-icon>
                        <Upload/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="运行" placement="top">
                    <el-button type="warning" link @click="handleRun(row)">
                      <el-icon>
                        <VideoPlay/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                  <el-tooltip content="删除" placement="top">
                    <el-button type="danger" link @click="handleDelete(row)">
                      <el-icon>
                        <Delete/>
                      </el-icon>
                    </el-button>
                  </el-tooltip>
                </el-button-group>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
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
        </el-card>

        <!-- 数据表列表卡片 -->
        <el-card class="list-card" v-if="showTableList">
          <el-table
              v-loading="tableLoading"
              :data="tableData"
              :size="tableSize"
              border
              stripe
              style="width: 100%"
              row-key="id"
          >
            <el-table-column prop="name" label="表名" show-overflow-tooltip/>
            <el-table-column prop="displayName" label="显示名称" show-overflow-tooltip/>
            <el-table-column prop="fieldCount" label="字段数" width="100" align="center"/>
            <el-table-column label="创建时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="更新时间" width="160">
              <template #default="{ row }">
                {{ formatDate(row.updateTime) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="250" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="handleEditTable(row)">
                  <el-icon>
                    <Edit/>
                  </el-icon>
                  编辑
                </el-button>
                <el-button link type="primary" @click="handleViewTable(row)">
                  <el-icon>
                    <Document/>
                  </el-icon>
                  查看
                </el-button>
                <el-button link type="danger" @click="handleDeleteTable(row)">
                  <el-icon>
                    <Delete/>
                  </el-icon>
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <!-- 分页 -->
          <div class="pagination">
            <el-pagination
                v-model:current-page="tablePagination.pageNum"
                v-model:page-size="tablePagination.pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="tablePagination.total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleTableSizeChange"
                @current-change="handleTablePageChange"
                background
            />
          </div>
        </el-card>
      </div>
    </div>


    <!-- 列设置抽屉 -->
    <el-drawer
        v-model="showColumnSettings"
        title="列设置"
        direction="rtl"
        size="300px"
    >
      <el-alert
          title="拖动列表项可以调整列的显示顺序"
          type="info"
          :closable="false"
          class="mb-4"
      />
      <el-checkbox-group v-model="visibleColumns">
        <draggable
            v-model="columnSettings"
            item-key="prop"
            handle=".drag-handle"
            class="column-list"
        >
          <template #item="{ element }">
            <div class="column-item">
              <el-icon class="drag-handle">
                <Rank/>
              </el-icon>
              <el-checkbox :label="element.prop">
                {{ element.label }}
              </el-checkbox>
            </div>
          </template>
        </draggable>
      </el-checkbox-group>
    </el-drawer>

    <!-- 模型设计器弹窗 -->
    <el-dialog
        v-model="designerVisible"
        :title="currentModel ? `编辑模型: ${currentModel.name}` : '新建模型'"
        width="75%"
        :destroy-on-close="false"
        :close-on-click-modal="false"
        class="designer-dialog"
        top="12vh"
    >
      <model-designer
          :model-id="currentModel?.id || ''"
          :domain-id="currentModel?.domainId || ''"
          @save-success="handleDesignerSave"
          @cancel="designerVisible = false"
      />
    </el-dialog>

    <!-- 表格编辑对话框 -->
    <el-dialog
        v-model="showEditor"
        :title="editingId ? '编辑表' : '新建表'"
        width="90%"
        :destroy-on-close="false"
        :close-on-click-modal="false"
        class="table-editor-dialog"
        top="5vh"
        :fullscreen="false"
        :modal="true"
        :append-to-body="true"
        :lock-scroll="true"
    >
      <!-- 表格编辑区域 -->
      <div class="table-editor">
        <div class="editor-header">
          <div class="basic-info">
            <el-form :model="tableForm" label-width="100px" class="table-form">
              <el-form-item label="表名" required>
                <el-input v-model="tableForm.name" placeholder="请输入表名"/>
              </el-form-item>
              <el-form-item label="显示名称" required>
                <el-input v-model="tableForm.displayName" placeholder="请输入显示名称"/>
              </el-form-item>
            </el-form>
          </div>
          <div class="actions">
            <el-radio-group v-model="editMode" class="edit-mode">
              <el-radio-button value="visual">可视化编辑</el-radio-button>
              <el-radio-button value="sql">SQL编辑</el-radio-button>
              <el-radio-button value="code">程序代码</el-radio-button>
            </el-radio-group>
          </div>
        </div>

        <!-- 可视化编辑模式 -->
        <div v-if="editMode === 'visual'" class="fields-editor">
          <div class="toolbar">
            <el-button-group>
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
            </el-button-group>
          </div>

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
            <el-table-column label="长度" width="160" align="center">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.length"
                    :min="1"
                    :disabled="!needLength(row.type)"
                    class="full-width-number"
                    controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="小数位数" width="160" align="center">
              <template #default="{ row }">
                <el-input-number
                    v-model="row.precision"
                    :min="0"
                    :disabled="!needPrecision(row.type)"
                    class="full-width-number"
                    controls-position="right"
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
                <el-input v-model="row.mgcomment" placeholder="备注"/>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- SQL编辑模式 -->
        <div v-else-if="editMode === 'sql'" class="sql-editor">
          <div class="sql-toolbar">
            <el-select
                v-model="currentDbType"
                placeholder="请选择数据库类型"
                class="db-type-select"
            >
              <el-option
                  v-for="option in dbTypeOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
              />
            </el-select>
            <el-button-group>
              <el-button @click="formatSQL">
                <el-icon>
                  <Document/>
                </el-icon>
                格式化
              </el-button>
              <el-button @click="parseSQL">
                <el-icon>
                  <CircleCheck/>
                </el-icon>
                解析SQL
              </el-button>
            </el-button-group>
          </div>
          <div class="sql-content">
            <div class="code-wrapper">
              <el-input
                  v-model="sqlContent"
                  type="textarea"
                  :rows="20"
                  :autosize="{ minRows: 20, maxRows: 30 }"
                  placeholder="请输入建表SQL语句"
                  @input="handleSQLChange"
              />
              <el-button
                  class="copy-btn"
                  type="primary"
                  link
                  @click="copyCode(sqlContent)"
              >
                <el-icon>
                  <Document/>
                </el-icon>
                复制
              </el-button>
            </div>
          </div>
        </div>

        <!-- 程序代码模式 -->
        <div v-else class="code-editor">
          <div class="code-toolbar">
            <el-form-item label="生成类型">
              <el-select v-model="codeType" placeholder="请选择生成类型">
                <el-option
                    v-for="option in codeTypeOptions"
                    :key="option.value"
                    :label="option.label"
                    :value="option.value"
                />
              </el-select>
            </el-form-item>
          </div>
          <div class="code-content">
            <el-tabs type="border-card">
              <el-tab-pane label="实体类">
                <div class="code-wrapper">
                  <el-input
                      v-model="generatedCode.entity"
                      type="textarea"
                      :rows="20"
                      readonly
                  />
                  <el-button
                      class="copy-btn"
                      type="primary"
                      link
                      @click="copyCode(generatedCode.entity)"
                  >
                    <el-icon>
                      <Document/>
                    </el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
              <el-tab-pane label="Mapper">
                <div class="code-wrapper">
                  <el-input
                      v-model="generatedCode.mapper"
                      type="textarea"
                      :rows="20"
                      readonly
                  />
                  <el-button
                      class="copy-btn"
                      type="primary"
                      link
                      @click="copyCode(generatedCode.mapper)"
                  >
                    <el-icon>
                      <Document/>
                    </el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
              <el-tab-pane label="Service">
                <div class="code-wrapper">
                  <el-input
                      v-model="generatedCode.service"
                      type="textarea"
                      :rows="20"
                      readonly
                  />
                  <el-button
                      class="copy-btn"
                      type="primary"
                      link
                      @click="copyCode(generatedCode.service)"
                  >
                    <el-icon>
                      <Document/>
                    </el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
              <el-tab-pane label="Controller">
                <div class="code-wrapper">
                  <el-input
                      v-model="generatedCode.controller"
                      type="textarea"
                      :rows="20"
                      readonly
                  />
                  <el-button
                      class="copy-btn"
                      type="primary"
                      link
                      @click="copyCode(generatedCode.controller)"
                  >
                    <el-icon>
                      <Document/>
                    </el-icon>
                    复制
                  </el-button>
                </div>
              </el-tab-pane>
            </el-tabs>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cancelEdit">取消</el-button>
          <el-button type="primary" @click="saveTable">保存</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 查看表结构对话框 -->
    <el-dialog
        v-model="viewDialogVisible"
        :title="viewingTable?.displayName + ' - 表结构'"
        width="80%"
        destroy-on-close
    >
      <div class="view-table-info">
        <div class="info-item">
          <span class="label">表名：</span>
          <span class="value">{{ viewingTable?.name }}</span>
        </div>
        <div class="info-item">
          <span class="label">显示名称：</span>
          <span class="value">{{ viewingTable?.displayName }}</span>
        </div>
      </div>

      <el-table :data="viewingTable?.fields" border class="view-fields-table">
        <el-table-column prop="name" label="字段名称" min-width="150"/>
        <el-table-column prop="displayName" label="显示名称" min-width="150"/>
        <el-table-column prop="type" label="数据类型" width="120">
          <template #default="{ row }">
            {{ row.type }}{{ getTypeDetail(row) }}
          </template>
        </el-table-column>
        <el-table-column label="主键" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.isPrimary" type="warning" effect="plain">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="不为空" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.notNull" type="info" effect="plain">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="自增" width="80" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.autoIncrement" type="success" effect="plain">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column prop="defaultValue" label="默认值" width="120">
          <template #default="{ row }">
            {{ row.defaultValue || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="mgcomment" label="备注" min-width="200">
          <template #default="{ row }">
            {{ row.mgcomment || '-' }}
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="viewDialogVisible = false">关闭</el-button>
          <el-button type="primary" @click="handleEdit(viewingTable)">编辑</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 物化到数据库对话框 -->
    <el-dialog
        v-model="materializeDialogVisible"
        title="物化到数据库"
        width="500px"
    >
      <el-form ref="materializeForm" :model="materializeForm" label-width="100px">
        <el-form-item label="数据源" required>
          <el-select
              v-model="refCreateForm.sourceId"
              placeholder="请选择数据源"
          >
            <el-option
                v-for="source in databaseSources"
                :key="source.id"
                :label="source.name"
                :value="source.id"
            />
          </el-select>
        </el-form-item>

        <!-- 显示选中的表 -->
        <el-form-item label="选中的表">
          <div class="selected-tables">
            <el-tag
                v-for="table in selectedTables"
                :key="table.id"
                class="mx-1"
            >
              {{ table.name }}
            </el-tag>
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="materializeDialogVisible = false">取消</el-button>
          <el-button
              type="primary"
              @click="handleMaterialize"
              :loading="materializeLoading"
          >
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- SQL新建表对话框 -->
    <el-dialog
        v-model="sqlCreateDialogVisible"
        title="SQL新建表"
        width="800px"
    >
      <div class="sql-editor">
        <el-input
            v-model="sqlCreateContent"
            type="textarea"
            :rows="10"
            placeholder="请输入建表SQL语句"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="sqlCreateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSQLCreate" :loading="sqlCreateLoading">
            执行
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 引用已有表对话框 -->
    <el-dialog
        v-model="refCreateDialogVisible"
        title="引用已有表"
        width="800px"
        @closed="resetRefCreateForm"
    >
      <el-form :model="refCreateForm" label-width="100px">
        <el-form-item label="数据源" required>
          <el-select
              v-model="refCreateForm.sourceId"
              placeholder="请选择数据源"
              @change="handleSourceChange"
          >
            <el-option
                v-for="source in databaseSources"
                :key="source.id"
                :label="source.name"
                :value="source.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="选择表" required>
          <el-table
              :data="availableTables"
              border
              height="400"
              @selection-change="handleTableSelectionChange"
          >
            <el-table-column type="selection" width="50"/>
            <el-table-column prop="name" label="表名" min-width="180">
              <template #default="{ row }">
                <el-tooltip :content="row.schema" placement="top" :disabled="!row.schema">
                  <span>{{ row.name }}</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="注释" min-width="180" show-overflow-tooltip/>
            <el-table-column prop="type" label="类型" width="100" align="center"/>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="refCreateDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleRefCreate" :loading="refCreateLoading">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 文件新建表对话框 -->
    <el-dialog
        v-model="fileCreateDialogVisible"
        title="文件新建表"
        width="500px"
    >
      <div class="file-upload">
        <el-upload
            class="upload-demo"
            drag
            action="/api/table/import"
            :accept="acceptFileTypes"
            :before-upload="beforeFileUpload"
            :on-success="handleFileUploadSuccess"
            :on-error="handleFileUploadError"
        >
          <el-icon class="el-icon--upload">
            <upload-filled/>
          </el-icon>
          <div class="el-upload__text">
            将文件拖到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 excel、csv、txt、dmp、sql 等格式文件
              <el-dropdown @command="handleEmptyTemplateDownload">
                <el-button link type="primary">
                  下载模板
                  <el-icon class="el-icon--right">
                    <arrow-down/>
                  </el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                        v-for="btn in downloadButtons"
                        :key="btn.format"
                        :command="btn.format"
                    >
                      <el-icon>
                        <component :is="btn.icon"/>
                      </el-icon>
                      {{ btn.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-upload>
      </div>
    </el-dialog>


    <!-- 在组件的最外层添加验证详情对话框 -->
    <el-dialog
        v-model="validationDetailsVisible"
        title="验证详情"
        width="60%"
    >
      <div v-if="currentValidationDetails">
        <div class="validation-detail-header">
          <p><strong>标准名称:</strong> {{ currentValidationDetails.standardName }}</p>
          <p><strong>标准编码:</strong> {{ currentValidationDetails.standardCode }}</p>
          <p><strong>验证时间:</strong> {{ formatDate(currentValidationDetails.validationTime) }}</p>
        </div>

        <div class="validation-issues">
          <h4>验证问题</h4>
          <el-table :data="currentValidationDetails.issues" border style="width: 100%">
            <el-table-column prop="field" label="字段" min-width="120"/>
            <el-table-column prop="issue" label="问题描述" min-width="200"/>
            <el-table-column prop="severity" label="严重程度" min-width="100"/>
            <el-table-column prop="suggestion" label="修改建议" min-width="200"/>
          </el-table>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="validationDetailsVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 新增标准对话框 -->
    <el-dialog
        v-model="addStandardDialogVisible"
        :title="`新增${addStandardType === 'required' ? '必须执行' : '建议执行'}的标准`"
        width="70%"
        destroy-on-close
    >
      <div class="add-standard-dialog">
        <div class="filter-bar" style="margin-bottom: 15px;">
          <el-input
              v-model="standardSearchKeyword"
              placeholder="输入关键词搜索标准"
              clearable
              style="width: 300px;"
          >
            <template #prefix>
              <el-icon>
                <Search/>
              </el-icon>
            </template>
          </el-input>
          <span style="margin-left: 15px;">找到 {{ filteredStandardRules.length }} 条记录</span>
        </div>

        <!-- 调试信息 -->
        <el-alert
            v-if="allStandardRules.length === 0 && !addStandardLoading"
            title="未找到标准规则数据"
            type="warning"
            show-icon
            :closable="false"
            style="margin-bottom: 15px;"
        >
          请检查API返回的数据是否正确。
        </el-alert>

        <el-table
            :data="filteredStandardRules"
            border
            style="width: 100%"
            height="400px"
            @selection-change="handleSelectStandard"
            v-loading="addStandardLoading"
        >
          <el-table-column type="selection" width="55"/>
          <el-table-column type="index" width="50" label="#"/>
          <el-table-column prop="name" label="标准名称" min-width="150" show-overflow-tooltip/>
          <el-table-column prop="code" label="标准编码" min-width="100" show-overflow-tooltip/>
          <el-table-column label="规则类型" min-width="120">
            <template #default="{ row }">
              {{ getStandardTypeText(row.ruleType || row.type) }}
            </template>
          </el-table-column>
          <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip/>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'">
                {{ row.status === 1 ? '启用' : '禁用' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <!-- 调试信息 -->
        <div v-if="filteredStandardRules.length === 0 && !addStandardLoading" class="empty-tip">
          没有可用的标准规则数据
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="addStandardDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmAddStandards" :disabled="selectedNewStandards.length === 0">
            确认添加 ({{ selectedNewStandards.length }})
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 在列表最后添加新建模型对话框 -->
    <el-dialog
        v-model="createModelDialogVisible"
        :title="isEditingModel ? '编辑模型' : '新建模型'"
        width="400px"
        destroy-on-close
    >
      <el-form ref="modelFormRef" :model="modelForm" label-width="80px" :rules="modelFormRules">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="modelForm.name" placeholder="请输入模型名称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="createModelDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitCreateModel" :loading="submitLoading">确认</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, nextTick, watch} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage, ElMessageBox, ElLoading} from 'element-plus'
import {
  Search, Refresh, Plus, Upload, Download, Grid, Setting,
  View, Edit, Delete, ArrowUp, ArrowDown, Rank, VideoPlay,
  DataBoard, Connection, Share, Document, CircleCheck, UploadFilled, Collection,
  RefreshRight, HomeFilled
} from '@element-plus/icons-vue'
import draggable from 'vuedraggable'
import ModelDesigner from './designer.vue'
import {getModelTree, getModelList, deleteModel, publishModel, runModel, getModel, addModel, updateModel} from '@/api/dataAsset/model'
import request from '@/utils/request'
import {getTableList} from '@/api/table'
import {useTagsViewStore} from '@/store/modules/tagsView'
import {getDatabaseSources, getSourceTableStructure} from '@/api/datasource'
import {
  createTable,
  updateTable,
  deleteTable,
  createTableBySQL,
  referenceExistingTables,
  getTableDetail,
  downloadEmptyTemplate
} from '@/api/table'
import {getTableSzList} from '@/api/database'
import {
  getDomainList,
  getAppliedStandards,
  removeDomainStandards,
  removeStandard,
  getDomainDetail, applyModelStandards,
  validateModelStandards,
  getAvailableStandards
} from '@/api/domain'
import { getRuleObjectsWithoutObjectId} from '@/api/domain' // 导入新的API方法
import {getRuleTypeName, getRuleSubTypeName} from '@/utils/format'

const router = useRouter()
const tagsViewStore = useTagsViewStore()

// 表格相关
const loading = ref(false)
const tableSize = ref('default')
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 显示控制
const showTableList = ref(false)

// Physical.vue 功能相关变量
const showEditor = ref(false)
const selectedFields = ref([])
const editingId = ref(null)
const editMode = ref('visual')
const sqlContent = ref('')
const viewDialogVisible = ref(false)
const viewingTable = ref(null)
const currentTable = ref(null)

// 数据类型选项
const dataTypes = [
  {label: 'VARCHAR', value: 'VARCHAR'},
  {label: 'CHAR', value: 'CHAR'},
  {label: 'TEXT', value: 'TEXT'},
  {label: 'LONGTEXT', value: 'LONGTEXT'},
  {label: 'TINYTEXT', value: 'TINYTEXT'},
  {label: 'INT', value: 'INT'},
  {label: 'TINYINT', value: 'TINYINT'},
  {label: 'SMALLINT', value: 'SMALLINT'},
  {label: 'MEDIUMINT', value: 'MEDIUMINT'},
  {label: 'BIGINT', value: 'BIGINT'},
  {label: 'DECIMAL', value: 'DECIMAL'},
  {label: 'FLOAT', value: 'FLOAT'},
  {label: 'DOUBLE', value: 'DOUBLE'},
  {label: 'DATETIME', value: 'DATETIME'},
  {label: 'DATE', value: 'DATE'},
  {label: 'TIME', value: 'TIME'},
  {label: 'TIMESTAMP', value: 'TIMESTAMP'},
  {label: 'YEAR', value: 'YEAR'},
  {label: 'BOOLEAN', value: 'BOOLEAN'},
  {label: 'JSON', value: 'JSON'},
  {label: 'BLOB', value: 'BLOB'}
]

// 数据库类型选项
const dbTypeOptions = [
  {label: 'MySQL', value: 'mysql'},
  {label: 'Oracle', value: 'oracle'},
  {label: 'PostgreSQL', value: 'postgresql'},
  {label: 'SQL Server', value: 'sqlserver'},
  {label: 'MariaDB', value: 'mariadb'},
  {label: 'DB2', value: 'db2'},
  {label: 'SQLite', value: 'sqlite'},
  {label: 'H2', value: 'h2'},
  {label: 'HSQLDB', value: 'hsqldb'},
  {label: 'Derby', value: 'derby'},
  {label: 'Informix', value: 'informix'},
  {label: 'Sybase', value: 'sybase'},
  {label: 'DM', value: 'dm'},
  {label: 'KingBase', value: 'kingbase'},
  {label: 'GBase', value: 'gbase'},
  {label: 'OpenGauss', value: 'opengauss'}
]


// 对话框相关
const standardsDialogVisible = ref(false)
const standardsActiveTab = ref('select') // 默认显示选择标准标签页

// 主题域相关
const currentDomainId = ref('')
const currentDomainName = ref('')
const currentDomainDescription = ref('')
const currentModelId = ref('')
// 树相关
const treeList = ref([])
const currentParentNode = ref(null)
// 表格相关
const multipleSelection = ref([])
const totalRequiredStandards = ref(0)
const totalSuggestedStandards = ref(0)
// 当前选中的数据库类型
const currentDbType = ref('mysql')
const appliedDomainIds = ref([])
const selectedAppliedDomainId = ref('customer') // 默认选中客户域
const standardsApplied = ref(false)
const applyProgress = ref(0)
// 可用标准相关
const availableStandards = ref({});
const availableStandardsLoading = ref(false);

// 应用标准表单数据
const standardsForm = reactive({
  targetType: 'model',
  targetNodeName: '',
  applyScope: 'all',
  description: ''
})

// 其他应用标准相关数据
const domainOptions = ref([])
const requiredStandards = ref([]) // 必须执行的标准
const suggestedStandards = ref([]) // 建议执行的标准

// 获取主题域名称
// 如果不需要通过domainId获取domain名称，可以移除以下方法
// const getDomainName = (domainId) => {
//   const domain = appliedStandards.value.domains.find(d => d.id === domainId);
//   return domain ? domain.name : domainId;
// }

// 获取指定标准类型的已应用标准
const getAppliedStandardsByType = (type) => {
  if (!appliedStandards.value || !appliedStandards.value[type]) {
    return [];
  }
  return appliedStandards.value[type] || [];
}

// 表单数据
const tableForm = reactive({
  id: null,
  name: '',
  displayName: '',
  fields: []
})

// 添加搜索表单数据
const searchForm = ref({
  name: '',
  displayName: '',
  createTimeStart: '',
  createTimeEnd: '',
  minFieldCount: null,
  maxFieldCount: null
})

// 物化到数据库相关
const materializeDialogVisible = ref(false)
const materializeLoading = ref(false)
const databaseSources = ref([])

// 选中的表
const selectedTables = ref([])

// SQL新建表相关
const sqlCreateDialogVisible = ref(false)
const sqlCreateContent = ref('')
const sqlCreateLoading = ref(false)

// 引用已有表相关
const refCreateDialogVisible = ref(false)
const refCreateForm = reactive({
  sourceId: '',
  selectedTables: []
})
const availableTables = ref([])
const refCreateLoading = ref(false)

// 文件新建表相关
const fileCreateDialogVisible = ref(false)
const acceptFileTypes = '.xlsx,.xls,.csv,.txt,.dmp,.sql'

// 代码生成相关
const codeType = ref('java')
const generatedCode = ref({
  entity: '',
  mapper: '',
  service: '',
  controller: ''
})

// 代码类型选项
const codeTypeOptions = [
  {label: 'Java', value: 'java'},
  {label: 'C#', value: 'csharp'},
  {label: 'Go', value: 'go'},
  {label: 'Python', value: 'python'},
  {label: 'TypeScript', value: 'typescript'}
]

// 下载按钮配置
const downloadButtons = [
  {
    label: 'Excel模板',
    format: 'excel',
    icon: 'Document',
    mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    extension: 'xlsx',
    contentTypes: [
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      'application/vnd.ms-excel'
    ]
  },
  {
    label: 'CSV模板',
    format: 'csv',
    icon: 'Document',
    mimeType: 'text/csv',
    extension: 'csv',
    contentTypes: ['text/csv']
  },
  {
    label: 'TXT模板',
    format: 'txt',
    icon: 'Document',
    mimeType: 'text/plain',
    extension: 'txt',
    contentTypes: ['text/plain']
  },
  {
    label: 'DMP模板',
    format: 'dmp',
    icon: 'Document',
    mimeType: 'application/octet-stream',
    extension: 'dmp',
    contentTypes: ['application/octet-stream']
  },
  {
    label: 'SQL模板',
    format: 'sql',
    icon: 'Document',
    mimeType: 'text/plain',
    extension: 'sql',
    contentTypes: ['text/plain']
  }
]

// 列设置
const showColumnSettings = ref(false)
const visibleColumns = ref(['name', 'type', 'domain', 'owner', 'status', 'updateTime'])
const columnSettings = ref([
  {prop: 'name', label: '模型名称'},
  {prop: 'type', label: '模型类型'},
  {prop: 'domain', label: '所属主题域'},
  {prop: 'owner', label: '负责人'},
  {prop: 'status', label: '状态'},
  {prop: 'updateTime', label: '更新时间'}
])

// 树形控件相关
const modelTreeRef = ref(null)
const treeFilter = ref('')
const contextMenuVisible = ref(false)
const contextMenuStyle = ref({
  left: '0px',
  top: '0px'
})
const contextMenuNode = ref({})
const treeData = ref([])

const treeProps = {
  label: 'name',
  children: 'children'
}

// 模型列表数据
const modelList = ref([])

// 表格数据相关
const tableData = ref([])
const tablePagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const tableLoading = ref(false)
const tableQuery = reactive({
  name: '',
  type: '',
  status: ''
})

// 当前选中的分组ID
const currentGroupId = ref('')


// 加载树形数据
const loadTreeData = async () => {
  try {
    loading.value = true
    const res = await getModelTree()
    if (res && res.code === 0) {
      // 直接使用后端返回的模型列表作为数据源
      treeData.value = res.data || []
      // 不再需要展开节点，因为不是树结构
    } else {
      ElMessage.error(res?.message || '获取模型列表失败')
    }
  } catch (error) {
    console.error('获取模型列表出错:', error)
    ElMessage.error('获取模型列表出错')
  } finally {
    loading.value = false
  }
}

// 加载模型列表
const loadModelList = async (params = {}) => {
  try {
    loading.value = true
    const queryParams = {
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      ...params
    }

    const res = await getModelList(queryParams)
    if (res && res.code === 0) {
      modelList.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      ElMessage.error(res?.message || '获取模型列表失败')
    }
  } catch (error) {
    console.error('获取模型列表出错:', error)
    ElMessage.error('获取模型列表出错')
  } finally {
    loading.value = false
  }
}

// 获取数据表列表
const loadTableList = (groupId, resetPage = false) => {
  if (resetPage) {
    tablePagination.pageNum = 1
  }

  tableLoading.value = true
  console.log('获取数据表列表, 参数:', {
    groupId,
    pageNum: tablePagination.pageNum,
    pageSize: tablePagination.pageSize,
    query: tableQuery
  })

  // 构建查询参数
  const params = {
    pageNum: tablePagination.pageNum,
    pageSize: tablePagination.pageSize,
    ...tableQuery
  }

  // 调用API获取表列表
  getTableList(groupId, params)
      .then(res => {
        console.log('获取数据表列表结果:', res)
        if (res && res.code === 0 && res.data) {
          // 设置表格数据
          tableData.value = res.data.records || []
          tablePagination.total = res.data.total || 0
        } else {
          ElMessage.error(res?.message || '获取数据表列表失败')
        }
      })
      .catch(error => {
        console.error('获取数据表列表出错:', error)
        ElMessage.error('获取数据表列表出错: ' + (error.message || error))
      })
      .finally(() => {
        tableLoading.value = false
      })
}

// 处理表格分页变化
const handleTablePageChange = (page) => {
  tablePagination.pageNum = page
  if (currentGroupId.value) {
    loadTableList(currentGroupId.value)
  }
}

// 处理表格每页条数变化
const handleTableSizeChange = (size) => {
  tablePagination.pageSize = size
  tablePagination.pageNum = 1
  if (currentGroupId.value) {
    loadTableList(currentGroupId.value)
  }
}

// 初始化加载数据
onMounted(() => {
  // 获取树数据
  loadTreeData()
  loadModelList()
  
  // 测试API可用性
  console.log('测试API导入是否正确:');
  console.log('- getModel API:', typeof getModel);
  console.log('- addModel API:', typeof addModel);
  console.log('- updateModel API:', typeof updateModel);
})

// 工具方法
const getModelTypeIcon = (type) => {
  const icons = {
    domain: 'Share',
    summary: 'DataBoard',
    detail: 'Connection'
  }
  return icons[type] || 'Connection'
}

const getModelTypeName = (type) => {
  const names = {
    summary: '汇总模型',
    detail: '明细模型'
  }
  return names[type] || type
}

const getModelTypeTag = (type) => {
  const tags = {
    summary: 'primary',
    detail: 'success'
  }
  return tags[type] || 'info'
}

const getModelStatusName = (status) => {
  const names = {
    draft: '草稿',
    published: '已发布',
    running: '运行中',
    failed: '运行失败'
  }
  return names[status] || status
}

const getModelStatusTag = (status) => {
  const tags = {
    draft: 'info',
    published: 'success',
    running: 'warning',
    failed: 'danger'
  }
  return tags[status] || 'info'
}

// 树节点过滤方法
const filterNode = (value, data) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

// 监听过滤关键字变化
watch(treeFilter, (val) => {
  modelTreeRef.value?.filter(val)
})

// 刷新模型列表
const refreshTree = () => {
  loadTreeData()
}

// 定义回到首页功能
const goHome = () => {
  console.log('回到首页')
  // 使用Vue Router导航到首页
  showTableList.value = false
  loadModelList()
}

// 处理树节点选择事件
const handleNodeClick = (node, nodeData, nodeComponent) => {
  console.log('列表项被点击:', node)

  // 保存当前选中节点
  currentNode.value = node
  currentModelId.value = node.id
  // 获取groupId，可能存储在id或其他属性中
  const groupId = node.id || node.groupId

  if (groupId) {
    // 切换到表格列表视图
    showTableList.value = true
    currentGroupId.value = groupId
  }

  // 根据模型ID加载模型详情
  showTableList.value = true
  loadTableList(node.id, true)

  // 保存当前选中的节点，以便其他操作使用
  contextMenuNode.value = node
}

// 设计器相关
const designerVisible = ref(false)
const currentModel = ref(null)

// 事件处理
const handleCreateModel = () => {
  // 重置表单
  resetModelForm();
  
  // 显示新建模型对话框
  createModelDialogVisible.value = true
}

// 重置模型表单为默认值
const resetModelForm = () => {
  Object.assign(modelForm, {
    id: null,
    name: '',
    type: 'detail',
    owner: '系统管理员',
    status: 'draft',
    description: '',
    config: '',
    createTime: null,
    updateTime: null,
    version: 0
  });
}

const handleImport = () => {
  // 实现导入逻辑
  ElMessage.info('导入功能待实现')
}

const handleExport = (row) => {
  // 实现导出逻辑
  if (row) {
    ElMessage.info(`导出模型: ${row.name}`)
  } else {
    ElMessage.info('导出所选模型')
  }
}

const refreshList = () => {
  if (showTableList.value) {
    if (currentGroupId.value) {
      loadTableList(currentGroupId.value, true)
    }
  } else {
    loadModelList()
  }
  ElMessage.success('刷新成功')
}

const toggleDensity = () => {
  const sizes = ['default', 'large', 'small']
  const currentIndex = sizes.indexOf(tableSize.value)
  tableSize.value = sizes[(currentIndex + 1) % sizes.length]
}

const handleSizeChange = (val) => {
  pageSize.value = val
  loadModelList()
}

const handleCurrentChange = (val) => {
  currentPage.value = val
  loadModelList()
}

const handleView = async (row) => {
  try {
    loading.value = true

    // 获取完整的模型信息
    const res = await getModel(row.id)
    if (res && res.code === 0) {
      // 这里可以实现查看详情的逻辑
      // 例如打开一个详情对话框
      ElMessage.info(`查看模型: ${res.data.name}`)
    } else {
      ElMessage.error(res?.message || '获取模型详情失败')
    }
  } catch (error) {
    console.error('获取模型详情出错:', error)
    ElMessage.error('获取模型详情出错')
  } finally {
    loading.value = false
  }
}

const handleDesign = async (row) => {
  try {
    loading.value = true
    console.log('开始设计模型，传入的row:', row);
    console.log('*******设计模型开始*******');

    // 获取完整的模型信息
    const res = await getModel(row.id)
    console.log('getModel 响应:', res);
    
    if (res && res.code === 0) {
      // 数据结构是 {standards: Array(0), design: null, model: {…}}
      // 我们需要从model属性获取实际的模型数据
      const modelData = res.data.model || res.data;
      currentModel.value = modelData;
      
      console.log('提取的模型数据:', modelData);
      
      // 保存完整的模型数据
      Object.assign(modelForm, {
        id: modelData.id,  // 明确指定ID
        name: modelData.name || '',
        type: modelData.type || 'detail',
        owner: modelData.owner || '系统管理员',
        status: modelData.status || 'draft',
        description: modelData.description || '',
        config: modelData.config || '',
        createTime: modelData.createTime,
        updateTime: modelData.updateTime,
        version: modelData.version || 0
      });
      
      console.log('设置后的模型表单:', JSON.stringify(modelForm));
      console.log('isEditingModel值:', isEditingModel.value);
      console.log('*******设计模型结束*******');
      
      createModelDialogVisible.value = true
    } else {
      ElMessage.error(res?.message || '获取模型详情失败')
    }
  } catch (error) {
    console.error('获取模型详情出错:', error)
    ElMessage.error('获取模型详情出错')
  } finally {
    loading.value = false
  }
}

const handleDesignerSave = (data) => {
  designerVisible.value = false

  // 刷新列表和树
  refreshList()
  refreshTree()

  ElMessage.success('模型保存成功')
}

const handlePublish = (row) => {
  ElMessageBox.confirm(
      `确定要发布模型"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await publishModel(row.id)
      if (res && res.code === 0) {
        ElMessage.success('发布成功')
        refreshList()
      } else {
        ElMessage.error(res?.message || '发布失败')
      }
    } catch (error) {
      console.error('发布模型出错:', error)
      ElMessage.error('发布模型出错')
    }
  }).catch(() => {
  })
}

const handleRun = (row) => {
  ElMessageBox.confirm(
      `确定要运行模型"${row.name}"吗？`,
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await runModel(row.id)
      if (res && res.code === 0) {
        ElMessage.success('运行成功')
        refreshList()
      } else {
        ElMessage.error(res?.message || '运行失败')
      }
    } catch (error) {
      console.error('运行模型出错:', error)
      ElMessage.error('运行模型出错')
    }
  }).catch(() => {
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
      `确定要删除模型"${row.name}"吗？删除后无法恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      const res = await deleteModel(row.id)
      if (res && res.code === 0) {
        ElMessage.success('删除成功')
        refreshList()
        refreshTree()
      } else {
        ElMessage.error(res?.message || '删除失败')
      }
    } catch (error) {
      console.error('删除模型出错:', error)
      ElMessage.error('删除模型出错')
    }
  }).catch(() => {
  })
}

// 表格操作方法
const handleCreateTable = () => {
  handleAdd()
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch (command) {
    case 'sqlCreate':
      sqlCreateDialogVisible.value = true
      break
    case 'refCreate':
      resetRefCreateForm()
      showRefCreateDialog()
      break
    case 'fileCreate':
      fileCreateDialogVisible.value = true
      break
  }
}

// 新建表时初始化
const handleAdd = () => {
  if (!currentGroupId.value) {
    ElMessage.warning('请先选择一个分组')
    return
  }

  // 重置表单
  resetTableForm()

  // 设置编辑模式为可视化
  editMode.value = 'visual'

  // 显示编辑器
  showEditor.value = true

  // 添加默认字段
  addField()

  // 生成初始SQL
  nextTick(() => {
    sqlContent.value = generateSQL(tableForm)
  })

  console.log('打开新建表编辑器', {
    showEditor: showEditor.value,
    tableForm,
    editMode: editMode.value
  })
}

// 判断数据类型是否需要长度
const needLength = (type) => {
  return [
    'VARCHAR',
    'CHAR',
    'INT',
    'TINYINT',
    'SMALLINT',
    'MEDIUMINT',
    'BIGINT'
  ].includes(type)
}

// 判断数据类型是否需要小数位数
const needPrecision = (type) => {
  return ['DECIMAL', 'FLOAT', 'DOUBLE'].includes(type)
}

// 判断数据类型是否可以自增
const canAutoIncrement = (type) => {
  return [
    'INT',
    'TINYINT',
    'SMALLINT',
    'MEDIUMINT',
    'BIGINT'
  ].includes(type)
}

// 获取数据类型详情
const getTypeDetail = (field) => {
  if (needLength(field.type)) {
    return `(${field.length})`
  }
  if (needPrecision(field.type)) {
    return `(${field.length},${field.precision})`
  }
  return ''
}

// 新增字段
const addField = () => {
  tableForm.fields.push({
    name: '',
    displayName: '',
    type: 'VARCHAR',
    length: 255,
    precision: 0,
    isPrimary: false,
    notNull: false,
    autoIncrement: false,
    defaultValue: '',
    comment: ''
  })
  // 强制更新SQL
  nextTick(() => {
    sqlContent.value = generateSQL(tableForm)
  })
}

// 删除字段
const deleteSelectedFields = () => {
  const ids = selectedFields.value.map(row => tableForm.fields.indexOf(row))
  ids.sort((a, b) => b - a) // 从后往前删除
  ids.forEach(index => {
    tableForm.fields.splice(index, 1)
  })
  selectedFields.value = []
  // 强制更新SQL
  nextTick(() => {
    sqlContent.value = generateSQL(tableForm)
  })
}

// 处理表格选择变化
const handleSelectionChange = (selection) => {
  selectedFields.value = selection
}

// 取消编辑
const cancelEdit = () => {
  showEditor.value = false
  editingId.value = null
  resetTableForm()
}

// 保存表
const saveTable = async () => {
  try {
    // 验证表单
    if (!tableForm.name) {
      ElMessage.warning('请输入表名')
      return
    }
    if (!tableForm.displayName) {
      ElMessage.warning('请输入显示名称')
      return
    }
    if (!tableForm.fields.length) {
      ElMessage.warning('请添加至少一个字段')
      return
    }

    if (!currentGroupId.value) {
      ElMessage.warning('分组ID不能为空')
      return
    }

    if (editingId.value) {  // 使用 editingId 判断是否为编辑模式
      // 更新表
      await updateTable(editingId.value, {
        ...tableForm,
        id: editingId.value,
        groupId: String(currentGroupId.value)
      })
      ElMessage.success('更新表成功')
    } else {
      // 创建表
      await createTable(String(currentGroupId.value), {
        ...tableForm,
        groupId: String(currentGroupId.value)
      })
      ElMessage.success('创建表成功')
    }

    showEditor.value = false
    // 重置表单
    resetTableForm()
    // 刷新表格列表
    loadTableList(currentGroupId.value, true)
  } catch (error) {
    console.error('保存表失败:', error)
    ElMessage.error('保存失败: ' + (error.message || '未知错误'))
  }
}

// 重置表单
const resetTableForm = () => {
  const emptyForm = {
    id: null,
    name: '',
    displayName: '',
    fields: []
  }
  Object.assign(tableForm, emptyForm)
  editingId.value = null  // 清除正在编辑的表 ID
  sqlContent.value = ''
}

// 生成SQL语句
const generateSQL = (table) => {
  if (!table || !table.name) {
    return '';
  }

  switch (currentDbType.value) {
    case 'mysql':
    case 'mariadb':
      return generateMySQLSQL(table);
    case 'oracle':
      return generateOracleSQL(table);
    case 'postgresql':
    case 'opengauss':
      return generatePostgreSQLSQL(table);
    default:
      return generateMySQLSQL(table); // 默认使用MySQL语法
  }
}

// 生成MySQL建表语句
const generateMySQLSQL = (table) => {
  if (!table.fields || table.fields.length === 0) {
    return `CREATE TABLE ${table.name}
            (
              id INT PRIMARY KEY AUTO_INCREMENT
            );`;
  }

  return `CREATE TABLE ${table.name}
          (
            ${table.fields.map(field => {
              const type = field.type + (needLength(field.type) ? `(${field.length})` : '');
              const constraints = [];
              if (field.isPrimary) constraints.push('PRIMARY KEY');
              if (field.autoIncrement) constraints.push('AUTO_INCREMENT');
              if (field.notNull) constraints.push('NOT NULL');
              if (field.defaultValue) constraints.push(`DEFAULT ${field.defaultValue}`);
              if (field.comment) constraints.push(`COMMENT '${field.comment}'`);
              return `${field.name} ${type} ${constraints.join(' ')}`;
            }).join(',\n  ')}
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='${table.displayName || table.name}';`;
}

// 生成Oracle建表语句
const generateOracleSQL = (table) => {
  if (!table.fields || table.fields.length === 0) {
    return `CREATE TABLE ${table.name}
            (
              id NUMBER PRIMARY KEY
            );`;
  }

  return `CREATE TABLE ${table.name}
          (
            ${table.fields.map(field => {
              let type = field.type;
              if (type === 'VARCHAR') type = `VARCHAR2(${field.length || 255})`;
              else if (type === 'INT') type = 'NUMBER(10)';

              const constraints = [];
              if (field.notNull) constraints.push('NOT NULL');
              if (field.defaultValue) constraints.push(`DEFAULT ${field.defaultValue}`);
              return `${field.name} ${type} ${constraints.join(' ')}`;
            }).join(',\n  ')}
          );
  ${table.fields.filter(f => f.isPrimary).length > 0 ?
      `\nALTER TABLE ${table.name} ADD CONSTRAINT PK_${table.name} PRIMARY KEY (${
          table.fields.filter(f => f.isPrimary).map(f => f.name).join(', ')
      });` : ''}
  ${table.fields.filter(f => f.comment).map(f =>
      `COMMENT ON COLUMN ${table.name}.${f.name} IS '${f.comment}';`
  ).join('\n')}`;
}

// 生成PostgreSQL建表语句
const generatePostgreSQLSQL = (table) => {
  if (!table.fields || table.fields.length === 0) {
    return `CREATE TABLE ${table.name}
            (
              id SERIAL PRIMARY KEY
            );`;
  }

  return `CREATE TABLE ${table.name}
          (
            ${table.fields.map(field => {
              let type = field.type;
              if (field.autoIncrement && field.type === 'INT') type = 'SERIAL';
              else if (field.type === 'VARCHAR') type = `VARCHAR(${field.length || 255})`;

              const constraints = [];
              if (field.isPrimary) constraints.push('PRIMARY KEY');
              if (field.notNull) constraints.push('NOT NULL');
              if (field.defaultValue) constraints.push(`DEFAULT ${field.defaultValue}`);
              return `${field.name} ${type} ${constraints.join(' ')}`;
            }).join(',\n  ')}
          );
  ${table.fields.filter(f => f.comment).map(f =>
      `COMMENT ON COLUMN ${table.name}.${f.name} IS '${f.comment}';`
  ).join('\n')}`;
}

// 格式化SQL
const formatSQL = () => {
  try {
    // 简单的格式化
    const formatted = sqlContent.value
        .replace(/\s+/g, ' ')
        .replace(/\(\s*/g, '(')
        .replace(/\s*\)/g, ')')
        .replace(/,\s*/g, ',\n  ')
        .replace(/CREATE TABLE/i, 'CREATE TABLE\n')
        .replace(/\((?=\s*`)/g, '(\n  ')
        .replace(/\)\s*ENGINE/g, '\n) ENGINE')

    sqlContent.value = formatted
    ElMessage.success('格式化成功')
  } catch (error) {
    ElMessage.error('SQL格式化失败')
  }
}

// 解析SQL为表单数据
const parseSQL = () => {
  try {
    ElMessage.warning('SQL解析功能开发中')
  } catch (error) {
    ElMessage.error('SQL解析失败')
  }
}

// 处理SQL变化
const handleSQLChange = () => {
  // 实时解析SQL更新表单
}

// 复制代码功能
const copyCode = async (code) => {
  try {
    await navigator.clipboard.writeText(code);
    ElMessage.success('复制成功');
  } catch (err) {
    console.error('复制失败:', err);
    ElMessage.error('复制失败');
  }
}

// 显示引用已有表对话框
const showRefCreateDialog = async () => {
  try {
    const {data} = await getDatabaseSources()
    databaseSources.value = data
    refCreateDialogVisible.value = true
  } catch (error) {
    ElMessage.error('获取数据源列表失败')
  }
}

// 数据源切换
const handleSourceChange = async (sourceId) => {
  refCreateForm.sourceId = sourceId
  try {
    const {data} = await getTableSzList(sourceId)
    availableTables.value = data.map(table => ({
      name: table.tableName || table.name,
      comment: table.tableComment || table.comment || '',
      schema: table.tableSchema || table.schema || '',
      type: table.tableType || table.type || 'TABLE'
    }))
  } catch (error) {
    ElMessage.error('获取表列表失败')
  }
}

// 表格选择变化
const handleTableSelectionChange = (selection) => {
  // 确保只发送表名数组，并且表名是字符串类型
  refCreateForm.selectedTables = selection.map(item =>
      typeof item === 'string' ? item : item.name
  ).filter(Boolean) // 过滤掉可能的空值
  selectedTables.value = selection
  // 只在选中一个表时设置当前表
  currentTable.value = selection.length === 1 ? selection[0] : null
}

// 重置引用表单
const resetRefCreateForm = () => {
  refCreateForm.sourceId = ''
  refCreateForm.selectedTables = []
  availableTables.value = []
}

// 引用已有表
const handleRefCreate = async () => {
  if (!refCreateForm.sourceId || !refCreateForm.selectedTables.length) {
    ElMessage.warning('请选择数据源和要引用的表')
    return
  }

  try {
    refCreateLoading.value = true

    if (!currentGroupId.value) {
      ElMessage.warning('分组ID不能为空')
      return
    }

    console.log('发送的数据:', {
      groupId: currentGroupId.value,
      sourceId: refCreateForm.sourceId,
      tables: refCreateForm.selectedTables
    })

    await referenceExistingTables(
        currentGroupId.value,
        refCreateForm.sourceId,
        refCreateForm.selectedTables
    )
    ElMessage.success('引用表成功')
    refCreateDialogVisible.value = false
    loadTableList(currentGroupId.value, true)
  } catch (error) {
    console.error('引用表失败:', error)
    ElMessage.error(error.response?.data?.msg || error.message || '引用失败')
  } finally {
    refCreateLoading.value = false
  }
}

// SQL新建表
const handleSQLCreate = async () => {
  try {
    if (!sqlCreateContent.value) {
      ElMessage.warning('请输入SQL语句')
      return
    }

    if (!currentGroupId.value) {
      ElMessage.warning('分组ID不能为空')
      return
    }

    sqlCreateLoading.value = true
    await createTableBySQL(String(currentGroupId.value), sqlCreateContent.value)

    ElMessage.success('创建成功')
    sqlCreateDialogVisible.value = false
    sqlCreateContent.value = ''  // 清空 SQL 内容
    loadTableList(currentGroupId.value, true)  // 刷新列表
  } catch (error) {
    ElMessage.error('创建失败: ' + (error.message || '未知错误'))
  } finally {
    sqlCreateLoading.value = false
  }
}

// 文件上传前检查
const beforeFileUpload = (file) => {
  const isValidType = acceptFileTypes.split(',').some(type =>
      file.name.toLowerCase().endsWith(type.substring(1))
  )
  if (!isValidType) {
    ElMessage.error('不支持的文件类型')
    return false
  }
  return true
}

// 文件上传成功
const handleFileUploadSuccess = (response) => {
  ElMessage.success('上传成功')
  fileCreateDialogVisible.value = false
  // 刷新表格列表
  loadTableList(currentGroupId.value, true)
}

// 文件上传失败
const handleFileUploadError = () => {
  ElMessage.error('上传失败')
}


// 处理空模板下载
const handleEmptyTemplateDownload = async (format) => {
  try {
    ElMessage.info('正在生成模板...')
    const response = await downloadEmptyTemplate(format)
    await handleFileDownload(response, format, 'template')
  } catch (error) {
    console.error('下载模板失败:', error)
    const errorMsg = error.response?.data?.msg || error.message || '下载模板失败'
    ElMessage.error(errorMsg)
  }
}

// 统一的文件下载处理函数
const handleFileDownload = async (response, format, filename) => {
  try {
    // 检查响应是否有效
    if (!response || response.size === 0) {
      throw new Error('下载失败：文件内容为空')
    }

    // 获取文件配置
    const fileConfig = downloadButtons.find(btn => btn.format === format)
    if (!fileConfig) {
      throw new Error('不支持的文件格式')
    }

    // 检查响应类型
    const contentType = response.type.toLowerCase()

    // 如果是JSON格式（错误响应）
    if (contentType.includes('application/json')) {
      const text = await response.text()
      const data = JSON.parse(text)
      throw new Error(data.msg || '下载失败')
    }

    // 根据不同格式处理响应数据
    let downloadData = response
    let downloadFilename = `${filename}.${fileConfig.extension}`

    // 对于文本类型的响应，确保正确的编码
    if (format === 'txt' || format === 'sql' || format === 'csv') {
      const text = await response.text()
      const bom = new Uint8Array([0xEF, 0xBB, 0xBF]) // UTF-8 BOM
      downloadData = new Blob([bom, text], {
        type: `${fileConfig.mimeType};charset=utf-8`
      })
    }

    // 创建下载链接
    const url = window.URL.createObjectURL(downloadData)
    const link = document.createElement('a')
    link.href = url
    link.download = downloadFilename

    // 触发下载
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)

    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载处理失败:', error)
    ElMessage.error(error.message || '下载模板失败')
    throw error
  }
}

const handleEditTable = async (row) => {
  try {
    // 获取表详情
    const {data} = await getTableDetail(row.id)
    if (!data) {
      ElMessage.warning('获取表详情失败')
      return
    }

    // 设置表单数据
    tableForm.id = data.id
    tableForm.name = data.name
    tableForm.displayName = data.displayName
    tableForm.fields = data.fields || []

    // 切换到编辑模式
    editingId.value = row.id  // 设置正在编辑的表 ID
    showEditor.value = true

    // 更新SQL内容
    nextTick(() => {
      sqlContent.value = generateSQL(tableForm)
    })
  } catch (error) {
    console.error('获取表详情失败:', error)
    ElMessage.error('获取表详情失败: ' + (error.message || '未知错误'))
  }
}

const handleViewTable = async (row) => {
  try {
    // 获取表详情
    const {data} = await getTableDetail(row.id)
    if (!data) {
      ElMessage.warning('获取表详情失败')
      return
    }

    // 设置查看数据
    viewingTable.value = data
    viewDialogVisible.value = true
  } catch (error) {
    console.error('获取表详情失败:', error)
    ElMessage.error('获取表详情失败: ' + (error.message || '未知错误'))
  }
}

const handleDeleteTable = (row) => {
  ElMessageBox.confirm(
      `确定要删除表"${row.name}"吗？删除后无法恢复！`,
      '警告',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(async () => {
    try {
      await deleteTable(row.id)
      ElMessage.success('删除成功')
      // 刷新表格列表
      loadTableList(currentGroupId.value, true)
    } catch (error) {
      console.error('删除表失败:', error)
      ElMessage.error('删除失败: ' + (error.message || '未知错误'))
    }
  }).catch(() => {
  })
}

// 在现有的搜索区域或表格上方添加这段代码
const showAdvanced = ref(false)
const queryParams = reactive({
  name: '',
  displayName: '',
  createTimeStart: '',
  createTimeEnd: '',
  updateTimeStart: '',
  updateTimeEnd: '',
  minFieldCount: null,
  maxFieldCount: null
})
const dateRange = reactive({
  createTime: [],
  updateTime: []
})

const handleQuery = () => {
  // 处理日期范围
  if (dateRange.createTime && dateRange.createTime.length === 2) {
    queryParams.createTimeStart = dateRange.createTime[0];
    queryParams.createTimeEnd = dateRange.createTime[1];
  } else {
    queryParams.createTimeStart = '';
    queryParams.createTimeEnd = '';
  }

  if (dateRange.updateTime && dateRange.updateTime.length === 2) {
    queryParams.updateTimeStart = dateRange.updateTime[0];
    queryParams.updateTimeEnd = dateRange.updateTime[1];
  } else {
    queryParams.updateTimeStart = '';
    queryParams.updateTimeEnd = '';
  }

  // 重置到第一页
  tablePagination.pageNum = 1;
  // 调用您现有的获取数据方法
  loadTableList(currentGroupId.value, true); // 或者您的其他获取数据方法名
}

const resetQuery = () => {
  this.queryParams = {
    name: '',
    displayName: '',
    createTimeStart: '',
    createTimeEnd: '',
    updateTimeStart: '',
    updateTimeEnd: '',
    minFieldCount: null,
    maxFieldCount: null
  };
  this.dateRange = {
    createTime: [],
    updateTime: []
  };
  this.handleQuery();
}

// 加载主题域选项
const loadDomainOptions = async () => {
  try {
    const res = await getDomainList()
    if (res && res.code === 0) {
      domainOptions.value = res.data || []
    } else {
      ElMessage.error(res?.message || '获取主题域列表失败')
    }
  } catch (error) {
    console.error('获取主题域列表失败:', error)
    ElMessage.error('获取主题域列表失败')
  }
}

// 处理建议标准的选择变化
const handleSuggestedStandardsSelectionChange = (selection) => {
  console.log('建议执行的标准选择变化:', selection)

  // 更新选中的建议执行标准
  selectedSuggestedStandards.value = selection.map(item => item.id)

  // 更新选中的建议执行标准对象映射
  selectedSuggestedStandardsMap.value = {}
  selection.forEach(item => {
    selectedSuggestedStandardsMap.value[item.id] = item
  })

  // 打印选中的标准信息，便于调试
  console.log('当前选中的建议执行标准IDs:', selectedSuggestedStandards.value)
  console.log('当前选中的建议执行标准对象:', selectedSuggestedStandardsMap.value)
  console.log('当前选中的建议执行标准对象数量:', Object.keys(selectedSuggestedStandardsMap.value).length)
}

// 获取标准类型的显示文本
const getStandardTypeText = (type) => {
  if (!type) return '未知'

  const typeMap = {
    'required': '必须执行',
    'suggested': '建议执行',
    'NAMING': '命名规范',
    'STRUCTURE': '结构规范',
    'DATA': '数据规范',
    'QUALITY': '质量规范',
    'DOMAIN': '主题域规范'
  }

  return typeMap[type] || type
}

// 应用标准
const handleApplyStandards = async () => {
  try {
    // ... 前置操作 ...

    // 调用 API
    const res = await applyModelStandards(params)

    // 确保 res 存在且有 code 属性
    if (res && typeof res.code !== 'undefined') {
      if (res.code === 0) {
        // 成功处理
        ElMessage.success('应用标准成功')
        // ... 其他成功操作 ...
      } else {
        // 失败处理
        ElMessage.error(res.message || '应用标准失败')
      }
    } else {
      // 处理无效响应
      ElMessage.error('应用标准失败: 无效的响应')
    }
  } catch (error) {
    // 错误处理
    console.error('应用标准出错:', error)
    ElMessage.error('应用标准失败: ' + (error.message || error))
  }
}

// 在组件挂载时进行测试
onMounted(() => {
  console.log('组件已挂载，进行测试请求')

  // 测试 API 调用
  getDomainList()
      .then(res => {
        console.log('测试 API 调用成功:', res)
      })
      .catch(error => {
        console.error('测试 API 调用失败:', error)
      })
})

// 监听对话框可见性变化
watch(standardsDialogVisible, (newVal) => {
  if (newVal) {
    console.log('对话框已打开，获取左侧树选中节点信息')
    console.log('左侧树选中节点:', currentParentNode)

    // 确保每次打开对话框时默认显示"选择标准"标签页
    standardsActiveTab.value = 'select'

    // 成功获取到主题域信息
    currentDomainId.value = currentParentNode.value.id
    currentDomainName.value = currentParentNode.value.name
    console.log('设置主题域信息:', currentDomainId.value, currentDomainName.value)

    // 如果是从左侧树选择的，可能没有选择具体模型，此时可以设置一个默认值或不设置

    // 加载主题域详情和标准
    loadCurrentDomainInfo()
    loadAppliedStandards()

  }
})

// 监听标签页变化
watch(standardsActiveTab, (newVal, oldVal) => {
  console.log('标签页切换到:', newVal)
  if (newVal === 'applied') {
    loadAppliedStandards()
  } else if (newVal === 'select') {
    loadDomainOptions(currentDomainId)
  } else if (newVal === 'configure') {
    console.log('重置应用范围为默认值')
    applyScope.value = 'all'
    applyDescription.value = ''
  }
})

// 处理标签页切换
const handleTabChange = (tabName) => {
  console.log('标签页切换事件触发:', tabName)
  standardsActiveTab.value = tabName
}

// 添加测试方法
const testApiCall = () => {
  console.log('测试 API 调用')

  // 使用 getDomainList 函数发送请求
  getDomainList()
      .then(res => {
        console.log('API 调用成功:', res)
        ElMessage.success('API 调用成功')
      })
      .catch(error => {
        console.error('API 调用失败:', error)
        ElMessage.error('API 调用失败')
      })
}

// 获取左侧树当前选中的节点
const getSelectedTreeNode = () => {
  console.log('尝试获取左侧树选中节点')

  // 方法1: 从组件内部存储的当前选中节点获取
  try {
    if (typeof currentNode !== 'undefined' && currentNode.value) {
      console.log('从组件内部变量currentNode获取到树节点:', currentNode.value)
      return currentNode.value
    }
  } catch (e) {
    console.warn('从currentNode获取树节点失败:', e)
  }

  // 方法2: 从全局变量获取
  try {
    if (window.currentNode) {
      console.log('从全局变量currentNode获取到树节点:', window.currentNode)
      return window.currentNode
    }

    if (window.currentSelectedNode) {
      console.log('从全局变量currentSelectedNode获取到树节点:', window.currentSelectedNode)
      return window.currentSelectedNode
    }
  } catch (e) {
    console.warn('从全局变量获取树节点失败:', e)
  }

  // 方法3: 从本地存储获取
  try {
    const savedNode = localStorage.getItem('currentTreeNode')
    if (savedNode) {
      const parsedNode = JSON.parse(savedNode)
      console.log('从本地存储获取到树节点:', parsedNode)
      return parsedNode
    }
  } catch (e) {
    console.warn('从本地存储获取树节点失败:', e)
  }

}

// 添加一个方法，用于在树节点选择变化时保存选中节点
const handleTreeNodeSelect = (node) => {
  console.log('树节点选择变化:', node)

  // 保存到本地变量
  if (typeof currentTreeNode !== 'undefined') {
    currentTreeNode.value = node
  }

  // 保存到本地存储（可选）
  try {
    localStorage.setItem('selectedTreeNode', JSON.stringify(node))
  } catch (e) {
    console.warn('保存树节点到本地存储失败:', e)
  }
}

// 在组件挂载时，尝试添加事件监听器
onMounted(() => {

  console.log('组件挂载，尝试添加树节点选择事件监听器')

  // 尝试找到树组件并添加事件监听器
  try {
    const treeComponent = document.querySelector('.el-tree')
    if (treeComponent) {
      console.log('找到树组件，添加事件监听器')

      // 使用MutationObserver监听树节点的变化
      const observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
            const target = mutation.target
            if (target.classList.contains('is-current')) {
              console.log('检测到树节点选择变化:', target)

              // 从DOM节点提取信息
              const nodeId = target.dataset.nodeId
              const nodeName = target.textContent.trim()
              const nodeType = target.dataset.nodeType || 'unknown'

              if (nodeId) {
                handleTreeNodeSelect({
                  id: nodeId,
                  name: nodeName,
                  type: nodeType
                })
              }
            }
          }
        })
      })

      observer.observe(treeComponent, {
        attributes: true,
        subtree: true,
        attributeFilter: ['class']
      })
    }

  } catch (e) {
    console.warn('添加树节点选择事件监听器失败:', e)
  }
})

// 从选中节点获取主题域节点（可能是节点本身或其父节点）
const getDomainNodeFromSelection = (selectedNode) => {
  if (!selectedNode) return null

  // 如果选中的节点本身就是domain类型
  if (selectedNode.type === 'domain') {
    return selectedNode
  }

  // 如果选中的节点是detail类型，需要查找其父节点
  if (selectedNode.type === 'detail') {
    // 尝试从树数据中查找父节点
    try {
      const treeData = treeList.value || [] // 请替换为实际存储树数据的变量

      // 查找节点的父节点
      const findParentNode = (nodes, targetId, parent = null) => {
        for (const node of nodes) {
          if (node.id === targetId) {
            return parent
          }
          if (node.children && node.children.length > 0) {
            const found = findParentNode(node.children, targetId, node)
            if (found) return found
          }
        }
        return null
      }

      const parentNode = findParentNode(treeData, selectedNode.id)
      if (parentNode && parentNode.type === 'domain') {
        console.log('找到父节点:', parentNode)
        return parentNode
      }
    } catch (e) {
      console.warn('查找父节点失败:', e)
    }
  }

  // 如果无法找到父节点，返回null
  console.warn('无法找到domain类型的父节点')
  return null
}

/**
 * 加载当前主题域信息
 * @param {string} domainId 主题域ID
 * @returns {Promise<boolean>} 是否成功加载
 */
const loadCurrentDomainInfo = async (domainId) => {
  if (!domainId) {
    console.warn('加载主题域信息失败: 主题域ID为空');
    return false;
  }

  try {
    console.log('加载主题域信息，参数:', {domainId});

    // 设置当前主题域ID
    currentDomainId.value = domainId;

    // 加载主题域标准
    const success = await loadDomainStandards(domainId);

    return success;
  } catch (error) {
    console.error('加载主题域信息出错:', error);
    ElMessage.error('加载主题域信息失败: ' + (error.message || error));
    return false;
  }
};

// 定义props（如果您的组件接收props）
const props = defineProps({
  // 如果您的组件接收currentNode作为prop，请取消下面的注释
  // currentNode: Object,
  // 其他props...
})

// 调试相关
const debugInfo = ref('')

// 测试获取树节点
const debugTreeNode = () => {
  const node = getSelectedTreeNode()
  debugInfo.value = JSON.stringify(node, null, 2)
  console.log('调试获取树节点结果:', node)
}


// 定义响应式变量
const currentNode = ref(null) // 当前选中的树节点

// 查看标准详情
const viewStandardDetails = (standard) => {
  console.log('查看标准详情:', standard)

  // 这里可以打开一个对话框显示标准详情
  ElMessageBox.alert(
      `<div>
      <h3>${standard.name}</h3>
      <p>${standard.description || '暂无描述'}</p>
      <p><strong>类型:</strong> ${standard.type === 'required' ? '必须执行' : '建议执行'}</p>
      <p><strong>所属主题域:</strong> ${standard.domainName || currentDomainName}</p>
    </div>`,
      '标准详情',
      {
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭'
      }
  )
}

// 标准选择相关
const suggestedStandardsTable = ref(null) // 建议执行的标准表格引用
const selectedStandards = ref([]) // 存储选中的标准ID
const selectedStandardsMap = ref({}) // 存储选中的标准对象，便于快速查找
const selectedSuggestedStandards = ref([]) // 存储选中的建议执行标准ID
const selectedSuggestedStandardsMap = ref({}) // 初始化为空对象而不是null
const currentSelectedSuggested = ref([]) // 存储当前选中的建议执行标准行

// 前往下一步
const goToNextStep = () => {
  try {
    // 获取选中标准总数
    const totalSelected = getTotalSelectedStandards()

    if (totalSelected === 0) {
      ElMessage.warning('请至少选择一个标准')
      return
    }

    console.log('前往下一步，当前选中的标准总数:', totalSelected)
    console.log('必须执行的标准:', requiredStandards.value)
    console.log('必须执行的标准数量:', requiredStandards.value.length)
    console.log('选中的建议执行标准IDs:', selectedSuggestedStandards.value)
    console.log('选中的建议执行标准数量:', selectedSuggestedStandards.value.length)
    console.log('当前保存的选中行数量:', currentSelectedSuggested.value ? currentSelectedSuggested.value.length : 0)

    // 先检查已保存的选中行
    if (currentSelectedSuggested.value && currentSelectedSuggested.value.length > 0) {
      console.log('使用已保存的选中行数据，数量:', currentSelectedSuggested.value.length)

      // 使用已保存的选中数据
      selectedSuggestedStandards.value = currentSelectedSuggested.value.map(item => item.id)

      // 更新选中的建议执行标准对象映射
      selectedSuggestedStandardsMap.value = {}
      currentSelectedSuggested.value.forEach(item => {
        selectedSuggestedStandardsMap.value[item.id] = item
      })
    }
    // 如果没有保存的选中行，尝试从表格获取
    else if (suggestedStandardsTable.value) {
      console.log('尝试从表格获取选中行...')
      const selection = suggestedStandardsTable.value.getSelectionRows()
      console.log('表格当前选中行:', selection)

      if (selection && selection.length > 0) {
        console.log('从表格获取到选中行，数量:', selection.length)

        // 保存到currentSelectedSuggested
        currentSelectedSuggested.value = [...selection]

        // 更新选中的建议执行标准
        selectedSuggestedStandards.value = selection.map(item => item.id)

        // 更新选中的建议执行标准对象映射
        selectedSuggestedStandardsMap.value = {}
        selection.forEach(item => {
          selectedSuggestedStandardsMap.value[item.id] = item
        })
      } else {
        console.warn('表格没有选中行，但selectedSuggestedStandards有值，尝试恢复数据')

        // 如果selectedSuggestedStandards有值但表格没有选中行，尝试从suggestedStandards中找到匹配的数据
        if (selectedSuggestedStandards.value && selectedSuggestedStandards.value.length > 0 &&
            suggestedStandards.value && suggestedStandards.value.length > 0) {
          const matchedRows = suggestedStandards.value.filter(item =>
              selectedSuggestedStandards.value.includes(item.id)
          )

          if (matchedRows.length > 0) {
            console.log('找到匹配的行数据，数量:', matchedRows.length)
            currentSelectedSuggested.value = [...matchedRows]

            // 重建映射
            selectedSuggestedStandardsMap.value = {}
            matchedRows.forEach(item => {
              selectedSuggestedStandardsMap.value[item.id] = item
            })
          }
        }
      }
    }

    console.log('已更新选中的建议执行标准:', selectedSuggestedStandards.value)
    console.log('已更新选中的建议执行标准对象:', selectedSuggestedStandardsMap.value)
    console.log('已更新选中的建议执行标准对象数量:', Object.keys(selectedSuggestedStandardsMap.value).length)

    // 在切换到配置标签页前，确认数据已正确保存
    const selectedSuggestedObjects = getSelectedSuggestedStandardsObjects()
    console.log('即将在配置页面显示的建议执行标准对象:', selectedSuggestedObjects)
    console.log('建议执行标准对象数量:', selectedSuggestedObjects.length)

    // 切换到配置标签页
    standardsActiveTab.value = 'configure'
  } catch (error) {
    console.error('前往下一步出错:', error)
    ElMessage.error('操作失败: ' + (error.message || '未知错误'))
  }
}

// 应用选中的标准
const applySelectedStandards = async () => {
  try {
    console.log('开始应用选中的标准')

    // 获取所有选中的标准
    const standards = getAllSelectedStandardsList()
    console.log('获取到的标准列表:', standards)

    if (!standards || standards.length === 0) {
      console.warn('没有选中的标准')
      ElMessage.warning('请至少选择一个标准')
      return
    }

    // 构建请求参数
    const params = {
      modelId: currentModelId.value,
      standards: standards,
      applyScope: applyScope.value,
      description: applyDescription.value || ''
    }

    console.log('应用标准请求参数:', JSON.stringify(params))

    loading.value = true
    // 更新应用进度
    applyProgress.value = 30
    standardsActiveTab.value = 'result' // 先切换到结果页面显示进度
    console.log('调用 applyModelStandards API')
    const res = await applyModelStandards(params)
    console.log('API 响应:', res)

    // 更新应用进度
    applyProgress.value = 80

    // 添加空值检查
    if (res && res.code === 0) {
      console.log('应用标准成功')
      ElMessage.success('应用标准成功')

      // 更新应用进度
      applyProgress.value = 100
      standardsApplied.value = true

      // 短暂延迟后切换到验证标签页
      setTimeout(() => {
        standardsActiveTab.value = 'validation' // 切换到验证标签页
        // 自动开始验证
        validateAppliedStandards()
      }, 1000)

      // 如果需要刷新数据，确保相关函数调用也有空值检查
      if (typeof refreshData === 'function') {
        refreshData()
      }

      return true // 返回成功状态
    } else {
      console.error('应用标准失败:', res ? res.message : '未知错误')
      ElMessage.error((res && res.message) || '应用标准失败')
      applyProgress.value = 0
      standardsApplied.value = false
      return false // 返回失败状态
    }
  } catch (error) {
    console.error('应用标准出错:', error)
    ElMessage.error('应用标准失败: ' + (error.message || error))
    applyProgress.value = 0
    standardsApplied.value = false
    return false // 返回失败状态
  } finally {
    loading.value = false
  }
}

// 监听标签页切换
watch(standardsActiveTab, (newVal, oldVal) => {
  console.log('标签页切换:', oldVal, '->', newVal)

  try {
    // 如果从选择标准切换到其他标签页，记录当前选中的建议执行标准
    if (oldVal === 'select') {
      console.log('从选择标准页面切换，保存当前选中状态')

      if (suggestedStandardsTable.value) {
        const selection = suggestedStandardsTable.value.getSelectionRows()
        console.log('保存当前选中的建议执行标准, 表格数据:', selection)

        if (selection && selection.length > 0) {
          // 保存到currentSelectedSuggested
          currentSelectedSuggested.value = [...selection]

          // 更新选中的建议执行标准
          selectedSuggestedStandards.value = selection.map(item => item.id)

          // 更新选中的建议执行标准对象映射
          selectedSuggestedStandardsMap.value = {}
          selection.forEach(item => {
            selectedSuggestedStandardsMap.value[item.id] = item
          })

          console.log('已保存选中数据，数量:', selection.length)
        } else if (currentSelectedSuggested.value.length > 0) {
          console.log('表格选中为空，使用之前保存的选中数据, 数量:', currentSelectedSuggested.value.length)

          // 使用之前保存的选中数据
          selectedSuggestedStandards.value = currentSelectedSuggested.value.map(item => item.id)

          // 更新选中的建议执行标准对象映射
          selectedSuggestedStandardsMap.value = {}
          currentSelectedSuggested.value.forEach(item => {
            selectedSuggestedStandardsMap.value[item.id] = item
          })
        } else {
          console.warn('没有选中的建议执行标准')
        }
      } else {
        console.warn('无法获取表格引用')
      }
    }
  } catch (error) {
    console.error('监听标签页切换出错:', error)
  }
})


// 获取所有选中的标准列表（必须执行的标准 + 选中的建议执行标准）
const getAllSelectedStandardsList = () => {
  console.log('获取所有选中的标准列表')

  // 确保 requiredStandards 存在且是数组
  const required = Array.isArray(requiredStandards.value)
      ? requiredStandards.value.map(item => {
        // 确保每个必须执行的标准是一个对象
        if (typeof item === 'string') {
          // 如果是字符串（可能是ID），尝试从缓存中获取完整对象
          return {
            id: item,
            standardType: 'required',
            // 添加其他必要的默认属性
            name: '必须执行标准',
            type: 'NAMING'
          }
        } else if (typeof item === 'object' && item !== null) {
          // 确保标准类型正确
          return {...item, standardType: 'required'}
        }
        return null
      }).filter(Boolean)
      : []

  // 获取选中的建议执行标准对象
  const selectedSuggested = Array.isArray(selectedSuggestedStandards.value)
      ? selectedSuggestedStandards.value
          .map(id => {
            // 确保 selectedSuggestedStandardsMap.value 存在
            if (!selectedSuggestedStandardsMap.value) {
              console.warn('selectedSuggestedStandardsMap 未定义')
              return null
            }

            const standard = selectedSuggestedStandardsMap.value[id]
            if (standard) {
              // 确保标准类型正确
              return {...standard, standardType: 'suggested'}
            }
            return null
          })
          .filter(Boolean)
      : []

  // 合并两个数组
  const allSelected = [...required, ...selectedSuggested]

  console.log('必须执行的标准数量:', required.length)
  console.log('选中的建议执行标准数量:', selectedSuggested.length)
  console.log('总选中的标准数量:', allSelected.length)

  // 调试输出，检查每个标准对象的格式
  console.log('必须执行的标准:', JSON.stringify(required))
  console.log('选中的建议执行标准:', JSON.stringify(selectedSuggested))
  console.log('所有选中的标准:', JSON.stringify(allSelected))

  return allSelected
}

// 获取选中标准的总数
const getTotalSelectedStandards = () => {
  // 确保 requiredStandards 存在且是数组
  const requiredCount = Array.isArray(requiredStandards.value) ? requiredStandards.value.length : 0

  // 确保 selectedSuggestedStandards 存在且是数组
  const suggestedCount = Array.isArray(selectedSuggestedStandards.value) ? selectedSuggestedStandards.value.length : 0

  const total = requiredCount + suggestedCount
  console.log('计算选中标准总数:', {
    requiredCount,
    suggestedCount,
    total
  })

  return total
}

// 清空选中的建议执行标准
const clearSelectedSuggestedStandards = () => {
  if (suggestedStandardsTable.value) {
    suggestedStandardsTable.value.clearSelection()
  }
  selectedSuggestedStandards.value = []
  selectedSuggestedStandardsMap.value = {}
}

// 移除选中的建议执行标准
const removeSelectedSuggestedStandard = (standard) => {
  const index = selectedSuggestedStandards.value.indexOf(standard.id)
  if (index !== -1) {
    selectedSuggestedStandards.value.splice(index, 1)
    delete selectedSuggestedStandardsMap.value[standard.id]

    // 如果返回到选择标准页面，需要更新表格的选中状态
    if (suggestedStandardsTable.value) {
      // 找到对应的行，取消选中
      const rowIndex = suggestedStandards.value.findIndex(item => item.id === standard.id)
      if (rowIndex !== -1) {
        suggestedStandardsTable.value.toggleRowSelection(suggestedStandards.value[rowIndex], false)
      }
    }
  }
}


// 标准应用配置
const applyScope = ref('all') // 应用范围：all-所有字段，selected-选中字段
const applyDescription = ref('') // 应用说明

// 监听应用范围变化
watch(applyScope, (newVal, oldVal) => {
  console.log('应用范围变化:', oldVal, '->', newVal)
})

// 在<script setup>部分添加以下变量
const appliedRequiredStandards = ref([])
const appliedSuggestedStandards = ref([])


// 在标准对话框打开时加载已应用的标准
watch(() => standardsDialogVisible.value, (newVal) => {
  if (newVal) {
    // 添加加载已应用标准的调用
    loadAppliedStandards()
  }
})

// 添加日期格式化函数
const formatDate = (date) => {
  if (!date) return '';

  if (typeof date === 'string') {
    return date;
  }

  const d = new Date(date);
  const year = d.getFullYear();
  const month = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  const hours = String(d.getHours()).padStart(2, '0');
  const minutes = String(d.getMinutes()).padStart(2, '0');
  const seconds = String(d.getSeconds()).padStart(2, '0');

  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
};
// 标准验证相关变量
const activeTab = ref('select') // 如果已存在，不需要重复定义
const validationResults = ref([])
const validationLoading = ref(false)
const validationDetailsVisible = ref(false)
const currentValidationDetails = ref(null)
const activeValidations = ref([0]) // 默认展开第一个验证结果


// 在验证前检查是否有应用的标准
const validateAppliedStandards = async () => {
  if (!currentModelId.value) {
    ElMessage.warning('请先选择模型');
    return;
  }

  // 如果还没有加载已应用的标准，先加载
  if (!appliedStandards.value) {
    await loadAppliedStandards(currentModelId.value);
  }


  try {
    validationLoading.value = true;
    console.log('开始验证标准，模型ID:', currentModelId.value);

    // 清除之前的验证结果
    validationResults.value = [];

    // 调用 API
    console.log('调用 validateModelStandards API');
    const res = await validateModelStandards(currentModelId.value);
    console.log('验证标准响应:', res);

    // 添加空值检查
    if (res && res.code === 0) {
      validationResults.value = res.data || [];

      if (validationResults.value.length === 0) {
        ElMessage.info('验证完成，未发现任何标准验证结果');
      } else {
        ElMessage.success('标准验证完成');
        // 默认展开第一个验证结果
        activeValidations.value = [0];
      }
    } else {
      console.error('验证标准失败:', res ? res.message : '未知错误');
      ElMessage.error((res && res.message) || '验证标准失败');
    }
  } catch (error) {
    console.error('验证标准出错:', error);
    ElMessage.error('验证标准失败: ' + (error.message || error));
  } finally {
    validationLoading.value = false;
  }
};

// 计算属性：检查是否有应用的标准
const hasAppliedStandards = computed(() => {
  if (!appliedStandards.value) {
    return false;
  }
  
  const hasRequired = appliedStandards.value.required && appliedStandards.value.required.length > 0
  const hasSuggested = appliedStandards.value.suggested && appliedStandards.value.suggested.length > 0
  
  return hasRequired || hasSuggested;
});

// 计算属性：通过的标准数量
const passedStandardsCount = computed(() => {
  return validationResults.value.filter(result => result.status).length;
});

// 计算属性：未通过的标准数量
const failedStandardsCount = computed(() => {
  return validationResults.value.filter(result => !result.status).length;
});

// 计算属性：问题总数
const totalIssuesCount = computed(() => {
  return validationResults.value.reduce((total, result) => {
    return total + (result.issues ? result.issues.length : 0);
  }, 0);
});

// 获取严重程度对应的类型
const getSeverityType = (severity) => {
  switch (severity) {
    case '高':
      return 'danger';
    case '中':
      return 'warning';
    case '低':
      return 'info';
    default:
      return '';
  }
};


// 加载模型详情
const loadModelDetail = async (modelId) => {
  try {
    // 加载已应用的标准
    await loadAppliedStandards(modelId);

  } catch (error) {
    console.error('加载模型详情失败:', error);
    ElMessage.error('加载模型详情失败: ' + (error.message || error));
  }
};

// 已应用的标准
const appliedStandards = ref(null);

// 添加加载已应用标准的函数
const loadAppliedStandards = async () => {
  if (!currentModelId.value) {
    console.warn('未选择模型，无法加载已应用标准')
    return
  }

  loading.value = true
  try {
    console.log('加载已应用标准，模型ID:', currentModelId.value)

    const res = await getAppliedStandards(currentModelId.value)
    console.log('已应用标准API返回数据:', res)

    if (res.code === 0 && res.data) {
      // 直接使用API返回的数据结构
      appliedStandards.value = res.data
      
      // 提取required和suggested数组
      appliedRequiredStandards.value = res.data.required || []
      appliedSuggestedStandards.value = res.data.suggested || []

      console.log('已应用的必须执行标准数量:', appliedRequiredStandards.value.length)
      console.log('已应用的建议执行标准数量:', appliedSuggestedStandards.value.length)
    } else {
      console.error('获取已应用标准失败:', res.message)
    }
  } catch (error) {
    console.error('加载已应用标准出错:', error)
  } finally {
    loading.value = false
  }
}

// 显示已应用标准详情
const showAppliedStandardsDetails = ref(false);

// 获取已应用标准总数
const getAppliedStandardsCount = () => {
  if (!appliedStandards.value) {
    return 0;
  }

  const requiredCount = (appliedStandards.value.required || []).length
  const suggestedCount = (appliedStandards.value.suggested || []).length
  return requiredCount + suggestedCount;
};

/**
 * 清除缓存数据
 */
const clearCacheData = () => {
  console.log('清除缓存数据');

  // 清除验证结果
  validationResults.value = [];

  // 清除已应用的标准缓存
  appliedStandards.value = null;

  // 清除可用标准缓存
  availableStandards.value = {};
  totalRequiredStandards.value = 0;
  totalSuggestedStandards.value = 0;

  // 清除主题域标准缓存
  domainStandards.value = {};

  // 清除选中的标准
  selectedStandardsMap.value = {};

  // 重置应用范围和描述
  applyScope.value = 'all';
  applyDescription.value = '';
};

// 添加这些处理函数到 methods 部分
// 添加必须执行的标准
const handleAddRequiredStandard = () => {
  // 显示新增标准对话框
  showAddStandardDialog('required')
}

// 添加建议执行的标准
const handleAddSuggestedStandard = () => {
  // 显示新增标准对话框
  showAddStandardDialog('suggested')
}

// 显示新增标准对话框
const showAddStandardDialog = (standardType) => {
  console.log('显示新增标准对话框, 类型:', standardType);
  addStandardType.value = standardType;
  selectedNewStandards.value = []; // 清空选中的标准
  standardSearchKeyword.value = ''; // 清空搜索关键词
  addStandardDialogVisible.value = true;

  // 加载所有标准规则
  loadAllStandardRules().then(() => {
    // 加载完成后显示调试信息
    setTimeout(debugShowAllRules, 500);
  });
}

// 加载所有标准规则
const loadAllStandardRules = () => {
  addStandardLoading.value = true;
  allStandardRules.value = []; // 每次加载前清空

  console.log('正在获取所有标准规则...');

  // 返回Promise对象，以便于链式调用
  return new Promise((resolve, reject) => {
    // 调用标准规则API，获取所有规则
    getRuleObjectsWithoutObjectId()
        .then(res => {
          if (res && res.code === 0) {
            console.log('获取所有标准规则成功, 数据条数:', res.data?.length);
            // 保存所有规则
            allStandardRules.value = res.data || [];

            // 检查数据是否正确
            console.log('allStandardRules 数据:', allStandardRules.value);
            console.log('过滤后的数据:', filteredStandardRules.value);

            resolve(res.data);
          } else {
            console.error('获取所有标准规则失败:', res?.message || '未知错误');
            ElMessage.error(res?.message || '获取所有标准规则失败');
            allStandardRules.value = [];
            reject(new Error(res?.message || '获取所有标准规则失败'));
          }
        })
        .catch(error => {
          console.error('获取所有标准规则出错:', error);
          ElMessage.error('获取所有标准规则出错: ' + (error.message || '未知错误'));
          allStandardRules.value = [];
          reject(error);
        })
        .finally(() => {
          addStandardLoading.value = false;
        });
  });
}

// 选择标准
const handleSelectStandard = (selection) => {
  selectedNewStandards.value = selection
}

// 确认添加标准
const confirmAddStandards = () => {
  if (selectedNewStandards.value.length === 0) {
    ElMessage.warning('请选择要添加的标准')
    return
  }

  // 根据标准类型，添加到不同的列表
  if (addStandardType.value === 'required') {
    // 添加到必须执行标准列表，避免重复添加
    selectedNewStandards.value.forEach(standard => {
      // 确保标准被标记为必须执行类型
      standard.standardType = 'required';

      // 只有在列表中不存在时才添加
      if (!requiredStandards.value.some(item => item.id === standard.id)) {
        requiredStandards.value.push(standard)
      }
    })
    totalRequiredStandards.value = requiredStandards.value.length
  } else {
    // 添加到建议执行标准列表，避免重复添加
    selectedNewStandards.value.forEach(standard => {
      // 确保标准被标记为建议执行类型
      standard.standardType = 'suggested';

      // 只有在列表中不存在时才添加
      if (!suggestedStandards.value.some(item => item.id === standard.id)) {
        suggestedStandards.value.push(standard)
      }
    })
    totalSuggestedStandards.value = suggestedStandards.value.length

    // 默认选中新添加的建议执行标准
    selectedNewStandards.value.forEach(standard => {
      if (!selectedSuggestedStandards.value.includes(standard.id)) {
        selectedSuggestedStandards.value.push(standard.id)
        selectedSuggestedStandardsMap.value[standard.id] = standard
      }
    })
  }

  // 打印日志，便于调试
  console.log('更新后的必须执行标准列表:', requiredStandards.value);
  console.log('更新后的建议执行标准列表:', suggestedStandards.value);

  // 关闭对话框
  addStandardDialogVisible.value = false
  ElMessage.success(`成功添加${selectedNewStandards.value.length}个标准`)
}

// 在 setup 函数中定义这些响应式变量
const addStandardDialogVisible = ref(false)
const addStandardType = ref('required') // 'required' 或 'suggested'
const allStandardRules = ref([])
const addStandardLoading = ref(false)
const selectedNewStandards = ref([])

// 添加搜索关键词变量
const standardSearchKeyword = ref('')

// 添加过滤后的标准规则计算属性
const filteredStandardRules = computed(() => {
  console.log('过滤标准规则, 类型:', addStandardType.value);
  console.log('allStandardRules 长度:', allStandardRules.value.length);

  // 如果没有数据，直接返回空数组
  if (!allStandardRules.value || allStandardRules.value.length === 0) {
    console.log('没有标准规则数据可过滤');
    return [];
  }

  // 首先根据类型过滤
  let filtered = allStandardRules.value;

  // 过滤已存在的标准
  if (addStandardType.value === 'required') {
    // 排除已在必须执行列表中的标准
    filtered = filtered.filter(rule =>
        !requiredStandards.value.some(item => item.id === rule.id)
    );
  } else {
    // 排除已在建议执行列表中的标准
    filtered = filtered.filter(rule =>
        !suggestedStandards.value.some(item => item.id === rule.id)
    );
  }

  // 然后根据搜索关键词过滤
  if (standardSearchKeyword.value) {
    const keyword = standardSearchKeyword.value.toLowerCase();
    filtered = filtered.filter(rule =>
        (rule.name && rule.name.toLowerCase().includes(keyword)) ||
        (rule.code && rule.code.toLowerCase().includes(keyword)) ||
        (rule.description && rule.description.toLowerCase().includes(keyword))
    );
  }

  console.log('过滤后的标准规则数量:', filtered.length);
  return filtered;
})

// 添加一个控制变量，用于显示建议执行标准部分
const showSuggestedStandards = ref(true)

// 打印API返回的所有标准规则
const debugShowAllRules = () => {
  console.log('---------- 调试信息 ----------');
  console.log('当前对话框类型:', addStandardType.value);
  console.log('allStandardRules 数据条数:', allStandardRules.value.length);

  if (allStandardRules.value.length > 0) {
    console.log('allStandardRules 第一条数据:', allStandardRules.value[0]);

    // 统计类型分布
    const typeCount = {};
    allStandardRules.value.forEach(rule => {
      const type = rule.type || rule.ruleType || '未知';
      typeCount[type] = (typeCount[type] || 0) + 1;
    });
    console.log('标准规则类型分布:', typeCount);
  }

  console.log('过滤后数据条数:', filteredStandardRules.value.length);
  console.log('已选中数据条数:', selectedNewStandards.value.length);
  console.log('-----------------------------');
}

// 获取选中的建议执行标准对象
const getSelectedSuggestedStandardsObjects = () => {
  console.log('获取选中的建议执行标准对象...')
  console.log('选中的建议执行标准IDs:', selectedSuggestedStandards.value)
  console.log('选中的建议执行标准对象Map:', selectedSuggestedStandardsMap.value)

  try {
    // 确保currentSelectedSuggested已定义
    if (typeof currentSelectedSuggested === 'undefined') {
      console.warn('currentSelectedSuggested未定义，请检查变量定义顺序')
      return []
    }

    console.log('当前保存的选中行:', currentSelectedSuggested.value)

    // 优先使用currentSelectedSuggested
    if (currentSelectedSuggested.value && Array.isArray(currentSelectedSuggested.value) && currentSelectedSuggested.value.length > 0) {
      console.log('使用currentSelectedSuggested返回选中对象，数量:', currentSelectedSuggested.value.length)
      return [...currentSelectedSuggested.value]
    }

    // 检查selectedSuggestedStandards是否已初始化
    if (!selectedSuggestedStandards.value || !Array.isArray(selectedSuggestedStandards.value) || selectedSuggestedStandards.value.length === 0) {
      console.warn('selectedSuggestedStandards未初始化、不是数组或为空')
      return []
    }

    // 检查selectedSuggestedStandardsMap是否已初始化
    if (!selectedSuggestedStandardsMap.value || typeof selectedSuggestedStandardsMap.value !== 'object' || Object.keys(selectedSuggestedStandardsMap.value).length === 0) {
      console.warn('selectedSuggestedStandardsMap未初始化、不是对象或为空')

      // 尝试从suggestedStandards中恢复数据
      if (suggestedStandards.value && suggestedStandards.value.length > 0) {
        console.log('尝试从suggestedStandards恢复数据...')
        const matchedRows = suggestedStandards.value.filter(item =>
            selectedSuggestedStandards.value.includes(item.id)
        )

        if (matchedRows.length > 0) {
          console.log('从suggestedStandards找到匹配的行，数量:', matchedRows.length)
          return matchedRows
        }
      }

      return []
    }

    // 使用map将ID转换为对象，并过滤掉无效的对象
    const result = selectedSuggestedStandards.value
        .map(id => selectedSuggestedStandardsMap.value[id])
        .filter(Boolean)

    console.log('从selectedSuggestedStandards和Map转换后的对象:', result)
    console.log('转换后对象数量:', result.length)

    return result
  } catch (error) {
    console.error('获取选中的建议执行标准对象出错:', error)
    return []
  }
}

// 添加调试函数
const debugShowSelectedStandards = () => {
  console.log('====== 调试: 当前选中标准信息 ======');
  console.log('必须执行标准:', requiredStandards.value);
  console.log('必须执行标准数量:', requiredStandards.value.length);
  console.log('建议执行标准IDs:', selectedSuggestedStandards.value);
  console.log('建议执行标准对象Map:', selectedSuggestedStandardsMap.value);

  // 获取建议执行标准对象
  const suggestedObjects = getSelectedSuggestedStandardsObjects();
  console.log('建议执行标准对象:', suggestedObjects);
  console.log('建议执行标准对象数量:', suggestedObjects.length);

  // 显示警告信息
  ElMessage({
    message: `选中标准情况: 必须执行${requiredStandards.value.length}个，建议执行${suggestedObjects.length}个`,
    type: 'warning',
    duration: 5000
  });

  console.log('================================');
}

// 指示是否为开发环境，用于显示调试信息
const isDevelopment = ref(true); // 开启调试信息

// 添加测试函数
const testSaveCurrentSelectedSuggested = () => {
  if (suggestedStandardsTable.value) {
    const selection = suggestedStandardsTable.value.getSelectionRows()
    console.log('测试: 保存当前选中的建议执行标准, 表格选中数:', selection.length)

    if (selection && selection.length > 0) {
      // 保存到currentSelectedSuggested
      currentSelectedSuggested.value = [...selection]

      // 更新其他状态
      selectedSuggestedStandards.value = selection.map(item => item.id)
      selectedSuggestedStandardsMap.value = {}
      selection.forEach(item => {
        selectedSuggestedStandardsMap.value[item.id] = item
      })

      // 显示保存结果
      ElMessage({
        message: `已保存当前选中的${selection.length}个建议执行标准`,
        type: 'success',
        duration: 5000
      })

      console.log('已保存currentSelectedSuggested:', currentSelectedSuggested.value)
      console.log('已更新selectedSuggestedStandards:', selectedSuggestedStandards.value)
      console.log('已更新selectedSuggestedStandardsMap:', selectedSuggestedStandardsMap.value)

      // 询问是否立即切换标签页测试
      ElMessageBox.confirm(
          '是否立即切换到配置标准页面测试数据保存？',
          '测试数据保存',
          {
            confirmButtonText: '是，立即切换',
            cancelButtonText: '否，稍后手动切换',
            type: 'info'
          }
      ).then(() => {
        // 用户点击确认，立即切换标签页
        console.log('用户选择立即切换标签页测试')
        standardsActiveTab.value = 'configure'
      }).catch(() => {
        console.log('用户选择稍后手动切换标签页')
      })
    } else {
      console.warn('表格没有选中行')
      ElMessage.warning('表格没有选中行，请先选择一些建议执行标准')
    }
  } else {
    console.warn('无法获取表格引用')
    ElMessage.error('无法获取表格引用')
  }
}

// 添加恢复选中数据函数
const restoreFromCurrentSelectedSuggested = () => {
  try {
    console.log('尝试从currentSelectedSuggested恢复数据')

    // 确保currentSelectedSuggested已定义
    if (typeof currentSelectedSuggested === 'undefined') {
      console.warn('currentSelectedSuggested未定义，请检查变量定义顺序')
      ElMessage.error('变量未定义，无法恢复数据')
      return
    }

    if (currentSelectedSuggested.value && currentSelectedSuggested.value.length > 0) {
      console.log('从currentSelectedSuggested恢复数据，数量:', currentSelectedSuggested.value.length)

      // 更新选中的建议执行标准
      selectedSuggestedStandards.value = currentSelectedSuggested.value.map(item => item.id)

      // 更新选中的建议执行标准对象映射
      selectedSuggestedStandardsMap.value = {}
      currentSelectedSuggested.value.forEach(item => {
        selectedSuggestedStandardsMap.value[item.id] = item
      })

      ElMessage.success(`已从保存的数据中恢复${currentSelectedSuggested.value.length}个建议执行标准`)

      // 刷新配置页面
      nextTick(() => {
        console.log('已更新选中的建议执行标准:', selectedSuggestedStandards.value)
        console.log('已更新选中的建议执行标准对象:', selectedSuggestedStandardsMap.value)
        console.log('已更新选中的建议执行标准对象数量:', Object.keys(selectedSuggestedStandardsMap.value).length)
      })
    } else {
      console.warn('currentSelectedSuggested为空，无法恢复数据')
      ElMessage.warning('没有保存的选中数据可供恢复')
    }
  } catch (error) {
    console.error('恢复选中数据出错:', error)
    ElMessage.error('恢复数据出错: ' + (error.message || '未知错误'))
  }
}

// 在 setup 函数中定义这些响应式变量
const createModelDialogVisible = ref(false)
const modelFormRef = ref(null)
const modelForm = reactive({
  id: null,
  name: '',
  type: 'detail',
  owner: '系统管理员',
  status: 'draft', 
  description: '',
  config: '',
  createTime: null,
  updateTime: null,
  version: 0
})
const modelFormRules = {
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ]
}
const submitLoading = ref(false)
const isEditingModel = computed(() => {
  const isEditing = !!modelForm.id;
  console.log('isEditingModel computed:', isEditing, 'modelForm.id =', modelForm.id);
  return isEditing;
}) // 判断是否为编辑模式

// 提交新建模型
const submitCreateModel = async () => {
  if (!modelFormRef.value) return
  
  try {
    // 表单校验
    await modelFormRef.value.validate()
    
    submitLoading.value = true
    
    let res;
    
    if (isEditingModel.value) {
      // 编辑模式，确保ID存在
      if (!modelForm.id) {
        console.error('编辑模式但ID为空，这是一个错误');
        ElMessage.error('模型ID缺失，无法更新');
        submitLoading.value = false;
        return;
      }
      
      // 在编辑模式下，直接使用完整的modelForm
      // 只更新表单可以编辑的字段
      modelForm.name = modelForm.name.trim();
      
      console.log('编辑模型，传递的数据:', JSON.stringify(modelForm, null, 2));
      
      // 直接使用与后端对应的URL和方法
      res = await request({
        url: '/api/database/model',  // 与ModelController的@PutMapping匹配
        method: 'put',              // 使用PUT方法
        data: modelForm             // 使用完整的模型数据
      });
      console.log('编辑请求响应:', res);
    } else {
      // 创建模式，新建模型
      // 构建模型数据，设置默认值
      const modelData = {
        name: modelForm.name.trim(),
        type: 'detail', // 默认为明细模型
        owner: '系统管理员', // 默认负责人
        description: `模型：${modelForm.name}`, // 简单描述
        status: 'draft' // 默认状态为草稿
      };
      
      console.log('创建模型，传递的数据:', JSON.stringify(modelData, null, 2));
      // 使用POST方法创建新模型
      res = await request({
        url: '/api/database/model',  // 与ModelController的@PostMapping匹配
        method: 'post',             // 使用POST方法
        data: modelData             // 与后端@RequestBody对应
      });
    }
    
    if (res && res.code === 0) {
      ElMessage.success(isEditingModel.value ? '模型更新成功' : '模型创建成功')
      createModelDialogVisible.value = false
      // 刷新模型列表
      loadModelList()
      refreshTree()
    } else {
      ElMessage.error(res?.message || (isEditingModel.value ? '模型更新失败' : '模型创建失败'))
    }
  } catch (error) {
    console.error(isEditingModel.value ? '模型更新失败:' : '模型创建失败:', error)
    ElMessage.error((isEditingModel.value ? '模型更新失败: ' : '模型创建失败: ') + (error.message || '未知错误'))
  } finally {
    submitLoading.value = false
  }
}

</script>

<style lang="scss" scoped>
.model-list {
  padding: 20px;

  .main-content {
    display: flex;
    gap: 20px;
  }

  .tree-card {
    width: 300px;
    height: calc(100vh - 100px);
    overflow: auto;

    .tree-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .tree-tools {
        display: flex;
        gap: 8px;
      }
    }

    .tree-filter {
      margin-bottom: 12px;
    }

    .custom-tree-node {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        font-size: 16px;
      }
    }
  }

  .content-area {
    flex: 1;
    min-width: 0;
  }

  .toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .left {
      display: flex;
      gap: 10px;
    }

    .right {
      display: flex;
      gap: 10px;
    }
  }

  .list-card {
    background: rgba(255, 255, 255, 0.9);

    .model-name {
      display: flex;
      align-items: center;
      gap: 8px;

      .el-icon {
        font-size: 16px;
      }
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.column-list {
  .column-item {
    display: flex;
    align-items: center;
    padding: 8px;
    border-bottom: 1px solid #eee;

    .drag-handle {
      cursor: move;
      margin-right: 8px;
      color: #909399;
    }

    &:hover {
      background: #f5f7fa;
    }
  }
}

.context-menu {
  position: fixed;
  background: white;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 8px 0;
  min-width: 150px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 3000;

  .menu-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 16px;
    cursor: pointer;

    &:hover {
      background-color: #f5f7fa;
    }

    .el-icon {
      font-size: 16px;
    }
  }
}

.designer-dialog {
  :deep(.el-dialog) {
    margin: 0 auto !important;
    position: relative;
    margin: 0 auto;
    max-height: 76vh;
    display: flex;
    flex-direction: column;
  }

  :deep(.el-dialog__body) {
    padding: 0;
    height: calc(76vh - 120px);
    overflow: hidden;
  }

  :deep(.el-dialog__header) {
    padding: 16px 20px;
    margin: 0;
    border-bottom: 1px solid #dcdfe6;
  }
}

.table-editor-dialog {
  :deep(.el-dialog) {
    margin: 0 auto !important;
    position: relative;
    max-height: 90vh;
    display: flex;
    flex-direction: column;
  }

  :deep(.el-dialog__body) {
    padding: 20px;
    max-height: calc(90vh - 120px);
    overflow: auto;
  }

  :deep(.el-dialog__header) {
    padding: 16px 20px;
    margin: 0;
    border-bottom: 1px solid #dcdfe6;
  }

  :deep(.el-dialog__footer) {
    padding: 10px 20px;
    border-top: 1px solid #dcdfe6;
  }
}

.table-editor {
  background: #fff;
  border-radius: 8px;
  padding: 0;
  width: 100%;
  height: 100%;
  overflow: visible;

  .editor-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 24px;
    background: rgba(255, 255, 255, 0.8);
    padding: 16px;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  }

  .basic-info {
    flex: 1;
    max-width: 500px;
  }

  .table-form {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 16px;
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .edit-mode {
    margin-right: 16px;
  }

  .fields-editor {
    background: rgba(255, 255, 255, 0.8);
    padding: 16px;
    border-radius: 8px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
    overflow: auto;
  }

  .toolbar {
    margin-bottom: 16px;
    display: flex;
    justify-content: space-between;
  }

  .fields-table {
    width: 100%;
    margin-bottom: 16px;
  }

  .full-width {
    width: 100%;
  }

  :deep(.el-input-number) {
    width: 100%;
  }

  :deep(.el-input-number .el-input__wrapper) {
    padding-right: 0;
  }

  :deep(.el-table) {
    --el-table-border-color: #dcdfe6;
    --el-table-header-bg-color: #f5f7fa;
  }

  :deep(.el-table__header-wrapper) {
    border-radius: 4px 4px 0 0;
    overflow: hidden;
  }

  :deep(.el-table__body-wrapper) {
    max-height: calc(70vh - 300px);
    overflow-y: auto;
  }
}

.sql-editor {
  background: rgba(255, 255, 255, 0.8);
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .sql-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .db-type-select {
    width: 150px;
  }

  .sql-content {
    background: #f8f9fa;
    border-radius: 4px;
    height: calc(70vh - 300px);
    overflow: auto;
  }

  .sql-content :deep(.el-textarea__inner) {
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.6;
    padding: 16px;
    color: #2c3e50;
    background: transparent;
    border: 1px solid rgba(64, 158, 255, 0.1);
    height: 100%;
    min-height: 300px;
  }

  .sql-content :deep(.el-textarea__inner:focus) {
    border-color: #409EFF;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
  }
}

.code-editor {
  background: rgba(255, 255, 255, 0.8);
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);

  .code-toolbar {
    margin-bottom: 16px;
  }

  .code-type-select {
    width: 200px;
  }

  .code-content {
    background: #f8f9fa;
    border-radius: 4px;
    height: calc(70vh - 300px);
    overflow: auto;
  }

  .code-content :deep(.el-textarea__inner) {
    font-family: 'Consolas', 'Monaco', monospace;
    font-size: 14px;
    line-height: 1.6;
    padding: 16px;
    color: #2c3e50;
    background: transparent;
    border: none;
    height: 100%;
    min-height: 300px;
  }

  .code-content :deep(.el-tabs__content) {
    padding: 0;
    height: calc(100% - 40px);
  }

  :deep(.el-tabs__nav-wrap) {
    padding: 0 16px;
  }

  :deep(.el-tab-pane) {
    height: 100%;
  }
}

.code-wrapper {
  position: relative;

  .copy-btn {
    position: absolute;
    top: 8px;
    right: 8px;
    z-index: 1;
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 4px 8px;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid #dcdfe6;
    border-radius: 4px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  }

  .copy-btn:hover {
    background: #fff;
    border-color: #409EFF;
  }

  .copy-btn .el-icon {
    font-size: 16px;
  }
}

.view-table-info {
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 4px;
  display: flex;
  gap: 24px;

  .info-item {
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .info-item .label {
    color: #606266;
    font-weight: 500;
  }

  .info-item .value {
    color: #303133;
  }
}

.view-fields-table {
  margin-bottom: 16px;
}

.file-upload {
  text-align: center;

  .el-upload__tip {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 8px;
    margin-top: 8px;
  }
}

.selected-tables {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  padding: 10px;
}

.search-card {
  margin-top: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);

  .el-form-item {
    margin-bottom: 16px;
  }

  .el-input {
    width: 100%;
  }

  .el-button {
    margin-top: 16px;
  }
}

.advanced-search-container {
  margin-bottom: 20px;

  .search-card {
    border-radius: 8px;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);

    .search-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .search-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }

      .toggle-btn {
        font-size: 14px;

        i {
          margin-left: 4px;
        }
      }
    }

    .basic-search {
      margin-bottom: 10px;

      .el-form-item {
        margin-bottom: 10px;
      }
    }

    .advanced-options {
      margin-top: 10px;

      .el-divider {
        margin: 16px 0;

        i {
          margin-right: 8px;
          color: #409EFF;
        }
      }

      .time-range, .field-count {
        margin-bottom: 16px;
      }

      .range-separator {
        display: flex;
        justify-content: center;
        align-items: center;
        color: #909399;
      }
    }
  }
}

// 响应式调整
@media screen and (max-width: 768px) {
  .advanced-search-container {
    .search-card {
      .basic-search {
        .el-form-item {
          display: block;
          margin-right: 0;

          .el-form-item__content {
            width: 100%;
          }
        }
      }
    }
  }
}

/* 应用标准相关样式 */
.current-domain-info {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;

  h3 {
    margin-top: 0;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    .el-tag {
      margin-left: 10px;
      font-size: 14px;
    }
  }

  .domain-description {
    color: #606266;
    margin-bottom: 0;
  }
}

.standards-lists {
  margin-top: 20px;
}

.dialog-footer {
  margin-top: 20px;
  text-align: right;
}

/* 标准操作按钮样式 */
.standards-actions {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* 选中标准样式 */
.selected-standards {
  margin-bottom: 20px;

  .empty-tip {
    padding: 20px;
    text-align: center;
    color: #909399;
    background-color: #f5f7fa;
    border-radius: 4px;
  }
}

/* 标准配置样式 */
.standards-config {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-weight: 600;
    color: #303133;
  }

  .form-item-help {
    margin-top: 5px;
    color: #909399;
  }

  .current-selection {
    margin-top: 15px;
    padding: 10px;
    background-color: #ecf5ff;
    border-radius: 4px;

    p {
      margin: 0;
      color: #409EFF;

      strong {
        font-weight: 600;
      }
    }
  }
}

.debug-info {
  margin-top: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;

  h4 {
    margin-top: 0;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    .el-icon {
      margin-right: 10px;
      font-size: 16px;
    }
  }

  pre {
    margin: 0;
    padding: 10px;
    color: #606266;
    background-color: #f9fafc;
    border-radius: 4px;
    border: 1px solid #e9e9eb;
  }
}

.form-item-help {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}

.current-selection {
  margin-top: 16px;
  padding: 10px;
  background-color: #f8f9fa;
  border-radius: 4px;

  p {
    margin: 0;
    font-size: 14px;
    color: #303133;
  }
}

.empty-data {
  padding: 20px;
  text-align: center;
  color: #909399;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #303133;
}

/* 添加以下样式 */
.standards-section {
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  color: #303133;
}

.empty-data {
  padding: 40px 0;
  text-align: center;
}

.validation-container {
  padding: 10px;
}

.validation-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.validation-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: bold;
}

.validation-loading, .empty-validation {
  padding: 20px 0;
  text-align: center;
}

.success-text {
  color: #67c23a;
  font-weight: bold;
}

.error-text {
  color: #f56c6c;
  font-weight: bold;
}

.validation-detail-header {
  margin-bottom: 20px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.validation-issues h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 14px;
  color: #f56c6c;
}

/* 标准验证样式 */
.validation-container {
  padding: 20px;
}

.validation-header {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
}

.validation-tip {
  margin-left: 15px;
  color: #909399;
  font-size: 14px;
}

.validation-loading {
  padding: 20px;
}

.validation-empty {
  margin-top: 40px;
}

.validation-results {
  margin-top: 20px;
}

.validation-summary {
  margin-bottom: 20px;
}

.summary-content {
  display: flex;
  flex-wrap: wrap;
}

.summary-item {
  margin-right: 30px;
  margin-bottom: 10px;
}

.summary-item .label {
  font-weight: bold;
  margin-right: 5px;
}

.summary-item .value {
  font-size: 16px;
}

.summary-item .success {
  color: #67C23A;
}

.summary-item .danger {
  color: #F56C6C;
}

.summary-item .warning {
  color: #E6A23C;
}

.validation-item-header {
  display: flex;
  align-items: center;
}

.validation-item-header .standard-name {
  margin-left: 10px;
  font-weight: bold;
  margin-right: 10px;
}

.validation-item-header .rule-name {
  color: #606266;
  margin-right: 10px;
}

.validation-item-content {
  padding: 10px;
}

.validation-info {
  display: flex;
  flex-wrap: wrap;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.info-item {
  margin-right: 20px;
  margin-bottom: 5px;
}

.info-item .label {
  font-weight: bold;
  margin-right: 5px;
}

.validation-issues {
  margin-top: 15px;
}

.issues-header {
  font-weight: bold;
  margin-bottom: 10px;
  font-size: 15px;
}

.validation-success {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: #67C23A;
  font-size: 16px;
}

.validation-success i {
  font-size: 24px;
  margin-right: 10px;
}

.applied-standards-summary {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 8px;
  border-left: 4px solid #409EFF;

  h3 {
    margin-top: 0;
    margin-bottom: 10px;
    display: flex;
    align-items: center;

    .el-tag {
      margin-left: 10px;
      font-size: 14px;
    }
  }

  .applied-standards-count {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .el-button {
      margin-top: 16px;
    }
  }
}

.applied-standards-details {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.domain-standards {
  margin-bottom: 15px;
}

.domain-standards h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 14px;
}

.empty-tip {
  text-align: center;
  padding: 20px;
  color: #909399;
  font-size: 14px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin: 10px 0;
}

// 数字输入框样式
:deep(.full-width-number) {
  width: 100% !important;
  
  .el-input-number {
    width: 100% !important;
  }
  
  .el-input__wrapper {
    padding: 0 8px !important;
  }
  
  .el-input__inner {
    text-align: center !important;
    padding-right: 25px !important;
  }
}

// 确保表格中的数字列居中显示
:deep(.fields-table) {
  .el-table__row td.is-center {
    .el-input-number {
      width: 100% !important;
      display: flex;
      justify-content: center;
    }
    
    .el-input-number__decrease,
    .el-input-number__increase {
      height: 100%;
      margin-top: 0;
      display: flex;
      align-items: center;
    }
  }
}
</style> 