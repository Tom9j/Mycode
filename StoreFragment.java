package com.example.trivia;

import android.app.Dialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class StoreFragment extends Fragment {

    private EditText symbolInput;
    private Button fetchButton;
    private Spinner rangeSpinner;
    private SimpleLineChartView stockChart;
    private SimpleLineChartView portfolioChart;
    private TextView stockTitle;
    private TextView stockPrice;
    private TextView stockChange;
    private TextView openPrice;
    private TextView lowPrice;
    private TextView highPrice;
    private TextView volumeText;
    private TextView[] stockRangeViews;
    private TextView[] portfolioRangeViews;

    private TabLayout tabLayout;
    private View portfolioContent;
    private View researchContent;
    private View leaderboardContent;

    private final String[] ranges = {"1d", "5d", "1mo", "3mo", "6mo", "1y"};
    private String selectedRange = "1mo";
    private String currentSymbol = "";

    // עבור פרטי הפורטפוליו
    private TextView portfolioValueText;
    private TextView gainPercentageText;
    private Button createPortfolioButton;
    private Spinner portfolioSpinner;
    private FloatingActionButton fabTrade;
    private LinearLayout holdingsContainer; // במקום RecyclerView
    private TextView coinsTextView;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference userRef;

    private double userCoins = 0;
    private Map<String, Object> userPortfolio = new HashMap<>();
    private List<String> portfolioNames = new ArrayList<>();
    private String currentPortfolioName = "תיק ראשי";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_store, container, false);

        // אתחול Firebase
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            userRef = database.getReference("users/" + user.getUid());
        }

        // אתחול הטאבים - רק אם קיימים
        tabLayout = view.findViewById(R.id.tabs);

        portfolioContent = view.findViewById(R.id.portfolio_content);
        researchContent = view.findViewById(R.id.research_content);

        // אתחול רכיבי חקר מניות - רק אם קיימים
        symbolInput = view.findViewById(R.id.symbol_input);
        fetchButton = view.findViewById(R.id.fetch_button);
        rangeSpinner = view.findViewById(R.id.range_spinner);
        stockChart = view.findViewById(R.id.stock_chart);
        stockTitle = view.findViewById(R.id.stock_title);
        stockPrice = view.findViewById(R.id.stock_price);
        stockChange = view.findViewById(R.id.stock_change);
        openPrice = view.findViewById(R.id.open_price);
        lowPrice = view.findViewById(R.id.low_price);
        highPrice = view.findViewById(R.id.high_price);
        volumeText = view.findViewById(R.id.volume);

        // אתחול רכיבי פורטפוליו - רק אם קיימים
        portfolioValueText = view.findViewById(R.id.portfolio_value);
        gainPercentageText = view.findViewById(R.id.gain_percentage);
        portfolioChart = view.findViewById(R.id.portfolio_chart);
        createPortfolioButton = view.findViewById(R.id.create_portfolio_button);
        portfolioSpinner = view.findViewById(R.id.portfolio_spinner);

        // ייתכן שלא קיים בלייאוט - רק למצוא אותו אם הוא קיים

        coinsTextView = view.findViewById(R.id.coins_counter);

        try {
            stockRangeViews = new TextView[] {
                    view.findViewById(R.id.range_1d),
                    view.findViewById(R.id.range_5d),
                    view.findViewById(R.id.range_1mo),
                    view.findViewById(R.id.range_3mo),
                    view.findViewById(R.id.range_1y)
            };

            portfolioRangeViews = new TextView[] {
                    view.findViewById(R.id.period_day),
                    view.findViewById(R.id.period_week),
                    view.findViewById(R.id.period_month),
                    view.findViewById(R.id.period_year),
                    view.findViewById(R.id.period_all)
            };
        } catch (Exception e) {
            Log.w("StoreFragment", "Error initializing range views", e);
            stockRangeViews = new TextView[0];
            portfolioRangeViews = new TextView[0];
        }

        // אתחול Spinner
        if (rangeSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                    android.R.layout.simple_spinner_item, ranges);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            rangeSpinner.setAdapter(adapter);
        }

        setupEventListeners(view);

        // טען נתוני משתמש
        loadPortfolioData();

        return view;
    }

    private void updateCoinsDisplay() {
        coinsTextView.setText(String.format(Locale.getDefault(), "%.2f", userCoins));
    }

    private void loadPortfolioData() {
        userRef.child("portfolio").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userPortfolio.clear();
                portfolioNames.clear();
                portfolioNames.add("תיק ראשי"); // תמיד יהיה תיק ראשי

                if (snapshot.exists()) {
                    // עובר על כל ההחזקות בתיק
                    for (DataSnapshot stockSnapshot : snapshot.getChildren()) {
                        String symbol = stockSnapshot.getKey();
                        Long shares = stockSnapshot.getValue(Long.class);
                        if (symbol != null && shares != null) {
                            userPortfolio.put(symbol, shares);
                        }
                    }
                }

                // מעדכן את ספינר בחירת התיק
                setupPortfolioSpinner();

                // מעדכן את תצוגת התיק
                updatePortfolioDisplay();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("StoreFragment", "Error fetching portfolio", error.toException());
                Toast.makeText(requireContext(), "שגיאה בטעינת תיק השקעות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupEventListeners(View view) {
        // מאזינים לטווחי זמן של הגרף
        setupRangeViews(stockRangeViews, true);
        setupRangeViews(portfolioRangeViews, false);

        // לחצן לחיפוש מניה
        fetchButton.setOnClickListener(v -> {
            String symbol = symbolInput.getText().toString().trim().toUpperCase();
            if (!symbol.isEmpty()) {
                currentSymbol = symbol;
                fetchStockData(symbol, selectedRange);

                // עדכון כותרות
                stockTitle.setText(symbol);
                stockChart.setChartTitle(symbol);

                // הצג אנימציית טעינה
                stockChart.showLoading();
            } else {
                Toast.makeText(getContext(), "הכנס סימול מניה", Toast.LENGTH_SHORT).show();
            }
        });

        // ספינר הטווחים
        rangeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View itemView, int pos, long id) {
                selectedRange = ranges[pos];
                updateSelectedRange(stockRangeViews, selectedRange, true);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                selectedRange = "1mo";
            }
        });

        // לחצן יצירת תיק
        createPortfolioButton.setOnClickListener(v -> {
            Toast.makeText(requireContext(), "פיצ'ר זה יהיה זמין בקרוב", Toast.LENGTH_SHORT).show();
        });

        // מאזין ל-FAB
        fabTrade.setOnClickListener(v -> {
            showTradeDialog();
        });

        // כפתורי קנייה ומכירה במסך חקר המניות
        Button buyButton = view.findViewById(R.id.buy_button);
        if (buyButton != null) {
            buyButton.setOnClickListener(v -> {
                if (!currentSymbol.isEmpty()) {
                    showBuyDialog(currentSymbol);
                } else {
                    Toast.makeText(requireContext(), "אנא חפש מניה קודם", Toast.LENGTH_SHORT).show();
                }
            });
        }

        Button sellButton = view.findViewById(R.id.sell_button);
        if (sellButton != null) {
            sellButton.setOnClickListener(v -> {
                if (!currentSymbol.isEmpty()) {
                    showSellDialog(currentSymbol);
                } else {
                    Toast.makeText(requireContext(), "אנא חפש מניה קודם", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void showTradeDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_quick_trade);

        EditText symbolInput = dialog.findViewById(R.id.quick_symbol_input);
        EditText sharesInput = dialog.findViewById(R.id.quick_shares_input);
        Button buyButton = dialog.findViewById(R.id.quick_buy_button);
        Button sellButton = dialog.findViewById(R.id.quick_sell_button);

        buyButton.setOnClickListener(v -> {
            String symbol = symbolInput.getText().toString().trim().toUpperCase();
            String sharesStr = sharesInput.getText().toString().trim();

            if (symbol.isEmpty() || sharesStr.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הזן סמל מניה וכמות", Toast.LENGTH_SHORT).show();
                return;
            }

            int shares = Integer.parseInt(sharesStr);
            dialog.dismiss();
            fetchAndBuyStock(symbol, shares);
        });

        sellButton.setOnClickListener(v -> {
            String symbol = symbolInput.getText().toString().trim().toUpperCase();
            String sharesStr = sharesInput.getText().toString().trim();

            if (symbol.isEmpty() || sharesStr.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הזן סמל מניה וכמות", Toast.LENGTH_SHORT).show();
                return;
            }

            int shares = Integer.parseInt(sharesStr);
            dialog.dismiss();
            fetchAndSellStock(symbol, shares);
        });

        dialog.show();
    }

    private void setupRangeViews(TextView[] rangeViews, boolean isStock) {
        String[] rangeMappings = {"1d", "5d", "1mo", "3mo", "1y"};

        for (int i = 0; i < rangeViews.length; i++) {
            final int index = i;
            rangeViews[i].setOnClickListener(v -> {
                // עדכון הטווח שנבחר
                String range = rangeMappings[index];

                // עדכון המראה החזותי
                updateSelectedRange(rangeViews, range, isStock);

                if (isStock) {
                    // עדכון נתוני מניה
                    selectedRange = range;
                    if (!currentSymbol.isEmpty()) {
                        fetchStockData(currentSymbol, selectedRange);
                        stockChart.showLoading();
                    }
                } else {
                    // עדכון נתוני פורטפוליו לפי טווח זמן
                    updatePortfolioChart(index);
                }
            });
        }
    }

    private void updateSelectedRange(TextView[] views, String range, boolean isStock) {
        String[] rangeMappings = {"1d", "5d", "1mo", "3mo", "1y"};
        int primaryColor = ContextCompat.getColor(requireContext(), R.color.primary);
        int secondaryColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);

        for (int i = 0; i < views.length; i++) {
            if ((isStock && rangeMappings[i].equals(range)) ||
                    (!isStock && i == getPortfolioTimeRangeIndex(range))) {
                views[i].setTextColor(primaryColor);
                views[i].setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                views[i].setTextColor(secondaryColor);
                views[i].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
        }
    }

    private int getPortfolioTimeRangeIndex(String range) {
        switch (range) {
            case "1d": return 0;
            case "5d": case "1w": return 1;
            case "1mo": return 2;
            case "3mo": case "6mo": return 3;
            case "1y": return 4;
            default: return 0;
        }
    }

    private void setupPortfolioSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, portfolioNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        portfolioSpinner.setAdapter(adapter);

        portfolioSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentPortfolioName = portfolioNames.get(position);
                updatePortfolioDisplay();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // לא עושה כלום
            }
        });
    }

    private void updatePortfolioDisplay() {
        // סימולציה של נתוני תיק
        updateHoldingsContainer();
        updatePortfolioChart(2); // טווח זמן ברירת מחדל: חודש
    }

    private void updateHoldingsContainer() {
        if (holdingsContainer == null) return;

        holdingsContainer.removeAllViews();

        if (userPortfolio.isEmpty()) {
            TextView emptyText = new TextView(requireContext());
            emptyText.setText("אין לך מניות בתיק.\nהתחל לסחור עכשיו!");
            emptyText.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            emptyText.setPadding(0, 50, 0, 50);
            holdingsContainer.addView(emptyText);

            portfolioValueText.setText("₪0.00");
            gainPercentageText.setText("0.00%");
            return;
        }

        double totalValue = 0;
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        // הוספת אייטמים עבור כל מניה בתיק
        for (Map.Entry<String, Object> entry : userPortfolio.entrySet()) {
            String symbol = entry.getKey();
            Long shares = (Long) entry.getValue();

            if (shares <= 0) continue;

            View holdingView = inflater.inflate(R.layout.item_holding, holdingsContainer, false);

            TextView symbolText = holdingView.findViewById(R.id.holding_symbol);
            TextView companyText = holdingView.findViewById(R.id.holding_company);
            TextView sharesText = holdingView.findViewById(R.id.holding_shares);
            TextView valueText = holdingView.findViewById(R.id.holding_value);
            TextView changeText = holdingView.findViewById(R.id.holding_change);

            symbolText.setText(symbol);
            companyText.setText(symbol); // בינתיים נציג רק את הסמל
            sharesText.setText(String.format(Locale.getDefault(), "%d מניות", shares));

            // בשלב זה נציג נתונים דמו עד שתהיה לנו קריאה אמיתית למחירים
            double mockPrice = getMockPrice(symbol);
            double mockValue = mockPrice * shares;
            totalValue += mockValue;

            valueText.setText(String.format(Locale.getDefault(), "₪%.2f", mockValue));

            // סימולציה של אחוז שינוי
            double mockChange = (Math.random() * 10) - 3; // בין -3% ל-7%
            changeText.setText(String.format(Locale.getDefault(), "%+.2f%%", mockChange));

            int colorResId = mockChange >= 0 ? R.color.profit_green : R.color.loss_red;
            changeText.setTextColor(ContextCompat.getColor(requireContext(), colorResId));

            holdingsContainer.addView(holdingView);

            // הוספת קו מפריד אם זה לא האייטם האחרון
            if (!entry.equals(userPortfolio.entrySet().toArray()[userPortfolio.size() - 1])) {
                View divider = new View(requireContext());
                divider.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (int) (requireContext().getResources().getDisplayMetrics().density + 0.5f) // 1dp
                ));
                divider.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.divider));
                holdingsContainer.addView(divider);
            }
        }

        // עדכון ערך כולל
        portfolioValueText.setText(String.format(Locale.getDefault(), "₪%.2f", totalValue));

        // סימולציה של תשואה
        double mockGain = (Math.random() * 15) - 5; // בין -5% ל-10%
        gainPercentageText.setText(String.format(Locale.getDefault(), "%+.2f%%", mockGain));

        int colorResId = mockGain >= 0 ? R.color.profit_green : R.color.loss_red;
        gainPercentageText.setTextColor(ContextCompat.getColor(requireContext(), colorResId));
    }

    // סימולציה של מחיר מניה
    private double getMockPrice(String symbol) {
        // סימולציה פשוטה - כל מניה מקבלת מחיר קבוע לפי האות הראשונה שלה
        char firstChar = symbol.charAt(0);
        double basePrice = (firstChar - 'A' + 1) * 10; // בין $10 ל-$260
        return basePrice + (Math.random() * basePrice * 0.1); // תנודה של עד 10%
    }

    private void updatePortfolioChart(int timeRangeIndex) {
        portfolioChart.showLoading();

        // סימולציה של נתוני גרף
        List<Float> values = generatePortfolioChartData(timeRangeIndex);
        List<String> labels = generateDateLabels(values.size(), timeRangeIndex);

        portfolioChart.setPrices(values);
        portfolioChart.setLabels(labels);

        portfolioChart.hideLoading();
    }

    private List<Float> generatePortfolioChartData(int timeRangeIndex) {
        List<Float> values = new ArrayList<>();
        int numPoints;
        float baseValue = 10000f;
        float volatility;
        float trend;

        switch (timeRangeIndex) {
            case 0: // יום
                numPoints = 24; // נקודה לכל שעה
                volatility = 0.002f;
                trend = 0.005f;
                break;
            case 1: // שבוע
                numPoints = 7; // נקודה לכל יום
                volatility = 0.005f;
                trend = 0.01f;
                break;
            case 2: // חודש
                numPoints = 30; // נקודה לכל יום בחודש
                volatility = 0.008f;
                trend = 0.02f;
                break;
            case 3: // שנה
                numPoints = 12; // נקודה לכל חודש
                volatility = 0.02f;
                trend = 0.15f;
                break;
            case 4: // הכל
                numPoints = 24; // 2 שנים, נקודה לכל חודש
                volatility = 0.03f;
                trend = 0.30f;
                break;
            default:
                numPoints = 30;
                volatility = 0.008f;
                trend = 0.02f;
                break;
        }

        // יצירת נקודות בגרף עם טרנד כללי כלפי מעלה ותנודתיות סביב הטרנד
        for (int i = 0; i < numPoints; i++) {
            float progress = (float) i / (numPoints - 1);
            float trendValue = baseValue * (1 + trend * progress);
            float noise = (float) ((Math.random() - 0.45) * trendValue * volatility);
            values.add(trendValue + noise);
        }

        return values;
    }

    private List<String> generateDateLabels(int count, int timeRangeIndex) {
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf;
        long intervalMs;

        // קביעת פורמט התאריכים וגודל הקפיצה לפי טווח הזמן
        switch (timeRangeIndex) {
            case 0: // יום
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                intervalMs = 60 * 60 * 1000; // שעה
                break;
            case 1: // שבוע
                sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000; // יום
                break;
            case 2: // חודש
                sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000; // יום
                break;
            case 3: // שנה
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                intervalMs = 30L * 24 * 60 * 60 * 1000; // חודש (בקירוב)
                break;
            case 4: // הכל
                sdf = new SimpleDateFormat("MM/yy", Locale.getDefault());
                intervalMs = 30L * 24 * 60 * 60 * 1000; // חודש
                break;
            default:
                sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000;
                break;
        }

        Date currentDate = new Date();

        // יצירת תוויות תאריך
        for (int i = count - 1; i >= 0; i--) {
            Date date = new Date(currentDate.getTime() - (i * intervalMs));
            // הצג רק חלק מהתאריכים לשמירה על נקיון
            if (i == 0 || i == count - 1 || i % Math.max(1, count / 5) == 0) {
                labels.add(sdf.format(date));
            } else {
                labels.add("");
            }
        }

        return labels;
    }

    private void fetchStockData(String symbol, String range) {
        stockChart.showLoading();

        // סימולציה של נתוני מניה
        simulateStockDataFetch(symbol, range);
    }

    // פונקצית סימולציה של בקשת API עד לשילוב האמיתי
    private void simulateStockDataFetch(String symbol, String range) {
        // נדמה תהליך של בקשה לשרת
        new Thread(() -> {
            try {
                Thread.sleep(1000); // סימולציה של השהייה ברשת

                // ערכים שימושיים לסימולציה
                double basePrice = getMockPrice(symbol);
                double openPrice = basePrice * (1 - 0.02 * Math.random());
                double closePrice = basePrice * (1 + 0.04 * Math.random() - 0.02);
                double lowPrice = Math.min(openPrice, closePrice) * (1 - 0.01 * Math.random());
                double highPrice = Math.max(openPrice, closePrice) * (1 + 0.01 * Math.random());
                double changeAmount = closePrice - openPrice;
                double changePercent = (changeAmount / openPrice) * 100;

                // יצירת נתוני גרף
                List<Float> priceValues = generateStockPriceData(range, openPrice, closePrice, lowPrice, highPrice);
                List<String> dateLabels = generateStockDateLabels(range, priceValues.size());

                // עדכון ממשק משתמש בתהליך הראשי
                requireActivity().runOnUiThread(() -> {
                    // עדכון פרטי המניה
                    stockPrice.setText(String.format(Locale.getDefault(), "₪%.2f", closePrice));
                    stockChange.setText(String.format(Locale.getDefault(), "%+.2f₪ (%+.2f%%)",
                            changeAmount, changePercent));

                    // עדכון צבע השינוי
                    int colorResId = changePercent >= 0 ? R.color.profit_green : R.color.loss_red;
                    stockChange.setTextColor(ContextCompat.getColor(requireContext(), colorResId));

                    // עדכון שדות נוספים
                    StoreFragment.this.openPrice.setText(String.format(Locale.getDefault(), "₪%.2f", openPrice));
                    StoreFragment.this.lowPrice.setText(String.format(Locale.getDefault(), "₪%.2f", lowPrice));
                    StoreFragment.this.highPrice.setText(String.format(Locale.getDefault(), "₪%.2f", highPrice));

                    // עדכון מידע על מחזור מסחר
                    long volume = (long) (Math.random() * 10_000_000);
                    String volumeFormatted;
                    if (volume >= 1_000_000) {
                        volumeFormatted = String.format(Locale.getDefault(), "%.1fM", volume / 1_000_000.0);
                    } else if (volume >= 1_000) {
                        volumeFormatted = String.format(Locale.getDefault(), "%.1fK", volume / 1_000.0);
                    } else {
                        volumeFormatted = String.valueOf(volume);
                    }
                    volumeText.setText(volumeFormatted);

                    // עדכון הגרף
                    stockChart.setPrices(priceValues);
                    stockChart.setLabels(dateLabels);
                    stockChart.hideLoading();
                });

            } catch (InterruptedException e) {
                e.printStackTrace();

                requireActivity().runOnUiThread(() -> {
                    stockChart.hideLoading();
                    Toast.makeText(requireContext(), "שגיאה בטעינת נתוני מניה", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private List<Float> generateStockPriceData(String range, double open, double close, double low, double high) {
        List<Float> prices = new ArrayList<>();
        int numPoints;

        // קביעת מספר הנקודות לפי טווח הזמן
        switch (range) {
            case "1d":
                numPoints = 24;
                break;
            case "5d":
                numPoints = 5;
                break;
            case "1mo":
                numPoints = 30;
                break;
            case "3mo":
                numPoints = 12;
                break;
            case "6mo":
                numPoints = 24;
                break;
            case "1y":
                numPoints = 12;
                break;
            default:
                numPoints = 30;
                break;
        }

        // יצירת נתונים עם טרנד מ-open ל-close עם תנודתיות סביב הקו
        double range_height = high - low;
        double trend_line_slope = (close - open) / (numPoints - 1);

        for (int i = 0; i < numPoints; i++) {
            double trendLineValue = open + (trend_line_slope * i);
            double noise = (Math.random() - 0.5) * range_height * 0.3;
            double price = Math.max(low, Math.min(high, trendLineValue + noise));
            prices.add((float) price);
        }

        return prices;
    }

    private List<String> generateStockDateLabels(String range, int count) {
        List<String> labels = new ArrayList<>();
        SimpleDateFormat sdf;
        long intervalMs;

        // קביעת פורמט התאריכים וגודל הקפיצה לפי טווח הזמן
        switch (range) {
            case "1d":
                sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                intervalMs = 60 * 60 * 1000; // שעה
                break;
            case "5d":
                sdf = new SimpleDateFormat("EEE", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000; // יום
                break;
            case "1mo":
                sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000; // יום
                break;
            case "3mo":
            case "6mo":
                sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                intervalMs = 7L * 24 * 60 * 60 * 1000; // שבוע
                break;
            case "1y":
                sdf = new SimpleDateFormat("MMM", Locale.getDefault());
                intervalMs = 30L * 24 * 60 * 60 * 1000; // חודש (בקירוב)
                break;
            default:
                sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
                intervalMs = 24 * 60 * 60 * 1000;
                break;
        }

        Date currentDate = new Date();

        // יצירת תוויות תאריך
        for (int i = count - 1; i >= 0; i--) {
            Date date = new Date(currentDate.getTime() - (i * intervalMs));
            // הצג רק חלק מהתאריכים לשמירה על נקיון
            if (i == 0 || i == count - 1 || i % Math.max(1, count / 5) == 0) {
                labels.add(sdf.format(date));
            } else {
                labels.add("");
            }
        }

        return labels;
    }

    private void showBuyDialog(String symbol) {
        // בדיקה שהמשתמש מחובר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "אנא התחבר כדי לסחור", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_buy_stock);

        TextView symbolText = dialog.findViewById(R.id.dialog_symbol);
        TextView priceText = dialog.findViewById(R.id.dialog_price);
        TextView totalText = dialog.findViewById(R.id.dialog_total);
        EditText sharesInput = dialog.findViewById(R.id.dialog_shares);
        Button buyButton = dialog.findViewById(R.id.dialog_buy_button);

        // השגת המחיר הנוכחי מהתצוגה
        double currentPrice = 0;
        try {
            currentPrice = Double.parseDouble(stockPrice.getText().toString().replace("₪", ""));
        } catch (NumberFormatException e) {
            // אם אין מחיר בתצוגה, נשתמש במחיר מדומה
            currentPrice = getMockPrice(symbol);
        }

        final double price = currentPrice;

        symbolText.setText(symbol);
        priceText.setText(String.format(Locale.getDefault(), "₪%.2f", price));

        // עדכון המחיר הכולל בשינוי כמות המניות
        sharesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int shares = 0;
                try {
                    shares = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    // טיפול בשגיאת פירסור
                }
                double total = shares * price;
                totalText.setText(String.format(Locale.getDefault(), "₪%.2f", total));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // מאזין ללחיצה על כפתור הקנייה
        final double finalPrice = price;
        buyButton.setOnClickListener(v -> {
            String sharesStr = sharesInput.getText().toString();
            if (sharesStr.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הזן כמות מניות", Toast.LENGTH_SHORT).show();
                return;
            }

            int shares = Integer.parseInt(sharesStr);
            if (shares <= 0) {
                Toast.makeText(requireContext(), "כמות המניות חייבת להיות חיובית", Toast.LENGTH_SHORT).show();
                return;
            }

            double total = shares * finalPrice;
            if (total > userCoins) {
                Toast.makeText(requireContext(), "אין מספיק מטבעות לרכישה זו", Toast.LENGTH_SHORT).show();
                return;
            }

            // ביצוע הרכישה
            buyStock(symbol, shares, finalPrice);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showSellDialog(String symbol) {
        // בדיקה שהמשתמש מחובר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "אנא התחבר כדי לסחור", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שיש למשתמש את המניה
        Long ownedShares = getOwnedShares(symbol);
        if (ownedShares == null || ownedShares <= 0) {
            Toast.makeText(requireContext(), "אין לך מניות של " + symbol + " למכירה", Toast.LENGTH_SHORT).show();
            return;
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_sell_stock);

        TextView symbolText = dialog.findViewById(R.id.dialog_symbol);
        TextView priceText = dialog.findViewById(R.id.dialog_price);
        TextView totalText = dialog.findViewById(R.id.dialog_total);
        TextView ownedText = dialog.findViewById(R.id.dialog_owned);
        EditText sharesInput = dialog.findViewById(R.id.dialog_shares);
        Button sellButton = dialog.findViewById(R.id.dialog_sell_button);

        // השגת המחיר הנוכחי מהתצוגה
        double currentPrice = 0;
        try {
            currentPrice = Double.parseDouble(stockPrice.getText().toString().replace("₪", ""));
        } catch (NumberFormatException e) {
            // אם אין מחיר בתצוגה, נשתמש במחיר מדומה
            currentPrice = getMockPrice(symbol);
        }

        final double price = currentPrice;

        symbolText.setText(symbol);
        priceText.setText(String.format(Locale.getDefault(), "₪%.2f", price));
        ownedText.setText(String.valueOf(ownedShares));

        // עדכון המחיר הכולל בשינוי כמות המניות
        sharesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int shares = 0;
                try {
                    shares = Integer.parseInt(s.toString());
                } catch (NumberFormatException e) {
                    // טיפול בשגיאת פירסור
                }
                double total = shares * price;
                totalText.setText(String.format(Locale.getDefault(), "₪%.2f", total));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // מאזין ללחיצה על כפתור המכירה
        final double finalPrice = price;
        sellButton.setOnClickListener(v -> {
            String sharesStr = sharesInput.getText().toString();
            if (sharesStr.isEmpty()) {
                Toast.makeText(requireContext(), "אנא הזן כמות מניות", Toast.LENGTH_SHORT).show();
                return;
            }

            int shares = Integer.parseInt(sharesStr);
            if (shares <= 0) {
                Toast.makeText(requireContext(), "כמות המניות חייבת להיות חיובית", Toast.LENGTH_SHORT).show();
                return;
            }

            if (shares > ownedShares) {
                Toast.makeText(requireContext(), "אין לך מספיק מניות למכירה", Toast.LENGTH_SHORT).show();
                return;
            }

            // ביצוע המכירה
            sellStock(symbol, shares, finalPrice);
            dialog.dismiss();
        });

        dialog.show();
    }

    private Long getOwnedShares(String symbol) {
        Object shares = userPortfolio.get(symbol);
        if (shares instanceof Long) {
            return (Long) shares;
        }
        return 0L;
    }

    private void fetchAndBuyStock(String symbol, int shares) {
        // בדיקה שהמשתמש מחובר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "אנא התחבר כדי לסחור", Toast.LENGTH_SHORT).show();
            return;
        }

        // השגת מחיר עדכני למניה
        double price = getMockPrice(symbol);
        double totalCost = price * shares;

        if (totalCost > userCoins) {
            Toast.makeText(requireContext(), "אין מספיק מטבעות לרכישה זו", Toast.LENGTH_SHORT).show();
            return;
        }

        buyStock(symbol, shares, price);
    }

    private void fetchAndSellStock(String symbol, int shares) {
        // בדיקה שהמשתמש מחובר
        if (auth.getCurrentUser() == null) {
            Toast.makeText(requireContext(), "אנא התחבר כדי לסחור", Toast.LENGTH_SHORT).show();
            return;
        }

        // בדיקה שיש למשתמש את המניה
        Long ownedShares = getOwnedShares(symbol);
        if (ownedShares == null || ownedShares < shares) {
            Toast.makeText(requireContext(), "אין לך מספיק מניות של " + symbol + " למכירה", Toast.LENGTH_SHORT).show();
            return;
        }

        // השגת מחיר עדכני למניה
        double price = getMockPrice(symbol);

        sellStock(symbol, shares, price);
    }

    private void buyStock(String symbol, int shares, double price) {
        double totalCost = shares * price;

        if (totalCost > userCoins) {
            Toast.makeText(requireContext(), "אין מספיק מטבעות לרכישה זו", Toast.LENGTH_SHORT).show();
            return;
        }

        // עדכון הנתונים בפיירבייס
        userRef.child("coins").setValue(userCoins - totalCost);

        // בדיקה אם כבר יש למשתמש מניות מסוג זה
        Long currentShares = getOwnedShares(symbol);
        if (currentShares == null) currentShares = 0L;

        userRef.child("portfolio").child(symbol).setValue(currentShares + shares);

        // עדכון הנתונים המקומיים
        userCoins -= totalCost;
        updateCoinsDisplay();

        // תיעוד העסקה
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("symbol", symbol);
        transaction.put("shares", shares);
        transaction.put("price", price);
        transaction.put("type", "buy");
        transaction.put("timestamp", System.currentTimeMillis());

        userRef.child("transactions").child(transactionId).setValue(transaction);

        Toast.makeText(requireContext(),
                String.format(Locale.getDefault(), "קנית %d מניות של %s ב-₪%.2f", shares, symbol, totalCost),
                Toast.LENGTH_SHORT).show();
    }

    private void sellStock(String symbol, int shares, double price) {
        // בדיקה שיש למשתמש את המניה
        Long ownedShares = getOwnedShares(symbol);
        if (ownedShares == null || ownedShares < shares) {
            Toast.makeText(requireContext(), "אין לך מספיק מניות של " + symbol + " למכירה", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalValue = shares * price;

        // עדכון הנתונים בפיירבייס
        userRef.child("coins").setValue(userCoins + totalValue);
        userRef.child("portfolio").child(symbol).setValue(ownedShares - shares);

        // עדכון הנתונים המקומיים
        userCoins += totalValue;
        updateCoinsDisplay();

        // תיעוד העסקה
        String transactionId = UUID.randomUUID().toString();
        Map<String, Object> transaction = new HashMap<>();
        transaction.put("symbol", symbol);
        transaction.put("shares", shares);
        transaction.put("price", price);
        transaction.put("type", "sell");
        transaction.put("timestamp", System.currentTimeMillis());

        userRef.child("transactions").child(transactionId).setValue(transaction);

        Toast.makeText(requireContext(),
                String.format(Locale.getDefault(), "מכרת %d מניות של %s ב-₪%.2f", shares, symbol, totalValue),
                Toast.LENGTH_SHORT).show();
    }
}