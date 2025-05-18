# 地图显示功能调用流程文档

## 1. 地图功能概述

地图显示是本旅行规划应用的重要组成部分，基于高德地图SDK集成，提供景点标记、行程路线规划、餐厅位置显示等核心功能。地图组件在多个活动中都有使用，包括行程详情查看、景点探索和餐厅推荐展示等。

## 2. 功能架构图

```
┌─────────────────┐    ┌───────────────────┐    ┌────────────────┐
│  行程数据获取   │───>│  地图标记生成     │───>│  地图组件显示  │
└─────────────────┘    └───────────────────┘    └────────────────┘
        │                       │                       │
        ▼                       ▼                       ▼
┌─────────────────┐    ┌───────────────────┐    ┌────────────────┐
│ DatabaseHelper  │    │ 经纬度坐标转换    │    │ 地图交互处理  │
└─────────────────┘    └───────────────────┘    └────────────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                ▼
                       ┌───────────────────┐
                       │   路线规划处理    │
                       └───────────────────┘
```

## 3. 核心组件

### 3.1 地图初始化与配置

地图组件主要在TripDetailActivity和相关活动中初始化，基于高德地图SDK实现。主要流程如下：

```java
// 地图初始化代码
private void initMap() {
    // 获取地图控件引用
    mapView = findViewById(R.id.map_view);
    mapView.onCreate(savedInstanceState);
    
    // 异步获取AMap对象
    mapView.getMapAsync(aMap -> {
        this.aMap = aMap;
        
        // 设置地图UI控制
        UiSettings uiSettings = aMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setCompassEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        
        // 设置定位蓝点
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
        aMap.setMyLocationStyle(myLocationStyle);
        aMap.setMyLocationEnabled(true);
        
        // 加载行程数据到地图
        loadItineraryToMap();
    });
}
```

### 3.2 行程数据加载与地图标记

行程数据从数据库中读取后，会转换为地图标记点并显示在地图上：

```java
private void loadItineraryToMap() {
    ArrayList<ItineraryAttraction> attractions = dbHelper.getItineraryAttractions(itineraryId);
    
    // 清除现有标记
    aMap.clear();
    
    // 用于路线规划的点集合
    List<LatLng> pathPoints = new ArrayList<>();
    
    for (ItineraryAttraction attraction : attractions) {
        // 获取景点详细信息
        Sites site = dbHelper.getSiteBySiteId(attraction.getSiteId());
        if (site != null) {
            // 创建标记点位置
            LatLng position = new LatLng(site.getLatitude(), site.getLongitude());
            
            // 创建标记选项
            MarkerOptions markerOptions = new MarkerOptions()
                .position(position)
                .title(attraction.getAttractionName())
                .snippet("Day " + attraction.getDayNumber() + " - " + site.getAddress());
            
            // 根据类型设置不同图标
            if ("餐厅".equals(attraction.getType())) {
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant_marker));
            } else if ("景点".equals(attraction.getType())) {
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_attraction_marker));
            } else {
                markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
            }
            
            // 添加标记到地图
            Marker marker = aMap.addMarker(markerOptions);
            marker.setObject(attraction);  // 关联原始数据
            
            // 添加到路径点
            pathPoints.add(position);
        }
    }
    
    // 如果有足够的点，绘制路线
    if (pathPoints.size() > 1) {
        drawRoutePath(pathPoints);
    }
    
    // 缩放地图以显示所有标记
    if (!pathPoints.isEmpty()) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : pathPoints) {
            builder.include(point);
        }
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }
}
```

### 3.3 路线规划与绘制

对于包含多个景点的行程，应用会自动规划路线并绘制在地图上：

