let csrfToken

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.requestId = undefined
    this.code = undefined
    this.fieldErrors = {}
  }
}

export function responseError(path, status, data) {
  if (status === 401)
    return new ApiError(
      path === '/api/auth/login'
        ? '이메일 또는 비밀번호가 일치하지 않습니다. 다시 확인해 주세요.'
        : '로그인이 만료되었습니다. 다시 로그인해 주세요.',
      status,
    )
  if (status === 403)
    return new ApiError(
      '요청을 확인할 수 없습니다. 화면을 새로고침한 후 다시 시도해 주세요.',
      status,
    )
  if (status === 409 && path === '/api/auth/register')
    return new ApiError(
      '이미 가입된 이메일입니다. 로그인하거나 다른 이메일을 입력해 주세요.',
      status,
    )
  if (status === 429)
    return new ApiError('요청이 많습니다. 잠시 기다린 후 다시 시도해 주세요.', status)
  if (status >= 500)
    return new ApiError('서버가 요청을 처리하지 못했습니다. 잠시 후 다시 시도해 주세요.', status)
  if (status === 404)
    return new ApiError('요청한 정보를 찾을 수 없습니다. 화면을 새로고침해 주세요.', status)
  const detail = data?.detail || data?.message
  const message =
    typeof detail === 'string' && /[가-힣]/.test(detail)
      ? detail
      : '입력한 정보를 확인한 후 다시 시도해 주세요.'
  return new ApiError(message, status)
}

async function request(path, options = {}) {
  let response
  try {
    response = await fetch(path, {
      ...options,
      credentials: 'include',
      signal: AbortSignal.timeout(15000),
    })
  } catch (cause) {
    throw new ApiError(
      cause.name === 'TimeoutError' || cause.name === 'AbortError'
        ? '서버 응답이 늦어지고 있습니다. 잠시 후 다시 시도해 주세요.'
        : '서버에 연결할 수 없습니다. 인터넷 연결을 확인하고 잠시 후 다시 시도해 주세요.',
    )
  }
  if (response.status === 204) return null
  let data
  try {
    data = response.headers.get('content-type')?.includes('json') ? await response.json() : null
  } catch {
    data = null
  }
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith('/api/auth/'))
      window.dispatchEvent(new Event('drivecare:session-expired'))
    const error = responseError(path, response.status, data)
    const requestId = response.headers.get('X-Request-ID') || data?.requestId
    if (typeof requestId === 'string' && /^[a-f0-9-]{32,36}$/i.test(requestId)) {
      error.requestId = requestId
      if (response.status >= 500) error.message += ` (문의 코드: ${requestId})`
    }
    error.code = data?.code
    if (data?.fieldErrors && typeof data.fieldErrors === 'object') {
      error.fieldErrors = Object.fromEntries(
        Object.entries(data.fieldErrors).filter(([, value]) => typeof value === 'string'),
      )
    }
    throw error
  }
  if (data === null)
    throw new ApiError('서버 응답을 확인할 수 없습니다. 화면을 새로고침한 후 다시 시도해 주세요.')
  return data
}

async function ensureCsrf() {
  if (!csrfToken) {
    const data = await request('/api/auth/csrf')
    if (!data.token) throw new ApiError('요청을 준비하지 못했습니다. 화면을 새로고침해 주세요.')
    csrfToken = data.token
  }
  return csrfToken
}

export async function api(path, options = {}) {
  const method = options.method ?? 'GET'
  const headers = { ...(options.headers ?? {}) }
  if (method !== 'GET' && method !== 'HEAD') headers['X-XSRF-TOKEN'] = await ensureCsrf()
  if (options.body && !(options.body instanceof FormData))
    headers['Content-Type'] = 'application/json'
  try {
    const result = await request(path, { ...options, method, headers })
    if (path === '/api/auth/login' || path === '/api/auth/logout') csrfToken = undefined
    return result
  } catch (cause) {
    if (cause.status === 403) csrfToken = undefined
    throw cause
  }
}
