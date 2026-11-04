import test from 'node:test'
import assert from 'node:assert/strict'
import { mileageVehiclesNeedingUpdate, odometerFreshnessLabel } from './odometerFreshness.js'

test('서버가 준 측정일과 경과일을 표시하고 오늘의 같은 값 확인을 최근 기록으로 표시한다', () => {
  assert.deepEqual(odometerFreshnessLabel({ observedOn: '2026-11-05', ageDays: 0, state: 'RECENT' }), {
    label: '계기판 측정일 2026-11-05 · 오늘', needsUpdate: false,
  })
  assert.deepEqual(odometerFreshnessLabel({ observedOn: '2026-10-06', ageDays: 30, state: 'STALE' }), {
    label: '계기판 측정일 2026-10-06 · 30일 전', needsUpdate: true,
  })
})

test('확인할 수 없는 날짜나 이전 API 응답을 최근 기록으로 오인하지 않는다', () => {
  for (const value of [undefined, { state: 'UNKNOWN' }, { state: 'RECENT', ageDays: 0 },
    { state: 'RECENT', ageDays: -1, observedOn: '2026-11-06' }]) {
    assert.deepEqual(odometerFreshnessLabel(value), {
      label: '현재 값의 측정일을 확인할 수 없어요.', needsUpdate: true,
    })
  }
})

test('정비 현황에 표시된 거리 기준 항목에만 차량별로 한 번씩 안내한다', () => {
  const vehicles = [
    { id: 1, odometerFreshness: { state: 'STALE', observedOn: '2026-10-06', ageDays: 30 } },
    { id: 2, odometerFreshness: { state: 'UNKNOWN' } },
    { id: 3, odometerFreshness: { state: 'RECENT', observedOn: '2026-11-05', ageDays: 0 } },
  ]
  const items = [
    { vehicleId: 1, intervalKm: 8000 }, { vehicleId: 1, intervalKm: 12000 },
    { vehicleId: 2, intervalKm: null }, { vehicleId: 3, intervalKm: 5000 },
  ]
  assert.deepEqual(mileageVehiclesNeedingUpdate(items, vehicles).map(({ id }) => id), [1])
  assert.deepEqual(mileageVehiclesNeedingUpdate(items.slice(2), vehicles), [])
})
