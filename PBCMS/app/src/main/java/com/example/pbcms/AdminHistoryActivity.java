package com.example.pbcms;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.TextView;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminHistoryActivity extends AppCompatActivity implements OnChartValueSelectedListener {

    private LineChart temperatureChart;
    private BarChart humidityChart;
    private TextView currentDateText, tempValueText, humidityValueText;
    private TextView dayBtn, weekBtn, monthBtn, yearBtn;
    private TextView prevDate, nextDate;
    private ImageButton backButton;
    private LinearLayout doorHistoryContainer;

    private Calendar currentDate;
    private String currentRange = "day"; // Default to day view

    private DatabaseReference databaseReference;
    private SimpleDateFormat firebaseDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault());


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_history);

        // Initialize views
        temperatureChart = findViewById(R.id.temperatureChart);
        humidityChart = findViewById(R.id.humidityChart);
        currentDateText = findViewById(R.id.currentDate);
        tempValueText = findViewById(R.id.tempValueText);
        humidityValueText = findViewById(R.id.humidityValueText);
        dayBtn = findViewById(R.id.dayBtn);
        weekBtn = findViewById(R.id.weekBtn);
        monthBtn = findViewById(R.id.monthBtn);
        yearBtn = findViewById(R.id.yearBtn);
        prevDate = findViewById(R.id.prevDate);
        nextDate = findViewById(R.id.nextDate);
        backButton = findViewById(R.id.backButton);

        // Initialize Firebase
        databaseReference = FirebaseDatabase.getInstance().getReference("sensors/history");

        // Set current date to today
        currentDate = Calendar.getInstance();

        // Set initial time range to day
        setTimeRange("day");

        // Setup charts
        setupTemperatureChart();
        setupHumidityChart();

        // Set click listeners
        dayBtn.setOnClickListener(v -> setTimeRange("day"));
        weekBtn.setOnClickListener(v -> setTimeRange("week"));
        monthBtn.setOnClickListener(v -> setTimeRange("month"));
        yearBtn.setOnClickListener(v -> setTimeRange("year"));

        prevDate.setOnClickListener(v -> navigateDate(-1));
        nextDate.setOnClickListener(v -> navigateDate(1));

        backButton.setOnClickListener(v -> finish());

        // Load initial data
        reloadData();
        doorHistoryContainer = findViewById(R.id.doorHistoryContainer);
    }

    private void reloadData() {
        loadSensorData();
        loadDoorHistory();
    }

    private void setupTemperatureChart() {
        temperatureChart.setOnChartValueSelectedListener(this);
        temperatureChart.setDrawGridBackground(false);
        temperatureChart.getDescription().setEnabled(false);
        temperatureChart.setTouchEnabled(true);
        temperatureChart.setDragEnabled(false);
        temperatureChart.setScaleEnabled(false);
        temperatureChart.setPinchZoom(false);

        XAxis xAxis = temperatureChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = temperatureChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(40f);
        leftAxis.setGranularity(5f);

        temperatureChart.getAxisRight().setEnabled(false);
        Legend legend = temperatureChart.getLegend();
        legend.setEnabled(false);
        temperatureChart.setExtraOffsets(0, 0, 0, 0);
    }

    private void setupHumidityChart() {
        humidityChart.setOnChartValueSelectedListener(this);
        humidityChart.getDescription().setEnabled(false);
        humidityChart.setTouchEnabled(true);
        humidityChart.setDragEnabled(false);
        humidityChart.setScaleEnabled(false);
        humidityChart.setPinchZoom(false);

        XAxis xAxis = humidityChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis leftAxis = humidityChart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(100f);
        leftAxis.setGranularity(10f);

        humidityChart.getAxisRight().setEnabled(false);
        Legend legend = humidityChart.getLegend();
        legend.setEnabled(false);

        humidityChart.setDrawBarShadow(false);
        humidityChart.setDrawValueAboveBar(false);
        humidityChart.setExtraOffsets(0, 0, 0, 0);
    }

    private void setTimeRange(String range) {
        currentRange = range;

        int activeColor = Color.parseColor("#5896B8");
        int inactiveColor = Color.parseColor("#212121");

        dayBtn.setBackgroundColor(range.equals("day") ? activeColor : Color.TRANSPARENT);
        dayBtn.setTextColor(range.equals("day") ? Color.WHITE : inactiveColor);

        weekBtn.setBackgroundColor(range.equals("week") ? activeColor : Color.TRANSPARENT);
        weekBtn.setTextColor(range.equals("week") ? Color.WHITE : inactiveColor);

        monthBtn.setBackgroundColor(range.equals("month") ? activeColor : Color.TRANSPARENT);
        monthBtn.setTextColor(range.equals("month") ? Color.WHITE : inactiveColor);

        yearBtn.setBackgroundColor(range.equals("year") ? activeColor : Color.TRANSPARENT);
        yearBtn.setTextColor(range.equals("year") ? Color.WHITE : inactiveColor);

        updateDateText();
        reloadData();
    }

    private void navigateDate(int direction) {
        switch (currentRange) {
            case "day":
                currentDate.add(Calendar.DAY_OF_MONTH, direction);
                break;
            case "week":
                currentDate.add(Calendar.WEEK_OF_YEAR, direction);
                break;
            case "month":
                currentDate.add(Calendar.MONTH, direction);
                break;
            case "year":
                currentDate.add(Calendar.YEAR, direction);
                break;
        }
        updateDateText();
        reloadData();
    }

    private void configureXAxis(XAxis xAxis) {
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(getXAxisFormatter());

        switch (currentRange) {
            case "day":
                // 24 hours, but chart only ~250dp tall, so we show 6 labels (every 4 hours)
                xAxis.setLabelCount(6, false);
                break;
            case "week":
                xAxis.setLabelCount(7, false);
                break;
            case "month":
                // Show 4 weeks max (W1, W2, W3, W4)
                xAxis.setLabelCount(4, false);
                break;
            case "year":
                // 12 months, all visible
                xAxis.setLabelCount(12, false);
                break;
        }
    }

    private ValueFormatter getXAxisFormatter() {
        switch (currentRange) {
            case "day":
                return new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.format(Locale.getDefault(), "%02d:00", (int) value);
                    }
                };
            case "week":
                return new ValueFormatter() {
                    private final String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value % 7;
                        return days[index];
                    }
                };
            case "month":
                return new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return "W" + ((int) value + 1); // W1, W2, etc.
                    }
                };
            case "year":
                return new ValueFormatter() {
                    private final String[] months = {
                            "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
                    };

                    @Override
                    public String getFormattedValue(float value) {
                        int index = (int) value % 12;
                        return months[index];
                    }
                };
            default:
                return new ValueFormatter() {
                    @Override
                    public String getFormattedValue(float value) {
                        return String.valueOf((int) value);
                    }
                };
        }
    }

    private void updateDateText() {
        SimpleDateFormat format;
        switch (currentRange) {
            case "day":
                format = new SimpleDateFormat("EEE dd, MMM yyyy", Locale.getDefault());
                currentDateText.setText(format.format(currentDate.getTime()));
                break;
            case "week":
                Calendar startOfWeek = (Calendar) currentDate.clone();
                startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
                SimpleDateFormat weekFormat = new SimpleDateFormat("MMM dd", Locale.getDefault());
                currentDateText.setText(weekFormat.format(startOfWeek.getTime()) + " - " +
                        weekFormat.format(endOfWeek.getTime()) + ", " +
                        new SimpleDateFormat("yyyy", Locale.getDefault()).format(currentDate.getTime()));
                break;
            case "month":
                format = new SimpleDateFormat("MMMM yyyy", Locale.getDefault());
                currentDateText.setText(format.format(currentDate.getTime()));
                break;
            case "year":
                format = new SimpleDateFormat("yyyy", Locale.getDefault());
                currentDateText.setText(format.format(currentDate.getTime()));
                break;
        }
    }

    private void loadSensorData() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Entry> tempEntries = new ArrayList<>();
                List<BarEntry> humidityEntries = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String key = data.getKey(); // e.g., "2025-09-20_10-00"
                    try {
                        Date date = firebaseDateFormat.parse(key);
                        if (date == null) continue;

                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        // Filter by current selected range
                        if (!isInCurrentRange(cal)) continue;

                        float xValue = getXValueForRange(cal);
                        Float temp = data.child("temperature").getValue(Float.class);
                        Float hum = data.child("humidity").getValue(Float.class);

                        // Only add if values exist
                        if (temp != null) tempEntries.add(new Entry(xValue, temp));
                        if (hum != null) humidityEntries.add(new BarEntry(xValue, hum));

                    } catch (ParseException e) {
                        Log.e("FirebaseParse", "Date parse error: " + key, e);
                    }
                }

                updateCharts(tempEntries, humidityEntries);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }

    private boolean isInCurrentRange(Calendar cal) {
        switch (currentRange) {
            case "day":
                return cal.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH) &&
                        cal.get(Calendar.DAY_OF_MONTH) == currentDate.get(Calendar.DAY_OF_MONTH);

            case "week":
                Calendar startOfWeek = (Calendar) currentDate.clone();
                startOfWeek.set(Calendar.DAY_OF_WEEK, startOfWeek.getFirstDayOfWeek());
                Calendar endOfWeek = (Calendar) startOfWeek.clone();
                endOfWeek.add(Calendar.DAY_OF_WEEK, 6);
                return !cal.before(startOfWeek) && !cal.after(endOfWeek);

            case "month":
                return cal.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH);

            case "year":
                return cal.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR);

            default:
                return false;
        }
    }

    private float getXValueForRange(Calendar cal) {
        switch (currentRange) {
            case "day":
                return cal.get(Calendar.HOUR_OF_DAY);
            case "week":
                return cal.get(Calendar.DAY_OF_WEEK) - 1;
            case "month":
                return cal.get(Calendar.WEEK_OF_MONTH) - 1;
            case "year":
                return cal.get(Calendar.MONTH);
            default:
                return 0;
        }
    }

    private void updateCharts(List<Entry> tempEntries, List<BarEntry> humidityEntries) {
        // Temperature dataset
        LineDataSet tempDataSet = new LineDataSet(tempEntries, "Temperature");
        tempDataSet.setColor(Color.parseColor("#FF5722"));
        tempDataSet.setLineWidth(2f);
        tempDataSet.setDrawCircles(false);
        tempDataSet.setDrawValues(false);
        tempDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        tempDataSet.setDrawFilled(true);
        tempDataSet.setFillColor(Color.parseColor("#22FF5722"));
        tempDataSet.setHighlightEnabled(true);

        temperatureChart.setData(new LineData(tempDataSet));
        configureXAxis(temperatureChart.getXAxis());
        temperatureChart.invalidate();

        // Humidity dataset
        BarDataSet humidityDataSet = new BarDataSet(humidityEntries, "Humidity");
        humidityDataSet.setColor(Color.parseColor("#03A9F4"));
        humidityDataSet.setDrawValues(false);
        humidityDataSet.setHighlightEnabled(true);

        BarData humidityData = new BarData(humidityDataSet);
        humidityChart.setData(humidityData);
        configureXAxis(humidityChart.getXAxis());
        humidityChart.invalidate();

        // Update latest values
        if (!tempEntries.isEmpty()) {
            Entry lastTemp = tempEntries.get(tempEntries.size() - 1);
            tempValueText.setText(String.format(Locale.getDefault(),
                    "Latest Temperature: %.1f°C", lastTemp.getY()));
        }

        if (!humidityEntries.isEmpty()) {
            BarEntry lastHum = humidityEntries.get(humidityEntries.size() - 1);
            humidityValueText.setText(String.format(Locale.getDefault(),
                    "Latest Humidity: %.1f%%", lastHum.getY()));
        }
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {
        String label = "";
        switch (currentRange) {
            case "day":
                label = String.format(Locale.getDefault(), "%02d:00", (int) e.getX());
                break;
            case "week":
                String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                label = days[(int) e.getX() % 7];
                break;
            case "month":
                label = "Week " + ((int) e.getX() + 1);
                break;
            case "year":
                String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                label = months[(int) e.getX()];
                break;
        }

        if (e instanceof BarEntry) {
            humidityValueText.setText(String.format(Locale.getDefault(),
                    "%s Humidity: %.1f%%", label, e.getY()));
        } else {
            tempValueText.setText(String.format(Locale.getDefault(),
                    "%s Temperature: %.1f°C", label, e.getY()));
        }
    }

    @Override
    public void onNothingSelected() {
    }

    private void loadDoorHistory() {
        DatabaseReference doorRef = FirebaseDatabase.getInstance().getReference("sensors/history");

        doorRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                doorHistoryContainer.removeAllViews(); // Clear previous items
                List<String[]> doorEvents = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    String timestamp = data.getKey(); // "YYYY-MM-DD HH:mm"
                    String doorStatus = data.child("door_status").getValue(String.class);
                    if (doorStatus != null && timestamp != null) {
                        try {
                            Date date = firebaseDateFormat.parse(timestamp);
                            if (date == null) continue;

                            Calendar cal = Calendar.getInstance();
                            cal.setTime(date);

                            // Filter by current selected range
                            if (!isInCurrentRange(cal)) continue;

                            doorEvents.add(new String[]{timestamp, doorStatus});

                        } catch (ParseException e) {
                            Log.e("DoorHistory", "Date parse error: " + timestamp, e);
                        }
                    }
                }
                // Sort latest first (descending)
                doorEvents.sort((a, b) -> b[0].compareTo(a[0]));

                // If no events, show "No record.."
                if (doorEvents.isEmpty()) {
                    TextView noRecordText = new TextView(AdminHistoryActivity.this);
                    noRecordText.setText("No record in this time.");
                    noRecordText.setTextColor(Color.parseColor("#757575"));
                    noRecordText.setTextSize(14);
                    noRecordText.setGravity(Gravity.CENTER);
                    doorHistoryContainer.addView(noRecordText);
                    return;
                }

                // Add sorted events to UI
                for (String[] event : doorEvents) {
                    addDoorHistoryItem(event[0], event[1]);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });
    }
    private void addDoorHistoryItem(String timestamp, String doorStatus) {
        // Parent horizontal layout
        LinearLayout itemLayout = new LinearLayout(this);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setPadding(12, 12, 12, 12);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 12);
        itemLayout.setLayoutParams(params);
        itemLayout.setBackgroundResource(R.drawable.bg_door_card_light);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);

        // Icon
        ImageView icon = new ImageView(this);
        int sizeInDp = 24;
        float scale = getResources().getDisplayMetrics().density;
        int sizeInPx = (int) (sizeInDp * scale + 0.5f);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(sizeInPx, sizeInPx);

        int marginRightDp = 12;
        int marginRightPx = (int) (marginRightDp * scale + 0.5f);
        iconParams.setMargins(0, 0, marginRightPx, 0);

        icon.setLayoutParams(iconParams);

        if (doorStatus.equalsIgnoreCase("OPEN")) {
            icon.setImageResource(R.drawable.ic_admin_door_open);
        } else {
            icon.setImageResource(R.drawable.ic_door_closed);
            icon.setColorFilter(Color.parseColor("#5896B8"));
        }

        // Text container
        LinearLayout textLayout = new LinearLayout(this);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        textLayout.setLayoutParams(textParams);

        // Time TextView
        TextView timeText = new TextView(this);
        timeText.setText(timestamp.split("_")[1].replace("-", ":"));
        timeText.setTextColor(Color.parseColor("#212121"));
        timeText.setTypeface(null, Typeface.BOLD);
        timeText.setTextSize(14);

        // Status TextView
        TextView statusText = new TextView(this);
        statusText.setText(doorStatus.equalsIgnoreCase("OPEN") ? "Door Opened" : "Door Closed");
        statusText.setTextColor(Color.parseColor("#757575"));
        statusText.setTextSize(13);

        textLayout.addView(timeText);
        textLayout.addView(statusText);

        itemLayout.addView(icon);
        itemLayout.addView(textLayout);

        doorHistoryContainer.addView(itemLayout);
    }
}