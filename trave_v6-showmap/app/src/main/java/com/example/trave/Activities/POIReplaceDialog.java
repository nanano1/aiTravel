package com.example.trave.Activities;

import android.app.Dialog;
import android.content.Context;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.trave.Domains.RecommendedPOI;
import com.example.trave.R;

import java.util.List;

public class POIReplaceDialog extends Dialog {
    private final List<RecommendedPOI> poiList;
    private RecommendedPOI selectedPOI;
    private final POIReplaceListener listener;
    private int currentIndex = 0;

    // UI components
    private TextView tvTitle, tvRating, tvDistance, tvAddress, tvOpenHours, tvRecommendReason;
    private RatingBar ratingBar;
    private ImageView imageView;
    private Button btnConfirm, btnCancel;
    private ImageButton btnPrevious, btnNext;

    public interface POIReplaceListener {
        void onPOISelected(RecommendedPOI selectedPOI);
        void onRefreshRequest();
    }

    public POIReplaceDialog(@NonNull Context context, List<RecommendedPOI> poiList, POIReplaceListener listener) {
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
        btnConfirm = findViewById(R.id.btnConfirm);
        btnCancel = findViewById(R.id.btnCancel);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);

        // 移除多选框，因为我们只需要选择当前显示的景点
        if (findViewById(R.id.checkBoxSelect) != null) {
            findViewById(R.id.checkBoxSelect).setVisibility(android.view.View.GONE);
        }

        // 更新确认按钮文本
        btnConfirm.setText("选择此景点");
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

        // 确认按钮 - 直接选择当前显示的景点
        btnConfirm.setOnClickListener(v -> {
            selectedPOI = poiList.get(currentIndex);
            if (listener != null) {
                listener.onPOISelected(selectedPOI);
            }
            dismiss();
        });

        // 取消按钮 - "换一批"
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
            try {
                // 将字符串转换为浮点数
                double distanceInMeters = Double.parseDouble(distance);
                // 转换为公里并保留一位小数
                double distanceInKilometers = distanceInMeters / 1000;
                // 格式化为一位小数
                String formattedDistance = String.format("%.1f", distanceInKilometers);
                tvDistance.setText("距离现有景点平均距离: " + formattedDistance + "公里");
            } catch (NumberFormatException e) {
                tvDistance.setText("距离: " + distance);
            }
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
        
        // 更新按钮状态
        btnPrevious.setEnabled(index > 0);
        btnNext.setEnabled(index < poiList.size() - 1);
    }
} 