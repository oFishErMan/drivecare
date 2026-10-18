import { useEffect, useState } from 'react'
import { DASHBOARD_TABS, tabFromHash } from '../domain/dashboardTabs'

export function useDashboardTabs() {
  const [hash, setHash] = useState(() => window.location.hash)
  useEffect(() => {
    const sync = () => {
      setHash(window.location.hash)
      window.scrollTo({ top: 0, behavior: 'instant' })
    }
    window.addEventListener('hashchange', sync)
    window.addEventListener('popstate', sync)
    return () => {
      window.removeEventListener('hashchange', sync)
      window.removeEventListener('popstate', sync)
    }
  }, [])

  function selectTab(id) {
    const tab = DASHBOARD_TABS.find((value) => value.id === id)
    if (!tab) return
    // Avoid native fragment navigation stealing keyboard focus from the tab.
    if (window.location.hash !== tab.hash) window.history.pushState(null, '', tab.hash)
    setHash(tab.hash)
    window.scrollTo({ top: 0, behavior: 'instant' })
    window.requestAnimationFrame(() => document.getElementById(`tab-${id}`)?.focus({ preventScroll: true }))
  }

  return { activeTab: tabFromHash(hash), hash, selectTab }
}
