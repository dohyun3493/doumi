<script setup>
import { ref, computed, onMounted } from 'vue'
import api from '@/lib/api'
import { Heart, LayoutDashboard, Users, Target, Building2 } from 'lucide-vue-next'
import AdminDashboard from '@/components/admin/AdminDashboard.vue'
import AdminOrganizations from '@/components/admin/AdminOrganizations.vue'
import AdminMembers from '@/components/admin/AdminMembers.vue'
import AdminCampaigns from '@/components/admin/AdminCampaigns.vue'

const activeMenu = ref('dashboard')

const menuItems = [
  { id: 'dashboard', label: '대시보드', icon: LayoutDashboard, component: AdminDashboard },
  { id: 'organizations', label: '단체 승인', icon: Building2, component: AdminOrganizations },
  { id: 'users', label: '회원 관리', icon: Users, component: AdminMembers },
  { id: 'campaigns', label: '캠페인 관리', icon: Target, component: AdminCampaigns },
]

const currentComponent = computed(
  () => menuItems.find((m) => m.id === activeMenu.value)?.component
)

// 사이드바 '단체 승인' 빨간 배지용 — 승인 대기 단체 수
const pendingOrganizations = ref(0)

async function fetchPendingCount() {
  try {
    const res = await api.get('/api/stats')
    pendingOrganizations.value = res.data?.pendingOrganizations ?? 0
  } catch (e) {
    /* 배지 실패는 조용히 무시 */
  }
}

onMounted(fetchPendingCount)
</script>

<template>
  <div class="flex min-h-screen" style="background-color: var(--background)">
    <!-- Sidebar -->
    <aside class="w-64 min-h-screen" style="background-color: var(--text-primary)">
      <div class="p-6">
        <!-- Logo (클릭 시 메인 홈으로) -->
        <RouterLink to="/" class="flex items-center gap-2 mb-8 w-fit">
          <Heart class="w-7 h-7 text-white" fill="white" />
          <span class="text-xl font-bold text-white">도우미</span>
        </RouterLink>

        <!-- Menu -->
        <nav class="space-y-1">
          <button
            v-for="item in menuItems"
            :key="item.id"
            @click="activeMenu = item.id"
            class="w-full text-left px-4 py-3 rounded-lg transition-colors flex items-center gap-3 text-sm text-white"
            :style="{ backgroundColor: activeMenu === item.id ? 'var(--primary)' : 'transparent' }"
          >
            <component :is="item.icon" class="w-5 h-5" />
            <span>{{ item.label }}</span>
            <span
              v-if="item.id === 'organizations' && pendingOrganizations > 0"
              class="ml-auto min-w-[20px] h-5 px-1.5 rounded-full text-[11px] font-bold flex items-center justify-center"
              style="background-color: var(--danger); color: white"
            >{{ pendingOrganizations }}</span>
          </button>
        </nav>
      </div>
    </aside>

    <!-- Main Content: 활성 탭 컴포넌트 -->
    <main class="flex-1 p-8">
      <component :is="currentComponent" @changed="fetchPendingCount" />
    </main>
  </div>
</template>
