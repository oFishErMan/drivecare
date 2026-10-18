import { useEffect, useRef, useState } from 'react'
import { api } from '../api'

export function useSaveFeedback(onCreated) {
  const [busy, setBusy] = useState(false)
  const [feedback, setFeedback] = useState({})
  const [fieldErrors, setFieldErrors] = useState({})
  const formRef = useRef(null)
  const savingRef = useRef(false)
  const fail = (message, errors = {}) => {
    setFeedback({ message, kind: 'error' })
    setFieldErrors(errors)
  }
  function clearFieldError(name) {
    if (!name) return
    const names = [name, ...(['intervalKm', 'intervalMonths'].includes(name) ? ['intervalPresent', 'intervalKm'] : [])]
    if (names.some((key) => fieldErrors[key])) {
      setFieldErrors((current) => Object.fromEntries(Object.entries(current).filter(([key]) => !names.includes(key))))
      setFeedback({})
    } else if (feedback.kind === 'success') setFeedback({})
  }
  useEffect(() => {
    for (const name of Object.keys(fieldErrors)) {
      const field = formRef.current?.elements.namedItem(name === 'intervalPresent' ? 'intervalKm' : name)
      if (field?.focus) {
        const details = field.closest('details')
        if (details) details.open = true
        field.focus()
        break
      }
    }
  }, [fieldErrors])
  async function save(path, body, message, reset) {
    if (savingRef.current) return
    savingRef.current = true
    setBusy(true)
    setFeedback({})
    setFieldErrors({})
    try {
      const result = await api(path, { method: 'POST', body: JSON.stringify(body) })
      reset()
      setFeedback({ message, kind: 'success' })
      // A refresh failure must not turn a committed save into a retryable save error.
      try {
        await onCreated(result)
      } catch {
        setFeedback({ kind: 'info', message: '저장은 완료했지만 화면을 갱신하지 못했어요. 다시 저장하지 말고 새로고침해 주세요.' })
      }
    } catch (cause) {
      fail(cause.message, cause.fieldErrors)
    } finally {
      savingRef.current = false
      setBusy(false)
    }
  }
  return { busy, feedback, fieldErrors, formRef, save, fail, clearFieldError }
}
