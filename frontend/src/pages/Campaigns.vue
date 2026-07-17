<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { Search, Grid3x3, List, SlidersHorizontal, RotateCcw } from 'lucide-vue-next'
import CampaignCard from '@/components/CampaignCard.vue'
import placeholderImg from '@/assets/placeholder.jpg'
import { imageUrl } from '@/lib/utils'
import api from '@/lib/api'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()

const viewMode = ref('grid')
const campaigns = ref([])

// 검색 및 필터 상태값들
const searchKeyword = ref('')
const selectedCategory = ref('all')
const selectedRegion = ref('all')
const selectedStatus = ref('all')
const selectedSort = ref('latest')

const showAdvancedFilters = ref(false)

// 진행 지역 옵션 (캠페인 등록 폼과 동일한 17개 시·도)
const regions = ['서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종', '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주']

// API 호출 및 검색 실행
const fetchCampaigns = async () => {
  try {
    const params = {
      category: selectedCategory.value,
      region: selectedRegion.value,
      status: selectedStatus.value,
      keyword: searchKeyword.value
    }
    const response = await api.get('/api/campaigns', { params })
    campaigns.value = response.data || []
  } catch (error) {
    console.error('캠페인 데이터 조회 실패:', error)
    campaigns.value = []
  }
}

// 필터 조건 리셋
const resetFilters = () => {
  searchKeyword.value = ''
  selectedCategory.value = 'all'
  selectedRegion.value = 'all'
  selectedStatus.value = 'all'
  selectedSort.value = 'latest'
  fetchCampaigns()
}

// progress, daysLeft 계산 및 클라이언트 정렬 적용
const processedCampaigns = computed(() => {
  const mapped = campaigns.value.map(c => {
    const progress = c.goalAmount > 0
      ? Math.min(Math.round((c.currentAmount / c.goalAmount) * 100), 100)
      : 0
    const today = new Date()
    const end = new Date(c.endDate)
    const daysLeft = Math.max(0, Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24)))
    return { ...c, progress, daysLeft }
  })

  // 상태 우선순위: 모집중(앞) → 모집예정(중간) → 모집완료/사용완료(맨 뒤)
  const statusRank = (c) => {
    if (c.status === '모집중') return 0
    if (c.status === '모집예정') return 1
    return 2   // 모집완료 / 사용완료 등 종료된 캠페인
  }

  // 선택한 정렬 기준 비교 함수
  const sortFns = {
    latest: (a, b) => b.campaignId - a.campaignId,
    popular: (a, b) => b.currentAmount - a.currentAmount,
    deadline: (a, b) => a.daysLeft - b.daysLeft,
    achievement: (a, b) => b.progress - a.progress,
  }
  const sortFn = sortFns[selectedSort.value]

  return [...mapped].sort((a, b) => {
    // 1) 상태 우선순위 (모집중 → 모집예정 → 종료)
    if (statusRank(a) !== statusRank(b)) return statusRank(a) - statusRank(b)
    // 2) 같은 그룹 안에서는 선택한 정렬 적용
    return sortFn ? sortFn(a, b) : 0
  })
})

// ===== 페이징 =====
const currentPage = ref(1)
const pageSize = 12

// 현재 페이지에 보여줄 캠페인만 자르기
const pagedCampaigns = computed(() => {
  const start = (currentPage.value - 1) * pageSize
  return processedCampaigns.value.slice(start, start + pageSize)
})

// 총 페이지 수
const totalPages = computed(() =>
  Math.max(1, Math.ceil(processedCampaigns.value.length / pageSize))
)

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

// 필터/검색/정렬이 바뀌면 1페이지로 리셋
watch([processedCampaigns], () => {
  if (currentPage.value > totalPages.value) currentPage.value = 1
})

// 즉시 필터링 적용 항목 감시
watch([selectedCategory, selectedRegion, selectedStatus, selectedSort], () => {
  currentPage.value = 1
  fetchCampaigns()
})

const onSearch = () => {
  currentPage.value = 1
  fetchCampaigns()
}

