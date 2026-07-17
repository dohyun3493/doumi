<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Heart } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const id = route.params.id

const step = ref(1)
const amountInput = ref('')   // 기부 금액 (입력/버튼 합산)
const isLoading = ref(false)
const errorMsg = ref('')
const campaign = ref(null)

const presetAmounts = [1000, 3000, 5000, 10000, 30000, 50000]

const finalAmount = computed(() => parseInt(amountInput.value) || 0)

// 보유 포인트 & 기부 후 잔여 포인트
const currentPoint = computed(() => authStore.currentUser?.pointBalance ?? 0)
const remainingPoint = computed(() => currentPoint.value - finalAmount.value)

// 버튼 클릭 시 해당 금액만큼 합산
function addAmount(amount) {
  const current = parseInt(amountInput.value) || 0
  amountInput.value = String(current + amount)
}

// 금액 초기화
function resetAmount() {
  amountInput.value = ''
}

async function donate() {
  if (!authStore.isAuthenticated) {
    alert('로그인이 필요합니다.')
    router.push('/login')
    return
  }
  if (!finalAmount.value) return

  isLoading.value = true
  errorMsg.value = ''
  try {
    await api.post('/points/use', {
      campaignId: Number(id),
      amount: finalAmount.value,
    })
    authStore.usePoint(finalAmount.value)
    step.value = 3
  } catch (e) {
    errorMsg.value = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : '기부에 실패했습니다.')
  } finally {
    isLoading.value = false
  }
}

