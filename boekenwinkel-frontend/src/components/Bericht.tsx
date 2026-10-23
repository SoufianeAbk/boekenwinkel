import type { ReactNode } from 'react'

interface Props {
  soort?: 'fout' | 'succes' | 'info'
  children: ReactNode
}

export function Bericht({ soort = 'info', children }: Props) {
  return (
    <div className={`bericht bericht--${soort}`} role={soort === 'fout' ? 'alert' : 'status'}>
      <div>{children}</div>
    </div>
  )
}
