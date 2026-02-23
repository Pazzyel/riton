package com.riton.controller;


import com.riton.domain.dto.Result;
import com.riton.domain.entity.Blog;
import com.riton.service.IBlogService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/blog")
public class BlogController {

    @Resource
    private IBlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        return blogService.saveBlog(blog);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        // 修改点赞数量
        return blogService.likeBlog(id);
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryMyBlog(current);
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return blogService.queryHotBlog(current);
    }

    @GetMapping("/{id}")
    public Result queryBlogById(@PathVariable("id") Long id) {
        return blogService.queryBlogById(id);
    }

    @GetMapping("/likes/{id}")
    public Result queryBlogLikes(@PathVariable("id") Long id) {
        return blogService.queryBlogLikes(id);
    }

    /**
     * 根据用户id分页查询用户blog
     * @param current 当前页
     * @param userId 用户id
     * @return 查询结果
     */
    @GetMapping("/of/user")
    public Result queryBlogByUserId(@RequestParam(value = "current", defaultValue = "1") Integer current,
                                    @RequestParam("id") Long userId) {
        return blogService.queryBlogByUserId(current, userId);
    }

    /**
     * 分页查询用户关注的博主的博客内容（用户收件箱）
     * @param max 上一次查询的最小时间戳（最旧博客的时间戳）
     * @param offset 上一次查询等于最小时间戳的记录的数目，本次查询要跳过这些记录
     * @return 分页查询结果
     */
    @GetMapping("/of/follow")
    public Result queryBlogOfFollow(@RequestParam("lastId") Long max,
                                    @RequestParam(value = "offset", defaultValue = "0") Integer offset) {
        return blogService.queryBlogOfFollow(max, offset);
    }
}
