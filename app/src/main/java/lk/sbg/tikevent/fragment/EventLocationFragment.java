package lk.sbg.tikevent.fragment;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import lk.sbg.tikevent.R;
import lk.sbg.tikevent.activity.MainActivity;

public class EventLocationFragment extends DialogFragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LatLng selectedLatLng;
    private String selectedAddress = "";
    private TextView tvCurrentAddress;
    private EditText etSearchLocation;
    private FrameLayout btnSearchLocation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_NoActionBar_Fullscreen);
        setCancelable(false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event_location, container, false);

        tvCurrentAddress = view.findViewById(R.id.tv_current_address);
        etSearchLocation = view.findViewById(R.id.et_search_location);
        btnSearchLocation = view.findViewById(R.id.btn_search_location);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm_location);
        ImageView btnClose = view.findViewById(R.id.btn_close_dialog);

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.google_map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSearchLocation.setOnClickListener(v -> searchLocationOnMap());
        etSearchLocation.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchLocationOnMap();
                return true;
            }
            return false;
        });

        btnConfirm.setOnClickListener(v -> {
            if (selectedLatLng != null) {
                sendLocationBack();
            } else {
                Toast.makeText(getContext(), "Please select a location on map", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getActivity() instanceof MainActivity) {

            ((MainActivity) getActivity()).findViewById(R.id.toolbar).setVisibility(View.GONE);
            ((MainActivity) getActivity()).findViewById(R.id.bottomNavigationView).setVisibility(View.GONE);
        }
    }

    @Override
    public void onDismiss(@NonNull android.content.DialogInterface dialog) {
        super.onDismiss(dialog);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        }
    }

    private void searchLocationOnMap() {
        String locationName = etSearchLocation.getText().toString().trim();
        if (locationName.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a location name", Toast.LENGTH_SHORT).show();
            return;
        }

        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocationName(locationName, 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                LatLng targetLatLng = new LatLng(address.getLatitude(), address.getLongitude());
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(targetLatLng).title(locationName));
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 15f));
                selectedLatLng = targetLatLng;
                selectedAddress = address.getAddressLine(0);
                tvCurrentAddress.setText(selectedAddress);
            } else {
                Toast.makeText(getContext(), "Location not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        LatLng sriLanka = new LatLng(7.8731, 80.7718);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(sriLanka, 7.5f));
        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng));
            selectedLatLng = latLng;
            getAddressFromLatLng(latLng);
        });
    }

    private void getAddressFromLatLng(LatLng latLng) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                selectedAddress = addresses.get(0).getAddressLine(0);
                tvCurrentAddress.setText(selectedAddress);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLocationBack() {
        Bundle result = new Bundle();
        result.putString("selectedAddress", selectedAddress);
        if (selectedLatLng != null) {
            result.putDouble("lat", selectedLatLng.latitude);
            result.putDouble("lng", selectedLatLng.longitude);
        }
        getParentFragmentManager().setFragmentResult("locationKey", result);
        dismiss();
    }
}