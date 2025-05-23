package com.example.trave.Activities;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.trave.Domains.RecommendedPOI;
import com.example.trave.R;

import java.util.ArrayList;
import java.util.List;

public class POIRecommendationDialog extends Dialog {
    private final List<RecommendedPOI> poiList;
    private final List<RecommendedPOI> selectedPOIs = new ArrayList<>();
    private final POIRecommendationListener listener;
    private int currentIndex = 0;

    // UI components
    private TextView tvTitle, tvRating, tvDistance, tvAddress, tvOpenHours, tvRecommendReason;
    private RatingBar ratingBar;
    private ImageView imageView;
    private CheckBox checkBoxSelect;
    private Button btnConfirm, btnCancel;
    private ImageButton btnPrevious, btnNext;

    public interface POIRecommendationListener {
        void onPOIsSelected(List<RecommendedPOI> selectedPOIs);
        void onRefreshRequest();
    }

    public POIRecommendationDialog(@NonNull Context context, List<RecommendedPOI> poiList, POIRecommendationListener listener) {
        super(context);
        this.poiList = poiList;
        this.listener = listener;

        // 设置对话框样式
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_poi_recommendation);
        setCancelable(true);
        setCanceledOnTouchOutside(true);

        // 初始化UI组件
        initViews();
        setupListeners();

        // 显示第一个POI
        if (!poiList.isEmpty()) {
            updateUI(currentIndex);
        } else {
            dismiss();
        }
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvRating = findViewById(R.id.tvRating);
        tvDistance = findViewById(R.id.tvDistance);
        tvAddress = findViewById(R.id.tvAddress);
        tvOpenHours = findViewById(R.id.tvOpenHours);
        tvRecommendReason = findViewById(R.id.tvRecommendReason);
        ratingBar = findViewById(R.id.ratingBar);
        imageView = findViewById(R.id.imageView);
        checkBoxSelect = findViewById(R.id.checkBoxSelect);
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        // 更新确认按钮文本
        updateConfirmButtonText();
    }

    private void setupListeners() {
        // 上一个POI按钮
        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                updateUI(currentIndex);
            }
        });

        // 下一个POI按钮
        btnNext.setOnClickListener(v -> {
            if (currentIndex < poiList.size() - 1) {
                currentIndex++;
                updateUI(currentIndex);
            }
        });

        // 选择复选框
        checkBoxSelect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            RecommendedPOI currentPOI = poiList.get(currentIndex);
            if (isChecked) {
                if (!selectedPOIs.contains(currentPOI)) {
                    selectedPOIs.add(currentPOI);
                }
            } else {
                selectedPOIs.remove(currentPOI);
            }
            updateConfirmButtonText();
        });

        // 确认按钮
        btnConfirm.setOnClickListener(v -> {
            if (selectedPOIs.isEmpty()) {
                Toast.makeText(getContext(), "请至少选择一个景点", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null) {
                listener.onPOIsSelected(selectedPOIs);
            }
            dismiss();
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRefreshRequest();
            }
            dismiss();
        });
    }

    private void updateUI(int index) {
        if (index < 0 || index >= poiList.size()) return;

        RecommendedPOI poi = poiList.get(index);
        
        // 更新UI组件
        tvTitle.setText(poi.getName());
        tvRating.setText(String.valueOf(poi.getRating()));
        ratingBar.setRating((float) poi.getRating());
        
        // 更新距离
        String distance = poi.getDistance();
        if (distance != null && !distance.isEmpty()) {
            // 将字符串转换为浮点数
            double distanceInMeters = Double.parseDouble(distance);
            // 转换为公里并保留一位小数
            double distanceInKilometers = distanceInMeters / 1000;
            // 格式化为一位小数
            String formattedDistance = String.format("%.1f", distanceInKilometers);
            tvDistance.setText("距离现有景点平均距离: " + formattedDistance + "公里");
        } else {
            tvDistance.setText("距离: 未知");
        }
        
        // 更新地址
        tvAddress.setText("地址: " + poi.getAddress());
        
        // 更新营业时间
        String openHours = poi.getOpeningHours();
        if (openHours != null && !openHours.isEmpty()) {
            tvOpenHours.setText("营业时间: " + openHours);
        } else {
            tvOpenHours.setText("营业时间: 未知");
        }
        
        // 更新推荐理由
        tvRecommendReason.setText("推荐理由: " + poi.getRecommendationReason());
        
        // 更新复选框状态
        checkBoxSelect.setChecked(selectedPOIs.contains(poi));
        
        // 更新按钮状态
        btnPrevious.setEnabled(index > 0);
        btnNext.setEnabled(index < poiList.size() - 1);
    }

    private void updateConfirmButtonText() {
        btnConfirm.setText("确认选择 (" + selectedPOIs.size() + ")");
    }
} 