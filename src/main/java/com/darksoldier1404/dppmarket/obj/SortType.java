package com.darksoldier1404.dppmarket.obj;

public enum SortType {
    RECENT,
    PRICE,
    SALES;

    public SortType next() {
        SortType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }

    public String langKey() {
        switch (this) {
            case PRICE:
                return "sort_price";
            case SALES:
                return "sort_sales";
            case RECENT:
            default:
                return "sort_recent";
        }
    }
}
