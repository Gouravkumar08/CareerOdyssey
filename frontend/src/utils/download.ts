/** Hands a blob to the browser's normal download flow. */
export function saveBlob(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

/** "HLD Roadmap" -> "HLD Roadmap.xlsx", with characters Windows file names reject removed. */
export function excelFileName(name: string) {
  return `${name.replace(/[\\/:*?"<>|]+/g, ' ').trim() || 'export'}.xlsx`
}
