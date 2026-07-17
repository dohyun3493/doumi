<script setup>
import { Heart, Home } from 'lucide-vue-next'
</script>

<template>
  <div
    class="min-h-screen flex items-center justify-center px-6"
    style="background-color: var(--background)"
  >
    <div class="text-center">
      <div
        class="w-24 h-24 rounded-full mx-auto mb-6 flex items-center justify-center"
        style="background-color: var(--accent)"
      >
        <Heart class="w-12 h-12" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
      </div>

      <h1 class="font-bold mb-4" style="font-size: 4rem; color: var(--text-primary)">404</h1>

      <h2 class="font-semibold mb-2" style="font-size: 1.5rem; color: var(--text-primary)">
        페이지를 찾을 수 없습니다
      </h2>

      <p class="mb-8" style="color: var(--text-secondary)">
        요청하신 페이지가 존재하지 않거나 이동되었습니다.
      </p>

      <RouterLink to="/">
        <button
          class="h-12 px-8 rounded-md text-white font-medium flex items-center gap-2 mx-auto"
          style="background-color: var(--primary)"
        >
          <Home class="w-5 h-5" />
          홈으로 돌아가기
        </button>
      </RouterLink>
    </div>
  </div>
</template>
