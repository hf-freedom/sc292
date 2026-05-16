<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/orders'
import { nodeApi } from '@/services/api'

const route = useRoute()
const router = useRouter()
const store = useOrderStore()

const orderId = route.params.id as string
const showCancelModal = ref(false)
const showManualModal = ref(false)
const showSimulateFailModal = ref(false)
const cancelReason = ref('')
const selectedNode = ref<any>(null)
const manualHandler = ref('')
const manualComment = ref('')

onMounted(async () => {
  await store.fetchOrderDetail(orderId)
})

const order = computed(() => store.currentOrder?.order)
const nodes = computed(() => store.currentOrder?.nodes || [])

const handleRetryNode = async (nodeId: string) => {
  try {
    const response = await nodeApi.retry(nodeId)
    if (response.data.code === 200) {
      alert(`节点重试成功！当前重试次数: ${response.data.data.retryCount}/${response.data.data.maxRetries}`)
    }
    await store.fetchOrderDetail(orderId)
  } catch (error) {
    console.error('Failed to retry node:', error)
    alert('重试失败，请稍后再试')
  }
}

const handleCompleteNode = async (nodeId: string) => {
  try {
    await nodeApi.complete(nodeId)
    alert('节点已完成！')
    await store.fetchOrderDetail(orderId)
  } catch (error) {
    console.error('Failed to complete node:', error)
    alert('完成节点失败，请稍后再试')
  }
}

const openManualModal = (node: any) => {
  selectedNode.value = node
  manualHandler.value = ''
  manualComment.value = ''
  showManualModal.value = true
}

const handleManualProcess = async () => {
  if (!manualHandler.value.trim()) {
    alert('请输入处理人姓名')
    return
  }
  
  if (selectedNode.value) {
    try {
      await nodeApi.complete(selectedNode.value.nodeId)
      alert(`人工处理完成！处理人: ${manualHandler.value}`)
      showManualModal.value = false
      selectedNode.value = null
      await store.fetchOrderDetail(orderId)
    } catch (error) {
      console.error('Failed to manual process:', error)
      alert('人工处理失败，请稍后再试')
    }
  }
}

const openSimulateFailModal = (node: any) => {
  selectedNode.value = node
  showSimulateFailModal.value = true
}

const simulateNodeFail = async () => {
  if (selectedNode.value) {
    try {
      await nodeApi.fail(selectedNode.value.nodeId, '模拟失败：用于测试重试功能')
      alert(`节点【${getNodeTypeText(selectedNode.value.type)}】已模拟为失败状态`)
      showSimulateFailModal.value = false
      selectedNode.value = null
      await store.fetchOrderDetail(orderId)
    } catch (error) {
      console.error('Failed to simulate:', error)
      alert('模拟失败操作失败，请稍后再试')
    }
  }
}

const handleCancelOrder = async () => {
  if (!cancelReason.value.trim()) {
    alert('请输入取消原因')
    return
  }

  await store.cancelOrder(orderId, cancelReason.value)
  showCancelModal.value = false
  cancelReason.value = ''
  alert('订单已取消')
}

const goBack = () => {
  router.push('/')
}

const getStatusColor = (status: string) => {
  const colors: any = {
    PENDING: '#6c757d',
    PROCESSING: '#1a73e8',
    COMPLETED: '#34a853',
    FAILED: '#ea4335',
    TIMEOUT: '#fbbc04',
    MANUAL: '#9c27b0'
  }
  return colors[status] || '#6c757d'
}

const getStatusText = (status: string) => {
  const texts: any = {
    PENDING: '待处理',
    PROCESSING: '进行中',
    COMPLETED: '已完成',
    FAILED: '失败',
    TIMEOUT: '超时',
    MANUAL: '人工处理'
  }
  return texts[status] || status
}

const getNodeTypeText = (type: string) => {
  const texts: any = {
    PAYMENT: '支付',
    PREPARING: '备货',
    SHIPPING: '发货',
    SIGNED: '签收'
  }
  return texts[type] || type
}

