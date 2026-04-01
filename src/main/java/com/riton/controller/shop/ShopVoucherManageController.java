package com.riton.controller.shop;

import com.riton.domain.dto.Result;
import com.riton.domain.dto.ShopAccountDTO;
import com.riton.domain.entity.Voucher;
import com.riton.service.IVoucherService;
import com.riton.utils.ShopHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店铺优惠券管理控制器。
 */
@RestController
@RequestMapping("/shop/manage/vouchers")
public class ShopVoucherManageController {

    private final IVoucherService voucherService;

    /**
     * 构造店铺优惠券管理控制器。
     *
     * @param voucherService 优惠券服务
     */
    @Autowired
    public ShopVoucherManageController(IVoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /**
     * 查询当前店铺优惠券列表。
     *
     * @return 优惠券列表
     */
    @GetMapping
    public Result queryVouchers() {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：查询当前店铺优惠券。
        return voucherService.queryManageVouchers(shopAccountDTO.getShopId());
    }

    /**
     * 新增普通券。
     *
     * @param voucher 优惠券参数
     * @return 新增结果
     */
    @PostMapping
    public Result addVoucher(@RequestBody Voucher voucher) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：新增当前店铺普通券。
        return voucherService.addVoucherByShop(shopAccountDTO.getShopId(), voucher);
    }

    /**
     * 新增秒杀券。
     *
     * @param voucher 优惠券参数
     * @return 新增结果
     */
    @PostMapping("/seckill")
    public Result addSeckillVoucher(@RequestBody Voucher voucher) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：新增当前店铺秒杀券。
        return voucherService.addSeckillVoucherByShop(shopAccountDTO.getShopId(), voucher);
    }

    /**
     * 更新普通券。
     *
     * @param voucher 优惠券参数
     * @return 更新结果
     */
    @PutMapping
    public Result updateVoucher(@RequestBody Voucher voucher) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：更新当前店铺普通券。
        return voucherService.updateVoucherByShop(shopAccountDTO.getShopId(), voucher);
    }

    /**
     * 更新秒杀券。
     *
     * @param voucher 优惠券参数
     * @return 更新结果
     */
    @PutMapping("/seckill")
    public Result updateSeckillVoucher(@RequestBody Voucher voucher) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：更新当前店铺秒杀券。
        return voucherService.updateSeckillVoucherByShop(shopAccountDTO.getShopId(), voucher);
    }

    /**
     * 删除优惠券。
     *
     * @param voucherId 优惠券ID
     * @return 删除结果
     */
    @DeleteMapping("/{voucherId}")
    public Result deleteVoucher(@PathVariable("voucherId") Long voucherId) {
        // 第一步：读取登录店铺ID。
        ShopAccountDTO shopAccountDTO = ShopHolder.getShop();
        if (shopAccountDTO == null || shopAccountDTO.getShopId() == null) {
            return Result.fail("请先登录店铺账号");
        }

        // 第二步：删除当前店铺券。
        return voucherService.deleteVoucherByShop(shopAccountDTO.getShopId(), voucherId);
    }
}
