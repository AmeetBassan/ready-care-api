package com.readycare.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginationRequest {
    private int page = 0;
    private int size = 20;
    private String sortBy = "createdAt";
    private String sortOrder = "DESC";

    public int getOffset() {
        return page * size;
    }
}