const getNodeTypeIcon = (type: string) => {
  const icons: any = {
    PAYMENT: '💳',
    PREPARING: '📦',
    SHIPPING: '🚚',
    SIGNED: '✅'
  }
  return icons[type] || '📋'
}

const canRetryNode = (node: any) => {
  return (node.status === 'FAILED' || node.status === 'TIMEOUT') && node.retryCount < node.maxRetries
}

const canManualProcess = (node: any) => {
  return node.status === 'MANUAL' || (node.status === 'FAILED' && node.retryCount >= node.maxRetries)
}

const canCompleteNode = (node: any) => {
  return node.status === 'PROCESSING'
}

const isNodeFailed = (node: any) => {
  return node.status === 'FAILED' || node.status === 'TIMEOUT' || node.status === 'MANUAL'
}

const getRetryInfo = (node: any) => {
  if (node.retryCount >= node.maxRetries) {
    return { text: '重试次数已用尽', canRetry: false }
  }
  return { text: `剩余重试次数: ${node.maxRetries - node.retryCount}`, canRetry: true }
}
</script>

<template>
  <div class="page-container">
    <header class="header">
      <button class="btn" @click="goBack">← 返回列表</button>
      <h1>订单详情</h1>
      <div></div>
    </header>

    <div v-if="order" class="detail-container">
      <div class="info-card">
        <h2>基本信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <label>订单号</label>
            <span>{{ order.orderNumber }}</span>
          </div>
          <div class="info-item">
            <label>商品信息</label>
            <span>{{ order.productInfo }}</span>
          </div>
          <div class="info-item">
            <label>收货地址</label>
            <span>{{ order.shippingAddress }}</span>
          </div>
          <div class="info-item">
            <label>创建时间</label>
            <span>{{ order.createTime }}</span>
          </div>
          <div class="info-item">
            <label>预计完成时间</label>
            <span>{{ order.estimatedCompleteTime || '待定' }}</span>
          </div>
          <div class="info-item">
            <label>订单状态</label>
            <span class="status-badge" :style="{ backgroundColor: getStatusColor(order.status) }">
              {{ getStatusText(order.status) }}
            </span>
          </div>
        </div>

        <div v-if="order.cancelled" class="cancellation-info">
          <h3>取消信息</h3>
          <p>取消原因：{{ order.cancellationReason }}</p>
        </div>

        <div v-if="order.compensations && order.compensations.length > 0" class="compensation-info">
          <h3>补偿记录</h3>
          <div v-for="(comp, index) in order.compensations" :key="index" class="compensation-item">
            <span class="comp-type">{{ comp.type }}</span>
            <span class="comp-desc">{{ comp.description }}</span>
          </div>
        </div>

        <button v-if="!order.cancelled" class="btn btn-danger" @click="showCancelModal = true">
          取消订单
        </button>
      </div>

      <div class="node-progress-card">
        <h2>节点进度</h2>
        <div class="node-timeline">
          <div
            v-for="(node, index) in nodes"
            :key="node.nodeId"
            class="node-item"
            :class="{ 
              completed: node.status === 'COMPLETED', 
              active: node.status === 'PROCESSING',
              failed: isNodeFailed(node),
              manual: node.status === 'MANUAL'
            }"
          >
            <div class="node-header">
              <div class="node-icon">{{ getNodeTypeIcon(node.type) }}</div>
              <div class="node-order">步骤 {{ index + 1 }}</div>
            </div>
            <div class="node-content">
              <div class="node-title">{{ getNodeTypeText(node.type) }}</div>
              <div class="node-status-row">
                <span class="node-status" :style="{ backgroundColor: getStatusColor(node.status) }">
                  {{ getStatusText(node.status) }}
                </span>
                <span v-if="node.retryCount > 0" class="retry-count">
                  重试 {{ node.retryCount }}/{{ node.maxRetries }}
                </span>
              </div>
              <div class="node-time" v-if="node.actualCompleteTime">
                完成时间: {{ node.actualCompleteTime }}
              </div>
              <div class="node-error" v-if="node.errorMessage">
                错误: {{ node.errorMessage }}
              </div>
              <div class="node-time" v-if="node.startTime && !node.actualCompleteTime">
                开始时间: {{ node.startTime }}
              </div>
            </div>
            <div class="node-actions">
              <button
                v-if="canRetryNode(node)"
                class="btn btn-sm btn-warning"
                @click="handleRetryNode(node.nodeId)"
              >
                重试节点
              </button>
              <button
                v-if="canCompleteNode(node)"
                class="btn btn-sm btn-success"
                @click="handleCompleteNode(node.nodeId)"
              >
                完成节点
              </button>
              <button
                v-if="canManualProcess(node)"
                class="btn btn-sm btn-manual"
                @click="openManualModal(node)"
              >
                人工处理
              </button>
              <button
                v-if="node.status === 'PROCESSING'"
                class="btn btn-sm btn-danger-outline"
                @click="openSimulateFailModal(node)"
              >
                模拟失败
              </button>
            </div>
            <div v-if="isNodeFailed(node)" class="node-retry-info">
              <span :class="{ 'text-danger': !getRetryInfo(node).canRetry }">
                {{ getRetryInfo(node).text }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <div class="logs-card">
        <h2>操作日志</h2>
        <div class="logs-list">
          <div v-for="node in nodes" :key="node.nodeId">
            <div v-for="(log, logIndex) in node.operationLogs" :key="logIndex" class="log-item">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-action" :class="'action-' + log.action.toLowerCase()">{{ log.action }}</span>
              <span class="log-operator">{{ log.operator }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
          </div>
          <div v-if="nodes.every(n => n.operationLogs.length === 0)" class="empty-logs">
            暂无操作日志
          </div>
        </div>
      </div>
    </div>

    <div v-if="showCancelModal" class="modal-overlay" @click.self="showCancelModal = false">
      <div class="modal">
        <h2>取消订单</h2>
        <div class="form-group">
          <label>取消原因</label>
          <textarea v-model="cancelReason" rows="4" placeholder="请输入取消原因"></textarea>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showCancelModal = false">取消</button>
          <button class="btn btn-danger" @click="handleCancelOrder">确认取消</button>
        </div>
      </div>
    </div>

    <div v-if="showManualModal" class="modal-overlay" @click.self="showManualModal = false">
      <div class="modal">
        <h2>人工处理节点</h2>
        <div class="manual-info">
          <p><strong>节点类型:</strong> {{ selectedNode ? getNodeTypeText(selectedNode.type) : '' }}</p>
          <p><strong>当前状态:</strong> {{ selectedNode ? getStatusText(selectedNode.status) : '' }}</p>
          <p v-if="selectedNode?.errorMessage"><strong>错误信息:</strong> {{ selectedNode.errorMessage }}</p>
        </div>
        <div class="form-group">
          <label>处理人 <span class="required">*</span></label>
          <input v-model="manualHandler" type="text" placeholder="请输入处理人姓名" />
        </div>
        <div class="form-group">
          <label>处理说明</label>
          <textarea v-model="manualComment" rows="3" placeholder="请输入处理说明（可选）"></textarea>
        </div>
        <div class="manual-warning">
          <span class="warning-icon">⚠️</span>
          <span>人工处理将直接完成当前节点，请确认问题已解决</span>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showManualModal = false">取消</button>
          <button class="btn btn-manual" @click="handleManualProcess">确认处理</button>
        </div>
      </div>
    </div>

    <div v-if="showSimulateFailModal" class="modal-overlay" @click.self="showSimulateFailModal = false">
      <div class="modal">
        <h2>模拟节点失败</h2>
        <div class="simulate-info">
          <p>将节点【{{ selectedNode ? getNodeTypeText(selectedNode.type) : '' }}】设置为失败状态</p>
          <p class="text-muted">此功能用于测试重试和人工处理流程</p>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showSimulateFailModal = false">取消</button>
          <button class="btn btn-danger" @click="simulateNodeFail">确认模拟</button>
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

.detail-container {
  display: grid;
  gap: 24px;
}

.info-card,
.node-progress-card,
.logs-card {
  background: white;
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
}

h2 {
  font-size: 18px;
  margin-bottom: 20px;
  color: #1a73e8;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-item label {
  font-size: 12px;
  color: #6c757d;
  font-weight: 500;
}

.info-item span {
  font-size: 14px;
  color: #333;
}

.status-badge,
.node-status {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 500;
  width: fit-content;
}

.cancellation-info,
.compensation-info {
  background: #fff3cd;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.cancellation-info h3,
.compensation-info h3 {
  font-size: 14px;
  margin-bottom: 8px;
}

.compensation-item {
  display: flex;
  gap: 12px;
  padding: 8px 0;
}

.comp-type {
  background: #fbbc04;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.node-timeline {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  padding: 20px 0;
}

.node-item {
  flex: 1;
  min-width: 220px;
  background: #f8f9fa;
  padding: 20px;
  border-radius: 12px;
  position: relative;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.node-item.completed {
  background: #e8f5e9;
  border-color: #34a853;
}

.node-item.active {
  background: #e3f2fd;
  border-color: #1a73e8;
}

.node-item.failed {
  background: #fce4ec;
  border-color: #ea4335;
}

.node-item.manual {
  background: #f3e5f5;
  border-color: #9c27b0;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.node-icon {
  font-size: 32px;
}

.node-order {
  background: #1a73e8;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 600;
}

.node-content {
  margin-bottom: 12px;
}

.node-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 8px;
}

.node-status-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.retry-count {
  font-size: 11px;
  color: #fbbc04;
  background: #fff8e1;
  padding: 2px 6px;
  border-radius: 4px;
}

.node-time {
  font-size: 12px;
  color: #6c757d;
}

.node-error {
  font-size: 12px;
  color: #ea4335;
  background: #ffebee;
  padding: 4px 8px;
  border-radius: 4px;
  margin-top: 8px;
}

.node-retry-info {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed #ddd;
  font-size: 12px;
  color: #6c757d;
}

.text-danger {
  color: #ea4335;
  font-weight: 500;
}

.text-muted {
  color: #6c757d;
  font-size: 13px;
}

.node-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

.btn:hover {
  background: #e9ecef;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-danger {
  background: #ea4335;
  color: white;
}

.btn-danger:hover {
  background: #c62828;
}

.btn-danger-outline {
  background: white;
  color: #ea4335;
  border: 1px solid #ea4335;
}

.btn-danger-outline:hover {
  background: #ffebee;
}

.btn-warning {
  background: #fbbc04;
  color: white;
}

.btn-warning:hover {
  background: #f9a825;
}

.btn-success {
  background: #34a853;
  color: white;
}

.btn-success:hover {
  background: #2e7d32;
}

.btn-manual {
  background: #9c27b0;
  color: white;
}

.btn-manual:hover {
  background: #7b1fa2;
}

.logs-list {
  max-height: 400px;
  overflow-y: auto;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 12px;
  border-bottom: 1px solid #e9ecef;
  font-size: 13px;
  align-items: center;
}

.log-time {
  color: #6c757d;
  white-space: nowrap;
  min-width: 180px;
}

.log-action {
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
  font-size: 11px;
  min-width: 60px;
  text-align: center;
}

.action_create { background: #e8f5e9; color: #34a853; }
.action_activate { background: #e3f2fd; color: #1a73e8; }
.action_complete { background: #e8f5e9; color: #34a853; }
.action_retry { background: #fff8e1; color: #f9a825; }
.action_manual { background: #f3e5f5; color: #9c27b0; }

.log-operator {
  color: #1a73e8;
  font-weight: 500;
  min-width: 60px;
}

.log-message {
  flex: 1;
  color: #333;
}

.empty-logs {
  padding: 40px 20px;
  text-align: center;
  color: #6c757d;
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

.manual-info {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.manual-info p {
  margin: 8px 0;
  font-size: 14px;
}

.manual-warning {
  background: #fff8e1;
  padding: 12px 16px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #f57c00;
}

.warning-icon {
  font-size: 18px;
}

.simulate-info {
  background: #ffebee;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.simulate-info p {
  margin: 8px 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}
</style>
