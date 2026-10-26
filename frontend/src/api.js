// Every call to the Spring REST API goes through this file, one function per endpoint.
// The paths start with /api, so in development Vite forwards them to Spring (see vite.config.js).

const BASE = '/api/games'

// Sends one request and returns the JSON answer.
// When the server answers with an error (a ProblemDetail), it throws an Error that carries
// the server's `detail` as its message, plus its `code` and `status`, so the page can show it.
async function request(method, path) {
  let response
  try {
    response = await fetch(BASE + path, {
      method,
      headers: { Accept: 'application/json' },
    })
  } catch {
    // fetch only throws when no answer came back at all, e.g. Spring isn't running.
    throw apiError('Cannot reach the server. Is the Spring app running?', 'NETWORK_ERROR', 0)
  }

  const body = await readJson(response)

  if (!response.ok) {
    throw apiError(
      body?.detail ?? body?.title ?? `Request failed (${response.status})`,
      body?.code ?? 'UNKNOWN_ERROR',
      response.status,
    )
  }
  return body
}

// An error answer from Vite's proxy (e.g. Spring stopped) may not be JSON, so read it carefully.
async function readJson(response) {
  const text = await response.text()
  if (!text) return null
  try {
    return JSON.parse(text)
  } catch {
    return null
  }
}

function apiError(message, code, status) {
  const error = new Error(message)
  error.code = code
  error.status = status
  return error
}

// Game ID, ad ID and item ID go into the URL, so encode them.
const id = encodeURIComponent

// Game: answer the game status (GameStatusView)
export const startGame = () => request('POST', '')
export const getCurrentGame = () => request('GET', '/current')
export const getStatus = (gameId) => request('GET', `/${id(gameId)}`)

// Message board: answer a list of ads (AdView)
export const getAds = (gameId) => request('GET', `/${id(gameId)}/ads`)
export const refreshAds = (gameId) => request('POST', `/${id(gameId)}/ads/refresh`)

// Actions: answer { outcome, status }
export const solveAd = (gameId, adId) => request('POST', `/${id(gameId)}/ads/${id(adId)}/solve`)
export const investigate = (gameId) => request('POST', `/${id(gameId)}/reputation`)
export const buyItem = (gameId, itemId) => request('POST', `/${id(gameId)}/shop/${id(itemId)}/buy`)

// Shop: answers a list of items (ShopItemView)
export const getShop = (gameId) => request('GET', `/${id(gameId)}/shop`)

// History: answers missions and purchases, newest first (HistoryEntryView)
export const getHistory = (gameId) => request('GET', `/${id(gameId)}/history`)
