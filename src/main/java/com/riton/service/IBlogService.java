package com.riton.service;

import com.riton.dto.Result;
import com.riton.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {

    /**
     * 根据id查询博客
     * @param id 博客id
     * @return 包含用户信息的博客信息
     */
    Result queryBlogById(Long id);

    /**
     * 点赞功能，对未点赞的点赞，已点赞的取消赞
     * @param id 博客id
     * @return 无
     */
    Result likeBlog(Long id);

    /**
     * 查询改博客id下最早点赞的top5
     * @param id 博客id
     * @return 最早点赞的top5用户列表
     */
    Result queryBlogLikes(Long id);

    /**
     * 保存当前用户的Blog
     * @param blog 要保存的blog
     * @return blogId
     */
    Result saveBlog(Blog blog);

    /**
     * 分页查询当前登录用户blog
     * @param current 当前页数
     * @return 查询结果
     */
    Result queryMyBlog(Integer current);

    /**
     * 分页查询当前用户blog，按点赞数倒序查询
     * @param current 当前页数
     * @return 查询结果
     */
    Result queryHotBlog(Integer current);

    /**
     * 根据用户id分页查询用户blog
     * @param current 当前页
     * @param userId 用户id
     * @return 查询结果
     */
    Result queryBlogByUserId(Integer current, Long userId);

    /**
     * 分页查询用户关注的博主的博客内容（用户收件箱）
     * @param max 上一次查询的最小时间戳（最旧博客的时间戳）
     * @param offset 上一次查询等于最小时间戳的记录的数目，本次查询要跳过这些记录
     * @return 分页查询结果
     */
    Result queryBlogOfFollow(Long max, Integer offset);
}
