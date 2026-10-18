import { useState } from 'react'
import { Field, Feedback } from '../../components/ui'
import { useSaveFeedback } from '../../hooks/useSaveFeedback'
import { localDate } from '../../domain/forms'
import { Icon } from '../../components/Icon'

export function OdometerForm({ vehicle, onUpdated }) {
  const [open, setOpen] = useState(false)
  const [odometer, setOdometer] = useState('')
  const { busy, feedback, fieldErrors, formRef, save, clearFieldError } = useSaveFeedback(onUpdated)
  const entered = odometer.trim() !== '' && /^\d+$/.test(odometer) ? Number(odometer) : null
  const increase = entered !== null ? entered - vehicle.currentOdometerKm : null
  async function submit(event) {
    event.preventDefault()
    await save(
      `/api/vehicles/${vehicle.id}/odometer`,
      { recordedOn: localDate(), odometerKm: Number(odometer) },
      '주행거리를 갱신하고 소모품 상태를 다시 계산했습니다.',
      () => {
        setOpen(false)
        setOdometer('')
      },
    )
  }
  return (
    <div className="odometer-editor">
      <button
        type="button"
        className="link"
        disabled={busy}
        onClick={() => setOpen(!open)}
        aria-expanded={open}
      >
        <Icon name="gauge" /> 현재 주행거리 갱신
      </button>
      {open && (
        <form ref={formRef} className="card-inline-form" onSubmit={submit}
          onChange={(event) => clearFieldError(event.target.name)}>
          <fieldset disabled={busy}>
            <legend>오늘 계기판의 누적 주행거리</legend>
            <Field
              name="odometerKm"
              error={fieldErrors.odometerKm}
              label="현재 주행거리"
              type="number"
              suffix="km"
              min={vehicle.currentOdometerKm}
              max={2147483647}
              value={odometer}
              onChange={setOdometer}
              required
            />
            <p className="odometer-current">기존 기록 <strong>{vehicle.currentOdometerKm.toLocaleString()}km</strong></p>
            {increase !== null && <p className={`odometer-change${increase < 0 ? ' is-invalid' : ''}`} role="status">
              {increase < 0 ? '기존 기록보다 작은 값은 저장할 수 없어요.'
                : increase === 0 ? '기존 기록과 같은 주행거리예요.'
                  : `기존보다 ${increase.toLocaleString()}km 증가해요.`}
            </p>}
            <p className="input-help">이번에 주행한 거리가 아닌 계기판의 누적 거리를 입력하세요. 저장하면 이 차량의 소모품 상태도 다시 계산됩니다.</p>
            <button type="submit">{busy ? '저장 중…' : '주행거리 저장'}</button>
            <button type="button" className="link" disabled={busy} onClick={() => setOpen(false)}>취소</button>
          </fieldset>
        </form>
      )}
      <Feedback {...feedback} />
    </div>
  )
}
