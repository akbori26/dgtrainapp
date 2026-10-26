// The shared game state. Every component imports `store` and the action functions from here,
// so no data has to be passed down from component to component.
//
// Only the game ID and the last reputation are kept in the browser (localStorage);
// everything else is loaded again from the API, which keeps the game in SQLite.

import { reactive } from 'vue'
import * as api from './api.js'

const SAVED_GAME_ID = 'dragonTrainer.gameId'
const SAVED_REPUTATION = 'dragonTrainer.reputation'

export const store = reactive({
  gameId: null,
  status: null, // GameStatusView: turn, lives, gold, level, score, highScore, lifeState, warning
  ads: [], // AdView list
  shop: [], // ShopItemView list
  history: [], // HistoryEntryView list, newest first
  reputation: null, // { gameId, turn, people, state, underworld }
  lastMessage: null, // { success, text } of the latest action
  lastError: null, // { message, code, status } of the latest failed request
  openDrawer: null, // 'board', 'shop', 'history' or null
  pinned: false, // true when the open drawer was clicked, not just hovered
  selectedAdId: null,
  busy: false, // true while a request is running
  busyAction: null, // which button started it, e.g. 'investigate' or 'solve:<adId>', so it can show "…"
  gameLost: false, // true when the Mugloar server no longer knows the game (it forgets old games)
})

// The Mugloar server's answers that mean the game can't be played any more
const LOST_GAME_CODES = ['GAME_NOT_FOUND', 'GAME_EXPIRED']

// Runs one request (or a few in a row) for a button.
// While it runs, `busy` is true so the page can disable its buttons and a double click sends nothing twice;
// `busyAction` names the button, which shows "…" meanwhile.
// A failure goes to `lastError`; a success clears the previous error.
async function run(key, action) {
  if (store.busy) return
  store.busy = true
  store.busyAction = key
  try {
    await action()
    store.lastError = null
  } catch (error) {
    store.lastError = { message: error.message, code: error.code, status: error.status }
    if (LOST_GAME_CODES.includes(error.code)) store.gameLost = true
  } finally {
    store.busy = false
    store.busyAction = null
  }
}

// Game over: no lives left, or the server has forgotten the game.
// The board, shop and history can still be read; only the actions stop.
export function isGameOver() {
  return store.status?.lifeState === 'GAME_OVER' || store.gameLost
}

// Solve, Buy, Investigate and Download ads are allowed only with a playable game and no request running
export function canPlay() {
  return Boolean(store.gameId) && !store.busy && !isGameOver()
}

// ---------- Game ----------

export function newGame() {
  return run('newGame', async () => {
    showGame(await api.startGame())
    store.lastMessage = { success: true, text: 'A new game has started. Download the ads to begin.' }
    await reloadLists()
  })
}

// Resumes the last game started (GET /api/games/current).
export function resumeGame() {
  return run('resume', async () => {
    showGame(await api.getCurrentGame())
    await reloadLists()
  })
}

// On page load: come back to the game saved in this browser, or else to the server's current game.
// Having no game yet is not an error, so nothing is shown in the error pane then.
export function loadSavedGame() {
  return run('load', async () => {
    const savedId = readSaved(SAVED_GAME_ID)
    let status = null
    if (savedId) {
      status = await api.getStatus(savedId).catch(() => null)
    }
    if (!status) {
      status = await api.getCurrentGame().catch(() => null)
    }
    if (status) {
      showGame(status)
      await reloadLists()
    }
  })
}

// Makes `status` the game shown on the page. A different game starts with empty lists.
function showGame(status) {
  if (status.gameId !== store.gameId) {
    store.ads = []
    store.shop = []
    store.history = []
    store.selectedAdId = null
    store.lastMessage = null
    store.gameLost = false
    const saved = readSaved(SAVED_REPUTATION)
    store.reputation = saved?.gameId === status.gameId ? saved : null
  }
  store.gameId = status.gameId
  store.status = status
  save(SAVED_GAME_ID, status.gameId)
}

