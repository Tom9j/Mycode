package com.example.trivia.models;

public class User {
    private String email;
    private String password;
    // אולי נתונים נוספים

    public User() {
        // עבור Firebase
    }

    public User(String email, String password) {
        this.email = email;
        this.password = password;
    }

    // getters ו-setters
}