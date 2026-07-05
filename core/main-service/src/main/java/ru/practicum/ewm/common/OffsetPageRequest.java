package ru.practicum.ewm.common;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class OffsetPageRequest implements Pageable {

    private final int from;
    private final int size;
    private final Sort sort;

    public OffsetPageRequest(int from, int size, Sort sort) {
        this.from = from;
        this.size = size;
        this.sort = sort;
    }

    public OffsetPageRequest(int from, int size) {
        this(from, size, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    public int getPageNumber() {
        return from / size;
    }

    @Override
    public int getPageSize() {
        return size;
    }

    @Override
    public long getOffset() {
        return from;
    }

    @Override
    public Sort getSort() {
        return sort;
    }

    @Override
    public Pageable next() {
        return new OffsetPageRequest(from + size, size, sort);
    }

    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? new OffsetPageRequest(from - size, size, sort) : first();
    }

    @Override
    public Pageable first() {
        return new OffsetPageRequest(0, size, sort);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetPageRequest(pageNumber * size, size, sort);
    }

    @Override
    public boolean hasPrevious() {
        return from > 0;
    }
}
