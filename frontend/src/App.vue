<script setup>
// The page layout. Each placeholder below becomes its own component in the next steps:
// ReputationPane (11), SideTabs + SideDrawer (12).
import GameHeader from './components/GameHeader.vue'
import PlayerBar from './components/PlayerBar.vue'
import LastMessagePane from './components/LastMessagePane.vue'
import ErrorPane from './components/ErrorPane.vue'
</script>

<template>
  <div class="app">
    <GameHeader />
    <PlayerBar />

    <div class="body">
      <!-- Content area: the three panes that are always visible -->
      <main class="content">
        <LastMessagePane />

        <!-- 4 Reputation (step 11) -->
        <section class="pane">
          <h2 class="pane-title">Reputation</h2>
          <p class="muted">People — · State — · Underworld —</p>
          <button>Investigate</button>
        </section>

        <ErrorPane />
      </main>

      <!-- << tab rail: each tab opens a drawer (step 12) -->
      <nav class="tab-rail">
        <button class="tab">&lt;&lt;<br />Board</button>
        <button class="tab">&lt;&lt;<br />Shop</button>
        <button class="tab">&lt;&lt;<br />History</button>
      </nav>
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

.content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.content p {
  margin: 0 0 0.5rem;
}

.tab-rail {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  width: 5.5rem;
}

.tab {
  padding: 1.2rem 0.5rem;
  border-color: var(--border);
  background: var(--surface);
  color: var(--text);
  line-height: 1.3;
}
</style>