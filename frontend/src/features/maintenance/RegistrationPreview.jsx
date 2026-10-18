import { Icon } from '../../components/Icon'
import { categoryLabel, distanceLabel } from '../../domain/maintenance'
import { vehicleLabel } from '../../domain/maintenanceView'
import { registrationPreview } from '../../domain/registrationPreview'
import { localDate } from '../../domain/forms'

export function RegistrationPreview({ form, vehicle }) {
  const preview = registrationPreview(form, vehicle, localDate())
  const requirements = [
    ['차량 선택', preview.vehicleReady],
    ['최근 교체일', preview.dateReady],
    ['교체 당시 주행거리', preview.odometerReady],
    ['교체주기 하나 이상', preview.intervalReady],
  ]
  return (
    <aside className="registration-preview" aria-labelledby="registration-preview-title">
      <div className="preview-heading"><span className="preview-icon"><Icon name="calendar" /></span>
        <div><span className="preview-eyebrow">저장 전 확인</span><h3 id="registration-preview-title">다음 교체 기준 미리보기</h3></div>
      </div>
      <p className="preview-vehicle">{vehicle ? vehicleLabel(vehicle) : '차량을 선택해 주세요'}</p>
      <p className="preview-category">{categoryLabel(form.category)}{form.productName.trim() && ` · ${form.productName.trim()}`}</p>
      <div className="preview-readiness">
        <div><span>필수 입력</span><strong>{preview.readyCount}/4</strong></div>
        <progress value={preview.readyCount} max={4} aria-label="필수 항목 입력 현황" />
        {preview.readyCount === 4 ? <p className="preview-ready"><Icon name="check" /> 필수 입력을 모두 채웠어요.</p>
          : <ul className="preview-checklist" aria-label="남은 필수 입력">
            {requirements.filter(([, ready]) => !ready).map(([label]) => <li key={label}>
              <Icon name="plus" /><span>{label}</span><small>확인 필요</small>
            </li>)}
          </ul>}
      </div>
      <div className="preview-calculations">
        <div className="preview-calculation">
          <span><Icon name="gauge" /> 거리 기준</span>
          {preview.distance ? <>
            <strong>{preview.distance.nextOdometerKm.toLocaleString('ko-KR')}km</strong>
            <small>계기판이 이 거리에 도달하면 교체 기준이에요.</small>
            <p className={preview.distance.remainingKm <= 0 ? 'preview-reached' : ''}>
              현재 기록 기준 {distanceLabel(preview.distance.remainingKm)}
            </p>
            <p className="preview-formula">{preview.distance.previousKm.toLocaleString('ko-KR')} + {preview.distance.intervalKm.toLocaleString('ko-KR')}km</p>
          </> : <p className="preview-placeholder">{preview.distanceOutOfRange
            ? '계산 가능한 주행거리 범위를 넘었어요. 입력한 교체 주행거리와 주기를 확인해 주세요.'
            : '차량, 교체 주행거리와 거리 주기를 입력하면 계산돼요.'}</p>}
        </div>
        <div className="preview-calculation">
          <span><Icon name="calendar" /> 기간 기준</span>
          {preview.nextDate ? <>
            <strong>{preview.nextDate}</strong>
            <small>최근 교체일 + {Number(form.intervalMonths).toLocaleString('ko-KR')}개월</small>
            {preview.periodReached && <p className="preview-reached">입력한 기간 기준에 이미 도달했어요.</p>}
          </> : <p className="preview-placeholder">{preview.periodOutOfRange
            ? '표시 가능한 날짜 범위를 넘었어요. 입력한 기간 주기를 확인해 주세요.'
            : '최근 교체일과 기간 주기를 입력하면 계산돼요.'}</p>}
        </div>
      </div>
      <p className="preview-disclaimer"><Icon name="info" />
        입력한 기록으로 계산한 미리보기예요. 두 기준이 있으면 먼저 도달한 기준으로 관리하며,
        최종 상태와 알림은 저장 후 서버에서 계산합니다. 차량 매뉴얼도 함께 확인해 주세요.
      </p>
    </aside>
  )
}
