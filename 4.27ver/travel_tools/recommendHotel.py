import re   
import json
from langchain_core.prompts import PromptTemplate
from langchain_openai import ChatOpenAI
from math import radians, sin, cos, sqrt, atan2, degrees

import requests

qwen_llm=ChatOpenAI(
            api_key="sk-",
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
            model="qwen2.5-14b-instruct-1m",
            temperature=0.3,
        )

def calculate_center_and_radius(coord1: list, coord2: list) -> dict:
    """
    计算两个经纬度坐标之间的中点和半径
    
    Args:
        coord1: 第一个点的坐标 [纬度, 经度]
        coord2: 第二个点的坐标 [纬度, 经度]
    
    Returns:
        dict: 包含中点坐标和半径的字典
        {
            'center': [中点纬度, 中点经度],
            'radius': 半径（单位：公里）
        }
    """
    # 从列表中获取经纬度
    lat1, lon1 = coord1
    lat2, lon2 = coord2
    
    # 将经纬度转换为弧度
    lat1, lon1, lat2, lon2 = map(radians, [lat1, lon1, lat2, lon2])
    
    # 计算中点
    Bx = cos(lat2) * cos(lon2 - lon1)
    By = cos(lat2) * sin(lon2 - lon1)
    
    mid_lat = atan2(sin(lat1) + sin(lat2),
                    sqrt((cos(lat1) + Bx) * (cos(lat1) + Bx) + By * By))
    mid_lon = lon1 + atan2(By, cos(lat1) + Bx)
    
    # 计算距离（使用 Haversine 公式）
    R = 6371  # 地球平均半径（单位：公里）
    
    dlat = lat2 - lat1
    dlon = lon2 - lon1
    
    a = (sin(dlat/2) * sin(dlat/2) +
         cos(lat1) * cos(lat2) * sin(dlon/2) * sin(dlon/2))
    c = 2 * atan2(sqrt(a), sqrt(1-a))
    distance = R * c
    
    # 将中点坐标从弧度转换回角度
    mid_lat = degrees(mid_lat)
    mid_lon = degrees(mid_lon)
    
    return {
        'center': [round(mid_lat, 6), round(mid_lon, 6)],  # 返回列表格式，保持与输入一致
        'radius': round(distance/2, 2)  # 半径为距离的一半
    }

getResInfo_prompt = PromptTemplate.from_template("""
    请作为行程分析助手，严格按以下步骤处理输入信息：
    # Role: 提取行程结构化信息
    
    # 用户原始输入
    "{user_input}"
    
    # 行程单概述
    "{itinerary_summary}"
    # 提取要求
    ## 被替换POI信息target_hotel
    - 准确识别要替换的具体名称（name）
    - 经纬度（coordinates）,经纬度一定要准确,根据行程单中每个poi对应的经纬度
    - 价格（price）,价格根据行程单中对应的酒店价格
    ## 目标酒店附近景点的坐标
    - 如果说只有第2天的时候住这个酒店,那么就从itinerary_summary中获取2天的最后一个景点prev_poi的坐标和3天第一个景点的坐标也就是next_poi,如果x是最后一天的话,那么就获取x-1天的酒店坐标和x天的最后一个景点next_poi的坐标.
    - 如果说有连续几天都住了这个酒店,比如1和2天都住这个酒店,那么prev_poi就是1天的最后一个景点,next_poi是3天的第一个景点
    ## 输出格式要求
    # 输出格式要求,严格按以下格式输出,不要添加任何注释和解释
```
{{
    "target_hotel": {{
        "hotel_info"{{
            "name": "赛特饭店",
            "coordinates": [39.906667, 116.441675],
            "price": 678.0,
        }},
        "prev_poi": {{
            "name": "八达岭长城",
            "coordinates": [39.915, 116.404]
            }},
        "next_poi": {{
            "name": "钟山风景名胜区",
            "coordinates": [39.915, 116.404]
        }}
    }}
}}  
""")

