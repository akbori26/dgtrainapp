<script setup>
import { store, drawers, hoverDrawer, leaveDrawer, clickTab } from '../store.js'

// Hover only means something for a mouse. A finger tap also sends "enter" and "leave" events,
// so on a touch screen only the click counts: tap to open, tap again to close.
function onEnter(event, key) {
  if (event.pointerType === 'mouse') hoverDrawer(key)
}

function onLeave(event) {
  if (event.pointerType === 'mouse') leaveDrawer()
}
</script>

<template>
  <!-- << tab rail: hover a tab to open its drawer, click to pin it open.
       On a phone it is a tab bar at the bottom of the screen. -->
  <nav class="tab-rail">
    <button
      v-for="drawer in drawers"
      :key="drawer.key"
      class="tab"
      :class="{ open: store.openDrawer === drawer.key, pinned: store.pinned && store.openDrawer === drawer.key }"
      :aria-expanded="store.openDrawer === drawer.key"
      @pointerenter="onEnter($event, drawer.key)"
      @pointerleave="onLeave"
      @click="clickTab(drawer.key)"
    >
      <!-- Desktop: "<<" points to where the drawer slides in from. Phone: an icon above the name. -->
      <span class="wide-only">&lt;&lt;<br /></span>
      <span class="phone-only icon" aria-hidden="true">{{ drawer.icon }}</span>
      {{ drawer.tab }}
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

/* Phone: a tab bar stuck to the bottom of the screen */
@media (max-width: 640px) {
  .tab-rail {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: 20;
    flex-direction: row;
    gap: 0;
    width: auto;
    border-top: 1px solid var(--border);
    background: var(--surface);
    box-shadow: 0 -2px 8px rgb(0 0 0 / 0.15);
  }

  .tab {
    flex: 1;
    padding: 0.4rem 0.25rem 0.5rem;
    border: none;
    border-radius: 0;
    font-size: 0.8rem;
  }

  .icon {
    display: block;
    font-size: 1.4rem;
    line-height: 1.2;
  }

  .tab + .tab {
    border-left: 1px solid var(--border);
  }
}
</style>