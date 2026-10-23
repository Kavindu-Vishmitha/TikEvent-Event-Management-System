package lk.sbg.tikevent.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.databinding.ActivityMainBinding;
import lk.sbg.tikevent.fragment.*;
import lk.sbg.tikevent.util.ThemeManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, NavigationBarView.OnItemSelectedListener {

    private boolean isRestoringFromThemeChange = false;
    private ActivityMainBinding binding;
    private DrawerLayout drawerLayout;
    private MaterialToolbar toolbar;
    private NavigationView sideNavigationView;
    private BottomNavigationView bottomNavigationView;
    private FirebaseAuth mAuth;
    private ActionBarDrawerToggle toggle;

    private Fragment activeFragment;
    private String userRole;
    private String currentEventNameForHeader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeManager.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        drawerLayout = binding.drawerLayout;
        toolbar = binding.toolbar;
        sideNavigationView = binding.sideNavigationView;
        bottomNavigationView = binding.bottomNavigationView;
        mAuth = FirebaseAuth.getInstance();

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        sideNavigationView.setNavigationItemSelectedListener(this);
        bottomNavigationView.setOnItemSelectedListener(this);

        if (savedInstanceState != null) {
            userRole = savedInstanceState.getString("USER_ROLE_KEY");
        } else {
            userRole = getIntent().getStringExtra("USER_ROLE");
        }

        if (userRole != null && (userRole.equalsIgnoreCase("Admin") || userRole.equalsIgnoreCase("Seller"))) {
            sideNavigationView.getMenu().clear();
            sideNavigationView.inflateMenu(R.menu.admin_side_nav);
            bottomNavigationView.getMenu().clear();
            bottomNavigationView.inflateMenu(R.menu.admin_bottom_nav);
        }

        updateSideNavAuthMenu();
        updateSideNavAuthMenu();

        if (savedInstanceState == null) {
            updateSideNavHeader();
            if (userRole != null && (userRole.equalsIgnoreCase("Admin") || userRole.equalsIgnoreCase("Seller"))) {
                loadFragment(new AdminDashboardFragment());
                syncSelection(R.id.side_nav_home, R.id.bottom_nav_home);
            } else {
                loadFragment(new HomeFragment());
                syncSelection(R.id.side_nav_home, R.id.bottom_nav_home);
            }
        } else {
            activeFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            String savedText = savedInstanceState.getString("SAVED_HEADER_TEXT");
            isRestoringFromThemeChange = true;

            if (activeFragment != null) {
                updateToolbarLayout(activeFragment);
                if (savedText != null && binding.toolbarUserGreeting != null) {
                    binding.toolbarUserGreeting.setText(savedText);
                }
            } else {
                setupDrawerToggle();
            }
            updateSideNavHeader();
        }

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                    if (currentFragment instanceof HomeFragment || currentFragment instanceof AdminDashboardFragment) {
                        finish();
                    } else if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                        getSupportFragmentManager().popBackStack();

                        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                            @Override
                            public void onBackStackChanged() {
                                Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                                if (current != null) {
                                    activeFragment = current;
                                    updateToolbarLayout(current);
                                    updateBottomNavSelection(current);
                                    updateSideNavSelection(current);
                                }
                                getSupportFragmentManager().removeOnBackStackChangedListener(this);
                            }
                        });
                    } else {
                        finish();
                    }
                }
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("USER_ROLE_KEY", userRole);
        if (binding != null && binding.toolbarUserGreeting != null) {
            outState.putString("SAVED_HEADER_TEXT", binding.toolbarUserGreeting.getText().toString());
        }
    }

    public void setToolbarTitle(String title) {
        if (binding != null && binding.toolbarUserGreeting != null) {
            binding.toolbarUserGreeting.setText(title);
        }
    }

    private void setupDrawerToggle() {
        toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.header_nav, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        MenuItem deleteItem = menu.findItem(R.id.action_delete);
        MenuItem notificationItem = menu.findItem(R.id.action_notifications);
        MenuItem themeItem = menu.findItem(R.id.action_theme_mode);

        if (notificationItem != null) {
            if (currentFrag instanceof NotificationFragment) {
                notificationItem.setVisible(false);
                notificationItem.setEnabled(false);
            } else {
                notificationItem.setVisible(true);
                notificationItem.setEnabled(true);
            }
        }

        if (deleteItem != null) {
            deleteItem.setVisible(false);

            if (currentFrag instanceof FavoriteFragment) {
                FavoriteFragment favFragment = (FavoriteFragment) currentFrag;
                if (favFragment.getAdapter() != null && favFragment.getAdapter().isSelectionActive()) {
                    deleteItem.setVisible(true);
                }
            } else if (currentFrag instanceof MyTicketFragment) {
                MyTicketFragment ticketFragment = (MyTicketFragment) currentFrag;
                if (ticketFragment.isSelectionActive()) {
                    deleteItem.setVisible(true);
                }
            } else if (currentFrag instanceof NotificationFragment) {
                NotificationFragment notiFragment = (NotificationFragment) currentFrag;
                if (notiFragment.isSelectionActive()) {
                    deleteItem.setVisible(true);
                }
            } else if (currentFrag instanceof ManageEventFragment) {
                ManageEventFragment manageFragment = (ManageEventFragment) currentFrag;
                if (manageFragment.getAdapter() != null && manageFragment.getAdapter().getSelectedItemsCount() > 0) {
                    deleteItem.setVisible(true);
                }
            } else if (currentFrag instanceof EventAttendanceFragment) {
                EventAttendanceFragment attendFragment = (EventAttendanceFragment) currentFrag;
                if (attendFragment.getAdapter() != null &&
                        attendFragment.getAdapter().isSelectionActive() &&
                        attendFragment.getAdapter().getSelectedItemsCount() > 0) {
                    deleteItem.setVisible(true);
                }
            }
        }
        if (themeItem != null) {
            boolean shouldShowIcon = !(
                    currentFrag instanceof EventEditFragment ||
                            currentFrag instanceof ScanFragment ||
                            currentFrag instanceof EventLocationFragment ||
                            currentFrag instanceof AllEventLocationFragment
            );

            themeItem.setVisible(shouldShowIcon);

            if (shouldShowIcon) {
                if (ThemeManager.isDarkMode(this)) {
                    themeItem.setIcon(R.drawable.dark_mode_24px);
                    themeItem.setTitle("Light Mode");
                } else {
                    themeItem.setIcon(R.drawable.moon_stars_24px);
                    themeItem.setTitle("Dark Mode");
                }
            }
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;

        int id = item.getItemId();

        if (id == R.id.action_delete) {
            Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

            if (currentFrag instanceof ManageEventFragment) {
                ((ManageEventFragment) currentFrag).confirmDeleteSelectedEvents();
                return true;
            } else if (currentFrag instanceof FavoriteFragment) {
                ((FavoriteFragment) currentFrag).deleteSelectedNotifications();
                return true;
            } else if (currentFrag instanceof NotificationFragment) {
                ((NotificationFragment) currentFrag).deleteSelectedNotifications();
                return true;
            } else if (currentFrag instanceof MyTicketFragment) {
                ((MyTicketFragment) currentFrag).confirmDeleteSelectedTickets();
                return true;
            } else if (currentFrag instanceof EventAttendanceFragment) {
                EventAttendanceFragment attendFragment = (EventAttendanceFragment) currentFrag;
                attendFragment.onBinIconClicked();
                return true;
            }
        } else if (id == R.id.action_notifications) {
            loadFragment(new NotificationFragment());
            return true;
        } else if (id == R.id.action_theme_mode) {
            ThemeManager.toggleTheme(this);
            recreate();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        boolean isAdmin = "ADMIN".equals(userRole) || "Seller".equalsIgnoreCase(userRole);

        if (id == R.id.side_nav_home || id == R.id.bottom_nav_home) {
            if (isAdmin) {
                loadFragment(new AdminDashboardFragment());
            } else {
                loadFragment(new HomeFragment());
            }
            syncSelection(R.id.side_nav_home, R.id.bottom_nav_home);
        } else if (id == R.id.side_nav_tickets || id == R.id.bottom_nav_tickets) {
            if (isAdmin) {
                loadFragment(new ManageEventFragment());
            } else {
                loadFragment(new MyTicketFragment());
            }
            syncSelection(R.id.side_nav_tickets, R.id.bottom_nav_tickets);
        } else if (id == R.id.side_nav_favorite || id == R.id.bottom_nav_favorite) {
            if (isAdmin) {
                loadFragment(new ReportFragment());
            } else {
                loadFragment(new FavoriteFragment());
            }
            syncSelection(R.id.side_nav_favorite, R.id.bottom_nav_favorite);
        } else if (id == R.id.side_nav_account || id == R.id.bottom_nav_account) {
            loadFragment(new AccountFragment());
            syncSelection(R.id.side_nav_account, R.id.bottom_nav_account);
        } else if (id == R.id.side_nav_login) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        } else if (id == R.id.side_nav_logout) {
            mAuth.signOut();

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            updateSideNavAuthMenu();
            updateSideNavHeader();

            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }

        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        return true;
    }

    private void syncSelection(int sideId, int bottomId) {
        sideNavigationView.setCheckedItem(sideId);
        bottomNavigationView.getMenu().findItem(bottomId).setChecked(true);
    }

    public void loadFragment(Fragment fragment) {
        activeFragment = fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .setTransition(androidx.fragment.app.FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                .addToBackStack(fragment instanceof HomeFragment ? null : "added")
                .commit();

        updateToolbarLayout(fragment);

        invalidateOptionsMenu();
    }

    private void updateToolbarLayout(Fragment fragment) {
        if (binding.toolbar == null) return;

        invalidateOptionsMenu();
        binding.bottomNavigationView.setVisibility(View.VISIBLE);

        if (fragment instanceof AdminDashboardFragment) {
            binding.toolbarUserGreeting.setText("Admin Dashboard");
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            }
            setupDrawerToggle();
        } else if (fragment instanceof CreateEventFragment) {
            binding.toolbarUserGreeting.setText("Create Event");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof EventLocationFragment) {
            binding.toolbar.setVisibility(View.GONE);
            binding.bottomNavigationView.setVisibility(View.GONE);

        } else if (fragment instanceof EventEditFragment) {
            binding.toolbarUserGreeting.setText("Edit Event");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof ManageEventFragment) {
            binding.toolbarUserGreeting.setText("Manage Events");
            setupBackNavigation();
        } else if (fragment instanceof HomeFragment) {
            if (mAuth.getCurrentUser() != null) {
                updateSideNavHeader();
            } else {
                binding.toolbarUserGreeting.setText("Hi User");
            }

            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            setupDrawerToggle();
        } else if (fragment instanceof FavoriteFragment) {
            binding.toolbarUserGreeting.setText("Favorite");
            setupBackNavigation();
        } else if (fragment instanceof NotificationFragment) {
            binding.toolbarUserGreeting.setText("Notifications");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof AccountFragment) {
            binding.toolbarUserGreeting.setText("Account");
            setupBackNavigation();
        } else if (fragment instanceof MyTicketFragment) {
            binding.toolbarUserGreeting.setText("My Tickets");
            setupBackNavigation();
        } else if (fragment instanceof CategoryFragment) {
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof EventSingleViewFragment) {
            binding.toolbarUserGreeting.setText("Event Details");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof ReportFragment) {
            binding.toolbarUserGreeting.setText("Reports");
            setupBackNavigation();
        } else if (fragment instanceof TicketManagementFragment) {
            binding.toolbarUserGreeting.setText("Ticket Management");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof EventAttendanceFragment) {
            if (fragment.getArguments() != null) {
                currentEventNameForHeader = fragment.getArguments().getString("event_name");
            }
            binding.toolbarUserGreeting.setText(currentEventNameForHeader != null ? currentEventNameForHeader : "Event Attendance");

            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();

        } else if (fragment instanceof ScanFragment) {
            binding.toolbarUserGreeting.setText("QR Scan");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        } else if (fragment instanceof AllEventLocationFragment) {
            binding.toolbarUserGreeting.setText("All Event Locations");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
        }

    }

    public void showBinIcon(boolean show) {
        invalidateOptionsMenu();
    }

    public void hideBottomNavigation(boolean hide) {
        if (binding.bottomNavigationView != null) {
            binding.bottomNavigationView.setVisibility(hide ? View.GONE : View.VISIBLE);
        }
    }

    private void updateSideNavAuthMenu() {
        if (sideNavigationView != null) {
            Menu menu = sideNavigationView.getMenu();
            MenuItem loginItem = menu.findItem(R.id.side_nav_login);
            MenuItem logoutItem = menu.findItem(R.id.side_nav_logout);

            if (mAuth.getCurrentUser() != null) {
                if (loginItem != null) loginItem.setVisible(false);
                if (logoutItem != null) logoutItem.setVisible(true);
            } else {
                if (loginItem != null) loginItem.setVisible(true);
                if (logoutItem != null) logoutItem.setVisible(false);
            }
        }
    }

    private void updateSideNavHeader() {
        if (sideNavigationView != null) {
            android.view.View headerView = sideNavigationView.getHeaderView(0);
            com.google.android.material.imageview.ShapeableImageView profilePic = headerView.findViewById(R.id.header_profile_pic);
            android.widget.TextView tvSideNavFullName = headerView.findViewById(R.id.header_user_name);
            android.widget.TextView tvUserEmail = headerView.findViewById(R.id.header_user_email);
            android.widget.TextView tvToolbarText = findViewById(R.id.toolbar_user_greeting);

            com.google.firebase.auth.FirebaseUser currentUser = mAuth.getCurrentUser();

            if (currentUser != null) {
                String uid = currentUser.getUid();

                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users").document(uid)
                        .get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {

                                String fullName = documentSnapshot.getString("fullName");
                                String userName = documentSnapshot.getString("username");
                                String email = documentSnapshot.getString("email");
                                String imageUri = documentSnapshot.getString("profileImage");
                                String role = documentSnapshot.getString("role");

                                if (fullName != null && !fullName.isEmpty()) {
                                    tvSideNavFullName.setText(fullName);
                                } else {
                                    tvSideNavFullName.setText(userName != null ? userName : "User");
                                }
                                if (tvToolbarText != null && !isRestoringFromThemeChange) {
                                    if ("Seller".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role)) {
                                        tvToolbarText.setText("Admin Dashboard");
                                    } else {
                                        Fragment currentFrag = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                                        if (currentFrag instanceof HomeFragment) {
                                            if (userName != null && !userName.isEmpty()) {
                                                tvToolbarText.setText("Hi " + userName);
                                            } else {
                                                tvToolbarText.setText("Hi User");
                                            }
                                        }
                                    }
                                }

                                isRestoringFromThemeChange = false;

                                if (email != null) {
                                    tvUserEmail.setText(email);
                                }

                                if (imageUri != null && !imageUri.isEmpty()) {
                                    com.bumptech.glide.Glide.with(MainActivity.this)
                                            .load(imageUri)
                                            .placeholder(R.drawable.user)
                                            .error(R.drawable.user)
                                            .into(profilePic);
                                } else {
                                    profilePic.setImageResource(R.drawable.user);
                                }
                            }
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("FirestoreError", "Error fetching data", e);
                        });
            } else {
                tvSideNavFullName.setText("Guest User");
                tvUserEmail.setText("Please sign in");
                profilePic.setImageResource(R.drawable.user);
                if (tvToolbarText != null) tvToolbarText.setText("Welcome");
            }
        }
    }

    public void setupAttendanceToolbar(String eventName) {
        if (binding != null) {
            this.currentEventNameForHeader = eventName;
            binding.toolbarUserGreeting.setText(eventName != null ? eventName : "Event Attendance");
            binding.bottomNavigationView.setVisibility(View.GONE);
            setupBackNavigation();
            invalidateOptionsMenu();
        }
    }

    private void setupBackNavigation() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                getSupportFragmentManager().popBackStack();
                getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                        if (current != null) {
                            activeFragment = current;
                            updateToolbarLayout(current);
                            updateBottomNavSelection(current);
                            updateSideNavSelection(current);
                        }
                        getSupportFragmentManager().removeOnBackStackChangedListener(this);
                    }
                });
            } else {
                loadFragment(new HomeFragment());
                bottomNavigationView.setSelectedItemId(R.id.bottom_nav_home);
            }
        });
    }

    private void updateBottomNavSelection(Fragment fragment) {
        if (bottomNavigationView == null) return;
        if (fragment instanceof HomeFragment || fragment instanceof AdminDashboardFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_home).setChecked(true);
        } else if (fragment instanceof MyTicketFragment || fragment instanceof ManageEventFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_tickets).setChecked(true);
        } else if (fragment instanceof FavoriteFragment || fragment instanceof ReportFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_favorite).setChecked(true);
        } else if (fragment instanceof AccountFragment) {
            bottomNavigationView.getMenu().findItem(R.id.bottom_nav_account).setChecked(true);
        }
    }

    private void updateSideNavSelection(Fragment fragment) {
        if (sideNavigationView == null) return;
        if (fragment instanceof HomeFragment || fragment instanceof AdminDashboardFragment) {
            sideNavigationView.setCheckedItem(R.id.side_nav_home);
        } else if (fragment instanceof MyTicketFragment || fragment instanceof ManageEventFragment) {
            sideNavigationView.setCheckedItem(R.id.side_nav_tickets);
        } else if (fragment instanceof FavoriteFragment || fragment instanceof ReportFragment) {
            sideNavigationView.setCheckedItem(R.id.side_nav_favorite);
        } else if (fragment instanceof AccountFragment) {
            sideNavigationView.setCheckedItem(R.id.side_nav_account);
        }
    }

    public void openEventDetails(String eventName) {
        EventSingleViewFragment fragment = new EventSingleViewFragment();
        Bundle args = new Bundle();
        args.putString("event_name", eventName);
        fragment.setArguments(args);
        loadFragment(fragment);
    }
}