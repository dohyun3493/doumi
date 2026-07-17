<script setup>
import { ref, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Users, Clock, Heart, ExternalLink } from 'lucide-vue-next'
import api, { uploadImage } from '@/lib/api'
import { useAuthStore } from '@/stores/auth'
import { imageUrl } from '@/lib/utils'
import ImageUploader from '@/components/ImageUploader.vue'
import placeholderImg from '@/assets/placeholder.jpg'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
let id = route.params.id

// ===== 찜 =====
const bookmarked = ref(false)

async function fetchBookmarked() {
  if (!authStore.isAuthenticated) return
  try {
    const res = await api.get('/bookmarks')
    bookmarked.value = res.data.some((c) => String(c.campaignId) === String(id))
  } catch (e) { /* 미로그인/실패 시 무시 */ }
}

async function toggleBookmark() {
  if (!authStore.isAuthenticated) {
    router.push('/login')
    return
  }
  try {
    if (bookmarked.value) {
      await api.delete(`/bookmarks/${id}`)
    } else {
      await api.post(`/bookmarks/${id}`)
    }
    bookmarked.value = !bookmarked.value
  } catch (e) {
    console.error('찜 처리 실패', e)
  }
}

const activeTab = ref('intro')
const campaign = ref(null)

const progress = computed(() => {
  if (!campaign.value) return 0
  return campaign.value.goalAmount > 0
    ? Math.min(Math.round((campaign.value.currentAmount / campaign.value.goalAmount) * 100), 100)
    : 0
})

const daysLeft = computed(() => {
  if (!campaign.value) return 0
  const today = new Date()
  const end = new Date(campaign.value.endDate)
  return Math.max(0, Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)))
})

const donations = ref([])

// ===== 기부금 사용 보고 =====
const report = ref(null)

// 보고서 작성 폼 (캠페인 소유 단체에게만 노출)
const showReportForm = ref(false)
const isEditMode = ref(false)   // true면 기존 보고 수정(PUT), false면 신규 등록(POST)
const reportForm = ref({
  content: '',
  expenses: [{ item: '', amount: null }],
  receipts: [],   // 영수증 사진 URL 목록
  reviews: [],    // 후기 사진 URL 목록
})
const reportMsg = ref('')
const reportImageUploading = ref(false)

// 보고서 사진 추가 (type: 'receipts' | 'reviews')
async function addReportImage(type, event) {
  const file = event.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) {
    reportMsg.value = '이미지 파일만 업로드할 수 있습니다.'
    event.target.value = ''
    return
  }
  reportImageUploading.value = true
  reportMsg.value = ''
  try {
    const url = await uploadImage(file)
    reportForm.value[type].push(url)
  } catch (e) {
    reportMsg.value = e.response?.data?.message || '이미지 업로드에 실패했습니다.'
  } finally {
    reportImageUploading.value = false
    event.target.value = ''   // 같은 파일 재선택 허용
  }
}

function removeReportImage(type, idx) {
  reportForm.value[type].splice(idx, 1)
}

// 보고서 조회 시 사진을 종류별로 분리
const reportReceipts = computed(() =>
  (report.value?.images || []).filter((i) => i.imageType === 'RECEIPT'))
const reportReviews = computed(() =>
  (report.value?.images || []).filter((i) => i.imageType === 'REVIEW'))

const isOwner = computed(() =>
  campaign.value?.ownerId != null
  && authStore.currentUser?.memberId === campaign.value.ownerId
)

// 관리자는 백엔드에서 모든 캠페인의 보고 작성/수정이 허용됨 (소유자가 아니어도)
const isAdmin = computed(() => authStore.currentUser?.memberType === 'ADMIN')

// 기부는 '모집중'일 때만 가능 (백엔드 규칙과 일치). 모집예정/완료/사용완료는 불가
const canDonate = computed(() => campaign.value?.status === '모집중')

// 단체 홈페이지 URL — 프로토콜 없으면 https:// 붙여 안전하게 정규화. 없으면 null
const orgHomepage = computed(() => {
  const url = campaign.value?.homepageUrl?.trim()
  if (!url) return null
  return /^https?:\/\//i.test(url) ? url : `https://${url}`
})

// 소유 단체 또는 관리자만 보고 작성 가능 (백엔드 권한 규칙과 일치)
const canWriteReport = computed(() =>
  (isOwner.value || isAdmin.value)
  && ['모집완료', '전달완료'].includes(campaign.value?.status)
  && !report.value
)

