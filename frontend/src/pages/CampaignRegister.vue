<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import api from '@/lib/api'
import ImageUploader from '@/components/ImageUploader.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

// 경로에 :id 가 있으면 수정 모드, 없으면 등록 모드
const editId = route.params.id
const isEdit = computed(() => !!editId)

const isLoading = ref(false)
const errorMsg = ref('')

// 날짜 헬퍼 (로컬 기준 YYYY-MM-DD)
const pad = (n) => String(n).padStart(2, '0')
const fmt = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
const todayStr = fmt(new Date())
// 신규 등록 시 시작/종료일은 '내일'부터 선택 가능 — 오늘 포함 과거 날짜는 막는다
const _tomorrow = new Date()
_tomorrow.setDate(_tomorrow.getDate() + 1)
const minDate = fmt(_tomorrow)

// 수정 모드용: 불러온 캠페인의 현재 상태 (시작 여부 판단)
const campaignStatus = ref('')
// 이미 시작된 캠페인(모집예정이 아닌 상태)은 시작일을 바꿀 수 없음 — 지난 시작 시점은 수정 불가
const isStarted = computed(() => isEdit.value && !!campaignStatus.value && campaignStatus.value !== '모집예정')

// 시작일 input min: 등록=내일 이후 / 수정=오늘 이후 (이미 시작된 캠페인은 아래 disabled로 잠금)
const startMin = computed(() => (isEdit.value ? todayStr : minDate))
// 종료일 input min: 시작일과 today/내일 중 더 늦은 날 (종료일 > 시작일 보장)
const endMin = computed(() => {
  const floor = isEdit.value ? todayStr : minDate
  return form.value.startDate && form.value.startDate > floor ? form.value.startDate : floor
})

const form = ref({
  orgName: '',
  title: '',
  description: '',
  purpose: '',
  goalAmount: '',
  region: '',
  category: '',
  startDate: '',
  endDate: '',
  homepageUrl: '',
  address: '',
  telNo: '',
  thumbnailUrl: '',
})

const categories = ['아동·청소년', '노인', '장애인', '환경', '동물', '의료', '교육', '해외', '기타']
const regions = ['서울', '부산', '대구', '인천', '광주', '대전', '울산', '세종', '경기', '강원', '충북', '충남', '전북', '전남', '경북', '경남', '제주']

// 날짜 문자열을 input[type=date]가 인식하는 YYYY-MM-DD로 정규화 (백엔드가 시간 포함해 줄 수 있음)
const toDateInput = (v) => (v ? String(v).slice(0, 10) : '')

// 수정 모드: 기존 캠페인 값을 불러와 폼을 채운다 (thumbnailUrl 포함 → 업로더에 기존 사진 미리보기)
onMounted(async () => {
  if (!isEdit.value) return
  try {
    const { data } = await api.get(`/api/campaigns/${editId}`)
    campaignStatus.value = data.status ?? ''
    Object.assign(form.value, {
      orgName: data.orgName ?? '',
      title: data.title ?? '',
      description: data.description ?? '',
      purpose: data.purpose ?? '',
      goalAmount: data.goalAmount ?? '',
      region: data.region ?? '',
      category: data.category ?? '',
      startDate: toDateInput(data.startDate),
      endDate: toDateInput(data.endDate),
      homepageUrl: data.homepageUrl ?? '',
      address: data.address ?? '',
      telNo: data.telNo ?? '',
      thumbnailUrl: data.thumbnailUrl ?? '',
    })
  } catch (e) {
    errorMsg.value = '캠페인 정보를 불러오지 못했습니다.'
  }
})

