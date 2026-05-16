import { defineStore } from 'pinia'
import { ref } from 'vue'
import { orderApi, alertApi, statsApi } from '@/services/api'

export const useOrderStore = defineStore('order', () => {
  const orders = ref<any[]>([])
  const currentOrder = ref<any>(null)
  const alerts = ref<any[]>([])
  const stats = ref<any>(null)
  const loading = ref(false)

  const fetchOrders = async (filters?: any) => {
    loading.value = true
    try {
      const response = await orderApi.list(filters)
      if (response.data.code === 200) {
        orders.value = response.data.data.content
      }
    } catch (error) {
      console.error('Failed to fetch orders:', error)
    } finally {
      loading.value = false
    }
  }

  const fetchOrderDetail = async (orderId: string) => {
    loading.value = true
    try {
      const response = await orderApi.detail(orderId)
      if (response.data.code === 200) {
        currentOrder.value = response.data.data
      }
    } catch (error) {
      console.error('Failed to fetch order detail:', error)
    } finally {
      loading.value = false
    }
  }

  const createOrder = async (productInfo: string, shippingAddress: string) => {
    try {
      const response = await orderApi.create({ productInfo, shippingAddress })
      if (response.data.code === 200) {
        await fetchOrders()
        return response.data.data
      }
    } catch (error) {
      console.error('Failed to create order:', error)
    }
    return null
  }

  const cancelOrder = async (orderId: string, reason: string) => {
    try {
      const response = await orderApi.cancel(orderId, reason)
      if (response.data.code === 200) {
        await fetchOrders()
        return response.data.data
      }
    } catch (error) {
      console.error('Failed to cancel order:', error)
    }
    return null
  }

  const fetchAlerts = async (filters?: any) => {
    try {
      const response = await alertApi.list(filters)
      if (response.data.code === 200) {
        alerts.value = response.data.data.content
      }
    } catch (error) {
      console.error('Failed to fetch alerts:', error)
    }
  }

  const resolveAlert = async (alertId: string, resolvedBy: string, comment: string) => {
    try {
      const response = await alertApi.resolve(alertId, { resolvedBy, comment })
      if (response.data.code === 200) {
        await fetchAlerts()
        return true
      }
    } catch (error) {
      console.error('Failed to resolve alert:', error)
    }
    return false
  }

  const fetchStats = async () => {
    try {
      const response = await statsApi.get()
      if (response.data.code === 200) {
        stats.value = response.data.data
      }
    } catch (error) {
      console.error('Failed to fetch stats:', error)
    }
  }

  return {
    orders,
    currentOrder,
    alerts,
    stats,
    loading,
    fetchOrders,
    fetchOrderDetail,
    createOrder,
    cancelOrder,
    fetchAlerts,
    resolveAlert,
    fetchStats
  }
})
