package com.riton.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.ScriptSortType;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.riton.domain.doc.ShopDoc;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.Shop;
import com.riton.domain.query.ShopPageSearch;
import com.riton.mapper.ShopMapper;
import com.riton.service.IShopSearchService;
import com.riton.utils.FenceUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class ShopSearchServiceImpl implements IShopSearchService {

    private static final String SHOP_INDEX = "shops";

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private ShopMapper shopMapper;

    @Override
    public Result searchShop(ShopPageSearch query) {
        if (query == null) {
            return Result.fail("query cannot be null");
        }
        // 分页计算
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? ShopPageSearch.DEFAULT_PAGE_NUM : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? ShopPageSearch.DEFAULT_PAGE_SIZE : query.getPageSize();
        int from = (pageNo - 1) * pageSize;
        // 坐标判断
        if ((query.getX() == null) ^ (query.getY() == null)) {
            return Result.fail("x and y must be provided together");
        }

        boolean hasLocation = query.getX() != null;
        // 构建查询条件
        Query finalQuery = buildShopQuery(query, hasLocation);

        try {
            SearchResponse<ShopDoc> response = elasticsearchClient.search(s -> {
                s.index(SHOP_INDEX).from(from).size(pageSize).query(finalQuery);
                if (hasLocation && ShopPageSearch.SORT_BY_DISTANCE.equals(query.getSortBy())) {
                    s.sort(so -> so.script(sc -> sc
                            .type(ScriptSortType.Number)
                            .order(SortOrder.Asc)
                            .script(ss -> ss.inline(inline -> inline
                                    .lang("painless")
                                    .source("(doc['x'].value - params.x) * (doc['x'].value - params.x) + (doc['y'].value - params.y) * (doc['y'].value - params.y)")
                                    .params("x", JsonData.of(query.getX()))
                                    .params("y", JsonData.of(query.getY()))
                            ))
                    ));
                } else {
                    s.sort(so -> so.score(sc -> sc.order(SortOrder.Desc)));
                }
                return s;
            }, ShopDoc.class);
            List<ShopDoc> docs = new ArrayList<>();
            response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .forEach(docs::add);
            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            return Result.ok(docs, total);
        } catch (Exception e) {
            log.error("search shop from es failed", e);
            return Result.fail("search shop failed");
        }
    }

    @Override
    public Result saveShopDoc(Shop shop) {
        if (shop == null || shop.getId() == null) {
            return Result.fail("shop and shopId cannot be null");
        }
        return indexShopDoc(shop);
    }

    @Override
    public Result updateShopDoc(Shop shop) {
        if (shop == null || shop.getId() == null) {
            return Result.fail("shop and shopId cannot be null");
        }
        return indexShopDoc(shop);
    }

    @Override
    public Result deleteShopDoc(Long shopId) {
        if (shopId == null) {
            return Result.fail("shopId cannot be null");
        }
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d.index(SHOP_INDEX).id(String.valueOf(shopId)));
            if (!"deleted".equals(response.result().jsonValue())) {
                log.warn("delete shop doc result is {}, shopId={}", response.result().jsonValue(), shopId);
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("delete shop doc failed, shopId={}", shopId, e);
            return Result.fail("delete shop doc failed");
        }
    }

    @Override
    public Result syncAllShopToEs() {
        List<Shop> shops = shopMapper.selectList(null);
        if (shops == null || shops.isEmpty()) {
            return Result.ok(0);
        }
        List<BulkOperation> operations = new ArrayList<>(shops.size());
        for (Shop shop : shops) {
            ShopDoc doc = BeanUtil.copyProperties(shop, ShopDoc.class);
            operations.add(BulkOperation.of(op -> op.index(idx -> idx
                    .index(SHOP_INDEX)
                    .id(String.valueOf(shop.getId()))
                    .document(doc)
            )));
        }
        try {
            BulkRequest request = BulkRequest.of(b -> b.operations(operations));
            BulkResponse response = elasticsearchClient.bulk(request);
            if (response.errors()) {
                StringBuilder failMsg = new StringBuilder();
                response.items().stream()
                        .filter(item -> item.error() != null)
                        .forEach(item -> failMsg.append("id=").append(item.id()).append(", reason=")
                                .append(item.error().reason()).append("; "));
                log.error("bulk sync shop docs failed, msg={}", failMsg.toString());
                return Result.fail("sync all shop to es failed");
            }
            return Result.ok(shops.size());
        } catch (Exception e) {
            log.error("bulk sync shop docs failed", e);
            return Result.fail("sync all shop to es failed");
        }
    }

    private Result indexShopDoc(Shop shop) {
        ShopDoc doc = BeanUtil.copyProperties(shop, ShopDoc.class);
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(SHOP_INDEX)
                    .id(String.valueOf(shop.getId()))
                    .document(doc)
            );
            if (response.result() == null) {
                return Result.fail("index shop doc failed");
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("index shop doc failed, shopId={}", shop.getId(), e);
            return Result.fail("index shop doc failed");
        }
    }

    /**
     * 构建ElasticSearch的查询条件
     * @param query 查询条件
     * @param hasLocation 请求是否携带地址信息
     * @return
     */
    private Query buildShopQuery(ShopPageSearch query, boolean hasLocation) {
        // bool条件
        return Query.of(q -> q.bool(b -> {
            if (StrUtil.isNotBlank(query.getShopName())) {
                // 有名字就分词匹配
                b.must(m -> m.match(mm -> mm.field("name").query(query.getShopName())));
            } else {
                // 没有就全部查询
                b.must(m -> m.matchAll(ma -> ma));
            }
            // 有类型就限定类型，term是严格匹配
            if (query.getTypeId() != null) {
                b.filter(f -> f.term(t -> t.field("typeId").value(query.getTypeId())));
            }
            // 加载地址信息
            if (hasLocation) {
                FenceUtils.Fence fence = FenceUtils.buildSquareFence(
                        query.getX(),
                        query.getY(),
                        ShopPageSearch.DEFAULT_DISTANCE_LIMIT
                );
                b.filter(f -> f.range(r -> r
                        .field("x")
                        .gte(JsonData.of(fence.getMinLng()))
                        .lte(JsonData.of(fence.getMaxLng()))
                ));
                b.filter(f -> f.range(r -> r
                        .field("y")
                        .gte(JsonData.of(fence.getMinLat()))
                        .lte(JsonData.of(fence.getMaxLat()))
                ));
            }
            return b;
        }));
    }
}