// Loads the stored board and history again, and the shop if it has been opened.
// The turn and gold change after every action, so turns left and `affordable` change too.
async function reloadLists() {
  const gameId = store.gameId
  const [ads, history, shop] = await Promise.all([
    api.getAds(gameId),
    api.getHistory(gameId),
    store.shop.length > 0 ? api.getShop(gameId) : store.shop,
  ])
  store.ads = ads
  store.history = history
  store.shop = shop
}

// ---------- Message board ----------

export function downloadAds() {
  return run('download', async () => {
    store.ads = await api.refreshAds(store.gameId)
    store.selectedAdId = null
  })
}

// Clicking the selected ad again unselects it
export function selectAd(adId) {
  store.selectedAdId = store.selectedAdId === adId ? null : adId
}

export function solveAd(adId) {
  return run('solve:' + adId, async () => {
    const { outcome, status } = await api.solveAd(store.gameId, adId)
    store.status = status
    store.selectedAdId = null
    store.lastMessage = {
      success: outcome.success,
      text: outcome.success ? `${outcome.message} +${outcome.reward} gold` : outcome.message,
    }
    await reloadLists()
  })
}

// ---------- Reputation ----------

export function investigate() {
  return run('investigate', async () => {
    const { outcome, status } = await api.investigate(store.gameId)
    store.status = status
    store.reputation = { gameId: store.gameId, turn: status.turn, ...outcome }
    save(SAVED_REPUTATION, store.reputation)
    store.lastMessage = { success: true, text: 'You investigated your reputation. It cost a turn.' }
    await reloadLists()
  })
}

// ---------- Shop ----------

export function loadShop() {
  return run('shop', async () => {
    store.shop = await api.getShop(store.gameId)
  })
}

export function buyItem(itemId) {
  return run('buy:' + itemId, async () => {
    const { outcome, status } = await api.buyItem(store.gameId, itemId)
    store.status = status
    // The server's message already names the cost: "Bought Healing potion for 50 gold"
    store.lastMessage = { success: outcome.success, text: outcome.message }
    await reloadLists()
  })
}

// ---------- Drawers ----------
// The panels that slide in from the right. `key` is what store.openDrawer holds;
// `icon` is shown on the phone's bottom tab bar.
export const drawers = [
  { key: 'board', tab: 'Board', title: 'Message board', icon: '📜' },
  { key: 'shop', tab: 'Shop', title: 'Shop', icon: '🛒' },
  { key: 'history', tab: 'History', title: 'History', icon: '📖' },
]

// Hovering a tab opens its drawer; it closes shortly after the mouse leaves both the tab and the drawer.
// Clicking a tab pins the drawer open until the tab is clicked again or ✕ is pressed.
const CLOSE_DELAY_MS = 300
let closeTimer = null

export function hoverDrawer(key) {
  clearTimeout(closeTimer)
  // A pinned drawer stays, so passing over another tab doesn't swap it away
  if (!store.pinned) store.openDrawer = key
}

// The mouse is on the open drawer: don't close it
export function keepDrawerOpen() {
  clearTimeout(closeTimer)
}

export function leaveDrawer() {
  if (store.pinned) return
  clearTimeout(closeTimer)
  closeTimer = setTimeout(() => {
    if (!store.pinned) store.openDrawer = null
  }, CLOSE_DELAY_MS)
}

export function clickTab(key) {
  if (store.openDrawer === key && store.pinned) {
    closeDrawer()
  } else {
    clearTimeout(closeTimer)
    store.openDrawer = key
    store.pinned = true
  }
}

export function closeDrawer() {
  clearTimeout(closeTimer)
  store.openDrawer = null
  store.pinned = false
}

// ---------- Errors ----------

export function dismissError() {
  store.lastError = null
}

// ---------- localStorage ----------
// The browser may refuse storage (e.g. a private window), so the page works without it.

function save(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value))
  } catch {
    // Not saved: after a reload the page asks the server for the current game instead.
  }
}

function readSaved(key) {
  try {
    return JSON.parse(localStorage.getItem(key))
  } catch {
    return null
  }
}

// In development, `store` can be looked at from the browser console: window.store
// Only the first copy is kept: after an edit Vite may load this file again under another URL,
// and window.store must stay the one the page uses.
if (import.meta.env?.DEV && !window.store) {
  window.store = store
}