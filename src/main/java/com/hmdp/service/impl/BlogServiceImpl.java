package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.ScrollResult;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Follow;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IFollowService;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private IFollowService followService;

    @Autowired
    public BlogServiceImpl(IUserService userService, StringRedisTemplate redisTemplate, IFollowService followService) {
        this.userService = userService;
        this.stringRedisTemplate = redisTemplate;
        this.followService = followService;
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
     * 根据blog的用户id查询作者用户并注入相应字段
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
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if (score == null) {
            //用户不是已经点赞集合的成员，本次操作为点赞
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if (success) {
                //点赞后，把用户加入已点赞集合
                stringRedisTemplate.opsForZSet().add(key, userId.toString(), System.currentTimeMillis());//第三个参数是score，我们按照时间排序
            }
        } else {
            //本次操作取消赞
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success) {
                //移除点赞集合
                stringRedisTemplate.opsForZSet().remove(key, userId.toString());
            }
        }
        return Result.ok();
    }

    /**
     * 查询改博客id下最早点赞的top5
     * @param id 博客id
     * @return 最早点赞的top5用户列表
     */
    @Override
    public Result queryBlogLikes(Long id) {
        String key = RedisConstants.BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key, 0, 4);//点赞最早5个人的id
        if(top5 == null || top5.isEmpty()){
            //没有人点赞
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = top5.stream().map(Long::parseLong).collect(Collectors.toList());//把集合按原来的顺序解析成id，
        String idStr = StrUtil.join(",",ids);//在原来的每个id之间插入逗号
        //根据用户id查询用户 WHERE id IN ( 5 , 1 ) ORDER BY FIELD(id, 5, 1)
        //in方法，第一个是字段，第二个是集合
        //last方法，追加到最后的sql语句，ORDER BY FIELD (field, ...)
        //把列表的结果参照field字段的 ... 顺序排序， 如果 ... 不包含这个数据的内容，这条数据会在最前面
        List<UserDTO> userDTOs = userService.query().in("id",ids).last("ORDER BY FIELD (id," + idStr + ")").list()
                .stream().map(o -> BeanUtil.copyProperties(o, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(userDTOs);
    }

    /**
     * 保存当前用户的Blog
     * @param blog 要保存的blog
     * @return blogId
     */
    @Override
    public Result saveBlog(Blog blog) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        // 保存探店博文
        boolean success = save(blog);
        if (!success) {
            return Result.fail("新增笔记失败");
        }
        // 推送到所有粉丝的收件箱
        //SELECT * FROM tb_follow WHERE follow_user_id = #{id}
        List<Follow> follows = followService.query().eq("follow_user_id", user.getId()).list();
        for (Follow follow : follows) {
            Long fansId = follow.getUserId();
            String key = RedisConstants.FEED_KEY + fansId;
            //每个粉丝都有自己的收件箱SortedSet
            stringRedisTemplate.opsForZSet().add(key,blog.getId().toString(),System.currentTimeMillis());//根据时间戳排序
        }

        // 返回id
        return Result.ok(blog.getId());
    }

    /**
     * 分页查询当前登录用户blog
     * @param current 当前页数
     * @return 查询结果
     */
    @Override
    public Result queryMyBlog(Integer current) {
        // 获取登录用户
        UserDTO user = UserHolder.getUser();
        // 根据用户查询
        Page<Blog> page = query()
                .eq("user_id", user.getId()).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        return Result.ok(records);
    }

    /**
     * 分页查询当前用户blog，按点赞数倒序查询
     * @param current 当前页数
     * @return 查询结果
     */
    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
        });
        return Result.ok(records);
    }

    /**
     * 根据用户id分页查询用户blog
     * @param current 当前页
     * @param userId 用户id
     * @return 查询结果
     */
    @Override
    public Result queryBlogByUserId(Integer current, Long userId) {
        // 根据用户查询
        Page<Blog> page = query().eq("user_id", userId).page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        //我的Blog用户就是我自己
        return Result.ok(records);
    }

    /**
     * 分页查询用户关注的博主的博客内容（用户收件箱）
     * @param max 上一次查询的最小时间戳（最旧博客的时间戳）
     * @param offset 上一次查询等于最小时间戳的记录的数目，本次查询要跳过这些记录
     * @return 分页查询结果
     */
    @Override
    public Result queryBlogOfFollow(Long max, Integer offset){
        //max是上一次查询的最小时间戳，offset是上一次查询时间戳等于最小时间戳记录的个数
        //offset的意义是因为ZSet分数范围查询的[min, max]是闭区间，但我们不需要等于上次查询最小时间戳的数据
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FEED_KEY + userId;
        //查询的参数分别是key：key的名称，min:score的最小值，max:score的最大值，count:查询的记录条数
        //reverse是反向查询，WithScores是携带score值
        Set<ZSetOperations.TypedTuple<String>> typedTuples = stringRedisTemplate.opsForZSet()
                .reverseRangeByScoreWithScores(key, 0, max, offset, 2);//每次查询2条数据，注意reverse，降序查询

        //处理空集合
        if(typedTuples == null || typedTuples.isEmpty()){
            return Result.ok(Collections.emptyList());
        }

        List<Long> ids = new ArrayList<>(typedTuples.size());//Blog的id集合
        long minTime = 0;//查询结果集合的最小时间戳
        int newOffset = 1;
        //返回的Set实现类是有序的
        for(ZSetOperations.TypedTuple<String> tuple : typedTuples){
            ids.add(Long.parseLong(tuple.getValue()));
            long time = tuple.getScore().longValue();
            //正向循环，查询的最后一个数据一定是时间戳最小的
            if(time == minTime){
                //有超过一条数据等于最小时间戳，增加newOffset计数
                ++newOffset;
            } else {
                //有更小的时间戳，重置
                minTime = time;
                newOffset = 1;
            }
        }

        //根据Blog的id列表查询Blog
        //List<Blog> blogs = this.listByIds(ids);//需要按时间顺序，应该手动写SQL
        String idStr = StrUtil.join(",",ids);
        //按照我们查询BlogId的顺序查询数据库
        List<Blog> blogs = this.query().in("id", ids).last("ORDER BY FIELD (id," + idStr + ")").list();

        for(Blog blog : blogs){
            //注入用户字段
            queryBlogUser(blog);
            //注入已点赞字段
            isBlogLiked(blog);
        }

        //封装并返回
        ScrollResult scrollResult = new ScrollResult();
        scrollResult.setList(blogs);
        scrollResult.setMinTime(minTime);
        scrollResult.setOffset(newOffset);
        return Result.ok(scrollResult);
    }

    /**
     * 向blog注入当前用户的点赞情况
     * @param blog 注入对象
     */
    private void isBlogLiked(Blog blog) {
        String key = RedisConstants.BLOG_LIKED_KEY + blog.getId();
        Long userId = UserHolder.getUser().getId();
        //查询该博客的点赞集合有没有当前用户
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        blog.setIsLike(score != null);
    }
}
