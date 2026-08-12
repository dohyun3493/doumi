<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, LogOut, TrendingUp, Award, DollarSign, Trash2, Heart, Megaphone, Shield } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'
import { imageUrl } from '@/lib/utils'
import CampaignCard from '@/components/CampaignCard.vue'
import ImageUploader from '@/components/ImageUploader.vue'

const router = useRouter()
const authStore = useAuthStore()

const activeMenu = ref('donations')
const showChargeModal = ref(false)
const chargeAmount = ref('')
const chargeMsg = ref('')

// 단체/관리자만 캠페인을 등록할 수 있으므로 '나의 캠페인' 메뉴도 그들에게만 노출
const isOrg = computed(() => ['ORGANIZATION', 'ADMIN'].includes(authStore.currentUser?.memberType))

// 관리자만 관리자 사이트로 이동하는 버튼을 노출
const isAdmin = computed(() => authStore.currentUser?.memberType === 'ADMIN')

const menuItems = computed(() => [
  { id: 'donations', label: '나의 기부 내역', Icon: TrendingUp },
  ...(isOrg.value ? [{ id: 'myCampaigns', label: '나의 캠페인', Icon: Megaphone }] : []),
  { id: 'bookmarks', label: '찜한 캠페인', Icon: Heart },
  { id: 'points', label: '포인트 관리', Icon: DollarSign },
  { id: 'profile', label: '내 정보 수정', Icon: User },
])

// ===== 나의 캠페인 (단체) =====
const myCampaigns = ref([])

async function fetchMyCampaigns() {
  if (!isOrg.value) return
  try {
    const res = await api.get('/campaigns/mine')
    myCampaigns.value = res.data
  } catch (e) {
    console.error('나의 캠페인 조회 실패', e)
  }
}

// ===== 찜한 캠페인 =====
const bookmarks = ref([])

async function fetchBookmarks() {
  try {
    const res = await api.get('/bookmarks')
    bookmarks.value = res.data
  } catch (e) {
    console.error('찜 목록 조회 실패', e)
  }
}

function progressOf(c) {
  return c.goalAmount > 0 ? Math.min(Math.round((c.currentAmount / c.goalAmount) * 100), 100) : 0
}

function daysLeftOf(c) {
  const end = new Date(c.endDate)
  return Math.max(0, Math.ceil((end.getTime() - Date.now()) / (1000 * 60 * 60 * 24)))
}

// 실제 포인트 내역 (DB에서 조회)
const pointHistory = ref([])

// 실제 기부 내역 (donation 테이블 - 캠페인 이름 포함)
const donations = ref([])

// 총 기부 금액
const totalDonationAmount = computed(() =>
  donations.value.reduce((sum, d) => sum + d.amount, 0)
)

// ===== 기부 내역 페이징 (10개씩) =====
const donationPage = ref(1)
const donationPageSize = 10

const pagedDonations = computed(() => {
  const start = (donationPage.value - 1) * donationPageSize
  return donations.value.slice(start, start + donationPageSize)
})

const donationTotalPages = computed(() =>
  Math.max(1, Math.ceil(donations.value.length / donationPageSize))
)

function goToDonationPage(page) {
  if (page < 1 || page > donationTotalPages.value) return
  donationPage.value = page
}

// ===== 내 정보 수정 =====
const profileForm = ref({ name: '', password: '', profileImageUrl: '' })
const profileMsg = ref('')
const profileSaving = ref(false)

async function fetchMemberInfo() {
  if (!authStore.currentUser?.email) return
  try {
    const res = await api.get(`/members/${encodeURIComponent(authStore.currentUser.email)}`)
    // DB의 실제 잔액으로 store 동기화
    authStore.syncPointBalance(res.data.pointBalance)
    // 이름·프로필 사진을 store에 반영 (헤더/사이드바 아바타 표시용)
    authStore.updateUser({ name: res.data.name, profileImageUrl: res.data.profileImageUrl })
    // 수정 폼 초기값 채우기
    profileForm.value.name = res.data.name || ''
    profileForm.value.profileImageUrl = res.data.profileImageUrl || ''
  } catch (e) {
    console.error('회원 정보 조회 실패', e)
  }
}

