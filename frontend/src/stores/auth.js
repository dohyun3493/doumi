import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useAuthStore = defineStore('auth', () => {
  const currentUser = ref(JSON.parse(localStorage.getItem('user')) || null)
  const isAuthenticated = computed(() => currentUser.value !== null)

  function login(user) {
    currentUser.value = user
    localStorage.setItem('user', JSON.stringify(user))
  }

  function logout() {
    currentUser.value = null
    localStorage.removeItem('user')
    localStorage.removeItem('accessToken')
    localStorage.removeItem('refreshToken')
  }

  function chargePoint(amount) {
    if (currentUser.value) {
      currentUser.value.pointBalance += amount
      localStorage.setItem('user', JSON.stringify(currentUser.value))
    }
  }

  function usePoint(amount) {
    if (currentUser.value) {
      currentUser.value.pointBalance -= amount
      localStorage.setItem('user', JSON.stringify(currentUser.value))
    }
  }

  // DB에서 가져온 실제 잔액으로 동기화
  function syncPointBalance(balance) {
    if (currentUser.value) {
      currentUser.value.pointBalance = balance
      localStorage.setItem('user', JSON.stringify(currentUser.value))
    }
  }

  // 회원 정보 일부 갱신 (이름, 프로필 사진 등) — store + localStorage 동기화
  function updateUser(partial) {
    if (currentUser.value) {
      currentUser.value = { ...currentUser.value, ...partial }
      localStorage.setItem('user', JSON.stringify(currentUser.value))
    }
  }

  return { currentUser, isAuthenticated, login, logout, chargePoint, usePoint, syncPointBalance, updateUser }
})
