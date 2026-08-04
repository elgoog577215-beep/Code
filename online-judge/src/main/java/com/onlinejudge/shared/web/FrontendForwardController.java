package com.onlinejudge.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendForwardController {

    @GetMapping({
            "/code",
            "/code/",
            "/code/student",
            "/code/student/login",
            "/code/student/assignments/{assignmentId:[0-9]+}",
            "/code/student/assignments/{assignmentId:[0-9]+}/ranking",
            "/code/student/assignments/{assignmentId:[0-9]+}/submissions",
            "/code/student/assignments/{assignmentId:[0-9]+}/problems/{problemId:[0-9]+}",
            "/code/student/assignments/public",
            "/code/student/assignments/public/problems/{problemId:[0-9]+}",
            "/code/teacher",
            "/code/teacher/classes",
            "/code/teacher/classes/{classId:[0-9]+}",
            "/code/teacher/classes/{classId:[0-9]+}/assignments/{assignmentId:[0-9]+}",
            "/code/teacher/classes/{classId:[0-9]+}/assignments/{assignmentId:[0-9]+}/problems/{problemId:[0-9]+}",
            "/code/teacher/manage",
            "/code/teacher/manage/classes",
            "/code/teacher/manage/problems",
            "/code/teacher/manage/ai-library",
            "/code/teacher/manage/system",
            "/code/teacher/assignment/new",
            "/code/teacher/assignment/{assignmentId:[0-9]+}",
            "/code/teacher/assignment/{assignmentId:[0-9]+}/problems/{problemId:[0-9]+}",
            "/code/teacher/assignment/{assignmentId:[0-9]+}/problems/{problemId:[0-9]+}/students/{studentProfileId:[0-9]+}",
            "/code/teacher-management",
            "/code/task-editor",
            "/code/class-overview"
    })
    public String forwardToApp() {
        return "forward:/code/index.html";
    }
}
