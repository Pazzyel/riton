package com.riton.domain.query;

import lombok.Data;
import lombok.EqualsAndHashCode;

import jakarta.validation.constraints.Min;

@EqualsAndHashCode
@Data
public class ShopPageSearch {

    public static final Integer DEFAULT_PAGE_SIZE = 20;
    public static final Integer DEFAULT_PAGE_NUM = 1;
    /**
     * 距离限制，单位 m
     */
    public static final Double DEFAULT_DISTANCE_LIMIT = 10000.0;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNo = DEFAULT_PAGE_NUM;

    @Min(value = 1, message = "每页查询数量不能小于1")
    private Integer pageSize = DEFAULT_PAGE_SIZE;

    /**
     * 查询名称，可能是shop的name里的某个分词字段
     */
    private String shopName;

    /**
     * 商铺类型
     */
    private Integer typeId;

    /**
     * 用户经度
     */
    private Double x;


    /**
     * 用户纬度
     */
    private Double y;

    /**
     * 排序方式
     */
    private Integer sortBy = SORT_BY_SCORE;
    public static final Integer SORT_BY_SCORE = 0;
    public static final Integer SORT_BY_DISTANCE = 1;
}