async function saveProfile() {
  profileMsg.value = ''
  const payload = {
    name: profileForm.value.name.trim(),
    // 빈 문자열이면 사진 제거를 의미하므로 항상 포함
    profileImageUrl: profileForm.value.profileImageUrl,
  }
  if (profileForm.value.password) {
    payload.password = profileForm.value.password
  }
  profileSaving.value = true
  try {
    await api.patch(`/members/${encodeURIComponent(authStore.currentUser.email)}`, payload)
    authStore.updateUser({ name: payload.name, profileImageUrl: payload.profileImageUrl })
    profileForm.value.password = ''
    profileMsg.value = '저장되었습니다.'
  } catch (e) {
    profileMsg.value = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : '저장에 실패했습니다.')
  } finally {
    profileSaving.value = false
  }
}

async function fetchHistory() {
  if (!authStore.currentUser?.email) return
  try {
    const res = await api.get('/points/history')
    pointHistory.value = res.data
  } catch (e) {
    console.error('포인트 내역 조회 실패', e)
  }
}

// ===== 충전(결제) 내역 + 환불 =====
const payments = ref([])

// 결제 상태 뱃지. CHARGED는 정상 완료라 뱃지를 달지 않는다.
// PENDING/CANCELING은 토스 결과를 아직 모르는 상태로, 서버 대사 스케줄러가 확정한다.
const PAYMENT_STATUS_LABEL = {
  PENDING: '처리 중',
  CANCELING: '환불 처리 중',
  CANCELED: '취소됨',
  FAILED: '실패',
}

async function fetchPayments() {
  if (!authStore.currentUser?.email) return
  try {
    const res = await api.get('/payments')
    payments.value = res.data
  } catch (e) {
    console.error('결제 내역 조회 실패', e)
  }
}

async function cancelPayment(paymentId) {
  if (!confirm('이 충전을 취소(환불)하시겠습니까?\n충전했던 포인트가 회수되며, 이미 사용한 경우 환불이 불가합니다.')) return
  try {
    const res = await api.post(`/payments/${paymentId}/cancel`, { reason: '사용자 요청 환불' })
    // 202 = 서버가 토스 취소 결과를 확인하지 못한 상태. 실패로 안내하면 안 되고,
    // 대사 스케줄러가 토스에 재조회해 확정한다.
    alert(res.status === 202
      ? '환불 처리 중입니다. 잠시 후 충전 내역에서 확인해 주세요.'
      : '결제가 취소(환불)되었습니다.')
    // 잔액·내역 새로고침
    await Promise.all([fetchMemberInfo(), fetchHistory(), fetchPayments()])
  } catch (e) {
    const msg = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : '결제 취소에 실패했습니다.')
    alert(msg)
  }
}

async function fetchDonations() {
  if (!authStore.currentUser?.email) return
  try {
    const res = await api.get(`/members/${encodeURIComponent(authStore.currentUser.email)}/donations`)
    donations.value = res.data
  } catch (e) {
    console.error('기부 내역 조회 실패', e)
  }
}

onMounted(async () => {
  await fetchMemberInfo()
  await Promise.all([fetchHistory(), fetchDonations(), fetchBookmarks(), fetchPayments(), fetchMyCampaigns()])
})

// 빠른 충전 금액 단위
const presetAmounts = [10000, 30000, 50000, 100000]

// 버튼 클릭 시 해당 금액만큼 더하기
function addAmount(value) {
  const current = parseInt(chargeAmount.value) || 0
  chargeAmount.value = String(current + value)
  chargeMsg.value = ''
}

// 금액 초기화
function resetAmount() {
  chargeAmount.value = ''
  chargeMsg.value = ''
}

function charge() {
  const amount = parseInt(chargeAmount.value)
  if (!amount || amount < 1) {
    chargeMsg.value = '1 이상의 금액을 입력하세요.'
    return
  }
  if (!authStore.currentUser?.email) {
    chargeMsg.value = '로그인이 필요합니다.'
    return
  }
  // 토스 결제위젯 페이지로 이동
  router.push(`/payment/checkout?amount=${amount}`)
}

function logout() {
  authStore.logout()
  router.push('/login')
}

