import { Icon } from '../../components/Icon'
import { FormHeader, Field, SelectField, Feedback } from '../../components/ui'
import { useSaveFeedback } from '../../hooks/useSaveFeedback'
import { blankMaintenanceForm, maintenancePayload, localDate } from '../../domain/forms'
import { MAINTENANCE_CATEGORIES, categoryLabel } from '../../domain/maintenance'
import { vehicleLabel } from '../../domain/maintenanceView'
import { RegistrationPreview } from './RegistrationPreview'

export function MaintenanceForm({ vehicles, items, onCreated, form, setForm, onViewExisting, onManageVehicle }) {
  const { busy, feedback, fieldErrors, formRef, save, fail, clearFieldError } = useSaveFeedback(onCreated)
  const selectedId = form.vehicleId || (vehicles.length === 1 ? String(vehicles[0].id) : '')
  const vehicle = vehicles.find((value) => String(value.id) === selectedId)
  const update = (name) => (value) => setForm((current) => ({ ...current, [name]: value }))
  const matchingItems = items.filter((item) => String(item.vehicleId) === selectedId && item.category === form.category)


  async function submit(event) {
    event.preventDefault()
    if (!form.intervalKm && !form.intervalMonths) {
      fail('거리 기준 또는 기간 기준 중 하나 이상을 입력해 주세요.',
        { intervalKm: '거리 또는 기간 중 하나 이상을 입력해 주세요.' })
      return
    }
    if (!vehicle) {
      fail('소모품을 관리할 차량을 선택해 주세요.', { vehicleId: '차량을 선택해 주세요.' })
      return
    }
    if (Number(form.lastReplacedOdometerKm) > vehicle.currentOdometerKm) {
      fail('교체 당시 주행거리는 차량에 기록된 현재 주행거리 이하로 입력해 주세요.',
        { lastReplacedOdometerKm: '차량 관리에서 현재 주행거리를 먼저 갱신해 주세요.' })
      return
    }
    await save('/api/maintenance-items', maintenancePayload({ ...form, vehicleId: selectedId }),
      '소모품을 등록했습니다. 정비 현황에서 교체 시점을 확인하세요.',
      () => setForm(blankMaintenanceForm(selectedId)))
  }

  return (
    <div className="maintenance-registration-layout">
    <form ref={formRef} className="panel registration-panel" id="maintenance-form" onSubmit={submit}
      onChange={(event) => clearFieldError(event.target.name)} aria-busy={busy}>
      <FormHeader step="02" icon="tool" title="소모품 등록"
        description="최근 교체 기록과 차량 매뉴얼의 교체주기를 입력해 주세요." />
      {!!matchingItems.length && <div className="existing-item-hint">
        <Icon name="info" />
        <div><strong>이 차량에 {categoryLabel(form.category)} {matchingItems.length}개가 등록되어 있어요.</strong>
          <p>같은 소모품을 교체했다면 기존 항목의 ‘교체 기록’을 이용해 이력을 이어가세요.
            별도로 관리할 소모품이라면 새로 등록해도 됩니다.</p>
          <button type="button" className="link" disabled={busy} onClick={() => onViewExisting(vehicle.id, form.category)}>
            기존 {categoryLabel(form.category)} 보기 <Icon name="arrow" />
          </button>
        </div>
      </div>}
      {!vehicles.length && (
        <Feedback kind="info" message="소모품을 관리하려면 먼저 차량을 등록해 주세요.">
          <a href="#vehicle-form">첫 차량 등록하기</a>
        </Feedback>
      )}
      <fieldset disabled={!vehicles.length || busy}>
        <legend>관리 대상</legend>
        <div className="form-grid">
          <SelectField name="vehicleId" label="차량" required value={selectedId}
            error={fieldErrors.vehicleId} onChange={update('vehicleId')}>
            <option value="">차량 선택</option>
            {vehicles.map((value) => <option key={value.id} value={value.id}>{vehicleLabel(value)}</option>)}
          </SelectField>
          <SelectField name="category" label="소모품 종류" required value={form.category}
            error={fieldErrors.category} onChange={update('category')}>
            {Object.entries(MAINTENANCE_CATEGORIES).map(([value, { label }]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </SelectField>
        </div>
        <Field name="productName" label="제품명" value={form.productName} error={fieldErrors.productName}
          onChange={update('productName')} placeholder="예: ZIC X7 5W-30" maxLength={160} />
        <details className="optional-fields">
          <summary>구매 링크도 저장하기 <span>선택</span></summary>
          <Field name="purchaseUrl" label="구매 링크" value={form.purchaseUrl} error={fieldErrors.purchaseUrl}
            onChange={update('purchaseUrl')} type="url" placeholder="https://쇼핑몰의-상품주소" maxLength={2048} />
          <p className="input-help">저장한 판매처를 다음 교체 때 다시 찾을 수 있어요.</p>
        </details>
      </fieldset>
      <fieldset disabled={!vehicles.length || busy}>
        <legend>최근 교체 기록</legend>
        {vehicle && <button type="button" className="link today-record-action" onClick={() => {
          setForm((current) => ({ ...current, lastReplacedOn: localDate(), lastReplacedOdometerKm: String(vehicle.currentOdometerKm) }))
          clearFieldError('lastReplacedOn')
          clearFieldError('lastReplacedOdometerKm')
        }}><Icon name="calendar" /> 오늘 · 현재 주행거리로 입력</button>}
        <div className="form-grid">
          <Field name="lastReplacedOn" label="교체일" type="date" value={form.lastReplacedOn}
            error={fieldErrors.lastReplacedOn} onChange={update('lastReplacedOn')} max={localDate()} required />
          <Field name="lastReplacedOdometerKm" label="교체 당시 주행거리" type="number" suffix="km"
            value={form.lastReplacedOdometerKm} error={fieldErrors.lastReplacedOdometerKm}
            onChange={update('lastReplacedOdometerKm')} max={vehicle?.currentOdometerKm} placeholder="예: 113000" required />
        </div>
        {vehicle && <div className="registration-odometer-context">
          <p className="input-help">차량에 기록된 현재 주행거리: {vehicle.currentOdometerKm.toLocaleString()}km</p>
          <button type="button" className="link" onClick={() => onManageVehicle(vehicle.id)}>
            현재 주행거리 갱신하기 <Icon name="arrow" />
          </button>
        </div>}
      </fieldset>
      <fieldset disabled={!vehicles.length || busy}>
        <legend>교체주기 <span className="legend-note">하나 이상 입력</span></legend>
        <div className="form-grid">
          <Field name="intervalKm" label="거리 기준" type="number" suffix="km" min={1}
            max={2147483647}
            value={form.intervalKm} error={fieldErrors.intervalKm || fieldErrors.intervalPresent}
            onChange={update('intervalKm')} placeholder="예: 8000" />
          <Field name="intervalMonths" label="기간 기준" type="number" suffix="개월" min={1}
            max={2147483647}
            value={form.intervalMonths} error={fieldErrors.intervalMonths}
            onChange={update('intervalMonths')} placeholder="예: 12" />
        </div>
        <p className="field-help"><Icon name="info" />예시는 권장 주기가 아니에요. 차량 매뉴얼이나 정비소 안내를 확인해 주세요.
          두 기준을 입력하면 먼저 도달하는 시점을 안내해요.</p>
      </fieldset>
      <Feedback {...feedback} />
      <p className="draft-retention-hint">탭을 이동해도 입력 내용은 유지돼요. 새로고침하거나 로그아웃하면 초기화됩니다.</p>
      <button className="submit-button" type="submit" disabled={!vehicles.length || busy}>
        <Icon name="plus" /> {busy ? '등록 중…' : '소모품 등록'}
      </button>
    </form>
    <RegistrationPreview form={form} vehicle={vehicle} />
    </div>
  )
}
