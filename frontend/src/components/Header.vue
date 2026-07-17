<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Bell, Heart, User, Menu, X } from 'lucide-vue-next'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'
import { imageUrl } from '@/lib/utils'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const navLinks = [
  { name: '캠페인', path: '/campaigns' },
  { name: '기부 랭킹', path: '/ranking' },
  { name: '커뮤니티', path: '/board' },
  { name: '서비스 소개', path: '/about' },
]

// ===== 모바일 메뉴 =====
const mobileMenuOpen = ref(false)
function toggleMobileMenu() {
  mobileMenuOpen.value = !mobileMenuOpen.value
}
function closeMobileMenu() {
  mobileMenuOpen.value = false
}

// ===== 알림 =====
const notifOpen = ref(false)
const notifications = ref([])
const unreadCount = computed(() => notifications.value.filter((n) => !n.read).length)
let notifTimer = null

async function fetchNotifications() {
  if (!authStore.isAuthenticated) return
  try {
    const res = await api.get('/notifications')
    notifications.value = res.data
  } catch (e) {
    // 알림 조회 실패는 조용히 무시 (다음 폴링에서 재시도)
  }
}

function toggleNotif() {
  notifOpen.value = !notifOpen.value
  if (notifOpen.value) fetchNotifications()
}

const detailNotif = ref(null)

async function openNotification(n) {
  if (!n.read) {
    try {
      await api.patch(`/notifications/${n.notificationId}/read`)
      n.read = true
    } catch (e) { /* 읽음 처리 실패해도 상세는 표시 */ }
  }
  notifOpen.value = false
  detailNotif.value = n   // 상세 내용 팝업 표시
}

function closeDetail() {
  detailNotif.value = null
}

function goDetailLink() {
  const url = detailNotif.value?.linkUrl
  detailNotif.value = null
  if (url) router.push(url)
}

async function markAllRead() {
  try {
    await api.patch('/notifications/read-all')
    notifications.value.forEach((n) => { n.read = true })
  } catch (e) { /* 무시 */ }
}

async function deleteNotification(n) {
  try {
    await api.delete(`/notifications/${n.notificationId}`)
    notifications.value = notifications.value.filter((x) => x.notificationId !== n.notificationId)
  } catch (e) { /* 무시 */ }
}

onMounted(() => {
  fetchNotifications()
  notifTimer = setInterval(fetchNotifications, 30000)
})

