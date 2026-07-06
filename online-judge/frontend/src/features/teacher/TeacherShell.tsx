import { ReactNode } from "react";
import "./TeacherHomeRefresh.css";

export function TeacherShell({ children }: { children: ReactNode }) {
  return <div className="teacher-shell">{children}</div>;
}
