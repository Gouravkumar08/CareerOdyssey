/**
 * A wallpaper photo chosen from the user's computer, kept in IndexedDB (localStorage is too small
 * for photos). Stays in this browser only.
 */
const DB_NAME = 'wallpaper'
const STORE = 'files'
const KEY = 'uploaded'

export const MAX_UPLOAD_BYTES = 25 * 1024 * 1024

function openDb() {
  return new Promise<IDBDatabase>((resolve, reject) => {
    const request = indexedDB.open(DB_NAME, 1)
    request.onupgradeneeded = () => request.result.createObjectStore(STORE)
    request.onsuccess = () => resolve(request.result)
    request.onerror = () => reject(request.error ?? new Error('Browser storage is unavailable'))
  })
}

async function run<T>(mode: IDBTransactionMode, action: (store: IDBObjectStore) => IDBRequest<T>) {
  const db = await openDb()
  try {
    return await new Promise<T>((resolve, reject) => {
      const request = action(db.transaction(STORE, mode).objectStore(STORE))
      request.onsuccess = () => resolve(request.result)
      request.onerror = () => reject(request.error ?? new Error('Browser storage failed'))
    })
  } finally {
    db.close()
  }
}

/** Returns an error message if the file can't be used as a wallpaper. */
export function validateUpload(file: File): string | null {
  if (!file.type.startsWith('image/')) return 'Choose an image file (JPG, PNG, WebP…).'
  if (file.size > MAX_UPLOAD_BYTES) return `That image is ${(file.size / 1024 / 1024).toFixed(0)} MB; please choose one under 25 MB.`
  return null
}

export async function saveUploadedWallpaper(file: Blob) {
  await run('readwrite', (store) => store.put(file, KEY))
}

export async function loadUploadedWallpaper(): Promise<Blob | null> {
  const blob = await run<Blob | undefined>('readonly', (store) => store.get(KEY))
  return blob ?? null
}

export async function deleteUploadedWallpaper() {
  await run('readwrite', (store) => store.delete(KEY))
}

let currentObjectUrl: string | null = null

/** An object URL for the stored photo; the previous one is released so memory isn't leaked. */
export async function uploadedWallpaperUrl(): Promise<string> {
  const blob = await loadUploadedWallpaper()
  if (!blob) throw new Error('No photo from your computer is saved yet — choose one in the Wallpaper settings')
  if (currentObjectUrl) URL.revokeObjectURL(currentObjectUrl)
  currentObjectUrl = URL.createObjectURL(blob)
  return currentObjectUrl
}
