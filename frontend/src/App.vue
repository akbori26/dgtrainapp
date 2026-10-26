<script setup>
// The page layout: header, Player bar, then the panes with the drawers and their << tabs.
import { store } from './store.js'
import GameHeader from './components/GameHeader.vue'
import PlayerBar from './components/PlayerBar.vue'
import LastMessagePane from './components/LastMessagePane.vue'
import ReputationPane from './components/ReputationPane.vue'
import ErrorPane from './components/ErrorPane.vue'
import SideTabs from './components/SideTabs.vue'
import SideDrawer from './components/SideDrawer.vue'
</script>

<template>
  <div class="app">
    <GameHeader />
    <PlayerBar />

    <div class="body">
      <!-- The drawer opens over this area, so it never covers the Player bar or the tabs -->
      <div class="stage">
        <!-- Content area: the three panes that are always visible, dimmed while a drawer is open -->
        <main class="content" :class="{ dimmed: store.openDrawer }">
          <LastMessagePane />

          <ReputationPane />

          <ErrorPane />
        </main>

        <SideDrawer />
      </div>

      <SideTabs />
    </div>
  </div>
</template>

<style scoped>
.app {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 1rem 1rem;
}

/* Content on the left, tab rail on the right */
.body {
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
}

.stage {
  position: relative;
  flex: 1;
  min-width: 0;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  transition: opacity 0.2s;
}

.content.dimmed {
  opacity: 0.4;
}
</style>