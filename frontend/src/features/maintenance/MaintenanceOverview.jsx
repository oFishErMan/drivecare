import { Icon } from '../../components/Icon'
import { EmptyState } from '../../components/ui'
import { MaintenanceCard } from './MaintenanceCard'
import { EMPTY_FILTERS, maintenanceCounts, selectMaintenance, vehicleLabel } from '../../domain/maintenanceView'
import { categoryLabel } from '../../domain/maintenance'

const states = [
  { value: 'ALL', label: '전체' },
  { value: 'OVERDUE', label: '교체 필요' },
  { value: 'DUE_SOON', label: '교체 임박' },
  { value: 'GOOD', label: '정상' },
]

export function MaintenanceOverview({ items, vehicles, filters, onFilter, onRegister, onVehicle,
  onUpdated, onDeleted }) {
  const scope = items.filter((item) => (!filters.vehicleId || String(item.vehicleId) === filters.vehicleId)
    && (!filters.category || item.category === filters.category))
  const counts = maintenanceCounts(scope)
  const visible = selectMaintenance(items, vehicles, filters)
  const update = (patch) => onFilter({ ...filters, ...patch, itemId: null })
  const reset = () => onFilter({ ...EMPTY_FILTERS })

  return (
    <>
      <div className="tab-panel-heading">
        <div><h2>소모품 상태</h2><p>교체가 필요한 항목부터 확인하고 정비를 기록하세요.</p></div>
        <button type="button" className="tab-primary-action" onClick={!vehicles.length ? () => onVehicle() : onRegister}>
          <Icon name="plus" /> {vehicles.length ? '소모품 등록' : '차량 등록'}
        </button>
      </div>
      {!vehicles.length ? (
        <EmptyState icon="car" title="내 차를 등록하고 시작해 볼까요?"
          description="현재 주행거리와 소모품 교체 기록을 등록하면 다음 정비 시점을 계산해 드려요."
          actionLabel="첫 차량 등록하기" onAction={onVehicle} />
      ) : !items.length ? (
        <EmptyState icon="tool" title="다음 정비를 기억할 첫 기록을 남겨 주세요"
          description="최근에 교체한 소모품부터 등록해 보세요. 거리나 기간 중 하나만으로도 시작할 수 있어요."
          actionLabel="첫 소모품 등록하기" onAction={onRegister} />
      ) : (
        <>
          <div className="maintenance-toolbar" aria-label="소모품 조회 조건">
            <label className="vehicle-filter">
              <span>차량</span>
              <select value={filters.vehicleId} onChange={(event) => update({ vehicleId: event.target.value })}>
                <option value="">전체 차량</option>
                {vehicles.map((vehicle) => <option key={vehicle.id} value={vehicle.id}>{vehicleLabel(vehicle)}</option>)}
              </select>
            </label>
            <label className="maintenance-search">
              <span>소모품 검색</span>
              <span className="search-input-wrap">
                <Icon name="search" />
                <input type="search" value={filters.query} placeholder="제품명·종류 검색"
                  onChange={(event) => update({ query: event.target.value })} />
              </span>
            </label>
          </div>
          <div className="status-filters" role="group" aria-label="정비 상태 필터">
            {states.map(({ value, label }) => (
              <button key={value} type="button" className={`status-filter filter-${value.toLowerCase()}`}
                aria-pressed={filters.state === value} onClick={() => update({ state: value })}>
                <span>{label}</span><strong>{counts[value]}</strong>
              </button>
            ))}
          </div>
          <div className="list-context">
            <p role="status">{filters.itemId != null ? '알림의 관련 소모품' : `조회 결과 ${visible.length}개`}
              {filters.category && <span> · {categoryLabel(filters.category)}</span>}<span> · 교체 필요 순</span></p>
            {(filters.vehicleId || filters.query || filters.category || filters.state !== 'ALL' || filters.itemId != null) && (
              <button type="button" className="link" onClick={reset}>전체 보기 · 필터 초기화</button>
            )}
          </div>
          {filters.category && <button type="button" className="category-filter-chip"
            onClick={() => update({ category: '' })} aria-label={`${categoryLabel(filters.category)} 종류 필터 해제`}>
            {categoryLabel(filters.category)} <Icon name="close" />
          </button>}
          <div className="grid cards maintenance-cards">
            {visible.map((item) => (
              <MaintenanceCard item={item} key={item.id}
                vehicle={vehicles.find((vehicle) => vehicle.id === item.vehicleId)}
                onUpdated={onUpdated} onDeleted={onDeleted} onVehicle={onVehicle} />
            ))}
          </div>
          {!visible.length && (
            <EmptyState icon="search" title="조건에 맞는 소모품이 없어요"
              description="다른 차량이나 상태를 선택하거나 검색어를 바꿔 보세요."
              actionLabel="필터 초기화" onAction={reset} />
          )}
        </>
      )}
    </>
  )
}
