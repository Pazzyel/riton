package com.riton.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.riton.domain.doc.BlogDoc;
import com.riton.domain.dto.Result;
import com.riton.domain.entity.Blog;
import com.riton.domain.query.BlogPageQuery;
import com.riton.mapper.BlogMapper;
import com.riton.service.IBlogSearchService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class BlogSearchServiceImpl implements IBlogSearchService {

    private static final String BLOG_INDEX = "blogs";

    @Resource
    private ElasticsearchClient elasticsearchClient;

    @Resource
    private BlogMapper blogMapper;

    /**
     * 贴文ES查询
     * @param query 查询条件
     * @return 查询List
     */
    @Override
    public Result searchBlog(BlogPageQuery query) {
        if (query == null) {
            return Result.fail("query cannot be null");
        }
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? BlogPageQuery.DEFAULT_PAGE_NUM : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? BlogPageQuery.DEFAULT_PAGE_SIZE : query.getPageSize();
        int from = (pageNo - 1) * pageSize;
        Query finalQuery = buildBlogQuery(query);
        try {
            SearchResponse<BlogDoc> response = elasticsearchClient.search(s -> s
                    .index(BLOG_INDEX)
                    .from(from)
                    .size(pageSize)
                    .query(finalQuery)
                    .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))), BlogDoc.class);
            List<BlogDoc> docs = new ArrayList<>();
            response.hits().hits().stream()
                    .map(hit -> hit.source())
                    .filter(Objects::nonNull)
                    .forEach(docs::add);
            long total = response.hits().total() == null ? 0L : response.hits().total().value();
            return Result.ok(docs, total);
        } catch (Exception e) {
            log.error("search blog from es failed", e);
            return Result.fail("search blog failed");
        }
    }

    @Override
    public Result saveBlogDoc(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return Result.fail("blog and blogId cannot be null");
        }
        return indexBlogDoc(blog);
    }

    @Override
    public Result updateBlogDoc(Blog blog) {
        if (blog == null || blog.getId() == null) {
            return Result.fail("blog and blogId cannot be null");
        }
        return indexBlogDoc(blog);
    }

    @Override
    public Result deleteBlogDoc(Long blogId) {
        if (blogId == null) {
            return Result.fail("blogId cannot be null");
        }
        try {
            DeleteResponse response = elasticsearchClient.delete(d -> d.index(BLOG_INDEX).id(String.valueOf(blogId)));
            if (!"deleted".equals(response.result().jsonValue())) {
                log.warn("delete blog doc result is {}, blogId={}", response.result().jsonValue(), blogId);
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("delete blog doc failed, blogId={}", blogId, e);
            return Result.fail("delete blog doc failed");
        }
    }

    @Override
    public Result syncAllBlogToEs() {
        List<Blog> blogs = blogMapper.selectList(null);
        if (blogs == null || blogs.isEmpty()) {
            return Result.ok(0);
        }
        List<BulkOperation> operations = new ArrayList<>(blogs.size());
        for (Blog blog : blogs) {
            BlogDoc doc = BeanUtil.copyProperties(blog, BlogDoc.class);
            operations.add(BulkOperation.of(op -> op.index(idx -> idx
                    .index(BLOG_INDEX)
                    .id(String.valueOf(blog.getId()))
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
                log.error("bulk sync blog docs failed, msg={}", failMsg.toString());
                return Result.fail("sync all blog to es failed");
            }
            return Result.ok(blogs.size());
        } catch (Exception e) {
            log.error("bulk sync blog docs failed", e);
            return Result.fail("sync all blog to es failed");
        }
    }

    private Result indexBlogDoc(Blog blog) {
        BlogDoc doc = BeanUtil.copyProperties(blog, BlogDoc.class);
        try {
            IndexResponse response = elasticsearchClient.index(i -> i
                    .index(BLOG_INDEX)
                    .id(String.valueOf(blog.getId()))
                    .document(doc)
            );
            if (response.result() == null) {
                return Result.fail("index blog doc failed");
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("index blog doc failed, blogId={}", blog.getId(), e);
            return Result.fail("index blog doc failed");
        }
    }

    /**
     * 构建Blog的ES查询条件
     */
    private Query buildBlogQuery(BlogPageQuery query) {
        return Query.of(q -> q.bool(b -> {
            if (StrUtil.isNotBlank(query.getTitle())) {
                b.must(m -> m.match(mm -> mm.field("title").query(query.getTitle())));
            }
            if (StrUtil.isNotBlank(query.getContent())) {
                b.must(m -> m.match(mm -> mm.field("content").query(query.getContent())));
            }
            if (query.getShopId() != null) {
                b.filter(f -> f.term(t -> t.field("shopId").value(query.getShopId())));
            }
            if (query.getUserId() != null) {
                b.filter(f -> f.term(t -> t.field("userId").value(query.getUserId())));
            }
            if (StrUtil.isBlank(query.getTitle()) && StrUtil.isBlank(query.getContent())) {
                b.must(m -> m.matchAll(ma -> ma));
            }
            return b;
        }));
    }
}
