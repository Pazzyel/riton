package com.riton.controller;

import com.riton.domain.dto.AgentSearchDTO;
import com.riton.domain.dto.Result;
import com.riton.domain.query.BlogPageQuery;
import com.riton.domain.query.ShopPageSearch;
import com.riton.service.IBlogSearchService;
import com.riton.service.IShopSearchService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.client.RestTemplate;

@Validated
@RestController
@RequestMapping("/search")
public class SearchController {

    @Resource
    private IShopSearchService shopSearchService;

    @Resource
    private IBlogSearchService blogSearchService;

    @GetMapping("/shop")
    public Result searchShop(@Valid ShopPageSearch query) {
        return shopSearchService.searchShop(query);
    }

    @GetMapping("/blog")
    public Result searchBlog(@Valid BlogPageQuery query) {
        return blogSearchService.searchBlog(query);
    }

    @PostMapping("/sync/shop")
    public Result syncShopToEs() {
        return shopSearchService.syncAllShopToEs();
    }

    @PostMapping("/sync/blog")
    public Result syncBlogToEs() {
        return blogSearchService.syncAllBlogToEs();
    }
}
