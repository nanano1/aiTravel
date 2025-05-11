import unittest
from ..agent.subgraphs.schedule_adjustment import ScheduleAdjustmentFlow

class TestScheduleAdjustmentFlow(unittest.TestCase):
    def setUp(self):
        self.flow = ScheduleAdjustmentFlow()
        self.sample_schedule = {
            "days": [
                {
                    "day": 1,
                    "spots": [
                        {"name": "王府井", "type": "购物", "duration": 120},
                        {"name": "故宫", "type": "文化", "duration": 180}
                    ]
                },
                {
                    "day": 2,
                    "spots": [
                        {"name": "天安门", "type": "文化", "duration": 60},
                        {"name": "颐和园", "type": "自然", "duration": 240}
                    ]
                }
            ]
        }

    def test_compress_days_intent(self):
        """测试压缩天数意图识别和处理"""
        user_input = "能否把行程压缩到一天完成？"
        intent = self.flow.parse_intent(user_input)
        self.assertEqual(intent["type"], "compress_days")
        
        result = self.flow.adjust_schedule(self.sample_schedule, intent)
        self.assertEqual(len(result["days"]), 1)
        self.assertGreater(len(result["days"][0]["spots"]), 2)

    def test_adjust_pace_intent(self):
        """测试调整节奏意图识别和处理"""
        user_input = "每天安排的景点太多了，能否放慢节奏？"
        intent = self.flow.parse_intent(user_input)
        self.assertEqual(intent["type"], "adjust_pace")
        
        result = self.flow.adjust_schedule(self.sample_schedule, intent)
        for day in result["days"]:
            self.assertLessEqual(len(day["spots"]), 2)

    def test_change_style_intent(self):
        """测试风格替换意图识别和处理"""
        user_input = "想要更多文化类的景点"
        intent = self.flow.parse_intent(user_input)
        self.assertEqual(intent["type"], "change_style")
        
        result = self.flow.adjust_schedule(self.sample_schedule, intent)
        cultural_spots = sum(1 for day in result["days"] 
                           for spot in day["spots"] 
                           if spot["type"] == "文化")
        self.assertGreater(cultural_spots, 2)

    def test_invalid_intent(self):
        """测试无效意图处理"""
        user_input = "今天天气真好"
        intent = self.flow.parse_intent(user_input)
        self.assertEqual(intent["type"], "unknown")
        
        with self.assertRaises(ValueError):
            self.flow.adjust_schedule(self.sample_schedule, intent)

    def test_poi_recommendation(self):
        """测试POI推荐功能"""
        preferences = {"type": "文化", "duration": "2-3小时"}
        recommendations = self.flow.recommend_pois(preferences)
        self.assertGreater(len(recommendations), 0)
        for poi in recommendations:
            self.assertEqual(poi["type"], "文化")
            self.assertGreaterEqual(poi["duration"], 120)
            self.assertLessEqual(poi["duration"], 180)

    def test_schedule_reorganization(self):
        """测试行程重组功能"""
        intent = {"type": "compress_days", "target_days": 1}
        result = self.flow.reorganize_schedule(self.sample_schedule, intent)
        self.assertEqual(len(result["days"]), 1)
        total_spots = sum(len(day["spots"]) for day in self.sample_schedule["days"])
        self.assertEqual(
            len(result["days"][0]["spots"]), 
            total_spots
        )

    def test_geographic_clustering(self):
        """测试地理聚类功能"""
        spots = [
            {"name": "故宫", "location": {"lat": 39.9163, "lng": 116.3972}},
            {"name": "天安门", "location": {"lat": 39.9054, "lng": 116.3976}},
            {"name": "王府井", "location": {"lat": 39.9158, "lng": 116.4108}},
            {"name": "颐和园", "location": {"lat": 39.9999, "lng": 116.2754}}
        ]
        clusters = self.flow.cluster_by_location(spots)
        self.assertGreater(len(clusters), 1)
        # 确保距离相近的景点被分到同一组
        for cluster in clusters:
            if "故宫" in [spot["name"] for spot in cluster]:
                self.assertTrue(
                    any(spot["name"] == "天安门" for spot in cluster)
                )

if __name__ == '__main__':
    unittest.main() 