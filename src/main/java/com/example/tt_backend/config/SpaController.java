package com.example.tt_backend.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class SpaController {

    @RequestMapping(value = {
            "/{path:^(?!api|auth|files|assets|index\\.html|.*\\.\\w+$).*$}",
            "/**/{path:^(?!api|auth|files|assets|index\\.html|.*\\.\\w+$).*$}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
