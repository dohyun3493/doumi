<script setup>
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Heart, User, Building2 } from 'lucide-vue-next'
import api from '@/lib/api'

const router = useRouter()
const accountType = ref(null)

const form = ref({
  email: '',
  name: '',
  password: '',
  passwordConfirm: '',
  orgRegNo: '',
  terms1: false,
  terms2: false,
})
const errorMsg = ref('')
const isLoading = ref(false)

// 이메일 인증 상태
const code = ref('')          // 사용자가 입력한 인증코드
const codeSent = ref(false)   // 코드 발송 여부 (입력칸 표시 트리거)
const emailVerified = ref(false)
const sendingCode = ref(false)
const verifyingCode = ref(false)
const verifyMsg = ref('')     // 인증 관련 안내(성공/실패) 메시지

// 이메일을 다시 수정하면 인증 상태를 초기화한다 (다른 주소로 가입 방지)
watch(() => form.value.email, () => {
  codeSent.value = false
  emailVerified.value = false
  code.value = ''
  verifyMsg.value = ''
})

// 인증코드 발송
async function sendCode() {
  errorMsg.value = ''
  verifyMsg.value = ''
  const email = form.value.email.trim()
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errorMsg.value = '올바른 이메일 형식을 입력해주세요.'
    return
  }
  sendingCode.value = true
  try {
    await api.post('/api/v1/auth/email/send-code', { email })
    codeSent.value = true
    verifyMsg.value = '인증코드를 메일로 보냈습니다. 메일함을 확인해주세요. (5분 이내 유효)'
  } catch (error) {
    errorMsg.value = error.response?.data?.message
      || (typeof error.response?.data === 'string' ? error.response.data : '인증코드 발송에 실패했습니다.')
  } finally {
    sendingCode.value = false
  }
}

// 인증코드 검증
async function verifyCode() {
  errorMsg.value = ''
  verifyMsg.value = ''
  if (!code.value.trim()) {
    errorMsg.value = '인증코드를 입력해주세요.'
    return
  }
  verifyingCode.value = true
  try {
    await api.post('/api/v1/auth/email/verify-code', {
      email: form.value.email.trim(),
      code: code.value.trim(),
    })
    emailVerified.value = true
    verifyMsg.value = '이메일 인증이 완료되었습니다. ✅'
  } catch (error) {
    errorMsg.value = error.response?.data?.message
      || (typeof error.response?.data === 'string' ? error.response.data : '인증코드 확인에 실패했습니다.')
  } finally {
    verifyingCode.value = false
  }
}

