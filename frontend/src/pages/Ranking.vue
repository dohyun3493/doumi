<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import api from '@/lib/api'
import { Trophy, Crown, Heart, Calendar, Loader2, AlertCircle } from 'lucide-vue-next'

const months = ref([])
const selectedMonth = ref('')

// 개인 기부 랭킹 관련 상태
const personalRankings = ref([])
const loadingDonors = ref(false)
const errorDonors = ref(null)

// 지난 6개월 연월 리스트 생성
const generateMonths = () => {
  const list = []
  const date = new Date()
  
  // 지난달부터 역순으로 6개월 생성
  date.setMonth(date.getMonth() - 1)
  for (let i = 0; i < 6; i++) {
    const y = date.getFullYear()
    const m = String(date.getMonth() + 1).padStart(2, '0')
    list.push({
      value: `${y}-${m}`,
      label: `${y}년 ${m}월`
    })
    date.setMonth(date.getMonth() - 1)
  }
  months.value = list
  if (list.length > 0) {
    selectedMonth.value = list[0].value
  }
}

// 개인 기부 랭킹 조회
const fetchPersonalRankings = async () => {
  if (!selectedMonth.value) return
  loadingDonors.value = true
  errorDonors.value = null
  try {
    const response = await api.get('/api/rankings', {
      params: {
        yearMonth: selectedMonth.value,
        limit: 10
      }
    })
    personalRankings.value = response.data
  } catch (err) {
    console.error('Failed to fetch personal rankings:', err)
    errorDonors.value = '랭킹 데이터를 불러오는데 실패했습니다.'
  } finally {
    loadingDonors.value = false
  }
}

// 등급 매칭 함수
function getDonorLevel(rank) {
  if (rank === 1) return '레전드'
  if (rank === 2) return '마스터'
  if (rank === 3) return '다이아몬드'
  if (rank <= 5) return '플래티넘'
  if (rank <= 7) return '골드'
  return '실버'
}

// 랭킹 뱃지 이모지
function getDonorBadge(rank) {
  if (rank === 1) return '🏆'
  if (rank === 2) return '🥈'
  if (rank === 3) return '🥉'
  return ''
}

// 탑 3 뱃지 배경색
function topDonorBadgeColor(rank) {
  if (rank === 1) return 'var(--primary)'
  if (rank === 2) return '#FFD700'
  return '#CD7F32'
}

// 개인 기부 랭킹 구분 (Top3 vs 나머지)
const top3Donors = computed(() => {
  return personalRankings.value.filter(d => d.rank <= 3)
})

const remainingDonors = computed(() => {
  return personalRankings.value.filter(d => d.rank > 3)
})

// 시각적 포디움 정렬 [2위, 1위, 3위]
const visualTop3Donors = computed(() => {
  const list = [...top3Donors.value].sort((a, b) => a.rank - b.rank)
  if (list.length === 3) {
    return [list[1], list[0], list[2]]
  }
  if (list.length === 2) {
    return [list[1], list[0]]
  }
  return list
})

// 선택 월 변경 감지
watch(selectedMonth, () => {
  fetchPersonalRankings()
})

onMounted(() => {
  generateMonths()
  fetchPersonalRankings()
})
</script>

