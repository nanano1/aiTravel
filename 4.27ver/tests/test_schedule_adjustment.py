import unittest
from agent.context_manager import ScheduleAdjustmentFlow, TripContextManager
from typing import Dict, List, Any

class TestScheduleAdjustment(unittest.TestCase):
    def setUp(self):
        # 创建测试用的行程数据
        self.test_itinerary = {
            "travel_itineraries": [{
                "metadata": {
                    "trip_id": "test_001",
                    "days": 3
                },
                "days": [
                    {
                        "day": 1,
                        "pois": [
                            {"name": "故宫", "tags": ["历史", "文化"], "duration": 3},
                            {"name": "天安门", "tags": ["历史", "地标"], "duration": 2}
                        ]
                    },
                    {
                        "day": 2,
                        "pois": [
                            {"name": "颐和园", "tags": ["自然", "文化"], "duration": 4},
                            {"name": "圆明园", "tags": ["历史", "文化"], "duration": 3}
                        ]
                    },
                    {
                        "day": 3,
                        "pois": [
                            {"name": "长城", "tags": ["自然", "历史"], "duration": 5},
                            {"name": "十三陵", "tags": ["历史", "文化"], "duration": 3}
                        ]
                    }
                ]
            }]
        }
        
        # 初始化上下文管理器
        self.context_manager = TripContextManager(self.test_itinerary)
        
    def test_adjust_days(self):
        """测试调整天数功能"""
        # 设置调整类型为改变天数
        self.context_manager.set_adjustment_type("adjust_days")
        
        # 设置原始天数和新天数
        self.context_manager.set_adjustment_days(3, 2)
        
        # 设置每日限制
        self.context_manager.set_daily_limit(3)
        
        # 验证设置是否正确
        self.assertEqual(self.context_manager.schedule_adjustment_flow.adjust_type, "adjust_days")
        self.assertEqual(self.context_manager.schedule_adjustment_flow.original_days, 3)
        self.assertEqual(self.context_manager.schedule_adjustment_flow.new_days, 2)
        self.assertEqual(self.context_manager.schedule_adjustment_flow.daily_limit, 3)
        
    def test_adjust_pace(self):
        """测试调整节奏功能"""
        # 设置调整类型为调整节奏
        self.context_manager.set_adjustment_type("adjust_pace")
        
        # 设置每日限制
        self.context_manager.set_daily_limit(2)
        
        # 验证设置是否正确
        self.assertEqual(self.context_manager.schedule_adjustment_flow.adjust_type, "adjust_pace")
        self.assertEqual(self.context_manager.schedule_adjustment_flow.daily_limit, 2)
        
    def test_reset_flow(self):
        """测试重置流程功能"""
        # 先设置一些值
        self.context_manager.set_adjustment_type("adjust_days")
        self.context_manager.set_adjustment_days(3, 2)
        self.context_manager.set_daily_limit(3)
        
        # 重置流程
        self.context_manager.reset_schedule_adjustment_flow()
        
        # 验证所有值是否被重置
        self.assertIsNone(self.context_manager.schedule_adjustment_flow.adjust_type)
        self.assertIsNone(self.context_manager.schedule_adjustment_flow.original_days)
        self.assertIsNone(self.context_manager.schedule_adjustment_flow.new_days)
        self.assertIsNone(self.context_manager.schedule_adjustment_flow.daily_limit)
        self.assertEqual(self.context_manager.schedule_adjustment_flow.suggested_poi_list, [])
        
    def test_update_adjusted_pois(self):
        """测试更新调整后的POI列表"""
        # 创建测试POI列表
        test_pois = [
            {"name": "新景点1", "tags": ["自然", "休闲"], "duration": 2},
            {"name": "新景点2", "tags": ["文化", "艺术"], "duration": 3}
        ]
        
        # 更新POI列表
        self.context_manager.update_adjusted_pois(test_pois)
        
        # 验证更新是否正确
        self.assertEqual(self.context_manager.schedule_adjustment_flow.suggested_poi_list, test_pois)

if __name__ == '__main__':
    unittest.main() 