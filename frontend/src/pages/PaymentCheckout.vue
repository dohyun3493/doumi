<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { loadTossPayments, ANONYMOUS } from '@tosspayments/tosspayments-sdk'
import { TOSS_CLIENT_KEY } from '@/lib/toss'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const amount = Number(route.query.amount) || 0
const widgets = ref(null)
const ready = ref(false)
const errorMsg = ref('')

function generateOrderId() {
  return 'order_' + Date.now() + '_' + Math.random().toString(36).slice(2, 8)
}

onMounted(async () => {
  if (!authStore.currentUser?.email) {
    router.push('/login')
    return
  }
  if (amount < 1) {
    errorMsg.value = '충전 금액이 올바르지 않습니다.'
    return
  }

  try {
    const tossPayments = await loadTossPayments(TOSS_CLIENT_KEY)
    const w = tossPayments.widgets({ customerKey: ANONYMOUS })

    // 결제 금액 설정 (렌더링보다 반드시 선행)
    await w.setAmount({ currency: 'KRW', value: amount })

    // 결제수단 + 약관 위젯 렌더링 (공식 샘플과 동일한 variantKey)
    await Promise.all([
      w.renderPaymentMethods({ selector: '#payment-method', variantKey: 'DEFAULT' }),
      w.renderAgreement({ selector: '#agreement', variantKey: 'AGREEMENT' }),
    ])

    widgets.value = w
    ready.value = true
  } catch (e) {
    errorMsg.value = '결제위젯을 불러오지 못했습니다: ' + (e.message || e)
    console.error('토스 위젯 오류', e)
  }
})

async function requestPay() {
  if (!widgets.value) return
  try {
    await widgets.value.requestPayment({
      orderId: generateOrderId(),
      orderName: `포인트 ${amount.toLocaleString()}P 충전`,
      successUrl: window.location.origin + '/payment/success',
      failUrl: window.location.origin + '/payment/fail',
      customerEmail: authStore.currentUser.email,
    })
  } catch (e) {
    if (e.code !== 'USER_CANCEL') {
      errorMsg.value = e.message || '결제 요청에 실패했습니다.'
    }
  }
}
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[600px] mx-auto px-6 py-12">
      <h1 class="font-bold mb-2" style="font-size: 1.75rem; color: var(--text-primary)">포인트 충전</h1>
      <p class="mb-8" style="color: var(--text-secondary)">
        충전 금액: <span class="font-bold" style="color: var(--primary)">₩{{ amount.toLocaleString() }}</span>
        ( {{ amount.toLocaleString() }}P )
      </p>

      <div v-if="errorMsg" class="mb-4 p-4 rounded-lg text-sm" style="background-color: var(--accent); color: var(--danger)">
        {{ errorMsg }}
      </div>

      <!-- 토스 결제위젯 렌더링 영역 -->
      <div id="payment-method" class="mb-4" />
      <div id="agreement" class="mb-6" />

      <div class="flex gap-3">
        <button @click="router.push('/mypage')" class="flex-1 h-14 rounded-md border font-medium"
          style="border-color: var(--border-color); color: var(--text-primary)">
          취소
        </button>
        <button @click="requestPay" :disabled="!ready"
          class="flex-1 h-14 rounded-md text-white font-medium disabled:opacity-50"
          style="background-color: var(--primary)">
          {{ ready ? `₩${amount.toLocaleString()} 결제하기` : '불러오는 중...' }}
        </button>
      </div>
    </div>
  </div>
</template>
