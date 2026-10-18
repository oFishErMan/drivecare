import { useState } from 'react'
import { api } from '../../api'
import { Icon } from '../../components/Icon'
import { Feedback } from '../../components/ui'
import { notificationMessage } from '../../domain/maintenance'

export function NotificationRow({ notification, onRead, onOpenItem }) {
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  async function markRead() {
    setBusy(true)
    setError('')
    try {
      onRead(
        await api(`/api/notifications/${notification.id}/read`, {
          method: 'PATCH',
        }),
      )
    } catch (cause) {
      setError(cause.message)
    } finally {
      setBusy(false)
    }
  }
  return (
    <div className={notification.readAt ? 'notification read' : 'notification'}>
      <span className={`notification-icon notification-${notification.type?.toLowerCase()}`}>
        <Icon name="bell" />
      </span>
      <div>
        <strong>{notification.title}</strong>
        <span>{notificationMessage(notification)}</span>
        <small className="muted">
          {new Date(notification.createdAt).toLocaleString('ko-KR')} 기준 알림 · 최신 정보는 소모품
          상태에서 확인하세요.
        </small>
        <Feedback message={error} />
        {notification.maintenanceItemId != null && (
          <button type="button" className="link notification-detail" onClick={() => onOpenItem(notification.maintenanceItemId)}>
            이 소모품의 현재 상태 보기 <Icon name="arrow" />
          </button>
        )}
      </div>
      {notification.readAt ? (
        <small className="muted">확인 완료</small>
      ) : (
        <button type="button" className="link" disabled={busy} onClick={markRead}>
          {busy ? '처리 중…' : '읽음 표시'}
        </button>
      )}
    </div>
  )
}
