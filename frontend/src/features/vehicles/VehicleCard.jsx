import { Icon } from '../../components/Icon'
import { OdometerForm } from './OdometerForm'
import { vehicleMaintenanceSummary } from '../../domain/maintenanceView'

export function VehicleCard({ vehicle, items, onUpdated, onRegister, onViewMaintenance }) {
  const { counts, state } = vehicleMaintenanceSummary(items, vehicle.id)
  const summaryLabel = {
    EMPTY: '아직 관리 중인 소모품이 없어요',
    OVERDUE: '교체가 필요한 소모품이 있어요',
    DUE_SOON: '교체 시점이 가까워지고 있어요',
    GOOD: '등록한 소모품은 모두 정상 범위예요',
  }[state]
  return (
    <article className="card vehicle-card" id={`vehicle-${vehicle.id}`} tabIndex={-1}>
      <div className="card-heading">
        <span className="icon-box vehicle-icon">
          <Icon name="car" />
        </span>
        <span className="year-chip">{vehicle.modelYear || '연식 미입력'}</span>
      </div>
      <div>
        <p className="card-kicker">등록 차량</p>
        <h3>
          {vehicle.manufacturer} {vehicle.model}
        </h3>
      </div>
      <div className="vehicle-metrics">
        <div>
          <Icon name="gauge" />
          <span>현재 주행거리</span>
          <strong>{vehicle.currentOdometerKm.toLocaleString()} km</strong>
        </div>
        <div>
          <Icon name="route" />
          <span>월평균 주행</span>
          <strong>{vehicle.monthlyAverageKm == null ? '미입력' : `${vehicle.monthlyAverageKm.toLocaleString()} km`}</strong>
        </div>
      </div>
      <div className={`vehicle-health health-${state.toLowerCase()}`}>
        <p><Icon name={state === 'GOOD' ? 'check' : state === 'EMPTY' ? 'tool' : 'info'} />{summaryLabel}</p>
        {!!counts.ALL && <>
          <dl className="vehicle-health-counts">
            <div><dt>관리 소모품</dt><dd>{counts.ALL}개</dd></div>
            <div><dt>교체 필요</dt><dd>{counts.OVERDUE}개</dd></div>
            <div><dt>교체 임박</dt><dd>{counts.DUE_SOON}개</dd></div>
          </dl>
          <button type="button" className="link" onClick={() => onViewMaintenance(vehicle.id)}>
            이 차량의 정비 상태 보기 <Icon name="arrow" />
          </button>
        </>}
        {!counts.ALL && <small>최근 교체한 소모품부터 추가하면 다음 교체 기준을 확인할 수 있어요.</small>}
      </div>
      <OdometerForm vehicle={vehicle} onUpdated={onUpdated} />
      <button type="button" className="secondary-button vehicle-register-item" onClick={() => onRegister(vehicle.id)}>
        <Icon name="plus" /> 이 차량에 소모품 등록
      </button>
    </article>
  )
}