onMounted(() => {
  fetchCampaigns()
})
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[1200px] mx-auto px-6 py-12">
      <!-- Header -->
      <div class="mb-8">
        <div class="flex justify-between items-center mb-2">
          <h1 class="font-bold" style="font-size: 2rem; color: var(--text-primary)">캠페인 목록</h1>
          <RouterLink v-if="['ORGANIZATION', 'ADMIN'].includes(authStore.currentUser?.memberType)" to="/campaigns/register">
            <button class="px-4 py-2 rounded-md text-white text-sm font-medium" style="background-color: var(--primary)">
              + 캠페인 등록
            </button>
          </RouterLink>
        </div>
        <p style="color: var(--text-secondary)">
          현재 <span style="color: var(--primary); font-weight: 600">{{ processedCampaigns.length }}개</span>의 캠페인이 진행 중입니다
        </p>
      </div>

      <!-- Search & Filters -->
      <div class="mb-8 space-y-4">
        <!-- Search Bar -->
        <div class="flex gap-3">
          <div class="relative flex-1">
            <Search
              class="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5"
              style="color: var(--text-muted)"
            />
            <input
              type="text"
              v-model="searchKeyword"
              @keyup.enter="onSearch"
              placeholder="캠페인 이름 또는 단체명을 입력하고 Enter를 누르세요"
              class="w-full pl-12 pr-4 h-12 rounded-lg border outline-none transition-all focus:ring-2 focus:ring-primary focus:border-transparent"
              style="background-color: var(--surface); border-color: var(--border-color); color: var(--text-primary)"
            />
          </div>
          <button
            @click="showAdvancedFilters = !showAdvancedFilters"
            class="h-12 px-4 rounded-lg border flex items-center gap-2 font-medium transition-all hover:bg-neutral-50 active:scale-95"
            :style="{
              backgroundColor: showAdvancedFilters ? 'var(--accent)' : 'var(--surface)',
              borderColor: 'var(--border-color)',
              color: 'var(--text-primary)'
            }"
          >
            <SlidersHorizontal class="w-5 h-5" style="color: var(--primary)" />
            상세 검색
          </button>
          <button
            @click="resetFilters"
            class="h-12 w-12 rounded-lg border flex items-center justify-center transition-all hover:bg-neutral-50 active:scale-95"
            style="background-color: var(--surface); border-color: var(--border-color)"
            title="필터 초기화"
          >
            <RotateCcw class="w-5 h-5" style="color: var(--text-muted)" />
          </button>
        </div>

        <!-- Advanced Filters Panel (Accordion) -->
        <div
          v-if="showAdvancedFilters"
          class="p-6 rounded-xl border grid grid-cols-1 md:grid-cols-3 gap-6 transition-all duration-300 ease-in-out"
          style="background-color: var(--surface); border-color: var(--border-color)"
        >
          <div>
            <label class="text-xs font-semibold mb-2 block" style="color: var(--text-muted)">분야별 카테고리</label>
            <select
              v-model="selectedCategory"
              class="w-full h-10 px-3 rounded-md border outline-none text-sm"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            >
              <option value="all">전체 분야</option>
              <option value="사회복지">사회복지</option>
              <option value="교육">교육·학술</option>
              <option value="보건">보건·의료</option>
              <option value="환경">환경·생태</option>
              <option value="아동">아동·청소년</option>
              <option value="문화">문화·예술</option>
              <option value="재난">재난·구호</option>
              <option value="동물">동물보호</option>
              <option value="기타">기타</option>
            </select>
          </div>

          <div>
            <label class="text-xs font-semibold mb-2 block" style="color: var(--text-muted)">진행 지역</label>
            <select
              v-model="selectedRegion"
              class="w-full h-10 px-3 rounded-md border outline-none text-sm"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            >
              <option value="all">전국 지역</option>
              <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
            </select>
          </div>

          <div>
            <label class="text-xs font-semibold mb-2 block" style="color: var(--text-muted)">모집 상태</label>
            <select
              v-model="selectedStatus"
              class="w-full h-10 px-3 rounded-md border outline-none text-sm"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            >
              <option value="all">전체 상태</option>
              <option value="모집중">모집중</option>
              <option value="모집예정">모집예정</option>
              <option value="모집완료">모집완료</option>
              <option value="사용완료">사용완료</option>
            </select>
          </div>
        </div>

        <!-- Filters Row & View Toggle -->
        <div class="flex flex-wrap gap-4 items-center justify-between pt-2">
          <div class="flex gap-3">
            <!-- 기본 영역에서도 정렬 셀렉트는 상시 노출 -->
            <select
              v-model="selectedSort"
              class="w-40 h-10 px-3 rounded-md border outline-none text-sm"
              style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)"
            >
              <option value="latest">최신 등록순</option>
              <option value="popular">인기 모금순</option>
              <option value="deadline">마감 임박순</option>
              <option value="achievement">달성률순</option>
            </select>
          </div>

          <!-- View Toggle -->
          <div class="flex gap-2">
            <button
              @click="viewMode = 'grid'"
              class="w-9 h-9 flex items-center justify-center rounded-md border transition-colors cursor-pointer"
              :style="{
                backgroundColor: viewMode === 'grid' ? 'var(--primary)' : 'transparent',
                color: viewMode === 'grid' ? 'white' : 'var(--text-secondary)',
                borderColor: 'var(--border-color)',
              }"
            >
              <Grid3x3 class="w-4 h-4" />
            </button>
            <button
              @click="viewMode = 'list'"
              class="w-9 h-9 flex items-center justify-center rounded-md border transition-colors cursor-pointer"
              :style="{
                backgroundColor: viewMode === 'list' ? 'var(--primary)' : 'transparent',
                color: viewMode === 'list' ? 'white' : 'var(--text-secondary)',
                borderColor: 'var(--border-color)',
              }"
            >
              <List class="w-4 h-4" />
            </button>
          </div>
        </div>

        <!-- Result Count -->
        <p class="text-sm pt-2" style="color: var(--text-muted)">
          총 <span class="font-semibold" style="color: var(--text-primary)">{{ processedCampaigns.length }}개</span>의 캠페인 검색됨
        </p>
      </div>

      <!-- Campaign Grid/List -->
      <div v-if="processedCampaigns.length === 0" class="py-24 text-center rounded-2xl border" style="background-color: var(--surface); border-color: var(--border-color)">
        <p style="color: var(--text-muted)">부합하는 기부 캠페인이 존재하지 않습니다.</p>
      </div>

      <div v-else-if="viewMode === 'grid'" class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        <CampaignCard
          v-for="campaign in pagedCampaigns"
          :key="campaign.campaignId"
          v-bind="campaign"
        />
      </div>

      <div v-else class="space-y-4">
        <RouterLink
          v-for="campaign in pagedCampaigns"
          :key="campaign.campaignId"
          :to="`/campaigns/${campaign.campaignId}`"
          class="flex gap-4 p-4 rounded-xl transition-all hover:shadow-md border hover:border-primary block"
          :style="{
            backgroundColor: 'var(--surface)',
            borderColor: 'var(--border-color)',
            opacity: ['모집완료', '사용완료'].includes(campaign.status) ? 0.6 : 1,
          }"
        >
          <img
            :src="imageUrl(campaign.thumbnailUrl) || placeholderImg"
            :alt="campaign.title"
            class="w-32 h-24 rounded-lg object-cover"
            :style="{ filter: ['모집완료', '사용완료'].includes(campaign.status) ? 'grayscale(100%) brightness(0.7)' : 'none' }"
          />
          <div class="flex-1">
            <p class="text-xs mb-1" style="color: var(--text-muted)">
              {{ campaign.category }} · {{ campaign.orgName }}
            </p>
            <h3 class="font-semibold mb-2" style="color: var(--text-primary)">{{ campaign.title }}</h3>
            <div class="flex items-center gap-4 flex-wrap">
              <span class="font-bold" style="color: var(--primary)">{{ campaign.progress }}% 달성</span>
              <span class="text-sm" style="color: var(--text-muted)">
                ₩{{ campaign.currentAmount.toLocaleString() }} / ₩{{ campaign.goalAmount.toLocaleString() }}
              </span>
              <span class="text-sm" style="color: var(--text-muted)">D-{{ campaign.daysLeft }}</span>
              <span class="text-xs px-2 py-0.5 rounded-full font-medium" :style="{
                backgroundColor: campaign.status === '모집중' ? 'var(--accent)' : '#f3f4f6',
                color: campaign.status === '모집중' ? 'var(--primary)' : 'var(--text-muted)'
              }">{{ campaign.status }}</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <!-- Pagination -->
      <div v-if="processedCampaigns.length > 0 && totalPages > 1" class="flex justify-center items-center gap-2 mt-12">
        <!-- 이전 -->
        <button
          @click="goToPage(currentPage - 1)"
          :disabled="currentPage === 1"
          class="w-10 h-10 rounded-md border text-sm font-medium disabled:opacity-40 transition-colors"
          style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)"
        >
          ‹
        </button>

        <!-- 페이지 번호 -->
        <button
          v-for="page in totalPages"
          :key="page"
          @click="goToPage(page)"
          class="w-10 h-10 rounded-md border text-sm font-medium transition-colors"
          :style="{
            backgroundColor: currentPage === page ? 'var(--primary)' : 'var(--surface)',
            color: currentPage === page ? 'white' : 'var(--text-primary)',
            borderColor: currentPage === page ? 'var(--primary)' : 'var(--border-color)',
          }"
        >
          {{ page }}
        </button>

        <!-- 다음 -->
        <button
          @click="goToPage(currentPage + 1)"
          :disabled="currentPage === totalPages"
          class="w-10 h-10 rounded-md border text-sm font-medium disabled:opacity-40 transition-colors"
          style="border-color: var(--border-color); color: var(--text-secondary); background-color: var(--surface)"
        >
          ›
        </button>
      </div>
    </div>
  </div>
</template>
