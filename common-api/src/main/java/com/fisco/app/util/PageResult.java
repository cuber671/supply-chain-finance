package com.fisco.app.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页响应包装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private List<T> list;
    private Pagination pagination;

    public PageResult(List<T> list, Long total, Integer page, Integer pageSize) {
        this.list = list;
        this.pagination = new Pagination(total, page, pageSize);
    }

    public static <T> PageResult<T> of(List<T> list, Long total, Integer page, Integer pageSize) {
        return new PageResult<>(list, total, page, pageSize);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Pagination implements Serializable {
        private Long total;
        private Integer page;
        private Integer pageSize;
        private Long pages;

        public Pagination(Long total, Integer page, Integer pageSize) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.pages = pageSize > 0 ? (total + pageSize - 1) / pageSize : 0;
        }
    }
}
