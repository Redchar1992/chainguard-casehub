import { createContext, useCallback, useContext, useRef, useState, type ReactNode } from 'react';

type ToastType = 'success' | 'warning' | 'error' | 'info';
type ToastItem = { id: number; type: ToastType; text: string };

interface ToastApi {
  success: (text: string) => void;
  warning: (text: string) => void;
  error: (text: string) => void;
  info: (text: string) => void;
}

const Ctx = createContext<ToastApi | null>(null);

/** Lightweight dark toast stack — replaces AntD `message` so notifications match the theme. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [items, setItems] = useState<ToastItem[]>([]);
  const seq = useRef(0);

  const push = useCallback((type: ToastType, text: string) => {
    const id = ++seq.current;
    setItems((prev) => [...prev, { id, type, text }]);
    window.setTimeout(() => {
      setItems((prev) => prev.filter((t) => t.id !== id));
    }, 3600);
  }, []);

  const api: ToastApi = {
    success: (t) => push('success', t),
    warning: (t) => push('warning', t),
    error: (t) => push('error', t),
    info: (t) => push('info', t),
  };

  return (
    <Ctx.Provider value={api}>
      {children}
      <div className="toast-stack" aria-live="polite">
        {items.map((t) => (
          <div className={`toast ${t.type}`} key={t.id}>
            <span className="toast-dot" />
            {t.text}
          </div>
        ))}
      </div>
    </Ctx.Provider>
  );
}

export function useToast(): ToastApi {
  const c = useContext(Ctx);
  if (!c) throw new Error('useToast must be used within ToastProvider');
  return c;
}
