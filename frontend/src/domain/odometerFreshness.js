export function odometerFreshnessLabel(freshness) {
  if (!['RECENT', 'STALE'].includes(freshness?.state)
      || !freshness.observedOn || !Number.isSafeInteger(freshness.ageDays) || freshness.ageDays < 0) {
    return { label: '현재 값의 측정일을 확인할 수 없어요.', needsUpdate: true }
  }
  const elapsed = freshness.ageDays === 0 ? '오늘' : `${freshness.ageDays}일 전`
  return {
    label: `계기판 측정일 ${freshness.observedOn} · ${elapsed}`,
    needsUpdate: freshness.state === 'STALE',
  }
}

export function mileageVehiclesNeedingUpdate(visibleItems, vehicles) {
  const ids = new Set(visibleItems.filter((item) => item.intervalKm != null).map((item) => item.vehicleId))
  return vehicles.filter((vehicle) => ids.has(vehicle.id)
    && odometerFreshnessLabel(vehicle.odometerFreshness).needsUpdate)
}
