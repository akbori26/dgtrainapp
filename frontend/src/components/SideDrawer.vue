<script setup>
import { computed } from 'vue'
import { store, drawers, keepDrawerOpen, leaveDrawer, closeDrawer } from '../store.js'

const drawer = computed(() => drawers.find((d) => d.key === store.openDrawer))

// Like the tabs: only a mouse keeps a hovered drawer open or lets it close by leaving
function onEnter(event) {
  if (event.pointerType === 'mouse') keepDrawerOpen()
}

function onLeave(event) {
  if (event.pointerType === 'mouse') leaveDrawer()
}
</script>

<template>
  <!-- The open drawer slides in from the right, over the panes but below the Player bar -->
  <Transition name="slide">
    <aside v-if="drawer" class="drawer" @pointerenter="onEnter" @pointerleave="onLeave">
      <header class="drawer-header">
        <h2>{{ drawer.title }}</h2>
        <button class="secondary close" title="Close" @click="closeDrawer">✕</button>
      </header>

      <!-- Filled in by the next steps: board (14), shop (16), history (17) -->
      <p class="muted">The {{ drawer.title.toLowerCase() }} comes here.</p>
    </aside>
  </Transition>
</template>

<style scoped>
.drawer {
  position: absolute;
  top: 0;
  right: 0;
  z-index: 5;
  width: min(640px, 100%);
  min-height: 100%;
  max-height: calc(100vh - 9rem);
  overflow-y: auto;
  padding: 0.75rem 1rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: -4px 0 16px rgb(0 0 0 / 0.25);
}

.drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.75rem;
}

.drawer-header h2 {
  font-size: 1.1rem;
}

.close {
  padding: 0.2rem 0.6rem;
}

/* Slide in from the right, and back out */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(40px);
  opacity: 0;
}

/* Phone: covers the whole content area, above the bottom tab bar */
@media (max-width: 640px) {
  .drawer {
    width: 100%;
    max-height: calc(100vh - 11rem);
  }
}
</style>