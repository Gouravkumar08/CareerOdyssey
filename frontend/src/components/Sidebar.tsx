import { NavLink } from 'react-router-dom'
import { ThemeToggle } from './ThemeToggle'
import {
  HomeIcon,
  UserIcon,
  BuildingIcon,
  SendIcon,
  CodeIcon,
  LayersIcon,
  CalendarIcon,
  CalendarDaysIcon,
  BookmarkIcon,
  ImageIcon,
} from './icons/NavIcons'

const navLinkClass = ({ isActive }: { isActive: boolean }) => (isActive ? 'active' : '')

const NAV_ITEMS = [
  { to: '/', label: 'Dashboard', icon: HomeIcon, end: true },
  { to: '/calendar', label: 'Calendar', icon: CalendarDaysIcon, end: false },
  { to: '/planner', label: 'Planner', icon: CalendarIcon, end: false },
  { to: '/dsa-tracker', label: 'DSA Tracker', icon: CodeIcon, end: true },
  { to: '/system-design', label: 'System Design', icon: LayersIcon, end: true },
  { to: '/resources', label: 'Free Resources', icon: BookmarkIcon, end: true },
  { to: '/outreach', label: 'Outreach', icon: SendIcon, end: false },
  { to: '/companies', label: 'Companies', icon: BuildingIcon, end: false },
  { to: '/profiles', label: 'Profiles', icon: UserIcon, end: false },
]

type SidebarProps = {
  onOpenWallpaper: () => void
}

export function Sidebar({ onOpenWallpaper }: SidebarProps) {
  return (
    <aside className="app-sidebar">
      <NavLink to="/" className="app-sidebar-brand">
        <img src="/brand/careerodyssey-logo.png" alt="CareerOdyssey" className="brand-logo" />
      </NavLink>

      <nav className="app-sidebar-nav">
        {NAV_ITEMS.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end} className={navLinkClass}>
            <Icon />
            <span>{label}</span>
          </NavLink>
        ))}
      </nav>

      <div className="app-sidebar-footer">
        <button type="button" className="theme-toggle-btn" onClick={onOpenWallpaper}>
          <ImageIcon />
          <span>Wallpaper</span>
        </button>
        <ThemeToggle />
      </div>
    </aside>
  )
}