<template>
  <div class="min-h-screen" style="background-color: var(--background)">
    <!-- Hero Section -->
    <div class="py-16" style="background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%)">
      <div class="max-w-[1200px] mx-auto px-6 text-center">
        <Trophy class="w-16 h-16 mx-auto mb-4 text-white animate-bounce" />
        <h1 class="text-3xl sm:text-4xl font-bold text-white mb-3">기부 랭킹</h1>
        <p class="text-white/90 text-lg">따뜻한 마음을 나눈 이달의 히어로들을 소개합니다</p>
      </div>
    </div>

    <div class="max-w-[1200px] mx-auto px-6 py-12">
      <!-- 연월 선택 바 -->
      <div class="flex items-center justify-between mb-8 flex-wrap gap-4 p-4 rounded-xl border" style="background-color: var(--surface); border-color: var(--border-color)">
        <div class="flex items-center gap-3">
          <Calendar class="w-5 h-5" style="color: var(--primary)" />
          <span class="text-sm font-semibold" style="color: var(--text-secondary)">집계 기준월 선택 :</span>
          <select
            v-model="selectedMonth"
            class="px-4 py-2 rounded-lg border focus:outline-none focus:ring-2 bg-white text-sm cursor-pointer transition-all"
            style="border-color: var(--border-color); color: var(--text-primary); focus:ring-color: var(--primary)"
          >
            <option v-for="m in months" :key="m.value" :value="m.value">
              {{ m.label }}
            </option>
          </select>
        </div>
        <div class="text-xs font-medium" style="color: var(--text-secondary)">
          * 매월 1일 새벽 2시에 지난달 누적 데이터가 일괄 집계됩니다.
        </div>
      </div>

      <!-- 로딩 상태 -->
      <div v-if="loadingDonors" class="py-24 text-center">
        <Loader2 class="w-12 h-12 animate-spin mx-auto mb-4" style="color: var(--primary)" />
        <p class="text-sm font-medium" style="color: var(--text-secondary)">기부 영웅들의 순위를 조회하고 있습니다...</p>
      </div>

      <!-- 에러 발생 상태 -->
      <div v-else-if="errorDonors" class="py-20 text-center border rounded-2xl" style="background-color: var(--surface); border-color: var(--border-color)">
        <AlertCircle class="w-14 h-14 mx-auto mb-4 text-red-500" />
        <h3 class="font-bold text-lg mb-2" style="color: var(--text-primary)">오류가 발생했습니다</h3>
        <p style="color: var(--text-secondary)">{{ errorDonors }}</p>
      </div>

      <!-- 데이터 없음 상태 -->
      <div v-else-if="personalRankings.length === 0" class="py-24 text-center border rounded-2xl" style="background-color: var(--surface); border-color: var(--border-color)">
        <Trophy class="w-16 h-16 mx-auto mb-4 text-gray-300" />
        <h3 class="font-bold text-lg mb-2" style="color: var(--text-primary)">랭킹 집계 내역이 없습니다</h3>
        <p class="text-sm" style="color: var(--text-secondary)">선택하신 월({{ selectedMonth }})의 기부 랭킹 배치 데이터가 아직 존재하지 않습니다.</p>
      </div>

      <!-- 정상 데이터 렌더링 -->
      <div v-else class="space-y-8">
        <!-- Top 3 포디움 디자인 -->
        <div class="grid md:grid-cols-3 gap-6 pt-6 items-end">
          <div
            v-for="donor in visualTop3Donors"
            :key="donor.rank"
            class="p-6 text-center relative overflow-hidden rounded-2xl border transition-all duration-300 hover:shadow-xl hover:-translate-y-1.5"
            :style="{
              backgroundColor: 'var(--surface)',
              borderColor: 'var(--border-color)',
              order: donor.rank === 1 ? 2 : (donor.rank === 2 ? 1 : 3),
              marginTop: donor.rank === 1 ? '0' : '2rem',
              boxShadow: donor.rank === 1 ? '0 10px 25px -5px rgba(255, 107, 74, 0.15)' : 'none'
            }"
          >
            <!-- 1위 왕관 데코레이션 -->
            <div v-if="donor.rank === 1" class="absolute -top-3 left-1/2 -translate-x-1/2 z-20">
              <Crown class="w-8 h-8 text-yellow-500 fill-yellow-500 animate-pulse" />
            </div>
            <div v-if="donor.rank === 1" class="absolute top-0 right-0 w-32 h-32 opacity-5" style="color: var(--primary)">
              <Crown class="w-full h-full" />
            </div>

            <div class="relative z-10">
              <div class="text-5xl mb-3">{{ getDonorBadge(donor.rank) }}</div>
              <span
                class="inline-block px-3 py-1 rounded-full text-xs font-semibold text-white mb-4 shadow-sm"
                :style="{ backgroundColor: topDonorBadgeColor(donor.rank) }"
              >
                {{ getDonorLevel(donor.rank) }}
              </span>
              <div
                class="w-20 h-20 rounded-full mx-auto mb-4 flex items-center justify-center shadow-inner transition-transform hover:scale-110"
                style="background-color: var(--accent)"
              >
                <Heart class="w-10 h-10" style="color: var(--primary)" />
              </div>
              <h3 class="font-bold text-xl mb-2" style="color: var(--text-primary)">{{ donor.memberName }}</h3>
              <p class="text-2xl font-bold mb-1" style="color: var(--primary)">{{ donor.totalAmount.toLocaleString() }}원</p>
              <p class="text-xs font-medium" style="color: var(--text-secondary)">집계 기준월 누적 기부액</p>
            </div>
          </div>
        </div>

        <!-- 4위 ~ 10위 리스트 디자인 -->
        <div v-if="remainingDonors.length > 0" class="rounded-2xl divide-y border shadow-sm overflow-hidden" style="background-color: var(--surface); border-color: var(--border-color)">
          <div
            v-for="donor in remainingDonors"
            :key="donor.rank"
            class="p-5 flex items-center justify-between hover:bg-gray-50/50 transition-colors"
          >
            <div class="flex items-center gap-5">
              <div
                class="w-10 h-10 rounded-full flex items-center justify-center font-bold text-sm shadow-sm"
                style="background-color: var(--accent); color: var(--text-secondary)"
              >
                {{ donor.rank }}
              </div>
              <div
                class="w-12 h-12 rounded-full flex items-center justify-center font-semibold text-white shadow-sm text-sm"
                style="background-color: var(--primary); opacity: 0.85;"
              >
                {{ donor.memberName[0] }}
              </div>
              <div>
                <p class="font-bold text-base" style="color: var(--text-primary)">{{ donor.memberName }}</p>
                <p class="text-xs" style="color: var(--text-secondary)">기부 랭커 회원</p>
              </div>
            </div>
            <div class="text-right">
              <p class="font-bold text-lg" style="color: var(--primary)">{{ donor.totalAmount.toLocaleString() }}원</p>
              <span
                class="text-xs border rounded-full px-3 py-0.5 bg-gray-50/80 font-medium"
                style="border-color: var(--border-color); color: var(--text-secondary)"
              >
                {{ getDonorLevel(donor.rank) }}
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
