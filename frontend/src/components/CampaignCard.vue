<script setup>
import { computed } from 'vue'
import { ArrowRight } from 'lucide-vue-next'
import placeholderImg from '@/assets/placeholder.jpg'
import { imageUrl } from '@/lib/utils'

const props = defineProps({
  campaignId: Number,
  category: String,
  daysLeft: Number,
  orgName: String,
  title: String,
  currentAmount: Number,
  goalAmount: Number,
  progress: Number,
  status: String,
  thumbnailUrl: String,
})

// 모집 완료/사용 완료된 캠페인 여부
const isClosed = computed(() =>
  ['모집완료', '사용완료'].includes(props.status)
)

// 기부 가능 여부 — '모집중'일 때만 (모집예정/완료/사용완료는 기부 불가)
const canDonate = computed(() => props.status === '모집중')

// 대표 이미지가 있으면 사용하고, 없으면 기본 플레이스홀더
const imgSrc = computed(() => imageUrl(props.thumbnailUrl) || placeholderImg)
</script>

<template>
  <div
    class="group rounded-2xl overflow-hidden transition-all duration-300"
    :class="isClosed ? '' : 'hover:-translate-y-1'"
    :style="{
      backgroundColor: 'var(--surface)',
      boxShadow: '0 2px 12px rgba(0,0,0,0.06)',
      opacity: isClosed ? 0.6 : 1,
    }"
  >
    <!-- Image -->
    <div class="relative aspect-[16/9] overflow-hidden">
      <img
        :src="imgSrc"
        :alt="title"
        class="w-full h-full object-cover transition-transform duration-300"
        :class="isClosed ? '' : 'group-hover:scale-105'"
        :style="{ filter: isClosed ? 'grayscale(100%) brightness(0.6)' : 'none' }"
      />
      <!-- 종료 상태 오버레이 (모집완료 / 사용완료 등 실제 상태 표시) -->
      <div v-if="isClosed" class="absolute inset-0 flex items-center justify-center" style="background-color: rgba(0,0,0,0.45)">
        <span class="px-4 py-1.5 rounded-full text-sm font-bold text-white" style="background-color: rgba(0,0,0,0.6)">
          {{ status }}
        </span>
      </div>
      <div class="absolute top-3 left-3 right-3 flex justify-between items-start">
        <span class="rounded-full px-3 py-1 text-sm" style="background-color: rgba(255,255,255,0.95); color: var(--text-primary)">
          {{ category }}
        </span>
        <span v-if="canDonate" class="rounded-full px-3 py-1 text-sm" style="background-color: var(--primary); color: white">
          D-{{ daysLeft }}
        </span>
        <span v-else-if="status === '모집예정'" class="rounded-full px-3 py-1 text-sm" style="background-color: rgba(255,255,255,0.95); color: var(--text-secondary)">
          모집 예정
        </span>
      </div>
    </div>

    <!-- Content -->
    <div class="p-5">
      <p class="text-xs mb-2" style="color: var(--text-muted)">{{ orgName }}</p>
      <h3 class="font-semibold mb-3 line-clamp-2 min-h-[3rem]" style="color: var(--text-primary); font-size: 1.125rem; line-height: 1.5rem">
        {{ title }}
      </h3>

      <!-- Progress -->
      <div class="mb-3">
        <div class="h-1.5 rounded-full mb-2" style="background-color: var(--border-color)">
          <div class="h-full rounded-full" :style="{ backgroundColor: 'var(--primary)', width: `${progress}%` }" />
        </div>
        <div class="flex justify-between items-center">
          <span class="font-bold" style="color: var(--primary); font-size: 0.875rem">{{ progress }}% 달성</span>
          <span class="text-xs" style="color: var(--text-muted)">목표 ₩{{ goalAmount?.toLocaleString() }}</span>
        </div>
      </div>

      <p class="font-bold mb-4" style="color: var(--text-primary); font-size: 1.25rem">
        ₩{{ currentAmount?.toLocaleString() }}
      </p>

      <RouterLink :to="`/campaigns/${campaignId}`">
        <button
          class="w-full h-11 rounded-lg flex items-center justify-center gap-1 transition-colors"
          :style="{
            backgroundColor: canDonate ? 'var(--primary)' : 'var(--border-color)',
            color: canDonate ? 'white' : 'var(--text-muted)',
          }"
        >
          <span>{{ canDonate ? '기부하기' : (status === '모집예정' ? '모집 예정' : status) }}</span>
          <ArrowRight v-if="canDonate" class="w-4 h-4" />
        </button>
      </RouterLink>
    </div>
  </div>
</template>
