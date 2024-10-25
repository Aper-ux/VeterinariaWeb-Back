package com.example.demo.dto;

import lombok.Data;
import java.util.List;

@Data
public class PaginatedResponse<T> {
    private List<T> content;
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    public static <T> PaginatedResponse<T> of(List<T> content, PaginationRequest request, long totalElements) {
        PaginatedResponse<T> response = new PaginatedResponse<>();
        response.setContent(content);
        response.setPageNumber(request.getPage());
        response.setPageSize(request.getSize());
        response.setTotalElements(totalElements);
        response.setTotalPages((int) Math.ceil((double) totalElements / request.getSize()));
        response.setLast(request.getPage() >= response.getTotalPages() - 1);
        return response;
    }
}