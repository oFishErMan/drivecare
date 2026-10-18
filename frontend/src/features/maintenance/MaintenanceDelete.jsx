import { useId, useRef, useState } from 'react'
import { api } from '../../api'
import { Icon } from '../../components/Icon'
import { Feedback } from '../../components/ui'
import { categoryLabel } from '../../domain/maintenance'

export function MaintenanceDelete({ item, disabled, onDeleted }) {
  const dialog = useRef(null)
  const titleId = useId()
  const descriptionId = useId()
  const [revision, setRevision] = useState(item.revision)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')

  function open() {
    setRevision(item.revision)
    setError('')
    dialog.current.showModal()
  }

  async function remove(event) {
    event.preventDefault()
    if (busy) return
    setBusy(true)
    setError('')
    try {
      await api(`/api/maintenance-items/${item.id}?expectedRevision=${revision}`, { method: 'DELETE' })
    } catch (cause) {
      setError(cause.message)
      setBusy(false)
      return
    }
    dialog.current.close()
    setBusy(false)
    onDeleted(item.id)
  }

  return (
    <>
      <button type="button" className="link delete-action" disabled={disabled || busy} onClick={open}>
        <Icon name="trash" /> 소모품 삭제
      </button>
      <dialog
        ref={dialog}
        className="delete-dialog"
        aria-labelledby={titleId}
        aria-describedby={descriptionId}
        onCancel={(event) => { if (busy) event.preventDefault() }}
      >
        <form onSubmit={remove} aria-busy={busy}>
          <span className="delete-dialog-icon"><Icon name="trash" /></span>
          <h2 id={titleId}>소모품을 삭제할까요?</h2>
          <p className="delete-item-name">{item.productName || categoryLabel(item.category)}</p>
          <p id={descriptionId}>
            소모품 목록과 관련 알림에서 제외되며, 더 이상 교체 알림을 받지 않습니다.
            기존 교체 이력은 보관되지만 현재 화면에서 조회하거나 복구할 수는 없습니다.
          </p>
          <Feedback message={error} />
          <div className="delete-dialog-actions">
            <button type="button" className="cancel-button" disabled={busy} autoFocus onClick={() => dialog.current.close()}>
              취소
            </button>
            <button type="submit" className="danger-button" disabled={busy}>
              {busy ? '삭제 중…' : '삭제하기'}
            </button>
          </div>
        </form>
      </dialog>
    </>
  )
}
