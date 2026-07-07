import { Link } from "react-router-dom";

export type BreadcrumbItem = {
  label: string;
  to?: string;
};

export function AnalyticsBreadcrumbs({ items }: { items: BreadcrumbItem[] }) {
  return (
    <nav className="teacher-analytics-breadcrumbs" aria-label="Breadcrumb">
      {items.map((item, index) => (
        <span key={`${item.label}-${index}`}>
          {item.to ? <Link to={item.to}>{item.label}</Link> : <strong>{item.label}</strong>}
        </span>
      ))}
    </nav>
  );
}
