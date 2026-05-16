import axios from 'axios'

const api = axios.create({
  baseURL: 'http://localhost:8005/api',
  timeout: 10000
})

export const orderApi = {
  list: (params: any) => api.get('/orders', { params }),
  detail: (id: string) => api.get(`/orders/${id}`),
  create: (data: any) => api.post('/orders', data),
  cancel: (id: string, reason: string) => api.post(`/orders/${id}/cancel`, { reason })
}

export const nodeApi = {
  retry: (nodeId: string) => api.post(`/nodes/${nodeId}/retry`),
  complete: (nodeId: string) => api.post(`/nodes/${nodeId}/complete`),
  fail: (nodeId: string, message?: string) => api.post(`/nodes/${nodeId}/fail`, { message })
}

export const alertApi = {
  list: (params: any) => api.get('/alerts', { params }),
  resolve: (alertId: string, data: any) => api.post(`/alerts/${alertId}/resolve`, data),
  scan: () => api.post('/alerts/scan'),
  stats: () => api.get('/alerts/stats'),
  createTest: (orderId: string, nodeId: string, message?: string) => 
    api.post('/alerts/test-timeout', { orderId, nodeId, message })
}

export const statsApi = {
  get: () => api.get('/stats')
}