getResPreference_prompt=PromptTemplate.from_template("""
请作为行程分析助手，提取用户输入中可能存在的对酒店的偏好要求,如果没有的项目就返回null

1. 输入解析：
用户输入: "{user_input}"
原本偏好:"{preference_text}"
检查用户输入有没有补充新的其中一个偏好,或者更新原有的偏好,如果有,则更新,如果没有,则返回原本的偏好
    # 提取要求

1. 偏好special_requirement | 提取用户可能提到的地区,或者停车场等,可以提取 | "我想选择东城区的酒店" → "东城区" |
2. 类型type,用户可能会告诉你他们的预算范围（可能是明确的数字，也可能是模糊表述如"预算不高"），
你的任务是根据预算范围推测适合的酒店类型，或者说从用户的预算,或者直接指明类型,提取类型
可选的酒店类型有:星级酒店,经济型,舒适型,高档型,豪华型,连锁酒店,快捷酒店,商务酒店,公寓式酒店,民宿,客栈,青年旅舍,招待所,家庭旅馆,电竞酒店
- 预算 0-200 元：青年旅舍 / 招待所 / 家庭旅馆 / 民宿
- 预算 200-400 元：经济型酒店 / 连锁酒店 / 快捷酒店
- 预算 400-700 元：舒适型酒店 / 商务酒店 / 三星级酒店
- 预算 700-1200 元：高档型酒店 / 四星级酒店 / 公寓式酒店 / 温泉酒店
- 预算 1200 元以上：豪华型酒店 / 五星级酒店
最多可以选择三种类型
3. rating,用户可能会提到对酒店的评分要求,一共有三种,包括health_score,overall_rating,default,比如"我想选择评分高的酒店",那么就返回"overall_rating",比如"我想选择卫生好的酒店",那么就返回"health_score",如果用户没有提到,那么就返回"default"
如果用户没有提到，对应返回null即可"
## 输出格式要求,严格按以下格式输出,不要添加任何注释和解释
```
{{
  "preference_analysis": {{
    "special_requirement": "东城区",
    "type": ["推荐酒店类型1", "推荐酒店类型2", ...],
    "rating": "overall_rating"
    }}
}}

""")

def get_itinerary_summary(trip_json: dict) -> str:
    """生成行程摘要（用于提示LLM上下文）"""
    output = ""
    for day in trip_json["daily_itinerary"]:
        output += f"\n第{day['day']}天：\n"
        # 打印酒店信息
        if 'hotel' in day:
            hotel = day['hotel']
            output += f"  酒店: {hotel['name']}（价格: {hotel.get('price', '未知')}元）\n"
            if 'coordinates' in hotel:
                output += f"     位置: [{hotel['coordinates'][0]}, {hotel['coordinates'][1]}]\n"
            else:
                output += f"     位置: [未知, 未知]\n"
        for idx, poi in enumerate(day["schedule"]):
            # 基本信息
            output += f"  {idx}. {poi['name']}（{poi.get('poi_type', '未知类型')}）\n"
            if 'coordinates' in poi:
                output += f"     位置: [{poi['coordinates'][0]}, {poi['coordinates'][1]}]\n"
            # 如果是餐厅，添加更多详细信息
            if poi.get('poi_type') == '餐厅':
                output += f"     时间: {poi['time']['start']}-{poi['time']['end']}\n"
                output += f"     价格: {poi.get('price', '未知')}元\n"
                output += f"     菜系: {poi.get('cuisine', '未知')}\n"
                # 如果有推荐菜品
                if poi.get('recommended_food'):
                    output += f"     推荐菜品: {', '.join(poi['recommended_food'])}\n"
                if 'duration_hours' in poi:
                    output += f"     建议游览时长: {poi['duration_hours']}小时\n"
                output += "\n"
    print(output)
    return output

def extract_json_from_response(response: str) -> dict:
    """
    从响应字符串中提取 JSON 部分。

    Args:
        response: 包含 JSON 的响应字符串。

    Returns:
        dict: 提取的 JSON 对象，如果提取失败则返回空字典。
    """
    try:
        json_start = response.find('{')
        json_end = response.rfind('}') + 1
        if json_start != -1 and json_end != -1:
            json_str = response[json_start:json_end]
            return json.loads(json_str)
    except Exception as e:
        print(f"[提取JSON失败] {response}")
        return {
            "target_restaurant": {
                "day": 1,
                "name": "未识别",
                "price": None,
                "cuisine": "未知",
                "time": "未知"
            },
            "reasons": []
        }
    
