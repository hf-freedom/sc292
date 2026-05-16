<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/orders'
import { alertApi } from '@/services/api'

const router = useRouter()
const store = useOrderStore()

const showResolveModal = ref(false)
const showTestModal = ref(false)
const selectedAlert = ref<any>(null)
const resolveForm = ref({
  resolvedBy: '',
  comment: ''
})
const testForm = ref({
  orderId: '',
  nodeId: '',
  message: '测试超时预警'
})
const alertStats = ref<any>(null)
const scanning = ref(false)

const filters = ref({
  type: null,
  resolved: null
})

onMounted(async () => {
  await loadAlerts()
})

const loadAlerts = async () => {
  await store.fetchAlerts()
  await loadStats()
}

const loadStats = async () => {
  try {
    const response = await alertApi.stats()
    if (response.data.code === 200) {
      alertStats.value = response.data.data
    }
  } catch (error) {
    console.error('Failed to load stats:', error)
  }
}

const goBack = () => {
  router.push('/')
}

const openResolveModal = (alert: any) => {
  selectedAlert.value = alert
  resolveForm.value = { resolvedBy: '', comment: '' }
  showResolveModal.value = true
}

const handleResolveAlert = async () => {
  if (!resolveForm.value.resolvedBy.trim()) {
    alert('请输入处理人')
    return
  }

  await store.resolveAlert(
    selectedAlert.value.alertId,
    resolveForm.value.resolvedBy,
    resolveForm.value.comment
  )

  showResolveModal.value = false
  selectedAlert.value = null
  await loadAlerts()
}

const handleManualScan = async () => {
  scanning.value = true
  try {
    const response = await alertApi.scan()
    if (response.data.code === 200) {
      alert(`扫描完成！发现 ${response.data.data.unresolvedAlerts} 条未处理预警`)
      await loadAlerts()
    }
  } catch (error) {
    console.error('Failed to scan:', error)
    alert('扫描失败，请稍后再试')
  } finally {
    scanning.value = false
  }
}

const openTestModal = () => {
  testForm.value = { orderId: '', nodeId: '', message: '测试超时预警' }
  showTestModal.value = true
}

const handleCreateTestAlert = async () => {
  if (!testForm.value.orderId.trim()) {
    alert('请输入订单ID')
    return
  }

  try {
    const response = await alertApi.createTest(
      testForm.value.orderId,
      testForm.value.nodeId,
      testForm.value.message
    )
    if (response.data.code === 200) {
      alert('测试预警已创建')
      showTestModal.value = false
      await loadAlerts()
    }
  } catch (error) {
    console.error('Failed to create test alert:', error)
    alert('创建测试预警失败')
  }
}

const getAlertTypeText = (type: string) => {
  const texts: any = {
    TIMEOUT: '超时预警',
    STUCK: '卡住预警',
    FAILURE: '失败预警',
    RISK_UP: '风险升级'
  }
  return texts[type] || type
}

const getAlertLevelColor = (level: string) => {
  const colors: any = {
    INFO: '#1a73e8',
    WARNING: '#fbbc04',
    ERROR: '#ea4335',
    CRITICAL: '#9c27b0'
  }
  return colors[level] || '#6c757d'
}

const getAlertLevelText = (level: string) => {
  const texts: any = {
    INFO: '信息',
    WARNING: '警告',
    ERROR: '错误',
    CRITICAL: '严重'
  }
  return texts[level] || level
}
</script>

