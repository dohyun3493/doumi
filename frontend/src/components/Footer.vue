<script setup>
import { Heart, Facebook, Twitter, Instagram, Youtube } from 'lucide-vue-next'

const siteMap = {
  '서비스': ['소개', '이용약관', '개인정보처리방침', '제휴문의'],
  '캠페인': ['진행중', '마감임박', '완료된 캠페인', '캠페인 등록'],
  '커뮤니티': ['공지사항', '기부 후기', 'FAQ', '문의하기'],
  '고객지원': ['자주 묻는 질문', '1:1 문의', '신고하기', '영수증 발급'],
}

const socialLinks = [
  { Icon: Facebook, href: '#' },
  { Icon: Twitter, href: '#' },
  { Icon: Instagram, href: '#' },
  { Icon: Youtube, href: '#' },
]
</script>

<template>
  <footer class="mt-24 py-12" style="background-color: var(--text-primary)">
    <div class="max-w-[1200px] mx-auto px-6">
      <div class="grid grid-cols-1 md:grid-cols-5 gap-12 mb-8">
        <!-- Logo & Description -->
        <div class="md:col-span-1">
          <div class="flex items-center gap-2 mb-4">
            <Heart class="w-6 h-6" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
            <span class="text-lg font-bold text-white">도우미</span>
          </div>
          <p class="text-sm opacity-60 text-white">
            투명한 기부로 더 좋은<br />세상을 만듭니다
          </p>
        </div>

        <!-- Site Map -->
        <div v-for="(links, category) in siteMap" :key="category">
          <h4 class="font-semibold text-white mb-4">{{ category }}</h4>
          <ul class="space-y-2">
            <li v-for="link in links" :key="link">
              <RouterLink
                to="#"
                class="text-sm opacity-60 hover:opacity-100 transition-opacity text-white"
              >
                {{ link }}
              </RouterLink>
            </li>
          </ul>
        </div>
      </div>

      <!-- Bottom Section -->
      <div
        class="pt-8 flex flex-col md:flex-row justify-between items-center gap-4"
        style="border-top: 1px solid rgba(255,255,255,0.1)"
      >
        <div class="flex flex-wrap gap-4 text-sm opacity-60 text-white">
          <RouterLink to="#" class="hover:opacity-100 transition-opacity">
            개인정보처리방침
          </RouterLink>
          <RouterLink to="#" class="hover:opacity-100 transition-opacity">
            이용약관
          </RouterLink>
          <span>© 2026 도우미. All rights reserved.</span>
        </div>

        <!-- Social Links -->
        <div class="flex gap-3">
          <a
            v-for="({ Icon, href }, index) in socialLinks"
            :key="index"
            :href="href"
            class="w-8 h-8 rounded-full flex items-center justify-center opacity-60 hover:opacity-100 transition-opacity"
            style="background-color: rgba(255,255,255,0.1)"
          >
            <component :is="Icon" class="w-4 h-4 text-white" />
          </a>
        </div>
      </div>
    </div>
  </footer>
</template>
