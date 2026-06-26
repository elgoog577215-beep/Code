export function EmptyState({ title, description }: { title: string; description?: string }) {
  return (
    <div className="empty-state" role="status" aria-live="polite">
      <strong>{title}</strong>
      {description && <p>{description}</p>}
    </div>
  );
}
