package com.example.trivia.models;

public class Transaction {
    private String type; // "buy" או "sell"
    private String symbol;
    private int shares;
    private double price;
    private long timestamp;

    public Transaction() {
        // עבור Firebase
    }

    public Transaction(String type, String symbol, int shares, double price) {
        this.type = type;
        this.symbol = symbol;
        this.shares = shares;
        this.price = price;
        this.timestamp = System.currentTimeMillis();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getShares() {
        return shares;
    }

    public void setShares(int shares) {
        this.shares = shares;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getTotalValue() {
        return shares * price;
    }
}