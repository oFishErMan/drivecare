export const DASHBOARD_TABS = Object.freeze([
  { id: 'maintenance', label: '정비 현황', icon: 'tool', hash: '#maintenance-status' },
  { id: 'vehicles', label: '차량 관리', icon: 'car', hash: '#vehicles' },
  { id: 'register', label: '소모품 등록', icon: 'plus', hash: '#maintenance-form' },
  { id: 'notifications', label: '알림', icon: 'bell', hash: '#notifications' },
])

export function tabFromHash(hash) {
  if (hash === '#vehicle-form') return 'vehicles'
  return DASHBOARD_TABS.find((tab) => tab.hash === hash)?.id || 'maintenance'
}

export function nextTabIndex(index, key) {
  const count = DASHBOARD_TABS.length
  if (key === 'ArrowRight') return (index + 1) % count
  if (key === 'ArrowLeft') return (index + count - 1) % count
  if (key === 'Home') return 0
  if (key === 'End') return count - 1
  return null
}
