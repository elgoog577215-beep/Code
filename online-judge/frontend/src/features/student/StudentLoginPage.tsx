import { useEffect, useState } from "react";
import { ArrowLeft, LogIn } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { api } from "../../shared/api/client";
import type { ClassGroup } from "../../shared/api/types";
import { saveActiveStudent } from "../../shared/storage";
import { Button, ButtonLink } from "../../shared/ui/Button";
import { EmptyState } from "../../shared/ui/EmptyState";
import { Field, Select, TextInput } from "../../shared/ui/Field";
import { Panel } from "../../shared/ui/Panel";
import { StatusPill } from "../../shared/ui/StatusPill";

export default function StudentLoginPage() {
  const navigate = useNavigate();
  const [classes, setClasses] = useState<ClassGroup[]>([]);
  const [classGroupId, setClassGroupId] = useState("");
  const [displayName, setDisplayName] = useState("");
  const [studentNo, setStudentNo] = useState("");
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [alert, setAlert] = useState<{ type: "error" | "success"; message: string } | null>(null);

  useEffect(() => {
    let ignore = false;
    setLoading(true);
    api.studentClasses()
      .then(result => {
        if (!ignore) {
          setClasses(result);
          if (result.length === 1) {
            setClassGroupId(String(result[0].id));
          }
        }
      })
      .catch(() => {
        if (!ignore) {
          setAlert({ type: "error", message: "班级列表加载失败。" });
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoading(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, []);

  async function submit() {
    if (!classGroupId) {
      setAlert({ type: "error", message: "请选择班级。" });
      return;
    }
    if (!displayName.trim()) {
      setAlert({ type: "error", message: "请输入姓名。" });
      return;
    }
    setBusy(true);
    try {
      const student = await api.loginStudent({
        classGroupId: Number(classGroupId),
        displayName: displayName.trim(),
        studentNo: studentNo.trim()
      });
      saveActiveStudent(student);
      navigate("/app/student", { replace: true });
    } catch (error) {
      setAlert({ type: "error", message: error instanceof Error ? error.message : "登录失败。" });
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="stack student-page student-login-page">
      <section className="student-home-command">
        <div>
          <p className="eyebrow">学生登录</p>
          <h1>确认班级和姓名</h1>
        </div>
        <ButtonLink to="/app/student" variant="ghost" icon={<ArrowLeft size={17} />}>
          返回
        </ButtonLink>
      </section>

      {alert && <div className={`alert alert--${alert.type === "success" ? "success" : "error"}`}>{alert.message}</div>}

      <Panel
        className="student-login-panel"
        title="登录班级"
        eyebrow="我的身份"
        action={<StatusPill tone={classes.length ? "info" : "neutral"}>{loading ? "加载中" : `${classes.length} 个班级`}</StatusPill>}
      >
        {loading ? (
          <EmptyState title="正在加载班级" />
        ) : classes.length === 0 ? (
          <EmptyState title="暂无班级" />
        ) : (
          <form
            className="student-login-form"
            onSubmit={event => {
              event.preventDefault();
              void submit();
            }}
          >
            <div className="form-grid">
              <Field label="班级">
                <Select value={classGroupId} onChange={event => setClassGroupId(event.target.value)}>
                  <option value="">选择班级</option>
                  {classes.map(item => (
                    <option value={item.id} key={item.id}>
                      {item.name}
                    </option>
                  ))}
                </Select>
              </Field>
              <Field label="姓名">
                <TextInput value={displayName} onChange={event => setDisplayName(event.target.value)} placeholder="请输入姓名" autoComplete="name" />
              </Field>
              <Field label="学号/座号">
                <TextInput value={studentNo} onChange={event => setStudentNo(event.target.value)} placeholder="可选" autoComplete="off" />
              </Field>
            </div>
            <Button type="submit" variant="primary" icon={<LogIn size={17} />} disabled={busy}>
              进入学生端
            </Button>
          </form>
        )}
      </Panel>
    </div>
  );
}
