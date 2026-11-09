import { useCallback, useEffect, useState } from 'react'
import {
  clearWallpaperCache,
  loadWallpaperSettings,
  preloadImage,
  resolveWallpaper,
  saveWallpaperSettings,
  type WallpaperSettings,
} from '../utils/wallpaper'

export type WallpaperStatus = { state: 'off' | 'loading' | 'ready' | 'error'; message?: string }

type Result = { for: WallpaperSettings; url: string | null; error: string | null }

export function useWallpaper() {
  const [settings, setSettings] = useState(loadWallpaperSettings)
  const [result, setResult] = useState<Result | null>(null)

  useEffect(() => {
    if (!settings.enabled) return
    let cancelled = false
    resolveWallpaper(settings)
      .then(preloadImage)
      .then((url) => {
        if (!cancelled) setResult({ for: settings, url, error: null })
      })
      .catch((err: Error) => {
        // Keep showing the previous photo (if any) and say why the new one failed.
        if (!cancelled) setResult((prev) => ({ for: settings, url: prev?.url ?? null, error: err.message }))
      })
    return () => {
      cancelled = true
    }
  }, [settings])

  /** `newPhoto` throws away today's remembered photo so the next one is fetched right away. */
  const save = useCallback((next: WallpaperSettings, newPhoto = false) => {
    if (newPhoto) clearWallpaperCache()
    saveWallpaperSettings(next)
    setSettings({ ...next })
  }, [])

  const status: WallpaperStatus = !settings.enabled
    ? { state: 'off' }
    : result?.for !== settings
      ? { state: 'loading' }
      : result.error
        ? { state: 'error', message: result.error }
        : { state: 'ready' }

  return { settings, imageUrl: settings.enabled ? (result?.url ?? null) : null, status, save }
}
