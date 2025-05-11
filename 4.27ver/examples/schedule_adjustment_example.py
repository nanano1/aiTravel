import sys
import os
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from agent.context_manager import TripContextManager

def main():
    # 创建示例行程数据
    sample_itinerary = {
        "travel_itineraries": [{
            "metadata": {
                "trip_id": "sample_001",
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
    context_manager = TripContextManager(sample_itinerary)

    # 示例1：调整天数（从3天改为2天）
    print("\n=== 示例1：调整天数 ===")
    context_manager.set_adjustment_type("adjust_days")
    context_manager.set_adjustment_days(3, 2)
    context_manager.set_daily_limit(3)
    
    # 模拟添加新的POI建议
    new_pois = [
        {"name": "南锣鼓巷", "tags": ["文化", "美食"], "duration": 2},
        {"name": "798艺术区", "tags": ["艺术", "文化"], "duration": 3}
    ]
    context_manager.update_adjusted_pois(new_pois)
    
    print(f"调整类型: {context_manager.schedule_adjustment_flow.adjust_type}")
    print(f"原始天数: {context_manager.schedule_adjustment_flow.original_days}")
    print(f"新天数: {context_manager.schedule_adjustment_flow.new_days}")
    print(f"每日限制: {context_manager.schedule_adjustment_flow.daily_limit}")
    print("建议的新POI:")
    for poi in context_manager.schedule_adjustment_flow.suggested_poi_list:
        print(f"- {poi['name']} ({', '.join(poi['tags'])})")

    # 示例2：调整节奏（控制每日景点数量）
    print("\n=== 示例2：调整节奏 ===")
    context_manager.reset_schedule_adjustment_flow()
    context_manager.set_adjustment_type("adjust_pace")
    context_manager.set_daily_limit(2)
    
    print(f"调整类型: {context_manager.schedule_adjustment_flow.adjust_type}")
    print(f"每日限制: {context_manager.schedule_adjustment_flow.daily_limit}")

    # 示例3：重置流程
    print("\n=== 示例3：重置流程 ===")
    context_manager.reset_schedule_adjustment_flow()
    print(f"调整类型: {context_manager.schedule_adjustment_flow.adjust_type}")
    print(f"原始天数: {context_manager.schedule_adjustment_flow.original_days}")
    print(f"新天数: {context_manager.schedule_adjustment_flow.new_days}")
    print(f"每日限制: {context_manager.schedule_adjustment_flow.daily_limit}")

if __name__ == "__main__":
    main() 