```java
private void drawRoutePath(List<LatLng> pathPoints) {
    // 按天分组路径点
    Map<Integer, List<LatLng>> dayPaths = new HashMap<>();
    
    for (int i = 0; i < attractions.size(); i++) {
        ItineraryAttraction attraction = attractions.get(i);
        LatLng point = pathPoints.get(i);
        
        int day = attraction.getDayNumber();
        if (!dayPaths.containsKey(day)) {
            dayPaths.put(day, new ArrayList<>());
        }
        dayPaths.get(day).add(point);
    }
    
    // 为每天绘制不同颜色的路线
    int[] colors = new int[]{Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN};
    
    for (Map.Entry<Integer, List<LatLng>> entry : dayPaths.entrySet()) {
        int day = entry.getKey();
        List<LatLng> dayPoints = entry.getValue();
        
        if (dayPoints.size() > 1) {
            // 选择颜色 (循环使用颜色数组)
            int color = colors[(day - 1) % colors.length];
            
            // 创建折线选项
            PolylineOptions polylineOptions = new PolylineOptions()
                .addAll(dayPoints)
                .width(10)
                .color(color)
                .setDottedLine(true);
            
            // 添加到地图
            aMap.addPolyline(polylineOptions);
        }
    }
}
```

### 3.4 餐厅与景点推荐在地图上的显示

从AI获取的餐厅和景点推荐也需要在地图上显示，以帮助用户了解其位置：

```java
public void showRecommendationsOnMap(List<RecommendedRestaurant> restaurants) {
    if (aMap == null || restaurants == null || restaurants.isEmpty()) {
        return;
    }
    
    // 清除之前的推荐标记
    clearRecommendationMarkers();
    
    // 创建新的标记集合
    recommendationMarkers = new ArrayList<>();
    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    
    for (RecommendedRestaurant restaurant : restaurants) {
        LatLng position = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
        
        MarkerOptions markerOptions = new MarkerOptions()
            .position(position)
            .title(restaurant.getName())
            .snippet(restaurant.getAddress())
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_recommended_restaurant));
        
        Marker marker = aMap.addMarker(markerOptions);
        marker.setObject(restaurant);
        recommendationMarkers.add(marker);
        
        builder.include(position);
    }
    
    // 缩放地图以显示所有推荐标记
    if (!recommendationMarkers.isEmpty()) {
        aMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
    }
}
```

## 4. 地图点击事件处理

地图标记点击时会显示详细信息并允许用户进行操作：

```java
aMap.setOnMarkerClickListener(marker -> {
    // 获取关联数据
    Object obj = marker.getObject();
    
    if (obj instanceof ItineraryAttraction) {
        // 处理行程景点标记点击
        ItineraryAttraction attraction = (ItineraryAttraction) obj;
        Sites site = dbHelper.getSiteBySiteId(attraction.getSiteId());
        
        // 显示信息窗口
        marker.showInfoWindow();
        
        // 显示详情对话框
        showAttractionDetailsDialog(attraction, site);
        return true;
    } else if (obj instanceof RecommendedRestaurant) {
        // 处理推荐餐厅标记点击
        RecommendedRestaurant restaurant = (RecommendedRestaurant) obj;
        
        // 显示信息窗口
        marker.showInfoWindow();
        
        // 显示餐厅详情
        showRestaurantDetailsDialog(restaurant);
        return true;
    }
    
    return false;
});
```

## 5. 餐厅推荐与地图整合

当从AI聊天界面选择一个推荐的餐厅后，系统会在地图上添加并显示该餐厅：

```java
private void addSelectedRestaurantToMap(RecommendedRestaurant restaurant) {
    // 首先检查地图是否已初始化
    if (aMap == null) {
        return;
    }
    
    // 创建餐厅位置
    LatLng position = new LatLng(restaurant.getLatitude(), restaurant.getLongitude());
    
    // 添加标记
    MarkerOptions markerOptions = new MarkerOptions()
        .position(position)
        .title(restaurant.getName())
        .snippet(restaurant.getAddress())
        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_restaurant_marker));
    
    Marker marker = aMap.addMarker(markerOptions);
    
    // 将视图移动到新添加的餐厅
    aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15f));
    
    // 显示信息窗口
    marker.showInfoWindow();
    
    // 更新路线规划
    updateRoutePlanWithNewPoi(position);
}
```

## 6. 后台位置数据处理

地图显示功能依赖于从数据库中获取的位置数据，主要由`DatabaseHelper`类处理：