<template>
  <div class="page-container">
    <header class="header">
      <button class="btn" @click="goBack">← 返回列表</button>
      <h1>预警中心</h1>
      <div></div>
    </header>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总预警数</div>
        <div class="stat-value">{{ alertStats?.total || 0 }}</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-label">未处理预警</div>
        <div class="stat-value">{{ alertStats?.unresolved || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">超时预警</div>
        <div class="stat-value">{{ alertStats?.timeoutCount || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">卡住预警</div>
        <div class="stat-value">{{ alertStats?.stuckCount || 0 }}</div>
      </div>
    </div>

    <div class="actions-bar">
      <div class="left-actions">
        <button class="btn btn-primary" @click="handleManualScan" :disabled="scanning">
          {{ scanning ? '扫描中...' : '手动扫描' }}
        </button>
        <button class="btn btn-info" @click="openTestModal">创建测试预警</button>
      </div>
      <div class="filters">
        <select v-model="filters.type" class="filter-select">
          <option :value="null">全部类型</option>
          <option value="TIMEOUT">超时预警</option>
          <option value="STUCK">卡住预警</option>
          <option value="FAILURE">失败预警</option>
          <option value="RISK_UP">风险升级</option>
        </select>
        <select v-model="filters.resolved" class="filter-select">
          <option :value="null">全部状态</option>
          <option :value="false">未处理</option>
          <option :value="true">已处理</option>
        </select>
      </div>
    </div>

    <div class="alerts-grid" v-if="store.alerts.length > 0">
      <div
        v-for="alert in store.alerts"
        :key="alert.alertId"
        class="alert-card"
        :class="{ resolved: alert.resolved }"
      >
        <div class="alert-header">
          <span class="alert-type">{{ getAlertTypeText(alert.type) }}</span>
          <span class="alert-level" :style="{ backgroundColor: getAlertLevelColor(alert.level) }">
            {{ getAlertLevelText(alert.level) }}
          </span>
        </div>
        <div class="alert-message">{{ alert.message }}</div>
        <div class="alert-meta">
          <span>订单: {{ alert.orderId.substring(0, 8) }}...</span>
          <span>{{ alert.createTime }}</span>
        </div>
        <div v-if="alert.resolved" class="resolve-info">
          <span>处理人: {{ alert.resolvedBy }}</span>
          <span>{{ alert.resolvedTime }}</span>
        </div>
        <button
          v-if="!alert.resolved"
          class="btn btn-sm btn-primary"
          @click="openResolveModal(alert)"
        >
          处理
        </button>
      </div>
    </div>

    <div v-else class="empty-state">
      <div class="empty-icon">📭</div>
      <h3>暂无预警信息</h3>
      <p>当前没有预警数据，您可以：</p>
      <ul>
        <li>点击"手动扫描"检查超时和卡住的节点</li>
        <li>点击"创建测试预警"添加测试数据</li>
        <li>在订单详情页模拟节点失败来触发预警</li>
      </ul>
    </div>

    <div v-if="showResolveModal" class="modal-overlay" @click.self="showResolveModal = false">
      <div class="modal">
        <h2>处理预警</h2>
        <div class="form-group">
          <label>处理人 <span class="required">*</span></label>
          <input v-model="resolveForm.resolvedBy" type="text" placeholder="请输入处理人姓名" />
        </div>
        <div class="form-group">
          <label>处理说明</label>
          <textarea v-model="resolveForm.comment" rows="4" placeholder="请输入处理说明（可选）"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showResolveModal = false">取消</button>
          <button class="btn btn-primary" @click="handleResolveAlert">确认</button>
        </div>
      </div>
    </div>

    <div v-if="showTestModal" class="modal-overlay" @click.self="showTestModal = false">
      <div class="modal">
        <h2>创建测试预警</h2>
        <div class="test-info">
          <p>用于测试预警功能，创建一个模拟的超时预警</p>
        </div>
        <div class="form-group">
          <label>订单ID <span class="required">*</span></label>
          <input v-model="testForm.orderId" type="text" placeholder="请输入订单ID" />
        </div>
        <div class="form-group">
          <label>节点ID</label>
          <input v-model="testForm.nodeId" type="text" placeholder="可选，输入节点ID" />
        </div>
        <div class="form-group">
          <label>预警消息</label>
          <input v-model="testForm.message" type="text" placeholder="预警消息内容" />
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showTestModal = false">取消</button>
          <button class="btn btn-warning" @click="handleCreateTestAlert">创建</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.header h1 {
  font-size: 24px;
  font-weight: 600;
  color: #1a73e8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.stat-card.warning {
  border-left: 4px solid #fbbc04;
}

.stat-label {
  font-size: 14px;
  color: #6c757d;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: #1a73e8;
}

.actions-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.left-actions {
  display: flex;
  gap: 12px;
}

.filters {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 10px 16px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
  font-size: 14px;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  background: #f8f9fa;
  color: #333;
}

.btn:hover:not(:disabled) {
  background: #e9ecef;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-sm {
  padding: 8px 16px;
  font-size: 12px;
}

.btn-primary {
  background: #1a73e8;
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: #1557b0;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #138496;
}

.btn-warning {
  background: #fbbc04;
  color: white;
}

.btn-warning:hover {
  background: #f9a825;
}

.alerts-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.alert-card {
  background: white;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  border-left: 4px solid #ea4335;
  transition: all 0.2s;
}

.alert-card.resolved {
  border-left-color: #34a853;
  opacity: 0.7;
}

.alert-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.alert-type {
  font-weight: 600;
  font-size: 16px;
  color: #333;
}

.alert-level {
  padding: 4px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.alert-message {
  font-size: 14px;
  color: #555;
  margin-bottom: 12px;
  line-height: 1.5;
}

.alert-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #6c757d;
  margin-bottom: 12px;
}

.resolve-info {
  background: #e8f5e9;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 12px;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.empty-state {
  padding: 60px 40px;
  text-align: center;
  color: #6c757d;
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

.empty-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 20px;
  margin-bottom: 12px;
  color: #333;
}

.empty-state p {
  margin-bottom: 12px;
}

.empty-state ul {
  text-align: left;
  display: inline-block;
  margin: 0;
  padding-left: 20px;
}

.empty-state li {
  margin-bottom: 8px;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  padding: 32px;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
}

.modal h2 {
  margin-bottom: 24px;
  font-size: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.form-group textarea {
  resize: vertical;
}

.required {
  color: #ea4335;
}

.test-info {
  background: #e3f2fd;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #1a73e8;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
