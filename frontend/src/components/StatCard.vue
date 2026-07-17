<script setup>
defineProps({
  label: String,
  value: [String, Number],
  isDark: Boolean,
})
</script>

<template>
  <div class="text-center">
    <div
      class="font-bold mb-1"
      :style="{
        fontSize: '2rem',
        color: isDark ? 'white' : 'var(--primary)',
      }"
    >
      {{ value }}
    </div>
    <div
      class="text-sm"
      :style="{
        color: isDark ? 'rgba(255,255,255,0.5)' : 'var(--text-muted)',
      }"
    >
      {{ label }}
    </div>
  </div>
</template>
