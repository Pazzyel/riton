package com.hmdp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.UserHolder;
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
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {

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
        if(followFlag){
            //关注
            Follow follow = new Follow();
            follow.setUserId(userId);
            follow.setFollowUserId(followUserId);
            save(follow);
        } else {
            //取关
            remove(new QueryWrapper<Follow>().eq("follow_user_id",followUserId).eq("user_id",userId));
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
}
