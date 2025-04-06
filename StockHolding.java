package com.example.trivia.models;


import java.util.ArrayList;
import java.util.List;

public class StockHolding {
    private String symbol;
    private String companyName;
    private int shares;
    private double currentPrice;
    private double averageCost;
    private List<Transaction> transactions;

    public StockHolding() {
        // עבור Firebase
        transactions = new ArrayList<>();
    }

    public StockHolding(String symbol, String companyName) {
        this();
        this.symbol = symbol;
        this.companyName = companyName;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getShares() {
        return shares;
    }

    public void setShares(int shares) {
        this.shares = shares;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getAverageCost() {
        return averageCost;
    }

    public void setAverageCost(double averageCost) {
        this.averageCost = averageCost;
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    public void addTransaction(Transaction transaction) {
        if (this.transactions == null) {
            this.transactions = new ArrayList<>();
        }
        this.transactions.add(transaction);

        // עדכון ממוצע העלות
        updateAverageCostAndShares();
    }

    private void updateAverageCostAndShares() {
        int totalShares = 0;
        double totalCost = 0;

        for (Transaction t : transactions) {
            if (t.getType().equals("buy")) {
                totalShares += t.getShares();
                totalCost += (t.getPrice() * t.getShares());
            } else if (t.getType().equals("sell")) {
                totalShares -= t.getShares();
                // לא משנים את העלות הכוללת בעת מכירה
            }
        }

        this.shares = totalShares;
        if (totalShares > 0) {
            this.averageCost = totalCost / totalShares;
        } else {
            this.averageCost = 0;
        }
    }

    public double getTotalValue() {
        return shares * currentPrice;
    }

    public double getProfitLoss() {
        return shares * (currentPrice - averageCost);
    }

    public double getProfitLossPercentage() {
        if (averageCost == 0) return 0;
        return ((currentPrice - averageCost) / averageCost) * 100;
    }
}