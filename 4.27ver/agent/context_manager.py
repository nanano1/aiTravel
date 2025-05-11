# context_manager.py

from typing import List, Dict, Optional, Any
from utils.file_utils import load_itinerary

class ScheduleAdjustmentFlow:
    def __init__(self):
        self.adjust_type: Optional[str] = None #调整类型
        self.daily_limit: Optional[int] = None #每日调整景点数量限制
        self.original_days: Optional[int] = None #原始天数
        self.new_days: Optional[int] = None #新天数
        self.suggested_poi_list: List[Dict[str, Any]] = [] #建议景点列表
        self.deleted_pois: List[str] = []  # 可选：记录哪些被删除
        self.added_pois: List[Dict[str, Any]] = [] #可选：记录哪些被添加

    def reset(self):
        self.adjust_type = None
        self.daily_limit = None
        self.original_days = None
        self.new_days = None
        self.suggested_poi_list = []
        self.deleted_pois = []
        self.added_pois = []


class AttractionFlow:
    def __init__(self):
        self.target_poi: Optional[Dict[str, Any]] = None
        self.user_preferences: Dict[str, str] = {}
        self.current_recommendations: List[Dict] = []
        self.retry_count: int = 0

    def reset(self):
        """重置 AttractionFlow 的所有属性"""
        self.target_poi = None
        self.user_preferences = {}
        self.current_recommendations = []
        self.retry_count = 0