async function submit() {
  if (!form.value.title || !form.value.orgName || !form.value.goalAmount) {
    errorMsg.value = '단체명, 캠페인명, 목표금액은 필수입니다.'
    return
  }
  // 날짜 검증: 종료일이 시작일보다 빠를 수 없음.
  // (과거 날짜 금지는 등록 시에만 — 수정 중인 캠페인은 이미 시작됐을 수 있으므로 제외)
  if (!isEdit.value) {
    if (form.value.startDate && form.value.startDate < minDate) {
      errorMsg.value = '시작일은 내일 이후 날짜로 선택해주세요.'
      return
    }
    if (form.value.endDate && form.value.endDate < minDate) {
      errorMsg.value = '종료일은 내일 이후 날짜로 선택해주세요.'
      return
    }
  }
  if (form.value.startDate && form.value.endDate && form.value.endDate < form.value.startDate) {
    errorMsg.value = '종료일은 시작일보다 빠를 수 없습니다.'
    return
  }
  isLoading.value = true
  errorMsg.value = ''
  try {
    const payload = { ...form.value, goalAmount: Number(form.value.goalAmount) }
    if (isEdit.value) {
      await api.patch(`/api/campaigns/${editId}`, payload)
      alert('캠페인이 수정되었습니다!')
      router.push(`/campaigns/${editId}`)
    } else {
      await api.post('/api/campaigns', payload)
      alert('캠페인이 등록되었습니다!')
      router.push('/campaigns')
    }
  } catch (e) {
    errorMsg.value = e.response?.data?.message
      || (typeof e.response?.data === 'string' ? e.response.data : (isEdit.value ? '수정에 실패했습니다.' : '등록에 실패했습니다.'))
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div style="background-color: var(--background); min-height: 100vh">
    <div class="max-w-[800px] mx-auto px-6 py-12">
      <div class="mb-8">
        <h1 class="font-bold mb-2" style="font-size: 2rem; color: var(--text-primary)">{{ isEdit ? '캠페인 수정' : '캠페인 등록' }}</h1>
        <p style="color: var(--text-secondary)">기부 캠페인 정보를 입력해주세요.</p>
      </div>

      <form @submit.prevent="submit" class="space-y-6">
        <!-- 기본 정보 -->
        <div class="rounded-2xl p-6" style="background-color: var(--surface)">
          <h2 class="font-semibold mb-4" style="color: var(--text-primary); font-size: 1.1rem">기본 정보</h2>
          <div class="space-y-4">
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">단체명 <span style="color: var(--danger)">*</span></label>
              <input v-model="form.orgName" type="text" placeholder="단체명을 입력하세요"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">캠페인명 <span style="color: var(--danger)">*</span></label>
              <input v-model="form.title" type="text" placeholder="캠페인 제목을 입력하세요"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <ImageUploader v-model="form.thumbnailUrl" label="대표 이미지" shape="wide" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">캠페인 소개</label>
              <textarea v-model="form.description" placeholder="캠페인을 소개해주세요" rows="4"
                class="w-full px-4 py-3 rounded-md border outline-none resize-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">모집 목적</label>
              <textarea v-model="form.purpose" placeholder="기부금 사용 목적을 입력하세요" rows="3"
                class="w-full px-4 py-3 rounded-md border outline-none resize-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
          </div>
        </div>

        <!-- 모집 정보 -->
        <div class="rounded-2xl p-6" style="background-color: var(--surface)">
          <h2 class="font-semibold mb-4" style="color: var(--text-primary); font-size: 1.1rem">모집 정보</h2>
          <div class="grid md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">목표금액 <span style="color: var(--danger)">*</span></label>
              <input v-model="form.goalAmount" type="number" placeholder="0" min="1"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">카테고리</label>
              <select v-model="form.category"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)">
                <option value="">선택하세요</option>
                <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">지역</label>
              <select v-model="form.region"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)">
                <option value="">선택하세요</option>
                <option v-for="r in regions" :key="r" :value="r">{{ r }}</option>
              </select>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">홈페이지 URL</label>
              <input v-model="form.homepageUrl" type="url" placeholder="https://" maxlength="300"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">시작일</label>
              <input v-model="form.startDate" type="date" :min="startMin" :disabled="isStarted"
                class="w-full h-11 px-4 rounded-md border outline-none disabled:opacity-60 disabled:cursor-not-allowed"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
              <p v-if="isStarted" class="text-xs mt-1" style="color: var(--text-muted)">이미 시작된 캠페인은 시작일을 변경할 수 없습니다.</p>
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">종료일</label>
              <input v-model="form.endDate" type="date" :min="endMin"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
          </div>
        </div>

        <!-- 연락처 정보 -->
        <div class="rounded-2xl p-6" style="background-color: var(--surface)">
          <h2 class="font-semibold mb-4" style="color: var(--text-primary); font-size: 1.1rem">연락처 정보</h2>
          <div class="grid md:grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">전화번호</label>
              <input v-model="form.telNo" type="tel" placeholder="02-000-0000"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
            <div>
              <label class="block text-sm font-medium mb-1" style="color: var(--text-primary)">주소</label>
              <input v-model="form.address" type="text" placeholder="주소를 입력하세요"
                class="w-full h-11 px-4 rounded-md border outline-none"
                style="border-color: var(--border-color); background-color: var(--surface); color: var(--text-primary)" />
            </div>
          </div>
        </div>

        <div v-if="errorMsg" class="text-sm text-center" style="color: var(--danger)">{{ errorMsg }}</div>

        <div class="flex gap-3">
          <button type="button" @click="router.back()"
            class="flex-1 h-12 rounded-md border font-medium"
            style="border-color: var(--border-color); color: var(--text-primary)">
            취소
          </button>
          <button type="submit" :disabled="isLoading"
            class="flex-1 h-12 rounded-md text-white font-medium disabled:opacity-50"
            style="background-color: var(--primary)">
            {{ isLoading ? (isEdit ? '수정 중...' : '등록 중...') : (isEdit ? '캠페인 수정' : '캠페인 등록') }}
          </button>
        </div>
      </form>
    </div>
  </div>
</template>
