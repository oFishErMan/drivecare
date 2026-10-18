import test from 'node:test'
import assert from 'node:assert/strict'
import {
  categoryLabel,
  categoryIcon,
  distanceLabel,
  notificationMessage,
  statusLabel,
} from './maintenance.js'

test('distance formatting distinguishes overdue, reached, remaining and missing intervals', () => {
  assert.equal(distanceLabel(-5100), '5,100km 초과')
  assert.equal(distanceLabel(0), '교체 시점 도달')
  assert.equal(distanceLabel(1000), '1,000km 남음')
  assert.equal(distanceLabel(null), '거리 주기 없음')
})

test('unknown categories and statuses have readable fallbacks', () => {
  assert.equal(categoryLabel('NEW_CATEGORY'), '기타 소모품')
  assert.equal(categoryIcon('NEW_CATEGORY'), 'tool')
  assert.equal(statusLabel('NEW_STATUS'), '상태 확인 필요')
})

test('legacy notification messages use shared labels and overdue guidance', () => {
  const message = notificationMessage({
    type: 'OVERDUE',
    message: 'ENGINE_OIL 교체까지 0km 남았습니다.',
  })
  assert.ok(message.startsWith('엔진오일 교체 시점에 도달'))
  assert.ok(!message.includes('0km 남음'))
})
