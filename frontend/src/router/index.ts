import { createRouter, createWebHistory } from 'vue-router'
import OrderListPage from '@/views/OrderListPage.vue'
import OrderDetailPage from '@/views/OrderDetailPage.vue'
import AlertCenter from '@/views/AlertCenter.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: OrderListPage
    },
    {
      path: '/order/:id',
      name: 'order-detail',
      component: OrderDetailPage
    },
    {
      path: '/alerts',
      name: 'alerts',
      component: AlertCenter
    }
  ]
})

export default router
