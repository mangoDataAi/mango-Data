<template>
  <div class="preferences-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <el-icon class="header-icon"><Setting /></el-icon>
        <h2>个性化设置</h2>
      </div>
      <div class="header-actions">
        <el-button-group>
          <el-button type="primary" @click="handleSave">
            <el-icon><Check /></el-icon> 保存设置
          </el-button>
          <el-button @click="handleReset">
            <el-icon><RefreshRight /></el-icon> 重置默认
          </el-button>
        </el-button-group>
      </div>
    </div>

    <!-- 设置内容 -->
    <el-row :gutter="20">
      <!-- 主题设置 -->
      <el-col :span="16">
        <el-card class="preference-card theme-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Brush /></el-icon> 主题外观
              </span>
              <el-button type="primary" link @click="handlePreview">
                <el-icon><View /></el-icon> 预览效果
              </el-button>
            </div>
          </template>

          <!-- 主题模式 -->
          <div class="setting-section">
            <div class="section-header">
              <span class="section-title">主题模式</span>
              <el-tag type="info" effect="plain">建议根据使用环境选择合适的主题模式</el-tag>
            </div>
            <div class="theme-mode-list">
              <div
                v-for="mode in themeModes"
                :key="mode.value"
                class="theme-mode-item"
                :class="{ active: currentTheme.mode === mode.value }"
                @click="handleThemeModeChange(mode.value)"
              >
                <div class="mode-preview" :class="mode.value">
                  <div class="preview-header"></div>
                  <div class="preview-sidebar"></div>
                  <div class="preview-content"></div>
                </div>
                <div class="mode-info">
                  <span class="mode-name">{{ mode.label }}</span>
                  <span class="mode-desc">{{ mode.description }}</span>
                </div>
                <el-icon v-if="currentTheme.mode === mode.value" class="mode-checked"><Select /></el-icon>
              </div>
            </div>
          </div>

          <!-- 主题色调 -->
          <div class="setting-section">
            <div class="section-header">
              <span class="section-title">主题色调</span>
              <el-tag type="info" effect="plain">点击色块即可切换主题色调</el-tag>
            </div>
            <div class="theme-color-list">
              <div
                v-for="color in themeColors"
                :key="color.value"
                class="theme-color-item"
                :class="{ active: currentTheme.color === color.value }"
                :style="{ backgroundColor: color.value }"
                @click="handleThemeColorChange(color.value)"
              >
                <el-icon v-if="currentTheme.color === color.value" class="color-checked"><Check /></el-icon>
              </div>
              <div class="theme-color-item custom">
                <el-color-picker
                  v-model="customColor"
                  :predefine="predefineColors"
                  @change="handleCustomColorChange"
                />
              </div>
            </div>
          </div>

          <!-- 界面布局 -->
          <div class="setting-section">
            <div class="section-header">
              <span class="section-title">界面布局</span>
              <el-tag type="info" effect="plain">选择最适合您的布局方式</el-tag>
            </div>
            <div class="layout-list">
              <div
                v-for="layout in layoutModes"
                :key="layout.value"
                class="layout-item"
                :class="{ active: currentTheme.layout === layout.value }"
                @click="handleLayoutChange(layout.value)"
              >
                <div class="layout-preview" :class="layout.value">
                  <div class="preview-header"></div>
                  <div class="preview-sidebar"></div>
                  <div class="preview-content"></div>
                </div>
                <div class="layout-info">
                  <span class="layout-name">{{ layout.label }}</span>
                  <span class="layout-desc">{{ layout.description }}</span>
                </div>
                <el-icon v-if="currentTheme.layout === layout.value" class="layout-checked"><Select /></el-icon>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 快捷设置 -->
      <el-col :span="8">
        <el-card class="preference-card quick-settings" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><MagicStick /></el-icon> 快捷设置
              </span>
            </div>
          </template>
          
          <el-form :model="quickSettings" label-position="top">
            <el-form-item label="导航模式">
              <el-radio-group v-model="quickSettings.navigation" size="large" fill>
                <el-radio-button label="side">侧边导航</el-radio-button>
                <el-radio-button label="top">顶部导航</el-radio-button>
              </el-radio-group>
            </el-form-item>

            <el-form-item label="菜单展示">
              <el-switch
                v-model="quickSettings.menuCollapse"
                :active-icon="Fold"
                :inactive-icon="Expand"
                inline-prompt
              />
              <span class="setting-label">默认收起菜单</span>
            </el-form-item>

            <el-form-item label="标签页">
              <el-switch
                v-model="quickSettings.tabBar"
                :active-icon="Document"
                :inactive-icon="Close"
                inline-prompt
              />
              <span class="setting-label">显示标签页</span>
            </el-form-item>

            <el-form-item label="页面动画">
              <el-switch
                v-model="quickSettings.pageTransition"
                :active-icon="VideoPlay"
                :inactive-icon="VideoPause"
                inline-prompt
              />
              <span class="setting-label">启用过渡动画</span>
            </el-form-item>

            <el-form-item label="操作反馈">
              <el-switch
                v-model="quickSettings.operationFeedback"
                :active-icon="Bell"
                :inactive-icon="Mute"
                inline-prompt
              />
              <span class="setting-label">操作提示音</span>
            </el-form-item>
          </el-form>

          <el-divider>推荐配置</el-divider>

          <div class="preset-list">
            <div
              v-for="preset in presetConfigs"
              :key="preset.value"
              class="preset-item"
              @click="handleApplyPreset(preset)"
            >
              <el-icon class="preset-icon" :class="preset.type"><component :is="preset.icon" /></el-icon>
              <div class="preset-info">
                <span class="preset-name">{{ preset.label }}</span>
                <span class="preset-desc">{{ preset.description }}</span>
              </div>
              <el-button type="primary" link>应用</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 界面元素设置 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="preference-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Grid /></el-icon> 界面元素与行为
              </span>
              <el-tooltip content="部分设置需要刷新页面后生效" placement="top">
                <el-icon class="header-tip"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>

          <el-tabs>
            <!-- 数据展示设置 -->
            <el-tab-pane>
              <template #label>
                <el-icon><DataLine /></el-icon>
                <span>数据展示</span>
              </template>
              
              <el-form :model="displaySettings" label-position="top">
                <el-row :gutter="32">
                  <el-col :span="8">
                    <el-form-item label="表格密度">
                      <el-radio-group v-model="displaySettings.tableSize" size="large">
                        <el-radio-button label="large">宽松</el-radio-button>
                        <el-radio-button label="default">默认</el-radio-button>
                        <el-radio-button label="small">紧凑</el-radio-button>
                      </el-radio-group>
                    </el-form-item>
                    
                    <el-form-item label="表格条纹">
                      <el-switch
                        v-model="displaySettings.tableStripe"
                        :active-icon="Check"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">隔行显示条纹底色</span>
                    </el-form-item>

                    <el-form-item label="表格边框">
                      <el-switch
                        v-model="displaySettings.tableBorder"
                        :active-icon="Check"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">显示单元格边框</span>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="默认分页">
                      <el-select v-model="displaySettings.pageSize" style="width: 100%">
                        <el-option label="10条/页" :value="10" />
                        <el-option label="20条/页" :value="20" />
                        <el-option label="50条/页" :value="50" />
                        <el-option label="100条/页" :value="100" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="数值千分位">
                      <el-switch
                        v-model="displaySettings.numberFormat"
                        :active-icon="Check"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">格式化大数值显示</span>
                    </el-form-item>

                    <el-form-item label="时间格式">
                      <el-select v-model="displaySettings.dateFormat" style="width: 100%">
                        <el-option label="YYYY-MM-DD HH:mm:ss" value="YYYY-MM-DD HH:mm:ss" />
                        <el-option label="YYYY-MM-DD HH:mm" value="YYYY-MM-DD HH:mm" />
                        <el-option label="MM-DD HH:mm" value="MM-DD HH:mm" />
                        <el-option label="相对时间" value="relative" />
                      </el-select>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="图表主题">
                      <el-select v-model="displaySettings.chartTheme" style="width: 100%">
                        <el-option label="明亮" value="light" />
                        <el-option label="暗黑" value="dark" />
                        <el-option label="科技蓝" value="tech-blue" />
                        <el-option label="商务风" value="business" />
                      </el-select>
                    </el-form-item>

                    <el-form-item label="图表动画">
                      <el-switch
                        v-model="displaySettings.chartAnimation"
                        :active-icon="VideoPlay"
                        :inactive-icon="VideoPause"
                        inline-prompt
                      />
                      <span class="setting-label">启用图表动画效果</span>
                    </el-form-item>

                    <el-form-item label="数据加载">
                      <el-switch
                        v-model="displaySettings.loadingAnimation"
                        :active-icon="Loading"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">显示加载动画</span>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-tab-pane>

            <!-- 操作交互设置 -->
            <el-tab-pane>
              <template #label>
                <el-icon><Mouse /></el-icon>
                <span>操作交互</span>
              </template>
              
              <el-form :model="interactionSettings" label-position="top">
                <el-row :gutter="32">
                  <el-col :span="8">
                    <el-form-item label="操作确认">
                      <el-radio-group v-model="interactionSettings.confirmLevel" size="large">
                        <el-radio-button label="none">无需确认</el-radio-button>
                        <el-radio-button label="important">重要操作</el-radio-button>
                        <el-radio-button label="all">所有操作</el-radio-button>
                      </el-radio-group>
                    </el-form-item>

                    <el-form-item label="键盘快捷键">
                      <el-switch
                        v-model="interactionSettings.keyboardShortcuts"
                        :active-icon="Check"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">启用键盘快捷操作</span>
                    </el-form-item>

                    <el-form-item label="右键菜单">
                      <el-switch
                        v-model="interactionSettings.contextMenu"
                        :active-icon="Check"
                        :inactive-icon="Close"
                        inline-prompt
                      />
                      <span class="setting-label">启用右键快捷菜单</span>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="表格行为">
                      <el-checkbox-group v-model="interactionSettings.tableFeatures">
                        <el-checkbox label="sort">列排序</el-checkbox>
                        <el-checkbox label="filter">列筛选</el-checkbox>
                        <el-checkbox label="drag">列拖拽</el-checkbox>
                        <el-checkbox label="resize">列宽调整</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>

                    <el-form-item label="搜索方式">
                      <el-radio-group v-model="interactionSettings.searchMode" size="large">
                        <el-radio-button label="instant">即时搜索</el-radio-button>
                        <el-radio-button label="submit">手动提交</el-radio-button>
                      </el-radio-group>
                    </el-form-item>

                    <el-form-item label="自动刷新">
                      <el-select v-model="interactionSettings.autoRefresh" style="width: 100%">
                        <el-option label="不自动刷新" value="0" />
                        <el-option label="每30秒" value="30" />
                        <el-option label="每1分钟" value="60" />
                        <el-option label="每5分钟" value="300" />
                      </el-select>
                    </el-form-item>
                  </el-col>

                  <el-col :span="8">
                    <el-form-item label="图表交互">
                      <el-checkbox-group v-model="interactionSettings.chartFeatures">
                        <el-checkbox label="zoom">缩放</el-checkbox>
                        <el-checkbox label="dataZoom">数据区域缩放</el-checkbox>
                        <el-checkbox label="tooltip">数据提示</el-checkbox>
                        <el-checkbox label="toolbox">工具箱</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>

                    <el-form-item label="图表工具">
                      <el-checkbox-group v-model="interactionSettings.chartTools">
                        <el-checkbox label="saveAsImage">保存图片</el-checkbox>
                        <el-checkbox label="restore">还原</el-checkbox>
                        <el-checkbox label="dataView">数据视图</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>

                    <el-form-item label="数据导出">
                      <el-checkbox-group v-model="interactionSettings.exportTypes">
                        <el-checkbox label="excel">Excel</el-checkbox>
                        <el-checkbox label="csv">CSV</el-checkbox>
                        <el-checkbox label="pdf">PDF</el-checkbox>
                      </el-checkbox-group>
                    </el-form-item>
                  </el-col>
                </el-row>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>

    <!-- 通知与权限设置 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="preference-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span class="header-label">
                <el-icon><Bell /></el-icon> 通知与权限设置
              </span>
              <el-tooltip content="设置您的通知偏好和数据权限" placement="top">
                <el-icon class="header-tip"><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </template>

          <el-tabs>
            <!-- 通知设置 -->
            <el-tab-pane>
              <template #label>
                <el-icon><Message /></el-icon>
                <span>通知设置</span>
              </template>
              
              <el-form :model="notificationSettings" label-position="top">
                <el-row :gutter="32">
                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">系统通知</span>
                        <el-switch
                          v-model="notificationSettings.systemEnabled"
                          :active-icon="Check"
                          :inactive-icon="Close"
                          inline-prompt
                        />
                      </div>
                      <div class="notification-list" :class="{ disabled: !notificationSettings.systemEnabled }">
                        <el-checkbox-group v-model="notificationSettings.systemTypes">
                          <el-checkbox label="maintenance">
                            <div class="notification-item">
                              <span class="item-label">系统维护</span>
                              <el-tag size="small" type="warning">重要</el-tag>
                            </div>
                          </el-checkbox>
                          <el-checkbox label="update">
                            <div class="notification-item">
                              <span class="item-label">功能更新</span>
                              <el-tag size="small" type="info">普通</el-tag>
                            </div>
                          </el-checkbox>
                          <el-checkbox label="announcement">
                            <div class="notification-item">
                              <span class="item-label">系统公告</span>
                              <el-tag size="small" type="info">普通</el-tag>
                            </div>
                          </el-checkbox>
                        </el-checkbox-group>
                      </div>
                    </div>
                  </el-col>

                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">数据通知</span>
                        <el-switch
                          v-model="notificationSettings.dataEnabled"
                          :active-icon="Check"
                          :inactive-icon="Close"
                          inline-prompt
                        />
                      </div>
                      <div class="notification-list" :class="{ disabled: !notificationSettings.dataEnabled }">
                        <el-checkbox-group v-model="notificationSettings.dataTypes">
                          <el-checkbox label="task">
                            <div class="notification-item">
                              <span class="item-label">任务完成</span>
                              <el-tag size="small" type="success">实时</el-tag>
                            </div>
                          </el-checkbox>
                          <el-checkbox label="analysis">
                            <div class="notification-item">
                              <span class="item-label">分析报告</span>
                              <el-tag size="small" type="warning">重要</el-tag>
                            </div>
                          </el-checkbox>
                          <el-checkbox label="threshold">
                            <div class="notification-item">
                              <span class="item-label">阈值预警</span>
                              <el-tag size="small" type="danger">紧急</el-tag>
                            </div>
                          </el-checkbox>
                          <el-checkbox label="export">
                            <div class="notification-item">
                              <span class="item-label">导出完成</span>
                              <el-tag size="small" type="info">普通</el-tag>
                            </div>
                          </el-checkbox>
                        </el-checkbox-group>
                      </div>
                    </div>
                  </el-col>

                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">通知方式</span>
                      </div>
                      <div class="notification-methods">
                        <el-form-item
                          v-for="method in notificationMethods"
                          :key="method.value"
                          :label="method.label"
                        >
                          <div class="method-item">
                            <el-switch
                              v-model="notificationSettings.methods[method.value]"
                              :active-icon="method.icon"
                              :inactive-icon="Close"
                              inline-prompt
                            />
                            <el-button
                              v-if="method.value === 'email' || method.value === 'phone'"
                              type="primary"
                              link
                              size="small"
                              @click="handleVerify(method.value)"
                            >
                              验证{{ method.label }}
                            </el-button>
                          </div>
                          <div class="method-desc">{{ method.description }}</div>
                        </el-form-item>

                        <el-form-item label="免打扰时间">
                          <el-time-picker
                            v-model="notificationSettings.quietTime"
                            is-range
                            range-separator="至"
                            start-placeholder="开始时间"
                            end-placeholder="结束时间"
                            format="HH:mm"
                            style="width: 100%"
                          />
                          <div class="method-desc">在设定的时间段内不接收通知</div>
                        </el-form-item>
                      </div>
                    </div>
                  </el-col>
                </el-row>

                <!-- 通知预览 -->
                <el-divider>通知预览</el-divider>
                <div class="preview-section">
                  <div class="preview-header">
                    <span>通知效果预览</span>
                    <el-button type="primary" link @click="handlePreviewNotification">
                      <el-icon><View /></el-icon> 发送测试通知
                    </el-button>
                  </div>
                  <div class="notification-examples">
                    <div
                      v-for="example in notificationExamples"
                      :key="example.type"
                      class="example-item"
                      :class="example.type"
                    >
                      <el-icon class="example-icon"><component :is="example.icon" /></el-icon>
                      <div class="example-content">
                        <div class="example-title">{{ example.title }}</div>
                        <div class="example-desc">{{ example.description }}</div>
                      </div>
                      <div class="example-time">{{ example.time }}</div>
                    </div>
                  </div>
                </div>
              </el-form>
            </el-tab-pane>

            <!-- 权限设置 -->
            <el-tab-pane>
              <template #label>
                <el-icon><Lock /></el-icon>
                <span>权限与隐私</span>
              </template>
              
              <el-form :model="permissionSettings" label-position="top">
                <el-row :gutter="32">
                  <!-- 数据访问权限 -->
                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">数据访问权限</span>
                        <el-tooltip content="查看您的数据访问权限" placement="top">
                          <el-button type="primary" link @click="handleViewPermissions">
                            <el-icon><View /></el-icon> 查看详情
                          </el-button>
                        </el-tooltip>
                      </div>
                      <div class="permission-list">
                        <div v-for="perm in dataPermissions" :key="perm.value" class="permission-item">
                          <div class="permission-info">
                            <el-icon class="permission-icon" :class="perm.type"><component :is="perm.icon" /></el-icon>
                            <div class="permission-content">
                              <div class="permission-name">{{ perm.label }}</div>
                              <div class="permission-desc">{{ perm.description }}</div>
                            </div>
                          </div>
                          <el-tag :type="perm.status === 'granted' ? 'success' : 'info'">
                            {{ perm.status === 'granted' ? '已授权' : '未授权' }}
                          </el-tag>
                        </div>
                      </div>
                    </div>
                  </el-col>

                  <!-- 数据共享设置 -->
                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">数据共享设置</span>
                      </div>
                      <div class="sharing-settings">
                        <el-form-item
                          v-for="item in sharingOptions"
                          :key="item.value"
                          :label="item.label"
                        >
                          <div class="sharing-item">
                            <el-select v-model="permissionSettings.sharing[item.value]" style="width: 100%">
                              <el-option
                                v-for="level in sharingLevels"
                                :key="level.value"
                                :label="level.label"
                                :value="level.value"
                              >
                                <div class="sharing-option">
                                  <el-icon><component :is="level.icon" /></el-icon>
                                  <span>{{ level.label }}</span>
                                  <el-tag size="small" :type="level.type">{{ level.desc }}</el-tag>
                                </div>
                              </el-option>
                            </el-select>
                            <div class="sharing-desc">{{ item.description }}</div>
                          </div>
                        </el-form-item>
                      </div>
                    </div>
                  </el-col>

                  <!-- 隐私保护设置 -->
                  <el-col :span="8">
                    <div class="setting-group">
                      <div class="group-header">
                        <span class="group-title">隐私保护设置</span>
                      </div>
                      <div class="privacy-settings">
                        <el-form-item
                          v-for="item in privacyOptions"
                          :key="item.value"
                          :label="item.label"
                        >
                          <div class="privacy-item">
                            <el-switch
                              v-model="permissionSettings.privacy[item.value]"
                              :active-icon="Check"
                              :inactive-icon="Close"
                              inline-prompt
                            />
                            <div class="privacy-desc">{{ item.description }}</div>
                            <div v-if="item.warning" class="privacy-warning">
                              <el-icon><Warning /></el-icon>
                              <span>{{ item.warning }}</span>
                            </div>
                          </div>
                        </el-form-item>
                      </div>
                    </div>
                  </el-col>
                </el-row>

                <!-- 安全日志 -->
                <el-divider>安全日志</el-divider>
                <div class="log-section">
                  <div class="log-header">
                    <div class="log-title">
                      <el-icon><List /></el-icon>
                      <span>近期安全活动</span>
                    </div>
                    <el-button type="primary" link @click="handleViewAllLogs">
                      查看全部日志 <el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                  <el-timeline>
                    <el-timeline-item
                      v-for="log in securityLogs"
                      :key="log.id"
                      :type="log.type"
                      :timestamp="log.time"
                      :hollow="log.type === 'info'"
                    >
                      <div class="log-content">
                        <div class="log-main">
                          <span class="log-event">{{ log.event }}</span>
                          <el-tag size="small" :type="log.type">{{ log.status }}</el-tag>
                        </div>
                        <div class="log-detail">
                          <span class="log-ip">IP: {{ log.ip }}</span>
                          <span class="log-location">{{ log.location }}</span>
                          <span class="log-device">{{ log.device }}</span>
                        </div>
                      </div>
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </el-form>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import {
  Setting, Check, RefreshRight, View,
  Brush, SelectStick, Fold,
  Expand, Document, Close, VideoPlay,
  VideoPause, Bell, Mute, Monitor,
  Sunny, Moon, Grid, InfoFilled,
  DataLine, Mouse, Loading, Message,
  ChatDotRound, Notification, PhoneFilled,
  Warning, Lock, List, ArrowRight,
  Share, UserFilled, Key, CircleCheck,
  CircleClose
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// 主题模式选项
const themeModes = [
  {
    label: '明亮模式',
    value: 'light',
    description: '默认的明亮主题，适合日间使用'
  },
  {
    label: '暗黑模式',
    value: 'dark',
    description: '护眼的深色主题，适合夜间使用'
  },
  {
    label: '跟随系统',
    value: 'auto',
    description: '自动跟随系统主题设置切换'
  }
]

// 主题色调选项
const themeColors = [
  { label: '拂晓蓝', value: '#1677ff' },
  { label: '薄暮红', value: '#f5222d' },
  { label: '极光绿', value: '#52c41a' },
  { label: '日暮橙', value: '#faad14' },
  { label: '酱紫色', value: '#722ed1' }
]

// 预定义颜色
const predefineColors = [
  '#1677ff',
  '#f5222d',
  '#52c41a',
  '#faad14',
  '#722ed1',
  '#eb2f96',
  '#13c2c2',
  '#2f54eb'
]

// 布局模式选项
const layoutModes = [
  {
    label: '侧边布局',
    value: 'side',
    description: '侧边导航布局，适合大屏设备'
  },
  {
    label: '顶部布局',
    value: 'top',
    description: '顶部导航布局，适合宽屏设备'
  },
  {
    label: '混合布局',
    value: 'mix',
    description: '混合导航布局，兼具两种布局优点'
  }
]

// 当前主题设置
const currentTheme = reactive({
  mode: 'light',
  color: '#1677ff',
  layout: 'side'
})

// 自定义颜色
const customColor = ref('')

// 快捷设置
const quickSettings = reactive({
  navigation: 'side',
  menuCollapse: false,
  tabBar: true,
  pageTransition: true,
  operationFeedback: true
})

// 预设配置
const presetConfigs = [
  {
    label: '默认推荐',
    value: 'default',
    description: '系统默认的推荐配置',
    icon: 'Monitor',
    type: 'primary'
  },
  {
    label: '日间模式',
    value: 'light',
    description: '明亮护眼的日间模式',
    icon: 'Sunny',
    type: 'warning'
  },
  {
    label: '夜间模式',
    value: 'dark',
    description: '柔和舒适的夜间模式',
    icon: 'Moon',
    type: 'info'
  }
]

// 数据展示设置
const displaySettings = reactive({
  tableSize: 'default',
  tableStripe: true,
  tableBorder: false,
  pageSize: 20,
  numberFormat: true,
  dateFormat: 'YYYY-MM-DD HH:mm:ss',
  chartTheme: 'light',
  chartAnimation: true,
  loadingAnimation: true
})

// 操作交互设置
const interactionSettings = reactive({
  confirmLevel: 'important',
  keyboardShortcuts: true,
  contextMenu: true,
  tableFeatures: ['sort', 'filter', 'resize'],
  searchMode: 'instant',
  autoRefresh: '0',
  chartFeatures: ['zoom', 'tooltip', 'toolbox'],
  chartTools: ['saveAsImage', 'restore'],
  exportTypes: ['excel', 'csv']
})

// 通知设置
const notificationSettings = reactive({
  systemEnabled: true,
  systemTypes: ['maintenance', 'update'],
  dataEnabled: true,
  dataTypes: ['task', 'analysis', 'threshold'],
  methods: {
    browser: true,
    desktop: false,
    email: true,
    phone: false
  },
  quietTime: [
    new Date(2000, 0, 1, 22, 0),
    new Date(2000, 0, 1, 8, 0)
  ]
})

// 通知方式选项
const notificationMethods = [
  {
    label: '浏览器通知',
    value: 'browser',
    icon: 'Notification',
    description: '在系统界面右上角显示通知提醒'
  },
  {
    label: '桌面通知',
    value: 'desktop',
    icon: 'Monitor',
    description: '在桌面右下角显示系统通知'
  },
  {
    label: '邮件通知',
    value: 'email',
    icon: 'Message',
    description: '发送通知邮件到您的邮箱'
  },
  {
    label: '短信通知',
    value: 'phone',
    icon: 'PhoneFilled',
    description: '发送通知短信到您的手机'
  }
]

// 通知示例
const notificationExamples = [
  {
    type: 'success',
    icon: 'Success',
    title: '数据分析任务完成',
    description: '您的月度销售分析报告已生成完成，点击查看详情',
    time: '刚刚'
  },
  {
    type: 'warning',
    icon: 'Warning',
    title: '系统维护通知',
    description: '系统将于今晚 23:00 进行例行维护，预计持续 2 小时',
    time: '10 分钟前'
  },
  {
    type: 'info',
    icon: 'Notification',
    title: '数据导出完成',
    description: '您导出的销售数据已准备就绪，请及时下载',
    time: '30 分钟前'
  }
]

// 权限设置
const permissionSettings = reactive({
  sharing: {
    personalData: 'private',
    analysisResults: 'team',
    customViews: 'public'
  },
  privacy: {
    activityTracking: true,
    dataCollection: true,
    locationService: false,
    autoSync: true
  }
})

// 数据权限列表
const dataPermissions = [
  {
    label: '数据查看权限',
    value: 'view',
    description: '查看数据的基本权限',
    icon: 'View',
    type: 'primary',
    status: 'granted'
  },
  {
    label: '数据编辑权限',
    value: 'edit',
    description: '修改和删除数据的权限',
    icon: 'Edit',
    type: 'warning',
    status: 'granted'
  },
  {
    label: '数据导出权限',
    value: 'export',
    description: '导出数据的权限',
    icon: 'Download',
    type: 'success',
    status: 'granted'
  },
  {
    label: '高级分析权限',
    value: 'analysis',
    description: '使用高级分析功能的权限',
    icon: 'DataLine',
    type: 'danger',
    status: 'pending'
  }
]

// 数据共享选项
const sharingOptions = [
  {
    label: '个人数据',
    value: 'personalData',
    description: '设置个人数据的共享范围'
  },
  {
    label: '分析结果',
    value: 'analysisResults',
    description: '设置数据分析结果的共享范围'
  },
  {
    label: '自定义视图',
    value: 'customViews',
    description: '设置自定义数据视图的共享范围'
  }
]

// 共享级别
const sharingLevels = [
  {
    label: '仅自己可见',
    value: 'private',
    icon: 'UserFilled',
    type: 'info',
    desc: '私密'
  },
  {
    label: '团队可见',
    value: 'team',
    icon: 'Share',
    type: 'warning',
    desc: '协作'
  },
  {
    label: '公开分享',
    value: 'public',
    icon: 'Link',
    type: 'success',
    desc: '公开'
  }
]

// 隐私选项
const privacyOptions = [
  {
    label: '活动追踪',
    value: 'activityTracking',
    description: '记录您的操作活动以提供个性化体验',
    warning: '关闭后将无法获得个性化推荐'
  },
  {
    label: '数据采集',
    value: 'dataCollection',
    description: '采集使用数据以改善系统功能',
    warning: '关闭后可能影响某些功能的使用'
  },
  {
    label: '位置服务',
    value: 'locationService',
    description: '使用位置信息优化访问体验',
    warning: null
  },
  {
    label: '自动同步',
    value: 'autoSync',
    description: '自动同步数据到云端备份',
    warning: '关闭后需要手动同步数据'
  }
]

// 安全日志
const securityLogs = [
  {
    id: 1,
    event: '修改数据共享设置',
    status: '成功',
    type: 'success',
    time: '2024-01-15 14:30:00',
    ip: '192.168.1.100',
    location: '广州市',
    device: 'Chrome 120.0.0'
  },
  {
    id: 2,
    event: '尝试访问未授权资源',
    status: '已拦截',
    type: 'warning',
    time: '2024-01-15 13:25:00',
    ip: '192.168.1.101',
    location: '深圳市',
    device: 'Firefox 121.0.0'
  },
  {
    id: 3,
    event: '更新隐私设置',
    status: '已完成',
    type: 'info',
    time: '2024-01-15 10:15:00',
    ip: '192.168.1.100',
    location: '广州市',
    device: 'Chrome 120.0.0'
  }
]

// 处理主题模式变更
const handleThemeModeChange = (mode) => {
  currentTheme.mode = mode
  ElMessage.success(`已切换至${themeModes.find(item => item.value === mode).label}`)
}

// 处理主题色调变更
const handleThemeColorChange = (color) => {
  currentTheme.color = color
  ElMessage.success('主题色调已更新')
}

// 处理自定义颜色变更
const handleCustomColorChange = (color) => {
  currentTheme.color = color
  ElMessage.success('已应用自定义颜色')
}

// 处理布局变更
const handleLayoutChange = (layout) => {
  currentTheme.layout = layout
  quickSettings.navigation = layout === 'top' ? 'top' : 'side'
  ElMessage.success(`已切换至${layoutModes.find(item => item.value === layout).label}`)
}

// 处理预览
const handlePreview = () => {
  ElMessage.success('正在生成预览...')
}

// 处理保存
const handleSave = () => {
  ElMessage.success('设置已保存')
}

// 处理重置
const handleReset = () => {
  ElMessageBox.confirm(
    '确定要恢复默认设置吗？当前的个性化配置将会丢失。',
    '重置确认',
    {
      confirmButtonText: '确定重置',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(() => {
    // 重置主题设置
    currentTheme.mode = 'light'
    currentTheme.color = '#1677ff'
    currentTheme.layout = 'side'
    
    // 重置快捷设置
    quickSettings.navigation = 'side'
    quickSettings.menuCollapse = false
    quickSettings.tabBar = true
    quickSettings.pageTransition = true
    quickSettings.operationFeedback = true
    
    // 重置数据展示设置
    displaySettings.tableSize = 'default'
    displaySettings.tableStripe = true
    displaySettings.tableBorder = false
    displaySettings.pageSize = 20
    displaySettings.numberFormat = true
    displaySettings.dateFormat = 'YYYY-MM-DD HH:mm:ss'
    displaySettings.chartTheme = 'light'
    displaySettings.chartAnimation = true
    displaySettings.loadingAnimation = true
    
    // 重置操作交互设置
    interactionSettings.confirmLevel = 'important'
    interactionSettings.keyboardShortcuts = true
    interactionSettings.contextMenu = true
    interactionSettings.tableFeatures = ['sort', 'filter', 'resize']
    interactionSettings.searchMode = 'instant'
    interactionSettings.autoRefresh = '0'
    interactionSettings.chartFeatures = ['zoom', 'tooltip', 'toolbox']
    interactionSettings.chartTools = ['saveAsImage', 'restore']
    interactionSettings.exportTypes = ['excel', 'csv']
    
    // 重置通知设置
    notificationSettings.systemEnabled = true
    notificationSettings.systemTypes = ['maintenance', 'update']
    notificationSettings.dataEnabled = true
    notificationSettings.dataTypes = ['task', 'analysis', 'threshold']
    notificationSettings.methods = {
      browser: true,
      desktop: false,
      email: true,
      phone: false
    }
    notificationSettings.quietTime = [
      new Date(2000, 0, 1, 22, 0),
      new Date(2000, 0, 1, 8, 0)
    ]
    
    // 重置权限设置
    permissionSettings.sharing = {
      personalData: 'private',
      analysisResults: 'team',
      customViews: 'public'
    }
    permissionSettings.privacy = {
      activityTracking: true,
      dataCollection: true,
      locationService: false,
      autoSync: true
    }
    
    ElMessage.success('已恢复默认设置')
  })
}

// 应用预设配置
const handleApplyPreset = (preset) => {
  ElMessageBox.confirm(
    `确定要应用"${preset.label}"配置吗？`,
    '应用确认',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    }
  ).then(() => {
    switch (preset.value) {
      case 'default':
        currentTheme.mode = 'light'
        currentTheme.color = '#1677ff'
        currentTheme.layout = 'side'
        quickSettings.navigation = 'side'
        quickSettings.menuCollapse = false
        quickSettings.tabBar = true
        quickSettings.pageTransition = true
        quickSettings.operationFeedback = true
        break
      case 'light':
        currentTheme.mode = 'light'
        currentTheme.color = '#52c41a'
        currentTheme.layout = 'top'
        quickSettings.navigation = 'top'
        quickSettings.menuCollapse = false
        quickSettings.tabBar = true
        quickSettings.pageTransition = true
        quickSettings.operationFeedback = false
        break
      case 'dark':
        currentTheme.mode = 'dark'
        currentTheme.color = '#722ed1'
        currentTheme.layout = 'side'
        quickSettings.navigation = 'side'
        quickSettings.menuCollapse = true
        quickSettings.tabBar = true
        quickSettings.pageTransition = true
        quickSettings.operationFeedback = false
        break
    }
    ElMessage.success(`已应用${preset.label}配置`)
  })
}

// 验证通知方式
const handleVerify = (type) => {
  if (type === 'email') {
    ElMessageBox.prompt('请输入您的邮箱地址', '验证邮箱', {
      confirmButtonText: '发送验证码',
      cancelButtonText: '取消',
      inputPattern: /[\w!#$%&'*+/=?^_`{|}~-]+(?:\.[\w!#$%&'*+/=?^_`{|}~-]+)*@(?:[\w](?:[\w-]*[\w])?\.)+[\w](?:[\w-]*[\w])?/,
      inputErrorMessage: '请输入正确的邮箱地址'
    }).then(({ value }) => {
      ElMessage.success(`验证码已发送至邮箱：${value}`)
    })
  } else if (type === 'phone') {
    ElMessageBox.prompt('请输入您的手机号码', '验证手机', {
      confirmButtonText: '发送验证码',
      cancelButtonText: '取消',
      inputPattern: /^1[3-9]\d{9}$/,
      inputErrorMessage: '请输入正确的手机号码'
    }).then(({ value }) => {
      ElMessage.success(`验证码已发送至手机：${value}`)
    })
  }
}

// 预览通知
const handlePreviewNotification = () => {
  ElMessage({
    message: '测试通知已发送',
    type: 'success'
  })
}

// 查看权限详情
const handleViewPermissions = () => {
  ElMessage('正在加载权限详情...')
}

// 查看所有日志
const handleViewAllLogs = () => {
  ElMessage('正在跳转到日志页面...')
}
</script>

<style scoped>
.preferences-container {
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

.preference-card {
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

.setting-section {
  margin-bottom: 24px;
}

.setting-section:last-child {
  margin-bottom: 0;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: bold;
}

/* 主题模式样式 */
.theme-mode-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.theme-mode-item {
  position: relative;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.theme-mode-item:hover {
  border-color: var(--el-color-primary);
}

.theme-mode-item.active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.mode-preview {
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.mode-preview.light {
  background-color: #ffffff;
  border: 1px solid #eee;
}

.mode-preview.dark {
  background-color: #141414;
}

.mode-preview.auto {
  background: linear-gradient(to right, #ffffff 50%, #141414 50%);
}

.preview-header {
  height: 20%;
  background-color: rgba(0, 0, 0, 0.1);
}

.preview-sidebar {
  float: left;
  width: 20%;
  height: 80%;
  background-color: rgba(0, 0, 0, 0.05);
}

.preview-content {
  float: right;
  width: 80%;
  height: 80%;
}

.mode-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.mode-name {
  font-size: 14px;
  font-weight: bold;
}

.mode-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.mode-checked {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--el-color-primary);
}

/* 主题色调样式 */
.theme-color-list {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.theme-color-item {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.theme-color-item:not(.custom)::after {
  content: '';
  position: absolute;
  inset: -2px;
  border-radius: 10px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.theme-color-item:not(.custom):hover::after {
  border-color: var(--el-color-primary-light-5);
}

.theme-color-item.active::after {
  border-color: var(--el-color-primary);
}

.color-checked {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #fff;
  font-size: 20px;
}

.theme-color-item.custom {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--el-bg-color-page);
  border: 1px dashed var(--el-border-color);
}

/* 布局样式 */
.layout-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.layout-item {
  position: relative;
  padding: 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.layout-item:hover {
  border-color: var(--el-color-primary);
}

.layout-item.active {
  border-color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.layout-preview {
  height: 120px;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 12px;
}

.layout-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.layout-name {
  font-size: 14px;
  font-weight: bold;
}

.layout-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.layout-checked {
  position: absolute;
  top: 8px;
  right: 8px;
  color: var(--el-color-primary);
}

/* 快捷设置样式 */
.quick-settings :deep(.el-form-item) {
  margin-bottom: 20px;
}

.setting-label {
  margin-left: 12px;
  color: var(--el-text-color-regular);
}

.preset-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.preset-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background-color: var(--el-bg-color-page);
  transition: all 0.3s;
  cursor: pointer;
}

.preset-item:hover {
  background-color: var(--el-color-primary-light-9);
}

.preset-icon {
  font-size: 24px;
  padding: 8px;
  border-radius: 8px;
}

.preset-icon.primary {
  color: var(--el-color-primary);
  background-color: var(--el-color-primary-light-9);
}

.preset-icon.warning {
  color: var(--el-color-warning);
  background-color: var(--el-color-warning-light-9);
}

.preset-icon.info {
  color: var(--el-color-info);
  background-color: var(--el-color-info-light-9);
}

.preset-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.preset-name {
  font-size: 14px;
  font-weight: bold;
}

.preset-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 新增样式 */
.header-tip {
  color: var(--el-text-color-secondary);
  font-size: 16px;
  cursor: help;
}

:deep(.el-tabs__header) {
  margin-bottom: 24px;
}

:deep(.el-tabs__nav) {
  border: none;
}

:deep(.el-tabs__item) {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
}

:deep(.el-checkbox-group) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:deep(.el-form-item__label) {
  font-weight: bold;
}

/* 通知设置样式 */
.setting-group {
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
  padding: 16px;
  height: 100%;
}

.group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.group-title {
  font-size: 15px;
  font-weight: bold;
}

.notification-list {
  transition: opacity 0.3s;
}

.notification-list.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.notification-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.item-label {
  margin-right: 8px;
}

.notification-methods {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.method-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.method-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.preview-section {
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
  padding: 16px;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: bold;
}

.notification-examples {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.example-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 12px;
  border-radius: 8px;
  background-color: var(--el-bg-color);
  transition: all 0.3s;
  cursor: pointer;
}

.example-item:hover {
  transform: translateX(4px);
}

.example-item.success .example-icon {
  color: var(--el-color-success);
}

.example-item.warning .example-icon {
  color: var(--el-color-warning);
}

.example-item.info .example-icon {
  color: var(--el-color-info);
}

.example-icon {
  font-size: 20px;
  padding: 8px;
  border-radius: 8px;
  background-color: var(--el-bg-color-page);
}

.example-content {
  flex: 1;
}

.example-title {
  font-weight: bold;
  margin-bottom: 4px;
}

.example-desc {
  font-size: 13px;
  color: var(--el-text-color-secondary);
}

.example-time {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

/* 权限设置样式 */
.permission-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.permission-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background-color: var(--el-bg-color);
  border-radius: 8px;
  transition: all 0.3s;
}

.permission-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.permission-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.permission-icon {
  font-size: 20px;
  padding: 8px;
  border-radius: 8px;
  background-color: var(--el-bg-color-page);
}

.permission-icon.primary {
  color: var(--el-color-primary);
}

.permission-icon.warning {
  color: var(--el-color-warning);
}

.permission-icon.success {
  color: var(--el-color-success);
}

.permission-icon.danger {
  color: var(--el-color-danger);
}

.permission-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.permission-name {
  font-weight: bold;
}

.permission-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.sharing-item {
  margin-bottom: 16px;
}

.sharing-option {
  display: flex;
  align-items: center;
  gap: 8px;
}

.sharing-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.privacy-item {
  margin-bottom: 16px;
}

.privacy-desc {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-top: 4px;
}

.privacy-warning {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 4px;
  font-size: 12px;
  color: var(--el-color-warning);
}

.log-section {
  background-color: var(--el-bg-color-page);
  border-radius: 8px;
  padding: 16px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.log-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: bold;
}

.log-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.log-main {
  display: flex;
  align-items: center;
  gap: 8px;
}

.log-event {
  font-weight: bold;
}

.log-detail {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

:deep(.el-timeline-item__node--success) {
  background-color: var(--el-color-success);
}

:deep(.el-timeline-item__node--warning) {
  background-color: var(--el-color-warning);
}
</style> 