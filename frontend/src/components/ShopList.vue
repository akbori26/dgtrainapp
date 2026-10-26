<script setup>
import { watch } from 'vue'
import { store, loadShop } from '../store.js'
import ShopItem from './ShopItem.vue'

// The shop is loaded the first time the drawer opens for a game (the server downloads it once per game).
// After that, every action reloads it, so `affordable` follows the gold.
watch(
  () => store.gameId,
  (gameId) => {
    if (gameId && !store.shop.length) loadShop()
  },
  { immediate: true },
)
</script>

<template>
  <!-- 3 Shop: Buy is active only when the player can afford the item -->
  <div class="shop">
    <p v-if="!store.gameId" class="muted">Start a game to see the shop.</p>
    <p v-else-if="!store.shop.length" class="muted">{{ store.busy ? 'Loading the shop…' : 'The shop is empty.' }}</p>

    <div class="items">
      <ShopItem v-for="item in store.shop" :key="item.itemId" :item="item" />
    </div>
  </div>
</template>

<style scoped>
.items {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}

p {
  margin: 0;
}
</style>