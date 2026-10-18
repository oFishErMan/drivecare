import test from 'node:test'
import assert from 'node:assert/strict'
import { wholeNumber, addCalendarMonths, registrationPreview } from './registrationPreview.js'
import { blankMaintenanceForm } from './forms.js'

const today = '2026-10-15'
const vehicle = { id: 1, currentOdometerKm: 120100 }
const draft = { ...blankMaintenanceForm(1), lastReplacedOn: '2026-06-01', lastReplacedOdometerKm: '113000', intervalKm: '8000' }

test('미리보기 숫자는 빈 값과 0을 구분하고 음수·소수·지수·정수 범위 초과를 제외한다', () => {
  assert.equal(wholeNumber('0'), 0)
  for (const value of ['', ' ', '-1', '1.5', '1e3', 'Infinity', '2147483648']) assert.equal(wholeNumber(value), null)
  assert.equal(wholeNumber('0', 1), null)
  assert.equal(wholeNumber('2147483647'), 2147483647)
})

test('월말·윤년의 기간 계산은 서버 plusMonths처럼 마지막 유효 날짜로 보정한다', () => {
  assert.equal(addCalendarMonths('2026-01-31', 1), '2026-02-28')
  assert.equal(addCalendarMonths('2024-01-31', 1), '2024-02-29')
  assert.equal(addCalendarMonths('2024-02-29', 12), '2025-02-28')
  assert.equal(addCalendarMonths('2026-12-31', 2), '2027-02-28')
})

test('유효하지 않은 날짜·기간은 임의의 교체일로 변환하지 않는다', () => {
  for (const value of ['2026-02-30', '2026-13-01', '', '2026-2-01', '0000-01-01']) assert.equal(addCalendarMonths(value, 1), null)
  assert.equal(addCalendarMonths('2026-01-01', 0), null)
  assert.equal(addCalendarMonths('9999-12-01', 1), null)
})

test('차량·기록이 없을 때 정상 상태나 0km 교체 기준을 만들어 내지 않는다', () => {
  const preview = registrationPreview(blankMaintenanceForm(), undefined, today)
  assert.equal(preview.readyCount, 0)
  assert.equal(preview.distance, null)
  assert.equal(preview.nextDate, null)
})

test('다음 교체 계기판 값과 현재 기준 남은 거리를 구분한다', () => {
  const preview = registrationPreview(draft, vehicle, today)
  assert.equal(preview.readyCount, 4)
  assert.deepEqual(preview.distance, { previousKm: 113000, intervalKm: 8000, nextOdometerKm: 121000, remainingKm: 900 })
  assert.equal(preview.nextDate, null)
  assert.equal(registrationPreview({ ...draft, intervalKm: '1000' }, vehicle, today).distance.remainingKm, -6100)
})

test('기간 전용·도달 당일·미래 교체일과 현재보다 큰 교체 주행거리를 처리한다', () => {
  const period = registrationPreview({ ...draft, intervalKm: '', intervalMonths: '4' }, vehicle, today)
  assert.equal(period.distance, null)
  assert.equal(period.nextDate, '2026-10-01')
  assert.equal(period.periodReached, true)
  assert.equal(registrationPreview({ ...draft, lastReplacedOn: '2026-06-15', intervalMonths: '4' }, vehicle, today).periodReached, true)
  assert.equal(registrationPreview({ ...draft, lastReplacedOn: '2026-10-16', intervalMonths: '4' }, vehicle, today).nextDate, null)
  assert.equal(registrationPreview({ ...draft, lastReplacedOdometerKm: '120101' }, vehicle, today).distance, null)
})

test('0km의 실제 교체 기록은 유효하며 잘못된 주기 조합은 완료로 표시하지 않는다', () => {
  assert.equal(registrationPreview({ ...draft, lastReplacedOdometerKm: '0' }, vehicle, today).odometerReady, true)
  assert.equal(registrationPreview({ ...draft, intervalMonths: '-1' }, vehicle, today).intervalReady, false)
  assert.equal(registrationPreview({ ...draft, intervalKm: '2147483647' }, vehicle, today).distance, null)
  assert.equal(registrationPreview({ ...draft, intervalKm: '2147483647' }, vehicle, today).distanceOutOfRange, true)
  assert.equal(registrationPreview({ ...draft, intervalMonths: '2147483647' }, vehicle, today).periodOutOfRange, true)
})
