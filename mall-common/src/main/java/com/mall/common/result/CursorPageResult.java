package com.mall.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
public class CursorPageResult<T> implements Serializable {
    private List<T> list;
    private String nextCursor;
    private boolean hasNext;
}
