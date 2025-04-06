package com.example.trivia.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.trivia.R;
import com.example.trivia.models.Portfolio;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PortfolioAdapter extends RecyclerView.Adapter<PortfolioAdapter.ViewHolder> {

    private List<Portfolio> portfolios;
    private OnPortfolioClickListener listener;

    public PortfolioAdapter() {
        this.portfolios = new ArrayList<>();
    }

    public void setPortfolios(List<Portfolio> portfolios) {
        this.portfolios = portfolios;
        notifyDataSetChanged();
    }

    public void setOnPortfolioClickListener(OnPortfolioClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_protfolio, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Portfolio portfolio = portfolios.get(position);
        holder.bind(portfolio);
    }

    @Override
    public int getItemCount() {
        return portfolios.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private TextView nameText;
        private TextView valueText;
        private TextView dateText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.portfolio_name);
            valueText = itemView.findViewById(R.id.portfolio_value);
            dateText = itemView.findViewById(R.id.portfolio_date);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onPortfolioClick(portfolios.get(position));
                }
            });
        }

        public void bind(Portfolio portfolio) {
            nameText.setText(portfolio.getName());

            // חישוב שווי תיק כולל
            double totalValue = 0;
            if (portfolio.getHoldings() != null) {
                for (String key : portfolio.getHoldings().keySet()) {
                    totalValue += portfolio.getHoldings().get(key).getTotalValue();
                }
            }

            valueText.setText(String.format(Locale.getDefault(), "₪%.2f", totalValue));

            // פורמט תאריך יצירת התיק
            Date date = new Date(portfolio.getCreatedAt());
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            dateText.setText("נוצר בתאריך: " + sdf.format(date));
        }
    }

    public interface OnPortfolioClickListener {
        void onPortfolioClick(Portfolio portfolio);
    }
}