import { uploadedWallpaperUrl } from './wallpaperUpload'

export type WallpaperFrequency = 'load' | 'daily'

/** Where the photo comes from: a link, or a photo the user chose from their computer. */
export type WallpaperMode = 'link' | 'upload'

export type WallpaperSettings = {
  enabled: boolean
  mode: WallpaperMode
  /** An Unsplash search/collection link, or a direct image link. */
  source: string
  frequency: WallpaperFrequency
  /** Optional free Unsplash access key; without it, photos come from a keyword image service. */
  unsplashKey: string
  /** How much the photo is blurred, 0–20 (px), so content on top stays readable. */
  blur: number
  /** How solid cards and tiles are, 15–100 (%); lower lets more of the photo show through. */
  glass: number
}

export const DEFAULT_WALLPAPER_SETTINGS: WallpaperSettings = {
  enabled: true,
  mode: 'link',
  source:'https://unsplash.com/s/photos/landscape',
  frequency: 'daily',
  unsplashKey: '',
  blur: 4,
  glass: 60,
}

const SETTINGS_KEY = 'wallpaper-settings'
// v2: entries from earlier builds could point at photo links that never loaded.
const DAILY_CACHE_KEY = 'wallpaper-daily-cache-v2'

/**
 * What an Unsplash page points at. Any of these can be empty (the home page, for instance), in which
 * case a random landscape photo is used. `keywords` is the page's own words, used when no key is set.
 */
type UnsplashTarget = {
  kind: 'unsplash'
  query?: string
  collection?: string
  topic?: string
  username?: string
  photoId?: string
  keywords?: string
}

type Source = { kind: 'image'; url: string } | UnsplashTarget

export function loadWallpaperSettings(): WallpaperSettings {
  try {
    const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) ?? 'null')
    if (saved && typeof saved.source === 'string') return { ...DEFAULT_WALLPAPER_SETTINGS, ...saved }
  } catch {
    // Unreadable or unavailable storage: use the defaults.
  }
  return DEFAULT_WALLPAPER_SETTINGS
}

export function saveWallpaperSettings(settings: WallpaperSettings) {
  try {
    localStorage.setItem(SETTINGS_KEY, JSON.stringify(settings))
  } catch {
    // Not remembered, but it still applies for this visit.
  }
}

