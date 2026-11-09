import { useState } from 'react'
import { Outlet } from 'react-router-dom'
import { useWallpaper } from '../hooks/useWallpaper'
import { Sidebar } from './Sidebar'
import { WallpaperDialog } from './WallpaperDialog'

export function Layout() {
  const { settings, imageUrl, status, save } = useWallpaper()
  const [wallpaperOpen, setWallpaperOpen] = useState(false)

  return (
    <div className="app-shell">
      <Sidebar onOpenWallpaper={() => setWallpaperOpen(true)} />
      <main className="app-content" style={{ '--glass': `${settings.glass}%` } as React.CSSProperties}>
        {imageUrl && (
          <div
            className="app-wallpaper"
            style={{ '--wallpaper': `url(${JSON.stringify(imageUrl)})`, '--wallpaper-blur': `${settings.blur}px` } as React.CSSProperties}
            aria-hidden="true"
          />
        )}
        <Outlet />
      </main>
      {wallpaperOpen && (
        <WallpaperDialog settings={settings} status={status} onSave={save} onClose={() => setWallpaperOpen(false)} />
      )}
    </div>
  )
}
