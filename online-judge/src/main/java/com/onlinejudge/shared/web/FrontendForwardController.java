package com.onlinejudge.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendForwardController {

    @GetMapping({
            "/student",
            "/teacher",
            "/teacher-management",
            "/task-editor",
            "/class-overview",
            "/problem/{id:[0-9]+}",
            "/teacher/assignment/new",
            "/teacher/assignment/{assignmentId:[0-9]+}",
            "/app",
            "/app/",
            "/app/student",
            "/app/student/login",
            "/app/student/assignments/{assignmentId:[0-9]+}",
            "/app/student/assignments/{assignmentId:[0-9]+}/problems/{problemId:[0-9]+}",
            "/app/student/assignments/public",
            "/app/student/assignments/public/problems/{problemId:[0-9]+}",
            "/app/teacher",
            "/app/teacher/assignment/new",
            "/app/teacher/assignment/{assignmentId:[0-9]+}",
            "/app/teacher-management",
            "/app/task-editor",
            "/app/class-overview",
            "/app/problem/{id:[0-9]+}"
    })
    public String forwardToApp() {
        return "forward:/app/index.html";
    }
}
