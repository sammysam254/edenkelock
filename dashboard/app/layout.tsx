import './globals.css'
import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Eden M-Kopa Dashboard',
  description: 'Device financing management system',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  )
}
