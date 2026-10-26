<script setup>
import { computed } from 'vue'
import { store, buyItem } from '../store.js'

// One item in the shop. The server decides `affordable`; we only show how much gold is missing.
const props = defineProps({
  item: { type: Object, required: true }, // ShopItemView from the API
})

const missingGold = computed(() => props.item.cost - (store.status?.gold ?? 0))
</script>

<template>
  <article class="card item" :class="{ greyed: !item.affordable }">
    <!-- Name and cost side by side; on a phone the cost goes below the name -->
    <div class="info">
      <span class="name">{{ item.name }}</span>
      <span class="gold">{{ item.cost }} gold</span>
    </div>

    <div class="buy">
      <button :disabled="!item.affordable" @click="buyItem(item.itemId)">Buy</button>
      <span v-if="!item.affordable && missingGold > 0" class="missing">you need {{ missingGold }} more gold</span>
    </div>
  </article>
</template>

<style scoped>
.item {
  display: flex;
  align-items: center;
  gap: 1.25rem;
}

.info {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.25rem 1.25rem;
}

/* Hovering highlights an item, like an ad on the board */
.item:hover {
  border-color: var(--primary);
}

.item.greyed:hover {
  border-color: var(--border);
}

.name {
  font-weight: 600;
}

.buy {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 0.15rem;
  min-width: 5rem;
}

.missing {
  font-size: 0.8rem;
  white-space: nowrap;
}

@media (max-width: 640px) {
  .info {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>