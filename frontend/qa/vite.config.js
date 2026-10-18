import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Local, read-only UI fixtures. No proxy, database connection, credentials or writes.
const vehicles = [
  { id: 901, manufacturer: '현대', model: '검수용 아반떼', modelYear: 2023, currentOdometerKm: 120100, monthlyAverageKm: 1500 },
  { id: 902, manufacturer: '기아', model: '검수용 셀토스', modelYear: 2024, currentOdometerKm: 8500, monthlyAverageKm: null },
]
const items = [
  { id: 910, vehicleId: 901, category: 'CABIN_FILTER', productName: '검수용 에어컨 필터', lastReplacedOn: '2025-08-01', lastReplacedOdometerKm: 105000, intervalKm: 10000, intervalMonths: 12, purchaseUrl: null, revision: 0,
    status: { state: 'OVERDUE', remainingKm: -5100, nextOdometerKm: 115000, nextDate: '2026-08-01', estimatedMileageDate: null } },
  { id: 911, vehicleId: 901, category: 'ENGINE_OIL', productName: '검수용 엔진오일', lastReplacedOn: '2026-06-01', lastReplacedOdometerKm: 113000, intervalKm: 8000, intervalMonths: 12, purchaseUrl: null, revision: 0,
    status: { state: 'DUE_SOON', remainingKm: 900, nextOdometerKm: 121000, nextDate: '2027-06-01', estimatedMileageDate: '2026-11-02' } },
  { id: 912, vehicleId: 901, category: 'BATTERY', productName: '검수용 배터리', lastReplacedOn: '2026-09-01', lastReplacedOdometerKm: 119500, intervalKm: null, intervalMonths: 36, purchaseUrl: null, revision: 0,
    status: { state: 'GOOD', remainingKm: null, nextOdometerKm: null, nextDate: '2029-09-01', estimatedMileageDate: null } },
]

export default defineConfig({
  plugins: [react(), {
    name: 'drivecare-readonly-ui-fixtures',
    configureServer(server) {
      server.middlewares.use((request, response, next) => {
        if (!request.url.startsWith('/api/')) return next()
        const path = request.url.split('?')[0]
        const scenario = new URL(request.headers.referer || 'http://127.0.0.1:3001').searchParams.get('scenario')
        response.setHeader('Content-Type', 'application/json; charset=utf-8')
        response.setHeader('Cache-Control', 'no-store')
        const send = (status, value) => { response.statusCode = status; response.end(JSON.stringify(value)) }
        if (request.method === 'POST' && path === '/api/usage/visits') {
          response.statusCode = 204
          return response.end()
        }
        if (request.method !== 'GET') {
          // Deliberately never acknowledge a save: QA cannot accidentally masquerade as real persistence.
          const fields = scenario === 'field-error' ? { purchaseUrl: '검수용 오류: https://로 시작하는 판매처 주소를 확인해 주세요.' } : {}
          return setTimeout(() => send(400, { detail: '화면 검수 모드에서는 저장하지 않습니다. 입력 내용은 유지됩니다.', fieldErrors: fields }), 500)
        }
        if (path === '/api/auth/me') return send(200, { id: 99001, displayName: '화면 검수 계정', email: 'ui-qa@example.invalid' })
        if (path === '/api/auth/csrf') return send(200, { token: 'local-qa-not-a-real-token' })
        if (path === '/api/vehicles') return send(200, scenario === 'empty' ? [] : vehicles)
        if (path === '/api/maintenance-items') return send(200, scenario === 'empty' ? [] : items)
        if (path === '/api/notifications') return send(200, [])
        if (/^\/api\/maintenance-items\/\d+\/replacements$/.test(path)) return send(200, [])
        return send(404, { detail: '검수용 응답이 없는 경로입니다.' })
      })
    },
  }],
})
