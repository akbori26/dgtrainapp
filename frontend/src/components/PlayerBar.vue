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
      <span class="score"><strong class="wide-only">SCORE</strong> —<span class="phone-only"> pts</span></span>
      <span>❤️<span class="wide-only"> — lives</span></span>
      <span class="gold">🪙 —<span class="wide-only"> gold</span></span>
      <span>🏅 <span class="wide-only">Level</span> —</span>
      <span class="muted wide-only">Best —</span>
    </div>

    <!-- On a phone the words are hidden (.wide-only): "120 pts ❤️❤️❤️ 🪙 85 🏅 1" -->
    <div v-else class="stats">
      <span class="score">
        <strong class="wide-only">SCORE</strong> {{ store.status.score }}<span class="phone-only"> pts</span>
      </span>
      <span>
        <span class="hearts">{{ hearts }}</span>
        <span class="wide-only">{{ store.status.lives }} {{ store.status.lives === 1 ? 'life' : 'lives' }}</span>
      </span>
      <span class="gold">🪙 {{ store.status.gold }}<span class="wide-only"> gold</span></span>
      <span>🏅 <span class="wide-only">Level</span> {{ store.status.level }}</span>
      <span class="muted wide-only">Turn {{ store.status.turn }}</span>
      <span class="muted wide-only">Best {{ store.status.highScore }}</span>
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

/* Phone: one compact row */
@media (max-width: 640px) {
  .player-bar {
    padding: 0.4rem 0.75rem;
  }

  .stats {
    gap: 0.25rem 1rem;
  }

  .score {
    font-size: 1rem;
    font-weight: 700;
  }

  .warning,
  .game-over-text {
    font-size: 0.85rem;
  }
}
</style>