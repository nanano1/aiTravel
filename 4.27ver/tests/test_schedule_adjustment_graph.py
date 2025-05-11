import unittest
from agent.context_manager import TripContextManager
from agent.state import TripState
from schedule_adjustment import create_schedule_adjustment_graph
from typing import Dict, List, Any

class TestScheduleAdjustmentGraph(unittest.TestCase):
    def setUp(self):
        # 创建测试用的行程数据
        self.test_itinerary = {
            "metadata": {
                "trip_id": "4",
                "title": "北京之旅",
                "destination": "北京",
                "total_days": 5,
                "start_date": "2025-05-07",
                "target_audience": "通用"
            },
            "daily_itinerary": [
                {
                    "day": 1,
                    "date": "2025-05-07",
                    "hotel": {
                        "name": "默认酒店",
                        "price": 300,
                        "numbed": 2,
                        "coordinates": [0, 0]
                    },
                    "schedule": [
                        {
                            "name": "北京王府井希尔顿酒店",
                            "type": "餐厅",
                            "poi_type": "景点",
                            "time": {"start": "09:00", "end": "10:30"},
                            "duration_hours": 1.5,
                            "notes": [],
                            "price": 0,
                            "coordinates": [39.914845, 116.413312],
                            "address": "王府井东街8号",
                            "tags": ["美食", "高档"]
                        },
                        {
                            "name": "王府井步行街",
                            "type": "景点",
                            "poi_type": "景点",
                            "time": {"start": "09:00", "end": "10:30"},
                            "duration_hours": 1.5,
                            "notes": [],
                            "price": 0,
                            "coordinates": [39.913392, 116.412423],
                            "address": "王府井大街256号",
                            "tags": ["购物", "文化"]
                        }
                    ]
                },
                {
                    "day": 2,
                    "date": "2025-05-08",
                    "hotel": {
                        "name": "默认酒店",
                        "price": 300,
                        "numbed": 2,
                        "coordinates": [0, 0]
                    },
                    "schedule": [
                        {
                            "name": "故宫博物院",
                            "type": "景点",
                            "poi_type": "景点",
                            "time": {"start": "09:00", "end": "12:00"},
                            "duration_hours": 3,
                            "notes": [],
                            "price": 60,
                            "coordinates": [39.916345, 116.397155],
                            "address": "东城区景山前街4号",
                            "tags": ["历史", "文化"]
                        }
                    ]
                }
            ]
        }
        
        # 初始化上下文管理器
        self.context_manager = TripContextManager({"travel_itineraries": [self.test_itinerary]})
        
        # 创建行程调整图
        self.graph = create_schedule_adjustment_graph()
        
    def test_compress_days_intent(self):
        """测试压缩天数意图"""
        # 创建初始状态
        initial_state = TripState({
            "user_input": "把5天行程压缩到3天",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        # 运行图
        final_state = self.graph.invoke(initial_state)
        
        # 验证结果
        self.assertEqual(final_state["flow_state"]["adjustment_type"], "compress_days")
        self.assertEqual(final_state["flow_state"]["target_days"], 3)
        self.assertTrue(final_state["should_continue"])
        
    def test_adjust_pace_intent(self):
        """测试调整节奏意图"""
        initial_state = TripState({
            "user_input": "每天少去两个景点",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        final_state = self.graph.invoke(initial_state)
        
        self.assertEqual(final_state["flow_state"]["adjustment_type"], "adjust_pace")
        self.assertEqual(final_state["flow_state"]["daily_limit"], 2)
        self.assertTrue(final_state["should_continue"])
        
    def test_style_change_intent(self):
        """测试风格替换意图"""
        initial_state = TripState({
            "user_input": "把历史景点换成自然风光",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        final_state = self.graph.invoke(initial_state)
        
        self.assertEqual(final_state["flow_state"]["adjustment_type"], "change_style")
        self.assertEqual(final_state["flow_state"]["target_style"], "自然风光")
        self.assertTrue(final_state["should_continue"])
        
    def test_invalid_intent(self):
        """测试无效意图处理"""
        initial_state = TripState({
            "user_input": "随便改改",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        final_state = self.graph.invoke(initial_state)
        
        self.assertFalse(final_state["should_continue"])
        self.assertIn("请明确说明您想要如何调整行程", final_state["response"])
        
    def test_poi_recommendation(self):
        """测试POI推荐功能"""
        initial_state = TripState({
            "user_input": "把历史景点换成自然风光",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        # 运行图
        final_state = self.graph.invoke(initial_state)
        
        # 验证是否生成了推荐POI
        self.assertTrue(len(final_state["flow_state"].get("recommended_pois", [])) > 0)
        # 验证推荐POI是否符合目标风格
        for poi in final_state["flow_state"].get("recommended_pois", []):
            self.assertIn("自然风光", poi.get("tags", []))
            
    def test_schedule_reorganization(self):
        """测试行程重组功能"""
        initial_state = TripState({
            "user_input": "把5天行程压缩到3天",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        # 运行图
        final_state = self.graph.invoke(initial_state)
        
        # 验证重组后的行程
        self.assertTrue("reorganized_schedule" in final_state["flow_state"])
        reorganized = final_state["flow_state"]["reorganized_schedule"]
        self.assertEqual(len(reorganized), 2)  # 验证天数是否正确
        
        # 验证每日景点数量是否合理
        for day in reorganized:
            self.assertLessEqual(len(day["schedule"]), 4)  # 假设每日最多4个景点
            
    def test_geographic_clustering(self):
        """测试地理聚类功能"""
        initial_state = TripState({
            "user_input": "优化一下路线",
            "conversation_history": [],
            "flow_state": {
                "active_flow": "行程调整",
                "on_progress": False
            },
            "should_continue": True
        })
        
        # 运行图
        final_state = self.graph.invoke(initial_state)
        
        # 验证地理聚类结果
        self.assertTrue("clustered_schedule" in final_state["flow_state"])
        clustered = final_state["flow_state"]["clustered_schedule"]
        
        # 验证每个聚类内的景点距离是否合理
        for cluster in clustered:
            for i in range(len(cluster) - 1):
                current = cluster[i]["coordinates"]
                next_poi = cluster[i + 1]["coordinates"]
                # 计算距离（简化版）
                distance = ((current[0] - next_poi[0])**2 + (current[1] - next_poi[1])**2)**0.5
                self.assertLess(distance, 0.1)  # 假设合理距离阈值

if __name__ == '__main__':
    unittest.main() 