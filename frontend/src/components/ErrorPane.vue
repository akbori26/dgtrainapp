<script setup>
import { store, dismissError } from '../store.js'
</script>

<template>
  <!-- 7 Server errors: the latest failed request. The next successful action clears it too. -->
  <section class="pane" :class="{ error: store.lastError }">
    <h2 class="pane-title">Server errors</h2>

    <template v-if="store.lastError">
      <p>❌ {{ store.lastError.message }}</p>
      <div class="error-footer">
        <span class="code">{{ store.lastError.code }} · {{ store.lastError.status || 'no answer' }}</span>
        <button class="secondary" @click="dismissError">Dismiss</button>
      </div>
    </template>
    <p v-else class="muted">No errors.</p>
  </section>
</template>

<style scoped>
p {
  margin: 0;
}

/* The pane title is muted grey by default; in an error it turns red with the rest */
.error .pane-title {
  color: var(--danger);
}

.error-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-top: 0.5rem;
}

.code {
  font-family: ui-monospace, monospace;
  font-size: 0.8rem;
  opacity: 0.8;
}

.error button.secondary {
  border-color: var(--danger);
  color: var(--danger);
}
</style>