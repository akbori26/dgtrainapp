<script setup>
import { store } from '../store.js'

// The history is loaded with the game and again after every action (see reloadLists in store.js).

// "2026-10-26T14:03:51Z" -> "16:03" in the player's own time zone
function clock(time) {
  return new Date(time).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

// +59, -50, 0
function signed(gold) {
  return gold > 0 ? '+' + gold : String(gold)
}

function goldClass(gold) {
  if (gold > 0) return 'plus'
  if (gold < 0) return 'minus'
  return 'muted'
}
</script>

<template>
  <!-- 5 History: missions and purchases, newest first -->
  <div class="history">
    <p v-if="!store.gameId" class="muted">Start a game to see its history.</p>
    <p v-else-if="!store.history.length" class="muted">No missions or purchases yet.</p>

    <table v-else>
      <thead>
        <tr>
          <th>Time</th>
          <th>Type</th>
          <th class="wide-only">Ad / item</th>
          <th class="number">Gold</th>
          <th class="center">Result</th>
          <th>Message</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="entry in store.history" :key="entry.id">
          <td class="muted">{{ clock(entry.time) }}</td>
          <td>
            <!-- 🗡️ mission, 💰 purchase; on a phone only the icon -->
            <span v-if="entry.type === 'MISSION'" :title="'Mission' + (entry.risk ? ' · ' + entry.risk.label : '')">
              🗡️<span class="wide-only"> Mission</span>
            </span>
            <span v-else title="Purchase">💰<span class="wide-only"> Purchase</span></span>
          </td>
          <td class="wide-only id">{{ entry.targetId }}</td>
          <td class="number" :class="goldClass(entry.goldChange)">{{ signed(entry.goldChange) }}</td>
          <td class="center">{{ entry.success ? '✅' : '❌' }}</td>
          <td class="message">{{ entry.message }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
p {
  margin: 0;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.9rem;
}

th {
  padding: 0 0.5rem 0.4rem;
  border-bottom: 1px solid var(--border);
  font-size: 0.75rem;
  font-weight: 600;
  letter-spacing: 0.06em;
  text-align: left;
  text-transform: uppercase;
  color: var(--muted);
}

td {
  padding: 0.45rem 0.5rem;
  border-bottom: 1px solid var(--border);
  vertical-align: top;
  white-space: nowrap;
}

/* Only the message may wrap onto several lines */
td.message {
  white-space: normal;
}

.number {
  font-weight: 600;
  text-align: right;
}

.center {
  text-align: center;
}

.id {
  font-family: ui-monospace, monospace;
  font-size: 0.8rem;
}

@media (max-width: 640px) {
  th,
  td {
    padding-right: 0.3rem;
    padding-left: 0.3rem;
  }
}
</style>