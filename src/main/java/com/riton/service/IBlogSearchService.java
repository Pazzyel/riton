package com.riton.service;

import com.riton.domain.dto.Result;
import com.riton.domain.entity.Blog;
import com.riton.domain.query.BlogPageQuery;

public interface IBlogSearchService {

    Result searchBlog(BlogPageQuery query);

    Result saveBlogDoc(Blog blog);

    Result updateBlogDoc(Blog blog);

    Result deleteBlogDoc(Long blogId);

    Result syncAllBlogToEs();
}
