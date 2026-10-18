import { useRef, useState } from 'react'
import { api } from '../../api'
import { Feedback, Field } from '../../components/ui'
import { Icon } from '../../components/Icon'
import { localDate } from '../../domain/forms'
import { MaintenanceDelete } from './MaintenanceDelete'

export function MaintenanceActions({ item, vehicle, onUpdated, onDeleted, onVehicle }) {
  const [panel, setPanel] = useState(null)
  const [url, setUrl] = useState(item.purchaseUrl || '')
  const [date, setDate] = useState(localDate())
  const [odometer, setOdometer] = useState('')
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState({})
  const [history, setHistory] = useState([])
  const [editingRevision, setEditingRevision] = useState(item.revision)
  const [fieldErrors, setFieldErrors] = useState({})
  const formRef = useRef(null)

  async function openPanel(next) {
    if (busy) return
    setFeedback({})
    setFieldErrors({})
    if (panel === next) {
      setPanel(null)
      return
    }
    setPanel(next)
    setEditingRevision(item.revision)
    if (next === 'link') setUrl(item.purchaseUrl || '')
    if (next === 'replace') {
      setDate(localDate())
      setOdometer('')
    }
    if (next === 'history') {
      setHistory([])
      setBusy(true)
      try {
        setHistory(await api(`/api/maintenance-items/${item.id}/replacements`))
      } catch (cause) {
        setFeedback({ message: cause.message })
      } finally {
        setBusy(false)
      }
    }
  }

  async function submit(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setFeedback({})
    setFieldErrors({})
    const updatingLink = panel === 'link'
    try {
      await api(
        `/api/maintenance-items/${item.id}/${updatingLink ? 'purchase-link' : 'replacements'}`,
        {
          method: updatingLink ? 'PATCH' : 'POST',
          body: JSON.stringify({
            expectedRevision: editingRevision,
            ...(updatingLink
              ? { purchaseUrl: url.trim() || null }
              : { replacedOn: date, odometerKm: Number(odometer) }),
          }),
        },
      )
      setPanel(null)
      setFeedback({
        kind: 'success',
        message: updatingLink
          ? url.trim()
            ? '구매 링크를 저장했습니다.'
            : '구매 링크를 삭제했습니다.'
          : '교체를 기록하고 다음 교체 시점을 계산했습니다.',
      })
      await onUpdated()
    } catch (cause) {
      setFeedback({ message: cause.message })
      setFieldErrors(cause.fieldErrors || {})
      formRef.current?.elements.namedItem(Object.keys(cause.fieldErrors || {})[0])?.focus()
    } finally {
      setBusy(false)
    }
  }

  // Opening the saved link must not depend on analytics availability.
  function trackRepurchase() {
    api(`/api/maintenance-items/${item.id}/repurchase-clicks`, { method: 'POST' }).catch(() => {})
  }

  return (
    <div className="maintenance-actions">
      {item.purchaseUrl ? (
        <>
          <a
            className="repurchase-link"
            href={item.purchaseUrl}
            target="_blank"
            rel="noopener noreferrer"
            onClick={trackRepurchase}
          >
            <Icon name="arrow" /> 지난번 제품 다시 보기{' '}
            <span className="external-label">새 창</span>
          </a>
          <p className="field-help">
            {new URL(item.purchaseUrl).hostname} · 저장한 판매처에서 가격과 규격을 확인하세요.
          </p>
        </>
      ) : null}
      <div className="maintenance-action-main">
        <button
          type="button"
          className="record-replacement"
          disabled={busy}
          onClick={() => openPanel('replace')}
          aria-expanded={panel === 'replace'}
        >
          <Icon name="plus" /> 교체 기록
        </button>
        <button
          type="button"
          className="secondary-button"
          disabled={busy}
          onClick={() => openPanel('history')}
          aria-expanded={panel === 'history'}
        >
          교체 이력
        </button>
      </div>
      <div className="card-action-row maintenance-manage-row">
        <button
          type="button"
          className="link"
          disabled={busy}
          onClick={() => openPanel('link')}
          aria-expanded={panel === 'link'}
        >
          {item.purchaseUrl ? '구매 링크 수정' : '구매 링크 추가'}
        </button>
        <MaintenanceDelete item={item} disabled={busy} onDeleted={onDeleted} />
      </div>
      {(panel === 'link' || panel === 'replace') && (
        <form ref={formRef} className="card-inline-form" onSubmit={submit}>
          <fieldset disabled={busy}>
            <legend>{panel === 'link' ? '구매 링크 관리' : '같은 제품으로 교체했나요?'}</legend>
            {panel === 'link' ? (
              <>
                <Field
                  name="purchaseUrl"
                  error={fieldErrors.purchaseUrl}
                  label="구매 링크"
                  type="url"
                  value={url}
                  onChange={setUrl}
                  maxLength={2048}
                  placeholder="https://쇼핑몰의-상품주소"
                />
                <p className="field-help">주소를 비우고 저장하면 링크가 삭제됩니다.</p>
              </>
            ) : (
              <>
                <p className="field-help">
                  동일한 제품을 기존 교체주기에 맞춰 교체한 경우 기록해 주세요.
                </p>
                <Field
                  name="replacedOn"
                  error={fieldErrors.replacedOn}
                  label="실제 교체일"
                  type="date"
                  value={date}
                  onChange={setDate}
                  min={item.lastReplacedOn}
                  max={localDate()}
                  required
                />
                <Field
                  name="odometerKm"
                  error={fieldErrors.odometerKm}
                  label="교체 당시 주행거리"
                  type="number"
                  suffix="km"
                  value={odometer}
                  onChange={setOdometer}
                  min={item.lastReplacedOdometerKm}
                  max={vehicle?.currentOdometerKm}
                  required
                />
                <p className="field-help">
                  차량에 기록된 현재 주행거리: {vehicle?.currentOdometerKm.toLocaleString()}km. 더
                  많이 주행했다면 차량 카드에서 먼저 갱신해 주세요.
                </p>
                {vehicle && <button type="button" className="link replacement-odometer-action"
                  onClick={() => onVehicle(vehicle.id)}>
                  현재 주행거리 갱신하기 <Icon name="arrow" />
                </button>}
              </>
            )}
            <div className="card-action-row">
              <button type="submit">
                {busy ? '저장 중…' : panel === 'link' ? '링크 저장' : '교체 기록 저장'}
              </button>
              <button type="button" className="link" onClick={() => setPanel(null)}>
                취소
              </button>
            </div>
          </fieldset>
        </form>
      )}
      {panel === 'history' && (
        <div className="replacement-history">
          <h4>교체 이력</h4>
          {busy ? (
            <p role="status">기록을 불러오는 중…</p>
          ) : (
            <>
              <ol>
                {history.map((record) => (
                  <li key={record.id}>
                    <strong>{record.replacedOn}</strong>
                    <span>
                      {record.odometerKm.toLocaleString()}km ·{' '}
                      {record.productName || '제품명 미입력'}
                    </span>
                  </li>
                ))}
              </ol>
              {!history.length && !feedback.message && <p>아직 교체 기록이 없습니다.</p>}
            </>
          )}
        </div>
      )}
      <Feedback {...feedback} />
    </div>
  )
}
