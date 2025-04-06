package com.example.trivia.services;

import com.example.trivia.models.StockPrice;
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class StockApiService {
    private static StockApiService instance;
    private final OkHttpClient client;
    private static final String BASE_URL = "http://10.0.0.41:8080"; // החלף ל-URL של שרת הפייתון שלך
    private final Gson gson;

    private StockApiService() {
        client = new OkHttpClient();
        gson = new Gson();
    }

    public static synchronized StockApiService getInstance() {
        if (instance == null) {
            instance = new StockApiService();
        }
        return instance;
    }

    public void getStockHistory(String symbol, String range, String interval, final StockApiCallback<List<StockPrice>> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("symbol", symbol);
            jsonBody.put("range", range);
            jsonBody.put("interval", interval);
        } catch (Exception e) {
            callback.onError(e.getMessage());
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JSONObject jsonObject = new JSONObject(responseData);
                    JSONArray pricesArray = jsonObject.getJSONArray("prices");

                    List<StockPrice> prices = new ArrayList<>();
                    for (int i = 0; i < pricesArray.length(); i++) {
                        JSONObject priceObj = pricesArray.getJSONObject(i);
                        String date = priceObj.getString("date");
                        double price = priceObj.getDouble("price");
                        prices.add(new StockPrice(date, price));
                    }

                    callback.onSuccess(prices);
                } catch (Exception e) {
                    callback.onError("JSON parsing error: " + e.getMessage());
                }
            }
        });
    }

    public void tradeStock(String userId, String symbol, String action, int amount, final StockApiCallback<JsonObject> callback) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("userId", userId);
            jsonBody.put("symbol", symbol);
            jsonBody.put("action", action); // "buy" או "sell"
            jsonBody.put("amount", amount);
        } catch (Exception e) {
            callback.onError(e.getMessage());
            return;
        }

        MediaType JSON = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);

        Request request = new Request.Builder()
                .url(BASE_URL + "/handle_stock_request")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Server error: " + response.code());
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JsonObject jsonObject = gson.fromJson(responseData, JsonObject.class);
                    callback.onSuccess(jsonObject);
                } catch (Exception e) {
                    callback.onError("JSON parsing error: " + e.getMessage());
                }
            }
        });
    }
}

interface StockApiCallback<T> {
    void onSuccess(T result);
    void onError(String errorMessage);
}