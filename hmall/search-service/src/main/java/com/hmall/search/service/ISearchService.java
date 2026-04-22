package com.hmall.search.service;

import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import com.hmall.search.domain.vo.PageVO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ISearchService {
    void saveItemById(Long itemId);

    void deleteItemById(Long itemId);


    PageVO<ItemDoc> search(ItemPageQuery query) throws IOException;

    Map<String, List<String>> getFilters(ItemPageQuery query);
}

