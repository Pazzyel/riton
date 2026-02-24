package com.riton.service;

import com.riton.domain.dto.Result;
import com.riton.domain.entity.Shop;
import com.riton.domain.query.ShopPageSearch;

public interface IShopSearchService {

    Result searchShop(ShopPageSearch query);

    Result saveShopDoc(Shop shop);

    Result updateShopDoc(Shop shop);

    Result deleteShopDoc(Long shopId);

    Result syncAllShopToEs();
}
