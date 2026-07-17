<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CheckCircle, XCircle } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const status = ref('processing') // processing | success | error
const message = ref('결제를 승인하는 중입니다...')
const chargedAmount = ref(0)

onMounted(async () => {
  const { paymentKey, orderId, amount } = route.query

  if (!paymentKey || !orderId || !amount) {
    status.value = 'error'
    message.value = '결제 정보가 올바르지 않습니다.'
    return
  }

  // 같은 결제건 중복 승인 방지 (새로고침/이중 마운트 대비)
  const guardKey = 'confirmed_' + paymentKey
  if (sessionStorage.getItem(guardKey)) {
    status.value = 'success'
    message.value = '이미 충전이 완료된 결제입니다.'
    chargedAmount.value = Number(amount)
    return
  }
  sessionStorage.setItem(guardKey, '1')

  try {
    await api.post('/payments/confirm', {
      paymentKey,
      orderId,
      amount: Number(amount),
    })
    authStore.chargePoint(Number(amount))
    chargedAmount.value = Number(amount)
    status.value = 'success'
    message.value = '포인트 충전이 완료되었습니다!'
  } catch (e) {
    status.value = 'error'
    message.value = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : '결제 승인에 실패했습니다.')
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
