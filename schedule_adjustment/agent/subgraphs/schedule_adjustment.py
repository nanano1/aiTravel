from typing import Dict, List, Any
import re
from sklearn.cluster import DBSCAN
import numpy as np
from math import radians, sin, cos, sqrt, atan2

class ScheduleAdjustmentFlow:
    def __init__(self):
        self.intent_patterns = {
            "compress_days": r"压缩|缩短|减少天数|一天",
            "adjust_pace": r"节奏|速度|放慢|太快|太赶|休闲",
            "change_style": r"风格|类型|文化|自然|购物|更多",
            "optimize_route": r"路线|路程|距离|合理|优化"
        }
        
        self.poi_database = {
            "文化": [
                {"name": "国家博物馆", "type": "文化", "duration": 180, 
                 "location": {"lat": 39.9054, "lng": 116.4012}},
                {"name": "天坛", "type": "文化", "duration": 150,
                 "location": {"lat": 39.8822, "lng": 116.4107}},
                {"name": "孔庙", "type": "文化", "duration": 120,
                 "location": {"lat": 39.9474, "lng": 116.4137}}
            ],
            "自然": [
                {"name": "北海公园", "type": "自然", "duration": 180,
                 "location": {"lat": 39.9287, "lng": 116.3905}},
                {"name": "香山公园", "type": "自然", "duration": 240,
                 "location": {"lat": 39.9906, "lng": 116.1947}}
            ],
            "购物": [
                {"name": "三里屯", "type": "购物", "duration": 180,
                 "location": {"lat": 39.9336, "lng": 116.4547}},
                {"name": "西单", "type": "购物", "duration": 150,
                 "location": {"lat": 39.9099, "lng": 116.3739}}
            ]
        }

    def parse_intent(self, user_input: str) -> Dict[str, str]:
        """解析用户输入，识别调整意图"""
        for intent_type, pattern in self.intent_patterns.items():
            if re.search(pattern, user_input):
                return {"type": intent_type}
        return {"type": "unknown"}

    def adjust_schedule(self, schedule: Dict[str, Any], intent: Dict[str, str]) -> Dict[str, Any]:
        """根据意图调整行程"""
        if intent["type"] == "unknown":
            raise ValueError("无法识别的调整意图")

        if intent["type"] == "compress_days":
            return self.compress_schedule(schedule)
        elif intent["type"] == "adjust_pace":
            return self.adjust_schedule_pace(schedule)
        elif intent["type"] == "change_style":
            return self.change_schedule_style(schedule)
        elif intent["type"] == "optimize_route":
            return self.optimize_route(schedule)

    def compress_schedule(self, schedule: Dict[str, Any]) -> Dict[str, Any]:
        """压缩行程天数"""
        all_spots = []
        for day in schedule["days"]:
            all_spots.extend(day["spots"])
            
        return {
            "days": [{
                "day": 1,
                "spots": all_spots
            }]
        }

    def adjust_schedule_pace(self, schedule: Dict[str, Any]) -> Dict[str, Any]:
        """调整行程节奏"""
        all_spots = []
        for day in schedule["days"]:
            all_spots.extend(day["spots"])
            
        # 每天最多安排2个景点
        days_needed = (len(all_spots) + 1) // 2
        adjusted_schedule = {"days": []}
        
        for i in range(days_needed):
            start_idx = i * 2
            day_spots = all_spots[start_idx:start_idx + 2]
            adjusted_schedule["days"].append({
                "day": i + 1,
                "spots": day_spots
            })
            
        return adjusted_schedule

    def change_schedule_style(self, schedule: Dict[str, Any]) -> Dict[str, Any]:
        """改变行程风格"""
        # 统计当前风格
        style_count = {}
        for day in schedule["days"]:
            for spot in day["spots"]:
                style_count[spot["type"]] = style_count.get(spot["type"], 0) + 1
                
        # 找出最少的风格类型
        target_style = min(style_count.items(), key=lambda x: x[1])[0]
        
        # 添加该风格的新景点
        new_spots = self.recommend_pois({"type": target_style})[:2]
        
        # 创建新行程
        new_schedule = {"days": []}
        for i, day in enumerate(schedule["days"]):
            new_day = {
                "day": i + 1,
                "spots": day["spots"]
            }
            if i == 0:  # 在第一天添加新景点
                new_day["spots"].extend(new_spots)
            new_schedule["days"].append(new_day)
            
        return new_schedule

    def recommend_pois(self, preferences: Dict[str, str]) -> List[Dict[str, Any]]:
        """根据偏好推荐POI"""
        poi_type = preferences.get("type", "文化")
        return self.poi_database.get(poi_type, [])

    def reorganize_schedule(self, schedule: Dict[str, Any], intent: Dict[str, str]) -> Dict[str, Any]:
        """重新组织行程"""
        if intent["type"] == "compress_days":
            return self.compress_schedule(schedule)
        return schedule

    def cluster_by_location(self, spots: List[Dict[str, Any]]) -> List[List[Dict[str, Any]]]:
        """使用地理位置对景点进行聚类"""
        # 提取经纬度
        coordinates = np.array([[spot["location"]["lat"], spot["location"]["lng"]] 
                              for spot in spots])
        
        # 使用DBSCAN进行聚类
        # eps参数表示最大距离（约2公里），min_samples表示最小点数
        clustering = DBSCAN(eps=0.02, min_samples=2).fit(coordinates)
        
        # 整理聚类结果
        clusters = {}
        for spot, label in zip(spots, clustering.labels_):
            if label not in clusters:
                clusters[label] = []
            clusters[label].append(spot)
            
        return list(clusters.values())

    def calculate_distance(self, point1: Dict[str, float], point2: Dict[str, float]) -> float:
        """计算两点之间的距离（使用Haversine公式）"""
        R = 6371  # 地球半径（公里）
        
        lat1 = radians(point1["lat"])
        lon1 = radians(point1["lng"])
        lat2 = radians(point2["lat"])
        lon2 = radians(point2["lng"])
        
        dlat = lat2 - lat1
        dlon = lon2 - lon1
        
        a = sin(dlat/2)**2 + cos(lat1) * cos(lat2) * sin(dlon/2)**2
        c = 2 * atan2(sqrt(a), sqrt(1-a))
        distance = R * c
        
        return distance

    def optimize_route(self, schedule: Dict[str, Any]) -> Dict[str, Any]:
        """优化行程路线"""
        optimized_schedule = {"days": []}
        
        for day in schedule["days"]:
            spots = day["spots"]
            if len(spots) <= 2:  # 如果景点数量少于等于2，保持原样
                optimized_schedule["days"].append(day)
                continue
                
            # 使用简单的贪心算法优化路线
            current_spot = spots[0]
            remaining_spots = spots[1:]
            optimized_spots = [current_spot]
            
            while remaining_spots:
                # 找到距离当前位置最近的下一个景点
                next_spot = min(
                    remaining_spots,
                    key=lambda x: self.calculate_distance(
                        current_spot["location"],
                        x["location"]
                    )
                )
                optimized_spots.append(next_spot)
                remaining_spots.remove(next_spot)
                current_spot = next_spot
                
            optimized_schedule["days"].append({
                "day": day["day"],
                "spots": optimized_spots
            })
            
        return optimized_schedule 