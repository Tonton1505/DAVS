package vn.edu.davs.backend.Wrapper;

import java.util.List;

/**
 * Bọc kết quả phân trang chuẩn 25 dòng/trang theo NFR-PERF-04.
 */
public class PageWrapper<T> {
    private List<T> items;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;

    public PageWrapper(List<T> items, int pageNumber, int pageSize, long totalElements, int totalPages) {
        this.items = items;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    public List<T> getItems() { return items; }
    public int getPageNumber() { return pageNumber; }
    public int getPageSize() { return pageSize; }
    public long getTotalElements() { return totalElements; }
    public int getTotalPages() { return totalPages; }
}
