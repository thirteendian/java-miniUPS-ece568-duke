package edu.duke.ece568.server;

public class ProductInfo {
    private String description;
    private Long count;

    public ProductInfo(String description, Long count) {
        this.description = description;
        this.count = count;
    }

    public String getDescription() {
        return description;
    }

    public Long getCount() {
        return count;
    }
}
