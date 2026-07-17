<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ThumbsUp, MessageCircle, ArrowLeft, Trash2, Pencil } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const route = useRoute()
const router = useRouter()
const id = route.params.id
const authStore = useAuthStore()

const post = ref(null)
const comments = ref([])
const comment = ref('')
const liked = ref(false)
const isSubmitting = ref(false)

// 권한: 게시글/댓글은 작성자 본인 또는 관리자가 삭제 가능 (수정은 작성자만)
const isAdmin = computed(() => authStore.currentUser?.memberType === 'ADMIN')
const isPostOwner = computed(() => !!post.value && authStore.currentUser?.memberId === post.value.authorId)
const isCommentOwner = (c) => authStore.currentUser?.memberId === c.authorId

async function toggleLike() {
  if (!authStore.isAuthenticated) {
    alert('로그인이 필요한 서비스입니다.')
    router.push('/login')
    return
  }
  try {
    const res = await api.post(`/board/${id}/like`)
    liked.value = res.data.liked
    if (post.value) {
      post.value.likes = res.data.likesCount
    }
  } catch (e) {
    console.error('좋아요 연동 실패', e)
    alert('좋아요 처리에 실패했습니다.')
  }
}

// 게시글 수정 모달 상태
const showEditModal = ref(false)
const editForm = ref({ title: '', content: '', category: '일반' })
const isEditingPost = ref(false)

// 댓글 수정
const editingCommentId = ref(null)
const editingContent = ref('')

async function fetchPost() {
  try {
    const res = await api.get(`/board/${id}`)
    post.value = res.data
    liked.value = post.value.isLiked
  } catch (e) {
    console.error('게시글 로드 실패', e)
  }
}

async function fetchComments() {
  try {
    const res = await api.get(`/board/${id}/comments`)
    comments.value = res.data
  } catch (e) {
    console.error('댓글 로드 실패', e)
  }
}

async function submitComment() {
  if (!comment.value.trim()) return
  isSubmitting.value = true
  try {
    await api.post(`/board/${id}/comments`, { content: comment.value })
    comment.value = ''
    await fetchComments()
  } catch (e) {
    alert('댓글 등록에 실패했습니다.')
  } finally {
    isSubmitting.value = false
  }
}

async function deleteComment(commentId) {
  if (!confirm('댓글을 삭제할까요?')) return
  try {
    await api.delete(`/board/comments/${commentId}`)
    await fetchComments()
  } catch (e) {
    alert('삭제에 실패했습니다.')
  }
}

async function updateComment(commentId) {
  try {
    await api.put(`/board/comments/${commentId}`, { content: editingContent.value })
    editingCommentId.value = null
    await fetchComments()
  } catch (e) {
    alert('수정에 실패했습니다.')
  }
}

function startEdit(c) {
  editingCommentId.value = c.commentId
  editingContent.value = c.content
}

async function deletePost() {
  if (!confirm('정말 이 게시글을 삭제하시겠습니까?')) return
  try {
    await api.delete(`/board/${id}`)
    alert('게시글이 삭제되었습니다.')
    router.push('/board')
  } catch (e) {
    alert('게시글 삭제에 실패했습니다.')
  }
}

function startEditPost() {
  if (!post.value) return
  editForm.value = {
    title: post.value.title,
    content: post.value.content,
    category: post.value.category
  }
  showEditModal.value = true
}

async function updatePost() {
  if (!editForm.value.title.trim()) return
  isEditingPost.value = true
  try {
    await api.put(`/board/${id}`, {
      title: editForm.value.title,
      content: editForm.value.content,
      category: editForm.value.category
    })
    showEditModal.value = false
    await fetchPost()
  } catch (e) {
    alert('게시글 수정에 실패했습니다.')
  } finally {
    isEditingPost.value = false
  }
}

