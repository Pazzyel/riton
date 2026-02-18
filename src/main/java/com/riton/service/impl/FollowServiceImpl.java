package com.riton.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.riton.dto.Result;
import com.riton.dto.UserDTO;
import com.riton.entity.Follow;
import com.riton.mapper.FollowMapper;
import com.riton.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.riton.service.IUserService;
import com.riton.constants.RedisConstants;
import com.riton.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

    private final StringRedisTemplate stringRedisTemplate;

    private final IUserService userService;

    @Autowired
    public FollowServiceImpl(StringRedisTemplate stringRedisTemplate, IUserService userService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userService = userService;
    }

    /**
     * 关注未关注，取关已关注
     * @param followUserId 被关注的用户id
     * @param followFlag 是否关注
     * @return 无
     */
    @Override
    public Result follow(Long followUserId, Boolean followFlag) {
        //获取当前登录用户id
        Long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_KEY + userId;
        if(followFlag){
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            boolean successFlag = save(follow);
            //用于查询共同关注，放入Redis Set
            if(successFlag){
                //一个用户关注的所有用户是一个集合
                stringRedisTemplate.opsForSet().add(key,followUserId.toString());
            }
        } else {
            //取关
            boolean successFlag = remove(new QueryWrapper<Follow>().eq("follow_user_id",followUserId).eq("user_id",userId));
            if(successFlag){
                //从关注集合移除
                stringRedisTemplate.opsForSet().remove(key,followUserId.toString());
            }
        }
        return Result.ok();
    }
    /**
     * 查询是否关注
     * @param followUserId 被关注的用户id
     * @return 是否关注
     */
    @Override
    public Result isFollow(Long followUserId) {
        //获取当前登录用户id
        Long userId = UserHolder.getUser().getId();
        //查询是否关注
        Integer count = query().eq("follow_user_id",followUserId).eq("user_id",userId).count();
        return Result.ok(count > 0);
    }

    @Override
    public Result followCommons(Long followUserId) {
        Long userId = UserHolder.getUser().getId();
        String key1 = RedisConstants.FOLLOW_KEY + userId;
        String key2 = RedisConstants.FOLLOW_KEY + followUserId;
        Set<String> intersection = stringRedisTemplate.opsForSet().intersect(key1,key2);
        if(intersection == null || intersection.isEmpty()){
            //没有共同关注
            return Result.ok(Collections.emptyList());
        }

        //把id集合转换为用户列表
        //如果要从数据库查，也可以，写法是 SELECT f1.follow_user_id FROM tb_follow f1 JOIN tb_follow f2
        //                           ON f1.follow_user_id = f2.follow_user_id
        //                           WHERE f1.user_id = #{userId} AND f2.user_id = #{followUserId}
        List<Long> ids = intersection.stream().map(Long::parseLong).collect(Collectors.toList());
        List<UserDTO> users = userService.listByIds(ids).stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(users);
    }
}
