import { useState } from 'react'
import { useDashboard } from './hooks/useDashboard'
import { useDashboardTabs } from './hooks/useDashboardTabs'
import { AuthScreen } from './features/auth/AuthScreen'
import { DashboardHero } from './features/dashboard/DashboardHero'
import { DashboardTabs, DashboardPanel } from './features/dashboard/DashboardTabs'
import { VehicleRegistrationDrawer } from './features/vehicles/VehicleRegistrationDrawer'
import { VehicleCard } from './features/vehicles/VehicleCard'
import { MaintenanceForm } from './features/maintenance/MaintenanceForm'
import { MaintenanceOverview } from './features/maintenance/MaintenanceOverview'
import { EMPTY_FILTERS } from './domain/maintenanceView'
import { blankMaintenanceForm } from './domain/forms'
import { NotificationCenter } from './features/notifications/NotificationCenter'
import { Icon } from './components/Icon'
import { EmptyState, Feedback } from './components/ui'
import './App.css'
import './experience.css'
import './journey.css'

export default function App() {
  const dashboard = useDashboard()
  if (dashboard.user === undefined) return <div className="loading" role="status">DriveCare를 불러오는 중…</div>
  if (!dashboard.user) return <AuthScreen initialNotice={dashboard.authNotice} onAuthenticated={dashboard.authenticate} />
  return <DashboardContent key={dashboard.user.id} dashboard={dashboard} />
}

