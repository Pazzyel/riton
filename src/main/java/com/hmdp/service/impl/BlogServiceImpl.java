package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    private IUserService userService;

    @Autowired
    public BlogServiceImpl(IUserService userService) {
        this.userService = userService;
    }

    /**
     * 根据id查询博客
     * @param id 博客id
     * @return 包含用户信息的博客信息
     */
    @Override
    public Result queryBlogById(Long id) {
        //1.查询博客信息
        Blog blog = getById(id);
        if (blog == null) {
            return Result.fail("笔记不存在！");
        }
        //2.填充用户信息
        queryBlogUser(blog);
        return Result.ok(blog);
    }

    /**
     * 根据blog的用户id查询对应用户并注入相应字段
     * @param blog 博客
     */
    private void queryBlogUser(Blog blog) {
        User user = userService.getById(blog.getUserId());
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
    }
}