onMounted(async () => {
  // 쿼리 스트링으로 전달받은 기부 금액 바인딩 (있을 경우)
  const queryAmount = Number(route.query.amount)
  if (queryAmount > 0) {
    amountInput.value = queryAmount.toString()
  }

  try {
    const res = await api.get(`/api/campaigns/${id}`)
    campaign.value = res.data
  } catch (e) {
    console.error('캠페인 로드 실패', e)
  }
})
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[560px] mx-auto px-6 py-12">
      <!-- Step Indicator -->
      <div class="mb-12">
        <div class="flex items-center justify-between mb-4">
          <div v-for="s in [1, 2, 3]" :key="s" class="flex items-center flex-1">
            <div class="w-10 h-10 rounded-full flex items-center justify-center font-semibold"
              :style="{ backgroundColor: step >= s ? 'var(--primary)' : 'var(--border-color)', color: step >= s ? 'white' : 'var(--text-muted)' }">
              {{ s }}
            </div>
            <div v-if="s < 3" class="flex-1 h-1 mx-2" :style="{ backgroundColor: step > s ? 'var(--primary)' : 'var(--border-color)' }" />
          </div>
        </div>
        <div class="flex justify-between text-sm">
          <span :style="{ color: step >= 1 ? 'var(--text-primary)' : 'var(--text-muted)' }">금액 선택</span>
          <span :style="{ color: step >= 2 ? 'var(--text-primary)' : 'var(--text-muted)' }">확인</span>
          <span :style="{ color: step >= 3 ? 'var(--text-primary)' : 'var(--text-muted)' }">완료</span>
        </div>
      </div>

      <!-- Step 1: 금액 선택 -->
      <div v-if="step === 1">
        <div v-if="campaign" class="flex gap-4 p-4 rounded-xl mb-8" style="background-color: var(--surface)">
          <div class="flex-1">
            <h3 class="font-semibold mb-2 text-sm" style="color: var(--text-primary)">{{ campaign.title }}</h3>
            <div class="flex items-center gap-2">
              <div class="h-1 flex-1 rounded-full" style="background-color: var(--border-color)">
                <div class="h-full rounded-full" :style="{ backgroundColor: 'var(--primary)', width: `${campaign.goalAmount > 0 ? Math.min(Math.round(campaign.currentAmount / campaign.goalAmount * 100), 100) : 0}%` }" />
              </div>
              <span class="text-xs font-semibold" style="color: var(--primary)">
                {{ campaign.goalAmount > 0 ? Math.min(Math.round(campaign.currentAmount / campaign.goalAmount * 100), 100) : 0 }}%
              </span>
            </div>
          </div>
        </div>

        <!-- 포인트 잔액 표시 -->
        <div v-if="authStore.isAuthenticated" class="mb-4 p-3 rounded-lg text-sm" style="background-color: var(--accent)">
          <span style="color: var(--text-secondary)">보유 포인트: </span>
          <span class="font-bold" style="color: var(--primary)">{{ authStore.currentUser?.pointBalance?.toLocaleString() ?? 0 }}P</span>
        </div>

        <div class="mb-6">
          <div class="flex items-center justify-between mb-3">
            <label class="text-lg font-semibold" style="color: var(--text-primary)">기부 금액 선택</label>
            <button @click="resetAmount" class="text-sm" style="color: var(--text-muted)">초기화</button>
          </div>
          <div class="grid grid-cols-3 gap-3 mb-4">
            <button v-for="amount in presetAmounts" :key="amount" @click="addAmount(amount)"
              class="h-14 rounded-md border text-sm transition-colors hover:border-primary"
              style="background-color: transparent; color: var(--text-primary); border-color: var(--border-color)">
              +₩{{ amount.toLocaleString() }}
            </button>
          </div>
          <div class="relative">
            <input v-model="amountInput" type="number" placeholder="직접 입력 또는 버튼으로 합산"
              class="w-full h-14 px-4 pr-12 rounded-md border outline-none text-right font-semibold"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            <span class="absolute right-4 top-1/2 -translate-y-1/2" style="color: var(--text-muted)">원</span>
          </div>
        </div>

        <button class="w-full h-14 rounded-md text-white font-medium disabled:opacity-50"
          style="background-color: var(--primary)" :disabled="!finalAmount" @click="step = 2">
          다음 단계 →
        </button>
      </div>

      <!-- Step 2: 확인 -->
      <div v-else-if="step === 2">
        <h2 class="font-bold mb-6" style="font-size: 1.5rem; color: var(--text-primary)">기부 확인</h2>

        <div class="p-6 rounded-xl mb-8" style="background-color: var(--surface-alt)">
          <h3 class="font-semibold mb-4" style="color: var(--text-primary)">결제 금액 요약</h3>
          <div class="space-y-2 mb-4">
            <div class="flex justify-between">
              <span style="color: var(--text-secondary)">캠페인</span>
              <span class="text-sm font-medium" style="color: var(--text-primary)">{{ campaign?.title }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-secondary)">기부 금액</span>
              <span style="color: var(--text-primary)">₩{{ finalAmount.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-secondary)">보유 포인트</span>
              <span style="color: var(--text-primary)">{{ currentPoint.toLocaleString() }}P</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-secondary)">기부 후 잔여 포인트</span>
              <span class="font-semibold" :style="{ color: remainingPoint < 0 ? 'var(--danger)' : 'var(--primary)' }">
                {{ remainingPoint.toLocaleString() }}P
              </span>
            </div>
          </div>
          <div class="flex justify-between pt-4" style="border-top: 1px solid var(--border-color)">
            <span class="font-semibold" style="color: var(--text-primary)">최종 결제액</span>
            <span class="font-bold" style="color: var(--primary); font-size: 1.25rem">₩{{ finalAmount.toLocaleString() }}</span>
          </div>

          <!-- 포인트 부족 경고 -->
          <p v-if="remainingPoint < 0" class="mt-3 text-sm text-center" style="color: var(--danger)">
            보유 포인트가 부족합니다. 마이페이지에서 충전해주세요.
          </p>
        </div>

        <div v-if="errorMsg" class="mb-4 text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <div class="flex gap-3">
          <button class="flex-1 h-14 rounded-md border font-medium" style="border-color: var(--border-color); color: var(--text-primary)" @click="step = 1">이전</button>
          <button class="flex-1 h-14 rounded-md text-white font-medium disabled:opacity-50"
            style="background-color: var(--primary)" :disabled="isLoading || remainingPoint < 0" @click="donate">
            {{ isLoading ? '처리 중...' : '기부하기 ❤️' }}
          </button>
        </div>
      </div>

      <!-- Step 3: 완료 -->
      <div v-else class="text-center py-12">
        <div class="w-20 h-20 rounded-full mx-auto mb-6 flex items-center justify-center" style="background-color: var(--accent)">
          <Heart class="w-10 h-10" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
        </div>
        <h2 class="font-bold mb-4" style="font-size: 2rem; color: var(--text-primary)">기부가 완료되었어요! 🎉</h2>
        <p class="mb-8" style="color: var(--text-secondary)">따뜻한 마음에 감사드립니다</p>

        <div class="p-6 rounded-xl mb-8 text-left" style="background-color: var(--surface)">
          <h3 class="font-semibold mb-4" style="color: var(--text-primary)">기부 영수증</h3>
          <div class="space-y-3 text-sm">
            <div class="flex justify-between">
              <span style="color: var(--text-muted)">캠페인</span>
              <span style="color: var(--text-primary)">{{ campaign?.title }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-muted)">기부 금액</span>
              <span class="font-semibold" style="color: var(--primary)">₩{{ finalAmount.toLocaleString() }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-muted)">일시</span>
              <span style="color: var(--text-primary)">{{ new Date().toLocaleString('ko-KR') }}</span>
            </div>
            <div class="flex justify-between">
              <span style="color: var(--text-muted)">남은 포인트</span>
              <span style="color: var(--text-primary)">{{ authStore.currentUser?.pointBalance?.toLocaleString() ?? 0 }}P</span>
            </div>
          </div>
        </div>

        <button @click="router.push('/')" class="text-sm font-medium" style="color: var(--primary)">
          메인으로 돌아가기
        </button>
      </div>
    </div>
  </div>
</template>