function parseSource(raw: string): Source {
  let url: URL
  try {
    url = new URL(/^[a-z][a-z0-9+.-]*:/i.test(raw.trim()) ? raw.trim() : `https://${raw.trim()}`)
  } catch {
    throw new Error('Enter a full link, for example https://unsplash.com/s/photos/cars')
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') throw new Error('Only http and https links are supported')

  // images.unsplash.com / plus.unsplash.com serve the image file itself.
  if (/(^|\.)unsplash\.com$/.test(url.hostname) && !/^(images|plus)\./.test(url.hostname)) {
    return parseUnsplashPage(url.pathname)
  }
  return { kind: 'image', url: url.toString() }
}

const words = (slug: string) => slug.replace(/-/g, ' ').trim()

/** Understands search, topic, collection, user and single-photo pages; anything else still works. */
function parseUnsplashPage(pathname: string): UnsplashTarget {
  const [first, second, third] = pathname.split('/').filter(Boolean).map((part) => decodeURIComponent(part))
  if (first === 's' && second === 'photos' && third) return { kind: 'unsplash', query: words(third), keywords: words(third) }
  if (first === 'collections' && second) return { kind: 'unsplash', collection: second, keywords: third ? words(third) : undefined }
  if (first === 't' && second) return { kind: 'unsplash', topic: second, keywords: words(second) }
  if (first?.startsWith('@')) return { kind: 'unsplash', username: first.slice(1) }
  if (first === 'photos' && second) {
    // Photo pages end in the photo id: /photos/some-title-AbC123xyz
    return { kind: 'unsplash', photoId: second.split('-').pop(), keywords: words(second.split('-').slice(0, -1).join('-')) }
  }
  const last = [first, second, third].filter(Boolean).pop()
  return { kind: 'unsplash', keywords: last ? words(last) : undefined }
}

/** Returns an error message if the link can't be used as a wallpaper source. */
export function validateWallpaperSource(raw: string): string | null {
  try {
    parseSource(raw)
    return null
  } catch (err) {
    return (err as Error).message
  }
}

async function fromUnsplashApi(source: UnsplashTarget, key: string) {
  const params = new URLSearchParams({ client_id: key })
  let path = 'photos/random'
  if (source.photoId) {
    path = `photos/${encodeURIComponent(source.photoId)}`
  } else {
    params.set('orientation', 'landscape')
    if (source.query) params.set('query', source.query)
    if (source.collection) params.set('collections', source.collection)
    if (source.topic) params.set('topics', source.topic)
    if (source.username) params.set('username', source.username)
  }
  const response = await fetch(`https://api.unsplash.com/${path}?${params}`)
  if (!response.ok) throw new Error(`Unsplash responded ${response.status}`)
  const photo = await response.json()
  return `${photo.urls.raw}&w=1920&q=80&fit=crop&auto=format`
}

/** Keyless fallback: a random large landscape photo from Wikimedia Commons matching the search words. */
async function fromKeyword(query: string) {
  const params = new URLSearchParams({
    action: 'query',
    generator: 'search',
    gsrsearch: `${query} filetype:bitmap`,
    gsrnamespace: '6',
    gsrlimit: '50',
    gsroffset: String(Math.floor(Math.random() * 100)),
    prop: 'imageinfo',
    iiprop: 'url|size',
    iiurlwidth: '1920',
    format: 'json',
    origin: '*',
  })
  const response = await fetch(`https://commons.wikimedia.org/w/api.php?${params}`)
  if (!response.ok) throw new Error(`Image search responded ${response.status}`)
  const pages: Array<{ imageinfo?: Array<{ width: number; height: number; thumburl?: string }> }> = Object.values(
    (await response.json()).query?.pages ?? {},
  )
  const landscape = pages
    .map((page) => page.imageinfo?.[0])
    .filter((info) => info?.thumburl && info.width >= 1600 && info.width / info.height >= 1.3)
  if (landscape.length === 0) throw new Error(`No wallpaper-sized photos found for "${query}"`)
  return landscape[Math.floor(Math.random() * landscape.length)]!.thumburl!
}

/** Resolves once the browser has actually loaded the image, so a dead link is reported, not silently blank. */
export function preloadImage(url: string) {
  return new Promise<string>((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(url)
    image.onerror = () => reject(new Error('The photo could not be loaded from its link'))
    image.src = url
  })
}

/** Last resort so there is always a wallpaper: a random photo, not matched to any topic. */
function randomPhoto() {
  return `https://picsum.photos/seed/${Math.random().toString(36).slice(2)}/1920/1080`
}

async function fetchFresh(settings: WallpaperSettings): Promise<string> {
  const source = parseSource(settings.source)
  if (source.kind === 'image') return source.url
  if (settings.unsplashKey.trim()) {
    try {
      return await fromUnsplashApi(source, settings.unsplashKey.trim())
    } catch {
      // Bad key or rate limit: fall back to the keyword service below when we can.
    }
  }
  if (source.keywords) {
    try {
      return await preloadImage(await fromKeyword(source.keywords))
    } catch {
      // Topic search unreachable or empty: show a random photo instead of nothing.
    }
  }
  return randomPhoto()
}

const signature = (s: WallpaperSettings) => `${s.source.trim()}|${s.unsplashKey.trim() ? 'key' : 'nokey'}`

// One fetch per page load (and per source) no matter how many components ask.
let inflight: { sig: string; promise: Promise<string> } | null = null

export function clearWallpaperCache() {
  inflight = null
  try {
    localStorage.removeItem(DAILY_CACHE_KEY)
  } catch {
    // Nothing cached to clear.
  }
}

/**
 * The photo to show now. "daily" keeps one photo per calendar day (remembered across reloads);
 * "load" picks a new photo each time the app is opened or refreshed.
 */
export async function resolveWallpaper(settings: WallpaperSettings): Promise<string> {
  // A photo from the user's computer is always the same photo; no fetching or daily rotation.
  if (settings.mode === 'upload') return uploadedWallpaperUrl()

  const sig = signature(settings)
  const today = new Date().toLocaleDateString('en-CA')

  if (settings.frequency === 'daily') {
    try {
      const cached = JSON.parse(localStorage.getItem(DAILY_CACHE_KEY) ?? 'null')
      if (cached?.sig === sig && cached.day === today) return cached.url
    } catch {
      // Ignore a corrupt cache and fetch again.
    }
  }

  if (inflight?.sig !== sig) {
    inflight = { sig, promise: fetchFresh(settings) }
    inflight.promise.catch(() => {
      inflight = null
    })
  }
  const url = await inflight.promise
  if (settings.frequency === 'daily') {
    try {
      localStorage.setItem(DAILY_CACHE_KEY, JSON.stringify({ sig, day: today, url }))
    } catch {
      // Still shown this visit.
    }
  }
  return url
}
