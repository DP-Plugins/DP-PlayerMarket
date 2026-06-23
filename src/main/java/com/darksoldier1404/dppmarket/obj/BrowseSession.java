package com.darksoldier1404.dppmarket.obj;

public class BrowseSession {
    private String category;
    private SortType sort = SortType.RECENT;
    private String keyword;

    public BrowseSession() {
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public SortType getSort() {
        return sort;
    }

    public void setSort(SortType sort) {
        this.sort = sort;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
