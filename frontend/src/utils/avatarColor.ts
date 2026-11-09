const AVATAR_COLOR_COUNT = 10

/** Deterministic (same name -> same color) so a company/profile looks the same everywhere it
 * appears, while different names spread across the 10 `.avatar-color-N` variants in index.css. */
export function avatarColorClass(seed: string): string {
  let hash = 0
  for (let i = 0; i < seed.length; i++) {
    hash = (hash * 31 + seed.charCodeAt(i)) >>> 0
  }
  return `avatar-color-${hash % AVATAR_COLOR_COUNT}`
}