def get_res_intent(user_input: str, trip_json: dict) -> dict:
    summary = get_itinerary_summary(trip_json)
    
    resInfo_prompt = getResInfo_prompt.format(
        user_input=user_input,
        itinerary_summary=summary
    )
    resInfo = qwen_llm.predict(resInfo_prompt).strip()
    resInfo_json = extract_json_from_response(resInfo)
    return resInfo_json

def get_res_preference(user_input: str,preferences:dict) -> dict:
    if preferences is None:
        preference_text = "原本没有偏好,这是第一次提取偏好" 
    else:
        preference_text = ""
        preference_text += f"budget:{preferences.get('budget')}cuisine_preference:{preferences.get('cuisine_preference')} special_requirement{preferences.get('special_requirement', '无标签')} - rating{preferences.get('rating', '未知')}\n"

    preference_prompt = getResPreference_prompt.format(
        user_input=user_input,
        preference_text=preference_text
    )
    preference = qwen_llm.predict(preference_prompt).strip()
    preference_json = extract_json_from_response(preference)
    return preference_json

def extract_res_intent(user_input: str, trip_json: dict) -> dict:
    resInfo = get_res_intent(user_input, trip_json)
    preference = get_res_preference(user_input,None)
    merged_json = {**resInfo, **preference}  # 合并字典

    print("提取的意图",merged_json)
    return merged_json

def search_restaurants_baidu(center: list, radius: float, preferences=None) -> list:
    """
    根据中心点和半径搜索周边餐厅
    """
    type = preferences.get("type")
    print("进入search_restaurants_baidu的preferences",preferences)
    location_string=f"{center[0]},{center[1]}"
    filter_string=f'industry_type:hotel|sort_name:{preferences.get("rating")}|sort_type:1'
    results_list = []
    for t in type:
        ak = 'UIOBPAhprxZdUqc2lufTIKy7nYMNh3mv'
        url = 'https://api.map.baidu.com/place/v2/search'
        params = {
            'query': '酒店',  # 检索关键字，可修改为其他内容
            'location': location_string,  # 圆形区域检索中心点，可修改
            'radius': 2000,  # 圆形区域检索半径，单位为米，可修改
            'output': 'json',  # 输出格式为json
            'ak': ak,
            'tag': t,
            'scope': '2',
            'filter': filter_string
        }
        try:
            response = requests.get(url, params=params)
            if response.status_code == 200:
                result = response.json()
                if result['status'] == 0:
                    pois = result['results']
                    for poi in pois:
                        # 提取所需信息
                        uid = poi.get('uid', '无uid')
                        name = poi.get('name', '无名称')
                        detail_info=poi.get('detail_info', '无详情')
                        price = detail_info.get('price', '无价格')
                        classified_poi_tag=detail_info.get('classified_poi_tag', '无分类')
                    # 构建总结字符串
                        results_list.append({
                            "uid": uid,
                            "name": name,
                            "price": price,
                            "label": classified_poi_tag
                        })
                else:
                    print(f"请求失败，错误信息: {result['message']}")
            else:
                print(f"请求失败，状态码: {response.status_code}")
        except requests.RequestException as e:
            print(f"请求发生异常: {e}")
        print(results_list)

    return results_list



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
            return trip_data['travel_itineraries'][0]
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


