package com.onlinejudge.shared.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class FrontendForwardController {

    @GetMapping({
            "/student",
            "/teacher",
            "/task-editor",
            "/class-overview",
            "/problem/{id:[0-9]+}",
            "/app/student",
            "/app/teacher",
            "/app/task-editor",
            "/app/class-overview",
            "/app/problem/{id:[0-9]+}"
    })
    public String forwardToApp() {
        return "forward:/app/index.html";
    }
}
