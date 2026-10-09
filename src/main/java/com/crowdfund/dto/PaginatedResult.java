package com.crowdfund.dto;

import java.util.Collections;
import java.util.List;

/**
 * Generic container demonstrating Generics and Java Collections (List).
 *
 * @param <T> Element type contained in the page
 */
public class PaginatedResult<T> {

    private final List<T> items;
    private final int pageNumber;
    private final int pageSize;
    private final long totalElements;

    public PaginatedResult(List<T> items, int pageNumber, int pageSize, long totalElements) {
        this.items = items != null ? items : Collections.emptyList();
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
    }

    public boolean hasNext() {
        return (pageNumber + 1) * pageSize < totalElements;
    }
}
