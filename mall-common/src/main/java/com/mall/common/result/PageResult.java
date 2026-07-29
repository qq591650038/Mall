package com.mall.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private Long total;
    private List<T> list;
    private Integer page;
    private Integer size;
    private Integer totalPages;

    public PageResult() {
    }

    public PageResult(Long total, List<T> list, Integer page, Integer size) {
        this.total = total;
        this.list = list;
        this.page = page;
        this.size = size;
        this.totalPages = size != null && size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
