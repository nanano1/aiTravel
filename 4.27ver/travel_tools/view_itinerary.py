import json
from agent.context_manager import TripContextManager
from typing import Dict, Any, Optional, List


def view_itinerary(context_manager: TripContextManager) -> str:
    """
    展示当前行程的完整详细信息

    参数:
        context_manager: 行程上下文管理器

    返回:
        str: 格式化后的行程详情字符串（包含元数据、每日行程、POI详情）
    """
    trip_id = context_manager.get_current_trip_id()
    print(f"当前行程ID: {trip_id}")  # 添加打印语句
    trip = context_manager.get_current_trip()
    if not trip:
        return "⚠️ 当前未选择行程"

    # 构建元数据部分
    metadata = trip['metadata']
    output = (
        f"🌟 {metadata['title']} 🌟\n"
        f"──────────────────────────\n"
        f"📍 目的地: {metadata['destination']}\n"
        f"📅 行程天数: {metadata['total_days']}天"
    )
    
    # 处理可选元数据
    if 'start_date' in metadata:
        output += f" | 开始日期: {metadata['start_date']}"
    output += "\n"
    
    if 'target_audience' in metadata:
        output += f"👥 适合人群: {metadata['target_audience']}\n"
    output += f"🆔 行程ID: {metadata['trip_id']}\n\n"

    # 构建每日行程部分
    for day in trip["daily_itinerary"]:
        output += f"\n🗓️ 第{day['day']}天"
        
        # 添加日期（如果存在）
        if 'date' in day:
            output += f" | {day['date']}"
        output += "\n"
        
        # 添加酒店信息（如果存在）
        if 'hotel' in day and day['hotel']:
            hotel = day['hotel']
            hotel_info = f"🏨 入住酒店: {hotel['name']} "
            if 'price' in hotel:
                hotel_info += f"(¥{hotel['price']}/晚"
                if 'numbed' in hotel:
                    hotel_info += f" | {hotel['numbed']}张床"
                hotel_info += ")"
            output += hotel_info + "\n"
        
        output += "──────────────────────────\n"

        for schedule in day["schedule"]:
            # 通用POI信息
            poi_str = f"📍 {schedule['name']}\n"
            
            # 添加时间信息（如果存在）
            if 'time' in schedule and schedule['time'] and 'start' in schedule['time'] and 'end' in schedule['time']:
                poi_str = (
                    f"⏰ {schedule['time']['start']}-{schedule['time']['end']} "
                    f"({schedule.get('duration_hours', '未指定')}小时) | "
                ) + poi_str
            
            # 添加POI类型信息（如果存在）
            if 'poi_type' in schedule:
                poi_type = schedule['poi_type']
                
                # 根据POI类型添加特色信息
                if poi_type == "景点" or poi_type == "属景点":
                    poi_str += f"   🏛️ 类型: {schedule.get('type', '未分类')} | "
                    poi_str += f"门票: ¥{schedule.get('price', '免费')}\n"
                    
                    if 'notes' in schedule and schedule['notes']:
                        poi_str += f"   📌 提示: {' | '.join(schedule['notes'])}\n"

                elif poi_type == "餐厅":
                    poi_str += f"   🍽️ 菜系: {schedule.get('cuisine', '未指定')} | "
                    if 'price' in schedule:
                        poi_str += f"人均: ¥{schedule['price']}\n"
                    else:
                        poi_str += "人均: 未指定\n"
                    
                    if 'recommended_food' in schedule and schedule['recommended_food']:
                        poi_str += f"   🍴 推荐菜: {', '.join(schedule['recommended_food'])}\n"

                elif poi_type == "购物":
                    poi_str += f"   🛍️ 类型: {schedule.get('type', '未分类')}\n"
            
            # 添加自定义的交通方式信息（从Android端传过来的）
            if 'transport' in schedule and schedule['transport']:
                poi_str += f"   🚗 交通方式: {schedule['transport']}\n"
                
            # 添加坐标信息（如果存在）
            if 'coordinates' in schedule:
                lat, lon = schedule['coordinates']
                poi_str += f"   📍 位置: {lat}, {lon}\n"
            elif 'latitude' in schedule and 'longitude' in schedule:
                poi_str += f"   📍 位置: {schedule['latitude']}, {schedule['longitude']}\n"
                
            # 添加地址信息（如果存在）
            if 'address' in schedule and schedule['address']:
                poi_str += f"   🏢 地址: {schedule['address']}\n"

            output += poi_str + "\n"

    return output
