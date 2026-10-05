package com.opablosantanaa.cambiopablo;

import android.os.Bundle;
import androidx.core.content.ContextCompat;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.opablosantanaa.cambiopablo.ui.DarkVeilView;
import com.opablosantanaa.cambiopablo.ui.GlideCurrencyPicker;
import com.opablosantanaa.cambiopablo.ui.JellyTabsLayout;
import com.opablosantanaa.cambiopablo.api.ApiService;
import com.opablosantanaa.cambiopablo.api.Currency;
import com.opablosantanaa.cambiopablo.api.RetrofitClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private static final String[] CODES = {"USD", "EUR", "GBP", "JPY", "BTC"};
    private static final String[] NAMES = {"Dólar americano", "Euro", "Libra esterlina", "Iene japonês", "Bitcoin"};
    private static final String[] SYMBOLS = {"US$", "€", "£", "¥", "₿"};
    private static final Locale PT = new Locale("pt", "BR");
    private int foreground, muted;
    private DarkVeilView darkVeil;
    private EditText amount;
    private TextView result, hint, updated, source, sourceName, marketStatus;
    private Button convert;
    private LinearLayout quoteList, historyList;
    private JellyTabsLayout jellyTabs;
    private final GlideCurrencyPicker currencyPicker = new GlideCurrencyPicker();
    private ApiService api;
    private final Map<String, Currency> quotes = new HashMap<>();
    private Call<Map<String, Currency>> quoteCall, conversionCall;
    private int selected = 0, tab = 0;
    private boolean loading;
    private long quoteTime;
    private JSONArray history;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_main);
        darkVeil = findViewById(R.id.darkVeil);
        foreground = ContextCompat.getColor(this, R.color.white);
        muted = ContextCompat.getColor(this, R.color.muted);
        View root = findViewById(R.id.root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        ViewCompat.requestApplyInsets(root);
        amount = findViewById(R.id.addValue);
        amount.setSaveEnabled(false);
        amount.setKeyListener(android.text.method.DigitsKeyListener.getInstance("0123456789.,"));
        amount.setRawInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        result = findViewById(R.id.finalResult);
        hint = findViewById(R.id.resultHint);
        updated = findViewById(R.id.tvUpdatedAt);
        source = findViewById(R.id.sourceCurrency);
        sourceName = findViewById(R.id.sourceName);
        if (getResources().getConfiguration().screenWidthDp < 360 || getResources().getConfiguration().fontScale > 1.15f) {
            LinearLayout row = (LinearLayout) amount.getParent();
            row.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(-1, dp(56));
            inputParams.topMargin = dp(12);
            amount.setLayoutParams(inputParams);
        }
        marketStatus = findViewById(R.id.marketStatus);
        convert = findViewById(R.id.btnConvert);
        jellyTabs = findViewById(R.id.jellyTabs);
        quoteList = findViewById(R.id.quoteList);
        historyList = findViewById(R.id.historyList);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(result, 22, 42, 1, android.util.TypedValue.COMPLEX_UNIT_SP);
        result.setMaxLines(1);
        api = RetrofitClient.getClient().create(ApiService.class);
        try { history = new JSONArray(getPreferences(MODE_PRIVATE).getString("history", "[]")); }
        catch (JSONException e) { history = new JSONArray(); }
        source.setOnClickListener(v -> chooseCurrency());
        convert.setOnClickListener(v -> converter());
        amount.setOnEditorActionListener((v, action, event) -> {
            if (action == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) { converter(); return true; }
            return false;
        });
        amount.addTextChangedListener(new android.text.TextWatcher() {
            private String digits = "";
            private boolean formatting;
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (formatting) return;
                cancelConversion();
                amount.setError(null);
                result.setText(getString(R.string.ui_r));
                hint.setText(getString(R.string.conversion_pending));
                updated.setText(getString(R.string.ui_cotacao_comercial_taxas_nao_incluidas));

                if(count > 0 && before == 0){
                    CharSequence added = s.subSequence(start, start + count);
                    for (int i = 0; i < added.length(); i++) {
                        char c = added.charAt(i);
                        if(c == ',' || c == '.'){
                            digits += "0";
                        } else if (Character.isDigit(c)) {
                            digits += c;
                        }
                    }
                } else if (count == 0 && before > 0) {
                    int deleteCount = before;
                    if (deleteCount >= digits.length()) {
                        digits = "";
                    } else {
                        digits = digits.substring(0, digits.length() - deleteCount);
                    }
                } else if (count > 0 && before > 0) {
                    digits = s.toString().replaceAll("[^0-9]", "");
                }
            }
            public void afterTextChanged(android.text.Editable s) {
                if (formatting) return;
                String formatted = AmountInputFormatter.format(digits);
                if (!formatted.contentEquals(s)) {
                    formatting = true;
                    try {
                        s.replace(0, s.length(), formatted);
                        amount.setSelection(amount.length());
                    } finally {
                        formatting = false;
                    }
                }
            }
        });
        findViewById(R.id.refreshQuotes).setOnClickListener(v -> fetchQuotes());
        findViewById(R.id.navConvert).setOnClickListener(v -> showTab(0));
        findViewById(R.id.navMarket).setOnClickListener(v -> showTab(1));
        findViewById(R.id.navHistory).setOnClickListener(v -> showTab(2));
        findViewById(R.id.clearHistory).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
            .setTitle(R.string.clear_history_title)
            .setMessage(R.string.clear_history_message)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.clear_history_action, (dialog, which) -> clearHistory())
            .show());
        findViewById(R.id.help).setOnClickListener(v -> new MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.ui_sobre_as_cotacoes))
            .setMessage(getString(R.string.help_body))
            .setPositiveButton(getString(R.string.understood), null).show());
        if (state != null) {
            selected = Math.max(0, Math.min(CODES.length - 1, state.getInt("selected")));
            amount.setText(state.getString("amount", ""));
            result.setText(state.getString("result", getString(R.string.ui_r)));
            hint.setText(state.getString("hint", getString(R.string.ui_escolha_a_moeda_e_informe_o_valor)));
            updated.setText(state.getString("updated", getString(R.string.ui_cotacao_comercial_taxas_nao_incluidas)));
            tab = state.getInt("tab");
        }
        updateSource();
        showTab(tab);
        renderQuotes();
        fetchQuotes();
    }

    private void chooseCurrency() {
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(amount.getWindowToken(), 0);
        amount.clearFocus();
        source.post(() -> {
            if (!isFinishing() && !isDestroyed() && source.hasWindowFocus()) {
                currencyPicker.show(source, CODES, NAMES, selected, this::selectCurrency);
            }
        });
    }
    private void selectCurrency(int index) {
        cancelConversion();
        selected = index;
        updateSource();
        result.setText(getString(R.string.ui_r));
        updated.setText(getString(R.string.ui_cotacao_comercial_taxas_nao_incluidas));
        showTab(0);
    }
    private void updateSource() {
        source.setText(getString(R.string.source_label, SYMBOLS[selected], CODES[selected]));
        source.setContentDescription(getString(R.string.source_accessibility, NAMES[selected]));
        sourceName.setText(NAMES[selected]);
    }
    private void showTab(int index) {
        tab = index;
        findViewById(R.id.developerCredit).setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.converterSection).setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        findViewById(R.id.marketSection).setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        findViewById(R.id.historySection).setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        int[] ids = {R.id.navConvert, R.id.navMarket, R.id.navHistory};
        for (int i = 0; i < ids.length; i++) {
            TextView nav = findViewById(ids[i]);
            nav.setTextColor(i == index ? foreground : muted);
            nav.setSelected(i == index);
        }
        jellyTabs.setSelectedIndex(index);
        if (index == 2) renderHistory();
        ((android.widget.ScrollView) findViewById(R.id.converterSection).getParent().getParent()).post(() -> ((android.widget.ScrollView) findViewById(R.id.converterSection).getParent().getParent()).scrollTo(0, 0));
    }
    private void setLoading(boolean value) {
        loading = value;
        convert.setEnabled(!value);
        convert.setAlpha(value ? 0.65f : 1f);
        convert.setText(value ? getString(R.string.loading_conversion) : getString(R.string.ui_converter_agora));
    }
    private void cancelConversion() {
        if (conversionCall != null) { conversionCall.cancel(); conversionCall = null; }
        setLoading(false);
    }
    public void converter() {
        if (loading) return;
        final BigDecimal value;
        try {
            String raw = amount.getText().toString().trim();
            if (raw.contains(",")) {
                if (!raw.matches("(?:[0-9]+|[0-9]{1,3}(?:\\.[0-9]{3})+),[0-9]{1,8}")) throw new NumberFormatException();
                raw = raw.replace(".", "").replace(',', '.');
            } else if (!raw.matches("[0-9]+(?:\\.[0-9]{1,8})?")) throw new NumberFormatException();
            value = new BigDecimal(raw);
            if (value.signum() <= 0 || value.compareTo(new BigDecimal("999999999999")) > 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            amount.setError(getString(R.string.amount_invalid));
            amount.requestFocus();
            return;
        }
        amount.setError(null);
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(amount.getWindowToken(), 0);
        amount.clearFocus();
        final String code = CODES[selected];
        setLoading(true);
        hint.setText(getString(R.string.loading_pair, code));
        updated.setText(getString(R.string.loading_api));
        conversionCall = api.getExchangeRate(code + "-BRL");
        conversionCall.enqueue(new Callback<Map<String, Currency>>() {
            @Override public void onResponse(Call<Map<String, Currency>> call, Response<Map<String, Currency>> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()){
                    return;
                }
                setLoading(false);
                Currency currency = response.body() == null ? null : response.body().get(code + "BRL");
                if (!response.isSuccessful() || currency == null) {
                    conversionError(getString(R.string.conversion_unavailable)); return;
                }
                try {
                    BigDecimal rate = new BigDecimal(currency.bid);
                    if (rate.signum() <= 0) throw new NumberFormatException();
                    BigDecimal total = value.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                    result.setText(money(total));
                    hint.setText(getString(R.string.conversion_summary, decimal(value), code));
                    updated.setText(getString(R.string.conversion_rate, code, rateMoney(rate), time(System.currentTimeMillis())));
                    quotes.put(code + "BRL", currency);
                    renderQuotes();
                    saveHistory(code, value, total);
                } catch (NumberFormatException | NullPointerException e) {
                    conversionError(getString(R.string.conversion_invalid_rate));
                }
            }
            @Override public void onFailure(Call<Map<String, Currency>> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                setLoading(false);
                conversionError(getString(R.string.conversion_offline));
            }
        });
    }
    private void conversionError(String message) {
        result.setText(getString(R.string.ui_r));
        hint.setText(message);
        updated.setText(getString(R.string.retry_conversion));
    }
    private void fetchQuotes() {
        if (quoteCall != null) quoteCall.cancel();
        marketStatus.setText(getString(R.string.loading_quotes));
        findViewById(R.id.refreshQuotes).setEnabled(false);
        quoteCall = api.getExchangeRate("USD-BRL,EUR-BRL,GBP-BRL,JPY-BRL,BTC-BRL");
        quoteCall.enqueue(new Callback<Map<String, Currency>>() {
            @Override public void onResponse(Call<Map<String, Currency>> call, Response<Map<String, Currency>> response) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                findViewById(R.id.refreshQuotes).setEnabled(true);
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    quotes.clear(); quotes.putAll(response.body()); quoteTime = System.currentTimeMillis();
                    marketStatus.setText(getString(R.string.quotes_summary, time(quoteTime))); renderQuotes();
                } else marketStatus.setText(getString(R.string.quotes_unavailable));
            }
            @Override public void onFailure(Call<Map<String, Currency>> call, Throwable error) {
                if (call.isCanceled() || isFinishing() || isDestroyed()) return;
                findViewById(R.id.refreshQuotes).setEnabled(true);
                marketStatus.setText(quotes.isEmpty() ? getString(R.string.quotes_offline) : getString(R.string.quotes_cached, time(quoteTime)));
            }
        });
    }
    private void renderQuotes() {
        quoteList.removeAllViews();
        for (int i = 0; i < CODES.length; i++) {
            final int index = i;
            Currency currency = quotes.get(CODES[i] + "BRL");
            String price = "—";
            try { if (currency != null && new BigDecimal(currency.bid).signum() > 0) price = rateMoney(new BigDecimal(currency.bid)); }
            catch (RuntimeException ignored) { }
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(16), dp(16), dp(16), dp(16));
            row.setBackgroundResource(R.drawable.panel_surface);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2); rowParams.bottomMargin = dp(10);
            quoteList.addView(row, rowParams);
            TextView symbol = text(SYMBOLS[i], 18, foreground);
            symbol.setGravity(Gravity.CENTER); symbol.setBackgroundResource(R.drawable.circle_surface);
            row.addView(symbol, new LinearLayout.LayoutParams(dp(44), dp(44)));
            LinearLayout name = new LinearLayout(this); name.setOrientation(LinearLayout.VERTICAL); name.setPadding(dp(12), 0, dp(8), 0);
            name.addView(text(CODES[i], 16, foreground)); name.addView(text(NAMES[i], 11, muted));
            row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
            TextView priceView = text(price, 16, foreground);
            priceView.setTypeface(null, Typeface.BOLD);
            row.addView(priceView);
            row.setFocusable(true); row.setClickable(true);
            row.setContentDescription(getString(R.string.quote_accessibility, NAMES[i], price));
            row.setOnClickListener(v -> selectCurrency(index));
        }
    }
    private void saveHistory(String code, BigDecimal value, BigDecimal total) {
        try {
            JSONObject entry = new JSONObject(); entry.put("code", code); entry.put("value", value.toPlainString());
            entry.put("total", total.toPlainString()); entry.put("time", System.currentTimeMillis());
            JSONArray next = new JSONArray(); next.put(entry);
            for (int i = 0; i < Math.min(history.length(), 19); i++) next.put(history.get(i));
            history = next; getPreferences(MODE_PRIVATE).edit().putString("history", history.toString()).apply();
            if (tab == 2) renderHistory();
        } catch (JSONException ignored) { }
    }
    private void clearHistory() {
        history = new JSONArray();
        getPreferences(MODE_PRIVATE).edit().remove("history").apply();
        renderHistory();
        com.google.android.material.snackbar.Snackbar.make(
            findViewById(R.id.root), R.string.history_cleared,
            com.google.android.material.snackbar.Snackbar.LENGTH_SHORT).show();
    }
    private void renderHistory() {
        findViewById(R.id.clearHistory).setVisibility(history.length() == 0 ? View.GONE : View.VISIBLE);
        historyList.removeAllViews();
        if (history.length() == 0) {
            TextView empty = text(getString(R.string.empty_history), 16, muted);
            empty.setLineSpacing(dp(6), 1f); historyList.addView(empty);
            Button start = (Button) getLayoutInflater().inflate(R.layout.history_empty_action, historyList, false);
            start.setOnClickListener(v -> showTab(0)); historyList.addView(start); return;
        }
        for (int i = 0; i < history.length(); i++) {
            try {
                JSONObject item = history.getJSONObject(i);
                LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.VERTICAL);
                row.setBackgroundResource(R.drawable.panel_surface); row.setPadding(dp(20), dp(18), dp(20), dp(18));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.bottomMargin = dp(12);
                row.addView(text(getString(R.string.history_pair, decimal(new BigDecimal(item.getString("value"))), item.getString("code")), 14, muted));
                row.addView(text(money(new BigDecimal(item.getString("total"))), 26, foreground));
                row.addView(text(new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", PT).format(new Date(item.getLong("time"))), 12, muted));
                historyList.addView(row, params);
            } catch (JSONException | NumberFormatException ignored) { }
        }
    }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value); view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private String money(BigDecimal value) { return NumberFormat.getCurrencyInstance(PT).format(value); }
    private String rateMoney(BigDecimal value) { NumberFormat format = NumberFormat.getCurrencyInstance(PT); format.setMaximumFractionDigits(4); return format.format(value); }
    private String decimal(BigDecimal value) { NumberFormat format = NumberFormat.getNumberInstance(PT); format.setMaximumFractionDigits(8); return format.format(value); }
    private String time(long value) { return new SimpleDateFormat("HH:mm", PT).format(new Date(value)); }
    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state); state.putInt("selected", selected); state.putInt("tab", tab);
        state.putString("amount", amount.getText().toString()); state.putString("result", result.getText().toString());
        state.putString("hint", loading ? getString(R.string.retry_restored) : hint.getText().toString());
        state.putString("updated", loading ? getString(R.string.ui_cotacao_comercial_taxas_nao_incluidas) : updated.getText().toString());
    }
    @Override protected void onResume() {
        super.onResume();
        darkVeil.setRunning(true);
    }
    @Override protected void onPause() {
        darkVeil.setRunning(false);
        super.onPause();
    }
    @Override protected void onStop() {
        currencyPicker.dismiss();
        super.onStop();
    }
    @Override protected void onDestroy() {
        if (quoteCall != null) quoteCall.cancel(); if (conversionCall != null) conversionCall.cancel(); super.onDestroy();
    }
}
