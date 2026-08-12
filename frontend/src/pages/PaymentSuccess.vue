<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle, XCircle, Clock } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// processing: 요청 진행 중 / success: 충전 완료 / pending: 결과 미확정(서버 202) / error: 확정 실패
const status = ref('processing')
const message = ref('결제를 승인하는 중입니다...')
const chargedAmount = ref(0)

function apply(outcome) {
  status.value = outcome.state
  message.value = outcome.message
  chargedAmount.value = outcome.amount ?? 0
}

function save(key, outcome) {
  sessionStorage.setItem(key, JSON.stringify(outcome))
  return outcome
}

function read(key) {
  try {
    const parsed = JSON.parse(sessionStorage.getItem(key))
    return parsed && typeof parsed.state === 'string' ? parsed : null
  } catch {
    return null
  }
}

onMounted(async () => {
  const { paymentKey, orderId, amount } = route.query

  if (!paymentKey || !orderId || !amount) {
    apply({ state: 'error', message: '결제 정보가 올바르지 않습니다.' })
    return
  }

  // 같은 결제건 중복 승인 방지 (새로고침/이중 마운트 대비).
  // 결과까지 함께 저장해, 다시 열렸을 때 실제 결과를 그대로 복원한다.
  const guardKey = 'confirm_' + paymentKey
  const saved = read(guardKey)
  if (saved) {
    apply(saved)
    return
  }

  // 응답을 받기 전에도 '처리 중'으로 먼저 남긴다.
  // 요청 도중 새로고침되면 결과를 모르는 상태이므로 성공으로 오인하면 안 된다.
  const pending = save(guardKey, {
    state: 'pending',
    message: '결제 처리 중입니다. 잠시 후 결제 내역에서 확인해 주세요.',
    amount: Number(amount),
  })

  try {
    const res = await api.post('/payments/confirm', {
      paymentKey,
      orderId,
      amount: Number(amount),
    })

    // 202 = 서버가 토스 승인 결과를 확인하지 못한 상태(타임아웃 등).
    // 실제로는 승인됐을 수 있으므로 실패로 단정하지 않고, 충전이 확정되지 않았으니
    // 잔액도 올리지 않는다. 최종 확정은 서버의 대사 스케줄러가 한다.
    if (res.status === 202) {
      apply(pending)
      return
    }

    authStore.chargePoint(Number(amount))
    apply(save(guardKey, {
      state: 'success',
      message: '포인트 충전이 완료되었습니다!',
      amount: Number(amount),
    }))
  } catch (e) {
    apply(save(guardKey, {
      state: 'error',
      message: e.response?.data?.message
        || (typeof e.response?.data === 'string' ? e.response.data : '결제 승인에 실패했습니다.'),
    }))
  }
})
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-6" style="background-color: var(--background)">
    <div class="w-full max-w-md rounded-2xl p-8 text-center" style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.08)">
      <div v-if="status === 'processing'">
        <div class="w-16 h-16 mx-auto mb-6 rounded-full border-4 animate-spin" style="border-color: var(--primary); border-top-color: transparent" />
        <p style="color: var(--text-secondary)">{{ message }}</p>
      </div>

      <div v-else-if="status === 'success'">
        <CheckCircle class="w-16 h-16 mx-auto mb-6" style="color: var(--secondary)" />
        <h2 class="font-bold mb-2" style="font-size: 1.5rem; color: var(--text-primary)">{{ message }}</h2>
        <p class="mb-6" style="color: var(--primary); font-size: 1.25rem; font-weight: 700">
          +{{ chargedAmount.toLocaleString() }}P
        </p>
        <button @click="router.push('/mypage')" class="w-full h-12 rounded-md text-white font-medium" style="background-color: var(--primary)">
          마이페이지로 이동
        </button>
      </div>

      <!-- 결과 미확정: 승인됐을 수도 있으므로 실패로 표시하지 않는다 -->
      <div v-else-if="status === 'pending'">
        <Clock class="w-16 h-16 mx-auto mb-6" style="color: var(--primary)" />
        <h2 class="font-bold mb-2" style="font-size: 1.5rem; color: var(--text-primary)">결제 처리 중</h2>
        <p class="mb-6 text-sm" style="color: var(--text-secondary)">{{ message }}</p>
        <button @click="router.push('/mypage')" class="w-full h-12 rounded-md text-white font-medium" style="background-color: var(--primary)">
          결제 내역 확인하기
        </button>
      </div>

      <div v-else>
        <XCircle class="w-16 h-16 mx-auto mb-6" style="color: var(--danger)" />
        <h2 class="font-bold mb-2" style="font-size: 1.5rem; color: var(--text-primary)">결제 승인 실패</h2>
        <p class="mb-6 text-sm" style="color: var(--text-secondary)">{{ message }}</p>
        <button @click="router.push('/mypage')" class="w-full h-12 rounded-md border font-medium" style="border-color: var(--border-color); color: var(--text-primary)">
          마이페이지로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>