// 이미 등록된 보고가 있고 소유 단체/관리자면 수정 가능
const canEditReport = computed(() =>
  (isOwner.value || isAdmin.value) && !!report.value
)

// 기존 보고 내용을 폼에 채워 수정 모드로 진입
function startEditReport() {
  const r = report.value
  if (!r) return
  reportForm.value = {
    content: r.content || '',
    expenses: (r.expenses || []).map((e) => ({ item: e.item, amount: e.amount })),
    receipts: (r.images || []).filter((i) => i.imageType === 'RECEIPT').map((i) => i.imageUrl),
    reviews: (r.images || []).filter((i) => i.imageType === 'REVIEW').map((i) => i.imageUrl),
  }
  if (reportForm.value.expenses.length === 0) {
    reportForm.value.expenses = [{ item: '', amount: null }]
  }
  reportMsg.value = ''
  isEditMode.value = true
  showReportForm.value = true
}

// 폼 닫기(취소) — 신규/수정 공통
function closeReportForm() {
  showReportForm.value = false
  isEditMode.value = false
}

async function fetchReport() {
  try {
    const res = await api.get(`/api/campaigns/${id}/report`)
    report.value = res.data
  } catch (e) {
    report.value = null   // 404 = 아직 보고 없음
  }
}

function addExpenseRow() {
  reportForm.value.expenses.push({ item: '', amount: null })
}

function removeExpenseRow(idx) {
  if (reportForm.value.expenses.length > 1) reportForm.value.expenses.splice(idx, 1)
}

// ===== AI 보고서 초안 도우미 =====
// 첨부한 영수증/사진 + 메모를 보내면 AI가 '보고 내용'과 '지출 내역'을 정리해 폼을 채운다.
const showAiPanel = ref(true)
const aiDrafting = ref(false)
const aiInput = ref({ note: '' })

async function generateAiDraft() {
  aiDrafting.value = true
  reportMsg.value = ''
  try {
    // 단체가 미리 적어둔 지출 행은 힌트로 함께 전달
    const expenseHints = reportForm.value.expenses
      .filter((e) => e.item.trim())
      .map((e) => ({ item: e.item.trim(), amount: Number(e.amount) || 0 }))
    const res = await api.post(`/api/campaigns/${id}/report/draft`, {
      note: aiInput.value.note,
      expenses: expenseHints,
    })
    if (res.data.content) reportForm.value.content = res.data.content
    if (Array.isArray(res.data.expenses) && res.data.expenses.length) {
      reportForm.value.expenses = res.data.expenses.map((e) => ({ item: e.item, amount: e.amount }))
    }
  } catch (e) {
    reportMsg.value = e.response?.data?.message || 'AI 초안 생성에 실패했습니다.'
  } finally {
    aiDrafting.value = false
  }
}

async function submitReport() {
  reportMsg.value = ''
  const expenses = reportForm.value.expenses
    .filter((e) => e.item.trim() && e.amount > 0)
    .map((e) => ({ item: e.item.trim(), amount: Number(e.amount) }))

  if (!reportForm.value.content.trim() || expenses.length === 0) {
    reportMsg.value = '보고 내용과 지출 항목을 1개 이상 입력해주세요.'
    return
  }

  // 영수증/후기 사진을 { url, type } 형태로 합침
  const images = [
    ...reportForm.value.receipts.map((url) => ({ url, type: 'RECEIPT' })),
    ...reportForm.value.reviews.map((url) => ({ url, type: 'REVIEW' })),
  ]

  const payload = { content: reportForm.value.content.trim(), expenses, images }

  try {
    if (isEditMode.value) {
      // 수정: 본문/지출/이미지 교체 (status는 그대로 '사용완료')
      await api.put(`/api/campaigns/${id}/report`, payload)
      closeReportForm()
      await fetchReport()
    } else {
      await api.post(`/api/campaigns/${id}/report`, payload)
      closeReportForm()
      // 보고 등록으로 status가 '사용완료'로 바뀌므로 캠페인도 다시 조회
      const campaignRes = await api.get(`/api/campaigns/${id}`)
      campaign.value = campaignRes.data
      await fetchReport()
    }
  } catch (e) {
    reportMsg.value = e.response?.data?.message
      || (isEditMode.value ? '보고 수정에 실패했습니다.' : '보고 등록에 실패했습니다.')
  }
}

// 소유 단체: 캠페인 삭제 요청 페이지로 이동 (관리자 승인 후 실제 삭제)
function requestDelete() {
  router.push(`/campaigns/${id}/delete-request`)
}

