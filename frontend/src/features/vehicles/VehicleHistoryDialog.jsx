import { useEffect, useId, useRef, useState } from 'react'
import { api, fetchCsv } from '../../api'
import { Feedback } from '../../components/ui'
import { categoryLabel } from '../../domain/maintenance'
import './VehicleHistoryDialog.css'

export function VehicleHistoryDialog({ vehicle, onClose }) {
  const dialogRef = useRef(null)
  const titleRef = useRef(null)
  const exportingRef = useRef(false)
  const titleId = useId()
  const [page, setPage] = useState(0)
  const [attempt, setAttempt] = useState(0)
  const [result, setResult] = useState(null)
  const [exporting, setExporting] = useState(false)
  const [exportFeedback, setExportFeedback] = useState({})
  const historyPath = `/api/vehicles/${vehicle.id}/history`

  useEffect(() => {
    const dialog = dialogRef.current
    dialog.showModal()
    return () => { if (dialog.open) dialog.close() }
  }, [])

  useEffect(() => {
    let active = true
    api(`${historyPath}?page=${page}`).then(
      (data) => { if (active) setResult({ data }) },
      (error) => { if (active) setResult({ error: error.message }) },
    )
    return () => { active = false }
  }, [historyPath, page, attempt])

  function navigate(next) {
    titleRef.current?.focus({ preventScroll: true })
    setResult(null)
    setPage(next)
    setAttempt((previous) => previous + 1)
  }

  function close() {
    dialogRef.current.close()
    onClose()
  }

  async function download() {
    if (exportingRef.current) return
    exportingRef.current = true
    const dialog = dialogRef.current
    setExporting(true)
    setExportFeedback({})
    try {
      const file = await fetchCsv(`${historyPath}/export`)
      if (!dialog.isConnected || !dialog.open) return
      const url = URL.createObjectURL(file)
      try {
        const anchor = document.createElement('a')
        anchor.href = url
        anchor.download = `drivecare-vehicle-${vehicle.id}-history.csv`
        dialog.append(anchor)
        anchor.click()
        anchor.remove()
        setExportFeedback({ kind: 'success', message: 'CSV 다운로드를 요청했어요.' })
      } finally {
        window.setTimeout(() => URL.revokeObjectURL(url), 30_000)
      }
    } catch (error) {
      if (dialog.isConnected && dialog.open) setExportFeedback({ message: error.message })
    } finally {
      exportingRef.current = false
      if (dialog.isConnected && dialog.open) setExporting(false)
    }
  }

  const entries = result?.data?.entries ?? []
  return (
    <dialog ref={dialogRef} className="vehicle-history-dialog" aria-labelledby={titleId}
      onCancel={(event) => { event.preventDefault(); close() }}>
      <div className="vehicle-history-content">
        <div className="vehicle-history-header">
          <div>
            <p className="card-kicker">{vehicle.manufacturer} {vehicle.model}</p>
            <h3 id={titleId} ref={titleRef} tabIndex={-1}>차량 교체 이력</h3>
          </div>
          <button type="button" className="secondary-button" onClick={close}>닫기</button>
        </div>
        <p className="field-help">DriveCare에 직접 기록한 이력이에요. 관리가 끝난 소모품의 기록도 남아 있어요.</p>
        {!result && <p role="status">교체 이력을 불러오는 중이에요…</p>}
        {result?.error && <>
          <Feedback message={result.error} />
          <button type="button" className="secondary-button" onClick={() => navigate(page)}>다시 불러오기</button>
        </>}
        {result?.data && entries.length === 0 && <p>{page === 0 ? '아직 교체 기록이 없어요.' : '이 페이지에 교체 기록이 없어요.'}</p>}
        {entries.length > 0 && <ol className="vehicle-history-list" aria-label="최근 교체 기록">
          {entries.map((record) => <li key={record.id}>
            <time dateTime={record.replacedOn}>{record.replacedOn}</time>
            <div>
              <strong>{categoryLabel(record.category)}</strong>
              {!record.active && <span className="vehicle-history-retired">관리 종료</span>}
              <p>{record.productName || '제품명 미입력'}</p>
            </div>
            <span className="vehicle-history-km">{record.odometerKm.toLocaleString('ko-KR')} km</span>
          </li>)}
        </ol>}
        {result?.data && <div className="vehicle-history-footer">
          {(page > 0 || result.data.hasNext) && <nav aria-label="교체 이력 페이지">
            <button type="button" className="secondary-button" disabled={page === 0}
              onClick={() => navigate(page - 1)}>이전</button>
            <span aria-live="polite">{page + 1} 페이지</span>
            <button type="button" className="secondary-button" disabled={!result.data.hasNext}
              onClick={() => navigate(page + 1)}>다음</button>
          </nav>}
          {(entries.length > 0 || page > 0) && <button type="button" className="secondary-button"
            disabled={exporting} onClick={download}>{exporting ? '내보내는 중…' : '전체 이력 CSV'}</button>}
        </div>}
        <Feedback {...exportFeedback} />
      </div>
    </dialog>
  )
}
