import { useEffect, useState } from 'react'
import { Icon } from '../../components/Icon'
import { EmptyState } from '../../components/ui'
import { NotificationRow } from './NotificationRow'

function browserPermission() {
  return 'Notification' in window ? Notification.permission : 'unsupported'
}

export function NotificationCenter({ notifications, onRead, onOpenItem, onEnable }) {
  const [unreadOnly, setUnreadOnly] = useState(false)
  const [permission, setPermission] = useState(browserPermission)
  const [requesting, setRequesting] = useState(false)
  const unread = notifications.filter((notification) => !notification.readAt).length
  const visible = notifications.filter((notification) => !unreadOnly || !notification.readAt)
    .sort((a, b) => Number(Boolean(a.readAt)) - Number(Boolean(b.readAt))
      || new Date(b.createdAt) - new Date(a.createdAt))

  useEffect(() => {
    const update = () => setPermission(browserPermission())
    window.addEventListener('focus', update)
    return () => window.removeEventListener('focus', update)
  }, [])

  async function enable() {
    setRequesting(true)
    try { await onEnable() }
    finally { setPermission(browserPermission()); setRequesting(false) }
  }

  const permissionLabel = {
    granted: '허용됨', denied: '차단됨', default: '설정 전', unsupported: '지원하지 않는 브라우저',
  }[permission]

  return (
    <>
      <div className="tab-panel-heading">
        <div><h2>정비 알림</h2><p>읽지 않은 알림 {unread}개 · 과거 알림과 현재 정비 상태는 다를 수 있어요.</p></div>
      </div>
      <div className="notification-preferences">
        <span className="icon-box"><Icon name="bell" /></span>
        <div><h3>브라우저 알림 <span className="permission-badge">{permissionLabel}</span></h3>
          <p>이 페이지가 열려 있을 때 새 정비 알림을 표시해요. 페이지를 닫으면 브라우저 알림은 오지 않아요.</p>
          {permission === 'denied' && <p>브라우저의 사이트 설정에서 알림을 허용해 주세요.</p>}
        </div>
        {permission !== 'granted' && permission !== 'unsupported' && permission !== 'denied' && (
          <button type="button" className="secondary-button" disabled={requesting} onClick={enable}>
            {requesting ? '확인 중…' : '브라우저 알림 켜기'}
          </button>
        )}
      </div>
      <div className="notification-filters" role="group" aria-label="알림 조회 조건">
        <button type="button" aria-pressed={!unreadOnly} onClick={() => setUnreadOnly(false)}>전체 {notifications.length}</button>
        <button type="button" aria-pressed={unreadOnly} onClick={() => setUnreadOnly(true)}>미확인 {unread}</button>
      </div>
      {visible.length ? (
        <div className="notification-list">
          {visible.map((notification) => (
            <NotificationRow key={notification.id} notification={notification} onRead={onRead} onOpenItem={onOpenItem} />
          ))}
        </div>
      ) : (
        <EmptyState icon="bell" title={unreadOnly ? '미확인 알림이 없어요' : '아직 도착한 알림이 없어요'}
          description={unreadOnly ? '확인한 알림은 전체 목록에서 다시 볼 수 있어요.' : '등록한 교체주기를 기준으로 정비 시점이 가까워지면 알려드려요.'}
          actionLabel={unreadOnly ? '전체 알림 보기' : undefined} onAction={() => setUnreadOnly(false)} />
      )}
    </>
  )
}
