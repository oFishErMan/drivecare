import test from 'node:test'
import assert from 'node:assert/strict'
import { maintenanceCounts, maintenanceTiming, selectMaintenance, vehicleLabel, vehicleMaintenanceSummary } from './maintenanceView.js'
import { blankMaintenanceForm } from './forms.js'

const vehicles = [
  { id: 1, manufacturer: '현대', model: '아반떼', modelYear: 2023 },
  { id: 2, manufacturer: '기아', model: 'K5', modelYear: null },
]
const items = [
  { id: 1, vehicleId: 1, productName: 'ZIC X7', category: 'ENGINE_OIL', status: { state: 'GOOD', remainingKm: 7000, nextDate: '2027-01-01' } },
  { id: 2, vehicleId: 2, productName: '보쉬', category: 'CABIN_FILTER', status: { state: 'OVERDUE', remainingKm: -5100, nextDate: '2026-08-01' } },
  { id: 3, vehicleId: 1, productName: '오일', category: 'ENGINE_OIL', status: { state: 'DUE_SOON', remainingKm: 900, nextDate: '2027-01-01' } },
]

test('정비 필요 우선 정렬은 서버 목록을 변경하지 않는다', () => {
  const before = items.map((item) => item.id)
  assert.deepEqual(selectMaintenance(items, vehicles).map((item) => item.id), [2, 3, 1])
  assert.deepEqual(items.map((item) => item.id), before)
})

test('차량·상태·검색은 교집합으로 적용하고 공백·영문 대소문자를 처리한다', () => {
  assert.deepEqual(selectMaintenance(items, vehicles, { vehicleId: '1', state: 'GOOD', query: ' zic ' }).map((item) => item.id), [1])
  assert.equal(selectMaintenance(items, vehicles, { vehicleId: '2', state: 'GOOD' }).length, 0)
  assert.equal(selectMaintenance(items, vehicles, { query: '에어컨 필터' })[0].id, 2)
  assert.equal(selectMaintenance(items, vehicles, { query: 'K5' })[0].id, 2)
  assert.equal(selectMaintenance(items, vehicles, { query: '없는 제품' }).length, 0)
})

test('알림에서 넘어온 소모품은 ID로 제한하고 없으면 다른 항목을 대신 보여주지 않는다', () => {
  assert.deepEqual(selectMaintenance(items, vehicles, { itemId: 3 }).map((item) => item.id), [3])
  assert.equal(selectMaintenance(items, vehicles, { itemId: 999 }).length, 0)
})

test('상태 수와 차량명은 표시할 근거를 보존한다', () => {
  assert.deepEqual(maintenanceCounts(items), { ALL: 3, OVERDUE: 1, DUE_SOON: 1, GOOD: 1 })
  assert.equal(vehicleLabel(vehicles[0]), '현대 아반떼 · 2023년')
  assert.equal(vehicleLabel(vehicles[1]), '기아 K5')
})

test('기간 만료는 남은 거리가 있어도 만료된 일수로 안내한다', () => {
  const timing = maintenanceTiming({ status: { remainingKm: 7000, nextDate: '2026-10-13', estimatedMileageDate: '2027-01-01' } }, '2026-10-15')
  assert.equal(timing.primaryValue, '2일 지남')
  assert.equal(timing.primaryLabel, '기간 기준')
  assert.equal(timing.dateLabel, '기간 기준 교체일')
  assert.equal(timing.secondaryDistance, '7,000km 남음')
})

test('기간 전용은 남은 일수와 당일 도달을 구분한다', () => {
  assert.equal(maintenanceTiming({ status: { remainingKm: null, nextDate: '2026-10-20' } }, '2026-10-15').primaryValue, '5일 남음')
  assert.equal(maintenanceTiming({ status: { remainingKm: null, nextDate: '2026-10-15' } }, '2026-10-15').primaryValue, '교체일 도달')
})

test('주행 추정일과 기간 기준일을 구분하고 날짜를 임의로 생성하지 않는다', () => {
  const timing = maintenanceTiming({ status: { remainingKm: -5100, nextDate: '2027-01-01', estimatedMileageDate: null } }, '2026-10-15')
  assert.equal(timing.primaryValue, '5,100km 초과')
  assert.equal(maintenanceTiming({ status: { remainingKm: 900, estimatedMileageDate: '2026-10-29', nextDate: '2027-01-01' } }, '2026-10-15').dateLabel, '주행 추정 교체일')
  assert.equal(maintenanceTiming({ status: { remainingKm: 900 } }, '2026-10-15').date, null)
})

test('새 소모품의 교체주기는 권장값을 가정하지 않고 독립된 입력 상태로 생성한다', () => {
  const first = blankMaintenanceForm(2)
  const second = blankMaintenanceForm()
  assert.equal(first.vehicleId, '2')
  assert.equal(first.intervalKm, '')
  assert.equal(first.intervalMonths, '')
  first.productName = '작성 중'
  assert.equal(second.productName, '')
})

test('차량 요약은 해당 차량만 집계하며 소모품이 없는 차량을 정상으로 표시하지 않는다', () => {
  assert.equal(vehicleMaintenanceSummary(items, 1).state, 'DUE_SOON')
  assert.equal(vehicleMaintenanceSummary(items, '2').state, 'OVERDUE')
  assert.equal(vehicleMaintenanceSummary(items, 3).state, 'EMPTY')
  assert.equal(vehicleMaintenanceSummary(items, 2).counts.ALL, 1)
  assert.equal(vehicleMaintenanceSummary(items.filter((item) => item.id === 1), 1).state, 'GOOD')
})

test('기존 소모품 바로가기는 제품명 검색이 아닌 차량과 정확한 종류의 교집합이다', () => {
  assert.deepEqual(selectMaintenance(items, vehicles, { vehicleId: '1', category: 'ENGINE_OIL' }).map((item) => item.id), [3, 1])
  assert.equal(selectMaintenance(items, vehicles, { vehicleId: '2', category: 'ENGINE_OIL' }).length, 0)
})
