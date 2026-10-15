import type { Content } from './question-api';
export default function AttemptText({ content }: { content: Content }) {
  return <><p className="question-text">{content.text}</p>{content.resources.map((r, i) => <p key={`${r.sha256}-${i}`}>Descrição da figura: {r.description}</p>)}</>;
}
