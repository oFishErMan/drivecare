// A draft preview, not the source of truth for saved maintenance status.
// Calendar arithmetic mirrors LocalDate.plusMonths (clamp to the target month's last day).
export function wholeNumber(value, minimum = 0) {
  if (!/^\d+$/.test(String(value))) return null
  const number = Number(value)
  return Number.isSafeInteger(number) && number >= minimum && number <= 2_147_483_647 ? number : null
}

function calendarDate(value) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value || '')) return null
  const [year, month, day] = value.split('-').map(Number)
  if (year < 1) return null
  const date = new Date(0)
  date.setUTCHours(0, 0, 0, 0)
  date.setUTCFullYear(year, month - 1, day)
  return date.toISOString().slice(0, 10) === value ? date : null
}

export function addCalendarMonths(value, months) {
  const date = calendarDate(value)
  if (!date || wholeNumber(months, 1) === null) return null
  const total = date.getUTCFullYear() * 12 + date.getUTCMonth() + Number(months)
  const year = Math.floor(total / 12)
  const month = total % 12
  if (year > 9999) return null
  const end = new Date(0)
  end.setUTCHours(0, 0, 0, 0)
  end.setUTCFullYear(year, month + 1, 0)
  const result = new Date(0)
  result.setUTCHours(0, 0, 0, 0)
  result.setUTCFullYear(year, month, Math.min(date.getUTCDate(), end.getUTCDate()))
  return result.toISOString().slice(0, 10)
}

export function registrationPreview(form, vehicle, today) {
  const odometer = wholeNumber(form.lastReplacedOdometerKm)
  const intervalKm = wholeNumber(form.intervalKm, 1)
  const intervalMonths = wholeNumber(form.intervalMonths, 1)
  const vehicleReady = Boolean(vehicle)
  const dateReady = Boolean(calendarDate(form.lastReplacedOn) && form.lastReplacedOn <= today)
  const odometerReady = vehicleReady && odometer !== null && odometer <= vehicle.currentOdometerKm
  const intervalReady = (intervalKm !== null || intervalMonths !== null)
    && (!form.intervalKm || intervalKm !== null) && (!form.intervalMonths || intervalMonths !== null)
  const nextOdometerKm = odometerReady && intervalKm !== null ? odometer + intervalKm : null
  const nextDate = dateReady && intervalMonths !== null ? addCalendarMonths(form.lastReplacedOn, intervalMonths) : null
  return {
    vehicleReady, dateReady, odometerReady, intervalReady,
    readyCount: [vehicleReady, dateReady, odometerReady, intervalReady].filter(Boolean).length,
    distance: nextOdometerKm !== null && nextOdometerKm <= 2_147_483_647 ? {
      previousKm: odometer, intervalKm, nextOdometerKm,
      remainingKm: nextOdometerKm - vehicle.currentOdometerKm,
    } : null,
    distanceOutOfRange: nextOdometerKm !== null && nextOdometerKm > 2_147_483_647,
    periodOutOfRange: dateReady && intervalMonths !== null && nextDate === null,
    nextDate,
    periodReached: nextDate !== null && nextDate <= today,
  }
}