class TripContextManager:
    def __init__(self, itinerary_data: dict):
        self.conversation_history = []
        self.itinerary_data = itinerary_data['travel_itineraries']
        self.current_trip_id: Optional[str] = None
        self.current_city: Optional[str] = None
        self.attraction_flow = AttractionFlow()  # 新增属性：景点流
        self.schedule_adjustment_flow = ScheduleAdjustmentFlow()  # 新增属性：行程调整流
        
        # 安全地设置当前行程ID，防止空列表导致IndexError
        if self.itinerary_data and len(self.itinerary_data) > 0:
            self.current_trip_id = str(self.itinerary_data[0]['metadata']['trip_id'])
        else:
            # 如果没有行程数据，设置一个默认值
            self.current_trip_id = "default_trip_id"

    # === 行程管理 ===
    def set_current_trip(self, trip_id: str) -> str:
        # 标准化行程ID，允许数字ID和字符串ID
        clean_trip_id = str(trip_id).replace('trip_id="', '').replace('"', '')
        
        # 首先尝试完全匹配
        for itinerary in self.itinerary_data:
            if str(itinerary["metadata"]["trip_id"]) == clean_trip_id:
                self.current_trip_id = clean_trip_id
                return f"✅ 当前行程设置为 {clean_trip_id}"
                
        # 如果没有找到完全匹配，尝试忽略类型的匹配（比如数字6和字符串"6"）
        for itinerary in self.itinerary_data:
            meta_trip_id = str(itinerary["metadata"]["trip_id"])
            if meta_trip_id == clean_trip_id or meta_trip_id.lower() == clean_trip_id.lower():
                self.current_trip_id = meta_trip_id  # 使用元数据中的ID格式
                return f"✅ 当前行程设置为 {meta_trip_id}"
                
        return f"❌ 未找到 ID 为 {clean_trip_id} 的行程单"

    def get_current_trip_id(self) -> Optional[str]:
        return self.current_trip_id

    def get_current_trip(self) -> Optional[dict]:
        print(f"当前行程ID: {self.current_trip_id}")  # 添加打印语句
        if not self.current_trip_id:
            return None
            
        # 支持不同类型的ID匹配（字符串、数字等）
        current_id_str = str(self.current_trip_id)
        for trip in self.itinerary_data:
            if str(trip["metadata"]["trip_id"]) == current_id_str:
                return trip
                
        return None

    # === 用户偏好管理 ===
    def update_preference(self, key: str, value: str):
        self.attraction_flow.user_preferences[key] = value

    def get_preferences(self) -> Dict[str, str]:
        return self.attraction_flow.user_preferences

    # === 辅助工具 ===
    def is_trip_selected(self) -> bool:
        return self.current_trip_id is not None

    # === 上下文集成 ===
    def get_context(self) -> dict:
        """整合所有上下文信息"""
        return {
            "current_trip": self.get_current_trip(),
            "preferences": self.attraction_flow.user_preferences,
            "current_city": self.current_city,
            "target_poi": self.attraction_flow.target_poi
        }
        
    def summary(self) -> str:
        summary_text = f"🧠 当前上下文状态：\n"
        summary_text += f" - 当前行程 ID: {self.current_trip_id}\n"
        summary_text += f" - 用户偏好: {self.attraction_flow.user_preferences if self.attraction_flow.user_preferences else '无'}\n"
        summary_text += f" - 当前城市: {self.current_city}\n"
        summary_text += f" - 当前更改中的景点: {self.attraction_flow.target_poi}\n"
        return summary_text

    # === 历史消息管理 ===
    def add_message(self, role: str, content: str):
        self.conversation_history.append({"role": role, "content": content})

    def update_preferences(self, preferences: dict):
        self.attraction_flow.user_preferences.update(preferences)
        return self.summary()

    # === 景点管理 ===
    def update_poi(self, poi: dict):
        self.attraction_flow.target_poi = poi
        return self.summary()

    def get_poi(self) -> Optional[Dict[str, str]]:
        return self.attraction_flow.target_poi
    
    # === 当前推荐列表管理 ===
    def update_recommendations(self, recommendations: List[dict]):
        """更新当前推荐列表"""
        self.attraction_flow.current_recommendations = recommendations
        return self.summary()

    def get_recommendations(self) -> List[dict]:
        """获取当前推荐列表"""
        return self.attraction_flow.current_recommendations
    
    # === 当前城市管理 ===
    def update_city(self, city: str):
        self.current_city = city
        return self.summary()
    
    def get_city(self) -> Optional[str]:
        return self.current_city

    def reset_attraction_flow(self):
        """重置 AttractionFlow"""
        self.attraction_flow.reset()

    # === 行程调整管理 ===
    def reset_schedule_adjustment_flow(self):
        """重置 ScheduleAdjustmentFlow"""
        self.schedule_adjustment_flow.reset()

    def set_adjustment_type(self, adjust_type: str):
        self.schedule_adjustment_flow.adjust_type = adjust_type

    def set_adjustment_days(self, original: int, new: int):
        self.schedule_adjustment_flow.original_days = original
        self.schedule_adjustment_flow.new_days = new

    def update_adjusted_pois(self, pois: List[Dict]):
        self.schedule_adjustment_flow.suggested_poi_list = pois

    def reset_schedule_adjustment(self):
        self.schedule_adjustment_flow.reset()

    def set_daily_limit(self, limit: int):
        self.schedule_adjustment_flow.daily_limit = limit

    def update_current_trip(self, trip_json: dict) -> dict:
        """更新当前行程数据，并返回更新结果
        
        Args:
            trip_json: 新的行程数据
            
        Returns:
            dict: 包含更新状态和数据的字典
        """
        if not trip_json:
            return {
                "success": False,
                "message": "无效的行程数据",
                "data": None,
                "data_type": None
            }
            
        if not self.current_trip_id:
            return {
                "success": False,
                "message": "当前没有选择行程，无法更新",
                "data": None,
                "data_type": None
            }
            
        try:
            # 更新行程数据
            for i, itinerary in enumerate(self.itinerary_data):
                if str(itinerary["metadata"]["trip_id"]) == str(self.current_trip_id):
                    # 保持原有的metadata中的其他信息不变
                    original_metadata = itinerary["metadata"].copy()
                    trip_json["metadata"] = {**original_metadata, **trip_json.get("metadata", {})}
                    
                    # 更新整个行程数据
                    self.itinerary_data[i] = trip_json
                    
                    return {
                        "success": True,
                        "message": f"行程 {self.current_trip_id} 已成功更新",
                        "data": trip_json,
                        "data_type": "trip"
                    }
            
            return {
                "success": False,
                "message": f"未找到ID为 {self.current_trip_id} 的行程",
                "data": None,
                "data_type": None
            }
            
        except Exception as e:
            return {
                "success": False,
                "message": f"更新行程时出错: {str(e)}",
                "data": None,
                "data_type": None
            }

