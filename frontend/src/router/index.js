import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/components/Layout.vue'

import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: Layout,
      children: [
        { path: '', component: () => import('@/pages/Home.vue') },
        { path: 'campaigns', component: () => import('@/pages/Campaigns.vue') },
        { path: 'campaigns/:id', component: () => import('@/pages/CampaignDetail.vue') },
        { path: 'donate/:id', component: () => import('@/pages/Donate.vue') },
        { path: 'mypage', component: () => import('@/pages/MyPage.vue') },
        { path: 'ranking', component: () => import('@/pages/Ranking.vue') },
        { path: 'about', component: () => import('@/pages/About.vue') },
        { path: 'board', component: () => import('@/pages/Board.vue') },
        { path: 'board/write', component: () => import('@/pages/BoardWrite.vue') },
        { path: 'board/:id', component: () => import('@/pages/BoardDetail.vue') },
        { path: 'campaigns/register', component: () => import('@/pages/CampaignRegister.vue') },
        { path: 'campaigns/:id/edit', component: () => import('@/pages/CampaignRegister.vue') },
        { path: 'campaigns/:id/delete-request', component: () => import('@/pages/CampaignDeleteRequest.vue') },
        { path: ':pathMatch(.*)*', component: () => import('@/pages/NotFound.vue') },
      ],
    },
    { path: '/login', component: () => import('@/pages/Login.vue') },
    { path: '/oauth/kakao/callback', component: () => import('@/pages/KakaoCallback.vue') },
    { path: '/register', component: () => import('@/pages/Register.vue') },
    { path: '/forgot-password', component: () => import('@/pages/ForgotPassword.vue') },
    { path: '/admin', component: () => import('@/pages/Admin.vue') },
    { path: '/payment/checkout', component: () => import('@/pages/PaymentCheckout.vue') },
    { path: '/payment/success', component: () => import('@/pages/PaymentSuccess.vue') },
    { path: '/payment/fail', component: () => import('@/pages/PaymentFail.vue') },
  ],
})

// 내비게이션 가드: 로그인 + 역할(Role) 접근 제어
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 1) 로그인이 필요한 주소들
  const protectedRoutes = ['/mypage', '/payment/checkout', '/admin', '/campaigns/register', '/board/write']
  // 2) 역할이 필요한 주소들 (허용 역할 목록)
  const roleRoutes = {
    '/campaigns/register': ['ORGANIZATION', 'ADMIN'], // 단체·관리자만
    '/admin': ['ADMIN'],                              // 관리자만
  }

  // 캠페인 수정/삭제요청: 동적 경로(/campaigns/5/edit, /campaigns/5/delete-request)라 정규식으로 별도 처리.
  // 로그인 + 단체/관리자만 (실제 소유자 검증은 백엔드에서 수행)
  const isCampaignEdit = /^\/campaigns\/\d+\/edit$/.test(to.path)
  const isCampaignDeleteRequest = /^\/campaigns\/\d+\/delete-request$/.test(to.path)
  const isOrgOnlyCampaignRoute = isCampaignEdit || isCampaignDeleteRequest

  // 로그인 체크
  if ((protectedRoutes.includes(to.path) || isOrgOnlyCampaignRoute) && !authStore.isAuthenticated) {
    alert('로그인이 필요한 서비스입니다.')
    return next('/login')
  }

  // 역할 체크
  const allowedRoles = roleRoutes[to.path] || (isOrgOnlyCampaignRoute ? ['ORGANIZATION', 'ADMIN'] : null)
  if (allowedRoles && !allowedRoles.includes(authStore.currentUser?.memberType)) {
    alert('접근 권한이 없습니다.')
    return next('/') // 권한 없으면 홈으로
  }

  next()
})

export default router