def search_restaurants_by_center_radius(center: list, radius: float) -> list:
    """
    根据中心点和半径搜索周边餐厅
    
    Args:
        center: 中心点坐标 [纬度, 经度]
        radius: 搜索半径（公里）
    
    Returns:
        list: 餐厅列表，每个餐厅包含名称、评分、价格等信息
    """
    url = "https://restapi.amap.com/v3/place/around"
    
    # 将公里转换为米
    radius_meters = int(radius * 1000)
    
    # 将坐标转换为高德地图API所需的格式 "经度,纬度"
    location = f"{center[1]},{center[0]}"
    
    params = {
        "key": "d477f25785fee6455f468f4702ff7bd5",
        "location": location,
        "radius": radius_meters,
        "keywords": "餐馆",
        "types": "050000",
        "offset": 25,
        "page": 1,
        "extensions": "all",
        "show_fields": "business"
    }

    try:    
            response = requests.get(url, params=params)
            print(f"响应状态码: {response.status_code}")
            print(f"响应内容: {response.text}")

            if response.status_code == 200:
                result = response.json()
                if result.get("status") == "1":
                    pois = result.get("pois", [])
                    if not pois:
                        print("没有找到符合条件的餐馆。你可以尝试调整搜索范围或更换中心点。")
                    for poi in pois:
                        biz_ext = poi.get('biz_ext', {})
                        rating = biz_ext.get('rating', '无评分')
                        print(f"名称: {poi['name']}, 评分: {rating}")
                else:
                    print(f"请求成功，但返回结果状态错误: {result}")
            else:
                print(f"请求失败，状态码: {response.status_code}")
    except requests.RequestException as e:
        print(f"请求异常: {e}")
def generate_res_recommendation(target_restaurant: dict, preference_analysis=None):
    prev_poi = target_restaurant.get('prev_poi', {})
    next_poi = target_restaurant.get('next_poi', {})
    print(prev_poi.get('coordinates'), next_poi.get('coordinates'))
    geo = calculate_center_and_radius(prev_poi.get('coordinates'), next_poi.get('coordinates'))
    print("进入generate_res_recommendation的preference_analysis",preference_analysis)
    print(f"两点中心坐标: {geo['center']}")
    print(f"搜索半径: {geo['radius']}公里")
    result=search_restaurants_baidu(geo['center'], geo['radius'], preference_analysis)
    return result


def rerocommend_byllm(candidate_pois: dict, preference_analysis: dict):
    pois_text = ""
    for poi in candidate_pois:
        pois_text += f"{poi.get('uid')}. {poi.get('name')} - {poi.get('label', '无标签')} "

    preference_text = f"用户提到的特殊偏好是{preference_analysis.get('special_requirement', '无偏好')},用户提到的酒店类型是{preference_analysis.get('type', '无类型')}"

    prompt_template = PromptTemplate.from_template("""
    你是一个智能餐厅推荐助手。根据用户的用餐偏好，帮他从以下候选餐厅中选择5家最符合的，并给出每家餐厅的推荐理由。

    用户偏好：
    {preference_text}

    候选餐厅列表：
    {pois_text}

    要求输出以下JSON格式，不要添加其他文字：
    [
    {{
        "uid": "餐厅ID",
        "name": "餐厅名称",
        "reason": "推荐理由"
    }},
    ...
    ]
    """)

    llm_input = prompt_template.format(
        preference_text=preference_text,
        pois_text=pois_text
    )

    print("[Debug] LLM 输入:", llm_input)

    # 第三步：调用LLM
    llm_response = qwen_llm.predict(llm_input).strip()

    # 尝试解析JSON
    try:
        json_start = llm_response.find('[')
        json_end = llm_response.rfind(']') + 1
        if json_start != -1 and json_end != -1:
            recommendations = json.loads(llm_response[json_start:json_end])
        else:
            raise ValueError("未能正确解析LLM返回的JSON")
    except Exception as e:
        print(f"解析LLM推荐结果出错: {e}")
        recommendations = []
    return recommendations

def main():
    first_trip = get_first_itinerary()
    result = extract_res_intent(user_input="我要换掉第二晚的酒店,预算大约200元", trip_json=first_trip)
    
    target_hotel = result.get('target_hotel', {})
    prev_poi = target_hotel.get('prev_poi', {})
    next_poi = target_hotel.get('next_poi', {})
    preference_analysis = result.get('preference_analysis', {})
    print(preference_analysis)

    geo = calculate_center_and_radius(prev_poi['coordinates'], next_poi['coordinates'])
    
    print(f"两点中心坐标: {geo['center']}")
    print(f"搜索半径: {geo['radius']}公里")
    result=search_restaurants_baidu(geo['center'], geo['radius'], preference_analysis)
    print(result)
    recommendations=rerocommend_byllm(result, preference_analysis)
    print(recommendations)


if __name__ == "__main__":
    main()




