<script setup>
import { store } from '../store.js'
</script>

<template>
  <!-- 6 Last message: the outcome of the latest action (solve, buy, investigate, new game) -->
  <section class="pane">
    <h2 class="pane-title">Last message</h2>

    <p v-if="store.lastMessage" :class="store.lastMessage.success ? 'success' : 'failure'">
      {{ store.lastMessage.success ? '✅' : '❌' }} {{ store.lastMessage.text }}
    </p>
    <p v-else class="muted">The outcome of your last action shows here.</p>
  </section>
</template>

<style scoped>
p {
  margin: 0;
}

.success {
  color: var(--success);
}

.failure {
  color: var(--danger);
}
</style>