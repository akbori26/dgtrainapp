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
import OutcomeToast from './components/OutcomeToast.vue'
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

    <!-- The result of a Solve or Buy, shown for a moment in the middle of the screen -->
    <OutcomeToast />
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
  /* The drawer starts its slide 40px to the right. Without clipping, a phone widens the whole page
     for that moment and the bottom tab bar ends up below the screen. */
  overflow-x: clip;
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

/* Phone: the tabs move to a bar at the bottom, so the content takes the full width
   and leaves room at the end for that bar */
@media (max-width: 640px) {
  .app {
    padding: 0 0.75rem 4.5rem;
  }

  .body {
    margin-top: 0.75rem;
  }
}
</style>