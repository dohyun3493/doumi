<script setup>
import { useRoute, useRouter } from 'vue-router'
import { XCircle } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()

const code = route.query.code
const message = route.query.message || '결제가 취소되었거나 실패했습니다.'
</script>

<template>
  <div class="min-h-screen flex items-center justify-center px-6" style="background-color: var(--background)">
    <div class="w-full max-w-md rounded-2xl p-8 text-center" style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.08)">
      <XCircle class="w-16 h-16 mx-auto mb-6" style="color: var(--danger)" />
      <h2 class="font-bold mb-2" style="font-size: 1.5rem; color: var(--text-primary)">결제 실패</h2>
      <p class="mb-2 text-sm" style="color: var(--text-secondary)">{{ message }}</p>
      <p v-if="code" class="mb-6 text-xs" style="color: var(--text-muted)">오류 코드: {{ code }}</p>
      <button @click="router.push('/mypage')" class="w-full h-12 rounded-md text-white font-medium" style="background-color: var(--primary)">
        마이페이지로 돌아가기
      </button>
    </div>
  </div>
</template>