```java
public Sites getSiteBySiteId(long siteId) {
    SQLiteDatabase db = this.getReadableDatabase();
    
    String[] projection = {
        COLUMN_SITE_ID,
        COLUMN_SITE_POI_ID,
        COLUMN_SITE_NAME,
        COLUMN_SITE_LATITUDE,
        COLUMN_SITE_LONGITUDE,
        COLUMN_SITE_ADDRESS,
        COLUMN_SITE_BUSINESS_AREA,
        COLUMN_SITE_TEL,
        COLUMN_SITE_WEBSITE,
        COLUMN_SITE_TYPE_DESC,
        COLUMN_SITE_PHOTOS
    };
    
    String selection = COLUMN_SITE_ID + "=?";
    String[] selectionArgs = {String.valueOf(siteId)};
    
    Cursor cursor = db.query(TABLE_SITES, projection, selection, selectionArgs, null, null, null);
    
    Sites site = null;
    if (cursor != null && cursor.moveToFirst()) {
        // 从Cursor中提取数据创建Sites对象
        site = extractSiteFromCursor(cursor);
        cursor.close();
    }
    
    return site;
}
```

## 7. POI数据获取与处理

应用使用高德API获取POI数据，通过`amap_poi_details.py`脚本处理的数据将存储在数据库中，并在地图上显示：

```python
def get_poi_details(poi_ids: List[str]) -> List[dict]:
    """调用高德API获取POI详细信息"""
    base_url = "https://restapi.amap.com/v5/place/detail"
    
    # 将POI IDs转换为"|"分隔的字符串
    poi_ids_str = "|".join(poi_ids)
    
    params = {
        "key": AMAP_KEY,
        "id": poi_ids_str,
        "show_fields": "business"  # 包含额外信息
    }
    
    response = requests.get(base_url, params=params)
    data = response.json()
    
    if data["status"] == "1":
        return data["pois"]
    else:
        print(f"API错误: {data['info']}")
        return []
```

## 8. 用例示例：显示行程路线

以下是一个完整的用例示例，展示如何在TripDetailActivity中加载并显示行程路线：

1. 用户打开行程详情页面
2. 系统从数据库加载行程数据
3. 系统初始化地图组件
4. 系统将行程景点转换为地图标记并添加到地图
5. 系统为同一天的景点生成连接路线
6. 系统缩放地图以显示整个行程
7. 用户可以点击地图上的标记查看详情

## 9. 注意事项与最佳实践

1. **地图生命周期管理**：务必在活动/Fragment的相应生命周期方法中正确管理地图组件的生命周期：
   ```java
   @Override
   protected void onResume() {
       super.onResume();
       mapView.onResume();
   }
   
   @Override
   protected void onPause() {
       mapView.onPause();
       super.onPause();
   }
   
   @Override
   protected void onDestroy() {
       mapView.onDestroy();
       super.onDestroy();
   }
   ```

2. **内存管理**：地图组件是重资源组件，注意在不需要时释放资源。

3. **数据刷新策略**：当添加新POI或更改行程后，确保正确刷新地图显示：
   ```java
   // 在数据变更后调用
   private void refreshMapDisplay() {
       if (aMap != null) {
           loadItineraryToMap();
       }
   }
   ```

4. **性能优化**：对于大量标记点，使用聚合标记技术减少渲染压力：
   ```java
   // 启用标记聚合
   MarkerClusterItem markerClusterItem = new MarkerClusterItem(latLng);
   markerClusterItem.setObject(attraction);  // 关联数据
   clusterItems.add(markerClusterItem);
   
   // 创建聚合渲染器
   ClusterRender clusterRender = new CustomClusterRender(getApplicationContext(), aMap, clusterItems);
   clusterOverlay = new ClusterOverlay(aMap, clusterItems, clusterRender);
   ```

5. **错误处理**：地图加载和POI数据获取可能会失败，添加适当的错误处理机制：
   ```java
   try {
       // 地图操作
   } catch (Exception e) {
       Log.e(TAG, "地图操作失败: " + e.getMessage());
       Toast.makeText(this, "地图显示出错，请重试", Toast.LENGTH_SHORT).show();
   }
   ``` 