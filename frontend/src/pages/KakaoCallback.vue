<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'
import { kakaoRedirectUri } from '@/lib/kakao'

const router = useRouter()
const authStore = useAuthStore()
const errorMsg = ref('')

onMounted(async () => {
  const params = new URLSearchParams(window.location.search)
  const code = params.get('code')
  if (!code) {
    errorMsg.value = '카카오 인가코드를 받지 못했습니다.'
    return
  }

  try {
    // 1. 인가코드를 백엔드로 보내 우리 서비스 JWT 발급
    const res = await api.post('/api/v1/auth/kakao', {
      code,
      redirectUri: kakaoRedirectUri(),
    })
    const { accessToken, refreshToken } = res.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)

    // 2. 토큰에서 이메일(subject)을 꺼내 프로필 조회
    const email = JSON.parse(atob(accessToken.split('.')[1])).email
    const profileRes = await api.get(`/members/${email}`)
    const user = profileRes.data
    authStore.login({
      memberId: user.memberId,
      email: user.email,
      name: user.name,
      memberType: user.memberType,
      pointBalance: user.pointBalance,
      profileImageUrl: user.profileImageUrl,
    })

    router.replace('/')
  } catch (e) {
    console.error('카카오 로그인 에러:', e)
    errorMsg.value = e.response?.data?.error || '카카오 로그인에 실패했습니다.'
  }
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-6" style="background-color: var(--background)">
    <div class="text-center">
      <template v-if="!errorMsg">
        <p class="text-lg font-medium" style="color: var(--text-primary)">카카오 로그인 처리 중...</p>
        <p class="text-sm mt-2" style="color: var(--text-secondary)">잠시만 기다려 주세요.</p>
      </template>
      <template v-else>
        <p class="text-lg font-medium" style="color: var(--danger)">{{ errorMsg }}</p>
        <RouterLink to="/login" class="text-sm mt-3 inline-block hover:underline" style="color: var(--primary)">
          로그인 페이지로 돌아가기
        </RouterLink>
      </template>
    </div>
  </div>
</template>
