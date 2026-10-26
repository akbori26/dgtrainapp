<script setup>
import { computed } from 'vue'
import { store, selectAd, solveAd, canPlay } from '../store.js'
import BusyLabel from './BusyLabel.vue'

// One ad on the message board. Click it to select it; the selected ad shows a Solve button.
const props = defineProps({
  ad: { type: Object, required: true }, // AdView from the API
})

const selected = computed(() => store.selectedAdId === props.ad.adId)

// Used and expired ads can't be selected
function select() {
  if (props.ad.solvable) selectAd(props.ad.adId)
}
</script>

<template>
  <article
    class="card ad"
    :class="{ greyed: !ad.solvable, selectable: ad.solvable, selected }"
    :tabindex="ad.solvable ? 0 : -1"
    :aria-selected="selected"
    @click="select"
    @keydown.enter="select"
  >
    <div class="ad-top">
      <span :class="['risk', 'risk-' + ad.risk.code]" :title="ad.probability">
        {{ ad.risk.icon }} {{ ad.risk.label }}
      </span>
      <span class="gold">{{ ad.reward }} gold</span>

      <span v-if="ad.used" class="state">USED</span>
      <span v-else-if="ad.expired" class="state">EXPIRED</span>
      <span v-else :class="{ 'last-turn': ad.turnsLeft === 1 }">
        ⏳ {{ ad.turnsLeft }} {{ ad.turnsLeft === 1 ? 'turn' : 'turns' }} left
      </span>
      <!-- On a phone there's no room; the red border and the Solve button show the selection -->
      <span v-if="selected" class="state selected-label wide-only">SELECTED</span>
    </div>

    <p class="message">{{ ad.message }}</p>

    <!-- Only on the selected ad, and only while it can still be solved.
         .stop: the click solves, it doesn't also unselect the card. -->
    <div v-if="selected && ad.solvable" class="actions">
      <button :disabled="!canPlay()" @click.stop="solveAd(ad.adId)">
        <BusyLabel :busy="store.busyAction === 'solve:' + ad.adId">Solve</BusyLabel>
      </button>
    </div>
  </article>
</template>

<style scoped>
.ad-top {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.25rem 1.25rem;
  font-size: 0.9rem;
}

.state {
  margin-left: auto;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.selected-label {
  color: var(--primary);
}

/* Expires after this turn: worth noticing */
.last-turn {
  color: var(--warning);
  font-weight: 600;
}

.message {
  margin: 0.3rem 0 0;
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.card:focus-visible {
  outline: 2px solid var(--gold);
  outline-offset: 2px;
}
</style>