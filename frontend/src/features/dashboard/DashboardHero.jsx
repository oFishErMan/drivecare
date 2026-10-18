import { Icon } from '../../components/Icon'

export function DashboardHero({ vehicles, items, lastUpdated, refreshing, onRefresh }) {
  return (
    <div className="dashboard-heading">
      <div>
        <span className="dashboard-eyebrow">MY GARAGE</span>
        <h1>내 차의 정비, 한눈에.</h1>
        <p>차량 {vehicles.length}대 · 소모품 {items.length}개를 관리하고 있어요.</p>
      </div>
      <div className="dashboard-sync">
        {lastUpdated && <time dateTime={lastUpdated.toISOString()}>
          {lastUpdated.toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' })} 갱신
        </time>}
        <button type="button" className="secondary-button" disabled={refreshing} onClick={onRefresh}
          aria-label={refreshing ? '갱신 중' : '새로고침'}>
          <Icon name="refresh" /><span>{refreshing ? '갱신 중…' : '새로고침'}</span>
        </button>
      </div>
    </div>
  )
}
