package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
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

    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    public BlogServiceImpl(IUserService userService, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.stringRedisTemplate = redisTemplate;
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

    /**
     * 点赞功能，对未点赞的点赞，已点赞的取消赞
     * @param id 博客id
     * @return 无
     */
    @Override
    public Result likeBlog(Long id) {
        //对未点赞的点赞，已点赞的取消赞
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.BLOG_LIKED_KEY + id;//id是blog的id，每个不同的blog有自己的key
        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key, userId.toString());
        if (BooleanUtil.isFalse(isMember)) {
            //用户不是已经点赞集合的成员，本次操作为点赞
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if (success) {
                //点赞后，把用户加入已点赞集合
                stringRedisTemplate.opsForSet().add(key, userId.toString());//第三个参数是score，我们按照时间排序
            }
        } else {
            //本次操作取消赞
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                //移除点赞集合
                stringRedisTemplate.opsForSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }
}