onUnmounted(() => clearInterval(notifTimer))

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <header
    class="sticky top-0 z-50 bg-white border-b"
    style="border-color: var(--border-color); height: 72px"
  >
    <div class="max-w-[1200px] mx-auto px-6 h-full flex items-center justify-between">
      <!-- Logo -->
      <RouterLink to="/" class="flex items-center gap-2">
        <Heart class="w-7 h-7" :style="{ color: 'var(--primary)' }" fill="var(--primary)" />
        <span class="text-xl font-bold" style="color: var(--text-primary)">도우미</span>
      </RouterLink>

      <!-- Navigation -->
      <nav class="hidden md:flex items-center gap-8">
        <RouterLink
          v-for="link in navLinks"
          :key="link.path"
          :to="link.path"
          class="transition-colors"
          :style="{
            color: route.path === link.path ? 'var(--primary)' : 'var(--text-secondary)',
            fontWeight: route.path === link.path ? 600 : 400,
          }"
        >
          {{ link.name }}
        </RouterLink>
      </nav>

      <!-- Right side -->
      <div class="flex items-center gap-2 md:gap-3">
        <!-- 모바일 햄버거 버튼 -->
        <button
          class="md:hidden p-2 -ml-1"
          aria-label="메뉴"
          @click="toggleMobileMenu"
        >
          <Menu v-if="!mobileMenuOpen" class="w-6 h-6" :style="{ color: 'var(--text-primary)' }" />
          <X v-else class="w-6 h-6" :style="{ color: 'var(--text-primary)' }" />
        </button>
        <template v-if="authStore.isAuthenticated">
          <div class="relative">
            <button class="relative p-2" aria-label="알림" @click="toggleNotif">
              <Bell class="w-5 h-5" />
              <span
                v-if="unreadCount > 0"
                class="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] px-1 rounded-full text-[11px] font-bold text-white flex items-center justify-center"
                style="background-color: var(--primary)"
              >{{ unreadCount > 9 ? '9+' : unreadCount }}</span>
            </button>

            <!-- 알림 드롭다운 -->
            <div
              v-if="notifOpen"
              class="fixed left-3 right-3 top-[76px] md:absolute md:left-auto md:right-0 md:top-full md:mt-2 md:w-80 rounded-xl overflow-hidden z-50"
              style="background-color: var(--surface); box-shadow: 0 8px 24px rgba(0,0,0,0.14); border: 1px solid var(--border-color)"
            >
              <div class="flex items-center justify-between px-4 py-3" style="border-bottom: 1px solid var(--border-color)">
                <span class="font-semibold text-sm" style="color: var(--text-primary)">알림</span>
                <button v-if="unreadCount > 0" class="text-xs" style="color: var(--primary)" @click="markAllRead">모두 읽음</button>
              </div>
              <div class="max-h-96 overflow-y-auto">
                <p v-if="notifications.length === 0" class="text-center py-8 text-sm" style="color: var(--text-muted)">
                  알림이 없습니다.
                </p>
                <div
                  v-for="n in notifications"
                  :key="n.notificationId"
                  class="relative flex items-start gap-2 px-4 py-3 text-sm transition-colors hover:opacity-90"
                  :style="{ backgroundColor: n.read ? 'transparent' : 'var(--accent)', borderBottom: '1px solid var(--border-color)' }"
                >
                  <button class="flex-1 text-left" @click="openNotification(n)">
                    <span class="block pr-2" style="color: var(--text-primary)">{{ n.content }}</span>
                    <span class="block text-xs mt-1" style="color: var(--text-muted)">{{ n.createdAt?.slice(0, 16).replace('T', ' ') }}</span>
                  </button>
                  <button
                    class="shrink-0 w-6 h-6 rounded-full flex items-center justify-center text-base leading-none hover:bg-black/10"
                    style="color: var(--text-muted)"
                    aria-label="알림 삭제"
                    @click.stop="deleteNotification(n)"
                  >×</button>
                </div>
              </div>
            </div>
          </div>
          <div class="px-3 py-1.5 rounded-full flex items-center gap-1.5" style="background-color: var(--accent)">
            <span class="text-sm font-semibold" style="color: var(--primary)">
              {{ authStore.currentUser?.pointBalance?.toLocaleString() ?? 0 }}P
            </span>
          </div>
          <RouterLink to="/mypage">
            <div class="w-9 h-9 rounded-full flex items-center justify-center cursor-pointer overflow-hidden" style="background-color: var(--primary)">
              <img v-if="imageUrl(authStore.currentUser?.profileImageUrl)" :src="imageUrl(authStore.currentUser.profileImageUrl)" alt="" class="w-full h-full object-cover" />
              <User v-else class="w-5 h-5 text-white" />
            </div>
          </RouterLink>
          <button @click="logout" class="h-9 px-4 rounded-md border text-sm font-medium" style="border-color: var(--border-color); color: var(--text-primary)">
            로그아웃
          </button>
        </template>
        <template v-else>
          <RouterLink to="/login">
            <button class="h-9 px-4 rounded-md border text-sm font-medium transition-colors" style="border-color: var(--border-color); color: var(--text-primary)">
              로그인
            </button>
          </RouterLink>
          <RouterLink to="/register">
            <button class="h-9 px-4 rounded-md text-sm font-medium text-white transition-colors" style="background-color: var(--primary)">
              회원가입
            </button>
          </RouterLink>
        </template>
      </div>
    </div>

    <!-- 모바일 드롭다운 메뉴 -->
    <nav
      v-if="mobileMenuOpen"
      class="md:hidden bg-white border-b px-6 py-3 flex flex-col"
      style="border-color: var(--border-color)"
    >
      <RouterLink
        v-for="link in navLinks"
        :key="link.path"
        :to="link.path"
        class="py-3 transition-colors"
        :style="{
          color: route.path === link.path ? 'var(--primary)' : 'var(--text-secondary)',
          fontWeight: route.path === link.path ? 600 : 400,
        }"
        @click="closeMobileMenu"
      >
        {{ link.name }}
      </RouterLink>
    </nav>
  </header>

  <!-- 알림 상세 팝업 -->
  <div
    v-if="detailNotif"
    class="fixed inset-0 z-[60] flex items-center justify-center px-4"
    style="background-color: rgba(0,0,0,0.45)"
    @click.self="closeDetail"
  >
    <div class="w-full max-w-sm rounded-2xl p-6" style="background-color: var(--surface)">
      <h3 class="font-bold mb-3" style="font-size: 1.1rem; color: var(--text-primary)">알림</h3>
      <p class="text-sm whitespace-pre-wrap mb-4" style="color: var(--text-secondary)">{{ detailNotif.content }}</p>
      <p class="text-xs mb-5" style="color: var(--text-muted)">
        {{ detailNotif.createdAt?.slice(0, 16).replace('T', ' ') }}
      </p>
      <div class="flex gap-2 justify-end">
        <button
          v-if="detailNotif.linkUrl"
          @click="goDetailLink"
          class="px-4 py-2 rounded-md text-sm font-medium text-white"
          style="background-color: var(--primary)"
        >바로가기</button>
        <button
          @click="closeDetail"
          class="px-4 py-2 rounded-md border text-sm font-medium"
          style="border-color: var(--border-color); color: var(--text-primary)"
        >닫기</button>
      </div>
    </div>
  </div>
</template>
