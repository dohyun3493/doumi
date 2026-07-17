<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import api from '@/lib/api'
import { Users, Target, DollarSign, TrendingUp, Building2 } from 'lucide-vue-next'

// 공공데이터 동기화
const isSyncing = ref(false)
const syncMessage = ref('')

async function syncPublicData() {
  isSyncing.value = true
  syncMessage.value = ''
  try {
    await api.post('/api/admin/public-data')
    syncMessage.value = '✅ 공공데이터 동기화 요청이 완료되었습니다.'
  } catch (e) {
    syncMessage.value = '❌ 동기화 요청에 실패했습니다.'
  } finally {
    isSyncing.value = false
  }
}

// 챗봇 벡터 DB(Redis) 재색인
const isReindexing = ref(false)
const reindexMessage = ref('')

async function reindexVectors() {
  isReindexing.value = true
  reindexMessage.value = ''
  try {
    const res = await api.post('/api/admin/chatbot/reindex')
    const n = res.data?.indexed
    reindexMessage.value = `✅ 재색인 완료${n != null ? ` (${n}건)` : ''}`
  } catch (e) {
    reindexMessage.value = '❌ 재색인에 실패했습니다.'
  } finally {
    isReindexing.value = false
  }
}

// 대시보드 통계 (백엔드 /api/stats)
const stats = ref(null)

async function fetchStats() {
  try {
    const res = await api.get('/api/stats')
    stats.value = res.data
  } catch (e) {
    console.error('통계 조회 실패', e)
  }
}

// 최근 6개월(기부 없는 달도 0으로 채워 항상 6칸 표시) — 막대그래프용
const monthly6 = computed(() => {
  const map = {}
  ;(stats.value?.monthlyDonations || []).forEach((m) => { map[m.month] = Number(m.amount) || 0 })
  const out = []
  const now = new Date()
  for (let i = 5; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    out.push({ month: key, label: `${d.getMonth() + 1}월`, amount: map[key] || 0 })
  }
  return out
})
const maxMonthly = computed(() => Math.max(1, ...monthly6.value.map((m) => m.amount)))
// 막대 높이: 최대치를 차트의 90%로 두고, 위 10%는 금액 라벨 공간으로 남긴다.
// animated 가 false인 초기엔 0 → 마운트 후 true가 되며 0에서 자라는 애니메이션이 보인다.
const barHeight = (amount) => {
  if (!animated.value) return '0%'
  return amount > 0 ? `${Math.max(4, Math.round((amount / maxMonthly.value) * 90))}%` : '0%'
}
const fmtWon = (n) => '₩' + (n || 0).toLocaleString()

// 막대마다 다른 색 (월 순서대로 — indigo/violet/pink/amber/emerald/cyan)
const barColors = ['#6366f1', '#8b5cf6', '#ec4899', '#f59e0b', '#10b981', '#06b6d4']

// 마운트 → 통계 로드 → DOM 갱신 후 막대를 0에서 키운다 (0에서 쑤욱 차오르는 효과)
const animated = ref(false)
onMounted(async () => {
  await fetchStats()
  await nextTick()
  requestAnimationFrame(() => { animated.value = true })
})
</script>

