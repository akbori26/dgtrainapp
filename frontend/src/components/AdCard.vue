<script setup>
// One ad on the message board. Read only for now; selecting and solving come in step 15.
defineProps({
  ad: { type: Object, required: true }, // AdView from the API
})
</script>

<template>
  <article class="card ad" :class="{ greyed: !ad.solvable }">
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
    </div>

    <p class="message">{{ ad.message }}</p>
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

/* Expires after this turn: worth noticing */
.last-turn {
  color: var(--warning);
  font-weight: 600;
}

.message {
  margin: 0.3rem 0 0;
}
</style>
