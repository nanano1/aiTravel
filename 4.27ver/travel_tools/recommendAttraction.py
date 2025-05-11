from langchain.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
import json
import os
import pandas as pd
from geopy.distance import geodesic
from datetime import datetime
import math
import re
from agent.app_context import AppContext
app_context=AppContext.get_instance()
qwen_llm=app_context.llm


# 意图提取模板
extract_prompt = PromptTemplate.from_template("""
    # Role: 智能旅行助手信息提取专家

    # 背景信息
    当前行程计划概览：
    {itinerary_summary}

    可用POI类型列表：
    [
            "游乐园/体育娱乐", "人文景观", "文化旅游区", "博物馆/纪念馆",
            "商业街区", "其它", "历史古迹", "公园",
            "美术馆/艺术馆", "红色景点", "自然风光", "大学校园"
    ]

    # 任务
    请从用户输入中同时提取以下两类信息：
    1. 需要替换的POI详情
    2. 用户的新偏好要求

    # 用户原始输入
    "{user_input}"

    # 提取要求
    ## 第一部分：被替换POI信息
    - 确定修改的是第几天（day）
    - 明确要替换的是景点还是餐厅（poi_type）
    - 准确识别要替换的具体名称（name）
    - 分析用户的替换原因（reason）
    - 提取前一个景点的名称和经纬度
    - 提取后一个景点的名称和经纬度

    ## 第二部分：偏好要求
    - 预算（budget）：提取明确的数字金额，没有则填null
    - 类型偏好（type）：如果用户没有提供什么偏好,则填null,如果有的话必须匹配可用类型列表中的选项

    # 输出格式
    请严格按以下JSON格式回答，不要添加任何其他内容,也不要解释和注释：
    {{
    "city": "beijing",
    "target_poi": {{
        "day": 3,             
        "poi_type": "景点",
        "name": "故宫",    
        "reason": "用户提到的原因",
        "coordinates": [39.9163, 116.3972], 
        "time": {{
            "start": "09:00", 
            "end": "17:00"   
        }},
        "duration_hours": 2.5, 
    }},
    "preferences": {{
        "budget": 500,       
        "type": "商业街区", 
    }},
    "previous_poi": {{
        "name": "前一个景点名称",
        "coordinates": [39.9163, 116.3972], 
    }},
    "next_poi": {{
        "name": "后一个景点名称",
        "coordinates": [39.9163, 116.3972],
    }},
    "missing_info": false 
    }}

""")


def extract_modification_intent(user_input: str, trip_json: dict) -> dict:
    summary = ""
    city=trip_json['metadata']['destination']
    summary += f"\n当前访问城市：{city}\n"
    for day in trip_json["daily_itinerary"]:
        summary += f"\n第{day['day']}天：\n"
        for idx, poi in enumerate(day["schedule"]):
            # 提取游览时间和经纬度
            visit_time = f"{poi['time']['start']} - {poi['time']['end']}"  # 游览时间
            coordinates = poi['coordinates']  # 获取经纬度
            duration = poi.get('duration_hours', '未知时长')  # 获取游览时长
            
            summary += (f"  {idx}. {poi['name']}（{poi.get('poi_type', '未知类型')}）"
                         f" 游览时间: {visit_time}，时长: {duration}小时，coordinates: [{coordinates[0]}, {coordinates[1]}]\n") 
    print(summary)
    prompt_text = extract_prompt.format(
        user_input=user_input,
        itinerary_summary=summary
    )
    try:
        response = qwen_llm.predict(prompt_text).strip()
        # 尝试从响应中提取 JSON 部分
        json_start = response.find('{')
        json_end = response.rfind('}') + 1
        if json_start != -1 and json_end != -1:
            json_str = response[json_start:json_end]
            parsed = json.loads(json_str)
            parsed["city"] = city
            # ✅ 添加意图完整性标志（关键！）
            target = parsed.get("target_poi", {})
            if not target.get("name") or not target.get("day"):
                parsed["missing_info"] = True
            else:
                parsed["missing_info"] = False
            return parsed

    except Exception as e:
        print(f"[意图提取失败] {e}\n原始返回:\n{response}")
        return {
            "city": trip_json['metadata']['destination'],
            "target_poi": {"name": None, "poi_type": "unknown", "day": None},
            "preferences": {},
            "missing_info": True
        }



