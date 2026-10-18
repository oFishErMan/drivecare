import test from 'node:test'
import assert from 'node:assert/strict'
import { DASHBOARD_TABS, tabFromHash, nextTabIndex } from './dashboardTabs.js'

test('탭 주소는 새로고침과 직접 진입에도 같은 화면을 선택한다', () => {
  for (const tab of DASHBOARD_TABS) assert.equal(tabFromHash(tab.hash), tab.id)
})

test('기존 차량 등록 바로가기는 차량 관리로 연결하고 알 수 없는 주소는 정비 현황을 연다', () => {
  assert.equal(tabFromHash('#vehicle-form'), 'vehicles')
  assert.equal(tabFromHash(''), 'maintenance')
  assert.equal(tabFromHash('#unknown'), 'maintenance')
})

test('키보드 방향키는 순환 이동하고 Home과 End는 양 끝 탭으로 이동한다', () => {
  assert.equal(nextTabIndex(3, 'ArrowRight'), 0)
  assert.equal(nextTabIndex(0, 'ArrowLeft'), 3)
  assert.equal(nextTabIndex(1, 'ArrowRight'), 2)
  assert.equal(nextTabIndex(2, 'Home'), 0)
  assert.equal(nextTabIndex(0, 'End'), 3)
  assert.equal(nextTabIndex(1, 'Tab'), null)
})