async function handleRegister() {
  errorMsg.value = ''

  if (!form.value.email.trim() || !form.value.name.trim() || !form.value.password) {
    errorMsg.value = '모든 필드를 입력해주세요.'
    return
  }
  if (!emailVerified.value) {
    errorMsg.value = '이메일 인증을 완료해주세요.'
    return
  }
  if (form.value.password !== form.value.passwordConfirm) {
    errorMsg.value = '비밀번호가 일치하지 않습니다.'
    return
  }
  if (!form.value.terms1 || !form.value.terms2) {
    errorMsg.value = '필수 약관에 동의해주세요.'
    return
  }
  if (accountType.value === 'organization' && !/^\d{3}-\d{2}-\d{5}$/.test(form.value.orgRegNo.trim())) {
    errorMsg.value = '단체 고유번호를 123-45-67890 형식으로 입력해주세요.'
    return
  }

  isLoading.value = true
  try {
    const payload = {
      email: form.value.email.trim(),
      password: form.value.password,
      name: form.value.name.trim(),
      memberType: accountType.value === 'individual' ? 'INDIVIDUAL' : 'ORGANIZATION',
      orgRegNo: accountType.value === 'organization' ? form.value.orgRegNo.trim() : null,
    }

    await api.post('/members', payload)
    if (accountType.value === 'organization') {
      alert('단체 회원가입이 완료되었습니다.\n관리자 승인 후 캠페인 등록이 가능합니다. 로그인해주세요.')
    } else {
      alert('회원가입이 완료되었습니다. 로그인해주세요.')
    }
    router.push('/login')
  } catch (error) {
    console.error('회원가입 에러:', error)
    errorMsg.value = error.response?.data?.message
      || (typeof error.response?.data === 'string' ? error.response.data : '회원가입에 실패했습니다.')
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
        <p class="text-sm" style="color: var(--text-secondary)">회원가입</p>
      </RouterLink>

      <!-- Account Type Selection -->
      <div v-if="!accountType" class="space-y-4">
        <h2 class="font-semibold mb-4" style="color: var(--text-primary)">회원 유형을 선택하세요</h2>

        <button
          @click="accountType = 'individual'"
          class="w-full p-6 rounded-xl transition-all border-2 text-left cursor-pointer hover:border-primary"
          style="border-color: var(--border-color); background-color: var(--surface)"
        >
          <div class="flex items-center gap-4">
            <div
              class="w-12 h-12 rounded-full flex items-center justify-center"
              style="background-color: var(--accent)"
            >
              <User class="w-6 h-6" style="color: var(--primary)" />
            </div>
            <div class="text-left flex-1">
              <div class="font-semibold mb-1" style="color: var(--text-primary)">개인 회원 🙋</div>
              <div class="text-sm" style="color: var(--text-muted)">기부하고 캠페인에 참여하세요</div>
            </div>
          </div>
        </button>

        <button
          @click="accountType = 'organization'"
          class="w-full p-6 rounded-xl transition-all border-2 text-left cursor-pointer hover:border-primary"
          style="border-color: var(--border-color); background-color: var(--surface)"
        >
          <div class="flex items-center gap-4">
            <div
              class="w-12 h-12 rounded-full flex items-center justify-center"
              style="background-color: var(--accent)"
            >
              <Building2 class="w-6 h-6" style="color: var(--primary)" />
            </div>
            <div class="text-left flex-1">
              <div class="font-semibold mb-1" style="color: var(--text-primary)">단체 회원 🏢</div>
              <div class="text-sm" style="color: var(--text-muted)">캠페인을 등록하고 관리하세요</div>
            </div>
          </div>
        </button>

        <div class="flex justify-center gap-4 text-sm pt-4 items-center">
          <RouterLink to="/login" class="hover:underline font-semibold" style="color: var(--primary)">
            로그인
          </RouterLink>
          <span style="color: var(--border-color)">|</span>
          <RouterLink to="/" class="hover:underline font-semibold" style="color: var(--text-secondary)">
            홈으로
          </RouterLink>
        </div>
      </div>

      <!-- Registration Form -->
      <div v-else>
        <button
          @click="accountType = null"
          class="mb-6 flex items-center gap-1 text-sm cursor-pointer hover:text-primary"
          style="color: var(--text-secondary)"
        >
          ← 회원 유형 다시 선택
        </button>

        <form class="space-y-4" @submit.prevent="handleRegister">
          <div>
            <label class="text-sm font-medium" style="color: var(--text-primary)">이메일</label>
            <div class="flex gap-2 mt-1">
              <input
                type="email"
                v-model="form.email"
                placeholder="example@email.com"
                :disabled="emailVerified"
                class="flex-1 h-12 px-4 rounded-md border outline-none disabled:opacity-60"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
                required
              />
              <button
                type="button"
                @click="sendCode"
                :disabled="sendingCode || emailVerified"
                class="shrink-0 h-12 px-4 rounded-md text-sm font-medium text-white disabled:opacity-50 cursor-pointer"
                style="background-color: var(--primary)"
              >
                {{ emailVerified ? '인증완료' : (sendingCode ? '발송 중...' : (codeSent ? '재전송' : '인증코드 받기')) }}
              </button>
            </div>

            <!-- 인증코드 입력 (코드 발송 후, 인증 완료 전까지 노출) -->
            <div v-if="codeSent && !emailVerified" class="flex gap-2 mt-2">
              <input
                type="text"
                v-model="code"
                inputmode="numeric"
                maxlength="6"
                placeholder="인증코드 6자리"
                class="flex-1 h-12 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              />
              <button
                type="button"
                @click="verifyCode"
                :disabled="verifyingCode"
                class="shrink-0 h-12 px-4 rounded-md text-sm font-medium disabled:opacity-50 cursor-pointer border"
                style="border-color: var(--primary); color: var(--primary); background-color: var(--surface)"
              >
                {{ verifyingCode ? '확인 중...' : '확인' }}
              </button>
            </div>

            <p
              v-if="verifyMsg"
              class="text-xs mt-1"
              :style="{ color: emailVerified ? 'var(--primary)' : 'var(--text-muted)' }"
            >
              {{ verifyMsg }}
            </p>
          </div>

          <div>
            <label class="text-sm font-medium" style="color: var(--text-primary)">
              {{ accountType === 'individual' ? '닉네임' : '단체명' }}
            </label>
            <input
              type="text"
              v-model="form.name"
              :placeholder="accountType === 'individual' ? '닉네임을 입력하세요' : '단체명을 입력하세요'"
              class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required
            />
          </div>

          <div v-if="accountType === 'organization'">
            <label class="text-sm font-medium" style="color: var(--text-primary)">단체 고유번호</label>
            <input
              type="text"
              v-model="form.orgRegNo"
              placeholder="123-45-67890"
              class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required
            />
            <p class="text-xs mt-1" style="color: var(--text-muted)">
              비영리단체 고유번호를 입력하세요. 관리자 승인 후 캠페인 등록이 가능합니다.
            </p>
          </div>

          <div>
            <label class="text-sm font-medium" style="color: var(--text-primary)">비밀번호</label>
            <input
              type="password"
              v-model="form.password"
              placeholder="비밀번호를 입력하세요"
              class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required
            />
          </div>

          <div>
            <label class="text-sm font-medium" style="color: var(--text-primary)">비밀번호 확인</label>
            <input
              type="password"
              v-model="form.passwordConfirm"
              placeholder="비밀번호를 다시 입력하세요"
              class="w-full h-12 mt-1 px-4 rounded-md border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required
            />
          </div>

          <!-- Terms -->
          <div class="space-y-3 pt-4">
            <div class="flex items-center space-x-2">
              <input type="checkbox" id="terms1" v-model="form.terms1" class="w-4 h-4" />
              <label for="terms1" class="cursor-pointer text-sm" style="color: var(--text-primary)">
                이용약관 동의 (필수)
              </label>
            </div>
            <div class="flex items-center space-x-2">
              <input type="checkbox" id="terms2" v-model="form.terms2" class="w-4 h-4" />
              <label for="terms2" class="cursor-pointer text-sm" style="color: var(--text-primary)">
                개인정보처리방침 동의 (필수)
              </label>
            </div>
          </div>

          <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

          <button
            type="submit"
            :disabled="isLoading"
            class="w-full h-12 mt-6 rounded-md text-white font-medium disabled:opacity-50 cursor-pointer"
            style="background-color: var(--primary)"
          >
            {{ isLoading ? '가입 중...' : '가입하기' }}
          </button>
        </form>

        <div class="flex justify-center gap-4 text-sm mt-6 items-center">
          <RouterLink to="/login" class="hover:underline font-semibold" style="color: var(--primary)">
            로그인
          </RouterLink>
          <span style="color: var(--border-color)">|</span>
          <RouterLink to="/" class="hover:underline font-semibold" style="color: var(--text-secondary)">
            홈으로
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
