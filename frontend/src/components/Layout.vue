<script setup>
import Header from './Header.vue'
import ChatbotWidget from './ChatbotWidget.vue'
</script>

<template>
  <div class="min-h-screen flex flex-col" style="background-color: var(--background)">
    <Header />
    <main class="flex-1">
      <RouterView />
    </main>
    <ChatbotWidget />
  </div>
</template>
