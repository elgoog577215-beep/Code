import { useEffect, useState } from "react";
import { ArrowRight, Plus } from "lucide-react";
import { Link } from "react-router-dom";
import { ApiError, api } from "../../../shared/api/client";
import type { ClassGroup } from "../../../shared/api/types";
import { useTranslation } from "../../../shared/i18n";
import { ButtonLink } from "../../../shared/ui/Button";
import { EmptyState } from "../../../shared/ui/EmptyState";

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
      <section className="teacher-analytics-hero">
        <div>
          <span>{t("teacherAnalytics.kicker")}</span>
          <h1>{t("teacherAnalytics.landing.title")}</h1>
          <p>{t("teacherAnalytics.landing.description")}</p>
        </div>
        <ButtonLink to="/app/teacher/assignment/new" variant="primary" icon={<Plus size={17} />}>
          {t("teacherAnalytics.actions.newAssignment")}
        </ButtonLink>
      </section>

      {error ? <div className="alert alert--error">{error}</div> : null}

      <section className="teacher-analytics-list-panel" aria-label={t("teacherAnalytics.landing.classList")}>
        {loading ? (
          <EmptyState title={t("teacherAnalytics.loading.classes")} live />
        ) : classes.length ? (
          <div className="teacher-analytics-class-grid">
            {classes.map(classGroup => (
              <Link className="teacher-analytics-class-card" to={`/app/teacher/classes/${classGroup.id}`} key={classGroup.id}>
                <span>{classGroup.grade || t("teacherAnalytics.landing.classFallback")}</span>
                <strong>{classGroup.name}</strong>
                <small>{classGroup.teacherName || t("teacherAnalytics.landing.noTeacherName")}</small>
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
