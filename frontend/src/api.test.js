import test from 'node:test'
import assert from 'node:assert/strict'
import { api } from './api.js'

const originalFetch = globalThis.fetch
test.afterEach(() => {
  globalThis.fetch = originalFetch
})
const json = (data, status = 200) =>
  new Response(JSON.stringify(data), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })

test('서버 장애 안내에 요청 추적 코드를 포함하고 내부 오류는 노출하지 않는다', async () => {
  const requestId = '0123456789abcdef0123456789abcdef'
  globalThis.fetch = async () => new Response(JSON.stringify({ code: 'INTERNAL_ERROR', detail: 'private SQL' }), {
    status: 500, headers: { 'Content-Type': 'application/problem+json', 'X-Request-ID': requestId },
  })
  await assert.rejects(api('/api/vehicles'), (error) => {
    assert.equal(error.requestId, requestId)
    assert.equal(error.code, 'INTERNAL_ERROR')
    assert.match(error.message, /문의 코드/)
    assert.ok(!error.message.includes('private SQL'))
    return true
  })
})

test('CSRF 요청의 연결 실패도 사용자 안내로 변환하고 저장 요청은 전송하지 않는다', async () => {
  const calls = []
  globalThis.fetch = async (path) => {
    calls.push(path)
    throw new TypeError('Failed to fetch')
  }
  await assert.rejects(api('/api/auth/login', { method: 'POST' }), /서버에 연결할 수 없습니다/)
  assert.deepEqual(calls, ['/api/auth/csrf'])
})

test('잘못된 로그인은 CSRF 토큰을 보내고 계정 정보 오류로 안내한다', async () => {
  globalThis.fetch = async (path, options) => {
    if (path === '/api/auth/csrf') return json({ token: 'test-token' })
    assert.equal(options.headers['X-XSRF-TOKEN'], 'test-token')
    assert.equal(options.credentials, 'include')
    return json({ message: 'Unauthorized' }, 401)
  }
  await assert.rejects(
    api('/api/auth/login', { method: 'POST' }),
    /이메일 또는 비밀번호가 일치하지 않습니다/,
  )
})

test('HTML 프록시 오류를 화면에 노출하지 않는다', async () => {
  globalThis.fetch = async () => new Response('<html>502 Bad Gateway</html>', { status: 502 })
  await assert.rejects(api('/api/vehicles'), /서버가 요청을 처리하지 못했습니다/)
})

test('서버 응답 지연을 연결 실패와 구분한다', async () => {
  globalThis.fetch = async () => {
    throw new DOMException('timeout', 'TimeoutError')
  }
  await assert.rejects(api('/api/vehicles'), /서버 응답이 늦어지고 있습니다/)
})

test('중복 이메일은 가입 대신 로그인을 안내한다', async () => {
  globalThis.fetch = async () => json({}, 409)
  await assert.rejects(api('/api/auth/register', { method: 'POST' }), /이미 가입된 이메일입니다/)
})

test('조회 중 세션이 만료되면 로그인 화면 전환 이벤트를 보낸다', async () => {
  const previousWindow = globalThis.window
  globalThis.window = new EventTarget()
  let expired = false
  globalThis.window.addEventListener('drivecare:session-expired', () => {
    expired = true
  })
  globalThis.fetch = async () => json({}, 401)
  try {
    await assert.rejects(api('/api/vehicles'), /로그인이 만료되었습니다/)
    assert.equal(expired, true)
  } finally {
    globalThis.window = previousWindow
  }
})

test('소모품 삭제는 CSRF 토큰과 버전을 전달하고 빈 204 응답을 처리한다', async () => {
  let deleted = false
  globalThis.fetch = async (path, options) => {
    if (path === '/api/auth/csrf') return json({ token: 'test-token' })
    assert.equal(path, '/api/maintenance-items/42?expectedRevision=3')
    assert.equal(options.method, 'DELETE')
    assert.equal(options.headers['X-XSRF-TOKEN'], 'test-token')
    deleted = true
    return new Response(null, { status: 204 })
  }
  assert.equal(await api('/api/maintenance-items/42?expectedRevision=3', { method: 'DELETE' }), null)
  assert.equal(deleted, true)
})

test('서버 필드 오류를 입력 화면에 전달하고 문자열이 아닌 항목은 제외한다', async () => {
  globalThis.fetch = async () => json({ detail: '입력값을 확인해주세요.',
    fieldErrors: { intervalKm: '1 이상 입력해 주세요.', unrelated: { private: 'value' } } }, 400)
  await assert.rejects(api('/api/maintenance-items', { method: 'POST' }), (error) => {
    assert.deepEqual(error.fieldErrors, { intervalKm: '1 이상 입력해 주세요.' })
    assert.equal(error.status, 400)
    return true
  })
})
