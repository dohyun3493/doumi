<script setup>
import { ref, onMounted } from 'vue'
import api from '@/lib/api'

const memberTypeFilter = ref('all')
const memberKeyword = ref('')
const members = ref([])
const memberLoading = ref(false)
const memberPage = ref(1)
const memberTotalPages = ref(1)
const MEMBER_PAGE_SIZE = 20

const memberTypeLabel = {
  INDIVIDUAL: '개인',
  ORGANIZATION: '단체',
  ADMIN: '관리자',
}

async function fetchMembers() {
  memberLoading.value = true
  try {
    const params = { page: memberPage.value, size: MEMBER_PAGE_SIZE }
    if (memberTypeFilter.value !== 'all') params.type = memberTypeFilter.value
    if (memberKeyword.value.trim()) params.keyword = memberKeyword.value.trim()
    const res = await api.get('/admin/members', { params })
    members.value = res.data.members
    memberTotalPages.value = res.data.totalPages || 1
  } catch (e) {
    console.error('회원 목록 조회 실패', e)
  } finally {
    memberLoading.value = false
  }
}

// 필터/검색은 항상 1페이지부터 다시 조회
function reloadMembers() {
  memberPage.value = 1
  fetchMembers()
}

function goMemberPage(p) {
  if (p < 1 || p > memberTotalPages.value) return
  memberPage.value = p
  fetchMembers()
}

async function forceWithdraw(m) {
  if (!confirm(`'${m.name}'(${m.email}) 회원을 강제 탈퇴시키겠습니까?\n탈퇴 후에는 되돌릴 수 없습니다.`)) return
  try {
    await api.delete(`/admin/members/${m.memberId}`)
    fetchMembers()
  } catch (e) {
    alert(e.response?.data?.message || '강제 탈퇴에 실패했습니다.')
  }
}

// 알림 작성 모달
const notifyTarget = ref(null)
const notifyContent = ref('')
const notifySending = ref(false)

function sendNotify(m) {
  notifyTarget.value = m
  notifyContent.value = ''
}

function closeNotify() {
  notifyTarget.value = null
}

async function submitNotify() {
  if (!notifyContent.value.trim()) {
    alert('알림 내용을 입력해주세요.')
    return
  }
  notifySending.value = true
  try {
    await api.post(`/admin/members/${notifyTarget.value.memberId}/notify`, {
      content: notifyContent.value.trim(),
    })
    alert('알림을 전송했습니다.')
    closeNotify()
  } catch (e) {
    alert(e.response?.data?.message || '알림 전송에 실패했습니다.')
  } finally {
    notifySending.value = false
  }
}

onMounted(fetchMembers)
</script>

