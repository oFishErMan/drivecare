import { useEffect, useState } from 'react'
import { Icon } from '../../components/Icon'
import { FormHeader, Field, Feedback } from '../../components/ui'
import { useSaveFeedback } from '../../hooks/useSaveFeedback'
import { vehiclePayload } from '../../domain/forms'

const blankVehicle = {
  manufacturer: '',
  model: '',
  modelYear: '',
  currentOdometerKm: '',
  monthlyAverageKm: '',
}

export function VehicleForm({ onCreated, headingId, onBusyChange }) {
  const [form, setForm] = useState(blankVehicle)
  const { busy, feedback, fieldErrors, formRef, save, fail, clearFieldError } = useSaveFeedback(onCreated)
  useEffect(() => { onBusyChange?.(busy) }, [busy, onBusyChange])
  async function submit(event) {
    event.preventDefault()
    if (!form.manufacturer.trim() || !form.model.trim()) {
      fail('제조사와 모델명을 입력해 주세요.')
      return
    }
    await save(
      '/api/vehicles',
      vehiclePayload(form),
      '차량을 등록했습니다. 이제 소모품 교체 기록을 추가해 보세요.',
      () => setForm(blankVehicle),
    )
  }
  return (
    <form ref={formRef} className="panel registration-panel" id="vehicle-form" onSubmit={submit}
      onChange={(event) => clearFieldError(event.target.name)} aria-busy={busy}>
      <FormHeader
        headingId={headingId}
        step="01"
        icon="car"
        title="차량 등록"
        description="내 차의 기본 정보와 현재 계기판 주행거리를 입력하세요."
      />
      <fieldset disabled={busy}>
        <legend>차량 기본 정보</legend>
        <div className="form-grid">
          <Field
            name="manufacturer"
            error={fieldErrors.manufacturer}
            value={form.manufacturer}
            onChange={(v) => setForm({ ...form, manufacturer: v })}
            label="제조사"
            placeholder="예: 현대"
            maxLength={80}
            required
          />
          <Field
            name="model"
            error={fieldErrors.model}
            value={form.model}
            onChange={(v) => setForm({ ...form, model: v })}
            label="모델"
            placeholder="예: 아반떼 CN7"
            maxLength={100}
            required
          />
          <Field
            name="modelYear"
            error={fieldErrors.modelYear}
            value={form.modelYear}
            onChange={(v) => setForm({ ...form, modelYear: v })}
            label="연식"
            type="number"
            min={1950}
            max={2100}
            placeholder="예: 2023"
          />
        </div>
      </fieldset>
      <fieldset disabled={busy}>
        <legend>운행 정보</legend>
        <div className="form-grid two-columns">
          <Field
            name="currentOdometerKm"
            error={fieldErrors.currentOdometerKm}
            value={form.currentOdometerKm}
            onChange={(v) => setForm({ ...form, currentOdometerKm: v })}
            label="현재 주행거리"
            suffix="km"
            type="number"
            placeholder="예: 120000"
            max={2147483647}
            required
          />
          <Field
            name="monthlyAverageKm"
            error={fieldErrors.monthlyAverageKm}
            value={form.monthlyAverageKm}
            onChange={(v) => setForm({ ...form, monthlyAverageKm: v })}
            label="월평균 주행거리"
            suffix="km"
            type="number"
            placeholder="예: 1500"
            max={2147483647}
          />
        </div>
        <p className="field-help">
          <Icon name="info" /> 계기판의 누적 주행거리를 입력하세요. 월평균 주행거리를 입력하면 거리
          기준 예상 교체일도 계산합니다.
        </p>
      </fieldset>
      <Feedback {...feedback} />
      <button className="submit-button" type="submit" disabled={busy}>
        <Icon name="plus" /> {busy ? '등록 중…' : '내 차량 등록'}
      </button>
    </form>
  )
}
