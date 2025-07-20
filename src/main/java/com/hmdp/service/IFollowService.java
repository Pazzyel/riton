package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {

    /**
     * 关注未关注，取关已关注
     * @param followUserId 被关注的用户id
     * @param followFlag 是否关注
     * @return 无
     */
    Result follow(Long followUserId, Boolean followFlag);

    /**
     * 查询是否关注
     * @param followUserId 被关注的用户id
     * @return boolean是否关注
     */
    Result isFollow(Long followUserId);
}
