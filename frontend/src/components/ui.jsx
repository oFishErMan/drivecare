import { Icon } from './Icon'
import { useEffect, useId, useRef } from 'react'

export function FormHeader({ step, icon, title, description, headingId }) {
  return (
    <div className="form-header">
      <span className="form-icon">
        <Icon name={icon} />
      </span>
      <div>
        <span className="form-step">STEP {step}</span>
        <h2 id={headingId}>{title}</h2>
        <p>{description}</p>
      </div>
    </div>
  )
}

export function Field({ label, onChange, suffix, error, help, ...props }) {
  const inputRef = useRef(null)
  // Quick-fill and changing vehicle constraints do not dispatch input events.
  useEffect(() => { inputRef.current?.setCustomValidity('') }, [props.value, props.min, props.max, props.type])
  const generatedId = useId()
  const id = props.id || generatedId
  const describedBy = [props['aria-describedby'], error && `${id}-error`, help && `${id}-help`].filter(Boolean).join(' ') || undefined
  return (
    <label>
      <span>
        {label}
        {props.required ? (
          <small className="required-mark"> 필수</small>
        ) : (
          <small className="optional-mark"> 선택</small>
        )}
      </span>
      <span className={suffix ? 'input-with-suffix' : undefined}>
        <input
          ref={inputRef}
          min={props.type === 'number' ? 0 : undefined}
          step={props.type === 'number' ? 1 : undefined}
          {...props}
          id={id}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          onInvalid={(event) => {
            const input = event.currentTarget
            input.setCustomValidity(
              input.validity.valueMissing
                ? `${label}을(를) 입력해 주세요.`
                : input.validity.typeMismatch
                  ? props.type === 'url'
                    ? 'https://로 시작하는 올바른 주소를 입력해 주세요.'
                    : '올바른 이메일 주소를 입력해 주세요.'
                  : input.validity.rangeUnderflow || input.validity.rangeOverflow
                    ? `${label}의 허용 범위를 확인해 주세요.`
                    : input.validity.tooShort
                      ? '비밀번호는 8자 이상 입력해 주세요.'
                      : `${label}을(를) 확인해 주세요.`,
            )
          }}
          onInput={(event) => event.currentTarget.setCustomValidity('')}
          onChange={onChange ? (event) => onChange(event.target.value) : undefined}
        />
        {suffix && <small aria-hidden="true">{suffix}</small>}
      </span>
      {help && <small className="input-help" id={`${id}-help`}>{help}</small>}
      {error && <small className="field-error" id={`${id}-error`}>{error}</small>}
    </label>
  )
}

export function Feedback({ message, kind = 'error', children }) {
  if (!message) return null
  return (
    <div className={`feedback feedback-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      <Icon name="info" />
      <div>
        <span>{message}</span>
        {children}
      </div>
    </div>
  )
}

export function SelectField({ label, children, onChange, error, ...props }) {
  const generatedId = useId()
  const id = props.id || generatedId
  return (
    <label>
      <span>
        {label}
        <small className={props.required ? 'required-mark' : 'optional-mark'}>
          {props.required ? ' 필수' : ' 선택'}
        </small>
      </span>
      <select {...props} id={id} aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${id}-error` : props['aria-describedby']}
        onChange={(event) => onChange(event.target.value)}>
        {children}
      </select>
      {error && <small className="field-error" id={`${id}-error`}>{error}</small>}
    </label>
  )
}

export function SectionTitle({ id, title, count }) {
  return (
    <div className="section-title" id={id}>
      <h2>{title}</h2>
      <span>{count}</span>
    </div>
  )
}

export function Empty({ text }) {
  return (
    <div className="empty">
      <Icon name="info" />
      <p>{text}</p>
    </div>
  )
}

export function EmptyState({ icon = 'info', title, description, actionLabel, onAction }) {
  return (
    <div className="empty-state">
      <span className="empty-state-icon"><Icon name={icon} /></span>
      <h3>{title}</h3>
      <p>{description}</p>
      {actionLabel && <button type="button" onClick={onAction}>{actionLabel}</button>}
    </div>
  )
}