function DashboardContent({ dashboard }) {
  const {
    user, vehicles, items, notifications, error, notice,
    refreshing, loggingOut, dashboardReady, lastUpdated, refresh, enableNotifications,
    logout, dismissNotice, showNotice, markNotificationRead, maintenanceDeleted,
  } = dashboard
  const { activeTab, hash, selectTab } = useDashboardTabs()
  const [showRegistration, setShowRegistration] = useState(false)
  const [filters, setFilters] = useState({ ...EMPTY_FILTERS })
  const [maintenanceDraft, setMaintenanceDraft] = useState(blankMaintenanceForm)
  const showVehicleForm = activeTab === 'vehicles' && (showRegistration || hash === '#vehicle-form')
  const unreadCount = notifications.filter((notification) => !notification.readAt).length

  async function vehicleCreated() {
    await refresh()
    setShowRegistration(false)
    showNotice('차량을 등록했습니다. 차량 카드에서 소모품 등록을 이어갈 수 있어요.')
    selectTab('vehicles')
  }

  async function maintenanceCreated() {
    await refresh()
    setFilters({ ...EMPTY_FILTERS })
    showNotice('소모품을 등록했습니다. 다음 교체 시점을 확인해 보세요.')
    selectTab('maintenance')
  }

  function closeVehicleForm() {
    setShowRegistration(false)
    if (hash === '#vehicle-form') selectTab('vehicles')
  }

  function registerForVehicle(vehicleId) {
    if (vehicleId) {
      const selectedId = maintenanceDraft.vehicleId || (vehicles.length === 1 ? String(vehicles[0].id) : '')
      const blank = blankMaintenanceForm()
      const hasDraft = Object.keys(blank).some((key) => key !== 'vehicleId' && maintenanceDraft[key] !== blank[key])
      const changingVehicle = selectedId !== String(vehicleId)
      if (changingVehicle && hasDraft
        && !window.confirm('다른 차량의 소모품을 작성 중이에요. 작성 내용을 지우고 선택한 차량으로 바꿀까요?')) {
        selectTab('register')
        return
      }
      if (changingVehicle) setMaintenanceDraft(blankMaintenanceForm(vehicleId))
      else setMaintenanceDraft((current) => ({ ...current, vehicleId: String(vehicleId) }))
    }
    selectTab('register')
  }

  function manageVehicle(vehicleId) {
    if (!vehicles.length) setShowRegistration(true)
    selectTab('vehicles')
    if (vehicleId) window.requestAnimationFrame(() => {
      const card = document.getElementById(`vehicle-${vehicleId}`)
      card?.focus({ preventScroll: true })
      card?.scrollIntoView({ block: 'start', behavior: 'instant' })
    })
  }

  function deleted(itemId) {
    if (filters.itemId === itemId) setFilters({ ...EMPTY_FILTERS })
    maintenanceDeleted(itemId)
    window.requestAnimationFrame(() => document.getElementById('tab-maintenance')?.focus({ preventScroll: true }))
  }

  function openNotificationItem(itemId) {
    setFilters({ ...EMPTY_FILTERS, itemId })
    selectTab('maintenance')
  }

  function viewVehicleMaintenance(vehicleId, category) {
    setFilters({ ...EMPTY_FILTERS, vehicleId: String(vehicleId), category: category || '' })
    selectTab('maintenance')
  }

  if (!dashboardReady)
    return (
      <main className="dashboard-loading">
        <div className="panel">
          <h2>{error ? '차량 정보를 불러오지 못했어요' : '내 차량 정보를 확인하고 있어요'}</h2>
          {error ? (
            <>
              <Feedback message={error} />
              <button disabled={refreshing} onClick={refresh}>{refreshing ? '불러오는 중…' : '다시 불러오기'}</button>
              <button className="link" disabled={loggingOut} onClick={logout}>로그아웃</button>
            </>
          ) : <p className="muted" role="status">잠시만 기다려 주세요.</p>}
        </div>
      </main>
    )

  return (
    <div className="app">
      <a className="skip-link" href="#main-content" onClick={(event) => {
        event.preventDefault()
        document.getElementById('main-content')?.focus()
      }}>본문으로 바로가기</a>
      <header>
        <a className="brand" href="#maintenance-status" aria-label="DriveCare 홈"
          onClick={(event) => { event.preventDefault(); selectTab('maintenance') }}>
          <img src="/drivecare-icon.svg" width="34" height="34" alt="" /><strong>DriveCare</strong>
        </a>
        <nav aria-label="계정 메뉴">
          <span>{user.displayName}</span>
          <button className="link" onClick={() => selectTab('notifications')}>브라우저 알림</button>
          <button className="link" disabled={loggingOut} onClick={logout}>{loggingOut ? '로그아웃 중…' : '로그아웃'}</button>
        </nav>
      </header>
      <main className="tabbed-dashboard" id="main-content" tabIndex={-1}>
        <DashboardHero vehicles={vehicles} items={items} lastUpdated={lastUpdated} refreshing={refreshing} onRefresh={refresh} />
        <DashboardTabs activeTab={activeTab} onSelect={selectTab}
          itemCount={items.length} vehicleCount={vehicles.length} unreadCount={unreadCount} />
        <div className="dashboard-feedback">
          {error && <Feedback message={error}>
            <span>최근에 불러온 정보를 보여드리고 있어요.</span>
            <button type="button" className="link" disabled={refreshing} onClick={refresh}>
              {refreshing ? '불러오는 중…' : '다시 불러오기'}
            </button>
          </Feedback>}
          {notice && <Feedback kind="info" message={notice}>
            <button type="button" className="link" onClick={dismissNotice}>닫기</button>
          </Feedback>}
        </div>
        <DashboardPanel id="maintenance" activeTab={activeTab}>
          <MaintenanceOverview items={items} vehicles={vehicles} filters={filters} onFilter={setFilters}
            onRegister={() => registerForVehicle(filters.vehicleId)} onVehicle={manageVehicle}
            onUpdated={refresh} onDeleted={deleted} />
        </DashboardPanel>
        <DashboardPanel id="vehicles" activeTab={activeTab}>
          <div className="tab-panel-heading">
            <div><h2>내 차량</h2><p>최신 주행거리로 갱신하면 소모품 상태도 다시 계산돼요.</p></div>
            <button type="button" className="tab-primary-action" onClick={() => setShowRegistration(true)}
              aria-haspopup="dialog" aria-controls="vehicle-registration">
              <Icon name="plus" /> 차량 등록
            </button>
          </div>
          {!vehicles.length && <EmptyState icon="car" title="첫 차량을 등록해 보세요"
            description="차량을 등록하면 주행거리와 소모품 교체 시점을 함께 관리할 수 있어요."
            actionLabel="첫 차량 등록" onAction={() => setShowRegistration(true)} />}
          <div className="grid cards vehicle-cards">
            {vehicles.map((vehicle) => <VehicleCard vehicle={vehicle} items={items} key={vehicle.id}
              onUpdated={refresh} onRegister={registerForVehicle} onViewMaintenance={viewVehicleMaintenance} />)}
          </div>
        </DashboardPanel>
        <DashboardPanel id="register" activeTab={activeTab}>
          <MaintenanceForm vehicles={vehicles} items={items} onCreated={maintenanceCreated}
            form={maintenanceDraft} setForm={setMaintenanceDraft} onViewExisting={viewVehicleMaintenance}
            onManageVehicle={manageVehicle} />
        </DashboardPanel>
        <DashboardPanel id="notifications" activeTab={activeTab}>
          <NotificationCenter notifications={notifications} onRead={markNotificationRead}
            onOpenItem={openNotificationItem} onEnable={enableNotifications} />
        </DashboardPanel>
      </main>
      <VehicleRegistrationDrawer open={showVehicleForm} onClose={closeVehicleForm} onCreated={vehicleCreated} />
    </div>
  )
}
