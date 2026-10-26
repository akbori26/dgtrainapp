<script setup>
// The page layout. Each placeholder below becomes its own component in the next steps:
// GameHeader (7), PlayerBar (8), LastMessagePane (9), ErrorPane (10), ReputationPane (11),
// SideTabs + SideDrawer (12).
</script>

<template>
  <div class="app">
    <!-- Header: title, current game, Resume and New game (step 7) -->
    <header class="header">
      <h1>🐉 Dragon Trainer</h1>
      <span class="muted">Game: — · Turn —</span>
      <div class="header-buttons">
        <button class="secondary">Resume</button>
        <button>New game</button>
      </div>
    </header>

    <!-- 1 Player: always visible (step 8) -->
    <section class="player-bar">
      <span><strong>SCORE</strong> —</span>
      <span>❤️ — lives</span>
      <span class="gold">🪙 — gold</span>
      <span>🏅 Level —</span>
      <span class="muted">Best —</span>
    </section>

    <div class="body">
      <!-- Content area: the three panes that are always visible -->
      <main class="content">
        <!-- 6 Last message (step 9) -->
        <section class="pane">
          <h2 class="pane-title">Last message</h2>
          <p class="muted">The outcome of your last action shows here.</p>
        </section>

        <!-- 4 Reputation (step 11) -->
        <section class="pane">
          <h2 class="pane-title">Reputation</h2>
          <p class="muted">People — · State — · Underworld —</p>
          <button>Investigate</button>
        </section>

        <!-- 7 Server errors (step 10) -->
        <section class="pane">
          <h2 class="pane-title">Server errors</h2>
          <p class="muted">No errors.</p>
        </section>
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

.header {
  display: flex;
  align-items: center;
  gap: 1.5rem;
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

/* Stays at the top while the page scrolls, so score, lives and gold are always on screen */
.player-bar {
  position: sticky;
  top: 0;
  z-index: 10;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 2rem;
  padding: 0.6rem 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow);
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