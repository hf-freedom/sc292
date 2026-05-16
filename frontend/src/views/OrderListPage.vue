<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useOrderStore } from '@/stores/orders'

const router = useRouter()
const store = useOrderStore()

const showCreateModal = ref(false)
const showDependencyModal = ref(false)
const selectedOrderNodes = ref<any[]>([])
const newOrder = ref({
  productInfo: '',
  shippingAddress: ''
})

const filters = ref({
  status: null,
  riskLevel: null
})

onMounted(async () => {
  await store.fetchOrders()
  await store.fetchStats()
})

const handleCreateOrder = async () => {
  if (!newOrder.value.productInfo || !newOrder.value.shippingAddress) {
    alert('请填写完整信息')
    return
  }

  await store.createOrder(newOrder.value.productInfo, newOrder.value.shippingAddress)
  showCreateModal.value = false
  newOrder.value = { productInfo: '', shippingAddress: '' }
}

const goToOrderDetail = (orderId: string) => {
  router.push(`/order/${orderId}`)
}

const goToAlerts = () => {
  router.push('/alerts')
}

const showNodeDependency = async (orderId: string) => {
  await store.fetchOrderDetail(orderId)
  if (store.currentOrder) {
    selectedOrderNodes.value = store.currentOrder.nodes
    showDependencyModal.value = true
  }
}

const getStatusColor = (status: string) => {
  const colors: any = {
    PENDING: '#6c757d',
    PROCESSING: '#1a73e8',
    COMPLETED: '#34a853',
    CANCELLED: '#ea4335',
    EXCEPTION: '#fbbc04'
  }
  return colors[status] || '#6c757d'
}

