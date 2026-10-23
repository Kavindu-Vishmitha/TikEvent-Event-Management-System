package lk.sbg.tikevent.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;
import lk.sbg.tikevent.model.Event;
import lk.sbg.tikevent.model.PurchasedTicket;

public class AdminDashboardFragment extends Fragment {

    private TextView tvTotalEvents, tvTicketsSold, tvRevenue, tvTotalBuyers;
    private FirebaseFirestore db;

    public AdminDashboardFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvTotalEvents = view.findViewById(R.id.tv_total_events_count);
        tvTicketsSold = view.findViewById(R.id.tv_tickets_sold_count);
        tvRevenue = view.findViewById(R.id.tv_revenue_count);
        tvTotalBuyers = view.findViewById(R.id.tv_total_buyers_count);

        listenToDashboardData();
        setupNavigationClickListeners(view);
    }

    private void listenToDashboardData() {

        db.collection("events")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
                        int totalEvents = value.size();
                        tvTotalEvents.setText(String.valueOf(totalEvents));
                    }
                });

        db.collection("tickets_purchased")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }

                    if (value != null) {
                        int totalTicketsSoldToday = 0;
                        long totalRevenueToday = 0;

                        String todayDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

                        for (QueryDocumentSnapshot doc : value) {
                            PurchasedTicket ticket = doc.toObject(PurchasedTicket.class);

                            if (ticket != null) {
                                String status = ticket.getStatus();
                                String purchaseAt = ticket.getPurchaseAt();

                                if (purchaseAt != null && purchaseAt.startsWith(todayDateStr)) {
                                    if ("SUCCESS".equalsIgnoreCase(status)) {

                                        totalTicketsSoldToday++;
                                        totalRevenueToday += ticket.getTotalPrice();
                                    }
                                }
                            }
                        }

                        tvTicketsSold.setText(String.valueOf(totalTicketsSoldToday));
                        tvRevenue.setText("Rs. " + String.format(Locale.getDefault(), "%,d", totalRevenueToday));
                    }
                });

        db.collection("users")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {

                        return;
                    }

                    if (value != null) {
                        int totalBuyersCount = 0;

                        for (QueryDocumentSnapshot doc : value) {
                            String role = doc.getString("role");

                            if (role != null && "BUYER".equalsIgnoreCase(role)) {
                                totalBuyersCount++;
                            }
                        }

                        if (tvTotalBuyers != null) {
                            tvTotalBuyers.setText(String.valueOf(totalBuyersCount));
                        }
                    }
                });
    }

    private void setupNavigationClickListeners(View view) {
        View cardManageEvents = view.findViewById(R.id.card_manage_events);
        if (cardManageEvents != null) {
            cardManageEvents.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.loadFragment(new ManageEventFragment());

                    View bottomNav = mainActivity.findViewById(R.id.bottomNavigationView);
                    if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                        ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav)
                                .getMenu().findItem(R.id.bottom_nav_tickets).setChecked(true);
                    }
                }
            });
        }

        View cardCreateEvent = view.findViewById(R.id.card_create_event);
        if (cardCreateEvent != null) {
            cardCreateEvent.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.loadFragment(new CreateEventFragment());
                }
            });
        }

        View btnViewMap = view.findViewById(R.id.btn_view_map);
        if (btnViewMap != null) {
            btnViewMap.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new AllEventLocationFragment());
                }
            });
        }

        View cardReports = view.findViewById(R.id.card_reports);
        if (cardReports != null) {
            cardReports.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.loadFragment(new ReportFragment());

                    View bottomNav = mainActivity.findViewById(R.id.bottomNavigationView);
                    if (bottomNav instanceof com.google.android.material.bottomnavigation.BottomNavigationView) {
                        ((com.google.android.material.bottomnavigation.BottomNavigationView) bottomNav)
                                .getMenu().findItem(R.id.bottom_nav_favorite).setChecked(true);
                    }
                }
            });
        }

        View cardTicketManagement = view.findViewById(R.id.card_ticket_management);
        if (cardTicketManagement != null) {
            cardTicketManagement.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new TicketManagementFragment());
                }
            });
        }
    }
}