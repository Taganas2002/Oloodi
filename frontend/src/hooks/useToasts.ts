import { useCallback, useState } from 'react'
import type { ToastMessage, ToastTone } from '../components/Toast'

let nextId = 1

export function useToasts() {
  const [toasts, setToasts] = useState<ToastMessage[]>([])

  const pushToast = useCallback((text: string, tone: ToastTone = 'success') => {
    const id = nextId++
    setToasts((prev) => [...prev.slice(-3), { id, text, tone }])
  }, [])

  const dismissToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id))
  }, [])

  return { toasts, pushToast, dismissToast }
}
