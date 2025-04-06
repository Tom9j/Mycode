package com.example.trivia;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SimpleLineChartView extends View {

    private List<Float> prices;
    private List<String> labels; // עבור תוויות ציר X
    private Paint linePaint, axisPaint, textPaint, fillPaint, gridPaint;
    private Path linePath, fillPath;

    // צבעים מהעיצוב של פורטפוליו השקעות
    private int primaryColor = Color.parseColor("#1E88E5"); // כחול בסיסי
    private int profitColor = Color.parseColor("#00C853"); // ירוק רווח
    private int lossColor = Color.parseColor("#D50000"); // אדום הפסד
    private int gridColor = Color.parseColor("#EEEEEE"); // אפור בהיר לרשת
    private int textColor = Color.parseColor("#757575"); // טקסט משני
    private int headerTextColor = Color.parseColor("#212121"); // טקסט ראשי

    private String chartTitle = "Stock Price";
    private boolean isLoading = false;
    private boolean animateLine = true;
    private float animationProgress = 0f;
    private Handler animationHandler;
    private Runnable animationRunnable;

    private boolean isTrendUp = true; // האם המגמה עולה או יורדת

    public SimpleLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SimpleLineChartView(Context context) {
        super(context);
        init();
    }

    public SimpleLineChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void showLoading() {
        isLoading = true;
        invalidate();
    }

    public void hideLoading() {
        isLoading = false;
        invalidate();
    }

    private void init() {
        // Line paint for main chart line
        linePaint = new Paint();
        linePaint.setStrokeWidth(4f);  // קו יותר עבה
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setAntiAlias(true);

        // Grid paint for dashed grid lines
        gridPaint = new Paint();
        gridPaint.setColor(gridColor);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setPathEffect(new DashPathEffect(new float[] {5, 5}, 0));

        // Axis paint for solid axes
        axisPaint = new Paint();
        axisPaint.setColor(gridColor);
        axisPaint.setStrokeWidth(2f);

        // Text paint for labels
        textPaint = new Paint();
        textPaint.setColor(textColor);
        textPaint.setTextSize(28f);  // גודל טקסט מותאם יותר
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setAntiAlias(true);

        // Fill paint for gradient underneath the line
        fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setAntiAlias(true);

        // Paths for drawing
        linePath = new Path();
        fillPath = new Path();

        // מאתחל את מערך התוויות
        labels = new ArrayList<>();

        // מאתחל את ההנדלר לאנימציית הקו
        animationHandler = new Handler(Looper.getMainLooper());

        updateChartColor();
    }

    private void updateChartColor() {
        // קובע צבע בהתאם למגמה
        int chartColor = isTrendUp ? profitColor : lossColor;
        linePaint.setColor(chartColor);
        primaryColor = chartColor;
        invalidate();
    }

    public void setPrices(List<Float> prices) {
        if (prices == null || prices.size() < 2) {
            this.prices = null;
            invalidate();
            return;
        }

        this.prices = new ArrayList<>(prices);

        // קובע את המגמה על פי ההשוואה בין הערך הראשון לאחרון
        isTrendUp = prices.get(0) <= prices.get(prices.size() - 1);
        updateChartColor();

        // יוצר תוויות ברירת מחדל אם אין
        if (labels == null || labels.isEmpty() || labels.size() != prices.size()) {
            labels = new ArrayList<>();
            for (int i = 0; i < prices.size(); i++) {
                // אם זה נקודה ראשונה, אמצעית או אחרונה, או חלק מ-4 נקודות שווה מרחק
                if (i == 0 || i == prices.size() - 1 || i % (Math.max(1, prices.size() / 4)) == 0) {
                    labels.add("D" + i);
                } else {
                    labels.add("");
                }
            }
        }

        // מפעיל אנימציה לציור הגרף
        if (animateLine) {
            startAnimation();
        } else {
            animationProgress = 1f;
            invalidate();
        }
    }

    public void setLabels(List<String> labels) {
        this.labels = new ArrayList<>(labels);
        invalidate();
    }

    public void setDateLabels(List<Date> dates) {
        if (dates == null || dates.isEmpty()) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd", Locale.getDefault());
        List<String> dateLabels = new ArrayList<>();

        for (Date date : dates) {
            dateLabels.add(sdf.format(date));
        }

        setLabels(dateLabels);
    }

    public void setChartColor(int color) {
        this.primaryColor = color;
        linePaint.setColor(color);
        invalidate();
    }

    public void setChartTitle(String title) {
        this.chartTitle = title;
        invalidate();
    }

    public void setAnimateLine(boolean animate) {
        this.animateLine = animate;
    }

    private void startAnimation() {
        // מבטל את האנימציה הקיימת אם יש
        if (animationRunnable != null) {
            animationHandler.removeCallbacks(animationRunnable);
        }

        // אפס את ההתקדמות
        animationProgress = 0f;

        // יצירת פעולה לאנימציה
        animationRunnable = new Runnable() {
            private long startTime = System.currentTimeMillis();
            private final long duration = 1000; // אנימציה של שניה

            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                animationProgress = Math.min(1f, (float) elapsed / duration);

                invalidate();

                if (animationProgress < 1f) {
                    // המשך האנימציה
                    animationHandler.postDelayed(this, 16); // ~60fps
                }
            }
        };

        // התחל את האנימציה
        animationHandler.post(animationRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // צייר רקע לבן
        canvas.drawColor(Color.WHITE);

        // צייר כותרת
        drawChartTitle(canvas);

        if (isLoading) {
            drawLoading(canvas);
            return;
        }

        if (prices == null || prices.size() < 2) {
            drawNoData(canvas);
            return;
        }

        float width = getWidth();
        float height = getHeight();
        float padding = 80f;
        float chartLeft = padding;
        float chartRight = width - padding / 2;
        float chartTop = padding;
        float chartBottom = height - padding;
        float chartWidth = chartRight - chartLeft;
        float chartHeight = chartBottom - chartTop;

        // חישוב הערך המינימלי והמקסימלי עם שוליים
        float max = Float.MIN_VALUE;
        float min = Float.MAX_VALUE;
        for (float price : prices) {
            if (price > max) max = price;
            if (price < min) min = price;
        }

        // הוספת שוליים למינימום ומקסימום לשיפור המראה החזותי
        float range = max - min;
        min = min - range * 0.05f;
        max = max + range * 0.05f;
        range = max - min;

        // חישוב צעד X על בסיס מספר נקודות הנתונים
        float stepX = chartWidth / (prices.size() - 1);

        // צייר קווי רשת אופקיים ותוויות ציר Y
        int yDivisions = 5;
        for (int i = 0; i <= yDivisions; i++) {
            float y = chartBottom - (chartHeight * i / yDivisions);
            float priceLabel = min + (range * i / yDivisions);

            // קו רשת
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);

            // תווית
            String label = String.format(Locale.getDefault(), "%.2f", priceLabel);
            float textWidth = textPaint.measureText(label);
            canvas.drawText(label, chartLeft - textWidth - 10, y + 8, textPaint);
        }

        // צייר ציר X בתחתית
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint);

        // חישוב נקודת הסיום של האנימציה
        float animatedEnd = chartLeft + chartWidth * animationProgress;

        // הכן את נתיב הקו ונתיב המילוי
        linePath.reset();
        fillPath.reset();

        // נקודות התחלה
        float startX = chartLeft;
        float startY = chartBottom - ((prices.get(0) - min) / range) * chartHeight;
        linePath.moveTo(startX, startY);
        fillPath.moveTo(startX, chartBottom);
        fillPath.lineTo(startX, startY);

        // צייר נקודות נתונים והשלם את הנתיבים עם אנימציה
        for (int i = 0; i < prices.size(); i++) {
            float x = chartLeft + i * stepX;

            // אם מעבר להתקדמות האנימציה, עצור
            if (x > animatedEnd) break;

            float y = chartBottom - ((prices.get(i) - min) / range) * chartHeight;

            if (i > 0) {
                linePath.lineTo(x, y);
                fillPath.lineTo(x, y);
            }

            // צייר תוויות ציר X עבור נקודות מסוימות
            if (i < labels.size() && !labels.get(i).isEmpty()) {
                String label = labels.get(i);
                float textWidth = textPaint.measureText(label);
                float labelX = x - textWidth / 2;

                // וודא שהתווית לא נחתכת בקצוות
                labelX = Math.max(labelX, 0);
                labelX = Math.min(labelX, width - textWidth);

                canvas.drawText(label, labelX, chartBottom + 30, textPaint);
            }
        }

        // השלם את נתיב המילוי
        fillPath.lineTo(animatedEnd, chartBottom);
        fillPath.close();

        // יצור מילוי גרדיאנט מתחת לקו עם סגנון עדין יותר
        LinearGradient gradient = new LinearGradient(
                0, chartTop, 0, chartBottom,
                Color.argb(120, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                Color.argb(10, Color.red(primaryColor), Color.green(primaryColor), Color.blue(primaryColor)),
                Shader.TileMode.CLAMP
        );
        fillPaint.setShader(gradient);

        // צייר את המילוי ואת הקו
        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }

    private void drawChartTitle(Canvas canvas) {
        float width = getWidth();
        float titleY = 40;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(32f);
        textPaint.setColor(headerTextColor);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));

        canvas.drawText(chartTitle, width / 2, titleY, textPaint);

        // שחזר הגדרות קודמות
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(28f);
        textPaint.setColor(textColor);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
    }

    private void drawLoading(Canvas canvas) {
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(32f);

        canvas.drawText("טוען נתונים...", centerX, centerY, textPaint);

        // שחזר הגדרות
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(28f);
    }

    private void drawNoData(Canvas canvas) {
        float centerX = getWidth() / 2;
        float centerY = getHeight() / 2;

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(32f);

        canvas.drawText("אין נתונים זמינים", centerX, centerY, textPaint);

        // שחזר הגדרות
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(28f);
    }
}