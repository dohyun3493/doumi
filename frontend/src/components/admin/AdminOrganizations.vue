<script setup>
import { ref, onMounted } from 'vue'
import api from '@/lib/api'
import { Check, X } from 'lucide-vue-next'

// 승인/거부 시 셸의 '승인 대기' 배지를 갱신하도록 알림
const emit = defineEmits(['changed'])

const orgFilter = ref('PENDING')
const organizations = ref([])
const orgLoading = ref(false)

const orgStatusLabel = {
  PENDING: '승인 대기',
  APPROVED: '승인됨',
  REJECTED: '거절됨',
}

async function fetchOrganizations() {
  orgLoading.value = true
  try {
    const params = orgFilter.value === 'all' ? {} : { status: orgFilter.value }
    const res = await api.get('/admin/organizations', { params })
    organizations.value = res.data
  } catch (e) {
    console.error('단체 목록 조회 실패', e)
  } finally {
    orgLoading.value = false
  }
}

function setOrgFilter(status) {
  orgFilter.value = status
  fetchOrganizations()
}

async function approveOrg(org) {
  if (!confirm(`'${org.name}' 단체를 승인하시겠습니까?`)) return
  try {
    await api.patch(`/admin/organizations/${org.memberId}/approve`)
    fetchOrganizations()
    emit('changed')
  } catch (e) {
    alert(e.response?.data?.message || '승인 처리에 실패했습니다.')
  }
}

async function rejectOrg(org) {
  const reason = prompt(`'${org.name}' 단체의 승인을 거절합니다.\n거절 사유를 입력하세요:`)
  if (reason === null) return
  try {
    await api.patch(`/admin/organizations/${org.memberId}/reject`, { reason })
    fetchOrganizations()
    emit('changed')
  } catch (e) {
    alert(e.response?.data?.message || '거절 처리에 실패했습니다.')
  }
}

onMounted(fetchOrganizations)
</script>

<template>
  <div>
    <h1 class="font-bold mb-8" style="font-size: 2rem; color: var(--text-primary)">단체 승인</h1>

    <!-- 상태 필터 -->
    <div class="flex gap-2 mb-6">
      <button
        v-for="s in ['PENDING', 'APPROVED', 'REJECTED', 'all']"
        :key="s"
        @click="setOrgFilter(s)"
        class="px-4 py-2 rounded-lg text-sm font-medium border"
        :style="{
          backgroundColor: orgFilter === s ? 'var(--primary)' : 'var(--surface)',
          color: orgFilter === s ? 'white' : 'var(--text-secondary)',
          borderColor: orgFilter === s ? 'var(--primary)' : 'var(--border-color)',
        }"
      >
        {{ s === 'all' ? '전체' : orgStatusLabel[s] }}
      </button>
    </div>

    <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
      <p v-if="orgLoading" class="text-center py-8" style="color: var(--text-muted)">불러오는 중...</p>
      <p v-else-if="organizations.length === 0" class="text-center py-8" style="color: var(--text-muted)">
        해당 상태의 단체가 없습니다.
      </p>
      <table v-else class="w-full text-sm block md:table overflow-x-auto">
        <thead>
          <tr style="border-bottom: 1px solid var(--border-color)">
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">단체명</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">이메일</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">고유번호</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">상태</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">가입일</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">처리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="org in organizations" :key="org.memberId" style="border-bottom: 1px solid var(--border-color)">
            <td class="py-3 px-2 font-medium" style="color: var(--text-primary)">{{ org.name }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ org.email }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ org.orgRegNo || '-' }}</td>
            <td class="py-3 px-2">
              <span
                class="px-2 py-0.5 rounded-full text-xs font-medium"
                :style="{
                  backgroundColor: org.orgStatus === 'APPROVED' ? 'rgba(45,191,142,0.12)' : org.orgStatus === 'REJECTED' ? 'rgba(239,68,68,0.12)' : 'var(--accent)',
                  color: org.orgStatus === 'APPROVED' ? 'var(--secondary)' : org.orgStatus === 'REJECTED' ? 'var(--danger)' : 'var(--primary)',
                }"
              >{{ orgStatusLabel[org.orgStatus] || org.orgStatus }}</span>
              <p v-if="org.orgStatus === 'REJECTED' && org.rejectReason" class="text-xs mt-1" style="color: var(--text-muted)">
                사유: {{ org.rejectReason }}
              </p>
            </td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ org.createdAt?.slice(0, 10) }}</td>
            <td class="py-3 px-2">
              <div v-if="org.orgStatus === 'PENDING'" class="flex gap-1">
                <button
                  @click="approveOrg(org)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white flex items-center gap-1"
                  style="background-color: var(--secondary)"
                >
                  <Check class="w-3.5 h-3.5" /> 승인
                </button>
                <button
                  @click="rejectOrg(org)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white flex items-center gap-1"
                  style="background-color: var(--danger)"
                >
                  <X class="w-3.5 h-3.5" /> 거절
                </button>
              </div>
              <span v-else class="text-xs" style="color: var(--text-muted)">처리 완료</span>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
