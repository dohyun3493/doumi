import { clsx } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs) {
  return twMerge(clsx(inputs))
}

// 백엔드(이미지 정적 서빙) 오리진. api.js의 baseURL과 동일하게 유지.
// 개발: http://localhost:8080 / 운영: '' (같은 도메인, Nginx가 /uploads 프록시)
export const API_ORIGIN = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

// 서버 상대 경로(/uploads/...)를 절대 URL로 변환. 빈 값이면 null 반환.
export function imageUrl(path) {
  if (!path) return null
  if (path.startsWith('http')) return path
  return `${API_ORIGIN}${path}`
}
