export default function LoadingScreen({ message = 'Loading…' }: { message?: string }) {
  return (
    <main className="screen">
      <p role="status">{message}</p>
    </main>
  )
}