const getRiskColor = (riskLevel: string) => {
  const colors: any = {
    LOW: '#34a853',
    MEDIUM: '#fbbc04',
    HIGH: '#ff9800',
    CRITICAL: '#ea4335'
  }
  return colors[riskLevel] || '#6c757d'
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

const getNodeStatusColor = (status: string) => {
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

const getNodeProgress = (order: any) => {
  const nodeTypes = ['PAYMENT', 'PREPARING', 'SHIPPING', 'SIGNED']
  const currentIndex = nodeTypes.indexOf(order.currentNodeType)
  return {
    current: currentIndex + 1,
    total: 4,
    percentage: Math.round(((currentIndex + 1) / 4) * 100)
  }
}

const validateDependency = (nodes: any[]) => {
  const results: any[] = []
  for (let i = 0; i < nodes.length; i++) {
    const node = nodes[i]
    let canExecute = true
    let reason = ''
    
    if (i === 0) {
      canExecute = true
      reason = '首个节点，无前置依赖'
    } else {
      const prevNode = nodes[i - 1]
      if (prevNode.status === 'COMPLETED') {
        canExecute = true
        reason = `前置节点【${getNodeTypeText(prevNode.type)}】已完成`
      } else {
        canExecute = false
        reason = `前置节点【${getNodeTypeText(prevNode.type)}】未完成，当前状态: ${prevNode.status}`
      }
    }
    
    results.push({
      node,
      canExecute,
      reason,
      order: i + 1
    })
  }
  return results
}

const dependencyResults = computed(() => {
  return validateDependency(selectedOrderNodes.value)
})
</script>

<template>
  <div class="page-container">
    <header class="header">
      <h1>订单履约节点系统</h1>
      <nav>
        <button class="nav-btn" @click="goToAlerts">预警中心</button>
      </nav>
    </header>

    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-label">总订单数</div>
        <div class="stat-value">{{ store.stats?.totalOrders || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日新增</div>
        <div class="stat-value">{{ store.stats?.todayOrders || 0 }}</div>
      </div>
      <div class="stat-card warning">
        <div class="stat-label">待处理预警</div>
        <div class="stat-value">{{ store.stats?.pendingAlerts || 0 }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">完成率</div>
        <div class="stat-value">{{ ((store.stats?.completionRate || 0) * 100).toFixed(1) }}%</div>
      </div>
    </div>

    <div class="actions-bar">
      <button class="btn btn-primary" @click="showCreateModal = true">创建订单</button>
      <div class="filters">
        <select v-model="filters.status" class="filter-select">
          <option :value="null">全部状态</option>
          <option value="PENDING">待处理</option>
          <option value="PROCESSING">处理中</option>
          <option value="COMPLETED">已完成</option>
          <option value="CANCELLED">已取消</option>
        </select>
        <select v-model="filters.riskLevel" class="filter-select">
          <option :value="null">全部风险</option>
          <option value="LOW">低风险</option>
          <option value="MEDIUM">中风险</option>
          <option value="HIGH">高风险</option>
          <option value="CRITICAL">紧急</option>
        </select>
      </div>
    </div>

    <div class="table-container">
      <table class="data-table">
        <thead>
          <tr>
            <th>订单号</th>
            <th>商品信息</th>
            <th>节点执行顺序</th>
            <th>当前节点</th>
            <th>状态</th>
            <th>风险等级</th>
            <th>预计完成时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in store.orders" :key="order.orderId">
            <td>{{ order.orderNumber }}</td>
            <td>{{ order.productInfo }}</td>
            <td>
              <div class="node-progress">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: getNodeProgress(order).percentage + '%' }"></div>
                </div>
                <span class="progress-text">{{ getNodeProgress(order).current }}/4 节点</span>
              </div>
            </td>
            <td>{{ getNodeTypeText(order.currentNodeType) }}</td>
            <td>
              <span class="status-badge" :style="{ backgroundColor: getStatusColor(order.status) }">
                {{ order.status }}
              </span>
            </td>
            <td>
              <span class="risk-badge" :style="{ backgroundColor: getRiskColor(order.riskLevel) }">
                {{ order.riskLevel }}
              </span>
            </td>
            <td>{{ order.estimatedCompleteTime || '待定' }}</td>
            <td>
              <div class="action-buttons">
                <button class="btn btn-sm" @click="goToOrderDetail(order.orderId)">详情</button>
                <button class="btn btn-sm btn-info" @click="showNodeDependency(order.orderId)">依赖校验</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="store.orders.length === 0" class="empty-state">
        暂无订单数据
      </div>
    </div>

    <div v-if="showCreateModal" class="modal-overlay" @click.self="showCreateModal = false">
      <div class="modal">
        <h2>创建新订单</h2>
        <div class="form-group">
          <label>商品信息</label>
          <input v-model="newOrder.productInfo" type="text" placeholder="请输入商品信息" />
        </div>
        <div class="form-group">
          <label>收货地址</label>
          <input v-model="newOrder.shippingAddress" type="text" placeholder="请输入收货地址" />
        </div>
        <div class="node-order-info">
          <h3>节点执行顺序</h3>
          <div class="node-flow">
            <div class="flow-item">
              <span class="flow-num">1</span>
              <span class="flow-text">支付</span>
            </div>
            <span class="flow-arrow">→</span>
            <div class="flow-item">
              <span class="flow-num">2</span>
              <span class="flow-text">备货</span>
            </div>
            <span class="flow-arrow">→</span>
            <div class="flow-item">
              <span class="flow-num">3</span>
              <span class="flow-text">发货</span>
            </div>
            <span class="flow-arrow">→</span>
            <div class="flow-item">
              <span class="flow-num">4</span>
              <span class="flow-text">签收</span>
            </div>
          </div>
          <p class="dependency-note">每个节点必须在前置节点完成后才能执行</p>
        </div>
        <div class="modal-actions">
          <button class="btn" @click="showCreateModal = false">取消</button>
          <button class="btn btn-primary" @click="handleCreateOrder">创建</button>
        </div>
      </div>
    </div>

    <div v-if="showDependencyModal" class="modal-overlay" @click.self="showDependencyModal = false">
      <div class="modal dependency-modal">
        <h2>节点依赖校验</h2>
        <div class="dependency-list">
          <div 
            v-for="result in dependencyResults" 
            :key="result.node.nodeId" 
            class="dependency-item"
            :class="{ 'can-execute': result.canExecute, 'cannot-execute': !result.canExecute }"
          >
            <div class="dep-header">
              <span class="dep-order">步骤 {{ result.order }}</span>
              <span class="dep-type">{{ getNodeTypeText(result.node.type) }}</span>
              <span 
                class="dep-status" 
                :style="{ backgroundColor: getNodeStatusColor(result.node.status) }"
              >
                {{ result.node.status }}
              </span>
            </div>
            <div class="dep-content">
              <div class="dep-check">
                <span v-if="result.canExecute" class="check-icon">✓</span>
                <span v-else class="check-icon error">✗</span>
                <span class="dep-reason">{{ result.reason }}</span>
              </div>
              <div v-if="result.node.previousNodeId" class="dep-prev">
                前置节点ID: {{ result.node.previousNodeId.substring(0, 8) }}...
              </div>
              <div v-else class="dep-prev">
                无前置节点（首个节点）
              </div>
            </div>
          </div>
        </div>
        <div class="dependency-summary">
          <h3>校验说明</h3>
          <ul>
            <li>节点必须按顺序执行：支付 → 备货 → 发货 → 签收</li>
            <li>前置节点状态为 COMPLETED 时，当前节点才能执行</li>
            <li>跳过前置节点会导致业务流程异常</li>
          </ul>
        </div>
        <div class="modal-actions">
          <button class="btn btn-primary" @click="showDependencyModal = false">关闭</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-container {
  max-width: 1400px;
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

.nav-btn {
  padding: 8px 16px;
  background: #f8f9fa;
  border: 1px solid #ddd;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.nav-btn:hover {
  background: #e9ecef;
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
  margin-bottom: 16px;
}

.filters {
  display: flex;
  gap: 12px;
}

.filter-select {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  background: white;
}

.btn {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn-primary {
  background: #1a73e8;
  color: white;
}

.btn-primary:hover {
  background: #1557b0;
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn-info {
  background: #17a2b8;
  color: white;
}

.btn-info:hover {
  background: #138496;
}

.table-container {
  background: white;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
}

.data-table thead {
  background: #f8f9fa;
}

.data-table th {
  padding: 16px;
  text-align: left;
  font-weight: 600;
  font-size: 14px;
  color: #495057;
}

.data-table td {
  padding: 16px;
  border-top: 1px solid #e9ecef;
  font-size: 14px;
}

.data-table tbody tr {
  transition: background 0.2s;
}

.data-table tbody tr:hover {
  background: #f8f9fa;
}

.node-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.progress-bar {
  width: 120px;
  height: 8px;
  background: #e9ecef;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #1a73e8, #34a853);
  border-radius: 4px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 12px;
  color: #6c757d;
}

.status-badge,
.risk-badge,
.dep-status {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  color: white;
  font-size: 12px;
  font-weight: 500;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

.empty-state {
  padding: 60px 20px;
  text-align: center;
  color: #6c757d;
  font-size: 16px;
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

.dependency-modal {
  max-width: 700px;
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

.form-group input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
}

.node-order-info {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.node-order-info h3 {
  font-size: 14px;
  margin-bottom: 12px;
  color: #495057;
}

.node-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 12px;
}

.flow-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  background: white;
  padding: 8px 12px;
  border-radius: 8px;
  border: 1px solid #1a73e8;
}

.flow-num {
  width: 24px;
  height: 24px;
  background: #1a73e8;
  color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  margin-bottom: 4px;
}

.flow-text {
  font-size: 12px;
  color: #333;
}

.flow-arrow {
  color: #1a73e8;
  font-size: 18px;
}

.dependency-note {
  font-size: 12px;
  color: #6c757d;
  text-align: center;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

.dependency-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 20px;
}

.dependency-item {
  border-radius: 8px;
  padding: 16px;
  border: 2px solid;
}

.dependency-item.can-execute {
  border-color: #34a853;
  background: #e8f5e9;
}

.dependency-item.cannot-execute {
  border-color: #ea4335;
  background: #fce4ec;
}

.dep-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.dep-order {
  background: #1a73e8;
  color: white;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.dep-type {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.dep-content {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.dep-check {
  display: flex;
  align-items: center;
  gap: 8px;
}

.check-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #34a853;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.check-icon.error {
  background: #ea4335;
}

.dep-reason {
  font-size: 14px;
  color: #333;
}

.dep-prev {
  font-size: 12px;
  color: #6c757d;
  font-family: monospace;
}

.dependency-summary {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 16px;
}

.dependency-summary h3 {
  font-size: 14px;
  margin-bottom: 8px;
  color: #495057;
}

.dependency-summary ul {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  color: #6c757d;
}

.dependency-summary li {
  margin-bottom: 4px;
}
</style>
