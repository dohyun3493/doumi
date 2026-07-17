<script setup>
import { ref, onMounted } from 'vue'
import { Heart, Target, Users, Shield, TrendingUp, Award } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'

const authStore = useAuthStore()

function formatAmount(value) {
  if (value >= 100000000) return `${(value / 100000000).toFixed(1)}억원`
  if (value >= 10000) return `${Math.floor(value / 10000).toLocaleString()}만원`
  return `${value.toLocaleString()}원`
}

const features = [
  { icon: Shield, title: '투명한 기부', description: '모든 기부 내역과 사용 내역을 실시간으로 확인할 수 있습니다.' },
  { icon: Users, title: '다양한 캠페인', description: '교육, 환경, 의료, 동물 등 다양한 분야의 캠페인이 진행됩니다.' },
  { icon: Heart, title: '쉬운 참여', description: '포인트로 간편하게 기부하고, 커뮤니티에서 소통할 수 있습니다.' },
  { icon: Award, title: '기부 인증', description: '기부 참여 이력을 인증하고, 랭킹에서 확인할 수 있습니다.' },
]

const stats = ref([
  { label: '누적 기부금', value: '-', icon: TrendingUp },
  { label: '진행 캠페인', value: '-', icon: Target },
  { label: '참여 회원', value: '-', icon: Users },
  { label: '완료 캠페인', value: '-', icon: Award },
])

onMounted(async () => {
  try {
    const res = await api.get('/api/stats')
    const d = res.data
    stats.value = [
      { label: '누적 기부금', value: formatAmount(d.totalDonationAmount), icon: TrendingUp },
      { label: '진행 캠페인', value: `${d.activeCampaigns.toLocaleString()}개`, icon: Target },
      { label: '참여 회원', value: `${d.totalMembers.toLocaleString()}명`, icon: Users },
      { label: '완료 캠페인', value: `${d.completedCampaigns.toLocaleString()}개`, icon: Award },
    ]
  } catch (e) {
    console.error('통계 로드 실패', e)
  }
})

const values = [
  { title: '투명성', description: '모든 기부금의 사용 내역을 투명하게 공개하여 신뢰를 구축합니다.' },
  { title: '접근성', description: '누구나 쉽게 참여할 수 있는 플랫폼을 만들어 나눔 문화를 확산합니다.' },
  { title: '연결성', description: '기부자와 수혜자, 단체를 연결하여 더 나은 세상을 만듭니다.' },
]
</script>

<template>
  <div class="min-h-screen" style="background-color: var(--background)">
    <!-- Hero Section -->
    <div class="py-20" style="background: linear-gradient(135deg, var(--primary) 0%, var(--secondary) 100%)">
      <div class="max-w-[1200px] mx-auto px-6 text-center">
        <Heart class="w-16 h-16 mx-auto mb-6 text-white" fill="white" />
        <h1 class="text-3xl sm:text-5xl font-bold text-white mb-4">도우미</h1>
        <p class="text-xl text-white/90 mb-2">따뜻한 마음을 나누는 투명한 기부 플랫폼</p>
        <p class="text-lg text-white/80">누구나 쉽게 참여하고, 모두가 신뢰할 수 있는 나눔 문화를 만들어갑니다</p>
      </div>
    </div>

    <!-- Stats Section -->
    <div class="max-w-[1200px] mx-auto px-6 -mt-12">
      <div class="grid grid-cols-2 md:grid-cols-4 gap-6">
        <div
          v-for="(stat, index) in stats"
          :key="index"
          class="p-6 text-center shadow-lg rounded-xl border"
          style="background-color: var(--surface); border-color: var(--border-color)"
        >
          <component :is="stat.icon" class="w-10 h-10 mx-auto mb-3" style="color: var(--primary)" />
          <p class="text-3xl font-bold mb-1" style="color: var(--text-primary)">{{ stat.value }}</p>
          <p class="text-sm" style="color: var(--text-secondary)">{{ stat.label }}</p>
        </div>
      </div>
    </div>

    <!-- Features Section -->
    <div class="max-w-[1200px] mx-auto px-6 py-20">
      <div class="text-center mb-12">
        <h2 class="text-3xl font-bold mb-3" style="color: var(--text-primary)">도우미의 특징</h2>
        <p style="color: var(--text-secondary)">투명하고 편리한 기부 경험을 제공합니다</p>
      </div>
      <div class="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div
          v-for="(feature, index) in features"
          :key="index"
          class="p-6 text-center hover:shadow-lg transition-shadow rounded-xl border"
          style="background-color: var(--surface); border-color: var(--border-color)"
        >
          <div
            class="w-14 h-14 rounded-full mx-auto mb-4 flex items-center justify-center"
            style="background-color: var(--accent)"
          >
            <component :is="feature.icon" class="w-7 h-7" style="color: var(--primary)" />
          </div>
          <h3 class="font-bold text-lg mb-2" style="color: var(--text-primary)">{{ feature.title }}</h3>
          <p class="text-sm" style="color: var(--text-secondary)">{{ feature.description }}</p>
        </div>
      </div>
    </div>

    <!-- Values Section -->
    <div class="py-16" style="background-color: var(--surface-alt)">
      <div class="max-w-[1200px] mx-auto px-6">
        <div class="text-center mb-12">
          <h2 class="text-3xl font-bold mb-3" style="color: var(--text-primary)">우리의 가치</h2>
          <p style="color: var(--text-secondary)">도우미가 추구하는 핵심 가치입니다</p>
        </div>
        <div class="grid md:grid-cols-3 gap-8">
          <div v-for="(value, index) in values" :key="index" class="text-center">
            <div
              class="w-16 h-16 rounded-full mx-auto mb-4 flex items-center justify-center text-2xl font-bold text-white"
              style="background-color: var(--primary)"
            >
              {{ index + 1 }}
            </div>
            <h3 class="font-bold text-xl mb-3" style="color: var(--text-primary)">{{ value.title }}</h3>
            <p style="color: var(--text-secondary)">{{ value.description }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- CTA Section: 비로그인 상태에서만 표시 -->
    <div v-if="!authStore.isAuthenticated" class="max-w-[1200px] mx-auto px-6 py-20">
      <div
        class="p-12 text-center rounded-xl border-2"
        style="background-color: var(--surface); border-color: var(--primary)"
      >
        <Heart class="w-14 h-14 mx-auto mb-4" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
        <h2 class="text-3xl font-bold mb-3" style="color: var(--text-primary)">함께 만드는 따뜻한 세상</h2>
        <p class="text-lg mb-8 max-w-2xl mx-auto" style="color: var(--text-secondary)">
          지금 도우미에 참여하여 세상을 변화시키는 주인공이 되어보세요.
          작은 나눔이 모여 큰 변화를 만듭니다.
        </p>
        <div class="flex gap-4 justify-center">
          <RouterLink to="/campaigns">
            <button class="px-6 py-3 rounded-md text-white font-medium" style="background-color: var(--primary)">
              캠페인 둘러보기
            </button>
          </RouterLink>
          <RouterLink to="/register">
            <button class="px-6 py-3 rounded-md border-2 font-medium" style="border-color: var(--primary); color: var(--primary)">
              회원가입하기
            </button>
          </RouterLink>
        </div>
      </div>
    </div>
  </div>
</template>
