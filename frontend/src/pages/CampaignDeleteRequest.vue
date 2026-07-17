<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/lib/api'

const route = useRoute()
const router = useRouter()
const id = route.params.id

const campaignTitle = ref('')
const reason = ref('')
const isLoading = ref(false)
const errorMsg = ref('')

// 어떤 캠페인을 삭제 요청하는지 제목을 보여주기 위해 불러온다
onMounted(async () => {
  try {
    const { data } = await api.get(`/api/campaigns/${id}`)
    campaignTitle.value = data.title ?? ''
    // 이미 삭제 요청된 캠페인이면 상세로 되돌린다
    if (data.deleteRequested) {
      alert('이미 삭제 요청된 캠페인입니다.')
      router.replace(`/campaigns/${id}`)
    }
  } catch (e) {
    errorMsg.value = '캠페인 정보를 불러오지 못했습니다.'
  }
})

async function submit() {
  if (!reason.value.trim()) {
    errorMsg.value = '삭제 요청 사유를 입력해주세요.'
    return
  }
  isLoading.value = true
  errorMsg.value = ''
  try {
    await api.post(`/api/campaigns/${id}/delete-request`, { reason: reason.value.trim() })
    alert('삭제 요청이 접수되었습니다. 관리자 승인 후 삭제됩니다.')
    router.push(`/campaigns/${id}`)
  } catch (e) {
    errorMsg.value = e.response?.data?.message || '삭제 요청에 실패했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[640px] mx-auto px-6 py-12">
      <div class="mb-8">
        <h1 class="font-bold mb-2" style="font-size: 2rem; color: var(--text-primary)">캠페인 삭제 요청</h1>
        <p style="color: var(--text-secondary)">
          삭제 요청은 관리자에게 전달되며, 승인 후에 삭제됩니다.
        </p>
      </div>

      <form @submit.prevent="submit" class="space-y-6">
        <div class="rounded-2xl p-6" style="background-color: var(--surface)">
          <!-- 대상 캠페인 안내 -->
          <div class="p-4 rounded-xl mb-6" style="background-color: var(--accent)">
            <p class="text-sm font-medium mb-1" style="color: var(--text-primary)">
              삭제 요청할 캠페인
            </p>
            <p class="font-semibold" style="color: var(--primary)">
              {{ campaignTitle || '캠페인' }}
            </p>
          </div>

          <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">
            삭제 요청 사유 <span style="color: var(--danger)">*</span>
          </label>
          <textarea v-model="reason" rows="5" maxlength="300"
            placeholder="관리자에게 전달할 삭제 요청 사유를 입력해주세요."
            class="w-full px-4 py-3 rounded-md border outline-none resize-none"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
          <p class="text-xs text-right mt-1" style="color: var(--text-muted)">{{ reason.length }}/300</p>
        </div>

        <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <div class="flex gap-3">
          <button type="button" @click="router.push(`/campaigns/${id}`)"
            class="flex-1 h-12 rounded-md border font-medium"
            style="border-color: var(--border-color); color: var(--text-primary)">
            취소
          </button>
          <button type="submit" :disabled="isLoading"
            class="flex-1 h-12 rounded-md text-white font-medium disabled:opacity-50"
            style="background-color: var(--danger)">
            {{ isLoading ? '요청 중...' : '삭제 요청' }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