<template>
  <div>
    <h1 class="font-bold mb-8" style="font-size: 2rem; color: var(--text-primary)">대시보드</h1>

    <!-- 운영 작업: 공공데이터 동기화 / 챗봇 재색인 -->
    <div class="grid md:grid-cols-2 gap-6 mb-8">
      <div class="p-4 rounded-xl flex items-center gap-4" style="background-color: var(--surface); border: 1px solid var(--border-color)">
        <div class="flex-1">
          <div class="font-semibold mb-1" style="color: var(--text-primary)">공공데이터 동기화</div>
          <div class="text-sm" style="color: var(--text-muted)">외부 공공데이터를 캠페인 DB에 동기화합니다.</div>
          <div v-if="syncMessage" class="text-sm mt-1" style="color: var(--primary)">{{ syncMessage }}</div>
        </div>
        <button
          @click="syncPublicData"
          :disabled="isSyncing"
          class="px-6 py-2 rounded-md text-white text-sm font-medium disabled:opacity-50"
          style="background-color: var(--primary)"
        >
          {{ isSyncing ? '동기화 중...' : '동기화 실행' }}
        </button>
      </div>

      <div class="p-4 rounded-xl flex items-center gap-4" style="background-color: var(--surface); border: 1px solid var(--border-color)">
        <div class="flex-1">
          <div class="font-semibold mb-1" style="color: var(--text-primary)">챗봇 벡터 재색인</div>
          <div class="text-sm" style="color: var(--text-muted)">전체 캠페인을 챗봇 추천용 벡터 DB(Redis)에 다시 색인합니다.</div>
          <div v-if="reindexMessage" class="text-sm mt-1" style="color: var(--primary)">{{ reindexMessage }}</div>
        </div>
        <button
          @click="reindexVectors"
          :disabled="isReindexing"
          class="px-6 py-2 rounded-md text-white text-sm font-medium disabled:opacity-50"
          style="background-color: var(--secondary)"
        >
          {{ isReindexing ? '재색인 중...' : '재색인 실행' }}
        </button>
      </div>
    </div>

    <!-- KPI Cards (백엔드 /api/stats 실데이터) -->
    <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <DollarSign class="w-8 h-8 mb-4" style="color: var(--primary)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          ₩{{ (stats?.totalDonationAmount ?? 0).toLocaleString() }}
        </div>
        <div class="text-sm" style="color: var(--text-muted)">총 누적 기부금</div>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <Target class="w-8 h-8 mb-4" style="color: var(--info)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          {{ (stats?.activeCampaigns ?? 0).toLocaleString() }}개
        </div>
        <div class="text-sm" style="color: var(--text-muted)">진행 중인 캠페인</div>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <Target class="w-8 h-8 mb-4" style="color: var(--secondary)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          {{ (stats?.completedCampaigns ?? 0).toLocaleString() }}개
        </div>
        <div class="text-sm" style="color: var(--text-muted)">완료된 캠페인</div>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <Users class="w-8 h-8 mb-4" style="color: var(--secondary)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          {{ (stats?.individualMembers ?? 0).toLocaleString() }}명
        </div>
        <div class="text-sm" style="color: var(--text-muted)">개인 회원</div>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <Building2 class="w-8 h-8 mb-4" style="color: var(--secondary)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          {{ (stats?.organizationMembers ?? 0).toLocaleString() }}곳
        </div>
        <div class="text-sm" style="color: var(--text-muted)">단체 회원</div>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <TrendingUp class="w-8 h-8 mb-4" style="color: var(--success)" />
        <div class="font-bold mb-1" style="font-size: 1.75rem; color: var(--text-primary)">
          {{ (stats?.monthlyDonors ?? 0).toLocaleString() }}명
        </div>
        <div class="text-sm" style="color: var(--text-muted)">이번 달 기부자 수</div>
      </div>
    </div>

    <!-- 최근 6개월 월별 기부 (막대그래프) -->
    <div class="rounded-xl p-6 border mt-8" style="background-color: var(--surface); border-color: var(--border-color)">
      <h3 class="font-semibold mb-6" style="color: var(--text-primary)">최근 6개월 기부 내역</h3>
      <div v-if="monthly6.some((m) => m.amount > 0)">
        <!-- 막대 영역 -->
        <div class="flex items-end justify-between gap-2 sm:gap-4" style="height: 200px">
          <div v-for="(m, i) in monthly6" :key="m.month" class="flex-1 h-full flex flex-col items-center justify-end">
            <div class="text-xs mb-1 font-medium whitespace-nowrap" style="color: var(--text-secondary)">{{ fmtWon(m.amount) }}</div>
            <div class="w-full rounded-t-md"
              :style="{
                height: barHeight(m.amount),
                background: barColors[i],
                transition: 'height 0.8s cubic-bezier(0.34, 1.56, 0.64, 1)',
                transitionDelay: `${i * 90}ms`,
              }"
              :title="`${m.month} · ${fmtWon(m.amount)}`"></div>
          </div>
        </div>
        <!-- 월 라벨 -->
        <div class="flex justify-between gap-2 sm:gap-4 mt-2 pt-2" style="border-top: 1px solid var(--border-color)">
          <div v-for="m in monthly6" :key="m.month" class="flex-1 text-center text-xs" style="color: var(--text-muted)">{{ m.label }}</div>
        </div>
      </div>
      <p v-else class="text-center py-10 text-sm" style="color: var(--text-muted)">최근 6개월 기부 내역이 없습니다.</p>
    </div>

    <!-- 카테고리별 / 지역별 캠페인 수 -->
    <div class="grid md:grid-cols-2 gap-6 mt-6">
      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <h3 class="font-semibold mb-4" style="color: var(--text-primary)">카테고리별 캠페인 수</h3>
        <table class="w-full text-sm">
          <tbody>
            <tr v-if="!stats?.categoryCounts?.length">
              <td class="text-center py-4" style="color: var(--text-muted)">데이터가 없습니다.</td>
            </tr>
            <tr v-for="c in stats?.categoryCounts" :key="c.label" style="border-bottom: 1px solid var(--border-color)">
              <td class="py-2 px-2" style="color: var(--text-primary)">{{ c.label }}</td>
              <td class="py-2 px-2 text-right font-medium" style="color: var(--text-secondary)">{{ c.count }}개</td>
            </tr>
          </tbody>
        </table>
      </div>

      <div class="rounded-xl p-6 border" style="background-color: var(--surface); border-color: var(--border-color)">
        <h3 class="font-semibold mb-4" style="color: var(--text-primary)">지역별 캠페인 수</h3>
        <table class="w-full text-sm">
          <tbody>
            <tr v-if="!stats?.regionCounts?.length">
              <td class="text-center py-4" style="color: var(--text-muted)">데이터가 없습니다.</td>
            </tr>
            <tr v-for="r in stats?.regionCounts" :key="r.label" style="border-bottom: 1px solid var(--border-color)">
              <td class="py-2 px-2" style="color: var(--text-primary)">{{ r.label }}</td>
              <td class="py-2 px-2 text-right font-medium" style="color: var(--text-secondary)">{{ r.count }}개</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>
