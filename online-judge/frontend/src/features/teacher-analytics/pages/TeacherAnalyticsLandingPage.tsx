import { useEffect, useState } from "react";
import { ArrowRight, Plus } from "lucide-react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../../../shared/api/client";
import type { ClassGroup } from "../../../shared/api/types";
import { useTranslation } from "../../../shared/i18n";
import { ButtonLink } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";
import { AnalyticsPageBar } from "../components/AnalyticsPageBar";

export default function TeacherAnalyticsLandingPage() {
  const { t } = useTranslation();
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void loadClasses();
  }, []);

  async function loadClasses() {
    setLoading(true);
    setError(null);
    try {
      setClasses(await api.classes());
    } catch (currentError) {
      setError(currentError instanceof ApiError || currentError instanceof Error ? currentError.message : t("teacherAnalytics.errors.load"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="teacher-analytics-page">
      <AnalyticsPageBar
        title={t("teacherAnalytics.landing.title")}
        t={t}
        action={<ButtonLink to="/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
          {t("teacherAnalytics.actions.newAssignment")}
        </ButtonLink>}
      />

      {error ? <div className="alert alert--error">{error}</div> : null}

      <section className="teacher-analytics-list-panel" aria-label={t("teacherAnalytics.landing.classList")}>
        {loading ? (
          <EmptyState title={t("teacherAnalytics.loading.classes")} live />
        ) : classes.length ? (
          <div className="teacher-analytics-class-grid">
            {classes.map(classGroup => (
              <Link className="teacher-analytics-class-card" to={`/teacher/classes/${classGroup.id}`} key={classGroup.id}>
                <div>
                  <strong>{classGroup.name}</strong>
                  <small>{classGroup.grade || t("teacherAnalytics.landing.classFallback")}</small>
                </div>
                <span>{classGroup.teacherName || t("teacherAnalytics.landing.noTeacherName")}</span>
                <ArrowRight size={18} />
              </Link>
            ))}
          </div>
        ) : (
          <EmptyState title={t("teacherAnalytics.empty.noClasses")} description={t("teacherAnalytics.empty.noClassesDescription")} />
        )}
      </section>
    </div>
  );
}
