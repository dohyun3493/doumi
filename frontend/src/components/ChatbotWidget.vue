<script setup>
import { ref, nextTick, watch } from 'vue'
import { MessageCircle, X, Send } from 'lucide-vue-next'
import api from '@/lib/api'

const isOpen = ref(false)
const input = ref('')
const loading = ref(false)
const messagesEl = ref(null)

// 새로고침해도 대화가 유지되도록 sessionStorage에 보관 (탭을 닫으면 초기화)
const STORAGE_KEY = 'chatbot:v1'
const saved = JSON.parse(sessionStorage.getItem(STORAGE_KEY) || 'null')

const DEFAULT_GREETING = {
  role: 'bot',
  text: '안녕하세요! 기부 도우미예요. 🤝\n어떤 분야에 기부하고 싶으신가요? 관심 있는 대상(아동, 노인, 동물, 환경 등)이나 지역을 말씀해 주시면 딱 맞는 캠페인을 찾아드릴게요.',
  campaigns: [],
}

// 세션 단위로 대화 맥락 유지 (서버 ChatMemory의 conversationId로 사용)
// 새로고침 시에도 같은 sessionId를 복원해야 서버 측 대화 맥락이 이어진다.
// crypto.randomUUID()는 HTTPS/localhost(보안 컨텍스트)에서만 동작 → HTTP 배포 대비 폴백
const sessionId = saved?.sessionId ?? (
  (typeof crypto !== 'undefined' && crypto.randomUUID)
    ? crypto.randomUUID()
    : `sess-${Date.now()}-${Math.random().toString(36).slice(2)}`
)

const messages = ref(saved?.messages ?? [DEFAULT_GREETING])

// 대화가 바뀔 때마다 저장 (메시지 내용·추천 캠페인까지 deep 감시)
watch(messages, () => {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify({ sessionId, messages: messages.value }))
}, { deep: true })

const scrollToBottom = async () => {
  await nextTick()
  if (messagesEl.value) {
    messagesEl.value.scrollTop = messagesEl.value.scrollHeight
  }
}

