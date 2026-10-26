<script setup>
import { computed } from 'vue'
import { store, downloadAds, canPlay } from '../store.js'
import AdCard from './AdCard.vue'
import BusyLabel from './BusyLabel.vue'

const solvableCount = computed(() => store.ads.filter((ad) => ad.solvable).length)
</script>

<template>
  <!-- 2 Message board: the stored ads of the game, in board order -->
  <div class="board">
    <div class="toolbar">
      <span class="muted">
        <template v-if="store.ads.length">{{ solvableCount }} of {{ store.ads.length }} ads can be solved</template>
      </span>
      <button :disabled="!canPlay()" @click="downloadAds">
        <BusyLabel :busy="store.busyAction === 'download'">Download ads</BusyLabel>
      </button>
    </div>

    <p v-if="!store.gameId" class="muted">Start a game to see the message board.</p>
    <p v-else-if="!store.ads.length" class="muted">No ads yet. Download ads to fill the board.</p>

    <div class="ads">
      <AdCard v-for="ad in store.ads" :key="ad.adId" :ad="ad" />
    </div>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 0.75rem;
  font-size: 0.9rem;
}

.ads {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

p {
  margin: 0;
}
</style>
