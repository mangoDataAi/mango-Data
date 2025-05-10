<template>
  <div class="model-publish">
    <!-- 顶部标签页 -->
    <el-tabs v-model="activeTab" class="main-tabs">
      <el-tab-pane label="发布配置" name="config">
        <div class="publish-config">
          <el-form :model="publishConfig" label-width="120px">
            <!-- 模型选择 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>选择模型</span>
                  <div class="header-right">
                    <el-button type="primary" size="small" @click="showModelSelector">
                      选择模型
                    </el-button>
                    <el-button type="primary" link size="small" @click="refreshModelList">
                      <el-icon>
                        <Refresh/>
                      </el-icon>
                      刷新
                    </el-button>
                  </div>
                </div>
              </template>
              <!-- 显示已选模型 -->
              <div v-if="selectedModels.length" class="selected-models compact-models">
                <el-tag
                    v-for="model in selectedModels"
                    :key="model.id"
                    closable
                    size="small"
                    @close="removeSelectedModel(model)"
                    class="model-tag"
                >
                  <span class="model-name-tag">{{ model.name }}</span>
                  <span class="model-domain-tag">{{ model.domain }}</span>
                </el-tag>
              </div>
              <el-empty v-else description="请选择要发布的模型" :image-size="60"/>
            </el-card>

            <!-- 基础配置 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>基础配置</span>
                </div>
              </template>
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="发布环境" required>
                    <el-select v-model="publishConfig.environment" style="width: 100%">
                      <el-option
                          v-for="env in environments"
                          :key="env.value"
                          :label="env.label"
                          :value="env.value"
                          :disabled="env.disabled"
                      >
                        <template #default>
                          <div style="display: flex; justify-content: space-between; align-items: center">
                            <span>{{ env.label }}</span>
                            <el-tag size="small" :type="getEnvType(env.value)">{{ env.description }}</el-tag>
                          </div>
                        </template>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="24">
                  <el-form-item label="发布说明" required>
                    <el-input
                        v-model="publishConfig.description"
                        type="textarea"
                        :rows="3"
                        placeholder="请输入本次发布的主要变更内容，例如：新增字段、优化性能等"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
            </el-card>

            <!-- 调度配置 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>调度配置</span>
                  <el-tooltip content="设置模型发布任务的执行时间和方式" placement="top">
                    <el-icon>
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </div>
              </template>
              <el-row :gutter="20">
                <el-col :span="24">
                  <el-form-item label="调度策略" required>
                    <el-radio-group v-model="publishConfig.schedule" @change="handleScheduleChange">
                      <el-radio-button value="immediate">
                        <el-icon>
                          <Timer/>
                        </el-icon>
                        立即执行
                      </el-radio-button>
                      <el-radio-button value="scheduled">
                        <el-icon>
                          <AlarmClock/>
                        </el-icon>
                        定时执行
                      </el-radio-button>
                      <el-radio-button value="periodic">
                        <el-icon>
                          <Calendar/>
                        </el-icon>
                        周期执行
                      </el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>

                <!-- 定时执行配置 -->
                <template v-if="publishConfig.schedule === 'scheduled'">
                  <el-col :span="24">
                    <el-form-item label="执行方式" required>
                      <el-radio-group v-model="publishConfig.scheduleType" size="default">
                        <el-radio value="specific">指定时间点</el-radio>
                        <el-radio value="delay">延迟执行</el-radio>
                        <el-radio value="daily">每日定时</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>

                  <el-col :span="12">
                    <template v-if="publishConfig.scheduleType === 'specific'">
                      <el-form-item label="执行时间" required>
                        <el-date-picker
                            v-model="publishConfig.scheduleTime"
                            type="datetime"
                            style="width: 100%"
                            :disabled-date="disabledDate"
                            :disabled-time="disabledTime"
                            placeholder="选择具体执行时间"
                            value-format="YYYY-MM-DD HH:mm:ss"
                            format="YYYY-MM-DD HH:mm:ss"
                        >
                          <template #default="{ item }">
                            <div style="display: flex; justify-content: space-between; align-items: center">
                              <span>{{ item.label }}</span>
                            </div>
                          </template>
                        </el-date-picker>
                        <div class="form-helper-text">选择未来的具体时间点执行任务</div>
                      </el-form-item>
                    </template>

                    <template v-else-if="publishConfig.scheduleType === 'delay'">
                      <el-form-item label="延迟时间" required>
                        <div class="delay-input-group">
                          <el-input-number
                              v-model="publishConfig.delayValue"
                              :min="1"
                              :max="999"
                              controls-position="right"
                              style="width: 120px"
                          />
                          <el-select v-model="publishConfig.delayUnit" style="width: 100px">
                            <el-option label="分钟" value="minute"/>
                            <el-option label="小时" value="hour"/>
                            <el-option label="天" value="day"/>
                          </el-select>
                        </div>
                        <div class="form-helper-text">
                          任务将在当前时间后 {{ getDelayDescription() }} 执行
                        </div>
                      </el-form-item>
                    </template>

                    <template v-else-if="publishConfig.scheduleType === 'daily'">
                      <el-form-item label="每日时间" required>
                        <el-time-picker
                            v-model="publishConfig.dailyTime"
                            format="HH:mm"
                            placeholder="选择每日执行时间"
                            style="width: 100%"
                        />
                        <div class="form-helper-text">
                          任务将在每天的 {{ formatDailyTime() }} 执行
                        </div>
                      </el-form-item>
                    </template>
                  </el-col>

                  <el-col :span="12"
                          v-if="publishConfig.scheduleType === 'specific' || publishConfig.scheduleType === 'daily'">
                    <el-form-item label="预计执行">
                      <div class="next-run-preview">
                        <el-tag type="info">{{ getNextRunTimePreview() }}</el-tag>
                      </div>
                    </el-form-item>
                  </el-col>
                </template>

                <!-- 周期执行配置 -->
                <template v-if="publishConfig.schedule === 'periodic'">
                  <el-col :span="24">
                    <el-form-item label="周期类型" required>
                      <el-radio-group v-model="publishConfig.periodicType" size="default">
                        <el-radio value="simple">简易设置</el-radio>
                        <el-radio value="cron">Cron表达式</el-radio>
                      </el-radio-group>
                    </el-form-item>
                  </el-col>

                  <template v-if="publishConfig.periodicType === 'simple'">
                    <el-col :span="12">
                      <el-form-item label="执行频率" required>
                        <el-select v-model="publishConfig.periodicFrequency" style="width: 100%">
                          <el-option-group label="每天">
                            <el-option label="每天一次" value="daily"/>
                            <el-option label="每天多次" value="daily_multiple"/>
                          </el-option-group>
                          <el-option-group label="每周">
                            <el-option label="每周一次" value="weekly"/>
                          </el-option-group>
                          <el-option-group label="每月">
                            <el-option label="每月一次" value="monthly"/>
                          </el-option-group>
                        </el-select>
                      </el-form-item>
                    </el-col>

                    <el-col :span="12">
                      <template v-if="publishConfig.periodicFrequency === 'daily'">
                        <el-form-item label="执行时间" required>
                          <el-time-picker
                              v-model="publishConfig.periodicDailyTime"
                              format="HH:mm"
                              placeholder="选择执行时间"
                              style="width: 100%"
                          />
                        </el-form-item>
                      </template>

                      <template v-if="publishConfig.periodicFrequency === 'daily_multiple'">
                        <el-form-item label="执行间隔" required>
                          <div class="interval-input-group">
                            <span>每</span>
                            <el-input-number
                                v-model="publishConfig.periodicInterval"
                                :min="1"
                                :max="12"
                                controls-position="right"
                                style="width: 100px"
                            />
                            <span>小时</span>
                          </div>
                        </el-form-item>
                      </template>

                      <template v-if="publishConfig.periodicFrequency === 'weekly'">
                        <el-form-item label="执行日期" required>
                          <el-select v-model="publishConfig.periodicWeekDay" style="width: 100%">
                            <el-option label="周一" :value="1"/>
                            <el-option label="周二" :value="2"/>
                            <el-option label="周三" :value="3"/>
                            <el-option label="周四" :value="4"/>
                            <el-option label="周五" :value="5"/>
                            <el-option label="周六" :value="6"/>
                            <el-option label="周日" :value="0"/>
                          </el-select>
                        </el-form-item>
                      </template>

                      <template v-if="publishConfig.periodicFrequency === 'monthly'">
                        <el-form-item label="执行日期" required>
                          <el-select v-model="publishConfig.periodicMonthDay" style="width: 100%">
                            <el-option
                                v-for="day in 31"
                                :key="day"
                                :label="`${day}日`"
                                :value="day"
                            />
                            <el-option label="最后一天" value="-1"/>
                          </el-select>
                        </el-form-item>
                      </template>
                    </el-col>

                    <template v-if="['weekly', 'monthly'].includes(publishConfig.periodicFrequency)">
                      <el-col :span="12">
                        <el-form-item label="执行时间" required>
                          <el-time-picker
                              v-model="publishConfig.periodicTime"
                              format="HH:mm"
                              placeholder="选择执行时间"
                              style="width: 100%"
                          />
                        </el-form-item>
                      </el-col>
                    </template>

                    <el-col :span="24">
                      <el-form-item>
                        <div class="cron-preview">
                          <span class="preview-label">预览：</span>
                          <el-tag type="success">{{ getPeriodicDescription() }}</el-tag>
                          <span class="cron-expression" v-if="generatedCronExpression">
                            Cron表达式: <code>{{ generatedCronExpression }}</code>
                          </span>
                        </div>
                      </el-form-item>
                    </el-col>
                  </template>

                  <template v-else>
                    <el-col :span="24">
                      <el-form-item
                          label="Cron表达式"
                          required
                          :tooltip="{
                          content: 'Cron表达式格式：秒 分 时 日 月 周，例如：0 0 12 * * ? 表示每天中午12点执行',
                          placement: 'top'
                        }"
                      >
                        <div class="cron-input-group">
                          <el-input
                              v-model="publishConfig.cronExpression"
                              placeholder="请输入Cron表达式"
                              style="width: 100%"
                          >
                            <template #append>
                              <el-button @click="showCronHelper">
                                <el-icon>
                                  <QuestionFilled/>
                                </el-icon>
                              </el-button>
                            </template>
                          </el-input>
                        </div>
                        <div class="form-helper-text">
                          <el-link type="primary" @click="showCronHelper">查看Cron表达式帮助</el-link>
                        </div>
                      </el-form-item>
                    </el-col>

                    <el-col :span="24">
                      <el-form-item>
                        <div class="cron-preview" v-if="publishConfig.cronExpression">
                          <span class="preview-label">预览：</span>
                          <el-tag type="success">{{ getCronDescription() }}</el-tag>
                        </div>
                      </el-form-item>
                    </el-col>
                  </template>
                </template>
              </el-row>
            </el-card>

            <!-- 物化配置 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>物化配置</span>
                  <el-tooltip content="物化策略决定了视图如何被转换为实体表" placement="top">
                    <el-icon>
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </div>
              </template>
              <el-row :gutter="20">
                <!-- 数据源选择 -->
                <el-col :span="24">
                  <el-form-item
                      label="数据源"
                      required
                      :tooltip="{
                      content: '选择要物化的数据来源',
                      placement: 'top'
                    }"
                  >
                    <div class="data-source-wrapper">
                      <div class="data-source-type-selector">
                        <el-radio-group v-model="publishConfig.dataSourceType" @change="handleDataSourceTypeChange">
                          <el-radio-button label="theme">主题域数据源</el-radio-button>
                          <el-radio-button label="custom">自定义数据源</el-radio-button>
                        </el-radio-group>
                      </div>

                      <!-- 主题域数据源选择器 -->
                      <div v-if="publishConfig.dataSourceType === 'theme'" class="theme-data-source">
                        <el-cascader
                            ref="dataSourceCascaderRef"
                            v-model="publishConfig.dataSource"
                            :options="domainTreeData"
                            :props="{
                            checkStrictly: true,
                            emitPath: true,
                            expandTrigger: 'hover',
                            label: 'name',
                            value: 'id',
                            children: 'children'
                          }"
                            clearable
                            placeholder="请选择主题域数据源"
                            style="width: 100%"
                            @focus="onDomainFocus"
                        >
                          <template #default="{ node, data }">
                            <span>{{ data.name }}</span>
                            <span v-if="data.sourceType">
                              <el-tag size="small" type="info" style="margin-left: 5px">{{ data.sourceType }}</el-tag>
                            </span>
                          </template>
                        </el-cascader>
                      </div>

                      <!-- 自定义数据源选择器 -->
                      <div v-else class="custom-data-source">
                        <el-select
                            v-model="publishConfig.customDataSource"
                            placeholder="请选择自定义数据源"
                            style="width: 100%"
                            @change="handleCustomDataSourceChange"
                        >
                          <el-option
                              v-for="source in dataSourceOptions"
                              :key="source.id || source.dbId"
                              :label="source.name || source.dbName"
                              :value="source.id || source.dbId"
                          >
                            <div class="data-source-option">
                              <span>{{ source.name || source.dbName }}</span>
                              <span class="data-source-type">{{ source.sourceType || source.dbType }}</span>
                            </div>
                          </el-option>
                        </el-select>
                      </div>

                      <!-- 主题域数据源选择结果展示 -->
                      <div
                          v-if="publishConfig.dataSourceType === 'theme' && publishConfig.dataSource && publishConfig.dataSource.length > 0"
                          class="selected-domain-info">
                        <el-alert
                            type="info"
                            :closable="false"
                            style="margin-top: 8px;"
                        >
                          <div>已选择: <strong>{{ getDomainNameByPath(publishConfig.dataSource) }}</strong></div>
                        </el-alert>
                      </div>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item
                      label="物化策略"
                      required
                      :tooltip="{
                      content: '选择将逻辑视图转换为实体表的方式',
                      placement: 'top'
                    }"
                  >
                    <el-select v-model="publishConfig.materializeStrategy" style="width: 100%"
                               @change="handleMaterializeStrategyChange">
                      <el-option
                          label="完全物化"
                          value="complete"
                          :title="'将整个视图查询结果保存为实体表'"
                      >
                        <div style="display: flex; align-items: center; justify-content: space-between;">
                          <span>完全物化</span>
                          <el-tag size="small" type="primary">完整数据</el-tag>
                        </div>
                      </el-option>
                      <el-option
                          label="表级增量物化"
                          value="table_incremental"
                          :title="'可为不同表配置不同的增量物化策略'"
                      >
                        <div style="display: flex; align-items: center; justify-content: space-between;">
                          <span>表级增量物化</span>
                          <el-tag size="small" type="success">精细控制</el-tag>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item
                      label="刷新方式"
                      required
                      :tooltip="{
                      content: '选择物化表的刷新方式',
                      placement: 'top'
                    }"
                  >
                    <el-select v-model="publishConfig.refreshMethod" style="width: 100%">
                      <el-option
                          label="重建表"
                          value="rebuild"
                          :title="'删除现有表并重建'"
                      >
                        <div style="display: flex; align-items: center; justify-content: space-between;">
                          <span>重建表</span>
                          <el-tag size="small" type="warning">完全刷新</el-tag>
                        </div>
                      </el-option>
                      <el-option
                          label="原表更新"
                          value="update"
                          :title="'保留表结构，更新表数据'"
                      >
                        <div style="display: flex; align-items: center; justify-content: space-between;">
                          <span>原表更新</span>
                          <el-tag size="small" type="success">高效更新</el-tag>
                        </div>
                      </el-option>
                    </el-select>
                  </el-form-item>
                </el-col>

                <!-- 表级增量物化配置 - 与模型选择联动 -->
                <template v-if="publishConfig.materializeStrategy === 'table_incremental'">
                  <el-col :span="24">
                    <el-divider content-position="left">表级增量配置</el-divider>
                  </el-col>

                  <!-- 如果没有表数据，显示提示 -->
                  <el-col :span="24" v-if="!modelTables.length">
                    <el-empty description="暂无表配置" :image-size="60">
                      <template #description>
                        <p v-if="!selectedModels.length">请先在上方选择模型以加载表</p>
                        <p v-else>所选模型没有可配置的表</p>
                      </template>
                    </el-empty>
                  </el-col>

                  <!-- 有表数据时显示表配置 -->
                  <el-col :span="24" v-else>
                    <div class="table-incremental-config">
                      <el-table :data="modelTables" border style="width: 100%">
                        <el-table-column prop="modelName" label="所属模型" width="150">
                          <template #default="scope">
                            <el-tag>{{ scope.row.modelName }}</el-tag>
                          </template>
                        </el-table-column>
                        <el-table-column prop="name" label="表名" width="180">
                          <template #default="scope">
                            <div class="table-name">
                              <span>{{ scope.row.name }}</span>
                              <el-tag size="small" type="success" v-if="scope.row.isMain">主表</el-tag>
                              <el-tag size="small" type="info" v-else>从表</el-tag>
                            </div>
                          </template>
                        </el-table-column>

                        <el-table-column label="物化方式" width="150">
                          <template #default="scope">
                            <el-select v-model="scope.row.materializeType" style="width: 100%">
                              <el-option label="全量" value="full"/>
                              <el-option label="增量" value="incremental"/>
                            </el-select>
                          </template>
                        </el-table-column>

                        <el-table-column label="增量字段" width="200">
                          <template #default="scope">
                            <el-select
                                v-model="scope.row.incrementalField"
                                style="width: 100%"
                                :disabled="scope.row.materializeType !== 'incremental'"
                                placeholder="选择增量字段"
                            >
                              <el-option
                                  v-for="field in getTableFields(scope.row)"
                                  :key="field.name"
                                  :label="field.label || field.name"
                                  :value="field.name"
                              />
                            </el-select>
                          </template>
                        </el-table-column>

                        <el-table-column label="写入模式" width="150">
                          <template #default="scope">
                            <el-select v-model="scope.row.writeMode" style="width: 100%">
                              <el-option label="追加" value="append"/>
                              <el-option label="覆盖" value="overwrite"/>
                            </el-select>
                          </template>
                        </el-table-column>

                        <el-table-column label="依赖表">
                          <template #default="scope">
                            <el-select
                                v-model="scope.row.dependencies"
                                multiple
                                collapse-tags
                                style="width: 100%"
                                placeholder="选择依赖表"
                            >
                              <el-option
                                  v-for="table in getOtherTables(scope.row.id)"
                                  :key="table.id"
                                  :label="table.name"
                                  :value="table.id"
                              />
                            </el-select>
                          </template>
                        </el-table-column>
                      </el-table>
                    </div>
                  </el-col>
                </template>

                <!-- 简单的高级选项 -->
                <el-col :span="24">
                  <el-divider content-position="left">
                    <div class="divider-with-toggle">
                      <span>高级选项</span>
                      <el-switch v-model="showAdvancedOptions" active-text="" inactive-text=""/>
                    </div>
                  </el-divider>
                </el-col>

                <template v-if="showAdvancedOptions">
                  <el-col :span="12">
                    <el-form-item
                        label="物化计划执行"
                        :tooltip="{
                        content: '物化作业的调度执行方式',
                        placement: 'top'
                      }"
                    >
                      <el-select v-model="publishConfig.executionMode" style="width: 100%">
                        <el-option label="串行执行" value="serial"/>
                        <el-option label="并行执行" value="parallel"/>
                        <el-option label="基于依赖执行" value="dependency"/>
                      </el-select>
                    </el-form-item>
                  </el-col>

                  <el-col :span="12">
                    <el-form-item
                        label="错误处理"
                        :tooltip="{
                        content: '物化过程中出错时的处理策略',
                      placement: 'top'
                    }"
                    >
                      <el-select v-model="publishConfig.errorHandling" style="width: 100%">
                        <el-option label="失败即停止" value="stop"/>
                        <el-option label="忽略错误继续" value="continue"/>
                        <el-option label="仅记录日志" value="log"/>
                      </el-select>
                    </el-form-item>
                  </el-col>
                </template>
              </el-row>
            </el-card>

            <!-- 物化高级配置 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>物化高级选项</span>
                  <el-tooltip content="高级选项决定了物化过程的详细配置" placement="top">
                    <el-icon>
                      <QuestionFilled/>
                    </el-icon>
                  </el-tooltip>
                </div>
              </template>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item
                      label="物化顺序"
                      :tooltip="{
                      content: '表物化的执行顺序',
                      placement: 'top'
                    }"
                  >
                    <el-select v-model="publishConfig.materializeOrder" style="width: 100%">
                      <el-option label="自动排序" value="auto">
                        <template #default>
                          <div style="display: flex; justify-content: space-between; align-items: center">
                            <span>自动排序</span>
                            <el-tooltip content="根据表依赖关系自动确定物化顺序" placement="top">
                              <el-tag size="small" type="info">推荐</el-tag>
                            </el-tooltip>
                          </div>
                        </template>
                      </el-option>
                      <el-option label="按表定义顺序" value="defined"/>
                      <el-option label="并行执行" value="parallel"/>
                    </el-select>
                  </el-form-item>
                </el-col>

                <el-col :span="12">
                  <el-form-item
                      label="错误处理"
                      :tooltip="{
                      content: '物化过程中遇到错误时的处理方式',
                      placement: 'top'
                    }"
                  >
                    <el-select v-model="publishConfig.errorHandling" style="width: 100%">
                      <el-option label="失败即终止" value="fail"/>
                      <el-option label="跳过错误继续" value="skip"/>
                      <el-option label="重试后终止" value="retry"/>
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-card>

            <!-- 高级配置 -->
            <el-card class="section-card">
              <template #header>
                <div class="card-header">
                  <span>高级配置</span>
                  <el-switch v-model="showAdvanced"/>
                </div>
              </template>
              <el-row :gutter="20" v-if="showAdvanced">
                <el-col :span="12">
                  <el-form-item label="并行度">
                    <el-slider
                        v-model="publishConfig.parallelism"
                        :min="1"
                        :max="10"
                        :marks="{
                        1: '低',
                        5: '中',
                        10: '高'
                      }"
                    />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="超时时间">
                    <el-input-number
                        v-model="publishConfig.timeout"
                        :min="0"
                        :step="10"
                        style="width: 100%"
                    >
                      <template #suffix>分钟</template>
                    </el-input-number>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-card>

            <!-- 操作按钮 -->
            <div class="form-actions">
              <el-button @click="publishConfig">重置</el-button>
              <el-button type="primary" @click="handlePublish" :loading="publishing">
                <el-icon>
                  <Connection/>
                </el-icon>
                创建物化任务
              </el-button>
            </div>
          </el-form>
        </div>
      </el-tab-pane>

      <!-- 发布任务 -->
      <el-tab-pane label="发布任务" name="tasks">
        <div class="publish-tasks">
          <el-card>
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-radio-group v-model="taskFilter" size="small">
                    <el-radio-button value="all">全部</el-radio-button>
                    <el-radio-button value="running">执行中</el-radio-button>
                    <el-radio-button value="scheduled">待执行</el-radio-button>
                    <el-radio-button value="completed">已完成</el-radio-button>
                    <el-radio-button value="failed">失败</el-radio-button>
                  </el-radio-group>
                </div>
                <div class="header-right">
                  <el-input
                      v-model="taskSearchKey"
                      placeholder="搜索任务"
                      prefix-icon="Search"
                      style="width: 200px"
                  />
                  <el-button-group>
                    <el-button @click="refreshTasks">
                      <el-icon>
                        <Refresh/>
                      </el-icon>
                    </el-button>
                  </el-button-group>
                </div>
              </div>
            </template>

            <!-- 调试区域 -->
            <div v-if="isDevelopment"
                 style="margin-bottom: 10px; background: #f0f9eb; padding: 10px; border-radius: 4px;">
              <p>调试区域：activeTab = {{ activeTab }}</p>
              <el-button type="primary" @click="loadTaskList">手动加载任务列表</el-button>
              <el-button type="warning" @click="activeTab = 'tasks'">切换到任务标签页</el-button>
            </div>

            <!-- 任务列表筛选部分 -->
            <div class="filter-container" v-if="activeTab === 'tasks'">
              <el-form :inline="true" :model="taskFilter" class="form-inline">
                <el-form-item label="状态">
                  <el-select v-model="taskFilter.status" placeholder="选择状态" clearable>
                    <el-option label="运行中" value="RUNNING"/>
                    <el-option label="等待中" value="WAITING"/>
                    <el-option label="已完成" value="COMPLETED"/>
                    <el-option label="已失败" value="FAILED"/>
                    <el-option label="已取消" value="CANCELLED"/>
                    <el-option label="已计划" value="SCHEDULED"/>
                    <el-option label="部分完成" value="PARTIAL"/>
                  </el-select>
                </el-form-item>
                <el-form-item label="任务名称">
                  <el-input v-model="taskFilter.taskName" placeholder="任务名称" clearable/>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="filterTasks">筛选</el-button>
                  <el-button @click="resetTaskFilter">重置</el-button>
                </el-form-item>
              </el-form>
            </div>

            <!-- 任务列表表格 -->
            <el-table
                v-loading="taskLoading"
                :data="taskList"
                border
                style="width: 100%"
            >
              <el-table-column prop="taskName" label="任务名称" min-width="150"/>
              <el-table-column prop="description" label="描述" min-width="200"/>
              <el-table-column label="状态" width="120">
                <template #default="scope">
                  <el-tag :type="getTaskStatusType(scope.row.status)">
                    {{ getTaskStatusLabel(scope.row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="执行时间" width="170">
                <template #default="scope">
                  <div v-if="scope.row.startTime">{{ formatDailyTime(scope.row.startTime) }}</div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column label="完成时间" width="170">
                <template #default="scope">
                  <div v-if="scope.row.endTime">{{ formatDailyTime(scope.row.endTime) }}</div>
                  <div v-else>-</div>
                </template>
              </el-table-column>
              <el-table-column label="进度" width="150">
                <template #default="scope">
                  <el-progress :percentage="scope.row.progress || 0"/>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="220" fixed="right">
                <template #default="scope">
                  <el-button type="primary" size="small" @click="viewTaskDetail(scope.row.id)">
                    查看详情
                  </el-button>
                  <el-button type="primary" size="small" @click="executeTask(scope.row.id)">
                    立即执行
                  </el-button>
                  <el-button
                      v-if="['RUNNING', 'WAITING'].includes(scope.row.status)"
                      type="danger"
                      size="small"
                      @click="cancelTask(scope.row.id)"
                  >
                    取消任务
                  </el-button>
                  <el-button
                      v-if="['WAITING', 'SCHEDULED'].includes(scope.row.status)"
                      type="success"
                      size="small"
                      @click="executeTask(scope.row.id)"
                  >
                    执行
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页控件 -->
            <div class="pagination-container">
              <el-pagination
                  background
                  layout="total, sizes, prev, pager, next, jumper"
                  :page-sizes="[5, 10, 20, 50]"
                  :total="taskTotal"
                  :current-page="taskQuery.pageNum"
                  :page-size="taskQuery.pageSize"
                  @size-change="(val) => { taskQuery.pageSize = val; loadTaskList(); }"
                  @current-change="(val) => { taskQuery.pageNum = val; loadTaskList(); }"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <!-- 发布历史 -->
      <el-tab-pane label="发布历史" name="history">
        <div class="publish-history">
          <el-card>
            <template #header>
              <div class="card-header">
                <div class="header-left">
                  <el-input
                      v-model="historySearchKey"
                      placeholder="搜索版本/描述"
                      prefix-icon="Search"
                      style="width: 200px"
                  />
                  <el-button type="primary" plain size="small" @click="toggleHistoryAdvancedSearch">
                    {{ showHistoryAdvancedSearch ? '收起' : '高级搜索' }}
                    <el-icon>
                      <component :is="showHistoryAdvancedSearch ? 'ArrowUp' : 'ArrowDown'"/>
                    </el-icon>
                  </el-button>
                </div>
                <div class="header-right">
                  <el-button-group>
                    <el-button @click="refreshHistory">
                      <el-icon>
                        <Refresh/>
                      </el-icon>
                    </el-button>
                    <el-button @click="exportHistory">
                      <el-icon>
                        <Download/>
                      </el-icon>
                    </el-button>
                    <el-button @click="handleCompareVersions" :disabled="!canCompare">
                      <el-icon>
                        <Document/>
                      </el-icon>
                      对比
                    </el-button>
                  </el-button-group>
                </div>
              </div>
            </template>

            <!-- 高级搜索区域 -->
            <div v-if="showHistoryAdvancedSearch" class="advanced-search-container">
              <el-form :model="historyAdvancedSearch" inline>
                <el-form-item label="任务名称">
                  <el-input v-model="historyAdvancedSearch.taskName" placeholder="任务名称" clearable/>
                </el-form-item>
                <el-form-item label="状态">
                  <el-select v-model="historyAdvancedSearch.status" placeholder="请选择状态" clearable>
                    <el-option label="成功" value="SUCCESS"/>
                    <el-option label="失败" value="FAILED"/>
                    <el-option label="执行中" value="RUNNING"/>
                    <el-option label="等待中" value="PENDING"/>
                    <el-option label="已取消" value="CANCELLED"/>
                  </el-select>
                </el-form-item>
                <el-form-item label="环境">
                  <el-select v-model="historyAdvancedSearch.environment" placeholder="请选择环境" clearable>
                    <el-option
                        v-for="env in environments"
                        :key="env.value"
                        :label="env.label"
                        :value="env.value"
                    />
                  </el-select>
                </el-form-item>
                <el-form-item label="时间范围">
                  <el-date-picker
                      v-model="historyTimeRange"
                      type="daterange"
                      range-separator="至"
                      start-placeholder="开始日期"
                      end-placeholder="结束日期"
                      value-format="YYYY-MM-DD HH:mm:ss"
                      :default-time="['00:00:00', '23:59:59']"
                  />
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" @click="applyHistoryAdvancedSearch">查询</el-button>
                  <el-button @click="resetHistoryAdvancedSearch">重置</el-button>
                </el-form-item>
              </el-form>
            </div>

            <el-table
                :data="filteredHistory"
                style="width: 100%"
                v-loading="historyLoading"
                @selection-change="handleHistorySelectionChange"
            >
              <el-table-column type="selection" width="55"/>
              <el-table-column prop="version" label="版本号" width="100">
                <template #default="{ row }">
                  <el-tag>v{{ row.version }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="environment" label="环境" width="120">
                <template #default="{ row }">
                  <el-tag :type="getEnvType(row.environment)">
                    {{ getEnvLabel(row.environment) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="mode" label="发布方式" width="120">
                <template #default="{ row }">
                  {{ row.mode === 'full' ? '全量发布' : '增量发布' }}
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="120">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)">
                    {{ getStatusLabel(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="publisher" label="发布人" width="120"/>
              <el-table-column prop="publishTime" label="发布时间" width="180"/>
              <el-table-column prop="description" label="发布说明" min-width="200" show-overflow-tooltip/>
              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button-group>
                    <el-button
                        type="primary"
                        link
                        @click="viewHistoryDetail(row.id)"
                    >
                      详情
                    </el-button>
                  </el-button-group>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination">
              <el-pagination
                  v-model:current-page="historyCurrentPage"
                  v-model:page-size="historyPageSize"
                  :page-sizes="[10, 20, 50, 100]"
                  :total="historyTotal"
                  layout="total, sizes, prev, pager, next, jumper"
                  @size-change="handleHistorySizeChange"
                  @current-change="handleHistoryCurrentChange"
              />
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 任务详情对话框 -->
    <el-dialog
        v-model="taskDetailVisible"
        title="任务详情"
        width="800px"
        class="task-detail-dialog"
    >
      <el-descriptions :column="2" border>
        <el-descriptions-item label="任务ID">{{ currentTask.id }}</el-descriptions-item>
        <el-descriptions-item label="任务名称">{{ currentTask.taskName }}</el-descriptions-item>
        <el-descriptions-item label="调度类型">
          {{ getScheduleLabel(currentTask.scheduleType) }}
        </el-descriptions-item>
        <el-descriptions-item label="任务状态">
          {{ getTaskStatusLabel(currentTask.status) }}
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ currentTask.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ currentTask.updateTime }}</el-descriptions-item>
        <el-descriptions-item label="物化策略">{{
            getMaterializeStrategyLabel(currentTask.materializeStrategy)
          }}
        </el-descriptions-item>
        <el-descriptions-item label="执行进度">
          <el-progress
              :percentage="currentTask.progress || 0"
              :status="getProgressStatus(currentTask.status)"
          />
        </el-descriptions-item>
      </el-descriptions>

      <el-tabs v-model="taskDetailTab" class="detail-tabs">
        <!-- 任务信息标签页 -->
        <el-tab-pane label="任务信息" name="info">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="开始时间">{{ currentTask.startTime || '未开始' }}</el-descriptions-item>
            <el-descriptions-item label="结束时间">{{ currentTask.endTime || '未结束' }}</el-descriptions-item>
            <el-descriptions-item label="刷新方式">{{
                getRefreshMethodLabel(currentTask.refreshMethod)
              }}
            </el-descriptions-item>
            <el-descriptions-item label="执行模式">{{
                getExecutionModeLabel(currentTask.executionMode)
              }}
            </el-descriptions-item>
            <el-descriptions-item label="错误处理">{{
                getErrorHandlingLabel(currentTask.errorHandling)
              }}
            </el-descriptions-item>
            <el-descriptions-item label="备份策略">{{
                getBackupStrategyLabel(currentTask.backupStrategy)
              }}
            </el-descriptions-item>
            <el-descriptions-item label="描述" :span="2">{{
                currentTask.description || '无描述'
              }}
            </el-descriptions-item>
          </el-descriptions>

          <div class="model-list">
            <h3>关联模型</h3>
            <el-table :data="currentTask.models || []" style="width: 100%">
              <el-table-column prop="id" label="模型ID" width="280"/>
              <el-table-column prop="name" label="模型名称" width="150"/>
              <el-table-column prop="type" label="类型" width="100"/>
              <el-table-column prop="status" label="状态" width="100"/>
              <el-table-column prop="version" label="版本" width="80"/>
            </el-table>
          </div>
        </el-tab-pane>

        <!-- 版本信息标签页 -->
        <el-tab-pane label="版本信息" name="versions">
          <div class="version-stats">
            <el-row :gutter="20">
              <el-col :span="4" v-for="(count, status) in currentTask.versionStats" :key="status">
                <el-card shadow="hover" :class="['stat-card', `stat-${status}`]">
                  <div class="stat-item">
                    <div class="stat-label">{{ getStatusLabel(status) }}</div>
                    <div class="stat-value">{{ count }}</div>
                  </div>
                </el-card>
              </el-col>
            </el-row>
          </div>

          <el-table :data="currentTask.versions || []" style="width: 100%; margin-top: 20px;" border stripe>
            <el-table-column prop="modelId" label="模型ID" width="200" show-overflow-tooltip>
              <template #default="scope">
                <el-tooltip :content="scope.row.modelId" placement="top" effect="light">
                  <span>{{ scope.row.modelId.substring(0, 15) }}...</span>
                </el-tooltip>
              </template>
            </el-table-column>
            <el-table-column label="模型名称" width="150">
              <template #default="scope">
                {{
                  currentTask.models && currentTask.models.find(m => m.id === scope.row.modelId)?.name || getModelName(scope.row.modelId)
                }}
              </template>
            </el-table-column>
            <el-table-column prop="versionNum" label="版本" width="70" align="center"/>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template #default="scope">
                <el-tag :type="getVersionStatusType(scope.row.status)">
                  {{ getVersionStatusLabel(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="创建时间" width="160" align="center">
              <template #default="scope">
                {{ formatTime(scope.row.createTime) }}
              </template>
            </el-table-column>
            <el-table-column label="是否最新" width="100" align="center">
              <template #default="scope">
                <el-tag type="success" v-if="isLatestVersion(scope.row)">最新</el-tag>
                <el-tag type="info" v-else>历史</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="变更类型" width="120" align="center">
              <template #default="scope">
                <el-tag type="warning" v-if="scope.row.changeType">{{ scope.row.changeType || '结构变更' }}</el-tag>
                <el-tag type="info" v-else>数据变更</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180" fixed="right">
              <template #default="scope">
                <el-button size="small" type="primary" @click="viewVersionDetail(scope.row.id)">
                  详情
                </el-button>
                <el-button size="small" type="success" @click="compareWithLatest(scope.row)"
                           :disabled="isLatestVersion(scope.row)">
                  对比
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 执行日志标签页 -->
        <el-tab-pane label="执行日志" name="logs">
          <div v-if="currentTask.logs && currentTask.logs.length > 0" class="log-content">
            <el-scrollbar height="400px">
              <pre><code>{{ currentTask.logs }}</code></pre>
            </el-scrollbar>
          </div>
          <el-empty v-else description="暂无日志信息"/>
        </el-tab-pane>

      </el-tabs>

      <template #footer>
      <span class="dialog-footer">
        <el-button v-if="['WAITING', 'RUNNING'].includes(currentTask.status)"
                   type="danger"
                   @click="cancelTask(currentTask.id)">
          取消任务
        </el-button>
        <el-button @click="taskDetailVisible = false">关闭</el-button>
        <el-button type="primary" @click="refreshTaskDetail">刷新</el-button>
      </span>
      </template>
    </el-dialog>

    <!-- 版本对比对话框 -->
    <el-dialog
        v-model="compareVisible"
        title="版本对比"
        width="1000px"
        class="compare-dialog"
    >
      <div class="compare-header">
        <div class="version-info">
          <h3>版本 v{{ compareVersions[0]?.version }}</h3>
          <p>{{ compareVersions[0]?.publishTime }}</p>
        </div>
        <div class="version-info">
          <h3>版本 v{{ compareVersions[1]?.version }}</h3>
          <p>{{ compareVersions[1]?.publishTime }}</p>
        </div>
      </div>

      <el-tabs v-model="compareTab">
        <el-tab-pane label="变更内容" name="changes">
          <div class="changes-diff">
            <!-- 使用代码对比组件 -->
          </div>
        </el-tab-pane>
        <el-tab-pane label="配置对比" name="config">
          <div class="config-diff">
            <!-- 使用配置对比组件 -->
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- Cron表达式帮助对话框 -->
    <el-dialog
        v-model="cronHelperVisible"
        title="Cron表达式帮助"
        width="600px"
    >
      <div class="cron-helper">
        <!-- Cron表达式生成器组件 -->
      </div>
    </el-dialog>

    <!-- 模型选择对话框 -->
    <el-dialog
        v-model="modelSelectorVisible"
        title="选择模型"
        width="80%"
        @closed="cancelModelSelection"
        destroy-on-close
    >
      <!-- 顶部过滤区域 -->
      <div class="filter-container">

        <el-input
            v-model="modelNameFilter"
            placeholder="搜索模型名称"
            style="width: 220px; margin-right: 15px;"
            clearable
            prefix-icon="Search"
        />

        <el-button type="primary" @click="refreshModelList">
          <el-icon>
            <Refresh/>
          </el-icon>
          刷新列表
        </el-button>
      </div>

      <!-- 模型列表与表格列表 -->
      <div class="model-tables-container">
        <!-- 左侧模型列表 -->
        <div class="model-list-panel">
          <div class="panel-header">
            <span>模型列表</span>
            <div>
              <el-checkbox v-model="selectAllModels" @change="handleSelectAllModels">全选</el-checkbox>
            </div>
          </div>

          <el-scrollbar height="400px">
            <div class="model-list">
              <div
                  v-for="model in filteredModelList"
                  :key="model.id"
                  class="model-item"
                  :class="{ 'selected': isModelSelected(model.id) }"
                  @click="toggleModelSelection(model)"
              >
                <el-checkbox
                    :modelValue="isModelSelected(model.id)"
                    @change="(val) => toggleModelSelection(model, val)"
                />
                <div class="model-info">
                  <div class="model-name">{{ model.name }}</div>
                  <div class="model-domain">
                    <el-tag size="small" :color="getDomainColor(model.domain)">
                      {{ model.domain }}
                    </el-tag>
                  </div>
                </div>
              </div>
            </div>
          </el-scrollbar>
        </div>

        <!-- 右侧表格列表 -->
        <div class="tables-panel">
          <div class="panel-header">
            <span>包含的表</span>
            <el-select
                v-model="currentModelPreview"
                placeholder="选择模型查看表"
                style="width: 200px"
                clearable
            >
              <el-option
                  v-for="model in tempSelectedModels"
                  :key="model.id"
                  :label="model.name"
                  :value="model.id"
              />
            </el-select>
          </div>

          <el-empty v-if="!previewTables.length" description="请选择模型查看表结构" :image-size="60"/>

          <el-scrollbar v-else height="400px">
            <el-table
                :data="previewTables"
                stripe
                border
                :expand-row-keys="expandedRows"
                @expand-change="handleRowExpand"
            >
              <el-table-column type="expand">
                <template #default="props">
                  <div class="expanded-fields">
                    <h4>字段列表 ({{ props.row.fields?.length || 0 }}个)</h4>
                    <el-table :data="props.row.fields || []" size="small" style="width: 100%">
                      <el-table-column prop="name" label="字段名" width="180"/>
                      <el-table-column prop="displayName" label="显示名" width="180"/>
                      <el-table-column prop="type" label="类型" width="120"/>
                      <el-table-column prop="notNull" label="可空" width="80">
                        <template #default="scope">
                          {{ scope.row.nullable ? '是' : '否' }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="mgcomment" label="描述"/>
                    </el-table>
                  </div>
                </template>
              </el-table-column>
              <el-table-column prop="name" label="表名称" width="180"/>
              <el-table-column prop="displayName" label="表注释" width="180"/>
              <el-table-column prop="isMain" label="表类型" width="100">
                <template #default="scope">
                  <el-tag size="small" type="success"
                          v-if="scope.row.isMain === true || scope.row.tableType === 'main'">主表
                  </el-tag>
                  <el-tag size="small" type="info" v-else>从表</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="字段数" width="100">
                <template #default="scope">
                  <span>{{ scope.row.fieldCount || scope.row.fields?.length || 0 }}</span>
                </template>
              </el-table-column>
              <!-- 删除重复的"字段"列，保留"预览字段"列 -->
              <el-table-column label="预览字段" min-width="300">
                <template #default="scope">
                  <div v-if="scope.row.fields && scope.row.fields.length">
                    <el-tag
                        v-for="field in (scope.row.fields || []).slice(0, 5)"
                        :key="field.name"
                        size="small"
                        class="field-tag"
                        style="margin-right: 5px; margin-bottom: 5px;"
                    >
                      {{ field.label || field.name }}
                    </el-tag>
                    <el-tag size="small" type="info" v-if="(scope.row.fields || []).length > 5">
                      +{{ scope.row.fields.length - 5 }} 个字段
                    </el-tag>
                  </div>
                  <span v-else class="text-muted">暂无字段信息</span>
                </template>
              </el-table-column>
            </el-table>
          </el-scrollbar>
        </div>
      </div>

      <!-- 底部已选区域 -->
      <div class="selected-summary">
        <div class="selected-count">
          已选模型:
          <el-tag type="primary">{{ tempSelectedModels.length }}</el-tag>
        </div>
        <div class="selected-models-preview" v-if="tempSelectedModels.length">
          <el-tag
              v-for="model in tempSelectedModels"
              :key="model.id"
              closable
              size="small"
              @close="toggleModelSelection(model)"
              class="preview-tag"
          >
            {{ model.name }}
          </el-tag>
        </div>
      </div>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelModelSelection">取消</el-button>
          <el-button type="primary" @click="confirmModelSelection">确认选择</el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 历史详情对话框 -->
    <el-dialog
        v-model="historyDetailVisible"
        :title="`模型版本历史`"
        width="900px"
        class="history-detail-dialog"
    >
      <div class="version-timeline">
        <el-timeline>
          <el-timeline-item
              v-for="version in historyDetailData"
              :key="version.version"
              :type="getVersionNodeType(version)"
              :timestamp="version.publishTime"
              :hollow="version.status === 'rolling-back'"
          >
            <el-card class="version-card" :class="{ 'current-version': version.isCurrent }">
              <template #header>
                <div class="version-header">
                  <div class="version-info">
                    <span class="version-number">v{{ version.version }}</span>
                    <el-tag
                        v-if="version.isCurrent"
                        type="success"
                        effect="dark"
                        size="small"
                    >
                      当前版本
                    </el-tag>
                    <el-tag
                        v-if="version.isRollback"
                        type="warning"
                        effect="dark"
                        size="small"
                    >
                      回滚版本
                    </el-tag>
                  </div>
                  <div class="version-actions">
                    <el-tooltip
                        content="查看详情"
                        placement="top"
                    >
                      <el-button
                          type="primary"
                          link
                          :icon="View"
                          @click="viewVersionDetail(version.id)"
                      />
                    </el-tooltip>
                    <el-tooltip
                        content="回滚到此版本"
                        placement="top"
                    >
                      <el-button
                          type="warning"
                          link
                          :icon="RefreshLeft"
                          @click="confirmRollback(version)"
                          :disabled="!canRollbackTo(version)"
                      />
                    </el-tooltip>
                  </div>
                </div>
              </template>

              <div class="version-content">
                <div class="publisher-info">
                  <el-avatar :size="24" :src="version.publisherAvatar">
                    {{ version.publisher?.charAt(0) }}
                  </el-avatar>
                  <span class="publisher-name">{{ version.publisher }}</span>
                  <el-tag
                      :type="getStatusType(version.status)"
                      size="small"
                      class="status-tag"
                  >
                    {{ getStatusLabel(version.status) }}
                  </el-tag>
                </div>

                <div class="version-description">
                  {{ version.description }}
                </div>

                <div class="change-summary" v-if="version.changes?.length">
                  <div class="change-stats">
                    <el-tag
                        v-for="(count, type) in getChangeTypeCounts(version.changes)"
                        :key="type"
                        :type="getChangeType(type)"
                        size="small"
                        class="change-stat-tag"
                    >
                      {{ getChangeLabel(type) }} {{ count }}
                    </el-tag>
                  </div>
                  <div class="change-preview">
                    <div
                        v-for="(change, index) in version.changes.slice(0, 2)"
                        :key="index"
                        class="change-item"
                    >
                      <el-tag :type="getChangeType(change.type)" size="small">
                        {{ getChangeLabel(change.type) }}
                      </el-tag>

                      <!-- 解析details字段 -->
                      <template v-if="change.details">
                        <!-- 如果是嵌套的details对象 -->
                        <span v-if="typeof change.details === 'object'" class="change-details">
          <!-- 字段变更 -->
          <template v-if="change.type?.toUpperCase() === 'FIELD' && change.details.fieldName">
            <span class="field-name">{{ change.details.fieldName }}</span>
            <span v-if="change.details.action" class="action-type">
              ({{ getActionLabel(change.details.action) }})
            </span>
            <span v-if="change.details.details" class="action-details">
              - {{ change.details.details }}
            </span>
          </template>
                          <!-- 表变更 -->
          <template v-else-if="change.type?.toUpperCase() === 'TABLE' && change.details.tableName">
            <span class="table-name">{{ change.details.tableName }}</span>
            <span v-if="change.details.action" class="action-type">
              ({{ getActionLabel(change.details.action) }})
            </span>
          </template>
                          <!-- 其他类型变更 -->
          <template v-else>
            <span v-for="(value, key) in change.details" :key="key" class="detail-item">
              {{ key }}: {{ value }}
            </span>
          </template>
        </span>
                        <!-- 如果是字符串型details -->
                        <span v-else class="change-details">
          {{ change.details }}
        </span>
                      </template>

                      <!-- 如果没有details，则显示path -->
                      <span v-else class="change-path">
        {{ change.path }}
      </span>

                      <!-- 添加变更描述 -->
                      <span v-if="change.description" class="change-description">
        {{ change.description }}
      </span>
                    </div>
                    <div v-if="version.changes.length > 2" class="more-changes">
                      还有 {{ version.changes.length - 2 }} 处变更...
                    </div>
                  </div>
                </div>
              </div>
            </el-card>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <!-- 版本详情对话框 -->
    <el-dialog
        v-model="versionDetailVisible"
        :title="'版本详情 v' + (currentVersion?.versionNum || '')"
        width="800px"
        class="version-detail-dialog"
        append-to-body
    >
      <div v-if="currentVersion" class="version-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="发布环境">
            <el-tag :type="getEnvType(currentVersion.environment)">
              {{ getEnvLabel(currentVersion.environment) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布状态">
            <el-tag :type="getStatusType(currentVersion.status)">
              {{ getStatusLabel(currentVersion.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布人">
            <div class="publisher-info">
              <el-avatar :size="24" :src="currentVersion.publisherAvatar">
                {{ currentVersion.publisher?.charAt(0) }}
              </el-avatar>
              <span>{{ currentVersion.publisher }}</span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">
            {{ formatTime(currentVersion.publishTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="当前版本">
            <el-tag type="success" v-if="currentVersion.isCurrent">是</el-tag>
            <el-tag type="info" v-else>否</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="回滚版本">
            <el-tag type="warning" v-if="currentVersion.isRollback">是</el-tag>
            <el-tag type="info" v-else>否</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-section">
          <div class="section-title">变更说明</div>
          <div class="section-content description-content">
            {{ currentVersion.description || '无变更说明' }}
          </div>
        </div>

        <div class="detail-section" v-if="currentVersion.changes?.length">
          <div class="section-title">变更内容</div>
          <div class="section-content changes-content">
            <div
                v-for="(change, index) in currentVersion.changes"
                :key="index"
                class="change-item"
            >
              <div class="change-header">
                <el-tag :type="getChangeType(change.type)" size="small">
                  {{ getChangeLabel(change.type) }}
                </el-tag>
                <span class="change-path">{{ change.path }}</span>
              </div>
              <div class="change-description">
                {{ change.description }}
              </div>
              <div v-if="change.details" class="change-details">
                <pre><code>{{ JSON.stringify(change.details, null, 2) }}</code></pre>
              </div>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <div class="section-title">物化配置</div>
          <div class="section-content config-content">
            <el-descriptions :column="2" border>
              <el-descriptions-item label="物化策略">
                {{ getMaterializeLabel(currentVersion.config?.materializeStrategy) }}
              </el-descriptions-item>
              <el-descriptions-item label="刷新方式">
                {{ getRefreshMethodLabel(currentVersion.config?.refreshMethod) }}
              </el-descriptions-item>
              <el-descriptions-item label="数据源">
                {{ currentVersion.config?.dataSourceType === 'theme' ? '主题域数据源' : '自定义数据源' }}
              </el-descriptions-item>
              <el-descriptions-item label="数据源名称" v-if="currentVersion.config?.customDataSourceName">
                {{ currentVersion.config?.customDataSourceName }}
              </el-descriptions-item>
              <el-descriptions-item label="执行模式">
                {{ getExecutionModeLabel(currentVersion.config?.executionMode) }}
              </el-descriptions-item>
              <el-descriptions-item label="错误处理">
                {{ getErrorHandlingLabel(currentVersion.config?.errorHandling) }}
              </el-descriptions-item>
              <el-descriptions-item label="并行度">
                {{ currentVersion.config?.parallelism }}
              </el-descriptions-item>
              <el-descriptions-item label="超时时间">
                {{ currentVersion.config?.timeout }}分钟
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </div>

        <div class="detail-section" v-if="currentVersion.logs?.length">
          <div class="section-title">执行日志</div>
          <div class="section-content log-content">
            <div
                v-for="(log, index) in currentVersion.logs"
                :key="index"
                class="log-item"
                :class="log.level.toLowerCase()"
            >
              <span class="log-time">{{ formatTime(log.timestamp) }}</span>
              <span class="log-level">{{ log.level }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-dialog>


  </div>
</template>

<script setup>
import {ref, reactive, computed, onMounted, nextTick, watch} from 'vue'
import {ElMessage, ElMessageBox, ElLoading} from 'element-plus'
import {
  Refresh, Timer, AlarmClock, Calendar,
  Document, Connection, RefreshLeft,
  Download, QuestionFilled, View,
  ArrowUp, ArrowDown
} from '@element-plus/icons-vue'
import dayjs from 'dayjs' // 需要添加dayjs依赖
import axios from 'axios';
import {getDomainTree} from '@/api/domain'
import {getDataSourceList} from '@/api/database'
import {getVersionDetail} from "@/api/dataIntegration/publish";
import {getVersionHistory} from "@/api/dataAsset/version.js";

// 标签页状态
const activeTab = ref('config')

// 发布配置
const publishConfig = reactive({
  environment: 'dev',
  description: '',
  schedule: 'immediate',
  scheduleType: 'specific',
  scheduleTime: null,
  delayValue: 30,
  delayUnit: 'minute',
  dailyTime: null,
  periodicType: 'simple',
  periodicFrequency: 'daily',
  periodicDailyTime: null,
  periodicInterval: 4,
  periodicWeekDay: 1,
  periodicMonthDay: 1,
  periodicTime: null,
  cronExpression: '',
  materializeStrategy: 'complete',
  materializeMode: 'domain',
  targetSourceId: '',
  writeMode: 'overwrite',
  parallelism: 5,
  timeout: 30,
  materializeOrder: 'auto',
  errorHandling: 'fail',
  dataSourceType: 'theme', // 'theme'表示主题域数据源，'custom'表示自定义数据源
  customDataSourceId: '',
  customDataSourceName: '',
  domainName: '',
  dataSource: '',
  domainId: '',
  modelIds: ''
})


// 处理自定义数据源变更
const handleCustomDataSourceChange = (value) => {
  if (!value) {
    publishConfig.customDataSourceId = '';
    publishConfig.customDataSourceName = '';
    return;
  }

  // 查找选中的数据源对象
  const selectedSource = dataSourceOptions.value.find(
      source => (source.id || source.dbId) === value
  );

  if (selectedSource) {
    console.log('选中的自定义数据源:', selectedSource);

    // 直接设置ID和名称
    publishConfig.customDataSourceId = value;
    publishConfig.customDataSourceName = selectedSource.name || selectedSource.dbName;

    // 清空主题域相关字段
    publishConfig.domainId = '';
    publishConfig.domainName = '';
  }
};


// 获取主题域路径名称
const getDomainNamePath = (idPath) => {
  if (!idPath || !Array.isArray(idPath) || idPath.length === 0) {
    return '';
  }

  const namePath = [];
  let currentList = domainTreeData.value;

  for (const id of idPath) {
    const found = findDomainItem(currentList, id);
    if (found) {
      namePath.push(found.name);
      currentList = found.children || [];
    }
  }

  return namePath.join(' > ');
};

// 递归查找域项
const findDomainItem = (list, id) => {
  if (!list || !Array.isArray(list)) return null;

  for (const item of list) {
    if (item.id === id) {
      return item;
    }
    if (item.children) {
      const found = findDomainItem(item.children, id);
      if (found) return found;
    }
  }

  return null;
};

// 级联选择器焦点处理（如果需要）
const onDomainFocus = () => {
  // 如果主题域数据为空，尝试加载
  if (!domainTreeData.value || domainTreeData.value.length === 0) {
    fetchDomainTree();
  }
};

// 高级配置开关
const showAdvanced = ref(false)

const selectedModels = ref([])

// 发布状态
const publishing = ref(false)
const modelSelectorVisible = ref(false)

// 任务管理相关的变量和函数
const taskFilter = ref({
  status: '',
  taskName: ''
});

// 筛选任务的方法
const filterTasks = () => {
  if (taskQuery.value) {
    taskQuery.value.pageNum = 1; // 重置页码
  }
  loadTaskList();
};

// 重置筛选条件
const resetTaskFilter = () => {
  taskFilter.value = {
    status: '',
    taskName: ''
  };
  filterTasks();
};

const taskSearchKey = ref('')
const taskCurrentPage = ref(1)
const taskPageSize = ref(10)
const taskTotal = ref(0)
const taskList = ref([])

// 历史记录相关
const historySearchKey = ref('')
const activeName  = ref('')
const historyLoading = ref(false)
const historyDetailVisible = ref(false)
const taskDetailVisible = ref(false)
const historyDetailData = ref([])
const historyCurrentPage = ref(1)
const historyPageSize = ref(20)
const selectedHistory = ref([])
const currentTask = ref([])
const historyList = ref([])
const historyTotal = ref(0)

// 高级搜索相关
const showHistoryAdvancedSearch = ref(false)
const historyAdvancedSearch = reactive({
  taskName: '',
  status: '',
  environment: '',
  includeVersions: true
})
const historyTimeRange = ref([])

// 切换高级搜索显示状态
const toggleHistoryAdvancedSearch = () => {
  showHistoryAdvancedSearch.value = !showHistoryAdvancedSearch.value
}

// 应用高级搜索条件
const applyHistoryAdvancedSearch = () => {
  historyCurrentPage.value = 1 // 重置页码
  refreshHistory()
}

// 重置高级搜索条件
const resetHistoryAdvancedSearch = () => {
  historyAdvancedSearch.taskName = ''
  historyAdvancedSearch.status = ''
  historyAdvancedSearch.environment = ''
  historyTimeRange.value = []
  historyCurrentPage.value = 1 // 重置页码
  refreshHistory()
}

// 历史记录方法
const refreshHistory = async () => {
  try {
    historyLoading.value = true

    // 构建请求参数
    const params = {
      pageNum: historyCurrentPage.value,
      pageSize: historyPageSize.value,
      includeVersions: historyAdvancedSearch.includeVersions
    }

    // 添加高级搜索条件
    if (historyAdvancedSearch.taskName) {
      params.taskName = historyAdvancedSearch.taskName
    }

    if (historyAdvancedSearch.status) {
      params.status = historyAdvancedSearch.status
    }

    if (historyAdvancedSearch.environment) {
      params.environment = historyAdvancedSearch.environment
    }

    // 处理时间范围
    if (historyTimeRange.value && historyTimeRange.value.length === 2) {
      params.startTime = historyTimeRange.value[0]
      params.endTime = historyTimeRange.value[1]
    }

    // 判断是否使用高级搜索接口
    const useAdvancedSearch = showHistoryAdvancedSearch.value &&
        (historyAdvancedSearch.taskName ||
            historyAdvancedSearch.status ||
            historyAdvancedSearch.environment ||
            (historyTimeRange.value && historyTimeRange.value.length === 2))

    // 调用接口获取数据
    if (useAdvancedSearch) {
      // 使用高级搜索接口
      const {data: result} = await axios.get('/api/materialize/tasks/history/advanced', {params})

      if (result.code === 0) {
        historyList.value = result.data.records || []
        historyTotal.value = result.data.total || 0
      } else {
        ElMessage.error(result.message || '获取历史记录失败')
      }
    } else {
      // 使用常规历史记录接口
      const {data: result} = await axios.get('/api/materialize/tasks/history', {params})

      if (result.code === 0) {
        historyList.value = result.data.records || []
        historyTotal.value = result.data.total || 0
      } else {
        ElMessage.error(result.message || '获取历史记录失败')
      }
    }

  } catch (error) {
    console.error('获取历史记录失败:', error)
    ElMessage.error('获取历史记录失败: ' + (error.message || '未知错误'))
  } finally {
    historyLoading.value = false
  }
}
const handleHistorySizeChange = (size) => {
  historyPageSize.value = size
  refreshHistory()
}

const handleHistoryCurrentChange = (page) => {
  historyCurrentPage.value = page
  refreshHistory()
}

const handleHistorySelectionChange = (selection) => {
  selectedHistory.value = selection
}

const exportHistory = async () => {
  try {
    const response = await fetch('/api/publish/history/export')
    const blob = await response.blob()
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '发布历史.xlsx'
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
  } catch (error) {
    ElMessage.error('导出历史记录失败')
  }
}



// 创建物化任务/发布操作
const handlePublish = async () => {
  try {
    // 验证必填字段
    if (!validatePublishForm()) {
      return;
    }

    publishing.value = true;

    // 构建请求数据
    const publishConfig = buildPublishRequestData();

    const formData = {
      // 基本信息
      taskName: publishConfig.taskName || `物化任务-${dayjs().format('YYYYMMDDHHmmss')}`,
      description: publishConfig.description || `物化发布于 ${dayjs().format('YYYY-MM-DD HH:mm:ss')}`,
      modelIds: selectedModels.value.map(model => model.id),

      // 调度信息 - 转换前端的schedule为后端的scheduleType
      scheduleType: mapScheduleType(publishConfig.schedule),
      scheduledTime: getScheduledTime(),
      cronExpression: publishConfig.cronExpression || '',

      // 数据源信息
      dataSourceType: publishConfig.dataSourceType,
      domainId: publishConfig.domainId || '',
      domainName: publishConfig.domainName || '',
      customDataSourceId: publishConfig.customDataSourceId || '',
      customDataSourceName: publishConfig.customDataSourceName || '',

      // 物化配置
      materializeStrategy: publishConfig.materializeStrategy,
      refreshMethod: publishConfig.refreshMethod || 'rebuild',
      executionMode: publishConfig.executionMode || 'serial',
      errorHandling: publishConfig.errorHandling || 'stop',

      // 传递表配置
      tableConfigs: convertTableConfigs(),

      // 传递完整的模型和表信息（后端可能不使用，但为了调试）
      models: selectedModels.value,
      tables: modelTables.value,

      // 其他配置参数直接传递
      environment: publishConfig.environment,
      materializeMode: publishConfig.materializeMode,
      targetSourceId: publishConfig.targetSourceId,

    };

    // 修改：使用批量发布接口
    const response = await axios.post('/api/materialize/tasks', formData);

    if (response.data.code === 0) {
      ElMessage.success('物化任务已创建成功');
      // 打开历史弹窗或执行其他后续操作
      historyDialogVisible.value = true;
    } else {
      ElMessage.error(response.data.msg || '创建物化任务失败');
    }
  } catch (error) {
    console.error('发布失败:', error);
    ElMessage.error('发布失败: ' + (error.response?.data?.msg || error.message));
  } finally {
    publishing.value = false;
  }
};



// 辅助方法：转换表配置
const convertTableConfigs = () => {
  if (!modelTables || modelTables.length === 0) {
    return [];
  }

  return modelTables.value.map(table => {
    return {
      tableId: table.id,
      tableName: table.name,
      tableComment: table.comment,
      modelId: table.modelId,
      isMain: !!table.isMain,
      materializeType: table.materializeType || 'full',
      incrementalField: table.incrementalField || '',
      writeMode: table.writeMode || 'overwrite',
      dependencies: table.dependencies || []
    };
  });
}

// 构建请求数据
const buildPublishRequestData = () => {
  // 构建包含多个模型ID的请求数据
  return {
    ...publishConfig,
    modelIds: selectedModels.value.map(model => model.id),
    models: selectedModels.value,  // 包含完整模型信息
    tables: modelTables.value,     // 包含所有表信息
    description: publishConfig.description || `物化发布于 ${dayjs().format('YYYY-MM-DD HH:mm:ss')}`
  };
};



// 辅助方法：转换调度类型
const mapScheduleType = (schedule) => {
  switch (schedule) {
    case 'immediate':
      return 'immediate';
    case 'scheduled':
      return 'scheduled';
    case 'periodic':
      return 'periodic';
    default:
      return 'immediate';
  }
}

// 辅助方法：获取调度时间
const getScheduledTime = () => {
  if (publishConfig.schedule !== 'scheduled') {
    return null;
  }

  if (publishConfig.scheduleTime) {
    return publishConfig.scheduleTime;
  }

  // 根据延迟时间计算调度时间
  if (publishConfig.delayValue && publishConfig.delayUnit) {
    const now = dayjs();
    switch (publishConfig.delayUnit) {
      case 'minute':
        return now.add(publishConfig.delayValue, 'minute').toDate();
      case 'hour':
        return now.add(publishConfig.delayValue, 'hour').toDate();
      case 'day':
        return now.add(publishConfig.delayValue, 'day').toDate();
      default:
        return now.add(30, 'minute').toDate();
    }
  }

  return null;
}

const handleCompareVersions = async () => {
  if (selectedHistory.value.length !== 2) {
    ElMessage.warning('请选择两个版本进行对比')
    return
  }

  try {
    const [v1, v2] = selectedHistory.value
    const response = await fetch('/api/publish/compare', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        version1: v1.version,
        version2: v2.version
      })
    })

    const data = await response.json()
    // 处理对比数据...
    compareVisible.value = true
  } catch (error) {
    ElMessage.error('加载版本对比失败')
  }
}



const getEnvType = (env) => {
  const typeMap = {
    'dev': 'info',
    'test': 'warning',
    'prod': 'success'
  };
  return typeMap[env] || 'info';
};

const getEnvLabel = (env) => {
  const labelMap = {
    'dev': '开发环境',
    'test': '测试环境',
    'prod': '生产环境'
  };
  return labelMap[env] || env;
};

const getStatusType = (status) => {
  const typeMap = {
    'SUCCESS': 'success',
    'RUNNING': 'primary',
    'PENDING': 'info',
    'FAILED': 'danger',
    'CANCELLED': 'warning',
    'DRAFT': 'info'
  };
  return typeMap[status] || 'info';
};

const getStatusLabel = (status) => {
  const labelMap = {
    'SUCCESS': '成功',
    'RUNNING': '执行中',
    'PENDING': '等待中',
    'FAILED': '失败',
    'CANCELLED': '已取消',
    'DRAFT': '草稿'
  };
  return labelMap[status] || status;
};


const showCronHelper = () => {
  cronHelperVisible.value = true
}

const disabledDate = (time) => {
  return time.getTime() < Date.now()
}

const disabledTime = (date) => {
  if (!date) return {}
  const now = new Date()
  if (date.getDate() === now.getDate()) {
    return {
      hours: Array.from({length: now.getHours()}, (_, i) => i),
      minutes: date.getHours() === now.getHours()
          ? Array.from({length: now.getMinutes()}, (_, i) => i)
          : []
    }
  }
  return {}
}

// 1. 先声明所有需要的响应式变量
// 过滤和选择相关状态
const domainFilter = ref('');
const modelNameFilter = ref('');
const currentModelPreview = ref(null);
const selectAllModels = ref(false);
const previewTables = ref([]);
const modelListLoading = ref(false);
const tableLoading = ref(false);
const loading = ref(false);

// 模型选择器相关代码
const modelList = ref([]);  // 可用模型列表
const tempSelectedModels = ref([]);  // 临时选择的模型（未确认）

// 2. 然后定义 watch 和函数
// 监听当前预览模型的变化
watch(currentModelPreview, (newValue) => {
  if (newValue) {
    loadModelTables(newValue);
  } else {
    previewTables.value = [];
  }
});

// 刷新模型列表
const refreshModelList = async () => {
  try {
    modelListLoading.value = true;
    // 调用后端接口获取模型列表
    const response = await axios.get('/api/database/model/list', {
      params: {
        name: modelNameFilter.value,
        pageNum: 1,
        pageSize: 100  // 可以根据实际需求调整
      }
    });

    if (response.data.code === 0) {
      modelList.value = response.data.data.list || [];
      // 重置选择状态
      tempSelectedModels.value = [];
      currentModelPreview.value = null;
    } else {
      ElMessage.error(response.data.msg || '加载模型列表失败');
    }
  } catch (error) {
    console.error('加载模型列表失败', error);
    ElMessage.error('加载模型列表失败: ' + (error.response?.data?.msg || error.message));
  } finally {
    modelListLoading.value = false;
  }
};

// 根据选择的模型加载表信息
const loadModelTables = async (modelId) => {
  if (!modelId) {
    previewTables.value = [];
    return;
  }

  try {
    tableLoading.value = true;
    // 调用表格接口
    const response = await axios.get(`/api/table/list/${modelId}`, {
      params: {
        pageNum: 1,
        pageSize: 100,
        status: 'active'
      }
    });

    if (response.data.code === 0) {
      // 从响应中获取表数据并添加字段数量信息
      const tableList = response.data.data.records || [];

      // 对表数据进行加工，确保每个表有字段信息和正确的表类型
      const processedTables = await Promise.all(tableList.map(async (table) => {
        // 如果表没有字段或字段为空数组，则加载字段详情
        if (!table.fields || !table.fields.length) {
          try {
            // 获取表详情以获取字段信息
            const detailResponse = await axios.get(`/api/table/${table.id}`);
            if (detailResponse.data.code === 0 && detailResponse.data.data) {
              const tableDetail = detailResponse.data.data;
              // 合并字段信息
              return {
                ...table,
                fields: tableDetail.fields || [],
                fieldCount: tableDetail.fields ? tableDetail.fields.length : 0,
                // 确保表类型正确，检查多种可能的属性
                isMain: determineTableType(tableDetail)
              };
            }
          } catch (error) {
            console.error(`获取表 ${table.id} 的字段信息失败:`, error);
          }
        }

        // 添加字段数量属性
        return {
          ...table,
          fieldCount: table.fields ? table.fields.length : 0,
          // 确保表类型正确，检查多种可能的属性
          isMain: determineTableType(table)
        };
      }));

      previewTables.value = processedTables;
      console.log('成功加载表数据，数量:', processedTables.length);
      return processedTables;
    } else {
      previewTables.value = [];
      ElMessage.warning(response.data.msg || '该模型没有相关表信息');
      return [];
    }
  } catch (error) {
    console.error('加载表信息失败', error);
    ElMessage.error('加载表信息失败: ' + (error.response?.data?.msg || error.message));
    previewTables.value = [];
    return [];
  } finally {
    tableLoading.value = false;
  }
};

// 添加一个辅助函数来判断表类型
const determineTableType = (table) => {
  // 检查多种可能的属性来确定是否是主表
  if (table.isMain === true) return true;
  if (table.tableType === 'main') return true;
  if (table.type === 'main') return true;
  if (table.isMaster === true) return true;
  if (table.isPrimary === true) return true;
  // 如果以上都不匹配，则默认为从表
  return false;
};

// 初始化加载模型列表
onMounted(() => {
  console.log('组件已挂载');

  // 初始化数据源设置
  if (!publishConfig.dataSourceType) {
    publishConfig.dataSourceType = 'theme'; // 默认使用主题域数据源
  }

  console.log('数据源选项已加载，共', dataSourceOptions.value.length, '个主题域');

  // 加载模型列表
  refreshModelList();
})

// 处理调度策略变更
const handleScheduleChange = (value) => {
  if (value === 'scheduled') {
    publishConfig.scheduleType = 'specific'
    if (!publishConfig.scheduleTime) {
      publishConfig.scheduleTime = dayjs().add(1, 'hour').format('YYYY-MM-DD HH:mm:ss')
    }
  } else if (value === 'periodic') {
    publishConfig.periodicType = 'simple'
    publishConfig.periodicFrequency = 'daily'
    if (!publishConfig.periodicDailyTime) {
      publishConfig.periodicDailyTime = '03:00'
    }
  }
}

// 获取延迟描述
const getDelayDescription = () => {
  const {delayValue, delayUnit} = publishConfig
  const unitMap = {
    'minute': '分钟',
    'hour': '小时',
    'day': '天'
  }
  return `${delayValue} ${unitMap[delayUnit]}`
}

// 格式化每日时间
const formatDailyTime = () => {
  if (!publishConfig.dailyTime) return '--:--'
  return publishConfig.dailyTime
}

// 获取下次执行时间预览
const getNextRunTimePreview = () => {
  if (publishConfig.scheduleType === 'specific' && publishConfig.scheduleTime) {
    return publishConfig.scheduleTime
  } else if (publishConfig.scheduleType === 'delay') {
    const now = dayjs()
    let future = now

    switch (publishConfig.delayUnit) {
      case 'minute':
        future = now.add(publishConfig.delayValue, 'minute')
        break
      case 'hour':
        future = now.add(publishConfig.delayValue, 'hour')
        break
      case 'day':
        future = now.add(publishConfig.delayValue, 'day')
        break
    }

    return future.format('YYYY-MM-DD HH:mm:ss')
  } else if (publishConfig.scheduleType === 'daily' && publishConfig.dailyTime) {
    const [hours, minutes] = publishConfig.dailyTime.split(':')
    const now = dayjs()
    let nextRun = now.hour(parseInt(hours)).minute(parseInt(minutes)).second(0)

    if (nextRun.isBefore(now)) {
      nextRun = nextRun.add(1, 'day')
    }

    return nextRun.format('YYYY-MM-DD HH:mm:ss')
  }

  return '未设置'
}

// 获取周期执行描述
const getPeriodicDescription = () => {
  const {periodicFrequency} = publishConfig

  switch (periodicFrequency) {
    case 'daily':
      if (!publishConfig.periodicDailyTime) return '未设置执行时间'
      return `每天 ${publishConfig.periodicDailyTime} 执行一次`

    case 'daily_multiple':
      return `每 ${publishConfig.periodicInterval} 小时执行一次`

    case 'weekly':
      if (!publishConfig.periodicTime) return '未设置执行时间'
      const weekDays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const weekDay = weekDays[publishConfig.periodicWeekDay]
      return `每周${weekDay} ${publishConfig.periodicTime} 执行一次`

    case 'monthly':
      if (!publishConfig.periodicTime) return '未设置执行时间'
      const day = publishConfig.periodicMonthDay === -1
          ? '最后一天'
          : `${publishConfig.periodicMonthDay}日`
      return `每月${day} ${publishConfig.periodicTime} 执行一次`

    default:
      return '未设置'
  }
}

// 生成Cron表达式
watch([
  () => publishConfig.periodicFrequency,
  () => publishConfig.periodicDailyTime,
  () => publishConfig.periodicInterval,
  () => publishConfig.periodicWeekDay,
  () => publishConfig.periodicMonthDay,
  () => publishConfig.periodicTime
], () => {
  if (publishConfig.periodicType !== 'simple') return

  const {periodicFrequency} = publishConfig
  let cronExp = ''

  switch (periodicFrequency) {
    case 'daily':
      if (publishConfig.periodicDailyTime) {
        const [hours, minutes] = publishConfig.periodicDailyTime.split(':')
        cronExp = `0 ${minutes} ${hours} * * ?`
      }
      break

    case 'daily_multiple':
      cronExp = `0 0 0/${publishConfig.periodicInterval} * * ?`
      break

    case 'weekly':
      if (publishConfig.periodicTime) {
        const [hours, minutes] = publishConfig.periodicTime.split(':')
        cronExp = `0 ${minutes} ${hours} ? * ${publishConfig.periodicWeekDay === 0 ? 1 : publishConfig.periodicWeekDay + 1}`
      }
      break

    case 'monthly':
      if (publishConfig.periodicTime) {
        const [hours, minutes] = publishConfig.periodicTime.split(':')
        const day = publishConfig.periodicMonthDay === -1 ? 'L' : publishConfig.periodicMonthDay
        cronExp = `0 ${minutes} ${hours} ${day} * ?`
      }
      break
  }

  generatedCronExpression.value = cronExp
})

// 获取Cron表达式描述
const getCronDescription = () => {
  // 这里可以使用一个Cron解析库来生成人类可读的描述
  // 简单实现，实际项目中可以使用更完善的库
  const cron = publishConfig.cronExpression
  if (!cron) return '未设置'

  try {
    const parts = cron.split(' ')
    if (parts.length !== 6) return '表达式格式不正确'

    const [second, minute, hour, day, month, week] = parts

    if (second === '0' && minute === '0' && hour === '0' && day === '*' && month === '*' && week === '?') {
      return '每天0点执行'
    } else if (second === '0' && minute === '0' && day === '*' && month === '*' && week === '?') {
      return `每天${hour}点执行`
    } else if (second === '0' && day === '*' && month === '*' && week === '?') {
      return `每天${hour}:${minute}执行`
    } else if (second === '0' && day === '?' && week.includes('/')) {
      return '每周特定时间执行'
    } else if (second === '0' && week === '?' && day.includes('L')) {
      return '每月最后一天执行'
    }

    return '自定义执行计划'
  } catch (e) {
    return '表达式解析错误'
  }
}
const modelTables = ref([])
// 处理物化策略变更 - 优化响应性
const handleMaterializeStrategyChange = (value) => {
  publishConfig.materializeStrategy = value;

  // 根据物化策略设置合理默认值
  if (value === 'complete') {
    publishConfig.refreshMethod = 'rebuild';
  } else if (value === 'table_incremental') {
    publishConfig.refreshMethod = 'update';

    // 如果没有表数据但有选择的模型，尝试加载表数据
    if ((!modelTables.value || modelTables.value.length === 0) && selectedModels.value.length > 0) {
      loadModelTables(selectedModels.value[0].id);
      return;
    }

    // 如果已有表数据，初始化表级配置
    if (modelTables.value && modelTables.value.length > 0) {
      modelTables.value = modelTables.value.map(table => ({
        ...table,
        materializeType: table.isMain ? 'incremental' : 'full',
        incrementalField: table.isMain ? (table.incrementalField || 'update_time') : '',
        writeMode: table.isMain ? 'append' : 'overwrite',
        dependencies: table.dependencies || []
      }));
    }
  }
};

// 获取主题域颜色
const getDomainColor = (domain) => {
  const colorMap = {
    '用户域': '#909399',
    '交易域': '#E6A23C',
    '商品域': '#67C23A',
    '支付域': '#409EFF',
    '营销域': '#F56C6C'
  };
  return colorMap[domain] || '#909399';
};

// 过滤后的模型列表
const filteredModelList = computed(() => {
  let filtered = [...modelList.value];

  // 按域过滤
  if (domainFilter.value) {
    filtered = filtered.filter(model => model.domain === domainFilter.value);
  }

  // 按名称过滤
  if (modelNameFilter.value) {
    const keyword = modelNameFilter.value.toLowerCase();
    filtered = filtered.filter(model =>
        model.name.toLowerCase().includes(keyword)
    );
  }

  return filtered;
});

// 检查模型是否被选中
const isModelSelected = (modelId) => {
  return tempSelectedModels.value.some(model => model.id === modelId);
};

// 切换模型选择状态
const toggleModelSelection = (model, checked = null) => {
  const isSelected = checked !== null ? checked : !isModelSelected(model.id);

  if (isSelected) {
    // 如果未选中，添加到临时选择
    if (!isModelSelected(model.id)) {
      tempSelectedModels.value.push(model);

      // 如果是第一个选择的模型，自动设为预览模型并加载表
      if (tempSelectedModels.value.length === 1) {
        currentModelPreview.value = model.id;
        loadModelTables(model.id);
      }
    }
  } else {
    // 如果已选中，从临时选择中移除
    tempSelectedModels.value = tempSelectedModels.value.filter(m => m.id !== model.id);

    // 如果当前预览的是被移除的模型，重置预览
    if (currentModelPreview.value === model.id) {
      currentModelPreview.value = tempSelectedModels.value.length > 0 ?
          tempSelectedModels.value[0].id : null;

      // 加载新的预览表
      if (currentModelPreview.value) {
        loadModelTables(currentModelPreview.value);
      } else {
        previewTables.value = [];
      }
    }
  }
};

// 处理全选/取消全选
const handleSelectAllModels = (val) => {
  if (val) {
    // 全选：添加所有不在已选中列表的模型
    filteredModelList.value.forEach(model => {
      if (!isModelSelected(model.id)) {
        tempSelectedModels.value.push(model);
      }
    });

    // 设置第一个为预览
    if (tempSelectedModels.value.length > 0 && !currentModelPreview.value) {
      currentModelPreview.value = tempSelectedModels.value[0].id;
      loadPreviewTables(currentModelPreview.value);
    }
  } else {
    // 取消全选：仅移除当前过滤列表中的模型
    const filteredIds = filteredModelList.value.map(model => model.id);
    tempSelectedModels.value = tempSelectedModels.value.filter(
        model => !filteredIds.includes(model.id)
    );

    // 更新预览
    if (tempSelectedModels.value.length > 0) {
      if (!tempSelectedModels.value.some(m => m.id === currentModelPreview.value)) {
        currentModelPreview.value = tempSelectedModels.value[0].id;
        loadPreviewTables(currentModelPreview.value);
      }
    } else {
      currentModelPreview.value = null;
      previewTables.value = [];
    }
  }
};

// 加载预览表
const loadPreviewTables = async (modelId) => {
  console.log('加载预览表, 模型ID:', modelId);
  if (!modelId) {
    previewTables.value = [];
    return;
  }

  try {
    // 使用loadModelTables方法加载表数据 - 这已经会调用正确的接口
    await loadModelTables(modelId);
  } catch (error) {
    console.error('加载预览表失败:', error);
    previewTables.value = [];
  }
};

// 显示模型选择器
const showModelSelector = () => {
  console.log('打开模型选择器');
  // 复制当前选择到临时选择
  tempSelectedModels.value = JSON.parse(JSON.stringify(selectedModels.value));
  // 重置预览和过滤状态
  currentModelPreview.value = tempSelectedModels.value.length > 0 ?
      tempSelectedModels.value[0].id : null;
  domainFilter.value = '';
  modelNameFilter.value = '';

  // 加载预览表
  if (currentModelPreview.value) {
    loadPreviewTables(currentModelPreview.value);
  }

  // 刷新模型列表
  refreshModelList();

  // 显示对话框
  modelSelectorVisible.value = true;
};

// 确认模型选择
const confirmModelSelection = () => {
  console.log('确认选择模型:', tempSelectedModels.value);
  // 更新实际选择的模型
  selectedModels.value = JSON.parse(JSON.stringify(tempSelectedModels.value));
  // 关闭对话框
  modelSelectorVisible.value = false;

  // 处理模型选择变更
  handleModelSelectionChange(selectedModels.value);
};


// 简化的高级选项显示开关
const showAdvancedOptions = ref(false);

// 处理模型选择变更
const handleModelSelectionChange = (selectedModels) => {
  console.log('模型选择变更:', selectedModels);

  // 清空现有的表数据
  modelTables.value = [];

  // 如果有选择的模型，加载每个模型的表
  if (selectedModels && selectedModels.length > 0) {
    // 为每个选择的模型加载表
    selectedModels.forEach(async (model) => {
      const modelId = model.id;
      const tables = await loadModelTables(modelId);

      // 将每个模型的表添加到modelTables中，添加modelId关联
      if (tables && tables.length > 0) {
        const tablesWithModelId = tables.map(table => ({
          ...table,
          modelId: modelId,  // 添加模型ID关联
          modelName: model.name // 添加模型名称关联
        }));

        // 添加到现有表列表
        modelTables.value = [...modelTables.value, ...tablesWithModelId];
      }
    });
  }
};

// 版本历史相关
const versionHistory = ref([])
const rollbackConfirmVisible = ref(false)
const rollbackTarget = ref(null)

// 生成的Cron表达式
const generatedCronExpression = ref('')

// 权限检查（示例）
const hasProductionPermission = computed(() => {
  // 这里可以根据实际的权限控制逻辑返回布尔值
  return true
})

// 环境配置
const environments = computed(() => [
  {
    label: '开发环境',
    value: 'dev',
    description: '用于开发测试',
    disabled: false
  },
  {
    label: '测试环境',
    value: 'test',
    description: '用于功能验证',
    disabled: false
  },
  {
    label: '预生产环境',
    value: 'staging',
    description: '用于上线前验证',
    disabled: false
  },
  {
    label: '生产环境',
    value: 'prod',
    description: '请谨慎操作',
    disabled: !hasProductionPermission.value
  }
])



const canCompare = computed(() => {
  return selectedHistory.value.length === 2
})

// 取消模型选择
const cancelModelSelection = () => {
  console.log('取消选择模型');
  modelSelectorVisible.value = false;
};

// 添加级联选择器引用
const dataSourceCascaderRef = ref(null);

// 数据源类型切换处理
const handleDataSourceTypeChange = (value) => {
  console.log('数据源类型变更为:', value);

  if (value === 'theme') {
    // 切换到主题域，清空自定义数据源相关字段
    publishConfig.customDataSource = '';
    publishConfig.customDataSourceId = '';
    publishConfig.customDataSourceName = '';
  } else {
    // 切换到自定义数据源，清空主题域相关字段
    publishConfig.dataSource = '';
    publishConfig.domainId = '';
    publishConfig.domainName = '';
  }
};


// 添加自定义数据源字段到publishConfig
publishConfig.customDataSource = '';

// 获取表字段的函数
const getTableFields = (table) => {
  // 如果表有字段定义，直接返回
  if (table.fields && Array.isArray(table.fields)) {
    return table.fields;
  }

  // 默认返回一些通用字段
  return [
    {name: 'id', label: 'ID'},
    {name: 'create_time', label: '创建时间'},
    {name: 'update_time', label: '更新时间'},
    {name: 'version', label: '版本号'}
  ];
};

// 获取其他表的函数
const getOtherTables = (currentTableId) => {
  return modelTables.value.filter(table => table.id !== currentTableId);
};


// 历史版本相关
const historyDialogVisible = ref(false);

// 版本详情相关
const versionDetailVisible = ref(false);
const versionDetailData = ref(null);

// 版本比较相关
const compareDialogVisible = ref(false);
const compareResult = ref(null);
const taskLoading = ref(null);
const detailLoading = ref(null);

// 当前版本数据
const currentVersion = ref(null);

// 添加展开行功能
const expandedRows = ref([]);

// 处理行展开事件
const handleRowExpand = (row, expandedRows) => {
  // 保存当前展开的行
  expandedRows.value = expandedRows.map(row => row.id);
};


// 任务状态映射（确保与后端状态对应）
const taskStatusMap = {
  'RUNNING': {type: 'primary', label: '运行中'},
  'WAITING': {type: 'info', label: '等待中'},
  'SCHEDULED': {type: 'warning', label: '已计划'},
  'COMPLETED': {type: 'success', label: '已完成'},
  'FAILED': {type: 'danger', label: '失败'},
  'CANCELLED': {type: 'info', label: '已取消'},
  'PARTIAL': {type: 'warning', label: '部分完成'}
};

// 调度类型映射（确保与后端调度类型对应）
const scheduleTypeMap = {
  'immediate': {type: '', label: '立即执行'},
  'scheduled': {type: 'warning', label: '定时执行'},
  'periodic': {type: 'success', label: '周期执行'}
};

// 获取任务状态类型
const getTaskStatusType = (status) => {
  const statusMap = {
    'RUNNING': 'primary',
    'WAITING': 'info',
    'SCHEDULED': 'warning',
    'COMPLETED': 'success',
    'FAILED': 'danger',
    'CANCELLED': 'info',
    'PARTIAL': 'warning'
  };
  return statusMap[status] || '';
};

// 获取任务状态显示文本
const getTaskStatusLabel = (status) => {
  const statusMap = {
    'RUNNING': '运行中',
    'WAITING': '等待中',
    'SCHEDULED': '已计划',
    'COMPLETED': '已完成',
    'FAILED': '已失败',
    'CANCELLED': '已取消',
    'PARTIAL': '部分完成'
  };
  return statusMap[status] || status;
};

// 获取调度类型标签
const getScheduleLabel = (schedule) => {
  return scheduleTypeMap[schedule]?.label || schedule;
};

// 获取进度条状态
const getProgressStatus = (status) => {
  if (status === 'FAILED') return 'exception';
  if (status === 'COMPLETED') return 'success';
  return '';
};

// 加载任务列表
const loadTaskList = async () => {
  try {
    // 确保 taskQuery 已初始化
    if (!taskQuery.value) {
      taskQuery.value = {
        pageNum: 1,
        pageSize: 10
      };
    }

    taskLoading.value = true;

    const params = {
      pageNum: taskQuery.value.pageNum,
      pageSize: taskQuery.value.pageSize
    };

    // 可以添加状态筛选条件
    if (taskFilter.value && taskFilter.value.status) {
      params.status = taskFilter.value.status;
    }

    // 可以添加任务名称搜索
    if (taskFilter.value && taskFilter.value.taskName) {
      params.taskName = taskFilter.value.taskName;
    }

    const {data: result} = await axios.get('/api/materialize/tasks', {params});

    if (result.code === 0) {
      taskList.value = result.data.records || [];
      taskTotal.value = result.data.total || 0;
      console.log('加载任务列表成功:', taskList.value);
    } else {
      ElMessage.error(result.message || '获取任务列表失败');
    }
  } catch (error) {
    console.error('加载任务列表失败:', error);
    ElMessage.error('加载任务列表失败: ' + (error.message || '未知错误'));
  } finally {
    taskLoading.value = false;
  }
};

// 查看任务详情
const viewTaskDetail = async (taskId) => {
  try {
    detailLoading.value = true;
    const {data: result} = await axios.get(`/api/materialize/task/${taskId}`);

    if (result.code === 0) {
      currentTask.value = result.data;
      taskDetailVisible.value = true;
    } else {
      ElMessage.error(result.message || '获取任务详情失败');
    }
  } catch (error) {
    console.error('获取任务详情失败:', error);
    ElMessage.error('获取任务详情失败: ' + error.message);
  } finally {
    detailLoading.value = false;
  }
};

// 取消任务
const cancelTask = async (taskId) => {
  try {
    await ElMessageBox.confirm('确定要取消此任务吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    });

    const {data: result} = await axios.post(`/api/materialize/cancel-batch/${taskId}`);

    if (result.code === 0) {
      ElMessage.success('任务已取消');
      loadTaskList(); // 重新加载任务列表
    } else {
      ElMessage.error(result.message || '取消任务失败');
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消任务失败:', error);
      ElMessage.error('取消任务失败: ' + error.message);
    }
  }
};

// 执行任务
const executeTask = async (taskId) => {
  try {
    const {data: result} = await axios.post(`/api/materialize/tasks/${taskId}/execute`);

    if (result.code === 0) {
      ElMessage.success('任务已开始执行');
      loadTaskList(); // 重新加载任务列表
    } else {
      ElMessage.error(result.message || '执行任务失败');
    }
  } catch (error) {
    console.error('执行任务失败:', error);
    ElMessage.error('执行任务失败: ' + error.message);
  }
};

// 当tab切换到任务列表时加载数据
watch(() => activeName.value, (newVal) => {
  if (newVal === 'task') {
    loadTaskList();
  }if (newVal === 'history') {
    refreshHistory();
  }

});


// 验证发布表单
const validatePublishForm = () => {
  if (selectedModels.length === 0) {
    ElMessage.warning('请选择至少一个模型');
    return false;
  }

  return true;
};

// 在 setup() 函数的开始或其他适当位置添加：
const taskQuery = ref({
  pageNum: 1,
  pageSize: 10
});


onMounted(() => {
  // 初始化默认值，以防组件中其他地方没有设置
  if (!taskQuery.value) {
    taskQuery.value = {pageNum: 1, pageSize: 10};
  }

  // 如果默认选项卡是任务列表，则加载任务列表
  if (activeTab.value === 'tasks') {
    setTimeout(() => {
      loadTaskList();
    }, 0);
  }else if (activeTab.value === 'history') {
    setTimeout(() => {
      refreshHistory();
    }, 0);
  }

});

// 监听选项卡变化 - 已由上方watch处理，此处更新为备用逻辑
watch(() => activeTab.value, (newVal) => {
  if (newVal === 'tasks') {
    loadTaskList();
  } else if (newVal === 'history') {
    refreshHistory();
  }
});

// 在发生错误的地方添加调试日志
console.log('taskQuery:', taskQuery);
console.log('taskQuery.value:', taskQuery.value);
console.log('activeTab:', activeTab.value);

// 或在组件中添加勾子函数
onMounted(() => {
  console.log('组件已挂载，初始状态:', {
    taskQuery: taskQuery.value,
    activeName: activeName.value
  });
});

// 用于主题域和数据源选择的状态变量
const domainTreeData = ref([])
const dataSourceOptions = ref([])

// 获取主题域树数据
const fetchDomainTree = async () => {
  try {
    console.log('开始获取主题域树数据')
    const response = await getDomainTree()
    if (response.code === 0) {
      domainTreeData.value = response.data || []
      console.log('主题域树数据获取成功，数量:', domainTreeData.value.length)

    } else {
      ElMessage.warning(response.message || '获取主题域失败')
    }
  } catch (error) {
    console.error('获取主题域失败:', error)
    ElMessage.error('获取主题域失败: ' + (error.message || '未知错误'))
  }
}

// 获取数据源列表
const fetchDataSources = async () => {
  try {
    console.log('开始获取数据源列表');
    const response = await getDataSourceList();

    if (response.code === 0) {
      // 处理数据源列表，确保格式一致
      dataSourceOptions.value = (response.data || []).map(item => {
        return {
          // 支持不同的属性名
          id: item.id || item.dbId,
          name: item.name || item.dbName,
          sourceType: item.sourceType || item.dbType,
          // 保留原始数据
          ...item
        };
      });

      console.log('数据源列表获取成功，数量:', dataSourceOptions.value.length);
    } else {
      ElMessage.warning(response.message || '获取数据源失败');
    }
  } catch (error) {
    console.error('获取数据源失败:', error);
    ElMessage.error('获取数据源失败: ' + (error.message || '未知错误'));
  }
};

// 根据路径获取主题域名称
const getDomainNameByPath = (path) => {
  if (!path || !Array.isArray(path) || path.length === 0) return '未选择'

  try {
    // 递归查找函数
    const findDomain = (id, domains) => {
      for (const domain of domains) {
        if (domain.id === id) {
          return domain.name
        }
        if (domain.children && domain.children.length > 0) {
          const found = findDomain(id, domain.children)
          if (found) return found
        }
      }
      return null
    }

    // 返回路径中最后一个ID对应的名称
    const lastId = path[path.length - 1]
    const name = findDomain(lastId, domainTreeData.value)
    return name || '未找到名称'
  } catch (error) {
    console.error('获取主题域名称失败:', error)
    return '获取名称失败'
  }
}

// 在组件加载后确保数据加载
onMounted(() => {
  // ... 其他初始化代码 ...

  // 确保主题域和数据源数据加载
  fetchDomainTree()
  fetchDataSources()

  // 延迟检查数据加载状态
  setTimeout(() => {
    if (domainTreeData.value.length === 0) {
      console.warn('主题域数据为空，尝试重新加载')
      fetchDomainTree()
    }
    if (dataSourceOptions.value.length === 0) {
      console.warn('数据源列表为空，尝试重新加载')
      fetchDataSources()
    }
  }, 2000)
})


// 监听主题域选择变化
watch(() => publishConfig.dataSource, (newValue) => {
  if (!newValue || (Array.isArray(newValue) && newValue.length === 0)) {
    publishConfig.domainId = '';
    publishConfig.domainName = '';
    return;
  }

  if (Array.isArray(newValue) && newValue.length > 0) {
    // 获取最后一级ID
    publishConfig.domainId = newValue[newValue.length - 1];
    // 获取完整路径名称
    publishConfig.domainName = getDomainNameByPath(newValue);

    console.log('主题域选择更新:', {
      domainId: publishConfig.domainId,
      domainName: publishConfig.domainName
    });
  } else if (typeof newValue === 'string' && newValue) {
    // 如果直接是字符串ID
    publishConfig.domainId = newValue;
    // 尝试获取名称
    const domainItem = findDomainById(domainTreeData.value, newValue);
    publishConfig.domainName = domainItem ? domainItem.name : '未知主题域';
  }
}, {deep: true});

/**
 * 通过ID查找主题域项
 * @param {Array} tree - 主题域树
 * @param {string} id - 要查找的ID
 * @returns {object|null} 找到的主题域项或null
 */
const findDomainById = (tree, id) => {
  if (!tree || !Array.isArray(tree)) return null;

  for (const item of tree) {
    if (item.id === id) {
      return item;
    }

    if (item.children && item.children.length > 0) {
      const found = findDomainById(item.children, id);
      if (found) return found;
    }
  }

  return null;
};

// 判断是否是最新版本
const isLatestVersion = (version) => {
  if (!version || !currentTask.value || !currentTask.value.versions) {
    return false;
  }

  // 按模型分组找出每个模型的最新版本
  const modelVersions = currentTask.value.versions.filter(v => v.modelId === version.modelId);
  if (!modelVersions.length) {
    return false;
  }

  // 按版本号排序，找出最大版本号
  const sortedVersions = [...modelVersions].sort((a, b) => {
    const versionA = parseFloat(a.versionNum || '0');
    const versionB = parseFloat(b.versionNum || '0');
    return versionB - versionA; // 降序
  });

  // 如果当前版本是该模型的最大版本号则为最新版本
  return version.id === sortedVersions[0].id;
};

// 与最新版本比较
const compareWithLatest = (version) => {
  if (!version || !currentTask.value || !currentTask.value.versions) {
    ElMessage.warning('无法找到可比较的版本');
    return;
  }

  // 找出同一模型的最新版本
  const latestVersion = findLatestVersion(version.modelId);
  if (!latestVersion) {
    ElMessage.warning('无法找到最新版本');
    return;
  }

  if (version.id === latestVersion.id) {
    ElMessage.warning('当前已是最新版本');
    return;
  }

  // 调用版本比较API
  compareVersions(version.id, latestVersion.id);
};

// 找出指定模型的最新版本
const findLatestVersion = (modelId) => {
  if (!modelId || !currentTask.value || !currentTask.value.versions) {
    return null;
  }

  const modelVersions = currentTask.value.versions.filter(v => v.modelId === modelId);
  if (!modelVersions.length) {
    return null;
  }

  return [...modelVersions].sort((a, b) => {
    const versionA = parseFloat(a.versionNum || '0');
    const versionB = parseFloat(b.versionNum || '0');
    return versionB - versionA; // 降序
  })[0];
};

// 版本比较
const compareVersions = async (versionId1, versionId2) => {
  try {
    compareLoading.value = true;
    const {data: result} = await axios.get('/api/materialize/versions/compare', {
      params: {
        versionId1,
        versionId2
      }
    });

    if (result.code === 0) {
      compareResult.value = result.data;
      compareVisible.value = true;
    } else {
      ElMessage.error(result.message || '比较版本失败');
    }
  } catch (error) {
    console.error('比较版本失败:', error);
    ElMessage.error('比较版本失败: ' + error.message);
  } finally {
    compareLoading.value = false;
  }
};

/**
 * 获取版本状态对应的类型
 * @param {string} status 状态值
 * @returns {string} 状态类型
 */
const getVersionStatusType = (status) => {
  const statusMap = {
    'SUCCESS': 'success',
    'RUNNING': 'primary',
    'PENDING': 'info',
    'FAILED': 'danger',
    'CANCELLED': 'warning',
    'DRAFT': 'info'
  }
  return statusMap[status] || 'info'
}

/**
 * 获取版本状态显示标签
 * @param {string} status 状态值
 * @returns {string} 状态标签
 */
const getVersionStatusLabel = (status) => {
  const labelMap = {
    'SUCCESS': '成功',
    'RUNNING': '执行中',
    'PENDING': '等待中',
    'FAILED': '失败',
    'CANCELLED': '已取消',
    'DRAFT': '草稿'
  }
  return labelMap[status] || status
}

/**
 * 格式化时间
 * @param {number|string} timestamp 时间戳
 * @returns {string} 格式化后的时间
 */
const formatTime = (timestamp) => {
  if (!timestamp) return '--'
  return new Date(timestamp).toLocaleString()
}

/**
 * 查看版本详情
 * @param {string|number} versionId 版本ID
 */
const viewVersionDetail = async (versionId) => {
  try {
    loading.value = true

    // 调用API时传递版本类型参数
    const response = await getVersionDetail(versionId)

    if (response.code === 0) {
      versionDetailData.value = response.data
      // 将数据赋值给currentVersion
      currentVersion.value = response.data
      versionDetailVisible.value = true
    } else {
      ElMessage.error(response.msg || '获取版本详情失败')
    }
  } catch (error) {
    console.error('获取版本详情失败', error)
    ElMessage.error('获取版本详情失败: ' + (error.message || error))
  } finally {
    loading.value = false
  }
}

/**
 * 获取刷新方式显示标签
 * @param {string} refreshMethod 刷新方式
 * @returns {string} 显示标签
 */
const getRefreshMethodLabel = (refreshMethod) => {
  const refreshMethodMap = {
    'FULL': '全量刷新',
    'INCREMENTAL': '增量刷新',
    'APPEND': '追加刷新',
    'MERGE': '合并刷新',
    'SNAPSHOT': '快照刷新'
  };
  return refreshMethodMap[refreshMethod] || refreshMethod;
};

/**
 * 获取错误处理方式显示标签
 * @param {string} errorHandling 错误处理方式
 * @returns {string} 显示标签
 */
const getErrorHandlingLabel = (errorHandling) => {
  const errorHandlingMap = {
    'ABORT': '中止任务',
    'CONTINUE': '继续执行',
    'SKIP': '跳过错误',
    'RETRY': '重试执行'
  };
  return errorHandlingMap[errorHandling] || errorHandling;
};

/**
 * 获取物化策略显示标签
 * @param {string} strategy 物化策略
 * @returns {string} 显示标签
 */
const getMaterializeStrategyLabel = (strategy) => {
  const strategyMap = {
    'FULL': '全量物化',
    'INCREMENTAL': '增量物化',
    'VIEW': '视图物化',
    'ON_DEMAND': '按需物化',
    'STREAMING': '流式物化'
  };
  return strategyMap[strategy] || strategy;
};

/**
 * 查看历史详情
 * @param {Object} row 历史记录行数据
 */
const viewHistoryDetail = async (id) => {
  try {
    loading.value = true;
    // 调用获取历史详情接口
    const response = await axios.get('/api/materialize/history/detail', {
      params: {
        id: id
      }
    });

    if (response.data.code === 0) {
      // 设置历史详情数据
      historyDetailData.value = response.data.data;
      // 显示历史详情对话框
      historyDetailVisible.value = true;
    } else {
      ElMessage.error(response.data.message || '获取历史详情失败');
    }
  } catch (error) {
    console.error('获取历史详情失败', error);
    ElMessage.error('获取历史详情失败: ' + (error.message || error));
  } finally {
    loading.value = false;
  }
};

/**
 * 获取执行模式显示标签
 * @param {string} mode 执行模式
 * @returns {string} 显示标签
 */
const getExecutionModeLabel = (mode) => {
  const modeMap = {
    'NORMAL': '普通模式',
    'PARALLEL': '并行执行',
    'SEQUENTIAL': '顺序执行',
    'BATCH': '批量执行'
  };
  return modeMap[mode] || mode;
};

/**
 * 获取备份策略显示标签
 * @param {string} strategy 备份策略
 * @returns {string} 显示标签
 */
const getBackupStrategyLabel = (strategy) => {
  const strategyMap = {
    'NONE': '无备份',
    'BEFORE_UPDATE': '更新前备份',
    'DAILY': '每日备份',
    'WEEKLY': '每周备份',
    'MONTHLY': '每月备份'
  };
  return strategyMap[strategy] || strategy;
};

/**
 * 获取变更类型对应的标签类型
 * @param {string} type 变更类型
 * @returns {string} 标签类型
 */
const getChangeType = (type) => {
  // 转为大写进行匹配，确保不区分大小写
  const upperType = type.toUpperCase();
  const typeMap = {
    'FIELD': 'info',
    'TABLE': 'warning',
    'MODEL': 'danger',
    'CONFIG': 'success',
    'CREATE': 'success',  // 添加对create类型的支持
    'UPDATE': 'warning',
    'DELETE': 'danger',
    'INITIAL': 'primary'
  };
  return typeMap[upperType] || 'info';
};

/**
 * 获取变更动作对应的显示标签
 * @param {string} action 变更动作
 * @returns {string} 显示标签
 */
const getActionLabel = (action) => {
  if (!action) return '';

  const upperAction = action.toUpperCase();
  const actionMap = {
    'ADDED': '新增',
    'ADDED_FIELD': '新增字段',
    'DELETED': '删除',
    'DELETED_FIELD': '删除字段',
    'MODIFIED': '修改',
    'MODIFIED_FIELD': '修改字段',
    'RENAMED': '重命名',
    'MOVED': '移动',
    'CHANGED': '变更',
    'CHANGED_TYPE': '类型变更'
  };

  return actionMap[upperAction] || action;
};

/**
 * 获取变更类型对应的显示标签
 * @param {string} type 变更类型
 * @returns {string} 显示标签
 */
const getChangeLabel = (type) => {
  // 转为大写进行匹配，确保不区分大小写
  const upperType = type.toUpperCase();
  const labelMap = {
    'FIELD': '字段变更',
    'TABLE': '表变更',
    'MODEL': '模型变更',
    'CONFIG': '配置变更',
    'CREATE': '创建',    // 添加对create类型的支持
    'UPDATE': '更新',
    'DELETE': '删除',
    'INITIAL': '初始化'
  };
  return labelMap[upperType] || type;
};

/**
 * 获取物化策略显示标签
 * @param {string} strategy 物化策略
 * @returns {string} 显示标签
 */
const getMaterializeLabel = (strategy) => {
  const strategyMap = {
    'FULL': '全量物化',
    'INCREMENTAL': '增量物化',
    'VIEW': '视图物化',
    'complete': '全量物化',
    'incremental': '增量物化'
  };
  return strategyMap[strategy] || strategy;
};

/**
 * 获取数据源名称
 * @param {string} sourceId 数据源ID
 * @returns {string} 数据源名称
 */
const getDataSourceName = (sourceId) => {
  if (!sourceId) return '未指定';
  const source = dataSourceOptions.value?.find(s => s.id === sourceId || s.dbId === sourceId);
  return source ? (source.name || source.dbName) : sourceId;
};


// 历史记录过滤计算属性
const filteredHistory = computed(() => {
  // 如果使用后端过滤，直接返回historyList
  return historyList.value.map(task => {
    // 处理版本信息显示
    const latestVersion = task.versions && task.versions.length > 0
        ? task.versions[0]
        : null;

    return {
      id: task.id,
      taskId: task.id,
      taskName: task.taskName,
      version: latestVersion ? latestVersion.versionNum : '无版本',
      status: latestVersion ? latestVersion.status : task.status,
      environment: task.environment,
      mode: latestVersion ? latestVersion.mode : 'full',
      publisher: latestVersion ? latestVersion.publisher : task.creator,
      publishTime: latestVersion ? latestVersion.publishTime : task.createTime,
      description: latestVersion ? latestVersion.description : task.description,
      // 保留原始数据用于详情查看
      originalTask: task,
      originalVersion: latestVersion
    };
  });
})

/**
 * 获取版本节点类型
 * @param {Object} version 版本对象
 * @returns {string} 节点类型
 */
const getVersionNodeType = (version) => {
  if (version.isRollback) return 'warning';
  if (version.isCurrent) return 'success';

  const statusMap = {
    'success': 'success',
    'failed': 'danger',
    'running': 'primary',
    'pending': 'info',
    'cancelled': 'warning',
    'rolling-back': 'warning'
  };

  return statusMap[version.status] || 'info';
};

/**
 * 统计不同类型的变更数量
 * @param {Array} changes 变更数组
 * @returns {Object} 类型和对应的数量
 */
const getChangeTypeCounts = (changes) => {
  if (!changes || !Array.isArray(changes)) return {};

  const counts = {};
  changes.forEach(change => {
    if (change.type) {
      counts[change.type] = (counts[change.type] || 0) + 1;
    }
  });

  return counts;
};


/**
 * 判断版本是否可以回滚
 * @param {Object} version 版本信息
 * @returns {boolean} 是否可以回滚
 */
const canRollbackTo = (version) => {
  // 只有成功的版本才能回滚到
  if (version.status !== 'SUCCESS' && version.status !== 'success') {
    return false;
  }

  // 不能回滚到当前版本
  if (version.isCurrent) {
    return false;
  }

  return true;
};

/**
 * 确认回滚对话框
 * @param {Object} version 要回滚到的版本
 */
const confirmRollback = (version) => {
  console.log('开始回滚确认对话框:', version);

  ElMessageBox.confirm(
      `确定要回滚到版本 v${version.versionNum || version.version} 吗？回滚操作将会创建一个新版本，并使用目标版本的结构重新创建数据表，可能会导致数据丢失。`,
      '回滚确认',
      {
        distinguishCancelAndClose: true, // 区分取消和关闭
        confirmButtonText: '确定回滚',
        cancelButtonText: '取消',
        type: 'warning'
      }
  ).then(() => {
    console.log('用户点击了确认回滚按钮');
    // 直接调用回滚方法
    rollbackToVersion(version);
  }).catch((action) => {
    console.log('回滚对话框结果:', action);
    // 根据不同的结果显示不同的消息
    if (action === 'cancel') {
      ElMessage({
        type: 'info',
        message: '已取消回滚操作'
      });
    } else if (action === 'close') {
      ElMessage({
        type: 'info',
        message: '已关闭回滚对话框'
      });
    } else {
      // 这里处理其他错误情况
      console.error('回滚确认过程中发生错误:', action);
      ElMessage({
        type: 'error',
        message: '回滚确认过程中发生错误'
      });
    }
  });
};

/**
 * 执行版本回滚
 * @param {Object} version 要回滚到的版本
 */
const rollbackToVersion = (version) => {
  const loading = ElLoading.service({
    lock: true,
    text: '正在执行回滚操作...',
    spinner: 'el-icon-loading',
    background: 'rgba(0, 0, 0, 0.7)'
  });

  axios.post('/api/materialize/rollback', {
    versionId: version.id,
    taskId: version.taskId,
    description: `回滚到版本 v${version.versionNum || version.version}`
  }).then(res => {
    loading.close();
    if (res.data.code === 0) {
      ElMessage({
        type: 'success',
        message: '回滚操作已提交，正在处理中'
      });
      // 刷新版本列表
      refreshHistory && refreshHistory();
      viewHistoryDetail(version.taskId) && viewHistoryDetail(version.taskId);
    } else {
      ElMessage({
        type: 'error',
        message: `回滚失败: ${res.data.msg || '未知错误'}`
      });
    }
  }).catch(err => {
    loading.close();
    console.error('回滚请求失败', err);
    ElMessage({
      type: 'error',
      message: `回滚请求失败: ${err.message || '网络错误'}`
    });
  });
};

</script>

<style lang="scss" scoped>
.model-publish {
  padding: 20px;

  .page-header {
    margin-bottom: 20px;
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-left {
      display: flex;
      align-items: center;
      gap: 10px;

      .page-title {
        font-size: 24px;
        margin: 0;
      }
    }
  }

  .status-overview {
    margin-bottom: 20px;

    .status-card {
      height: 120px;
      transition: all 0.3s;

      &:hover {
        transform: translateY(-5px);
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .el-icon {
          font-size: 20px;
        }
      }

      .card-value {
        font-size: 28px;
        font-weight: bold;
        margin: 10px 0;
      }

      .card-trend {
        font-size: 12px;
        color: #909399;

        .up {
          color: #67c23a;
        }

        .down {
          color: #f56c6c;
        }
      }

      &.success {
        .el-icon {
          color: #67c23a;
        }
      }

      &.warning {
        .el-icon {
          color: #e6a23c;
        }
      }

      &.primary {
        .el-icon {
          color: #409eff;
        }
      }

      &.info {
        .el-icon {
          color: #909399;
        }
      }
    }
  }

  .publish-form {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .publish-history {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-right {
        display: flex;
        gap: 10px;
      }
    }

    .pagination {
      margin-top: 20px;
      display: flex;
      justify-content: flex-end;
    }
  }

  .log-content {
    pre {
      margin: 0;
      padding: 10px;
      background-color: #1e1e1e;
      border-radius: 4px;

      code {
        color: #e6e6e6;
        font-family: 'Courier New', Courier, monospace;
        font-size: 14px;
        line-height: 1.5;
        white-space: pre-wrap;
        word-break: break-all;
      }
    }
  }

  // 为不同级别的日志添加颜色
  .log-item {
    padding: 4px 8px;
    margin-bottom: 2px;
    border-radius: 2px;
    font-family: 'Courier New', Courier, monospace;

    &.info {
      background-color: rgba(64, 158, 255, 0.1);
      border-left: 3px solid #409eff;
    }

    &.warn, &.warning {
      background-color: rgba(230, 162, 60, 0.1);
      border-left: 3px solid #e6a23c;
    }

    &.error {
      background-color: rgba(245, 108, 108, 0.1);
      border-left: 3px solid #f56c6c;
    }

    &.debug {
      background-color: rgba(144, 147, 153, 0.1);
      border-left: 3px solid #909399;
    }

    .log-time {
      color: #909399;
      margin-right: 8px;
      font-size: 12px;
    }

    .log-level {
      font-weight: bold;
      margin-right: 8px;
      display: inline-block;
      width: 60px;
      text-align: center;

      &.info {
        color: #409eff;
      }

      &.warn, &.warning {
        color: #e6a23c;
      }

      &.error {
        color: #f56c6c;
      }

      &.debug {
        color: #909399;
      }
    }

    .log-message {
      color: #303133;
      word-break: break-word;
    }
  }
}

.publish-detail-dialog {
  .detail-tabs {
    margin-top: 20px;

    .changes-list {
      .change-item {
        display: flex;
        align-items: center;
        gap: 10px;
        margin-bottom: 10px;
        padding: 10px;
        background: #f5f7fa;
        border-radius: 4px;

        &:last-child {
          margin-bottom: 0;
        }

        .change-path {
          color: #606266;
          font-family: monospace;
        }

        .change-desc {
          color: #909399;
          font-size: 12px;
        }
      }
    }

    .log-content {
      background: #1e1e1e;
      color: #d4d4d4;
      padding: 15px;
      border-radius: 4px;
      max-height: 300px;
      overflow: auto;

      pre {
        margin: 0;
        white-space: pre-wrap;
        word-wrap: break-word;
      }
    }

    .performance-charts {
      display: flex;
      gap: 20px;

      .chart-item {
        flex: 1;

        .chart {
          height: 300px;
        }
      }
    }
  }
}

.selected-models {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;

  .model-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}

.model-selector-dialog {
  .dialog-toolbar {
    margin-bottom: 16px;
    display: flex;
    gap: 16px;
  }

  .model-name {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.history-detail-dialog {
  .version-timeline {
    padding: 0 20px;
    max-height: 600px;
    overflow-y: auto;

    .version-card {
      .version-header {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .version-info {
          display: flex;
          align-items: center;
          gap: 8px;

          .version-number {
            font-size: 16px;
            font-weight: 500;
          }
        }

        .version-actions {
          display: flex;
          gap: 8px;
        }
      }

      .version-content {
        .publisher-info {
          display: flex;
          align-items: center;
          gap: 8px;
          margin-bottom: 12px;

          .publisher-name {
            color: #606266;
          }

          .status-tag {
            margin-left: auto;
          }
        }

        .version-description {
          color: #303133;
          margin-bottom: 16px;
          line-height: 1.5;
        }

        .change-summary {
          background: #f5f7fa;
          border-radius: 4px;
          padding: 12px;
          margin-bottom: 16px;

          .change-stats {
            margin-bottom: 8px;
            display: flex;
            gap: 8px;
          }

          .change-preview {
            .change-item {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 6px;

              &:last-child {
                margin-bottom: 0;
              }

              .change-path {
                color: #606266;
                font-family: monospace;
                font-size: 13px;
              }
            }

            .more-changes {
              color: #909399;
              font-size: 13px;
              margin-top: 6px;
            }
          }
        }

        .version-config {
          :deep(.el-descriptions__cell) {
            padding: 8px 12px;
          }
        }
      }

      &.current-version {
        border-color: var(--el-color-success);
      }
    }
  }
}

.version-detail-dialog {
  .version-detail {
    .detail-section {
      margin-top: 20px;

      .section-title {
        font-size: 14px;
        font-weight: 500;
        margin-bottom: 12px;
        color: #303133;
      }

      .section-content {
        &.description-content {
          color: #303133;
          line-height: 1.6;
          white-space: pre-wrap;
        }

        &.changes-content {
          .change-item {
            padding: 12px;
            background: #f5f7fa;
            border-radius: 4px;
            margin-bottom: 12px;

            &:last-child {
              margin-bottom: 0;
            }

            .change-header {
              display: flex;
              align-items: center;
              gap: 8px;
              margin-bottom: 8px;

              .change-path {
                color: #606266;
                font-family: monospace;
                font-size: 13px;
              }
            }

            .change-description {
              color: #303133;
              margin-bottom: 8px;
            }

            .change-diff {
              background: #1e1e1e;
              border-radius: 4px;
              padding: 12px;

              pre {
                margin: 0;
                color: #d4d4d4;
                font-family: monospace;
                font-size: 12px;
                white-space: pre-wrap;
                word-wrap: break-word;
              }
            }
          }
        }

        &.log-content {
          .log-item {
            padding: 6px 8px;
            font-family: monospace;
            font-size: 12px;
            display: flex;
            gap: 8px;

            &:nth-child(odd) {
              background: #f5f7fa;
            }

            .log-time {
              color: #909399;
              white-space: nowrap;
            }

            .log-level {
              min-width: 60px;
              text-align: center;
              padding: 0 4px;
              border-radius: 2px;
            }

            .log-message {
              flex: 1;
              word-break: break-all;
            }

            &.info .log-level {
              background: #ecf5ff;
              color: #409eff;
            }

            &.warning .log-level {
              background: #fdf6ec;
              color: #e6a23c;
            }

            &.error .log-level {
              background: #fef0f0;
              color: #f56c6c;
            }

            &.success .log-level {
              background: #f0f9eb;
              color: #67c23a;
            }
          }
        }
      }
    }
  }
}

.compact-models {
  margin: -4px;
  max-height: 120px;
  overflow-y: auto;

  .model-tag {
    margin: 4px;
    display: inline-flex;
    align-items: center;
    gap: 6px;

    .model-name-tag {
      font-weight: 500;
    }

    .model-domain-tag {
      font-size: 12px;
      opacity: 0.8;
      border-left: 1px solid rgba(255, 255, 255, 0.3);
      padding-left: 6px;
    }
  }
}

.operation-type-mapping {
  display: flex;
  gap: 12px;

  .mapping-item {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 8px;

    .mapping-label {
      width: 40px;
      text-align: right;
      color: #606266;
    }
  }
}

.table-config-tabs {
  margin-bottom: 20px;

  :deep(.el-tabs__content) {
    padding: 15px;
  }
}

/* 添加调试卡片样式 */
.debug-card {
  margin-bottom: 15px;
}

/* 表配置相关样式 */
.table-config-tabs {
  margin-top: 10px;
  margin-bottom: 20px;
}

.tab-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-config-form {
  padding: 15px 5px;
}

/* 确保表单元素样式 */
.el-form-item {
  margin-bottom: 18px;
}

/* 添加到现有样式中 */

/* 模型选择器样式 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.model-tables-container {
  display: flex;
  gap: 20px;
  margin-bottom: 15px;
}

.model-list-panel, .tables-panel {
  flex: 1;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  font-weight: bold;
}

.model-list {
  padding: 0;
}

.model-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.2s;
}

.model-item:hover {
  background-color: #f5f7fa;
}

.model-item.selected {
  background-color: #ecf5ff;
}

.model-info {
  margin-left: 10px;
  flex: 1;
}

.model-name {
  font-weight: 500;
  margin-bottom: 5px;
}

.model-domain {
  display: flex;
  align-items: center;
}

.field-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.selected-summary {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  margin-top: 15px;
}

.selected-count {
  font-weight: 500;
}

.selected-models-preview {
  display: flex;
  flex-wrap: wrap;
  gap: 5px;
}

.preview-tag {
  margin-bottom: 5px;
}

/* 模型标签样式 */
.selected-models {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 10px 0;
}

.model-tag {
  display: flex;
  align-items: center;
  padding-right: 8px;
}

.model-name-tag {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.model-domain-tag {
  font-size: 0.85em;
  color: #909399;
  margin-left: 5px;
  border-left: 1px solid #e0e0e0;
  padding-left: 5px;
}

/* 紧凑的模型展示 */
.compact-models .model-tag {
  margin-bottom: 5px;
}

/* 添加到现有样式 */

/* 数据源级联选择器样式 */
:deep(.el-cascader-panel) {
  max-height: 300px;
}

:deep(.el-cascader-menu) {
  min-width: 200px;
}

/* 调整级联选择器下拉项样式 */
:deep(.el-cascader-node__label) {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 增强卡片标题样式 */
.card-header {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
}

.card-header .el-icon {
  margin-left: 8px;
  color: #909399;
  cursor: help;
}

/* 数据源选择器样式 */
.data-source-wrapper {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.data-source-type-selector {
  margin-bottom: 10px;
}

.theme-data-source, .custom-data-source {
  width: 100%;
}

/* 调整级联选择器样式 */
:deep(.el-cascader) {
  width: 100%;
}

:deep(.el-cascader-panel) {
  max-height: 300px;
}

:deep(.el-cascader-menu) {
  min-width: 200px;
}

/* 级联选择器下拉项样式 */
:deep(.el-cascader-node__label) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

/* 确保标签显示正确 */
:deep(.el-tag) {
  margin-right: 3px;
}

/* 增强卡片标题样式 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: bold;
}

.card-header .el-icon {
  color: #909399;
  cursor: help;
}

/* 添加到现有样式中 */

/* 高级选项样式 */
.advanced-options {
  margin-top: 10px;
}

.divider-with-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 强化卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

/* 增量配置部分 */
.el-divider__text {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

/* 添加到现有样式 */

/* 分割线标题样式 */
.divider-with-toggle {
  display: flex;
  align-items: center;
  gap: 10px;
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 高级搜索区域 */
.advanced-search-container {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 15px;

  .el-form-item {
    margin-bottom: 15px;
    margin-right: 15px;
  }

  .el-form-item__label {
    font-weight: 500;
  }

  .el-input, .el-select, .el-date-picker {
    width: 220px;
  }

  .el-button {
    margin-left: 10px;
  }
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 高级搜索区域 */
.advanced-search-container {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 15px;

  .el-form-item {
    margin-bottom: 15px;
    margin-right: 15px;
  }

  .el-form-item__label {
    font-weight: 500;
  }

  .el-input, .el-select, .el-date-picker {
    width: 220px;
  }

  .el-button {
    margin-left: 10px;
  }
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 高级搜索区域 */
.advanced-search-container {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 15px;

  .el-form-item {
    margin-bottom: 15px;
    margin-right: 15px;
  }

  .el-form-item__label {
    font-weight: 500;
  }

  .el-input, .el-select, .el-date-picker {
    width: 220px;
  }

  .el-button {
    margin-left: 10px;
  }
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 高级搜索区域 */
.advanced-search-container {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 15px;

  .el-form-item {
    margin-bottom: 15px;
    margin-right: 15px;
  }

  .el-form-item__label {
    font-weight: 500;
  }

  .el-input, .el-select, .el-date-picker {
    width: 220px;
  }

  .el-button {
    margin-left: 10px;
  }
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 高级搜索区域 */
.advanced-search-container {
  margin-top: 10px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  border: 1px solid #ebeef5;
  margin-bottom: 15px;

  .el-form-item {
    margin-bottom: 15px;
    margin-right: 15px;
  }

  .el-form-item__label {
    font-weight: 500;
  }

  .el-input, .el-select, .el-date-picker {
    width: 220px;
  }

  .el-button {
    margin-left: 10px;
  }
}

/* 表级增量配置样式 */
.table-incremental-config {
  margin: 10px 0;
}

.table-name {
  display: flex;
  align-items: center;
  gap: 5px;
}

/* 表格样式调整 */
:deep(.el-table) {
  --el-table-header-bg-color: #f5f7fa;
}

:deep(.el-table__header) {
  font-weight: bold;
}

:deep(.el-select) {
  width: 100%;
}

/* 简化的卡片标题 */
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 500;
}

.expanded-fields {
  padding: 15px;

  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    font-size: 16px;
  }
}

.text-muted {
  color: #909399;
  font-size: 12px;
}

/* 添加到已有的 <style> 部分 */
.publish-tasks {
  margin-top: 10px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.debug-panel {
  background-color: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
  margin-top: 10px;
}

/* 添加到已有的 <style> 部分 */
:deep(.task-detail-dialog) {
  .el-message-box__content {
    max-height: 70vh;
    overflow-y: auto;
  }

  .detail-section {
    margin-bottom: 20px;
    border-left: 3px solid #f0f0f0;
    padding-left: 10px;
  }

  .table-list ul {
    max-height: 150px;
    overflow-y: auto;
    padding-left: 20px;
  }

  h3 {
    border-bottom: 1px solid #ebeef5;
    padding-bottom: 10px;
    margin-bottom: 15px;
  }

  h4 {
    margin-top: 15px;
    margin-bottom: 10px;
    font-size: 16px;
  }
}

/* 任务列表筛选部分 */
.filter-container {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.form-inline {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-item {
  margin-bottom: 0;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 在 style 部分添加以下样式 */

/* 主题域和数据源选择样式 */
.materialize-config-form {
  .el-tree-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
    }
  }
}

/* 可以添加一些树形选择器的自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 添加用于表示选中状态的样式 */
.el-select-dropdown__item.selected,
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

/* ... 现有样式 ... */

/* 确保树形选择器和下拉菜单样式正确 */
.materialize-config-form {
  margin-top: 20px;

  .el-tree-select,
  .el-select {
    width: 100%;
  }

  .data-source-option {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .data-source-type {
      color: #909399;
      font-size: 12px;
      background-color: #f0f2f5;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }
}

/* 选中信息的样式 */
.selected-info {
  margin: 10px 0 20px;
}

/* 树形选择器自定义样式 */
:deep(.el-tree-select) {
  .el-select-dropdown__item {
    padding-right: 20px;
  }

  .el-tree-node__content {
    height: 32px;
    padding-right: 8px;
  }

  .el-tree-node__label {
    font-size: 14px;
  }
}

/* 选中状态的样式 */
:deep(.el-select-dropdown__item.selected),
:deep(.el-tree-node.is-current > .el-tree-node__content) {
  color: var(--el-color-primary);
  font-weight: bold;
}

.change-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 8px;
  flex-wrap: wrap;
}

.change-path, .change-details {
  margin-left: 8px;
  font-size: 13px;
  color: #606266;
  word-break: break-all;
}

.change-description {
  margin-left: 8px;
  font-size: 12px;
  color: #909399;
  font-style: italic;
}

.field-name, .table-name {
  font-weight: bold;
  color: #409eff;
}

.action-type {
  color: #67c23a;
  margin: 0 4px;
}

.action-details {
  color: #606266;
}

.detail-item {
  display: inline-block;
  margin-right: 8px;
  color: #606266;
}

.change-stat-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}

.more-changes {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
}
</style> 