const send = async () => {
  const text = input.value.trim()
  if (!text || loading.value) return

  messages.value.push({ role: 'user', text, campaigns: [] })
  input.value = ''
  loading.value = true
  scrollToBottom()

  try {
    const { data } = await api.post('/chatbot/chat', { sessionId, message: text })
    messages.value.push({
      role: 'bot',
      text: data.answer,
      campaigns: data.campaigns ?? [],
    })
  } catch (e) {
    messages.value.push({
      role: 'bot',
      text: '죄송해요, 답변을 가져오지 못했어요. 잠시 후 다시 시도해 주세요.',
      campaigns: [],
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const progressOf = (c) =>
  c.goalAmount ? Math.min(100, Math.round((c.currentAmount / c.goalAmount) * 100)) : 0
</script>

<template>
  <!-- 플로팅 버튼 -->
  <button
    v-if="!isOpen"
    class="fixed bottom-6 right-6 z-50 w-14 h-14 rounded-full flex items-center justify-center shadow-lg transition-transform hover:scale-110"
    style="background-color: var(--primary); color: white"
    aria-label="기부 도우미 열기"
    @click="isOpen = true; scrollToBottom()"
  >
    <MessageCircle class="w-7 h-7" />
  </button>

  <!-- 챗봇 패널 -->
  <div
    v-if="isOpen"
    class="fixed bottom-6 right-6 z-50 flex flex-col rounded-2xl overflow-hidden"
    style="width: 380px; max-width: calc(100vw - 2rem); height: 560px; max-height: calc(100vh - 6rem); background-color: var(--surface); box-shadow: 0 8px 32px rgba(0,0,0,0.18)"
  >
    <!-- 헤더 -->
    <div class="flex items-center justify-between px-4 py-3" style="background-color: var(--primary)">
      <div class="flex items-center gap-2 text-white">
        <MessageCircle class="w-5 h-5" />
        <span class="font-semibold">기부 도우미</span>
      </div>
      <button class="text-white/90 hover:text-white" aria-label="닫기" @click="isOpen = false">
        <X class="w-5 h-5" />
      </button>
    </div>

    <!-- 메시지 영역 -->
    <div ref="messagesEl" class="flex-1 overflow-y-auto p-4 flex flex-col gap-3">
      <div v-for="(msg, i) in messages" :key="i" class="flex flex-col gap-2">
        <!-- 말풍선 -->
        <div
          class="max-w-[85%] rounded-2xl px-4 py-2.5 text-sm whitespace-pre-line"
          :class="msg.role === 'user' ? 'self-end' : 'self-start'"
          :style="msg.role === 'user'
            ? { backgroundColor: 'var(--primary)', color: 'white' }
            : { backgroundColor: 'var(--background)', color: 'var(--text-primary)' }"
        >
          {{ msg.text }}
        </div>

        <!-- 추천 캠페인 카드 -->
        <RouterLink
          v-for="c in msg.campaigns"
          :key="c.campaignId"
          :to="`/campaigns/${c.campaignId}`"
          class="self-start w-[85%] rounded-xl p-3 transition-colors hover:opacity-90"
          style="background-color: var(--background); border: 1px solid var(--border-color)"
          @click="isOpen = false"
        >
          <div class="flex items-center justify-between mb-1">
            <span class="text-xs rounded-full px-2 py-0.5" style="background-color: var(--primary); color: white">
              {{ c.category }}
            </span>
            <span class="text-xs" style="color: var(--text-muted)">{{ c.region }}</span>
          </div>
          <p class="text-sm font-semibold line-clamp-2 mb-1" style="color: var(--text-primary)">{{ c.title }}</p>
          <p class="text-xs mb-2" style="color: var(--text-muted)">{{ c.orgName }}</p>
          <div class="h-1 rounded-full mb-1" style="background-color: var(--border-color)">
            <div
              class="h-full rounded-full"
              :style="{ backgroundColor: 'var(--primary)', width: `${progressOf(c)}%` }"
            />
          </div>
          <div class="flex justify-between text-xs">
            <span class="font-bold" style="color: var(--primary)">{{ progressOf(c) }}% 달성</span>
            <span style="color: var(--text-muted)">목표 ₩{{ c.goalAmount?.toLocaleString() }}</span>
          </div>
        </RouterLink>
      </div>

      <!-- 로딩 인디케이터 -->
      <div
        v-if="loading"
        class="self-start rounded-2xl px-4 py-3 flex gap-1"
        style="background-color: var(--background)"
      >
        <span class="chatbot-dot" />
        <span class="chatbot-dot" style="animation-delay: 0.15s" />
        <span class="chatbot-dot" style="animation-delay: 0.3s" />
      </div>
    </div>

    <!-- 입력 영역 -->
    <div class="flex items-center gap-2 p-3" style="border-top: 1px solid var(--border-color)">
      <input
        v-model="input"
        type="text"
        placeholder="예) 광주에서 유기견 돕는 캠페인 있어?"
        class="flex-1 h-10 rounded-lg px-3 text-sm outline-none"
        style="background-color: var(--background); color: var(--text-primary)"
        :disabled="loading"
        @keyup.enter="send"
      />
      <button
        class="w-10 h-10 rounded-lg flex items-center justify-center transition-opacity disabled:opacity-40"
        style="background-color: var(--primary); color: white"
        :disabled="loading || !input.trim()"
        aria-label="보내기"
        @click="send"
      >
        <Send class="w-4 h-4" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.chatbot-dot {
  width: 6px;
  height: 6px;
  border-radius: 9999px;
  background-color: var(--text-muted);
  animation: chatbot-bounce 1s infinite;
}
@keyframes chatbot-bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
  30% { transform: translateY(-4px); opacity: 1; }
}
</style>