def is_time_compatible(original_poi, recommended_poi):
    """
    检查原POI和推荐POI的时间兼容性
    """

    if isinstance(recommended_poi, tuple):
        recommended_poi = {
            "id": recommended_poi[0],
            "name": recommended_poi[1],
            "type": recommended_poi[2],
            "lat": recommended_poi[3],
            "lon": recommended_poi[4],
            "opentime": recommended_poi[5],
            "endtime": recommended_poi[6],
            "price": recommended_poi[7],
            "recommendmintime": recommended_poi[8],
            "recommendmaxtime": recommended_poi[9]
        }

    original_poi["time"]["start"] = original_poi["time"]["start"].replace('24:00', '00:00')
    original_poi["time"]["end"] = original_poi["time"]["end"].replace('24:00', '00:00')

    def parse_time(time_str):
        try:
            return datetime.strptime(time_str, "%H:%M")
        except ValueError:
            print(f"无法解析时间: {time_str}")
            return None

    # 解析原 POI 的时间
    original_start = parse_time(original_poi["time"]["start"])
    original_end = parse_time(original_poi["time"]["end"])
    if original_start is None or original_end is None:
        return False
    original_duration = original_poi["duration_hours"]

    # 解析推荐 POI 的时间
    recommended_start = parse_time(recommended_poi["opentime"])
    recommended_end = parse_time(recommended_poi["endtime"].replace('24:00', '00:00'))
    if recommended_start is None or recommended_end is None:
        return False
    recommended_min_duration = recommended_poi["recommendmintime"]
    recommended_max_duration = recommended_poi["recommendmaxtime"]


    # 检查原 POI 的游览时间是否在推荐 POI 的开放时间范围内
    if original_start < recommended_start or original_end > recommended_end:
        return False

    # 检查原 POI 的游览时长是否在推荐 POI 的最小和最大游览时长范围内
    if original_duration < recommended_min_duration or original_duration > recommended_max_duration:
        return False

    return True

def is_geographically_compatible(original_poi, recommended_poi, max_distance):
    if isinstance(recommended_poi, tuple):
        recommended_poi = {
            "id": recommended_poi[0],
            "name": recommended_poi[1],
            "type": recommended_poi[2],
            "lat": recommended_poi[3],
            "lon": recommended_poi[4],
            "opentime": recommended_poi[5],
            "endtime": recommended_poi[6],
            "price": recommended_poi[7],
            "recommendmintime": recommended_poi[8],
            "recommendmaxtime": recommended_poi[9]
        }
    original_lat = original_poi["coordinates"][0]
    original_lon = original_poi["coordinates"][1]
    recommended_lat = recommended_poi["lat"]
    recommended_lon = recommended_poi["lon"]

    distance = haversine_distance(original_lat, original_lon, recommended_lat, recommended_lon)
    return distance <= max_distance

def haversine_distance(lat1, lon1, lat2, lon2):
    # 将经纬度从度转换为弧度
    lat1, lon1, lat2, lon2 = map(math.radians, [lat1, lon1, lat2, lon2])

    # Haversine 公式
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    a = math.sin(dlat / 2) ** 2 + math.cos(lat1) * math.cos(lat2) * math.sin(dlon / 2) ** 2
    c = 2 * math.asin(math.sqrt(a))
    r = 6371  # 地球半径，单位为公里
    return c * r

def get_first_itinerary(file_path: str = 'data/sample_trip.json'):
    """
    从指定的JSON文件中读取并返回第一个行程信息
    Returns:
        dict: 第一个行程的完整信息，包含metadata和daily_itinerary
        如果文件不存在或格式错误，返回None
    """
    try:
        # 打开并读取JSON文件
        with open(file_path, 'r', encoding='utf-8') as f:
            trip_data = json.load(f)
            
        # 获取第一个行程
        if trip_data and 'travel_itineraries' in trip_data and trip_data['travel_itineraries']:
            return trip_data['travel_itineraries'][1]
        else:
            print("未找到有效的行程数据")
            return None
            
    except FileNotFoundError:
        print(f"未找到文件: {file_path}")
        return None
    except json.JSONDecodeError:
        print(f"JSON文件格式错误: {file_path}")
        return None
    except Exception as e:
        print(f"读取行程信息时发生错误: {e}")
        return None

