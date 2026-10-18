import { notificationMessage } from '../../domain/maintenance'

export async function enableBrowserNotifications() {
  if (!('Notification' in window))
    return '이 브라우저는 브라우저 알림을 지원하지 않습니다. 화면의 알림 목록에서 확인할 수 있습니다.'
  const permission = await Notification.requestPermission()
  return permission === 'granted'
    ? '브라우저 알림이 켜졌습니다. 이 페이지가 열려 있을 때 새 정비 알림을 표시합니다.'
    : permission === 'denied'
      ? '알림이 차단되어 있습니다. 브라우저의 사이트 설정에서 알림을 허용해 주세요. 화면의 알림 목록은 계속 이용할 수 있습니다.'
      : '알림 설정을 보류했습니다. 나중에 알림 설정에서 다시 켤 수 있습니다.'
}

export function showBrowserNotifications(items) {
  if (!('Notification' in window) || Notification.permission !== 'granted') return
  try {
    const seen = new Set(JSON.parse(localStorage.getItem('drivecare-notifications') || '[]'))
    items
      .filter((item) => !item.readAt && !seen.has(item.id))
      .forEach((item) => {
        new Notification(item.title, { body: notificationMessage(item) })
        seen.add(item.id)
      })
    localStorage.setItem('drivecare-notifications', JSON.stringify([...seen].slice(-100)))
  } catch {
    /* 알림이나 저장소 제한이 대시보드 조회를 막지 않도록 합니다. */
  }
}
