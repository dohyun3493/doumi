<script setup>
import { ref, computed, onMounted } from 'vue'
import { ArrowRight, Loader2 } from 'lucide-vue-next'
import CampaignCard from '@/components/CampaignCard.vue'
import StatCard from '@/components/StatCard.vue'
import api from '@/lib/api'

const activeTab = ref('전체')
const campaigns = ref([])
const stats = ref({
  totalDonationAmount: 0,
  activeCampaigns: 0,
  totalMembers: 0,
  monthlyDonors: 0,
  completedCampaigns: 0,
})

const tabs = ['전체', '사회복지', '교육', '보건', '환경']

// 금액을 억/만 단위로 축약 (예: 120000000 → ₩1.2억)
function formatAmount(value) {
  if (value >= 100000000) return `₩${(value / 100000000).toFixed(1)}억`
  if (value >= 10000) return `₩${Math.floor(value / 10000).toLocaleString()}만`
  return `₩${value.toLocaleString()}`
}

const processedCampaigns = computed(() =>
  campaigns.value.map(c => {
    const progress = c.goalAmount > 0
      ? Math.min(Math.round((c.currentAmount / c.goalAmount) * 100), 100)
      : 0
    const today = new Date()
    const end = new Date(c.endDate)
    const daysLeft = Math.max(0, Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)))
    return { ...c, progress, daysLeft }
  })
)

// 상태 우선순위: 모집중(앞) → 모집예정(중간) → 모집완료/사용완료(맨 뒤)
const statusRank = (c) => {
  if (c.status === '모집중') return 0
  if (c.status === '모집예정') return 1
  return 2
}

const filteredCampaigns = computed(() => {
  const list = activeTab.value === '전체'
    ? processedCampaigns.value
    : processedCampaigns.value.filter(c => c.category && c.category.includes(activeTab.value))
  // 상태 우선순위 정렬, 같은 그룹 안에서는 최신순(campaignId 내림차순)
  return [...list].sort((a, b) =>
    statusRank(a) !== statusRank(b) ? statusRank(a) - statusRank(b) : b.campaignId - a.campaignId
  )
})

// 기부 랭킹 관련 데이터 및 통신 상태
const topDonors = ref([])
const loadingDonors = ref(false)

// 지난달 연월 문자열(yyyy-MM) 계산
const getPreviousMonth = () => {
  const date = new Date()
  date.setMonth(date.getMonth() - 1)
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  return `${y}-${m}`
}

// 홈 화면에 표시할 지난달 연월 레이블(예: 2026년 05월)
const prevMonthLabel = computed(() => {
  const date = new Date()
  date.setMonth(date.getMonth() - 1)
  return `${date.getFullYear()}년 ${String(date.getMonth() + 1).padStart(2, '0')}월`
})

const fetchTopDonors = async () => {
  const prevMonth = getPreviousMonth()
  loadingDonors.value = true
  try {
    const res = await api.get('/api/rankings', {
      params: {
        yearMonth: prevMonth,
        limit: 10
      }
    })
    topDonors.value = res.data
  } catch (err) {
    console.error('Failed to fetch top donors on Home page:', err)
  } finally {
    loadingDonors.value = false
  }
}

function rankColor(rank) {
  if (rank === 1) return '#FFD700'
  if (rank === 2) return '#C0C0C0'
  if (rank === 3) return '#CD7F32'
  return 'var(--surface-alt)'
}

onMounted(async () => {
  try {
    const [campaignsRes, statsRes] = await Promise.all([
      api.get('/api/campaigns'),
      api.get('/api/stats'),
      fetchTopDonors()
    ])
    campaigns.value = campaignsRes.data
    stats.value = statsRes.data
  } catch (e) {
    console.error('데이터 로드 실패', e)
  }
})
</script>

