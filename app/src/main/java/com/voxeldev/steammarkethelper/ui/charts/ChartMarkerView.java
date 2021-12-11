package com.voxeldev.steammarkethelper.ui.charts;

import android.content.Context;
import android.graphics.Canvas;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.market.MarketItemPriceHistory;

public class ChartMarkerView extends MarkerView {
    private final MarketItemPriceHistory priceHistory;
    private final TextView datetimeTextView;
    private final TextView priceTextView;
    private final TextView amountTextView;

    public ChartMarkerView(Context context, int resource, MarketItemPriceHistory priceHistory, LineChart chart) {
        super(context, resource);

        setChartView(chart);

        this.priceHistory = priceHistory;

        datetimeTextView = findViewById(R.id.markerview_datetime);
        priceTextView = findViewById(R.id.markerview_price);
        amountTextView = findViewById(R.id.markerview_amount);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        datetimeTextView.setText(priceHistory.prices.get((int)e.getX()).get(0));
        String price = priceHistory.prices.get((int)e.getX()).get(1);
        if (!priceHistory.price_prefix.isEmpty()) { price = String.format("%s %s", priceHistory.price_prefix, price); }
        if (!priceHistory.price_suffix.isEmpty()) { price = String.format("%s %s", price, priceHistory.price_suffix); }
        priceTextView.setText(price);
        amountTextView.setText(priceHistory.prices.get((int)e.getX()).get(2));

        super.refreshContent(e, highlight);
    }

    @Override
    public void draw(Canvas canvas, float posX, float posY) {
        super.draw(canvas, posX, posY);
        getOffsetForDrawingAtPoint(posX, posY);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(0, -100);
    }
}
