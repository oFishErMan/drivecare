import { useEffect, useRef, useState } from 'react'
import { Icon } from '../../components/Icon'
import { VehicleForm } from './VehicleForm'
import './VehicleRegistrationDrawer.css'

const CLOSE_DURATION_MS = 220

export function VehicleRegistrationDrawer({ open, onClose, onCreated }) {
  const dialogRef = useRef(null)
  const restoreRef = useRef(null)
  const [busy, setBusy] = useState(false)

  useEffect(() => {
    const dialog = dialogRef.current
    let closeTimer
    let focusFrame
    if (open) {
      dialog.dataset.state = 'open'
      if (!dialog.open) {
        const previousFocus = document.activeElement
        const previousOverflow = document.body.style.overflow
        restoreRef.current = () => {
          document.body.style.overflow = previousOverflow
          if (previousFocus?.isConnected) previousFocus.focus({ preventScroll: true })
        }
        document.body.style.overflow = 'hidden'
        dialog.showModal()
      }
      // Fragment navigation can move focus after showModal; focus on the next frame.
      focusFrame = window.requestAnimationFrame(() => {
        dialog.querySelector('[name="manufacturer"]')?.focus({ preventScroll: true })
      })
    } else if (dialog.open) {
      dialog.dataset.state = 'closing'
      const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      closeTimer = window.setTimeout(() => {
        dialog.close()
        restoreRef.current?.()
        restoreRef.current = null
      }, reducedMotion ? 0 : CLOSE_DURATION_MS)
    }
    return () => {
      window.clearTimeout(closeTimer)
      window.cancelAnimationFrame(focusFrame)
    }
  }, [open])

  useEffect(() => {
    const dialog = dialogRef.current
    return () => {
      if (dialog.open) dialog.close()
      restoreRef.current?.()
      restoreRef.current = null
    }
  }, [])

  function keepFocusInside(event) {
    if (event.key !== 'Tab') return
    const controls = [...dialogRef.current.querySelectorAll('button:not(:disabled), input:not(:disabled)')]
    const target = event.shiftKey && document.activeElement === controls[0]
      ? controls.at(-1)
      : !event.shiftKey && document.activeElement === controls.at(-1) ? controls[0] : null
    if (target) {
      event.preventDefault()
      target.focus({ preventScroll: true })
    }
  }

  return (
    <dialog ref={dialogRef} id="vehicle-registration" className="vehicle-registration-drawer"
      aria-labelledby="vehicle-registration-title" aria-describedby="vehicle-draft-hint"
      onKeyDown={keepFocusInside}
      onCancel={(event) => { event.preventDefault(); if (!busy) onClose() }}>
      <div className="vehicle-drawer-toolbar">
        <span><Icon name="car" /> 내 차량 추가</span>
        <button type="button" className="vehicle-drawer-close" aria-label="차량 등록 닫기"
          disabled={busy} onClick={onClose}><Icon name="close" /></button>
      </div>
      <div className="vehicle-drawer-content">
        <VehicleForm onCreated={onCreated} headingId="vehicle-registration-title" onBusyChange={setBusy} />
        <p id="vehicle-draft-hint" className="vehicle-draft-hint"><Icon name="info" />
          닫아도 입력 내용은 유지돼요. 새로고침하거나 로그아웃하면 초기화됩니다.
        </p>
      </div>
    </dialog>
  )
}
