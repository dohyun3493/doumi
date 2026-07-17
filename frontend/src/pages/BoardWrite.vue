<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, MessageSquarePlus } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const router = useRouter()
const authStore = useAuthStore()
const isAdmin = computed(() => authStore.currentUser?.memberType === 'ADMIN')

const writeForm = ref({ title: '', content: '', category: '일반' })
const isSubmitting = ref(false)

async function submitPost() {
  if (!writeForm.value.title.trim()) {
    alert('제목을 입력해주세요.')
    return
  }
  if (!writeForm.value.content.trim()) {
    alert('내용을 입력해주세요.')
    return
  }

  isSubmitting.value = true
  try {
    await api.post('/board', {
      title: writeForm.value.title,
      content: writeForm.value.content,
      category: writeForm.value.category,
      isPinned: writeForm.value.category === '공지', // 공지는 상단 고정
    })
    alert('게시글이 성공적으로 등록되었습니다.')
    router.push('/board')
  } catch (e) {
    console.error('글쓰기 실패', e)
    alert('글쓰기에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

function cancel() {
  router.back()
}
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh" class="py-12">
    <div class="max-w-[800px] mx-auto px-6">
      <!-- Back Button -->
      <button @click="cancel" class="mb-6 flex items-center gap-2 text-sm cursor-pointer" style="color: var(--text-secondary)">
        <ArrowLeft class="w-4 h-4" /> 뒤로가기
      </button>

      <!-- Form Card -->
      <div class="rounded-2xl p-8 shadow-sm" style="background-color: var(--surface)">
        <!-- Header -->
        <div class="flex items-center gap-3 mb-8 border-b pb-4" style="border-color: var(--border-color)">
          <div class="w-10 h-10 rounded-xl flex items-center justify-center" style="background-color: var(--accent)">
            <MessageSquarePlus class="w-5 h-5" style="color: var(--primary)" />
          </div>
          <div>
            <h2 class="font-bold text-xl" style="color: var(--text-primary)">게시글 작성</h2>
            <p class="text-xs" style="color: var(--text-muted)">커뮤니티에 따뜻한 이야기나 질문을 남겨보세요</p>
          </div>
        </div>

        <form @submit.prevent="submitPost" class="space-y-6">
          <!-- Category Selection -->
          <div>
            <label class="text-sm font-semibold mb-2 block" style="color: var(--text-primary)">카테고리</label>
            <select v-model="writeForm.category" class="w-full h-12 px-4 rounded-lg border outline-none text-sm cursor-pointer"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)">
              <option v-if="isAdmin" value="공지">공지</option>
              <option value="일반">일반</option>
              <option value="기부 후기">기부 후기</option>
              <option value="FAQ">FAQ</option>
            </select>
          </div>

          <!-- Title Input -->
          <div>
            <label class="text-sm font-semibold mb-2 block" style="color: var(--text-primary)">제목</label>
            <input v-model="writeForm.title" type="text" placeholder="제목을 입력하세요"
              class="w-full h-12 px-4 rounded-lg border outline-none"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required />
          </div>

          <!-- Content Textarea -->
          <div>
            <label class="text-sm font-semibold mb-2 block" style="color: var(--text-primary)">내용</label>
            <textarea v-model="writeForm.content" placeholder="내용을 입력하세요..."
              class="w-full min-h-[350px] p-4 rounded-lg border outline-none resize-none leading-relaxed"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
              required />
          </div>

          <!-- Form Buttons -->
          <div class="flex gap-4 pt-4 border-t" style="border-color: var(--border-color)">
            <button type="button" @click="cancel" class="flex-1 h-12 rounded-lg border font-medium cursor-pointer"
              style="border-color: var(--border-color); color: var(--text-secondary)">
              취소
            </button>
            <button type="submit" :disabled="isSubmitting" class="flex-1 h-12 rounded-lg text-white font-medium disabled:opacity-50 cursor-pointer"
              style="background-color: var(--primary)">
              {{ isSubmitting ? '등록 중...' : '등록하기' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>
