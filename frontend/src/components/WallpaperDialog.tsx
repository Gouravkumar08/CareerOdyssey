import { useEffect, useState } from 'react'
import type { WallpaperStatus } from '../hooks/useWallpaper'
import {
  validateWallpaperSource,
  type WallpaperFrequency,
  type WallpaperMode,
  type WallpaperSettings,
} from '../utils/wallpaper'
import { loadUploadedWallpaper, saveUploadedWallpaper, validateUpload } from '../utils/wallpaperUpload'

type WallpaperDialogProps = {
  settings: WallpaperSettings
  status: WallpaperStatus
  onSave: (next: WallpaperSettings, newPhoto: boolean) => void
  onClose: () => void
}

export function WallpaperDialog({ settings, status, onSave, onClose }: WallpaperDialogProps) {
  const [enabled, setEnabled] = useState(settings.enabled)
  const [source, setSource] = useState(settings.source)
  const [frequency, setFrequency] = useState<WallpaperFrequency>(settings.frequency)
  const [unsplashKey, setUnsplashKey] = useState(settings.unsplashKey)
  const [blur, setBlur] = useState(settings.blur)
  const [glass, setGlass] = useState(settings.glass)
  const [mode, setMode] = useState<WallpaperMode>(settings.mode)
  const [error, setError] = useState<string | null>(null)
  /** A photo picked in this dialog, saved only when the user presses Save. */
  const [pickedFile, setPickedFile] = useState<File | null>(null)
  /** Preview of the picked photo, or of the one already saved from an earlier visit. */
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let url: string | null = null
    let cancelled = false
    const show = (blob: Blob) => {
      url = URL.createObjectURL(blob)
      setPreviewUrl(url)
    }
    if (pickedFile) show(pickedFile)
    else
      loadUploadedWallpaper()
        .then((blob) => {
          if (blob && !cancelled) show(blob)
        })
        .catch(() => {
          // No saved photo to preview.
        })
    return () => {
      cancelled = true
      if (url) URL.revokeObjectURL(url)
    }
  }, [pickedFile])

  function pickFile(file: File | undefined) {
    if (!file) return
    const problem = validateUpload(file)
    if (problem) {
      setError(problem)
      return
    }
    setError(null)
    setPickedFile(file)
  }

  async function submitUpload() {
    if (enabled && !pickedFile && !previewUrl) {
      setError('Choose a photo from your computer first.')
      return
    }
    setSaving(true)
    try {
      if (pickedFile) await saveUploadedWallpaper(pickedFile)
    } catch (err) {
      setSaving(false)
      setError(`Couldn't save the photo in this browser: ${(err as Error).message}`)
      return
    }
    onSave({ ...settings, enabled, mode: 'upload', blur, glass }, false)
    onClose()
  }

  function submit(newPhoto: boolean) {
    if (mode === 'upload') {
      void submitUpload()
      return
    }
    let link = source.trim()
    let key = unsplashKey.trim()
    // A link pasted into the key box is the source link in the wrong field.
    if (/^https?:\/\//i.test(key)) {
      if (link) {
        setError('The access key box contains a link. Keep the link in "Photo source link" and leave the key empty, or paste your real key.')
        return
      }
      link = key
      key = ''
    }
    const problem = enabled ? (link ? validateWallpaperSource(link) : 'Paste your photo source link in the first box, for example https://unsplash.com/s/photos/cars') : null
    if (problem) {
      setError(problem)
      return
    }
    onSave({ enabled, mode: 'link', source: link, frequency, unsplashKey: key, blur, glass }, newPhoto)
    onClose()
  }

  return (
    <div className="modal-overlay" onMouseDown={onClose}>
      <form
        className="modal-dialog wallpaper-dialog"
        onMouseDown={(e) => e.stopPropagation()}
        onSubmit={(e) => {
          e.preventDefault()
          submit(false)
        }}
      >
        <h3>Wallpaper</h3>
        {error && <div className="banner banner-error">{error}</div>}
        {status.state === 'loading' && <p className="wallpaper-note">Fetching a photo…</p>}
        {status.state === 'ready' && <p className="wallpaper-note">✓ Wallpaper loaded.</p>}
        {status.state === 'error' && <div className="banner banner-error">Couldn&apos;t load a photo: {status.message}</div>}

        <label className="wallpaper-option">
          <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
          <span>Show a wallpaper across the app</span>
        </label>

        <div className="wallpaper-mode" role="radiogroup" aria-label="Photo comes from">
          <label className={`wallpaper-mode-option${mode === 'upload' ? ' active' : ''}`}>
            <input
              type="radio"
              name="wallpaper-mode"
              checked={mode === 'upload'}
              disabled={!enabled}
              onChange={() => {
                setMode('upload')
                setError(null)
              }}
            />
            <span>From my computer</span>
          </label>
          <label className={`wallpaper-mode-option${mode === 'link' ? ' active' : ''}`}>
            <input
              type="radio"
              name="wallpaper-mode"
              checked={mode === 'link'}
              disabled={!enabled}
              onChange={() => {
                setMode('link')
                setError(null)
              }}
            />
            <span>From a link</span>
          </label>
        </div>

        {mode === 'upload' ? (
          <div
            className="wallpaper-field"
            onDragOver={(e) => {
              if (enabled) e.preventDefault()
            }}
            onDrop={(e) => {
              e.preventDefault()
              if (enabled) pickFile(e.dataTransfer.files[0])
            }}
          >
            <span>Your photo</span>
            {previewUrl && <img className="wallpaper-preview" src={previewUrl} alt="Selected wallpaper preview" />}
            <label className={`btn btn-secondary wallpaper-upload-btn${!enabled ? ' disabled' : ''}`}>
              {previewUrl ? 'Choose a different photo…' : 'Choose a photo…'}
              <input
                type="file"
                accept="image/*"
                hidden
                disabled={!enabled}
                onChange={(e) => {
                  pickFile(e.target.files?.[0])
                  e.target.value = ''
                }}
              />
            </label>
            <small>{pickedFile ? `${pickedFile.name} — press Save to use it.` : 'JPG, PNG or WebP up to 25 MB. You can also drag a photo here.'}</small>
          </div>
        ) : (
          <>
        <label className="wallpaper-field">
          <span>Photo source link</span>
          <input
            type="text"
            value={source}
            disabled={!enabled}
            onChange={(e) => setSource(e.target.value)}
            placeholder="https://unsplash.com/s/photos/cars"
          />
          <small>Any Unsplash link (search, topic, collection, user or photo page), or a direct link to one image.</small>
        </label>

        <label className="wallpaper-field">
          <span>New photo</span>
          <select value={frequency} disabled={!enabled} onChange={(e) => setFrequency(e.target.value as WallpaperFrequency)}>
            <option value="daily">Once a day</option>
            <option value="load">Every time I open or refresh the app</option>
          </select>
        </label>

        <label className="wallpaper-field">
          <span>Unsplash access key (optional — a short code, not a link)</span>
          <input
            type="text"
            value={unsplashKey}
            disabled={!enabled}
            onChange={(e) => setUnsplashKey(e.target.value)}
            placeholder="Optional short code — not a link"
          />
          <small>With a key you get the exact Unsplash photos from your link. Without one, photos matching the link&apos;s words come from Wikimedia Commons, or a random photo if that can&apos;t be reached.</small>
        </label>
          </>
        )}

        <label className="wallpaper-field">
          <span>Blur: {blur === 0 ? 'off' : `${blur}px`}</span>
          <input type="range" min={0} max={20} step={1} value={blur} disabled={!enabled} onChange={(e) => setBlur(Number(e.target.value))} />
          <small>0 shows the photo sharp; higher softens it so text on top is easier to read.</small>
        </label>

        <label className="wallpaper-field">
          <span>Cards &amp; tiles: {glass}% solid</span>
          <input type="range" min={15} max={100} step={5} value={glass} onChange={(e) => setGlass(Number(e.target.value))} />
          <small>Lower makes cards, tiles and the calendar see-through so the wallpaper shows behind them.</small>
        </label>

        <p className="wallpaper-note">Saved in this browser only.</p>

        <div className="modal-actions">
          <button type="button" className="btn btn-secondary" onClick={onClose}>
            Cancel
          </button>
          {mode === 'link' && (
            <button type="button" className="btn btn-secondary" disabled={!enabled} onClick={() => submit(true)}>
              Save &amp; new photo
            </button>
          )}
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  )
}
