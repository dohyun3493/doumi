<script setup>
import { ref, onMounted } from 'vue'
import api from '@/lib/api'

const campaigns = ref([])
const campaignLoading = ref(false)
const campaignStatusFilter = ref('모집완료')
const CAMPAIGN_STATUSES = ['모집완료', '전달완료', '사용완료', '모집중', '모집예정', 'all']

// 캠페인별 기부 내역 펼치기
const expandedCampaignId = ref(null)
const campaignDonations = ref([])
const donationsLoading = ref(false)

async function toggleDonations(c) {
  if (expandedCampaignId.value === c.campaignId) {
    expandedCampaignId.value = null   // 다시 누르면 접기
    return
  }
  expandedCampaignId.value = c.campaignId
  campaignDonations.value = []
  donationsLoading.value = true
  try {
    const res = await api.get(`/api/campaigns/${c.campaignId}/donations`)
    campaignDonations.value = res.data || []
  } catch (e) {
    console.error('기부 내역 조회 실패', e)
  } finally {
    donationsLoading.value = false
  }
}

async function fetchCampaigns() {
  campaignLoading.value = true
  try {
    const params = {}
    if (campaignStatusFilter.value !== 'all') params.status = campaignStatusFilter.value
    const res = await api.get('/api/campaigns', { params })
    campaigns.value = res.data || []
  } catch (e) {
    console.error('캠페인 목록 조회 실패', e)
  } finally {
    campaignLoading.value = false
  }
}

function setCampaignFilter(s) {
  campaignStatusFilter.value = s
  fetchCampaigns()
}

// 전달 가능 여부: 모집완료(마감)이거나 목표 달성, 그리고 아직 전달/사용 전
function canDeliver(c) {
  if (c.status === '전달완료' || c.status === '사용완료') return false
  return c.status === '모집완료' || (c.goalAmount > 0 && c.currentAmount >= c.goalAmount)
}

async function deliverCampaign(c) {
  if (!confirm(`'${c.title}' 캠페인의 기부금을 단체에 전달 처리하시겠습니까?`)) return
  try {
    await api.post(`/admin/campaigns/${c.campaignId}/deliver`)
    fetchCampaigns()
  } catch (e) {
    alert(e.response?.data?.message || '전달 처리에 실패했습니다.')
  }
}

// 삭제 요청 목록 + 승인/거부
const deleteRequests = ref([])

async function fetchDeleteRequests() {
  try {
    const res = await api.get('/admin/campaigns/delete-requests')
    deleteRequests.value = res.data
  } catch (e) {
    console.error('삭제 요청 목록 조회 실패', e)
  }
}

async function approveDelete(c) {
  if (!confirm(`'${c.title}' 캠페인 삭제 요청을 승인하시겠습니까?\n캠페인이 실제로 삭제됩니다.`)) return
  try {
    await api.post(`/admin/campaigns/${c.campaignId}/delete-approve`)
    fetchDeleteRequests()
    fetchCampaigns()
  } catch (e) {
    alert(e.response?.data?.message || '삭제 승인에 실패했습니다.')
  }
}

async function rejectDelete(c) {
  const reason = prompt(`'${c.title}' 삭제 요청을 거부합니다.\n거부 사유를 입력하세요:`)
  if (reason === null) return
  try {
    await api.post(`/admin/campaigns/${c.campaignId}/delete-reject`, { reason })
    fetchDeleteRequests()
  } catch (e) {
    alert(e.response?.data?.message || '삭제 거부에 실패했습니다.')
  }
}

onMounted(() => {
  fetchCampaigns()
  fetchDeleteRequests()
})
</script>