onMounted(async () => {
  await fetchPost()
  await fetchComments()
})
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[800px] mx-auto px-6 py-12">
      <RouterLink to="/board">
        <button class="mb-6 flex items-center gap-2 text-sm" style="color: var(--text-secondary)">
          <ArrowLeft class="w-4 h-4" /> 목록으로
        </button>
      </RouterLink>

      <!-- 게시글 -->
      <div v-if="post" class="rounded-2xl p-8 mb-6" style="background-color: var(--surface)">
        <div class="mb-6">
          <div class="flex items-center gap-2 mb-3">
            <span class="rounded-full px-3 py-1 text-sm" style="background-color: var(--accent); color: var(--primary)">
              {{ post.category }}
            </span>
            <span class="text-sm" style="color: var(--text-muted)">{{ post.createdAt?.slice(0, 10) }}</span>
            <span class="text-sm" style="color: var(--text-muted)">조회 {{ post.views }}</span>
          </div>
          <h1 class="font-bold mb-4" style="font-size: 1.75rem; color: var(--text-primary)">{{ post.title }}</h1>
          <div class="flex justify-between items-center">
            <div class="flex items-center gap-3">
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-semibold" style="background-color: var(--accent); color: var(--primary)">
                {{ (post.authorName || '?')[0] }}
              </div>
              <div class="font-semibold text-sm" style="color: var(--text-primary)">{{ post.authorName || '작성자 #' + post.authorId }}</div>
            </div>
            <!-- 작성자: 수정/삭제 · 관리자: 삭제 -->
            <div v-if="isPostOwner || isAdmin" class="flex gap-2">
              <button v-if="isPostOwner" @click="startEditPost" class="flex items-center gap-1 px-3 py-1.5 rounded-md border text-xs font-medium transition-colors hover:bg-slate-50" style="border-color: var(--border-color); color: var(--text-secondary)">
                <Pencil class="w-3 h-3" /> 수정
              </button>
              <button @click="deletePost" class="flex items-center gap-1 px-3 py-1.5 rounded-md border text-xs font-medium transition-colors hover:bg-red-50" style="border-color: var(--border-color); color: var(--danger)">
                <Trash2 class="w-3 h-3" /> 삭제
              </button>
            </div>
          </div>
        </div>

        <div class="mb-6 whitespace-pre-line" style="color: var(--text-primary); line-height: 1.8">{{ post.content }}</div>

        <div class="flex items-center gap-4 pt-6" style="border-top: 1px solid var(--border-color)">
          <button @click="toggleLike"
            class="flex items-center gap-2 px-4 py-2 rounded-md border text-sm font-medium transition-colors"
            :style="{ backgroundColor: liked ? 'var(--primary)' : 'transparent', color: liked ? 'white' : 'var(--text-secondary)', borderColor: 'var(--border-color)' }">
            <ThumbsUp class="w-4 h-4" />
            좋아요 {{ post.likes || 0 }}
          </button>
        </div>
      </div>

      <div v-else class="text-center py-12" style="color: var(--text-muted)">게시글을 불러오는 중...</div>

      <!-- 댓글 섹션 -->
      <div class="rounded-2xl p-8" style="background-color: var(--surface)">
        <h3 class="font-semibold mb-6 flex items-center gap-2" style="color: var(--text-primary)">
          <MessageCircle class="w-5 h-5" /> 댓글 {{ comments.length }}
        </h3>

        <!-- 댓글 입력 -->
        <div v-if="authStore.isAuthenticated" class="mb-8">
          <textarea v-model="comment" placeholder="댓글을 입력하세요..."
            class="w-full min-h-24 p-4 rounded-md border outline-none resize-none mb-3"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
          <div class="flex justify-end">
            <button @click="submitComment" :disabled="!comment.trim() || isSubmitting"
              class="px-4 py-2 rounded-md text-white text-sm font-medium disabled:opacity-50"
              style="background-color: var(--primary)">
              {{ isSubmitting ? '등록 중...' : '댓글 달기' }}
            </button>
          </div>
        </div>
        <p v-else class="mb-6 text-sm" style="color: var(--text-muted)">
          댓글을 작성하려면 <RouterLink to="/login" style="color: var(--primary)">로그인</RouterLink>이 필요합니다.
        </p>

        <!-- 댓글 목록 -->
        <div class="space-y-6">
          <div v-for="c in comments" :key="c.commentId" class="flex gap-4">
            <!-- 삭제된 댓글: 삭제 주체 안내만 표시 -->
            <p v-if="c.deletedBy" class="flex-1 py-2 text-sm italic" style="color: var(--text-muted)">
              {{ c.deletedBy === 'ADMIN' ? '관리자에 의해 삭제된 댓글입니다.' : '작성자에 의해 삭제된 댓글입니다.' }}
            </p>
            <!-- 정상 댓글 -->
            <template v-else>
              <div class="w-10 h-10 rounded-full flex items-center justify-center font-semibold flex-shrink-0"
                style="background-color: var(--surface-alt); color: var(--text-primary)">
                {{ (c.authorName || '?')[0] }}
              </div>
              <div class="flex-1">
                <div class="flex items-center justify-between mb-2">
                  <div class="flex items-center gap-2">
                    <span class="font-semibold text-sm" style="color: var(--text-primary)">{{ c.authorName || '작성자 #' + c.authorId }}</span>
                    <span class="text-xs" style="color: var(--text-muted)">{{ c.createdAt?.slice(0, 10) }}</span>
                  </div>
                  <!-- 작성자: 수정/삭제 · 관리자: 삭제 -->
                  <div v-if="isCommentOwner(c) || isAdmin" class="flex gap-2">
                    <button v-if="isCommentOwner(c)" @click="startEdit(c)" style="color: var(--text-muted)"><Pencil class="w-3.5 h-3.5" /></button>
                    <button @click="deleteComment(c.commentId)" style="color: var(--danger)"><Trash2 class="w-3.5 h-3.5" /></button>
                  </div>
                </div>

                <!-- 수정 모드 -->
                <div v-if="editingCommentId === c.commentId">
                  <textarea v-model="editingContent" class="w-full p-3 rounded-md border outline-none resize-none mb-2 text-sm"
                    style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
                  <div class="flex gap-2">
                    <button @click="editingCommentId = null" class="text-xs px-3 py-1 rounded-md border"
                      style="border-color: var(--border-color); color: var(--text-secondary)">취소</button>
                    <button @click="updateComment(c.commentId)" class="text-xs px-3 py-1 rounded-md text-white"
                      style="background-color: var(--primary)">저장</button>
                  </div>
                </div>
                <p v-else class="mb-2" style="color: var(--text-secondary)">{{ c.content }}</p>
              </div>
            </template>
          </div>
          <p v-if="comments.length === 0" class="text-sm" style="color: var(--text-muted)">첫 댓글을 남겨보세요!</p>
        </div>
      </div>
    </div>

    <!-- 게시글 수정 모달 -->
    <div v-if="showEditModal" class="fixed inset-0 flex items-center justify-center z-50" style="background-color: rgba(0,0,0,0.5)">
      <div class="w-full max-w-lg rounded-2xl p-6" style="background-color: var(--surface)">
        <h3 class="font-bold mb-4" style="font-size: 1.25rem; color: var(--text-primary)">게시글 수정</h3>
        <select v-model="editForm.category" class="w-full h-10 px-3 rounded-md border outline-none text-sm mb-3"
          style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)">
          <option v-if="isAdmin" value="공지">공지</option>
          <option value="일반">일반</option>
          <option value="기부 후기">기부 후기</option>
          <option value="FAQ">FAQ</option>
        </select>
        <input v-model="editForm.title" type="text" placeholder="제목을 입력하세요"
          class="w-full h-12 px-4 rounded-md border outline-none mb-3"
          style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
        <textarea v-model="editForm.content" placeholder="내용을 입력하세요"
          class="w-full min-h-32 p-4 rounded-md border outline-none resize-none mb-4"
          style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
        <div class="flex gap-3">
          <button @click="showEditModal = false" class="flex-1 h-12 rounded-md border font-medium"
            style="border-color: var(--border-color); color: var(--text-primary)">취소</button>
          <button @click="updatePost" :disabled="isEditingPost" class="flex-1 h-12 rounded-md text-white font-medium disabled:opacity-50"
            style="background-color: var(--primary)">{{ isEditingPost ? '수정 중...' : '수정 완료' }}</button>
        </div>
      </div>
    </div>
  </div>
</template>
