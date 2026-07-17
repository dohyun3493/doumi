<script setup>
import { ref, computed } from 'vue'
import { uploadImage } from '@/lib/api'
import { imageUrl } from '@/lib/utils'
import { ImagePlus, Loader2, X } from 'lucide-vue-next'

const props = defineProps({
  // 현재 이미지 경로 (/uploads/... 또는 절대 URL)
  modelValue: { type: String, default: '' },
  label: { type: String, default: '이미지' },
  // 미리보기 모양: 'wide'(16:9) | 'avatar'(원형)
  shape: { type: String, default: 'wide' },
})

const emit = defineEmits(['update:modelValue'])

const uploading = ref(false)
const error = ref('')
const fileInput = ref(null)

const previewSrc = computed(() => imageUrl(props.modelValue))

function openPicker() {
  fileInput.value?.click()
}

async function onChange(e) {
  const file = e.target.files?.[0]
  if (!file) return

  // 클라이언트 1차 검증 (서버에서도 재검증함)
  if (!file.type.startsWith('image/')) {
    error.value = '이미지 파일만 업로드할 수 있습니다.'
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    error.value = '파일 용량은 5MB 이하만 가능합니다.'
    return
  }

  error.value = ''
  uploading.value = true
  try {
    const url = await uploadImage(file)
    emit('update:modelValue', url)
  } catch (err) {
    error.value = err.response?.data?.message || '업로드에 실패했습니다.'
  } finally {
    uploading.value = false
    // 같은 파일 다시 선택해도 change 이벤트가 발생하도록 초기화
    if (fileInput.value) fileInput.value.value = ''
  }
}

function remove() {
  emit('update:modelValue', '')
  error.value = ''
}
</script>

<template>
  <div>
    <p v-if="label" class="text-sm font-medium mb-2" style="color: var(--text-primary)">{{ label }}</p>

    <input ref="fileInput" type="file" accept="image/*" class="hidden" @change="onChange" />

    <!-- 아바타(원형) 모양 -->
    <div v-if="shape === 'avatar'" class="flex items-center gap-4">
      <div
        class="relative w-24 h-24 rounded-full overflow-hidden flex items-center justify-center cursor-pointer"
        style="background-color: var(--border-color)"
        @click="openPicker"
      >
        <img v-if="previewSrc" :src="previewSrc" alt="" class="w-full h-full object-cover" />
        <ImagePlus v-else class="w-7 h-7" style="color: var(--text-muted)" />
        <div v-if="uploading" class="absolute inset-0 flex items-center justify-center" style="background-color: rgba(0,0,0,0.45)">
          <Loader2 class="w-6 h-6 text-white animate-spin" />
        </div>
      </div>
      <div class="flex flex-col gap-2">
        <button type="button" class="px-3 py-1.5 rounded-lg text-sm" style="background-color: var(--primary); color: white" @click="openPicker">
          사진 변경
        </button>
        <button v-if="modelValue" type="button" class="px-3 py-1.5 rounded-lg text-sm" style="background-color: var(--border-color); color: var(--text-primary)" @click="remove">
          제거
        </button>
      </div>
    </div>

    <!-- wide(16:9) 모양 -->
    <div v-else>
      <div
        class="relative aspect-[16/9] rounded-xl overflow-hidden flex items-center justify-center cursor-pointer border-2 border-dashed"
        style="background-color: var(--surface); border-color: var(--border-color)"
        @click="openPicker"
      >
        <img v-if="previewSrc" :src="previewSrc" alt="" class="w-full h-full object-cover" />
        <div v-else class="flex flex-col items-center gap-2" style="color: var(--text-muted)">
          <ImagePlus class="w-8 h-8" />
          <span class="text-sm">클릭하여 이미지 업로드</span>
        </div>
        <button
          v-if="modelValue && !uploading"
          type="button"
          class="absolute top-2 right-2 w-8 h-8 rounded-full flex items-center justify-center"
          style="background-color: rgba(0,0,0,0.55); color: white"
          @click.stop="remove"
        >
          <X class="w-4 h-4" />
        </button>
        <div v-if="uploading" class="absolute inset-0 flex items-center justify-center" style="background-color: rgba(0,0,0,0.45)">
          <Loader2 class="w-7 h-7 text-white animate-spin" />
        </div>
      </div>
    </div>

    <p v-if="error" class="text-sm mt-2" style="color: #dc2626">{{ error }}</p>
  </div>
</template>
