import { useCallback, useEffect, useRef, useState } from 'react'
import { api } from '../api'
import {
  enableBrowserNotifications,
  showBrowserNotifications,
} from '../features/notifications/browserNotifications'

export function useDashboard() {
  const [user, setUser] = useState(undefined)
  const [vehicles, setVehicles] = useState([])
  const [items, setItems] = useState([])
  const [notifications, setNotifications] = useState([])
  const [error, setError] = useState('')
  const [authNotice, setAuthNotice] = useState('')
  const [notice, setNotice] = useState('')
  const [refreshing, setRefreshing] = useState(false)
  const [loggingOut, setLoggingOut] = useState(false)
  const [dashboardReady, setDashboardReady] = useState(false)
  const [lastUpdated, setLastUpdated] = useState(null)
  const requestVersion = useRef(0)

  // Page entry/login only: periodic dashboard refreshes are not user visits.
  useEffect(() => {
    if (user?.id) api('/api/usage/visits', { method: 'POST' }).catch(() => {})
  }, [user?.id])

  const loadDashboard = useCallback(async () => {
    const version = ++requestVersion.current
    const data = await Promise.all([
      api('/api/vehicles'),
      api('/api/maintenance-items'),
      api('/api/notifications'),
    ]).catch((cause) => {
      if (version !== requestVersion.current) return null
      throw cause
    })
    if (!data || version !== requestVersion.current) return
    const [vehicleData, itemData, notificationData] = data
    setVehicles(vehicleData)
    setItems(itemData)
    setNotifications(notificationData)
    setDashboardReady(true)
    setError('')
    setLastUpdated(new Date())
    showBrowserNotifications(notificationData)
  }, [])

  useEffect(() => {
    api('/api/auth/me')
      .then((me) => {
        setUser(me)
        loadDashboard().catch((cause) => setError(cause.message))
      })
      .catch((cause) => {
        setUser(null)
        if (cause.status !== 401) setAuthNotice(cause.message)
      })
    const expired = () => {
      requestVersion.current++
      setUser(null)
      setDashboardReady(false)
      setAuthNotice('로그인이 만료되었습니다. 다시 로그인해 주세요.')
    }
    window.addEventListener('drivecare:session-expired', expired)
    return () => window.removeEventListener('drivecare:session-expired', expired)
  }, [loadDashboard])

  useEffect(() => {
    if (!user) return undefined
    const timer = window.setInterval(
      () => loadDashboard().catch((cause) => setError(cause.message)),
      60_000,
    )
    return () => window.clearInterval(timer)
  }, [user, loadDashboard])

  async function refresh() {
    setRefreshing(true)
    try {
      await loadDashboard()
    } catch (cause) {
      setError(cause.message)
    } finally {
      setRefreshing(false)
    }
  }

  async function enableNotifications() {
    try {
      setNotice(await enableBrowserNotifications())
    } catch {
      setError('알림 설정을 변경하지 못했습니다. 브라우저의 알림 설정을 확인해 주세요.')
    }
  }

  async function logout() {
    setLoggingOut(true)
    try {
      await api('/api/auth/logout', { method: 'POST' })
      requestVersion.current++
      setAuthNotice('')
      setDashboardReady(false)
      setError('')
      setUser(null)
    } catch (cause) {
      setError(cause.message)
    } finally {
      setLoggingOut(false)
    }
  }

  function authenticate(me) {
    setUser(me)
    setAuthNotice('')
    setNotice('')
    loadDashboard().catch((cause) => setError(cause.message))
  }

  function dismissNotice() {
    setNotice('')
  }
  function maintenanceDeleted(itemId) {
    setItems((current) => current.filter((item) => item.id !== itemId))
    setNotifications((current) => current.filter((notification) => notification.maintenanceItemId !== itemId))
    setNotice('소모품을 삭제했습니다. 해당 소모품의 교체 알림도 중단했습니다.')
    refresh()
  }
  function markNotificationRead(updated) {
    setNotifications((current) =>
      current.map((value) => (value.id === updated.id ? updated : value)),
    )
  }

  return {
    user,
    vehicles,
    items,
    notifications,
    error,
    authNotice,
    notice,
    refreshing,
    loggingOut,
    dashboardReady,
    lastUpdated,
    refresh,
    enableNotifications,
    logout,
    authenticate,
    dismissNotice,
    showNotice: setNotice,
    markNotificationRead,
    maintenanceDeleted,
  }
}
