import { Icon } from '../../components/Icon'
import { MaintenanceActions } from './MaintenanceActions'
import { categoryLabel, categoryIcon, statusLabel } from '../../domain/maintenance'
import { maintenanceTiming, vehicleLabel } from '../../domain/maintenanceView'
import { localDate } from '../../domain/forms'

export function MaintenanceCard({ item, vehicle, onUpdated, onDeleted, onVehicle }) {
  const state = item.status.state.toLowerCase()
  const timing = maintenanceTiming(item, localDate())
  return (
    <article className={`card maintenance-card status-${state}`} aria-label={`${vehicleLabel(vehicle)} · ${item.productName || categoryLabel(item.category)}`}>
      <p className="maintenance-vehicle"><Icon name="car" />{vehicleLabel(vehicle)}</p>
      <div className="card-heading">
        <span className={`icon-box category-${item.category.toLowerCase()}`}>
          <Icon name={categoryIcon(item.category)} />
        </span>
        <span className="badge">
          <span className="status-dot" />
          {statusLabel(item.status.state)}
        </span>
      </div>
      <div>
        <p className="card-kicker">{categoryLabel(item.category)}</p>
        <h3>{item.productName || categoryLabel(item.category)}</h3>
      </div>
      <div className="maintenance-details">
        <div className="primary-metric">
          <Icon name={timing.primaryLabel === '기간 기준' ? 'calendar' : 'gauge'} />
          <div>
            <span>{timing.primaryLabel}</span>
            <strong>{timing.primaryValue}</strong>
          </div>
        </div>
        {timing.secondaryDistance && <p className="secondary-distance">거리 기준 {timing.secondaryDistance}</p>}
        <div className="date-row">
          <Icon name="calendar" />
          <span>{timing.date ? timing.dateLabel : '교체 날짜'}</span>
          <strong>{timing.date || '추정 정보 없음'}</strong>
        </div>
        <p className="calculation-help">{timing.dateHelp}</p>
        <p className="last-replacement">최근 교체 {item.lastReplacedOn} · {item.lastReplacedOdometerKm.toLocaleString()}km</p>
        <button type="button" className="link vehicle-odometer-link" onClick={() => onVehicle(vehicle?.id)}>
          현재 {vehicle?.currentOdometerKm.toLocaleString() || '—'}km · 주행거리 갱신 <Icon name="arrow" />
        </button>
      </div>
      <MaintenanceActions item={item} vehicle={vehicle} onUpdated={onUpdated} onDeleted={onDeleted} onVehicle={onVehicle} />
    </article>
  )
}