<template>
  <div>
    <!-- Hero Section -->
    <section class="relative py-20 overflow-hidden" style="background-color: var(--background)">
      <div class="absolute top-0 right-0 w-96 h-96 rounded-full blur-3xl opacity-20"
        style="background: radial-gradient(circle, var(--primary) 0%, transparent 70%)" />
      <div class="max-w-[1200px] mx-auto px-6 relative z-10">
        <div class="grid md:grid-cols-2 gap-12 items-center">
          <div>
            <div class="inline-flex items-center gap-2 px-4 py-2 rounded-full mb-6"
              style="background-color: var(--accent); color: var(--primary); font-size: 0.75rem; font-weight: 600">
              💛 투명한 기부 플랫폼
            </div>
            <h1 class="mb-6" style="font-size: 3rem; font-weight: 700; line-height: 1.2; color: var(--text-primary)">
              작은 마음이 모여<br />세상을 바꿉니다
            </h1>
            <p class="mb-8" style="font-size: 1rem; line-height: 1.6; color: var(--text-secondary)">
              쉽고 투명한 기부로 더 좋은 세상을 만들어요.<br />오늘 첫 기부를 시작해보세요.
            </p>
            <div class="flex flex-wrap gap-4 mb-12">
              <RouterLink to="/campaigns">
                <button class="h-14 px-8 rounded-lg text-white" style="background-color: var(--primary); font-size: 1rem">
                  지금 기부하기
                </button>
              </RouterLink>
              <RouterLink to="/campaigns">
                <button class="h-14 px-8 rounded-lg border" style="border-color: var(--border-color); color: var(--text-primary); font-size: 1rem">
                  캠페인 둘러보기
                </button>
              </RouterLink>
            </div>
            <div class="grid grid-cols-3 gap-6">
              <div>
                <div class="font-bold mb-1" style="font-size: 1.5rem; color: var(--primary)">{{ formatAmount(stats.totalDonationAmount) }}</div>
                <div class="text-xs" style="color: var(--text-muted)">총 기부금</div>
              </div>
              <div>
                <div class="font-bold mb-1" style="font-size: 1.5rem; color: var(--primary)">{{ stats.activeCampaigns.toLocaleString() }}개</div>
                <div class="text-xs" style="color: var(--text-muted)">진행 캠페인</div>
              </div>
              <div>
                <div class="font-bold mb-1" style="font-size: 1.5rem; color: var(--primary)">{{ stats.totalMembers.toLocaleString() }}명</div>
                <div class="text-xs" style="color: var(--text-muted)">기부 회원</div>
              </div>
            </div>
          </div>

          <div class="relative hidden md:block">
            <div class="relative">
              <div class="absolute top-0 right-0 w-72 transform rotate-3" style="z-index: 2">
                <div class="rounded-xl p-4" style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.1)">
                  <div class="w-full h-32 rounded-lg mb-3 bg-gradient-to-br from-orange-200 to-orange-400" />
                  <div class="h-2 rounded-full mb-2" style="background-color: var(--border-color)">
                    <div class="h-full rounded-full" style="background-color: var(--primary); width: 68%" />
                  </div>
                  <div class="text-sm font-bold" style="color: var(--primary)">68% 달성</div>
                </div>
              </div>
              <div class="absolute top-16 left-0 w-64 transform -rotate-2" style="z-index: 1">
                <div class="rounded-xl p-4" style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.08)">
                  <div class="w-full h-28 rounded-lg mb-3 bg-gradient-to-br from-green-200 to-green-400" />
                  <div class="h-2 rounded-full mb-2" style="background-color: var(--border-color)">
                    <div class="h-full rounded-full" style="background-color: var(--secondary); width: 82%" />
                  </div>
                  <div class="text-sm font-bold" style="color: var(--secondary)">82% 달성</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- Stats Bar -->
    <section class="py-8" style="background-color: var(--text-primary)">
      <div class="max-w-[1200px] mx-auto px-6">
        <div class="grid grid-cols-2 md:grid-cols-4 gap-8">
          <StatCard label="총 누적 기부금" :value="formatAmount(stats.totalDonationAmount)" :isDark="true" />
          <StatCard label="이번 달 기부자 수" :value="`${stats.monthlyDonors.toLocaleString()}명`" :isDark="true" />
          <StatCard label="완료된 캠페인" :value="`${stats.completedCampaigns.toLocaleString()}개`" :isDark="true" />
          <StatCard label="진행 중인 캠페인" :value="`${stats.activeCampaigns.toLocaleString()}개`" :isDark="true" />
        </div>
      </div>
    </section>

    <!-- Active Campaigns -->
    <section class="py-16" style="background-color: var(--background)">
      <div class="max-w-[1200px] mx-auto px-6">
        <div class="flex justify-between items-center mb-8">
          <h2 class="font-bold" style="font-size: 2rem; color: var(--text-primary)">지금 진행 중인 캠페인</h2>
          <RouterLink to="/campaigns" class="flex items-center gap-1 transition-colors" style="color: var(--primary)">
            전체 보기 <ArrowRight class="w-4 h-4" />
          </RouterLink>
        </div>

        <div class="mb-8 flex gap-2 border-b" style="border-color: var(--border-color)">
          <button
            v-for="tab in tabs"
            :key="tab"
            @click="activeTab = tab"
            class="rounded-full px-6 py-2 text-sm transition-colors"
            :style="{
              backgroundColor: activeTab === tab ? 'var(--primary)' : 'transparent',
              color: activeTab === tab ? 'white' : 'var(--text-secondary)',
            }"
          >
            {{ tab }}
          </button>
        </div>

        <div v-if="filteredCampaigns.length > 0" class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
          <CampaignCard
            v-for="campaign in filteredCampaigns.slice(0, 6)"
            :key="campaign.campaignId"
            v-bind="campaign"
          />
        </div>
        <div v-else class="text-center py-12" style="color: var(--text-muted)">
          캠페인을 불러오는 중입니다...
        </div>
      </div>
    </section>

    <!-- Monthly Ranking -->
    <section class="py-16" style="background-color: var(--surface-alt)">
      <div class="max-w-[1200px] mx-auto px-6">
        <h2 class="font-bold mb-8 flex items-center gap-3" style="font-size: 2rem; color: var(--text-primary)">
          <span>{{ prevMonthLabel }} 기부 TOP 10</span>
          <Trophy class="w-7 h-7 text-yellow-500 fill-yellow-500 animate-pulse" />
        </h2>

        <!-- 로딩 상태 -->
        <div v-if="loadingDonors" class="py-12 text-center">
          <Loader2 class="w-10 h-10 animate-spin mx-auto mb-3" style="color: var(--primary)" />
          <p class="text-sm" style="color: var(--text-secondary)">기부 랭킹 데이터를 로드하고 있습니다...</p>
        </div>

        <!-- 데이터 없음 상태 -->
        <div v-else-if="topDonors.length === 0" class="text-center py-12 border border-dashed rounded-2xl bg-white" style="border-color: var(--border-color)">
          <Trophy class="w-12 h-12 mx-auto mb-3 text-gray-300" />
          <p class="text-sm font-medium" style="color: var(--text-secondary)">해당 월의 기부 랭킹 집계 데이터가 없습니다.</p>
        </div>

        <!-- 기부 랭킹 리스트 -->
        <div v-else class="grid gap-4">
          <div
            v-for="donor in topDonors"
            :key="donor.rank"
            class="flex items-center gap-4 p-4 rounded-xl transition-all hover:shadow-md border"
            :style="{ 
              backgroundColor: donor.rank <= 3 ? 'var(--accent)' : 'var(--surface)',
              borderColor: donor.rank <= 3 ? 'rgba(255, 107, 74, 0.15)' : 'var(--border-color)'
            }"
          >
            <div
              class="w-12 h-12 rounded-full flex items-center justify-center font-bold text-sm shadow-sm"
              :style="{ backgroundColor: rankColor(donor.rank), color: donor.rank <= 3 ? 'white' : 'var(--text-secondary)' }"
            >
              {{ donor.rank }}
            </div>
            <div class="flex-1">
              <div class="font-bold text-base" style="color: var(--text-primary)">{{ donor.memberName }}</div>
              <div class="text-xs" style="color: var(--text-secondary)">기부 히어로 누적 서포터</div>
            </div>
            <div class="font-bold" style="color: var(--primary); font-size: 1.125rem">
              {{ donor.totalAmount.toLocaleString() }}원
            </div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
