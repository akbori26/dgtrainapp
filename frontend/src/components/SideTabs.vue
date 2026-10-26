<script setup>
import { store, drawers, hoverDrawer, leaveDrawer, clickTab } from '../store.js'
</script>

<template>
  <!-- << tab rail: hover a tab to open its drawer, click to pin it open -->
  <nav class="tab-rail">
    <button
      v-for="drawer in drawers"
      :key="drawer.key"
      class="tab"
      :class="{ open: store.openDrawer === drawer.key, pinned: store.pinned && store.openDrawer === drawer.key }"
      :aria-expanded="store.openDrawer === drawer.key"
      @mouseenter="hoverDrawer(drawer.key)"
      @mouseleave="leaveDrawer"
      @click="clickTab(drawer.key)"
    >
      &lt;&lt;<br />{{ drawer.tab }}
    </button>
  </nav>
</template>

<style scoped>
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

/* The tab whose drawer is showing */
.tab.open {
  border-color: var(--primary);
  color: var(--primary);
}

/* Clicked: stays open until clicked again or ✕ */
.tab.pinned {
  background: var(--primary);
  color: var(--primary-text);
}
</style>