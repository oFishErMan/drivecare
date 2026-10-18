import { categoryLabel, distanceLabel } from './maintenance.js'

const priority = { OVERDUE: 0, DUE_SOON: 1, GOOD: 2 }
export const EMPTY_FILTERS = Object.freeze({ vehicleId: '', query: '', category: '', state: 'ALL', itemId: null })

export function vehicleLabel(vehicle) {
  if (!vehicle) return '차량 정보 확인 필요'
  return `${vehicle.manufacturer} ${vehicle.model}${vehicle.modelYear ? ` · ${vehicle.modelYear}년` : ''}`
}

export function maintenanceCounts(items) {
  return {
    ALL: items.length,
    OVERDUE: items.filter((item) => item.status.state === 'OVERDUE').length,
    DUE_SOON: items.filter((item) => item.status.state === 'DUE_SOON').length,
    GOOD: items.filter((item) => item.status.state === 'GOOD').length,
  }
}

export function vehicleMaintenanceSummary(items, vehicleId) {
  const counts = maintenanceCounts(items.filter((item) => String(item.vehicleId) === String(vehicleId)))
  return {
    counts,
    state: !counts.ALL ? 'EMPTY' : counts.OVERDUE ? 'OVERDUE' : counts.DUE_SOON ? 'DUE_SOON' : 'GOOD',
  }
}

export function selectMaintenance(items, vehicles, { vehicleId = '', state = 'ALL', query = '', category = '', itemId = null } = {}) {
  const search = query.trim().toLocaleLowerCase('ko-KR')
  const vehicleNames = new Map(vehicles.map((vehicle) => [vehicle.id, vehicleLabel(vehicle)]))
  return items.filter((item) => (
    (!vehicleId || String(item.vehicleId) === String(vehicleId))
    && (state === 'ALL' || item.status.state === state)
    && (!category || item.category === category)
    && (itemId == null || item.id === itemId)
    && (!search || `${item.productName || ''} ${categoryLabel(item.category)} ${vehicleNames.get(item.vehicleId) || ''}`
      .toLocaleLowerCase('ko-KR').includes(search))
  )).sort((a, b) => (
    (priority[a.status.state] ?? 3) - (priority[b.status.state] ?? 3)
    || (a.status.remainingKm ?? Number.POSITIVE_INFINITY) - (b.status.remainingKm ?? Number.POSITIVE_INFINITY)
    || (a.status.nextDate || '9999').localeCompare(b.status.nextDate || '9999')
    || a.id - b.id
  ))
}

function calendarDay(value) {
  const [year, month, day] = value.split('-').map(Number)
  return Date.UTC(year, month - 1, day) / 86_400_000
}

export function maintenanceTiming(item, today) {
  const { remainingKm, nextDate, estimatedMileageDate } = item.status
  const days = nextDate ? calendarDay(nextDate) - calendarDay(today) : null
  const useDate = days !== null && (days <= 0 || remainingKm == null)
  const primaryLabel = useDate ? '기간 기준' : '주행거리 기준'
  const primaryValue = useDate
    ? days < 0 ? `${Math.abs(days).toLocaleString('ko-KR')}일 지남`
      : days === 0 ? '교체일 도달' : `${days.toLocaleString('ko-KR')}일 남음`
    : distanceLabel(remainingKm)
  const date = [nextDate, estimatedMileageDate].filter(Boolean).sort()[0] || null
  const estimated = Boolean(date && date !== nextDate)
  return {
    primaryLabel, primaryValue, date,
    secondaryDistance: useDate && remainingKm != null ? distanceLabel(remainingKm) : null,
    dateLabel: estimated ? '주행 추정 교체일' : '기간 기준 교체일',
    dateHelp: estimated ? '월평균 주행거리로 계산한 참고일이에요.'
      : date ? '등록한 교체일과 기간 주기를 기준으로 계산했어요.'
        : '월평균 주행거리를 등록하면 예상 날짜도 볼 수 있어요.',
  }
}
