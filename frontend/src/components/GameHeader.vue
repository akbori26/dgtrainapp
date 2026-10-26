<script setup>
import { onMounted } from 'vue'
import { store, newGame, resumeGame, loadSavedGame } from '../store.js'

// When the page opens (or is reloaded), come back to the game we were playing.
onMounted(loadSavedGame)
</script>

<template>
  <header class="header">
    <h1>🐉 Dragon Trainer</h1>

    <!-- On a phone only the title and New game fit (the last game comes back by itself on load) -->
    <span v-if="store.gameId" class="muted wide-only">
      Game: <strong>{{ store.gameId }}</strong> · Turn {{ store.status.turn }}
    </span>
    <span v-else class="muted wide-only">No game yet. Start a new one.</span>

    <div class="header-buttons">
      <button class="secondary wide-only" @click="resumeGame">Resume</button>
      <button @click="newGame">New game</button>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0.5rem 1.5rem;
  padding: 0.75rem 0;
}

.header h1 {
  font-size: 1.4rem;
}

.header-buttons {
  display: flex;
  gap: 0.5rem;
  margin-left: auto;
}

@media (max-width: 640px) {
  .header {
    padding: 0.5rem 0;
  }

  .header h1 {
    font-size: 1.15rem;
  }
}
</style>