package com.example.trivia.models;

import java.util.HashMap;
import java.util.Map;

public class Portfolio {
    private String name;
    private String id;
    private Map<String, StockHolding> holdings;
    private long createdAt;
    private double currentValue;
    private double initialValue;

    public Portfolio() {
        // עבור Firebase
        this.holdings = new HashMap<>();
        this.createdAt = System.currentTimeMillis();
    }

    public Portfolio(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, StockHolding> getHoldings() {
        return holdings;
    }

    public void setHoldings(Map<String, StockHolding> holdings) {
        this.holdings = holdings;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }

    public double getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(double initialValue) {
        this.initialValue = initialValue;
    }

    public void addHolding(StockHolding holding) {
        holdings.put(holding.getSymbol(), holding);
    }

    public StockHolding getHolding(String symbol) {
        return holdings.get(symbol);
    }

    public double getChangePercentage() {
        if (initialValue == 0) return 0;
        return ((currentValue - initialValue) / initialValue) * 100;
    }
}