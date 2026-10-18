// Registration options, cards and notification translations share this metadata.
export const MAINTENANCE_CATEGORIES = Object.freeze({
  ENGINE_OIL: { label: '엔진오일', icon: 'oil' },
  OIL_FILTER: { label: '오일 필터', icon: 'filter' },
  AIR_FILTER: { label: '에어 필터', icon: 'filter' },
  CABIN_FILTER: { label: '에어컨 필터', icon: 'filter' },
  BRAKE_FLUID: { label: '브레이크액', icon: 'oil' },
  BRAKE_PAD: { label: '브레이크 패드', icon: 'tool' },
  COOLANT: { label: '냉각수', icon: 'oil' },
  TRANSMISSION_FLUID: { label: '변속기 오일', icon: 'oil' },
  TIRE: { label: '타이어', icon: 'tire' },
  WIPER: { label: '와이퍼', icon: 'tool' },
  BATTERY: { label: '배터리', icon: 'battery' },
  OTHER: { label: '기타 소모품', icon: 'tool' },
})

export function categoryLabel(category) {
  return (MAINTENANCE_CATEGORIES[category] || MAINTENANCE_CATEGORIES.OTHER).label
}

export function categoryIcon(category) {
  return (MAINTENANCE_CATEGORIES[category] || MAINTENANCE_CATEGORIES.OTHER).icon
}

export function statusLabel(status) {
  return { GOOD: '정상', DUE_SOON: '교체 임박', OVERDUE: '교체 필요' }[status] || '상태 확인 필요'
}

export function distanceLabel(remainingKm) {
  if (remainingKm == null) return '거리 주기 없음'
  if (remainingKm < 0) return `${Math.abs(remainingKm).toLocaleString('ko-KR')}km 초과`
  if (remainingKm === 0) return '교체 시점 도달'
  return `${remainingKm.toLocaleString('ko-KR')}km 남음`
}

export function notificationMessage(notification) {
  let message = notification.message
  Object.entries(MAINTENANCE_CATEGORIES).forEach(([key, { label }]) => {
    message = message.replaceAll(key, label)
  })
  if (notification.type === 'OVERDUE')
    message = message.replace(
      /교체까지 0km 남았습니다\./,
      '교체 시점에 도달했거나 주기를 초과했습니다. 정비 상태를 확인해 주세요.',
    )
  return message
}
