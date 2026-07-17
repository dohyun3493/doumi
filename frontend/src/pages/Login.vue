<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, Eye, EyeOff } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'
import { getKakaoAuthUrl } from '@/lib/kakao'

const router = useRouter()
const authStore = useAuthStore()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const errorMsg = ref('')
const isLoading = ref(false)

// 카카오 로그인: 카카오 동의 화면으로 이동 → 콜백(/oauth/kakao/callback)에서 처리
function handleKakaoLogin() {
  window.location.href = getKakaoAuthUrl()
}

async function handleLogin() {
  errorMsg.value = ''
  if (!email.value.trim() || !password.value) {
    errorMsg.value = '이메일과 비밀번호를 입력해주세요.'
    return
  }
  isLoading.value = true

  try {
    // 1. JWT 로그인 요청 (POST)
    const loginRes = await api.post('/api/v1/auth/login', {
      email: email.value.trim(),
      password: password.value
    })

    // 2. 발급받은 Access / Refresh Token 저장
    const { accessToken, refreshToken } = loginRes.data
    localStorage.setItem('accessToken', accessToken)
    localStorage.setItem('refreshToken', refreshToken)

    // 3. 토큰을 이용해 백엔드에서 사용자 상세 프로필 조회
    const profileRes = await api.get(`/members/${email.value.trim()}`)
    const user = profileRes.data
    
    // 4. Pinia Auth Store에 유저 정보 세팅 및 로그인 완료 처리
    authStore.login({
      memberId: user.memberId,
      email: user.email,
      name: user.name,
      memberType: user.memberType,
      pointBalance: user.pointBalance,
      profileImageUrl: user.profileImageUrl
    })
    
    alert('로그인에 성공했습니다!')
    router.push('/')
  } catch (error) {
    console.error('로그인 에러:', error)
    if (error.response?.status === 401) {
      errorMsg.value = '이메일 또는 비밀번호가 잘못되었습니다.'
    } else if (error.response?.status === 404) {
      errorMsg.value = '존재하지 않는 회원 정보입니다. 회원가입을 먼저 진행해주세요.'
    } else {
      errorMsg.value = error.response?.data?.error || error.response?.data || '로그인 중 오류가 발생했습니다.'
    }
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div
    class="min-h-screen flex items-center justify-center px-6"
    style="background-color: var(--background)"
  >
    <div
      class="w-full max-w-md rounded-2xl p-8"
      style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.08)"
    >
      <!-- Logo -->
      <RouterLink
        to="/"
        class="flex flex-col items-center mb-8 no-underline"
        style="text-decoration: none; cursor: pointer;"
      >
        <div class="flex items-center gap-2 mb-2">
          <Heart class="w-8 h-8" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
          <span class="text-2xl font-bold" style="color: var(--text-primary)">도우미</span>
        </div>
        <p class="text-sm" style="color: var(--text-secondary)">따뜻한 마음을 나누는 기부 플랫폼</p>
      </RouterLink>

      <!-- 카카오 OAuth 로그인 -->
      <div class="space-y-3 mb-6">
        <button
          @click="handleKakaoLogin"
          class="w-full h-12 rounded-md font-medium cursor-pointer"
          style="background-color: #FEE500; color: #000000"
        >
          카카오로 빠른 시작
        </button>
      </div>

      <div class="relative my-6">
        <div class="h-px" style="background-color: var(--border-color)" />
        <span
          class="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 px-3 text-sm"
          style="background-color: var(--surface); color: var(--text-muted)"
        >
          또는
        </span>
      </div>

      <!-- Email Login Form -->
      <form class="space-y-4 mb-6" @submit.prevent="handleLogin">
        <div>
          <label for="email" class="text-sm font-medium" style="color: var(--text-primary)">이메일</label>
          <input
            id="email"
            type="email"
            v-model="email"
            placeholder="example@email.com"
            class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            required
          />
        </div>

        <div>
          <label for="password" class="text-sm font-medium" style="color: var(--text-primary)">비밀번호</label>
          <div class="relative mt-1">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="비밀번호를 입력하세요"
              class="w-full h-12 px-4 pr-12 rounded-md border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required
            />
            <button
              type="button"
              @click="showPassword = !showPassword"
              class="absolute right-4 top-1/2 -translate-y-1/2"
            >
              <EyeOff v-if="showPassword" class="w-5 h-5" style="color: var(--text-muted)" />
              <Eye v-else class="w-5 h-5" style="color: var(--text-muted)" />
            </button>
          </div>
        </div>

        <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full h-12 rounded-md text-white font-medium disabled:opacity-50 cursor-pointer"
          style="background-color: var(--primary)"
        >
          {{ isLoading ? '로그인 중...' : '로그인' }}
        </button>

        <div class="text-center">
          <RouterLink to="/forgot-password" class="text-sm hover:underline" style="color: var(--text-secondary)">
            비밀번호를 잊으셨나요?
          </RouterLink>
        </div>
      </form>

      <!-- Links -->
      <div class="flex justify-center gap-4 text-sm items-center">
        <RouterLink
          to="/register"
          class="hover:underline font-semibold"
          style="color: var(--primary)"
        >
          회원가입
        </RouterLink>
        <span style="color: var(--border-color)">|</span>
        <RouterLink
          to="/"
          class="hover:underline font-semibold"
          style="color: var(--text-secondary)"
        >
          홈으로
        </RouterLink>
      </div>
    </div>
  </div>
</template>
