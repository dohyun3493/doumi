<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { Heart } from 'lucide-vue-next'
import api from '@/lib/api'

const router = useRouter()

// step 1: 이메일 입력 → 코드 발송 / step 2: 코드 + 새 비밀번호 입력
const step = ref(1)
const email = ref('')
const code = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')

const errorMsg = ref('')
const infoMsg = ref('')
const isLoading = ref(false)

// 인증코드 발송
async function sendCode() {
  errorMsg.value = ''
  infoMsg.value = ''
  const value = email.value.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
    errorMsg.value = '올바른 이메일 형식을 입력해주세요.'
    return
  }
  isLoading.value = true
  try {
    await api.post('/api/v1/auth/password/send-code', { email: value })
    step.value = 2
    infoMsg.value = '인증코드를 메일로 보냈습니다. 메일함을 확인해주세요. (30분 이내 유효)'
  } catch (error) {
    errorMsg.value = error.response?.data?.message
      || (typeof error.response?.data === 'string' ? error.response.data : '인증코드 발송에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

// 코드 검증 + 비밀번호 재설정
async function resetPassword() {
  errorMsg.value = ''
  infoMsg.value = ''
  if (!code.value.trim()) {
    errorMsg.value = '인증코드를 입력해주세요.'
    return
  }
  if (!newPassword.value) {
    errorMsg.value = '새 비밀번호를 입력해주세요.'
    return
  }
  if (newPassword.value !== newPasswordConfirm.value) {
    errorMsg.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  isLoading.value = true
  try {
    await api.post('/api/v1/auth/password/reset', {
      email: email.value.trim(),
      code: code.value.trim(),
      newPassword: newPassword.value,
    })
    alert('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.')
    router.push('/login')
  } catch (error) {
    errorMsg.value = error.response?.data?.message
      || (typeof error.response?.data === 'string' ? error.response.data : '비밀번호 변경에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div
    class="min-h-screen flex items-center justify-center px-6 py-12"
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
        <p class="text-sm" style="color: var(--text-secondary)">비밀번호 찾기</p>
      </RouterLink>

      <!-- Step 1: 이메일 입력 -->
      <form v-if="step === 1" class="space-y-4" @submit.prevent="sendCode">
        <p class="text-sm" style="color: var(--text-secondary)">
          가입하신 이메일로 인증코드를 보내드립니다.
        </p>
        <div>
          <label class="text-sm font-medium" style="color: var(--text-primary)">이메일</label>
          <input
            type="email"
            v-model="email"
            placeholder="example@email.com"
            class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            required
          />
        </div>

        <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full h-12 mt-2 rounded-md text-white font-medium disabled:opacity-50 cursor-pointer"
          style="background-color: var(--primary)"
        >
          {{ isLoading ? '발송 중...' : '인증코드 받기' }}
        </button>
      </form>

      <!-- Step 2: 코드 + 새 비밀번호 -->
      <form v-else class="space-y-4" @submit.prevent="resetPassword">
        <p v-if="infoMsg" class="text-xs" style="color: var(--text-muted)">{{ infoMsg }}</p>

        <div>
          <label class="text-sm font-medium" style="color: var(--text-primary)">인증코드</label>
          <input
            type="text"
            v-model="code"
            inputmode="numeric"
            maxlength="6"
            placeholder="인증코드 6자리"
            class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            required
          />
        </div>

        <div>
          <label class="text-sm font-medium" style="color: var(--text-primary)">새 비밀번호</label>
          <input
            type="password"
            v-model="newPassword"
            placeholder="새 비밀번호를 입력하세요"
            class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            required
          />
        </div>

        <div>
          <label class="text-sm font-medium" style="color: var(--text-primary)">새 비밀번호 확인</label>
          <input
            type="password"
            v-model="newPasswordConfirm"
            placeholder="새 비밀번호를 다시 입력하세요"
            class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            required
          />
        </div>

        <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <button
          type="submit"
          :disabled="isLoading"
          class="w-full h-12 mt-2 rounded-md text-white font-medium disabled:opacity-50 cursor-pointer"
          style="background-color: var(--primary)"
        >
          {{ isLoading ? '변경 중...' : '비밀번호 변경' }}
        </button>

        <button
          type="button"
          @click="sendCode"
          :disabled="isLoading"
          class="w-full text-sm hover:underline cursor-pointer"
          style="color: var(--text-secondary)"
        >
          인증코드 재전송
        </button>
      </form>

      <!-- Links -->
      <div class="flex justify-center gap-4 text-sm mt-6 items-center">
        <RouterLink to="/login" class="hover:underline font-semibold" style="color: var(--primary)">
          로그인
        </RouterLink>
        <span style="color: var(--border-color)">|</span>
        <RouterLink to="/register" class="hover:underline font-semibold" style="color: var(--text-secondary)">
          회원가입
        </RouterLink>
      </div>
    </div>
  </div>
</template>