<template>
  <div>
    <h1 class="font-bold mb-8" style="font-size: 2rem; color: var(--text-primary)">회원 관리</h1>

    <!-- 필터 -->
    <div class="flex flex-wrap gap-2 mb-6 items-center">
      <button
        v-for="t in ['all', 'INDIVIDUAL', 'ORGANIZATION', 'ADMIN']"
        :key="t"
        @click="memberTypeFilter = t; reloadMembers()"
        class="px-4 py-2 rounded-lg text-sm font-medium border"
        :style="{
          backgroundColor: memberTypeFilter === t ? 'var(--primary)' : 'var(--surface)',
          color: memberTypeFilter === t ? 'white' : 'var(--text-secondary)',
          borderColor: memberTypeFilter === t ? 'var(--primary)' : 'var(--border-color)',
        }"
      >
        {{ t === 'all' ? '전체' : memberTypeLabel[t] }}
      </button>
      <input
        v-model="memberKeyword"
        @keyup.enter="reloadMembers"
        placeholder="이메일·이름 검색 후 Enter"
        class="h-10 px-3 rounded-lg border outline-none text-sm flex-1 min-w-[200px]"
        style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
      />
    </div>

    <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
      <p v-if="memberLoading" class="text-center py-8" style="color: var(--text-muted)">불러오는 중...</p>
      <p v-else-if="members.length === 0" class="text-center py-8" style="color: var(--text-muted)">
        회원이 없습니다.
      </p>
      <table v-else class="w-full text-sm block md:table overflow-x-auto">
        <thead>
          <tr style="border-bottom: 1px solid var(--border-color)">
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">ID</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">이메일</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">이름</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">유형</th>
            <th class="text-right py-3 px-2" style="color: var(--text-muted)">포인트</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">상태</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">가입일</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">처리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in members" :key="m.memberId" style="border-bottom: 1px solid var(--border-color)">
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ m.memberId }}</td>
            <td class="py-3 px-2" style="color: var(--text-primary)">{{ m.email }}</td>
            <td class="py-3 px-2" style="color: var(--text-primary)">{{ m.name }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ memberTypeLabel[m.memberType] || m.memberType }}</td>
            <td class="py-3 px-2 text-right" style="color: var(--text-secondary)">{{ m.pointBalance?.toLocaleString() }}P</td>
            <td class="py-3 px-2">
              <span
                v-if="m.deletedAt"
                class="px-2 py-0.5 rounded-full text-xs font-medium"
                style="background-color: #f3f4f6; color: var(--text-muted)"
              >탈퇴</span>
              <span v-else class="px-2 py-0.5 rounded-full text-xs font-medium"
                style="background-color: rgba(45,191,142,0.12); color: var(--secondary)">활성</span>
            </td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ m.createdAt?.slice(0, 10) }}</td>
            <td class="py-3 px-2">
              <div v-if="!m.deletedAt && m.memberType !== 'ADMIN'" class="flex gap-1">
                <button
                  @click="sendNotify(m)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white"
                  style="background-color: var(--primary)"
                >알림</button>
                <button
                  @click="forceWithdraw(m)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white"
                  style="background-color: var(--danger)"
                >강제탈퇴</button>
              </div>
              <span v-else class="text-xs" style="color: var(--text-muted)">-</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 페이징 (20개씩) -->
    <div v-if="memberTotalPages > 1" class="flex justify-center items-center gap-2 mt-6">
      <button @click="goMemberPage(memberPage - 1)" :disabled="memberPage === 1"
        class="w-9 h-9 rounded-md border text-sm disabled:opacity-40"
        style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)">‹</button>
      <button v-for="p in memberTotalPages" :key="p" @click="goMemberPage(p)"
        class="w-9 h-9 rounded-md border text-sm"
        :style="{
          backgroundColor: memberPage === p ? 'var(--primary)' : 'var(--surface)',
          color: memberPage === p ? 'white' : 'var(--text-primary)',
          borderColor: memberPage === p ? 'var(--primary)' : 'var(--border-color)',
        }">{{ p }}</button>
      <button @click="goMemberPage(memberPage + 1)" :disabled="memberPage === memberTotalPages"
        class="w-9 h-9 rounded-md border text-sm disabled:opacity-40"
        style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)">›</button>
    </div>

    <!-- 알림 작성 모달 -->
    <div
      v-if="notifyTarget"
      class="fixed inset-0 z-50 flex items-center justify-center px-4"
      style="background-color: rgba(0,0,0,0.45)"
      @click.self="closeNotify"
    >
      <div class="w-full max-w-md rounded-2xl p-6" style="background-color: var(--surface)">
        <h3 class="font-bold mb-1" style="font-size: 1.25rem; color: var(--text-primary)">알림 보내기</h3>
        <p class="text-sm mb-4" style="color: var(--text-muted)">
          받는 사람: {{ notifyTarget.name }} ({{ notifyTarget.email }})
        </p>

        <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">내용</label>
        <textarea
          v-model="notifyContent"
          rows="6"
          placeholder="안녕하세요, 관리자입니다. 최근 ..."
          class="w-full px-3 py-2 mb-4 rounded-md border outline-none resize-none"
          style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
        />

        <div class="flex gap-2 justify-end">
          <button @click="closeNotify" class="px-4 py-2 rounded-md border text-sm font-medium"
            style="border-color: var(--border-color); color: var(--text-primary)">취소</button>
          <button @click="submitNotify" :disabled="notifySending"
            class="px-4 py-2 rounded-md text-sm font-medium text-white disabled:opacity-50"
            style="background-color: var(--primary)">
            {{ notifySending ? '전송 중...' : '전송' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
