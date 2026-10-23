package lk.sbg.tikevent.fragment;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.*;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.util.ThemeManager;

public class ReportFragment extends Fragment {

    private BarChart barChart;
    private LineChart lineChart;
    private FirebaseFirestore db;
    private LinearLayout tableContainer;
    private MaterialButtonToggleGroup toggleGroup;
    private TextView tvTotalRevenue, tvTotalTickets;

    private AutoCompleteTextView autoEvent, autoTicketType;

    private String selectedEvent = "All Events";
    private String selectedType = "All Tickets";

    private View btnExportPdf;

    private TreeMap<String, Float> lastRevenueMap = new TreeMap<>();
    private TreeMap<String, Float> lastSalesMap = new TreeMap<>();
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (autoEvent != null) outState.putString("savedEvent", autoEvent.getText().toString());
        if (autoTicketType != null)
            outState.putString("savedType", autoTicketType.getText().toString());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            selectedEvent = savedInstanceState.getString("savedEvent", "All Events");
            selectedType = savedInstanceState.getString("savedType", "All Tickets");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);

        barChart = view.findViewById(R.id.bar_chart);
        lineChart = view.findViewById(R.id.line_chart);
        toggleGroup = view.findViewById(R.id.period_toggle_group);
        tableContainer = view.findViewById(R.id.table_container);
        tvTotalRevenue = view.findViewById(R.id.tv_total_revenue);
        tvTotalTickets = view.findViewById(R.id.tv_total_tickets);
        btnExportPdf = view.findViewById(R.id.btn_export_pdf);

        autoEvent = view.findViewById(R.id.auto_event);
        autoTicketType = view.findViewById(R.id.auto_ticket_type);

        db = FirebaseFirestore.getInstance();

        setupDropdowns();

        toggleGroup.check(R.id.btn_daily);
        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) loadChartData();
        });

        if (autoEvent != null) {
            autoEvent.setOnItemClickListener((parent, view1, position, id) -> loadChartData());
        }
        if (autoTicketType != null) {
            autoTicketType.setOnItemClickListener((parent, view1, position, id) -> loadChartData());
        }

        loadChartData();
        view.findViewById(R.id.btn_export_pdf).setOnClickListener(v -> exportToPDF());
        return view;
    }

    private void setupDropdowns() {
        db.collection("events").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> events = new ArrayList<>();
            events.add("All Events");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("eventName");
                if (name != null) events.add(name);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, events) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setTextSize(16f);
                    view.setPadding(20, 10, 20, 10);
                    return view;
                }
            };
            autoEvent.setAdapter(adapter);
            autoEvent.setText(selectedEvent, false);
            loadChartData();
        });

        db.collection("ticket_types").get().addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> types = new ArrayList<>();
            types.add("All Tickets");
            types.add("Standard");
            types.add("VIP");
            types.add("VVIP");
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                String name = doc.getString("name");
                if (name != null) types.add(name);
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, types) {
                @NonNull
                @Override
                public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                    TextView view = (TextView) super.getView(position, convertView, parent);
                    view.setTextSize(16f);
                    view.setPadding(20, 10, 20, 10);
                    return view;
                }
            };
            autoTicketType.setAdapter(adapter);
            autoTicketType.setText(selectedType, false);
            loadChartData();
        });
    }

    private void loadChartData() {
        int checkedId = toggleGroup.getCheckedButtonId();
        String selectedEvent = autoEvent.getText().toString();
        String selectedType = autoTicketType.getText().toString();

        db.collection("tickets_purchased").get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                TreeMap<String, Float> revenueMap = new TreeMap<>();
                TreeMap<String, Float> salesMap = new TreeMap<>();
                float totalRevenue = 0f;
                HashSet<String> uniqueTicketIds = new HashSet<>();
                Calendar now = Calendar.getInstance();
                boolean hasData = false;

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String eventName = doc.getString("eventName");
                    String ticketType = doc.getString("ticketType");

                    boolean eventMatch = selectedEvent.equals("All Events") || selectedEvent.equals(eventName);
                    boolean typeMatch = selectedType.equals("All Tickets") || selectedType.equals(ticketType);

                    if (!eventMatch || !typeMatch) continue;

                    String purchaseAt = doc.getString("purchaseAt");
                    if (purchaseAt == null) continue;

                    try {
                        Date date = sdf.parse(purchaseAt.split(" ")[0]);
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(date);

                        boolean isMatch = false;
                        if (checkedId == R.id.btn_daily) isMatch = isSameDay(now, cal);
                        else if (checkedId == R.id.btn_weekly) isMatch = isSameWeek(now, cal);
                        else if (checkedId == R.id.btn_monthly) isMatch = isSameMonth(now, cal);
                        else if (checkedId == R.id.btn_yearly) isMatch = isSameYear(now, cal);

                        if (isMatch) {
                            String key = purchaseAt.split(" ")[0];
                            Double priceDouble = doc.getDouble("totalPrice");
                            float price = (priceDouble != null) ? priceDouble.floatValue() : 0f;
                            revenueMap.put(key, revenueMap.getOrDefault(key, 0f) + price);
                            salesMap.put(key, salesMap.getOrDefault(key, 0f) + 1f);
                            totalRevenue += price;
                            String ticketId = doc.getString("ticketId");
                            if (ticketId != null) uniqueTicketIds.add(ticketId);
                            hasData = true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                tableContainer.removeAllViews();

                if (tvTotalRevenue != null && tvTotalTickets != null) {
                    tvTotalRevenue.setText("Total Revenue: Rs. " + String.format("%,.0f", totalRevenue));
                    tvTotalTickets.setText("Total tickets sold: " + uniqueTicketIds.size());
                }

                if (!hasData) {
                    barChart.clear();
                    lineChart.clear();
                    lastRevenueMap.clear();
                    lastSalesMap.clear();
                    btnExportPdf.setVisibility(View.GONE);

                } else {
                    btnExportPdf.setVisibility(View.VISIBLE);
                    addHeaderRow();
                    updateCharts(revenueMap, salesMap);

                    lastRevenueMap.clear();
                    lastSalesMap.clear();
                    lastRevenueMap.putAll(revenueMap);
                    lastSalesMap.putAll(salesMap);

                    for (Map.Entry<String, Float> entry : revenueMap.entrySet()) {
                        addTableRow(entry.getKey(),
                                String.valueOf((int) (float) salesMap.getOrDefault(entry.getKey(), 0f)),
                                "Rs. " + String.format("%,.0f", entry.getValue()));
                    }
                }
            }
        });
    }

    private void addTableRow(String date, String sold, String revenue) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 24, 0, 24);
        row.addView(createTextView(date, 1.2f, Gravity.START));
        row.addView(createTextView(sold, 0.8f, Gravity.CENTER));
        row.addView(createTextView(revenue, 1.2f, Gravity.END));
        tableContainer.addView(row);

        View divider = new View(getContext());
        divider.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.LTGRAY);
        tableContainer.addView(divider);
    }

    private TextView createTextView(String text, float weight, int gravity) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        tv.setText(text);
        tv.setGravity(gravity);
        tv.setTextColor(ThemeManager.isDarkMode(getContext()) ? Color.WHITE : Color.BLACK);
        tv.setTextSize(13f);
        return tv;
    }

    private void addHeaderRow() {
        LinearLayout headerRow = new LinearLayout(getContext());
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setPadding(0, 24, 0, 24);
        headerRow.setBackgroundColor(Color.LTGRAY);

        headerRow.addView(createHeaderTextView("Date", 1.2f));
        headerRow.addView(createHeaderTextView("Sold", 0.8f));
        headerRow.addView(createHeaderTextView("Revenue", 1.2f));

        tableContainer.addView(headerRow);
    }

    private TextView createHeaderTextView(String text, float weight) {
        TextView tv = new TextView(getContext());
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight));
        tv.setText(text);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setGravity(Gravity.CENTER);
        tv.setTextSize(14f);
        tv.setTextColor(Color.BLACK);
        return tv;
    }

    private boolean isSameDay(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isSameWeek(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.WEEK_OF_YEAR) == c2.get(Calendar.WEEK_OF_YEAR);
    }

    private boolean isSameMonth(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.MONTH) == c2.get(Calendar.MONTH);
    }

    private boolean isSameYear(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR);
    }

    private void updateCharts(TreeMap<String, Float> revenueMap, TreeMap<String, Float> salesMap) {
        ArrayList<BarEntry> bEntries = new ArrayList<>();
        ArrayList<Entry> lEntries = new ArrayList<>();
        ArrayList<String> dates = new ArrayList<>(revenueMap.keySet());
        for (int i = 0; i < dates.size(); i++) {
            String date = dates.get(i);
            bEntries.add(new BarEntry(i, revenueMap.get(date)));
            lEntries.add(new Entry(i, salesMap.get(date)));
        }
        int textColor = ThemeManager.isDarkMode(getContext()) ? Color.WHITE : Color.BLACK;
        BarDataSet bSet = new BarDataSet(bEntries, "Revenue (LKR)");
        bSet.setValueTextColor(textColor);
        barChart.setData(new BarData(bSet));
        LineDataSet lSet = new LineDataSet(lEntries, "Tickets Sold");
        lSet.setDrawValues(true);
        lSet.setValueTextColor(textColor);
        lSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        lSet.setDrawFilled(true);
        lSet.setLineWidth(2f);
        lineChart.setData(new LineData(lSet));
        configureChart(barChart, textColor, dates);
        configureChart(lineChart, textColor, dates);
    }

    private void configureChart(com.github.mikephil.charting.charts.BarLineChartBase<?> chart, int textColor, ArrayList<String> dates) {
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDragEnabled(true);
        chart.setVisibleXRangeMaximum(7f);
        chart.moveViewToX(dates.size());
        chart.getDescription().setEnabled(false);
        chart.getAxisLeft().setTextColor(textColor);
        chart.getAxisLeft().setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        if (chart instanceof LineChart) {
            chart.getAxisLeft().setGranularity(5f);
            chart.getAxisLeft().setGranularityEnabled(true);
        } else if (chart instanceof BarChart) {
            chart.getAxisLeft().setGranularity(400f);
            chart.getAxisLeft().setGranularityEnabled(true);
        }
        chart.getAxisLeft().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(textColor);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int idx = (int) value;
                return (idx >= 0 && idx < dates.size()) ? dates.get(idx).substring(5) : "";
            }
        });
        if (chart instanceof LineChart) chart.animateX(2000);
        else if (chart instanceof BarChart) chart.animateXY(1500, 1500);
        chart.invalidate();
    }

    private void exportToPDF() {
        try {
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
            if (!path.exists()) path.mkdirs();

            String fileName = "Ticket_Sales_Report_" + System.currentTimeMillis() + ".pdf";
            File file = new File(path, fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(file));
            Document document = new Document(new PdfDocument(writer));

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normalFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            com.itextpdf.kernel.colors.DeviceRgb black = new com.itextpdf.kernel.colors.DeviceRgb(0, 0, 0);
            com.itextpdf.kernel.colors.DeviceRgb headerColor = new com.itextpdf.kernel.colors.DeviceRgb(220, 220, 220); // Light Gray

            Paragraph title = new Paragraph("Ticket Sales Report")
                    .setFontSize(26)
                    .setFont(boldFont)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(title);

            Paragraph line = new Paragraph("________________________________________________")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER);
            document.add(line);
            document.add(new Paragraph("\n"));

            document.add(new Paragraph("Event: " + autoEvent.getText().toString()).setFont(boldFont));
            document.add(new Paragraph("Ticket Type: " + autoTicketType.getText().toString()).setFont(boldFont));
            document.add(new Paragraph("Period: " + getSelectedPeriodText()).setFont(boldFont).setMarginBottom(20));

            document.add(new Paragraph("Total Revenue: " + tvTotalRevenue.getText().toString().replace("Total Revenue: ", ""))
                    .setFontSize(20).setFont(boldFont).setFontColor(black));

            document.add(new Paragraph(tvTotalTickets.getText().toString())
                    .setFontSize(16).setFont(normalFont).setFontColor(black).setMarginBottom(20));

            document.add(new Paragraph("Ticket Sale Performance").setFontSize(14).setFont(boldFont).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            addImageToPdf(document, lineChart);

            document.add(new Paragraph("\nRevenue Trends").setFontSize(14).setFont(boldFont).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            addImageToPdf(document, barChart);

            document.add(new Paragraph("\nSummary Table:").setFont(boldFont));
            Table table = new Table(new float[]{2, 1, 2}).setWidth(UnitValue.createPercentValue(100));

            table.addHeaderCell(new Cell().add(new Paragraph("Date").setFont(boldFont)).setBackgroundColor(headerColor).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("Sold").setFont(boldFont)).setBackgroundColor(headerColor).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
            table.addHeaderCell(new Cell().add(new Paragraph("Revenue").setFont(boldFont)).setBackgroundColor(headerColor).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            for (Map.Entry<String, Float> entry : lastRevenueMap.entrySet()) {
                table.addCell(new Cell().add(new Paragraph(entry.getKey())).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph(String.valueOf((int) (float) lastSalesMap.getOrDefault(entry.getKey(), 0f)))).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));
                table.addCell(new Cell().add(new Paragraph("Rs. " + String.format("%,.0f", entry.getValue()))).setTextAlignment(com.itextpdf.layout.properties.TextAlignment.RIGHT));
            }
            document.add(table);

            document.close();
            showCustomDialog("success", "Success", "Your PDF Exported Successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showCustomDialog(String type, String title, String message) {
        if (getContext() == null) return;

        final Dialog dialog = new Dialog(getContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.custom_dialog);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setDimAmount(0.6f);
            WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
            lp.copyFrom(dialog.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            lp.gravity = Gravity.CENTER;
            dialog.getWindow().setAttributes(lp);
        }

        FrameLayout iconContainer = dialog.findViewById(R.id.dialogIconContainer);
        TextView iconText = dialog.findViewById(R.id.dialogIconText);
        TextView tvTitle = dialog.findViewById(R.id.customDialogTitle);
        TextView tvMessage = dialog.findViewById(R.id.customDialogText);
        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);

        if (tvTitle != null) tvTitle.setText(title);
        if (tvMessage != null) tvMessage.setText(message);


        if (type.equals("success")) {
            if (iconContainer != null)
                iconContainer.setBackgroundResource(R.drawable.dialog_icon_bg_success);
            if (iconText != null) iconText.setText("✓");
            if (btnCancel != null) btnCancel.setVisibility(View.GONE);
            if (btnOk != null) {
                btnOk.setText("Okay");
                btnOk.setOnClickListener(v -> dialog.dismiss());
            }
        }

        dialog.show();
    }

    private void addImageToPdf(Document doc, com.github.mikephil.charting.charts.BarLineChartBase<?> chart) throws Exception {

        int originalTextColor = ThemeManager.isDarkMode(getContext()) ? Color.WHITE : Color.BLACK;

        chart.getXAxis().setTextColor(Color.BLACK);
        chart.getAxisLeft().setTextColor(Color.BLACK);
        if (chart.getLegend() != null) chart.getLegend().setTextColor(Color.BLACK);
        chart.getData().setValueTextColor(Color.BLACK);
        chart.invalidate();

        Bitmap bitmap = Bitmap.createBitmap(chart.getWidth(), chart.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(Color.WHITE);
        chart.draw(canvas);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        Image img = new Image(ImageDataFactory.create(stream.toByteArray()));

        float documentWidth = doc.getPdfDocument().getDefaultPageSize().getWidth() - doc.getLeftMargin() - doc.getRightMargin();
        img.setWidth(documentWidth);
        img.setAutoScale(true);
        img.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
        doc.add(img);

        chart.getXAxis().setTextColor(originalTextColor);
        chart.getAxisLeft().setTextColor(originalTextColor);
        if (chart.getLegend() != null) chart.getLegend().setTextColor(originalTextColor);
        chart.getData().setValueTextColor(originalTextColor);
        chart.invalidate();
    }

    private String getSelectedPeriodText() {
        int id = toggleGroup.getCheckedButtonId();
        if (id == R.id.btn_daily) return "Daily";
        if (id == R.id.btn_weekly) return "Weekly";
        if (id == R.id.btn_monthly) return "Monthly";
        return "Yearly";
    }
}