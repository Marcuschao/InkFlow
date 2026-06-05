package com.blog.personalblogbackend.search;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchReindexResult {
    private int indexed;
    private String rebuildIndex;
    private boolean swapped;
}
