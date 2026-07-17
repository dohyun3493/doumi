<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ThumbsUp, MessageCircle, Pin } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const authStore = useAuthStore()

const activeTab = ref('전체')
const posts = ref([])
const totalCount = ref(0)
const totalPage = ref(1)
const currentPage = ref(1)
const pageSize = 20

const tabs = ['전체', '공지', '기부 후기', 'FAQ', '일반']

// 백엔드에서 이미 category 필터링을 수행하므로 클라이언트 필터링 불필요
const filteredPosts = computed(() => posts.value)

async function fetchPosts() {
  try {
    const params = { page: currentPage.value, size: pageSize }
    if (activeTab.value !== '전체') params.category = activeTab.value
    const res = await api.get('/board', { params })
    posts.value = res.data.posts || []
    totalCount.value = res.data.totalElements || 0
    totalPage.value = res.data.totalPages || 1
  } catch (e) {
    console.error('게시글 로드 실패', e)
  }
}



watch(activeTab, () => {
  currentPage.value = 1
  fetchPosts()
})

onMounted(fetchPosts)
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[1200px] mx-auto px-6 py-12">
      <div class="grid lg:grid-cols-[1fr_300px] gap-8">
        <!-- Main Content -->
        <div>
          <div class="flex justify-between items-center mb-8">
            <h1 class="font-bold" style="font-size: 2rem; color: var(--text-primary)">커뮤니티</h1>
            <RouterLink v-if="authStore.isAuthenticated" to="/board/write">
              <button
                class="px-4 py-2 rounded-md text-white text-sm font-medium cursor-pointer"
                style="background-color: var(--primary)">
                글쓰기
              </button>
            </RouterLink>
          </div>

          <!-- Filter Tabs -->
          <div class="mb-6 flex gap-2 border-b" style="border-color: var(--border-color)">
            <button v-for="tab in tabs" :key="tab" @click="activeTab = tab"
              class="rounded-full px-6 py-2 text-sm transition-colors"
              :style="{ backgroundColor: activeTab === tab ? 'var(--primary)' : 'transparent', color: activeTab === tab ? 'white' : 'var(--text-secondary)' }">
              {{ tab }}
            </button>
          </div>

          <!-- Posts List -->
          <div v-if="filteredPosts.length > 0" class="space-y-3">
            <RouterLink v-for="post in filteredPosts" :key="post.boardId" :to="`/board/${post.boardId}`">
              <div class="p-5 rounded-xl transition-all hover:shadow-md"
                :style="{ backgroundColor: post.isPinned ? 'var(--accent)' : 'var(--surface)' }">
                <div class="flex items-start gap-4">
                  <Pin v-if="post.isPinned" class="w-5 h-5 flex-shrink-0 mt-1" style="color: var(--primary)" />
                  <div class="flex-1 min-w-0">
                    <div class="flex items-center gap-2 mb-2">
                      <span class="rounded-full px-2.5 py-0.5 text-xs font-medium"
                        :style="{ backgroundColor: post.category === '공지' ? 'var(--primary)' : 'var(--surface-alt)', color: post.category === '공지' ? 'white' : 'var(--text-primary)' }">
                        {{ post.category }}
                      </span>
                    </div>
                    <h3 class="font-semibold mb-2" style="color: var(--text-primary)">{{ post.title }}</h3>
                    <div class="flex items-center gap-4 text-sm">
                      <span style="color: var(--text-muted)">{{ post.authorName || '작성자 #' + post.authorId }}</span>
                      <span style="color: var(--text-muted)">{{ post.createdAt?.slice(0, 10) }}</span>
                      <span style="color: var(--text-muted)">조회 {{ post.views }}</span>
                      <span class="flex items-center gap-1" style="color: var(--text-muted)">
                        <ThumbsUp class="w-3.5 h-3.5" /> {{ post.likes }}
                      </span>
                    </div>
                  </div>
                </div>
              </div>
            </RouterLink>
          </div>
          <div v-else class="text-center py-12" style="color: var(--text-muted)">게시글이 없습니다.</div>

          <!-- 페이징 -->
          <div v-if="totalPage > 1" class="flex justify-center gap-2 mt-8">
            <button v-for="p in totalPage" :key="p" @click="currentPage = p; fetchPosts()"
              class="w-9 h-9 rounded-md text-sm font-medium"
              :style="{ backgroundColor: currentPage === p ? 'var(--primary)' : 'var(--surface)', color: currentPage === p ? 'white' : 'var(--text-primary)' }">
              {{ p }}
            </button>
          </div>
        </div>

        <!-- Sidebar -->
        <div class="space-y-6">
          <div class="rounded-xl p-5" style="background-color: var(--surface)">
            <h3 class="font-semibold mb-4" style="color: var(--text-primary)">전체 게시글</h3>
            <p class="text-sm" style="color: var(--text-muted)">총 {{ totalCount }}개의 게시글</p>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>