<template>
  <div>
    <h1 class="font-bold mb-8" style="font-size: 2rem; color: var(--text-primary)">캠페인 관리</h1>

    <!-- 삭제 요청 패널 (요청이 있을 때만) -->
    <div v-if="deleteRequests.length > 0" class="rounded-xl p-6 border mb-8"
      style="background-color: var(--surface); border-color: var(--danger)">
      <h2 class="font-semibold mb-4 flex items-center gap-2" style="color: var(--danger)">
        🗑️ 삭제 요청 {{ deleteRequests.length }}건
      </h2>
      <table class="w-full text-sm block md:table overflow-x-auto">
        <thead>
          <tr style="border-bottom: 1px solid var(--border-color)">
            <th class="text-left py-2 px-2" style="color: var(--text-muted)">캠페인명</th>
            <th class="text-left py-2 px-2" style="color: var(--text-muted)">단체</th>
            <th class="text-left py-2 px-2" style="color: var(--text-muted)">요청 사유</th>
            <th class="text-left py-2 px-2" style="color: var(--text-muted)">처리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="c in deleteRequests" :key="c.campaignId" style="border-bottom: 1px solid var(--border-color)">
            <td class="py-3 px-2 font-medium" style="color: var(--text-primary)">{{ c.title }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ c.orgName }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ c.deleteRequestReason || '-' }}</td>
            <td class="py-3 px-2">
              <div class="flex gap-1">
                <button @click="approveDelete(c)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white"
                  style="background-color: var(--danger)">삭제 승인</button>
                <button @click="rejectDelete(c)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium border"
                  style="border-color: var(--border-color); color: var(--text-primary)">거부</button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 상태 필터 -->
    <div class="flex flex-wrap gap-2 mb-6">
      <button
        v-for="s in CAMPAIGN_STATUSES"
        :key="s"
        @click="setCampaignFilter(s)"
        class="px-4 py-2 rounded-lg text-sm font-medium border"
        :style="{
          backgroundColor: campaignStatusFilter === s ? 'var(--primary)' : 'var(--surface)',
          color: campaignStatusFilter === s ? 'white' : 'var(--text-secondary)',
          borderColor: campaignStatusFilter === s ? 'var(--primary)' : 'var(--border-color)',
        }"
      >
        {{ s === 'all' ? '전체' : s }}
      </button>
    </div>

    <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
      <p v-if="campaignLoading" class="text-center py-8" style="color: var(--text-muted)">불러오는 중...</p>
      <p v-else-if="campaigns.length === 0" class="text-center py-8" style="color: var(--text-muted)">
        해당 상태의 캠페인이 없습니다.
      </p>
      <table v-else class="w-full text-sm block md:table overflow-x-auto">
        <thead>
          <tr style="border-bottom: 1px solid var(--border-color)">
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">ID</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">캠페인명</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">단체</th>
            <th class="text-right py-3 px-2" style="color: var(--text-muted)">모금 현황</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">상태</th>
            <th class="text-left py-3 px-2" style="color: var(--text-muted)">처리</th>
          </tr>
        </thead>
        <tbody>
          <template v-for="c in campaigns" :key="c.campaignId">
          <tr style="border-bottom: 1px solid var(--border-color)">
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ c.campaignId }}</td>
            <td class="py-3 px-2 font-medium" style="color: var(--text-primary)">{{ c.title }}</td>
            <td class="py-3 px-2" style="color: var(--text-secondary)">{{ c.orgName }}</td>
            <td class="py-3 px-2 text-right" style="color: var(--text-secondary)">
              ₩{{ c.currentAmount?.toLocaleString() }} / ₩{{ c.goalAmount?.toLocaleString() }}
            </td>
            <td class="py-3 px-2">
              <span class="px-2 py-0.5 rounded-full text-xs font-medium"
                :style="{
                  backgroundColor: c.status === '전달완료' ? 'rgba(45,191,142,0.12)' : c.status === '모집중' ? 'var(--accent)' : '#f3f4f6',
                  color: c.status === '전달완료' ? 'var(--secondary)' : c.status === '모집중' ? 'var(--primary)' : 'var(--text-muted)',
                }">{{ c.status }}</span>
            </td>
            <td class="py-3 px-2">
              <div class="flex gap-1">
                <button
                  v-if="canDeliver(c)"
                  @click="deliverCampaign(c)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium text-white"
                  style="background-color: var(--primary)"
                >전달</button>
                <button
                  @click="toggleDonations(c)"
                  class="px-3 py-1.5 rounded-md text-xs font-medium border"
                  style="border-color: var(--border-color); color: var(--text-primary)"
                >{{ expandedCampaignId === c.campaignId ? '닫기' : '내역' }}</button>
              </div>
            </td>
          </tr>

          <!-- 기부 내역 펼침 행 -->
          <tr v-if="expandedCampaignId === c.campaignId">
            <td colspan="6" class="px-4 py-3" style="background-color: var(--surface-alt)">
              <p v-if="donationsLoading" class="text-center py-3" style="color: var(--text-muted)">불러오는 중...</p>
              <p v-else-if="campaignDonations.length === 0" class="text-center py-3" style="color: var(--text-muted)">
                기부 내역이 없습니다.
              </p>
              <table v-else class="w-full text-xs">
                <thead>
                  <tr style="border-bottom: 1px solid var(--border-color)">
                    <th class="text-left py-2 px-2" style="color: var(--text-muted)">기부자</th>
                    <th class="text-right py-2 px-2" style="color: var(--text-muted)">금액</th>
                    <th class="text-left py-2 px-2" style="color: var(--text-muted)">일시</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="d in campaignDonations" :key="d.donationId">
                    <td class="py-2 px-2" style="color: var(--text-primary)">{{ d.maskedName }}</td>
                    <td class="py-2 px-2 text-right font-medium" style="color: var(--primary)">₩{{ d.amount?.toLocaleString() }}</td>
                    <td class="py-2 px-2" style="color: var(--text-secondary)">{{ d.donatedAt?.slice(0, 16).replace('T', ' ') }}</td>
                  </tr>
                </tbody>
              </table>
            </td>
          </tr>
          </template>
        </tbody>
      </table>
    </div>
  </div>
</template>
