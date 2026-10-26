<script setup>
import { computed } from 'vue'
import { store, investigate, canPlay } from '../store.js'
import BusyLabel from './BusyLabel.vue'

// The three groups the server reports, in the order they are shown
const groups = [
  { key: 'people', label: 'People' },
  { key: 'state', label: 'State' },
  { key: 'underworld', label: 'Underworld' },
]

const rep = computed(() => store.reputation)

// +1.5, -2, 0
function signed(value) {
  return value > 0 ? '+' + value : String(value)
}

function colorClass(value) {
  if (value > 0) return 'plus'
  if (value < 0) return 'minus'
  return 'muted'
}
</script>

<template>
  <!-- 4 Reputation: the last result, and the button to check again -->
  <section class="pane">
    <h2 class="pane-title">Reputation</h2>

    <div class="values">
      <span v-for="group in groups" :key="group.key">
        {{ group.label }}
        <strong v-if="rep" :class="colorClass(rep[group.key])">{{ signed(rep[group.key]) }}</strong>
        <strong v-else class="muted">—</strong>
      </span>
    </div>

    <div class="footer">
      <span class="muted">
        <template v-if="rep">Last checked at turn {{ rep.turn }}.</template>
        <template v-else>Not checked yet.</template>
        Investigating costs a turn.
      </span>
      <button :disabled="!canPlay()" @click="investigate">
        <BusyLabel :busy="store.busyAction === 'investigate'">Investigate</BusyLabel>
      </button>
    </div>
  </section>
</template>

<style scoped>
.values {
  display: flex;
  flex-wrap: wrap;
  gap: 0.5rem 2.5rem;
  font-size: 1.05rem;
}

.footer {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 0.5rem 1rem;
  margin-top: 0.5rem;
  font-size: 0.9rem;
}
</style>