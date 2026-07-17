import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const api = axios.create({
  // 개발: http://localhost:8080 / 운영: '' (같은 도메인, Nginx가 /api 프록시) — .env.production 에서 주입
  baseURL: import.meta.env.VITE_API_BASE ?? 'http://localhost:8080',
  withCredentials: true
})

// Request Interceptor: /api prefix 통일 + Authorization 헤더 자동 주입
api.interceptors.request.use((config) => {
  // API 명세 통일: URL이 /api로 시작하지 않는 상대 경로면 자동으로 /api 추가
  if (config.url && !config.url.startsWith('/api') && !config.url.startsWith('http')) {
    config.url = `/api${config.url}`
  }

  // Authorization 헤더에 Access Token 주입
  const accessToken = localStorage.getItem('accessToken')
  if (accessToken) {
    config.headers['Authorization'] = `Bearer ${accessToken}`
  }
  return config
}, (error) => {
  return Promise.reject(error)
})

// Response Interceptor: 401 에러 감지 시 자동 토큰 재발급 및 재요청
api.interceptors.response.use((response) => {
  return response
}, async (error) => {
  const originalRequest = error.config

  // 401 에러이고, 아직 재시도를 진행하지 않은 요청인 경우
  if (error.response?.status === 401 && !originalRequest._retry) {
    originalRequest._retry = true
    
    const refreshToken = localStorage.getItem('refreshToken')
    
    if (refreshToken) {
      try {
        // 리프레시 토큰으로 새로운 Access / Refresh 토큰 재발급 요청
        const res = await axios.post('http://localhost:8080/api/v1/auth/refresh', {}, {
          headers: {
            'Refresh-Token': refreshToken
          }
        })

        // 신규 토큰 저장
        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = res.data
        localStorage.setItem('accessToken', newAccessToken)
        localStorage.setItem('refreshToken', newRefreshToken)

        // 원래 실패했던 요청의 헤더를 갱신하여 재요청 수행
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`
        return api(originalRequest)

      } catch (refreshError) {
        // 리프레시 토큰도 만료되어 실패한 경우 로그아웃 처리 및 로그인 페이지로 리다이렉트
        const authStore = useAuthStore()
        authStore.logout()
        alert('로그인 세션이 만료되었습니다. 다시 로그인해 주세요.')
        router.push('/login')
        return Promise.reject(refreshError)
      }
    }
  }

  return Promise.reject(error)
})

// 이미지 1장을 업로드하고 서버가 돌려준 접근 경로(/uploads/...)를 반환한다.
export async function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  const res = await api.post('/files', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
  return res.data.url
}

export default api
