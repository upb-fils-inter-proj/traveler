import type { ConfigProblem } from '../config.ts'

export default function ConfigErrorPage({ problems }: { problems: ConfigProblem[] }) {
  return (
    <main className="screen">
      <h1>Configuration incomplete</h1>
      <p role="alert" className="error">
        The app is not configured correctly. These environment variables are
        missing or invalid:
      </p>
      <ul>
        {problems.map((problem) => (
          <li key={problem.variable}>
            <code>{problem.variable}</code> ({problem.reason})
          </li>
        ))}
      </ul>
      <p>
        Copy <code>.env.example</code> to <code>.env</code>, fill in the values
        and restart the app.
      </p>
    </main>
  )
}
