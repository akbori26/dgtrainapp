<script setup>
import { computed } from 'vue'
import { store } from '../store.js'

const gameOver = computed(() => store.status?.lifeState === 'GAME_OVER')

// One heart per life. Healing potions can add lives, so very many are shown as a number only.
const hearts = computed(() => {
  const lives = store.status?.lives ?? 0
  return lives <= 10 ? '❤️'.repeat(lives) : '❤️ ×' + lives
})
</script>

<template>
  <section class="player-bar" :class="{ 'game-over': gameOver }">
    <!-- No game yet: show the layout with dashes -->
    <div v-if="!store.status" class="stats">
      <span><strong>SCORE</strong> —</span>
      <span>❤️ — lives</span>
      <span class="gold">🪙 — gold</span>
      <span>🏅 Level —</span>
      <span class="muted">Best —</span>
    </div>

    <div v-else class="stats">
      <span class="score"><strong>SCORE</strong> {{ store.status.score }}</span>
      <span>
        <span class="hearts">{{ hearts }}</span>
        {{ store.status.lives }} {{ store.status.lives === 1 ? 'life' : 'lives' }}
      </span>
      <span class="gold">🪙 {{ store.status.gold }} gold</span>
      <span>🏅 Level {{ store.status.level }}</span>
      <span class="muted">Turn {{ store.status.turn }}</span>
      <span class="muted">Best {{ store.status.highScore }}</span>
    </div>

    <!-- The server sends the warning text: 1 life left, or game over -->
    <p v-if="gameOver" class="game-over-text">💀 GAME OVER: {{ store.status.warning }}</p>
    <p v-else-if="store.status?.warning" class="warning">⚠️ {{ store.status.warning }}</p>
  </section>
</template>

<style scoped>
/* Stays at the top while the page scrolls, so score, lives and gold are always on screen */
.player-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  padding: 0.6rem 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
}

.stats {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 2rem;
}

.score {
  font-size: 1.15rem;
}

.hearts {
  letter-spacing: 0.1em;
}

.warning,
.game-over-text {
  margin: 0.5rem 0 0;
}

/* Game over: the whole bar turns red */
.player-bar.game-over {
  border-color: var(--danger);
  background: var(--danger-bg);
}

.game-over-text {
  font-weight: 700;
  color: var(--danger);
}
</style>