async function withdrawMember() {
  if (!confirm('정말로 회원을 탈퇴하시겠습니까?\n탈퇴 후에는 복구할 수 없습니다.')) return
  try {
    await api.delete(`/members/${encodeURIComponent(authStore.currentUser?.email)}`)
    authStore.logout()
    alert('회원 탈퇴가 완료되었습니다.')
    router.push('/')
  } catch (e) {
    const msg = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : '회원 탈퇴에 실패했습니다.')
    alert(msg)
  }
}
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[1200px] mx-auto px-6 py-12">
      <div class="grid lg:grid-cols-[240px_1fr] gap-8">
        <!-- Sidebar -->
        <div>
          <div class="rounded-2xl p-6 mb-6" style="background-color: var(--surface)">
            <div class="flex flex-col items-center mb-6">
              <div class="w-16 h-16 rounded-full mb-3 flex items-center justify-center overflow-hidden" style="background-color: var(--primary)">
                <img v-if="imageUrl(authStore.currentUser?.profileImageUrl)" :src="imageUrl(authStore.currentUser.profileImageUrl)" alt="" class="w-full h-full object-cover" />
                <User v-else class="w-8 h-8 text-white" />
              </div>
              <h3 class="font-semibold" style="color: var(--text-primary)">{{ authStore.currentUser?.name || '사용자' }}</h3>
              <p class="text-sm" style="color: var(--text-muted)">{{ authStore.currentUser?.email || '' }}</p>
            </div>

            <nav class="space-y-1">
              <button
                v-for="item in menuItems"
                :key="item.id"
                @click="activeMenu = item.id"
                class="w-full text-left px-4 py-3 rounded-lg transition-colors flex items-center gap-3 text-sm"
                :style="{
                  backgroundColor: activeMenu === item.id ? 'var(--accent)' : 'transparent',
                  color: activeMenu === item.id ? 'var(--primary)' : 'var(--text-secondary)',
                  borderLeft: activeMenu === item.id ? '3px solid var(--primary)' : '3px solid transparent',
                }"
              >
                <component :is="item.Icon" class="w-4 h-4" />
                {{ item.label }}
              </button>
              <button v-if="isAdmin" @click="router.push('/admin')"
                class="w-full text-left px-4 py-3 rounded-lg transition-colors flex items-center gap-3 text-sm font-medium"
                style="background-color: var(--accent); color: var(--primary)">
                <Shield class="w-4 h-4" />
                관리자 사이트
              </button>
              <button @click="logout" class="w-full text-left px-4 py-3 rounded-lg transition-colors flex items-center gap-3 text-sm" style="color: var(--text-secondary)">
                <LogOut class="w-4 h-4" />
                로그아웃
              </button>
              <button @click="withdrawMember" class="w-full text-left px-4 py-3 rounded-lg transition-colors flex items-center gap-3 text-sm" style="color: var(--danger)">
                <Trash2 class="w-4 h-4" />
                회원 탈퇴
              </button>
            </nav>
          </div>
        </div>

        <!-- Content -->
        <div>
          <!-- Profile Card -->
          <div class="rounded-2xl p-6 mb-6" style="background-color: var(--surface)">
            <div class="grid md:grid-cols-3 gap-6 mb-6">
              <div class="text-center">
                <div class="w-16 h-16 rounded-full mx-auto mb-2 flex items-center justify-center" style="background-color: var(--accent)">
                  <DollarSign class="w-8 h-8" style="color: var(--primary)" />
                </div>
                <div class="font-bold mb-1" style="color: var(--primary); font-size: 1.5rem">
                  {{ authStore.currentUser?.pointBalance?.toLocaleString() ?? 0 }}P
                </div>
                <div class="text-sm" style="color: var(--text-muted)">포인트 잔액</div>
              </div>
              <div class="text-center">
                <div class="font-bold mb-1" style="font-size: 1.5rem; color: var(--text-primary)">{{ donations.length }}건</div>
                <div class="text-sm" style="color: var(--text-muted)">총 기부 횟수</div>
              </div>
              <div class="text-center">
                <div class="font-bold mb-1" style="font-size: 1.5rem; color: var(--text-primary)">
                  ₩{{ totalDonationAmount.toLocaleString() }}
                </div>
                <div class="text-sm" style="color: var(--text-muted)">총 기부 금액</div>
              </div>
            </div>
            <div class="flex items-center justify-center gap-2 p-4 rounded-xl" style="background-color: var(--accent)">
              <Award class="w-5 h-5" style="color: var(--primary)" />
              <span class="font-semibold" style="color: var(--primary)">이번 달 랭킹 집계 중</span>
            </div>
          </div>

          <!-- 찜한 캠페인 탭 -->
          <div v-if="activeMenu === 'bookmarks'" class="rounded-2xl p-6" style="background-color: var(--surface)">
            <h2 class="font-bold mb-6" style="font-size: 1.5rem; color: var(--text-primary)">찜한 캠페인</h2>
            <div v-if="bookmarks.length === 0" class="text-center py-8" style="color: var(--text-muted)">
              아직 찜한 캠페인이 없습니다. 캠페인 상세에서 하트를 눌러보세요!
            </div>
            <div v-else class="grid md:grid-cols-2 gap-6">
              <CampaignCard
                v-for="c in bookmarks"
                :key="c.campaignId"
                :campaign-id="c.campaignId"
                :category="c.category"
                :days-left="daysLeftOf(c)"
                :org-name="c.orgName"
                :title="c.title"
                :current-amount="c.currentAmount"
                :goal-amount="c.goalAmount"
                :progress="progressOf(c)"
                :status="c.status"
                :thumbnail-url="c.thumbnailUrl"
              />
            </div>
          </div>

          <!-- 나의 캠페인 탭 (단체/관리자) -->
          <div v-if="activeMenu === 'myCampaigns'" class="rounded-2xl p-6" style="background-color: var(--surface)">
            <div class="flex items-center justify-between mb-6">
              <h2 class="font-bold" style="font-size: 1.5rem; color: var(--text-primary)">나의 캠페인</h2>
              <button @click="router.push('/campaigns/register')"
                class="px-4 py-2 rounded-md text-white text-sm font-medium" style="background-color: var(--primary)">
                + 캠페인 등록
              </button>
            </div>
            <div v-if="myCampaigns.length === 0" class="text-center py-8" style="color: var(--text-muted)">
              아직 등록한 캠페인이 없습니다. 오른쪽 위 버튼으로 캠페인을 등록해 보세요!
            </div>
            <div v-else class="grid md:grid-cols-2 gap-6">
              <CampaignCard
                v-for="c in myCampaigns"
                :key="c.campaignId"
                :campaign-id="c.campaignId"
                :category="c.category"
                :days-left="daysLeftOf(c)"
                :org-name="c.orgName"
                :title="c.title"
                :current-amount="c.currentAmount"
                :goal-amount="c.goalAmount"
                :progress="progressOf(c)"
                :status="c.status"
                :thumbnail-url="c.thumbnailUrl"
              />
            </div>
          </div>

          <!-- 기부 내역 탭 -->
          <div v-if="activeMenu === 'donations'" class="rounded-2xl p-6" style="background-color: var(--surface)">
            <h2 class="font-bold mb-6" style="font-size: 1.5rem; color: var(--text-primary)">나의 기부 내역</h2>
            <div v-if="donations.length === 0" class="text-center py-8" style="color: var(--text-muted)">기부 내역이 없습니다.</div>
            <table v-else class="w-full text-sm">
              <thead>
                <tr style="border-bottom: 1px solid var(--border-color)">
                  <th class="text-left py-3 px-2" style="color: var(--text-muted)">날짜</th>
                  <th class="text-left py-3 px-2" style="color: var(--text-muted)">캠페인명</th>
                  <th class="text-left py-3 px-2" style="color: var(--text-muted)">금액</th>
                  <th class="text-left py-3 px-2" style="color: var(--text-muted)">결제수단</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="donation in pagedDonations" :key="donation.donationId" style="border-bottom: 1px solid var(--border-color)">
                  <td class="py-3 px-2" style="color: var(--text-primary)">{{ donation.donatedAt?.slice(0, 10) }}</td>
                  <td class="py-3 px-2" style="color: var(--text-primary)">
                    <RouterLink :to="`/campaigns/${donation.campaignId}`" style="color: var(--primary)">
                      {{ donation.campaignTitle }}
                    </RouterLink>
                  </td>
                  <td class="py-3 px-2 font-semibold" style="color: var(--primary)">₩{{ donation.amount.toLocaleString() }}</td>
                  <td class="py-3 px-2" style="color: var(--text-primary)">포인트</td>
                </tr>
              </tbody>
            </table>

            <!-- 기부 내역 페이징 -->
            <div v-if="donations.length > 0 && donationTotalPages > 1" class="flex justify-center items-center gap-2 mt-6">
              <button @click="goToDonationPage(donationPage - 1)" :disabled="donationPage === 1"
                class="w-9 h-9 rounded-md border text-sm disabled:opacity-40"
                style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)">‹</button>
              <button v-for="p in donationTotalPages" :key="p" @click="goToDonationPage(p)"
                class="w-9 h-9 rounded-md border text-sm font-medium"
                :style="{
                  backgroundColor: donationPage === p ? 'var(--primary)' : 'var(--surface)',
                  color: donationPage === p ? 'white' : 'var(--text-primary)',
                  borderColor: donationPage === p ? 'var(--primary)' : 'var(--border-color)',
                }">{{ p }}</button>
              <button @click="goToDonationPage(donationPage + 1)" :disabled="donationPage === donationTotalPages"
                class="w-9 h-9 rounded-md border text-sm disabled:opacity-40"
                style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)">›</button>
            </div>
          </div>

          <!-- 포인트 탭 -->
          <div v-else-if="activeMenu === 'points'" class="rounded-2xl p-6" style="background-color: var(--surface)">
            <div class="flex justify-between items-center mb-6">
              <h2 class="font-bold" style="font-size: 1.5rem; color: var(--text-primary)">포인트 관리</h2>
              <button @click="showChargeModal = true" class="px-4 py-2 rounded-md text-white text-sm font-medium" style="background-color: var(--primary)">
                충전하기
              </button>
            </div>

            <div class="p-6 rounded-xl mb-6 text-center" style="background-color: var(--accent)">
              <div class="text-sm mb-2" style="color: var(--text-secondary)">현재 잔액</div>
              <div class="font-bold" style="font-size: 2.5rem; color: var(--primary)">
                {{ authStore.currentUser?.pointBalance?.toLocaleString() ?? 0 }}P
              </div>
            </div>

            <!-- 충전 내역 (환불 가능) -->
            <h3 class="font-semibold mb-4" style="color: var(--text-primary)">충전 내역</h3>
            <div class="space-y-3 mb-8">
              <p v-if="payments.length === 0" class="text-sm text-center py-4" style="color: var(--text-muted)">충전 내역이 없습니다.</p>
              <div v-for="p in payments" :key="p.paymentId"
                class="flex justify-between items-center p-4 rounded-xl"
                style="background-color: var(--surface-alt)">
                <div>
                  <div class="font-semibold mb-1 flex items-center gap-2" style="color: var(--text-primary)">
                    포인트 충전
                    <span v-if="PAYMENT_STATUS_LABEL[p.status]" class="text-xs px-2 py-0.5 rounded-full"
                      style="background-color: #f3f4f6; color: var(--text-muted)">{{ PAYMENT_STATUS_LABEL[p.status] }}</span>
                  </div>
                  <div class="text-sm" style="color: var(--text-muted)">{{ p.createdAt?.slice(0, 10) }}</div>
                </div>
                <div class="flex items-center gap-3">
                  <div class="font-bold" style="color: var(--secondary); font-size: 1.125rem">
                    +₩{{ p.amount.toLocaleString() }}
                  </div>
                  <button v-if="p.status === 'CHARGED'" @click="cancelPayment(p.paymentId)"
                    class="px-3 py-1.5 rounded-md border text-sm font-medium transition-colors"
                    style="border-color: var(--danger); color: var(--danger)">
                    환불
                  </button>
                </div>
              </div>
            </div>

            <h3 class="font-semibold mb-4" style="color: var(--text-primary)">포인트 내역</h3>
            <div class="space-y-3">
              <p v-if="pointHistory.length === 0" class="text-sm text-center py-4" style="color: var(--text-muted)">포인트 내역이 없습니다.</p>
              <div v-for="item in pointHistory" :key="item.historyId"
                class="flex justify-between items-center p-4 rounded-xl"
                style="background-color: var(--surface-alt)">
                <div>
                  <div class="font-semibold mb-1" style="color: var(--text-primary)">
                    {{ item.type === 'CHARGE' ? '포인트 충전' : item.type === 'REFUND' ? '충전 취소(환불)' : '기부 사용' }}
                  </div>
                  <div class="text-sm" style="color: var(--text-muted)">{{ item.createdAt?.slice(0, 10) }}</div>
                </div>
                <div class="font-bold"
                  :style="{ color: item.type === 'CHARGE' ? 'var(--secondary)' : 'var(--danger)', fontSize: '1.125rem' }">
                  {{ item.type === 'CHARGE' ? '+' : '-' }}₩{{ item.amount.toLocaleString() }}
                </div>
              </div>
            </div>
          </div>

          <!-- 내 정보 수정 탭 -->
          <div v-else-if="activeMenu === 'profile'" class="rounded-2xl p-6" style="background-color: var(--surface)">
            <h2 class="font-bold mb-6" style="font-size: 1.5rem; color: var(--text-primary)">내 정보 수정</h2>
            <div class="space-y-6 max-w-md">
              <ImageUploader v-model="profileForm.profileImageUrl" label="프로필 사진" shape="avatar" />

              <div>
                <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">이름</label>
                <input v-model="profileForm.name" type="text"
                  class="w-full h-11 px-4 rounded-md border outline-none"
                  style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
              </div>

              <div>
                <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">비밀번호 변경</label>
                <input v-model="profileForm.password" type="password" placeholder="변경할 경우에만 입력하세요"
                  class="w-full h-11 px-4 rounded-md border outline-none"
                  style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
              </div>

              <p v-if="profileMsg" class="text-sm" style="color: var(--text-secondary)">{{ profileMsg }}</p>

              <button @click="saveProfile" :disabled="profileSaving"
                class="h-11 px-6 rounded-md text-white font-medium disabled:opacity-50"
                style="background-color: var(--primary)">
                {{ profileSaving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 충전 모달 -->
    <div v-if="showChargeModal" class="fixed inset-0 flex items-center justify-center z-50" style="background-color: rgba(0,0,0,0.5)">
      <div class="w-full max-w-sm rounded-2xl p-6" style="background-color: var(--surface)">
        <h3 class="font-bold mb-4" style="font-size: 1.25rem; color: var(--text-primary)">포인트 충전</h3>

        <!-- 금액 입력 (현재 선택된 금액 표시) -->
        <div class="relative mb-3">
          <input v-model="chargeAmount" type="number" placeholder="충전할 금액 선택 또는 입력"
            class="w-full h-12 px-4 pr-10 rounded-md border outline-none text-right font-semibold"
            style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
          <span class="absolute right-4 top-1/2 -translate-y-1/2 text-sm" style="color: var(--text-muted)">원</span>
        </div>

        <!-- 빠른 금액 버튼 -->
        <div class="grid grid-cols-2 gap-2 mb-2">
          <button v-for="amt in presetAmounts" :key="amt" @click="addAmount(amt)"
            class="h-11 rounded-md border text-sm font-medium transition-colors"
            style="border-color: var(--border-color); color: var(--text-primary)">
            +{{ (amt / 10000) }}만원
          </button>
        </div>
        <button @click="resetAmount" class="w-full h-9 rounded-md text-sm mb-3" style="color: var(--text-muted)">
          초기화
        </button>

        <div v-if="chargeMsg" class="text-sm mb-3" style="color: var(--danger)">{{ chargeMsg }}</div>
        <div class="flex gap-3">
          <button @click="showChargeModal = false; resetAmount()" class="flex-1 h-12 rounded-md border font-medium"
            style="border-color: var(--border-color); color: var(--text-primary)">취소</button>
          <button @click="charge" class="flex-1 h-12 rounded-md text-white font-medium"
            style="background-color: var(--primary)">결제하기</button>
        </div>
      </div>
    </div>
  </div>
</template>
