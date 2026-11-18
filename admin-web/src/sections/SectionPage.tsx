import { useEffect } from 'react'

/** A section of the app that has no content yet. */
export default function SectionPage({ title }: { title: string }) {
  useEffect(() => {
    document.title = `${title} - Traveler Admin`
  }, [title])

  return (
    <>
      <h1>{title}</h1>
      <p className="placeholder">This section is empty for now.</p>
    </>
  )
}
