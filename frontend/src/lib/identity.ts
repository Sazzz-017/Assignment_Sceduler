const KEY = 'slot-ledger.booker-name'

export function getRememberedName(): string {
  try {
    return localStorage.getItem(KEY) ?? ''
  } catch {
    return ''
  }
}

export function rememberName(name: string): void {
  try {
    localStorage.setItem(KEY, name)
  } catch {
    /* ignore storage errors */
  }
}
