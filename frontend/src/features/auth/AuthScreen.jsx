import { useRef, useState } from 'react'
import { api } from '../../api'
import { Field, Feedback } from '../../components/ui'

export function AuthScreen({ onAuthenticated, initialNotice }) {
  const [register, setRegister] = useState(false)
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [success, setSuccess] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const formRef = useRef(null)
  async function submit(event) {
    event.preventDefault()
    if (busy) return
    const credentials = Object.fromEntries(new FormData(event.currentTarget))
    credentials.email = credentials.email.trim()
    if (register && !credentials.displayName.trim()) {
      setError('이름을 입력해 주세요.')
      return
    }
    setError('')
    setFieldErrors({})
    setBusy(true)
    try {
      if (register) {
        await api('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify(credentials),
        })
        setRegister(false)
        setSuccess('회원가입이 완료되었습니다. 입력한 계정으로 로그인해 주세요.')
        return
      }
      const me = await api('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({
          email: credentials.email,
          password: credentials.password,
        }),
      })
      onAuthenticated(me)
    } catch (cause) {
      setError(cause.message)
      setFieldErrors(cause.fieldErrors || {})
      formRef.current?.elements.namedItem(Object.keys(cause.fieldErrors || {})[0])?.focus()
    } finally {
      setBusy(false)
    }
  }
  return (
    <main className="auth-shell">
      <div className="auth-copy">
        <p className="eyebrow">DRIVECARE</p>
        <h1>
          차량 관리를
          <br />더 간단하게.
        </h1>
        <p className="muted">교체 기록을 모아두고, 다음 정비 시점을 한눈에 확인하세요.</p>
      </div>
      <form ref={formRef} className="panel auth-panel" onSubmit={submit} aria-busy={busy} aria-label={register ? '회원가입' : '로그인'}>
        <h2>{register ? '회원가입' : '다시 만나 반가워요'}</h2>
        <p className="muted auth-description">
          {register
            ? '내 차량 관리를 시작할 계정을 만들어 주세요.'
            : '이메일과 비밀번호로 로그인해 주세요.'}
        </p>
        {initialNotice && !error && <Feedback kind="info" message={initialNotice} />}
        {success && <Feedback kind="success" message={success} />}
        {register && (
          <Field key="displayName" name="displayName" label="이름" error={fieldErrors.displayName} autoComplete="name" maxLength={80} required />
        )}
        <Field
          key="email"
          error={fieldErrors.email}
          name="email"
          label="이메일"
          type="email"
          placeholder="name@example.com"
          autoComplete="username"
          required
        />
        <Field
          key="password"
          error={fieldErrors.password}
          name="password"
          label="비밀번호"
          type={showPassword ? 'text' : 'password'}
          minLength={register ? 8 : undefined}
          maxLength={register ? 72 : undefined}
          autoComplete={register ? 'new-password' : 'current-password'}
          required
        />
        <button
          type="button"
          className="link password-toggle"
          aria-pressed={showPassword}
          onClick={() => setShowPassword(!showPassword)}
        >
          {showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
        </button>
        {register && <p className="field-help">비밀번호는 8~72자로 입력해 주세요.</p>}
        {error && <Feedback message={error} />}
        <button type="submit" disabled={busy}>
          {busy ? (register ? '가입 중…' : '로그인 중…') : register ? '계정 만들기' : '로그인'}
        </button>
        <button
          type="button"
          disabled={busy}
          className="link"
          onClick={() => {
            setRegister(!register)
            setError('')
            setFieldErrors({})
            setSuccess('')
            setShowPassword(false)
          }}
        >
          {register ? '이미 계정이 있어요 · 로그인' : '처음 오셨나요? · 회원가입'}
        </button>
      </form>
    </main>
  )
}