def filter_poi_candidates(original_poi, poi_data, preferences=None, max_distance=10):
    """
    综合筛选POI候选列表，优先考虑用户偏好，然后验证时间和地理兼容性
    
    参数:
    - original_poi: 原始POI信息，包含时间、坐标等
    - poi_data: 候选POI列表         
    - preferences: 用户偏好字典，包含budget和type等字段
    - max_distance: 最大地理距离(公里)
    
    返回:
    - 筛选后的POI列表
    """
    
    filtered_pois = []
    
    # 打印原始POI信息
    print(f"原始POI: {original_poi['name']}，类型: {original_poi.get('poi_type', '未知类型')}")
    print(f"坐标: {original_poi['coordinates']}，时间: {original_poi['time']['start']} - {original_poi['time']['end']}")
    
    # 首先检查是否有用户偏好
    has_preferences = preferences and (preferences.get("budget") is not None or 
                                     (preferences.get("type") and preferences.get("type") != "unknown"))
    
    print(f"用户偏好: {'有' if has_preferences else '无'}")
    if has_preferences:
        print(f"预算: {preferences.get('budget')}, 类型: {preferences.get('type')}")
    
    # 遍历所有候选POI
    for poi in poi_data:
        # 标准化POI格式（处理元组或字典两种情况）
        if isinstance(poi, tuple):
            poi_dict = {
                "id": poi[0],
                "name": poi[1],
                "type": poi[2],
                "lat": poi[3],
                "lon": poi[4],
                "opentime": poi[5],
                "endtime": poi[6],
                "price": poi[7],
                "recommendmintime": poi[8],
                "recommendmaxtime": poi[9]
            }
        else:
            poi_dict = poi
        
        
        # 第一步：时间兼容性检查（基础筛选，无论是否有偏好都需要）
        if not is_time_compatible(original_poi, poi_dict):
            continue
        
        # 第二步：地理位置兼容性检查（基础筛选，无论是否有偏好都需要）
        if not is_geographically_compatible(original_poi, poi_dict, max_distance):
            continue
        
        # 第三步：如果有用户偏好，进行偏好筛选
        if has_preferences:
            # 预算筛选
            budget = preferences.get("budget")
            if budget is not None:
                if poi_dict["price"] > budget:
                        continue
            
            # 类型筛选
            poi_type = preferences.get("type")
            if poi_type and poi_type != "unknown":
                if poi_dict["type"] != poi_type:
                    continue
        
        # 通过所有筛选，添加到结果列表
        filtered_pois.append(poi_dict)
    
    # 按照距离排序（从近到远）
    if filtered_pois:
        filtered_pois.sort(key=lambda x: haversine_distance(
            original_poi["coordinates"][1], original_poi["coordinates"][0], 
            x["lat"], x["lon"]
        ))
    
    print(f"\n筛选完成，共找到{len(filtered_pois)}个符合条件的POI")
    return filtered_pois

def main():
    """
    确定当前需要被替换的类型
    """
    last_user_msg=("我要换掉明孝陵")
    trip = get_first_itinerary()
    if not trip:
        return "⚠️ 当前未选择行程"

    # 提取用户意图
    intent = extract_modification_intent(last_user_msg, trip)
    print("识别结果:", intent)

    target_poi = intent.get("target_poi", {})
    preferences = intent.get("preferences", {"budget": None, "type": "unknown"})

    city = trip["metadata"]["destination"]  # 从trip中获取城市
    file_path = os.path.join('data', 'database', 'attractions', city, 'attractions.csv')
    poi_data = pd.read_csv(file_path)

    # 过滤候选POI
    filtered_pois = filter_poi_candidates(target_poi, poi_data.itertuples(index=False), preferences)
    print("过滤后的候选POI:", filtered_pois[:10])



if __name__ == "__main__":
    main()

