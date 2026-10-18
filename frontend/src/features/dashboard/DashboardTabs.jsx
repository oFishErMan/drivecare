import { useRef } from 'react'
import { Icon } from '../../components/Icon'
import { DASHBOARD_TABS, nextTabIndex } from '../../domain/dashboardTabs'

export function DashboardTabs({ activeTab, onSelect, itemCount, vehicleCount, unreadCount }) {
  const buttons = useRef([])
  const counts = { maintenance: itemCount, vehicles: vehicleCount, notifications: unreadCount }

  function handleKey(event, index) {
    const next = nextTabIndex(index, event.key)
    if (next === null) return
    event.preventDefault()
    buttons.current[next]?.focus()
    onSelect(DASHBOARD_TABS[next].id)
  }

  return (
    <div className="dashboard-tabs" role="tablist" aria-label="차량 관리 메뉴">
      {DASHBOARD_TABS.map((tab, index) => (
        <button
          key={tab.id}
          ref={(node) => { buttons.current[index] = node }}
          type="button"
          role="tab"
          id={`tab-${tab.id}`}
          aria-controls={`panel-${tab.id}`}
          aria-selected={activeTab === tab.id}
          tabIndex={activeTab === tab.id ? 0 : -1}
          onClick={() => onSelect(tab.id)}
          onKeyDown={(event) => handleKey(event, index)}
        >
          <Icon name={tab.icon} />
          <span>{tab.label}</span>
          {counts[tab.id] !== undefined && (
            <small className={tab.id === 'notifications' && unreadCount ? 'tab-count unread' : 'tab-count'}
              aria-label={tab.id === 'notifications' ? `${unreadCount}개 읽지 않음` : undefined}>
              {counts[tab.id]}
            </small>
          )}
        </button>
      ))}
    </div>
  )
}

// Keep panels mounted so switching tabs never discards an unfinished form.
export function DashboardPanel({ id, activeTab, children }) {
  return (
    <section id={`panel-${id}`} className="dashboard-panel" role="tabpanel"
      aria-labelledby={`tab-${id}`} hidden={activeTab !== id} tabIndex={0}>
      {children}
    </section>
  )
}
