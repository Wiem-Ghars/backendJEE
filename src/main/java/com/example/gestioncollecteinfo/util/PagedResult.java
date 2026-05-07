package com.example.gestioncollecteinfo.util;

import java.util.List;

/*
 * Generic wrapper for all paginated API responses.
 * Every paginated endpoint returns this shape:
 *
 * {
 *   "content":       [...],   // the actual data for this page
 *   "currentPage":   0,       // zero-based page index
 *   "pageSize":      10,      // items per page requested
 *   "totalElements": 42,      // total records in the DB (for this query)
 *   "totalPages":    5        // Math.ceil(totalElements / pageSize)
 * }
 *
 * Angular's MatPaginator uses totalElements to render the page controls.
 * currentPage and pageSize are echoed back so the client doesn't have to
 * track them separately.
 */
public class PagedResult<T> {

    private List<T> content;
    private int     currentPage;
    private int     pageSize;
    private long    totalElements;
    private int     totalPages;

    public PagedResult(List<T> content, int currentPage, int pageSize, long totalElements) {
        this.content       = content;
        this.currentPage   = currentPage;
        this.pageSize      = pageSize;
        this.totalElements = totalElements;
        this.totalPages    = (int) Math.ceil((double) totalElements / pageSize);
    }

    public List<T> getContent()         { return content; }
    public int     getCurrentPage()     { return currentPage; }
    public int     getPageSize()        { return pageSize; }
    public long    getTotalElements()   { return totalElements; }
    public int     getTotalPages()      { return totalPages; }
}
