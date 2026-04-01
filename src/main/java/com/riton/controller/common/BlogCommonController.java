package com.riton.controller.common;

import com.riton.domain.dto.Result;
import com.riton.service.IBlogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/blog")
/**
 * 无需鉴权的blog接口
 */
public class BlogCommonController {

    private final IBlogService blogService;

    @Autowired
    public BlogCommonController(IBlogService blogService) {
        this.blogService = blogService;
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }
}
