<script setup>
// A button's text that turns into a spinning ring while its request runs:
//   <button><BusyLabel :busy="store.busyAction === 'investigate'">Investigate</BusyLabel></button>
// The text stays in place (invisible), so the button keeps its size.
defineProps({
  busy: { type: Boolean, default: false },
})
</script>

<template>
  <span class="busy-label" :class="{ busy }">
    <span class="text"><slot /></span>
    <span v-if="busy" class="spinner" role="status" aria-label="Working"></span>
  </span>
</template>

<style scoped>
.busy-label {
  position: relative;
  display: inline-block;
}

.busy .text {
  visibility: hidden;
}

/* A ring with one open side, turning; it takes the button's text color */
.spinner {
  position: absolute;
  inset: 0;
  width: 1em;
  height: 1em;
  margin: auto;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* People who asked their system for less motion get a slower turn */
@media (prefers-reduced-motion: reduce) {
  .spinner {
    animation-duration: 2s;
  }
}
</style>