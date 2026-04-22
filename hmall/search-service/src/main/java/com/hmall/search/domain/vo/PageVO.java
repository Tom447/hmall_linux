package com.hmall.search.domain.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {
    protected Long total;
    protected Long pages;
    protected List<T> list;

    public static <T> PageVO<T> empty(Long total, Long pages) {
        return new PageVO<>(total, pages, Collections.emptyList());
    }

    public static <T> PageVO<T> of(Long total, Long pages, List<T> list) {
        return new PageVO<>(total, pages,list);
    }
}
