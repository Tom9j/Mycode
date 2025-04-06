package com.example.trivia.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trivia.R;
import com.example.trivia.models.StockHolding;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HoldingsAdapter extends RecyclerView.Adapter<HoldingsAdapter.ViewHolder> {

    private List<StockHolding> holdings;
    private OnHoldingClickListener listener;

    public HoldingsAdapter() {
        this.holdings = new ArrayList<>();
    }

    public void setHoldings(List<StockHolding> holdings) {
        this.holdings = holdings;
        notifyDataSetChanged();
    }

    public void setOnHoldingClickListener(OnHoldingClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_holding, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StockHolding holding = holdings.get(position);
        holder.bind(holding);
    }

    @Override
    public int getItemCount() {
        return holdings.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView symbolText;
        private TextView companyText;
        private TextView sharesText;
        private TextView valueText;
        private TextView changeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            symbolText = itemView.findViewById(R.id.holding_symbol);
            companyText = itemView.findViewById(R.id.holding_company);
            sharesText = itemView.findViewById(R.id.holding_shares);
            valueText = itemView.findViewById(R.id.holding_value);
            changeText = itemView.findViewById(R.id.holding_change);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onHoldingClick(holdings.get(position));
                }
            });
        }

        public void bind(StockHolding holding) {
            symbolText.setText(holding.getSymbol());
            companyText.setText(holding.getCompanyName());
            sharesText.setText(String.format(Locale.getDefault(), "%d מניות", holding.getShares()));
            valueText.setText(String.format(Locale.getDefault(), "₪%.2f", holding.getTotalValue()));

            double changePercent = holding.getProfitLossPercentage();
            changeText.setText(String.format(Locale.getDefault(), "%+.2f%%", changePercent));

            if (changePercent >= 0) {
                changeText.setTextColor(Color.parseColor("#00C853")); // ירוק רווח
            } else {
                changeText.setTextColor(Color.parseColor("#D50000")); // אדום הפסד
            }
        }
    }

    public interface OnHoldingClickListener {
        void onHoldingClick(StockHolding holding);
    }
}