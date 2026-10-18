import test from 'node:test'
import assert from 'node:assert/strict'
import { vehiclePayload, maintenancePayload, localDate } from './forms.js'

test('vehicle payload keeps optional blanks distinct from zero', () => {
  assert.deepEqual(
    vehiclePayload({
      manufacturer: ' 현대 ',
      model: ' 아반떼 ',
      modelYear: '',
      currentOdometerKm: '120000',
      monthlyAverageKm: '',
    }),
    {
      manufacturer: '현대',
      model: '아반떼',
      modelYear: null,
      currentOdometerKm: 120000,
      monthlyAverageKm: null,
    },
  )
})

test('maintenance payload normalizes numeric strings and optional intervals', () => {
  assert.deepEqual(
    maintenancePayload({
      vehicleId: '42',
      category: 'ENGINE_OIL',
      productName: ' ',
      lastReplacedOn: '2026-10-07',
      lastReplacedOdometerKm: '0',
      intervalKm: '',
      intervalMonths: '12',
    }),
    {
      vehicleId: 42,
      category: 'ENGINE_OIL',
      productName: null,
      purchaseUrl: null,
      lastReplacedOn: '2026-10-07',
      lastReplacedOdometerKm: 0,
      intervalKm: null,
      intervalMonths: 12,
    },
  )
})

test('local date uses local calendar values with padding', () => {
  assert.equal(localDate(new Date(2026, 0, 2)), '2026-01-02')
})
