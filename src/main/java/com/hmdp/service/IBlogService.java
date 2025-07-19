package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
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
}