async function loadCampaign() {
  try {
    const [campaignRes, donationsRes] = await Promise.all([
      api.get(`/api/campaigns/${id}`),
      api.get(`/api/campaigns/${id}/donations`),
    ])
    campaign.value = campaignRes.data
    donations.value = donationsRes.data
  } catch (e) {
    console.error('캠페인 로드 실패', e)
  }
  fetchBookmarked()
  fetchReport()
}

// 같은 CampaignDetail 컴포넌트에서 다른 캠페인(/campaigns/:id)으로 이동하면
// Vue Router가 컴포넌트를 재사용(재마운트 X)하므로 라우트 파라미터를 watch 해 재조회한다.
// immediate:true 로 최초 진입 시에도 한 번 로드.
watch(() => route.params.id, (newId) => {
  id = newId
  loadCampaign()
}, { immediate: true })
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div v-if="campaign" class="max-w-[1200px] mx-auto px-6 py-12">
      <div class="grid lg:grid-cols-[1fr_400px] gap-8">
        <!-- Left: Content -->
        <div>
          <img
            :src="imageUrl(campaign.thumbnailUrl) || placeholderImg"
            :alt="campaign.title"
            class="w-full h-96 object-cover rounded-2xl mb-6"
          />

          <div class="flex items-center gap-3 mb-4">
            <span class="rounded-full px-3 py-1 text-sm" style="background-color: var(--accent); color: var(--primary)">
              {{ campaign.category || '기타' }}
            </span>
            <span class="text-sm" style="color: var(--text-muted)">{{ campaign.orgName }}</span>
          </div>

          <h1 class="font-bold mb-6" style="font-size: 2rem; color: var(--text-primary)">{{ campaign.title }}</h1>

          <div class="mb-4 flex gap-2 border-b" style="border-color: var(--border-color)">
            <button
              v-for="tab in ['intro', 'donations', 'report']"
              :key="tab"
              @click="activeTab = tab"
              class="px-4 py-2 text-sm font-medium transition-colors"
              :style="{
                color: activeTab === tab ? 'var(--primary)' : 'var(--text-secondary)',
                borderBottom: activeTab === tab ? '2px solid var(--primary)' : '2px solid transparent',
              }"
            >
              {{ tab === 'intro' ? '소개' : tab === 'donations' ? '기부 내역' : '사용 보고' }}
            </button>
          </div>

          <div v-if="activeTab === 'intro'" class="mt-6 space-y-4" style="color: var(--text-primary)">
            <p>{{ campaign.description }}</p>
            <p v-if="campaign.purpose"><strong>모집목적:</strong> {{ campaign.purpose }}</p>
            <p v-if="campaign.address"><strong>주소:</strong> {{ campaign.address }}</p>
            <p v-if="campaign.telNo"><strong>전화번호:</strong> {{ campaign.telNo }}</p>
            <a v-if="orgHomepage" :href="orgHomepage" target="_blank" rel="noopener noreferrer"
              class="inline-flex items-center gap-1.5 px-4 h-10 rounded-lg border text-sm font-medium transition-colors"
              style="border-color: var(--primary); color: var(--primary)">
              <ExternalLink class="w-4 h-4" />
              단체 홈페이지 바로가기
            </a>
          </div>

          <div v-else-if="activeTab === 'donations'" class="mt-6 space-y-4">
            <p v-if="donations.length === 0" class="text-center py-8" style="color: var(--text-muted)">아직 기부 내역이 없습니다.</p>
            <div v-for="donation in donations" :key="donation.donationId" class="p-4 rounded-xl" style="background-color: var(--surface)">
              <div class="flex justify-between items-center">
                <div class="flex items-center gap-3">
                  <div class="w-10 h-10 rounded-full flex items-center justify-center font-semibold" style="background-color: var(--accent); color: var(--primary)">
                    {{ donation.maskedName[0] }}
                  </div>
                  <div>
                    <div class="font-semibold" style="color: var(--text-primary)">{{ donation.maskedName }}</div>
                    <div class="text-xs" style="color: var(--text-muted)">{{ donation.donatedAt?.slice(0, 10) }}</div>
                  </div>
                </div>
                <div class="font-bold" style="color: var(--primary)">₩{{ donation.amount.toLocaleString() }}</div>
              </div>
            </div>
          </div>

          <!-- 사용 보고 탭 -->
          <div v-else class="mt-6">
            <!-- 보고서가 있는 경우 (수정 중이 아닐 때만 표시) -->
            <div v-if="report && !showReportForm">
              <div class="p-5 rounded-xl mb-4" style="background-color: var(--surface)">
                <div class="flex items-center justify-between mb-3">
                  <span class="font-semibold" style="color: var(--text-primary)">기부금 사용 보고</span>
                  <div class="flex items-center gap-3">
                    <span class="text-xs" style="color: var(--text-muted)">{{ report.createdAt?.slice(0, 10) }}</span>
                    <button
                      v-if="canEditReport"
                      class="px-3 py-1 rounded-md border text-xs font-medium"
                      style="border-color: var(--primary); color: var(--primary)"
                      @click="startEditReport"
                    >수정</button>
                  </div>
                </div>
                <p class="whitespace-pre-line text-sm" style="color: var(--text-primary)">{{ report.content }}</p>
              </div>

              <div class="p-5 rounded-xl" style="background-color: var(--surface)">
                <span class="font-semibold block mb-3" style="color: var(--text-primary)">지출 내역</span>
                <table class="w-full text-sm">
                  <thead>
                    <tr style="border-bottom: 1px solid var(--border-color)">
                      <th class="text-left py-2" style="color: var(--text-muted)">항목</th>
                      <th class="text-right py-2" style="color: var(--text-muted)">금액</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="e in report.expenses" :key="e.expenseId" style="border-bottom: 1px solid var(--border-color)">
                      <td class="py-2" style="color: var(--text-primary)">{{ e.item }}</td>
                      <td class="py-2 text-right" style="color: var(--text-primary)">₩{{ e.amount.toLocaleString() }}</td>
                    </tr>
                    <tr>
                      <td class="py-2 font-bold" style="color: var(--text-primary)">합계</td>
                      <td class="py-2 text-right font-bold" style="color: var(--primary)">₩{{ report.totalSpent.toLocaleString() }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>

              <!-- 영수증 사진 -->
              <div v-if="reportReceipts.length" class="p-5 rounded-xl mt-4" style="background-color: var(--surface)">
                <span class="font-semibold block mb-3" style="color: var(--text-primary)">영수증</span>
                <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  <a v-for="img in reportReceipts" :key="img.imageId" :href="imageUrl(img.imageUrl)" target="_blank" rel="noopener" class="block aspect-square rounded-lg overflow-hidden">
                    <img :src="imageUrl(img.imageUrl)" alt="영수증" class="w-full h-full object-cover" />
                  </a>
                </div>
              </div>

              <!-- 후기 사진 -->
              <div v-if="reportReviews.length" class="p-5 rounded-xl mt-4" style="background-color: var(--surface)">
                <span class="font-semibold block mb-3" style="color: var(--text-primary)">후기</span>
                <div class="grid grid-cols-3 sm:grid-cols-4 gap-2">
                  <a v-for="img in reportReviews" :key="img.imageId" :href="imageUrl(img.imageUrl)" target="_blank" rel="noopener" class="block aspect-square rounded-lg overflow-hidden">
                    <img :src="imageUrl(img.imageUrl)" alt="후기" class="w-full h-full object-cover" />
                  </a>
                </div>
              </div>
            </div>

            <!-- 보고서가 없거나, 등록/수정 폼을 연 경우 -->
            <div v-else>
              <p v-if="!canWriteReport && !showReportForm" style="color: var(--text-muted)">아직 등록된 사용 보고가 없습니다.</p>

              <!-- 소유 단체/관리자: 작성 안내 + 폼 (수정 모드에서도 이 분기로 폼 표시) -->
              <div v-else>
                <div v-if="!showReportForm" class="p-5 rounded-xl flex items-center justify-between" style="background-color: var(--accent)">
                  <p class="text-sm" style="color: var(--text-primary)">
                    모금이 종료되었습니다. 기부금 사용 보고를 등록하면 캠페인이 종결(사용완료) 처리됩니다.
                  </p>
                  <button
                    class="px-4 py-2 rounded-md text-white text-sm font-medium shrink-0"
                    style="background-color: var(--primary)"
                    @click="showReportForm = true"
                  >보고 작성</button>
                </div>

                <div v-else class="p-5 rounded-xl space-y-4" style="background-color: var(--surface)">
                  <!-- AI 도우미: 메모를 주면 보고 내용 + 지출 내역을 정리해 아래 칸을 채워준다 (등록/수정 모두 사용 가능) -->
                  <div class="rounded-xl border p-4" style="border-color: var(--primary); background-color: var(--accent)">
                    <button type="button" class="w-full flex items-center justify-between text-sm font-semibold"
                      style="color: var(--primary)" @click="showAiPanel = !showAiPanel">
                      <span>✨ AI 도우미 — 메모로 자동 작성</span>
                      <span>{{ showAiPanel ? '−' : '+' }}</span>
                    </button>

                    <div v-if="showAiPanel" class="mt-4 space-y-3">
                      <p class="text-xs" style="color: var(--text-secondary)">
                        어떤 활동을 했고 어디에 얼마를 썼는지 적고 [초안 생성]을 누르면, AI가 보고 내용과 지출 내역을 정리해 줍니다.
                      </p>

                      <textarea v-model="aiInput.note" rows="3"
                        placeholder="예) 연탄 5,000장을 장당 600원에 구입해 독거 어르신 200가구에 전달했어요."
                        class="w-full p-3 rounded-md border outline-none text-sm"
                        style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />

                      <div class="flex items-center gap-3 flex-wrap">
                        <button type="button" :disabled="aiDrafting"
                          class="px-4 py-2 rounded-md text-white text-sm font-medium disabled:opacity-50"
                          style="background-color: var(--primary)"
                          @click="generateAiDraft">
                          {{ aiDrafting ? 'AI가 정리하는 중…' : '초안 생성' }}
                        </button>
                        <span class="text-xs" style="color: var(--text-muted)">생성된 내용·금액은 등록 전 꼭 확인·수정하세요.</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label class="text-sm font-medium block mb-1" style="color: var(--text-primary)">보고 내용</label>
                    <textarea
                      v-model="reportForm.content"
                      rows="5"
                      placeholder="기부금이 어떻게 사용되었는지 기부자들에게 알려주세요."
                      class="w-full p-3 rounded-md border outline-none text-sm"
                      style="border-color: var(--border-color); background-color: var(--background); color: var(--text-primary)"
                    />
                  </div>

                  <div>
                    <label class="text-sm font-medium block mb-1" style="color: var(--text-primary)">지출 내역</label>
                    <div v-for="(e, idx) in reportForm.expenses" :key="idx" class="flex gap-2 mb-2">
                      <input
                        v-model="e.item"
                        type="text"
                        placeholder="지출 항목 (예: 사료 구입)"
                        class="flex-1 h-10 px-3 rounded-md border outline-none text-sm"
                        style="border-color: var(--border-color); background-color: var(--background); color: var(--text-primary)"
                      />
                      <input
                        v-model.number="e.amount"
                        type="number"
                        min="1"
                        placeholder="금액(원)"
                        class="w-36 h-10 px-3 rounded-md border outline-none text-sm"
                        style="border-color: var(--border-color); background-color: var(--background); color: var(--text-primary)"
                      />
                      <button
                        class="w-10 h-10 rounded-md border text-sm"
                        style="border-color: var(--border-color); color: var(--text-muted)"
                        @click="removeExpenseRow(idx)"
                      >−</button>
                    </div>
                    <button class="text-sm" style="color: var(--primary)" @click="addExpenseRow">+ 항목 추가</button>
                  </div>


                  <!-- 후기 사진 -->
                  <div>
                    <label class="text-sm font-medium block mb-2" style="color: var(--text-primary)">후기 사진 <span class="text-xs" style="color: var(--text-muted)">(선택)</span></label>
                    <div class="flex flex-wrap gap-2">
                      <div v-for="(url, idx) in reportForm.reviews" :key="url" class="relative w-20 h-20 rounded-lg overflow-hidden">
                        <img :src="imageUrl(url)" alt="" class="w-full h-full object-cover" />
                        <button type="button" class="absolute top-0.5 right-0.5 w-5 h-5 rounded-full text-white text-xs flex items-center justify-center" style="background-color: rgba(0,0,0,0.6)" @click="removeReportImage('reviews', idx)">×</button>
                      </div>
                      <label class="w-20 h-20 rounded-lg border-2 border-dashed flex items-center justify-center cursor-pointer text-2xl" style="border-color: var(--border-color); color: var(--text-muted)">
                        +
                        <input type="file" accept="image/*" class="hidden" @change="addReportImage('reviews', $event)" />
                      </label>
                    </div>
                  </div>

                  <p v-if="reportImageUploading" class="text-sm" style="color: var(--text-muted)">이미지 업로드 중...</p>
                  <p v-if="reportMsg" class="text-sm" style="color: var(--danger)">{{ reportMsg }}</p>

                  <div class="flex gap-2">
                    <button
                      class="px-5 h-11 rounded-md text-white text-sm font-medium"
                      style="background-color: var(--primary)"
                      @click="submitReport"
                    >{{ isEditMode ? '수정 완료' : '보고 등록' }}</button>
                    <button
                      class="px-5 h-11 rounded-md border text-sm"
                      style="border-color: var(--border-color); color: var(--text-secondary)"
                      @click="closeReportForm"
                    >취소</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Right: Sticky Donation Panel -->
        <div>
          <div class="sticky top-24 rounded-2xl p-6" style="background-color: var(--surface); box-shadow: 0 2px 12px rgba(0,0,0,0.06)">
            <div class="mb-6">
              <div class="font-bold mb-2" style="font-size: 1.75rem; color: var(--primary)">
                ₩{{ campaign.currentAmount?.toLocaleString() }}
              </div>
              <div class="h-2.5 rounded-full mb-2" style="background-color: var(--border-color)">
                <div class="h-full rounded-full" :style="{ backgroundColor: 'var(--primary)', width: `${progress}%` }" />
              </div>
              <div class="flex justify-between text-sm mb-4">
                <span style="color: var(--text-muted)">목표 ₩{{ campaign.goalAmount?.toLocaleString() }}</span>
                <span class="font-semibold" style="color: var(--primary)">{{ progress }}% 달성</span>
              </div>

              <div class="grid grid-cols-2 gap-4">
                <div class="flex items-center gap-2">
                  <Users class="w-4 h-4" style="color: var(--text-muted)" />
                  <span class="text-sm" style="color: var(--text-secondary)">{{ campaign.status }}</span>
                </div>
                <div class="flex items-center gap-2">
                  <Clock class="w-4 h-4" style="color: var(--text-muted)" />
                  <span class="text-sm" style="color: var(--text-secondary)">
                    <span class="font-semibold">D-{{ daysLeft }}</span> 남음
                  </span>
                </div>
              </div>
            </div>

            <div class="flex gap-2 mb-4">
              <RouterLink v-if="canDonate" :to="`/donate/${campaign.campaignId}`" class="flex-1">
                <button class="w-full h-14 rounded-lg text-white font-medium" style="background-color: var(--primary)">
                  지금 기부하기 ❤️
                </button>
              </RouterLink>
              <button v-else disabled
                class="flex-1 h-14 rounded-lg font-medium cursor-not-allowed"
                style="background-color: var(--border-color); color: var(--text-muted)">
                {{ campaign.status === '모집예정' ? '모집 예정 (기부 준비 중)' : '기부 마감' }}
              </button>
              <button
                class="w-14 h-14 rounded-lg flex items-center justify-center border transition-colors"
                :style="{
                  borderColor: bookmarked ? 'var(--primary)' : 'var(--border-color)',
                  backgroundColor: bookmarked ? 'var(--accent)' : 'transparent',
                }"
                :aria-label="bookmarked ? '찜 해제' : '찜하기'"
                @click="toggleBookmark"
              >
                <Heart
                  class="w-6 h-6"
                  :style="{ color: bookmarked ? 'var(--primary)' : 'var(--text-muted)' }"
                  :fill="bookmarked ? 'var(--primary)' : 'none'"
                />
              </button>
            </div>

            <!-- 소유 단체/관리자: 캠페인 수정 (사용완료된 캠페인은 제외) -->
            <div v-if="isOwner && campaign.status !== '사용완료'" class="mb-4 space-y-2">
              <button
                class="w-full h-11 rounded-lg border font-medium text-sm transition-colors"
                style="border-color: var(--primary); color: var(--primary)"
                @click="router.push(`/campaigns/${campaign.campaignId}/edit`)"
              >
                캠페인 수정
              </button>
              <!-- 삭제는 관리자 승인 후 처리됨 -->
              <button
                v-if="!campaign.deleteRequested"
                class="w-full h-11 rounded-lg border font-medium text-sm transition-colors"
                style="border-color: var(--danger); color: var(--danger)"
                @click="requestDelete"
              >
                삭제 요청
              </button>
              <p v-else class="text-center text-sm py-2" style="color: var(--text-muted)">
                삭제 요청됨 — 관리자 승인 대기 중
              </p>
            </div>

          </div>
        </div>
      </div>
    </div>

    <div v-else class="flex items-center justify-center min-h-screen">
      <p style="color: var(--text-muted)">캠페인 정보를 불러오는 중...</p>
    </div>
  </div>
</template>
