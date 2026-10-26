<script setup>
import { watch, onBeforeUnmount } from 'vue'
import { store, closeOutcome } from '../store.js'

// The result of the latest Solve or Buy, in the middle of the screen for a few seconds.
// Taps and clicks go through it to the board underneath, so playing quickly never loses a tap;
// only ✕ closes it early.
const SHOW_MS = 4000
let timer = null

// A new outcome (new id) starts the 4 seconds again
watch(
  () => store.outcome?.id,
  (id) => {
    clearTimeout(timer)
    if (id) timer = setTimeout(closeOutcome, SHOW_MS)
  },
)

onBeforeUnmount(() => clearTimeout(timer))
</script>

<template>
  <div class="layer">
    <Transition name="pop">
      <div
        v-if="store.outcome"
        :key="store.outcome.id"
        class="outcome"
        :class="store.outcome.success ? 'success' : 'failure'"
        role="status"
      >
        <span class="icon">{{ store.outcome.success ? '✅' : '❌' }}</span>
        <span class="text">{{ store.outcome.text }}</span>
        <button class="secondary close" title="Close" @click="closeOutcome">✕</button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
/* Covers the screen only to center the card */
.layer {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: grid;
  place-items: center;
}

/* The layer and the card let every tap and click through to the page underneath... */
.layer,
.outcome {
  pointer-events: none;
}

/* ...except ✕ */
.close {
  pointer-events: auto;
}

.outcome {
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  width: min(420px, calc(100% - 32px));
  padding: 0.9rem 1rem;
  border: 2px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: 0 8px 28px rgb(0 0 0 / 0.35);
  font-size: 1.05rem;
  font-weight: 600;
}

.outcome.success {
  border-color: var(--success);
  color: var(--success);
}

.outcome.failure {
  border-color: var(--danger);
  color: var(--danger);
}

.text {
  flex: 1;
}

.close {
  padding: 0.1rem 0.5rem;
  border-color: currentColor;
  color: inherit;
  font-size: 0.85rem;
}

/* Fades in and grows a little; fades out */
.pop-enter-active,
.pop-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>