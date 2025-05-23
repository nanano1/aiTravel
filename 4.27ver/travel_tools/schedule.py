import requests
import time
import numpy as np
import json
from langchain.prompts import PromptTemplate
from agent.app_context import AppContext
from typing import List, Dict
import math
from sklearn.cluster import KMeans

def get_total_attractions(trip_json):
    """计算行程中的总景点数
    
    Args:
        trip_json: 行程JSON数据
        
    Returns:
        int: 总景点数
    """
    # 处理trip_json为None的情况
    if trip_json is None:
        print("警告: 行程数据为None，返回默认值0")
        return 0
        
    try:
        total_attractions = 0
        for day in trip_json.get("daily_itinerary", []):
            attractions_count = sum(1 for item in day.get("schedule", []) if item.get("poi_type") == "景点")
            total_attractions += attractions_count
        return total_attractions
    except (KeyError, TypeError, AttributeError) as e:
        print(f"计算景点数量出错: {e}")
        return 0

def calculate_poi_change(existing_pois, current_days, target_days, max_per_day):
    expected_total = target_days * max_per_day
    diff = expected_total - len(existing_pois)
    return diff

def search_pois_by_center(center_poi, radius=4, limit=20):
    """
    根据中心POI坐标搜索周边景点
    
    Args:
        center_poi: 中心点坐标 [纬度, 经度]
        radius: 搜索半径（公里），默认4公里
        limit: 返回景点数量限制，默认10个
    
    Returns:
        list: 景点列表
    """
    url = "https://restapi.amap.com/v5/place/around"
    
    # 将坐标格式转换为高德地图API需要的格式：经度,纬度
    location = f"{center_poi[1]},{center_poi[0]}"
    
    params = {
        "key": "d477f25785fee6455f468f4702ff7bd5",
        "location": location,
        "radius": radius * 1000,  # 转换为米
        "keywords": "景点",
        "types": "110000",  # 景点类型代码
        "offset": limit,  # 每页记录数
        "page": 1,
        "extensions": "all",
        "show_fields": "business"
    }

    result_pois = []
    
    try:    
        response = requests.get(url, params=params)
        
        if response.status_code == 200:
            result = response.json()
            if result.get("status") == "1":
                pois = result.get("pois", [])
                
                for poi in pois:
                    poi_type = poi.get('type', '无类型')
                    biz_ext = poi.get('business', {})
                    rating = biz_ext.get('rating', '无评分')
                    
                    # 提取经纬度
                    location = poi.get('location', '')
                    coordinates = None
                    if location:
                        lng, lat = location.split(',')
                        coordinates = [float(lat), float(lng)]
                    
                    poi_info = {
                        "poi_id": poi.get('id'),
                        "name": poi.get('name'),
                        "type": poi_type,
                        "rating": rating,
                        "coordinates": coordinates,
                        "address": poi.get('address', '无地址'),
                        "distance": poi.get('distance', '未知'),
                        "tel": biz_ext.get('tel', '无电话'),
                        "opentime_week": biz_ext.get('opentime_week', '无营业时间'),
                    }
                    
                    result_pois.append(poi_info)
                
                # 添加请求延迟，避免QPS限制    
                time.sleep(1.0)  # 每次搜索景点后等待1秒
                return result_pois
            else:
                print(f"请求成功，但返回结果状态错误: {result}")
        else:
            print(f"请求失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"搜索景点时发生异常: {e}")
    
    return result_pois

def calculate_distance(origin_poi, destination_poi, type=1):
    """
    计算两个POI之间的距离和预计时间
    
    Args:
        origin_poi: 起点坐标 [纬度, 经度]
        destination_poi: 终点坐标 [纬度, 经度]
        type: 计算方式，0-直线距离，1-驾车导航距离，3-步行规划距离
    
    Returns:
        dict: 包含距离(米)和时间(秒)的字典
    """
    url = "https://restapi.amap.com/v3/distance"
    
    # 高德地图API要求的坐标格式：经度,纬度
    origin = f"{origin_poi[1]},{origin_poi[0]}"
    destination = f"{destination_poi[1]},{destination_poi[0]}"
    
    params = {
        "key": "d477f25785fee6455f468f4702ff7bd5",
        "origins": origin,
        "destination": destination,
        "type": type,
        "output": "json"
    }
    
    try:
        response = requests.get(url, params=params)
        
        if response.status_code == 200:
            result = response.json()
            if result.get("status") == "1":
                # 提取距离和时间
                results = result.get("results", [])
                if results:
                    distance = int(results[0].get("distance", 0))
                    duration = int(results[0].get("duration", 0))
                    return {
                        "distance": distance,  # 单位：米
                        "duration": duration,  # 单位：秒
                        "origin": origin_poi,
                        "destination": destination_poi
                    }
            else:
                print(f"请求成功，但返回结果状态错误: {result}")
        else:
            print(f"请求失败，状态码: {response.status_code}")
    except Exception as e:
        print(f"计算距离时发生异常: {e}")
    
    # 请求失败或出错时返回默认值
    return {
        "distance": 0,
        "duration": 0,
        "origin": origin_poi,
        "destination": destination_poi,
        "error": "计算失败"
    }

def batch_calculate_distances(origin_pois, destination_poi, type=1, batch_size=50):
    """
    批量计算多个起点到同一个终点的距离
    
    Args:
        origin_pois: 起点坐标列表，每个坐标为 [纬度, 经度]
        destination_poi: 终点坐标 [纬度, 经度]
        type: 计算方式，0-直线距离，1-驾车导航距离，3-步行规划距离
        batch_size: 每批处理的起点数量，最大100个
    
    Returns:
        list: 包含距离信息的列表
    """
    url = "https://restapi.amap.com/v3/distance"
    
    # 将坐标格式转换为高德地图API需要的格式：经度,纬度
    origins_list = []
    for poi in origin_pois:
        if poi and len(poi) >= 2:
            origins_list.append(f"{poi[1]},{poi[0]}")
    
    destination = f"{destination_poi[1]},{destination_poi[0]}"
    
    # 分批处理，每批最多50个起点（减少每批数量，降低失败风险）
    results = []
    for i in range(0, len(origins_list), batch_size):
        batch_origins = origins_list[i:i+batch_size]
        origins_str = "|".join(batch_origins)
        
        params = {
            "key": "d477f25785fee6455f468f4702ff7bd5",
            "origins": origins_str,
            "destination": destination,
            "type": type,
            "output": "json"
        }
        
        try:
            # 添加更长的间隔时间
            time.sleep(1.5)  # 延长等待时间到1.5秒
            response = requests.get(url, params=params)
            
            if response.status_code == 200:
                result = response.json()
                if result.get("status") == "1":
                    # 提取距离和时间
                    batch_results = result.get("results", [])
                    results.extend(batch_results)
                else:
                    print(f"请求成功，但返回结果状态错误: {result}")
                    # 添加错误重试逻辑
                    retry_count = 0
                    while retry_count < 3 and result.get("status") != "1":
                        # 错误重试，指数退避
                        retry_wait = 2 ** retry_count
                        print(f"等待 {retry_wait} 秒后重试...")
                        time.sleep(retry_wait)
                        retry_count += 1
                        
                        response = requests.get(url, params=params)
                        if response.status_code == 200:
                            result = response.json()
                            if result.get("status") == "1":
                                batch_results = result.get("results", [])
                                results.extend(batch_results)
                                break
                        
                        print(f"重试 {retry_count}/3 失败")
            else:
                print(f"请求失败，状态码: {response.status_code}")
        except Exception as e:
            print(f"批量计算距离时发生异常: {e}")
        
        # 添加延迟以避免QPS限制
        if i + batch_size < len(origins_list):
            time.sleep(2.0)  # 增加到2秒等待时间
    
    return results

def calculate_distances_to_pois(target_poi, existing_pois):
    """
    计算目标POI到已有POI列表中各个景点的距离
    
    Args:
        target_poi: 目标POI字典，包含coordinates键
        existing_pois: 已有POI列表，每个POI包含coordinates键
        
    Returns:
        list: 距离信息列表，每项包含POI名称、距离和时间
    """
    results = []
    target_coordinates = target_poi["coordinates"]
    
    # 筛选有效的POI（具有坐标信息）
    valid_pois = []
    for poi in existing_pois:
        if poi.get("coordinates"):
            valid_pois.append(poi)
    
    # 如果有效POI数量较少，使用单个请求
    if len(valid_pois) <= 1:
        for poi in valid_pois:
            # 计算距离
            distance_info = calculate_distance(target_coordinates, poi["coordinates"])
            
            # 组装结果
            result = {
                "poi_name": poi["name"],
                "rating": poi.get("rating", "无评分"),
                "type": poi.get("type", "无类型"),
                "distance": distance_info["distance"],
                "duration": distance_info["duration"],
                "target_poi": target_poi["name"]
            }
            
            results.append(result)
    else:
        # 使用批量请求
        origin_coords = [poi["coordinates"] for poi in valid_pois]
        batch_results = batch_calculate_distances(origin_coords, target_coordinates)
        
        # 处理批量结果
        for i, res in enumerate(batch_results):
            if i < len(valid_pois):
                result = {
                    "poi_name": valid_pois[i]["name"],
                    "rating": valid_pois[i].get("rating", "无评分"),
                    "type": valid_pois[i].get("type", "无类型"),
                    "distance": int(res.get("distance", 0)),
                    "duration": int(res.get("duration", 0)),
                    "target_poi": target_poi["name"]
                }
                results.append(result)
    
    # 按距离排序
    results.sort(key=lambda x: x["distance"])
    print("calculate_distances_to_pois results",results)
    return results

def normalize_score(value, min_val, max_val):
    """
    将数值归一化到0-1范围
    
    Args:
        value: 要归一化的值
        min_val: 最小值
        max_val: 最大值
        
    Returns:
        float: 归一化后的值(0-1)
    """
    if max_val == min_val:
        return 0.5  # 避免除以零
    
    normalized = (value - min_val) / (max_val - min_val)
    return max(0, min(1, normalized))  # 确保结果在0-1之间

def evaluate_preference_match(poi_intro, prefer_tags, llm_client=None):
    """
    评估POI与用户偏好标签的匹配度
    
    Args:
        poi_intro: POI介绍
        prefer_tags: 用户偏好标签列表
        llm_client: LLM客户端(可选)
        
    Returns:
        float: 偏好匹配度(0-1)
    """
    # 如果没有偏好标签，返回中等匹配度
    if not prefer_tags or len(prefer_tags) == 0:
        return 0.5
    
    # 简单匹配：检查POI类型是否包含任何偏好标签
    poi_intro_lower = str(poi_intro).lower() if poi_intro else ""
    for tag in prefer_tags:
        if tag and str(tag).lower() in poi_intro_lower:
            return 1.0  # 完全匹配
    
    # 如果有LLM客户端，使用LLM进行匹配度评估 (放在简单匹配之后避免不必要的API调用)
    if llm_client and poi_intro:
        try:
            prompt = f"""
            请评估景点"{poi_intro}"与用户偏好标签{prefer_tags}的匹配程度。
            返回一个0到1之间的分数，1表示完全匹配，0表示完全不匹配。
            只需返回一个数字，不要有任何其他文字。
            """
            response = llm_client.predict(prompt).strip()
            try:
                score = float(response)
                print("score",score)
                return max(0, min(1, score))  # 确保结果在0-1之间
            except ValueError:
                print(f"LLM返回的匹配度无法转换为浮点数: {response}")
                # 如果无法转换，继续使用简单匹配
        except Exception as e:
            print(f"使用LLM评估匹配度时出错: {e}")
    
    # 默认中等匹配度
    return 0.5

def batch_calculate_compatibility(pois, existing_pois, max_distance=10000):
    """
    批量计算POI的位置兼容性和平均距离
    
    Args:
        pois: 候选POI列表
        existing_pois: 已有POI列表
        max_distance: 最大距离(米)，用于归一化
        
    Returns:
        dict: 包含位置兼容性分数和平均距离的字典
        - compatibility_scores: 兼容性分数列表
        - avg_distances: 平均距离列表
    """
    # 提取所有现有POI的坐标
    existing_coords = []
    for poi in existing_pois:
        if poi.get("coordinates"):
            existing_coords.append(poi["coordinates"])
    print("existing_coords",existing_coords)
    if not existing_coords:
        # 如果没有现有POI，所有候选POI的兼容性都是中等
        return {
            "compatibility_scores": [0.5] * len(pois),
            "avg_distances": [None] * len(pois)
        }
    
    # 按批次处理，每次处理一个现有POI与所有候选POI
    poi_distances = {}  # 存储每个候选POI到所有现有POI的距离
    
    # 初始化距离列表
    for i, poi in enumerate(pois):
        poi_distances[i] = []
    
    # 对每个现有POI，计算所有候选POI到它的距离
    for existing_coord in existing_coords:
        # 准备所有候选POI的坐标
        candidate_coords = []
        valid_indices = []
        
        for i, poi in enumerate(pois):
            if poi.get("coordinates"):
                candidate_coords.append(poi["coordinates"])
                valid_indices.append(i)
        
        # 批量计算距离
        batch_results = batch_calculate_distances(candidate_coords, existing_coord)
        
        # 处理结果
        for i, result in enumerate(batch_results):
            if i < len(valid_indices):
                poi_idx = valid_indices[i]
                distance = int(result.get("distance", max_distance))
                poi_distances[poi_idx].append(distance)
    print("poi_distances",poi_distances)
    
    # 计算每个候选POI的平均距离，并转换为兼容性分数
    avg_distances = []
    for i in range(len(pois)):
        distances = poi_distances.get(i, [])
        if distances:
            avg_distances.append(np.mean(distances))
        else:
            avg_distances.append(None)  # 标记无数据的POI
    print("avg_distances",avg_distances)
    
    # Step 2: 计算全局最小和最大平均距离
    valid_avg = [d for d in avg_distances if d is not None]
    min_avg = min(valid_avg) if valid_avg else 0
    max_avg = max(valid_avg) if valid_avg else 1
    
    compatibility_scores = []
    for d in avg_distances:
        if d is None:
            compatibility = 0.5  # 默认中等兼容性
        else:
            if max_avg == min_avg:
                normalized = 1.0  # 所有距离相同的情况
            else:
                normalized = (max_avg - d) / (max_avg - min_avg)
            compatibility = normalized
        compatibility_scores.append(compatibility)
    
    return {
        "compatibility_scores": compatibility_scores,
        "avg_distances": avg_distances
    }

def calculate_utility_score(poi, prefer_tags=None, weights=None, llm_client=None, compatibility_data=None, poi_index=None):
    """
    计算POI的效用评分
    
    Args:
        poi: POI字典，包含coordinates、type、rating等键
        prefer_tags: 用户偏好标签列表(可选)
        weights: 权重列表 [偏好匹配权重, 位置兼容权重, 评分权重]
        llm_client: 用于评估偏好匹配的LLM客户端(可选)
        compatibility_data: 包含兼容性分数和平均距离的字典
        poi_index: 当前POI在列表中的索引，用于获取预计算的兼容性分数
        
    Returns:
        tuple: (效用评分, 评分详情字典)
    """
    if weights is None:
        weights = [0.5, 0.3, 0.2]  # 默认权重
    
    # 1. 偏好匹配度
    preference_match = evaluate_preference_match(poi.get("name","")+poi.get("type", ""), prefer_tags, llm_client)
    
    # 2. 位置兼容性
    location_compatibility = compatibility_data["compatibility_scores"][poi_index]
    print("location_compatibility",location_compatibility)
    
    # 3. 评分得分
    rating_score = calculate_rating_score(poi)
    print("rating_score",rating_score)
    
    # 计算加权得分
    utility_score = (
        weights[0] * preference_match + 
        weights[1] * location_compatibility + 
        weights[2] * rating_score
    )
    
    return utility_score, {
        "preference_match": preference_match,
        "location_compatibility": location_compatibility,
        "rating_score": rating_score,
        "avg_distance": compatibility_data["avg_distances"][poi_index]  # 添加平均距离信息
    }

def rank_pois_by_utility(pois, existing_pois, prefer_tags=None, weights=None, llm_client=None, is_reversed=False, exclude_self=False):
    """
    根据效用评分对POI列表进行排序
    
    Args:
        pois: 候选POI列表
        existing_pois: 已有POI列表
        prefer_tags: 用户偏好标签列表(可选)
        weights: 权重列表 [偏好匹配权重, 位置兼容权重, 评分权重]
        llm_client: 用于评估偏好匹配的LLM客户端(可选)
        is_reversed: 是否反转排序。True表示升序（效用分低的在前，用于删除），False表示降序（默认，效用分高的在前，用于添加）
        exclude_self: 在计算兼容性时是否排除自身。用于删除POI场景，每个POI需要与除自身外的其他POI计算兼容性
        
    Returns:
        list: 按效用评分排序的POI列表，每个POI增加utility_score字段
    """
    if not pois:
        return []
    
    if weights is None:
        weights = [0.5, 0.3, 0.2]  # 默认权重
    
    # 如果是删除场景且需要排除自身
    if exclude_self and pois == existing_pois:
        # 计算所有POI的平均距离
        avg_distances = calculate_all_pois_avg_distances(pois)
        # 归一化平均距离
        compatibility_scores = normalize_distances_to_scores(avg_distances)
        
        ranked_pois = []
        
        for i, poi in enumerate(pois):
             # 计算偏好匹配度
            preference_match = evaluate_preference_match(poi.get("name","")+poi.get("type", ""), prefer_tags, llm_client)
            
            # 获取评分得分
            rating_score = calculate_rating_score(poi)
            
            # 获取地理兼容性分数
            location_compatibility = compatibility_scores[i]
           # 计算加权效用分数
            utility_score = (
                weights[0] * preference_match + 
                weights[1] * location_compatibility + 
                weights[2] * rating_score
            )
            
            # 添加评分到POI字典
            poi_with_score = poi.copy()
            poi_with_score["utility_score"] = utility_score
            poi_with_score["score_details"] = {
                "preference_match": preference_match,
                "location_compatibility": location_compatibility,
                "rating_score": rating_score
            }
            
            ranked_pois.append(poi_with_score)
        
        # 对删除场景也需要进行排序
        ranked_pois.sort(key=lambda x: x["utility_score"], reverse=not is_reversed)
    else:
        # 标准场景：添加新POI或不需要排除自身的删除
        # 预先批量计算所有POI的位置兼容性分数和平均距离
        compatibility_data = batch_calculate_compatibility(pois, existing_pois)
        print("compatibility_data", compatibility_data)
        
        ranked_pois = []
        
        for i, poi in enumerate(pois):
            # 计算效用评分
            utility_score, score_details = calculate_utility_score(
                poi, prefer_tags, weights, llm_client,
                compatibility_data, i
            )
            
            # 添加评分到POI字典
            poi_with_score = poi.copy()
            poi_with_score["utility_score"] = utility_score
            poi_with_score["score_details"] = score_details
            
            ranked_pois.append(poi_with_score)
        
        # 按效用评分排序：默认降序（最高分在前），删除场景用升序（最低分在前）
        ranked_pois.sort(key=lambda x: x["utility_score"], reverse=not is_reversed)
    
    return ranked_pois

def extract_intent_from_user_input(user_input: str, trip_json: dict, app_context: AppContext) -> dict:
    """提取用户意图"""
    # 处理trip_json为None的情况
    if trip_json is None:
        print("警告: 行程数据为None，使用默认值")
        base_days = 3  # 默认天数
        daily_pois = 2  # 默认每日景点
    else:
        # 计算原计划天数和每日平均景点数
        try:
            base_days = trip_json["metadata"]["total_days"]
            daily_pois = get_total_attractions(trip_json) / base_days
            print("平均景点数", daily_pois)
        except (KeyError, TypeError, ZeroDivisionError) as e:
            print(f"获取行程数据失败: {e}，使用默认值")
            base_days = 3  # 默认天数
            daily_pois = 2  # 默认每日景点

    # 使用 LLM 提取意图
    intent_prompt = PromptTemplate.from_template("""
    # 角色：智能行程调整解析专家
    ## 背景知识
    当前行程基础数据：
    - 原计划天数：{base_days} 天
    - 原每日平均景点数：{daily_pois} 个/天

    ## 任务
    将用户自然语言请求转换为结构化调整指令，注意以下要点：
    1. 识别用户要的操作是change_days,adjust_pace,change_style,optimize_route中的哪一个
    2. 优先处理明确数字指令（如天数/数量）
    3. 风格替换需保持同类目转换（历史→文化，自然→户外）
    4. 强度描述需量化（"轻松"=减少30%景点）
    5. 如果用户没有提到每天景点的调整,就设置为原本的{daily_pois}.如果用户提到总天数调整,就设置为原本的{base_days}

    ## 输入示例
    用户输入 | 期望输出
    ---|---
    "我不喜欢历史景点" | {{"style_replace": {{"历史": ["文化", "艺术"]}} }}
    "每天别超过2个景点" | {{"daily_limit": 2}}
    "只玩三天" | {{"adjust_type": "change_days", "target_days": 3}}
    "我想轻松点" | {{"daily_limit": 原来的景点数{daily_pois}-1, "prefer_tags": ["自然", "休闲"]}} 
    "我还想去798艺术区" | {{"add_pois": ["798艺术区"], "prefer_tags": ["艺术"]}}

    ## 当前请求处理
    用户输入: "{user_input}"

    请输出严格遵循此结构的 JSON（保留未提及字段为 null）：不要添加任何解释
    {{
    "adjust_type": "change_days/adjust_pace/change_style/optimize_route/invalid", 
    "target_days": 数值/null,
    "daily_limit": 数值,      
    "style_replace": {{"旧标签": ["新标签1", "新标签2"]}}/null,
    "add_pois": [POI名称]/null,
    "prefer_tags": [标签]/null
    }}
    """)

    # 调用 LLM 进行意图提取
    llm_response = app_context.llm.predict(intent_prompt.format(user_input=user_input, base_days=base_days, daily_pois=daily_pois)).strip()
    print("llm_response", llm_response)
    
    # 处理可能的Markdown格式JSON
    import re
    # 检查是否有Markdown代码块标记
    if llm_response.startswith("```") and "```" in llm_response[3:]:
        # 提取代码块内容
        pattern = r"```(?:json)?\s*([\s\S]*?)\s*```"
        match = re.search(pattern, llm_response)
        if match:
            llm_response = match.group(1).strip()
    
    # 解析 LLM 返回的 JSON
    try:
        extracted_info = json.loads(llm_response)
    except json.JSONDecodeError:
        print("解析 LLM 返回的 JSON 失败", llm_response)
        extracted_info = {
            "adjust_type": "invalid",
            "target_days": None,
            "daily_limit": None,
            "style_replace": None,
            "add_pois": None,
            "prefer_tags": None,
            "error": "解析失败"
        }

    # 打印提取的结果
    print("提取的意图:", extracted_info)
    return extracted_info

def generate_recommendation_reason(poi_intro, score_details, prefer_tags, llm_client=None, is_removal=False):
    """
    生成POI的推荐理由或删除理由
    
    Args:
        poi_intro: POI介绍
        score_details: POI的评分详情
        prefer_tags: 用户偏好标签列表
        llm_client: LLM客户端(可选)
        is_removal: 是否为删除场景。True表示生成删除理由，False表示生成推荐理由
        
    Returns:
        str: 推荐理由或删除理由
    """
    print("poi_intro", poi_intro)
    print("prefer_tags", prefer_tags)
    print("score_details", score_details)
    location_compatibility = score_details['location_compatibility']
    rating_score = score_details['rating_score']
    preference_match = score_details['preference_match']
    
    # 如果有LLM客户端，使用LLM生成理由
    if llm_client and poi_intro:
        try:
            # 根据是否是删除场景，使用不同的提示词
            if is_removal:
                prompt = f"""
                请为删除以下景点生成简短理由（15字以内）：
                景点介绍：{poi_intro}
                用户偏好标签：{prefer_tags}
                偏好匹配度：{preference_match:.2f}（越高越好）
                离其他景点的距离兼容性：{location_compatibility:.2f}（越高越好）
                评分：{rating_score:.2f}（越高越好）
                
                请返回一个删除理由，简单扼要,15字以内，从评分低、位置偏远、不符合用户偏好等角度考虑。
                """
            else:
                prompt = f"""
                请为以下景点生成推荐理由：
                景点介绍：{poi_intro}
                用户偏好标签：{prefer_tags}
                离其他景点的距离兼容性：{location_compatibility:.2f}（越高越好）
                评分：{rating_score:.2f}（越高越好）

                请返回一个推荐理由，简单扼要,15字以内。
                """
            
            response = llm_client.predict(prompt).strip()
            try:
                return response
            except ValueError:
                print(f"LLM返回的结果无法处理: {response}")
        except Exception as e:
            print(f"使用LLM生成理由时出错: {e}")
    
    # 如果没有LLM或出错，返回默认理由
    if is_removal:
        return f"综合评分低：位置偏远、评分较低"
    else:
        return "不错的选择"

def calculate_poi_avg_distance_to_others(poi, other_pois):
    """计算单个POI到其他POI的平均距离
    
    Args:
        poi: 当前POI
        other_pois: 除当前POI外的其他POI列表
        
    Returns:
        float: 平均距离
    """
    # 使用现有的calculate_distances_to_pois函数获取距离信息
    distance_info = calculate_distances_to_pois(poi, other_pois)
    
    if not distance_info:
        return None
    
    # 提取距离值
    distances = [info['distance'] for info in distance_info]
    
    if not distances:
        return None
        
    # 计算平均距离
    return np.mean(distances)

def calculate_all_pois_avg_distances(pois):
    """计算所有POI的平均距离
    
    Args:
        pois: POI列表
        
    Returns:
        list: 每个POI的平均距离列表
    """
    avg_distances = []
    
    for i, poi in enumerate(pois):
        # 创建不包含当前POI的列表
        other_pois = [p for p in pois if p != poi]
        
        # 计算当前POI到其他POI的平均距离
        avg_distance = calculate_poi_avg_distance_to_others(poi, other_pois)
        avg_distances.append(avg_distance)
    
    return avg_distances

def normalize_distances_to_scores(avg_distances):
    """将平均距离归一化为地理兼容性分数
    
    Args:
        avg_distances: 平均距离列表
        
    Returns:
        list: 地理兼容性分数列表
    """
    # 过滤出有效的平均距离
    valid_avg = [d for d in avg_distances if d is not None]
    
    if not valid_avg:
        return [0.5] * len(avg_distances)  # 如果没有有效距离，返回默认值
    
    # 找出最小和最大平均距离
    min_avg = min(valid_avg)
    max_avg = max(valid_avg)
    
    # 归一化到0-1区间
    compatibility_scores = []
    for d in avg_distances:
        if d is None:
            compatibility = 0.5  # 默认中等兼容性
        else:
            if max_avg == min_avg:
                normalized = 1.0  # 所有距离相同的情况
            else:
                # 距离越远，分数越低；距离越近，分数越高
                normalized = (max_avg - d) / (max_avg - min_avg)
            compatibility = normalized
        compatibility_scores.append(compatibility)
        
    print("compatibility_scores", compatibility_scores)
    
    return compatibility_scores

def calculate_rating_score(poi):
    """计算POI的评分得分
    
    Args:
        poi: POI字典
        
    Returns:
        float: 评分得分(0-1)
    """
    rating_score = 0.5  # 默认中等评分
    try:
        if isinstance(poi.get("rating"), (int, float)):
            rating = float(poi["rating"])
            rating_score = normalize_score(rating, 1, 5)  # 假设评分范围为1-5
        elif isinstance(poi.get("rating"), str) and poi["rating"] != "无评分":
            try:
                rating = float(poi["rating"])
                rating_score = normalize_score(rating, 1, 5)
            except ValueError:
                pass  # 使用默认评分
    except (ValueError, TypeError):
        pass  # 使用默认评分
    
    return rating_score

def cluster_pois_kmeans(pois: List[Dict], n_clusters: int, pois_per_day: float = None) -> List[List[Dict]]:       
    """
    使用KMeans对POI进行聚类，并限制每天POI数量
    
    Args:
        pois: POI列表，每个POI包含coordinates键
        n_clusters: 聚类数量（天数）
        pois_per_day: 每天POI数量限制
        
    Returns:
        List[List[Dict]]: 聚类后的POI分组列表
    """
    if not pois:
        return [[] for _ in range(n_clusters)]
    
    # 提取坐标
    coordinates = []
    valid_pois = []
    for poi in pois:
        coords = None
        if poi.get("coordinates"):
            coords = poi["coordinates"]
        elif poi.get("lat") is not None and poi.get("lng") is not None:
            coords = [poi["lat"], poi["lng"]]
        
        if coords:
            coordinates.append(coords)
            valid_pois.append(poi)
        
    if not coordinates:
        return [[] for _ in range(n_clusters)]
   
    total_pois = len(valid_pois)
    print(f"valid_pois: {valid_pois}")
    print(f"总共有效POI: {total_pois}个")
    
    # 计算每天实际的POI数量上限
    if pois_per_day is None:
        pois_per_day = math.ceil(total_pois / n_clusters)
    else:
        pois_per_day = float(pois_per_day)  # 确保是浮点数，支持小数值
    
    daily_limit = math.ceil(pois_per_day)
    print(f"每天POI数量限制: {daily_limit}个")
    
    # 转换为numpy数组
    X = np.array(coordinates)
    
    # KMeans聚类
    kmeans = KMeans(n_clusters=n_clusters, random_state=42)
    labels = kmeans.fit_predict(X)
    cluster_centers = kmeans.cluster_centers_
    
    # 计算每个POI到所有聚类中心的距离
    poi_distances = []
    for i, poi_coord in enumerate(coordinates):
        distances = []
        for center in cluster_centers:
            dist = np.sqrt(np.sum((np.array(poi_coord) - center) ** 2))
            distances.append(dist)
        poi_distances.append((i, distances))
    
    # 按照到最近聚类中心的距离排序POI
    poi_distances.sort(key=lambda x: min(x[1]))
    
    # 初始化每天的POI列表
    daily_pois = [[] for _ in range(n_clusters)]
    daily_counts = [0 for _ in range(n_clusters)]
    
    # 分配POI到各天
    for poi_idx, distances in poi_distances:
        # 找出距离最近的聚类（天）
        closest_clusters = sorted(range(n_clusters), key=lambda i: distances[i])
        
        # 尝试分配到最近的聚类，如果已满则尝试次近的
        assigned = False
        for cluster_idx in closest_clusters:
            if daily_counts[cluster_idx] < daily_limit:
                daily_pois[cluster_idx].append(valid_pois[poi_idx])
                daily_counts[cluster_idx] += 1
                assigned = True
                break
        
        # 如果所有聚类都已达到上限，则分配到拥有POI最少的一天
        if not assigned:
            min_count_idx = daily_counts.index(min(daily_counts))
            daily_pois[min_count_idx].append(valid_pois[poi_idx])
            daily_counts[min_count_idx] += 1
    
    print(f"每天POI数量分布: {daily_counts}")
    
    return daily_pois

def calculate_cluster_centers(clusters: List[List[Dict]]) -> List[np.ndarray]:
    """
    计算每个聚类的中心点
    
    Args:
        clusters: 聚类后的POI分组列表
        
    Returns:
        List[np.ndarray]: 每个聚类的中心点坐标
    """
    def get_coordinates(poi):
        if poi.get("coordinates"):
            return poi["coordinates"]
        elif poi.get("lat") is not None and poi.get("lng") is not None:
            return [poi["lat"], poi["lng"]]
        return None

    centers = []
    for cluster in clusters:
        if cluster:
            coordinates = []
            for poi in cluster:
                coords = get_coordinates(poi)
                if coords:
                    coordinates.append(coords)
            
            if coordinates:
                center = np.mean(coordinates, axis=0)
                centers.append(center)
            else:
                centers.append(None)
        else:
            centers.append(None)
    return centers

def optimize_daily_route(pois: List[Dict]) -> List[Dict]:
    """
    使用TSP优化单天的POI访问顺序
    
    Args:
        pois: 单天的POI列表
        
    Returns:
        List[Dict]: 优化顺序后的POI列表
    """
    if len(pois) <= 1:
        return pois
    
    # 首先统一坐标格式
    def get_coordinates(poi):
        if poi.get("coordinates"):
            return poi["coordinates"]
        elif poi.get("lat") is not None and poi.get("lng") is not None:
            return [poi["lat"], poi["lng"]]
        return None
    
    # 过滤掉没有坐标的POI
    valid_pois = []
    valid_coords = []
    for poi in pois:
        coords = get_coordinates(poi)
        if coords:
            valid_pois.append(poi)
            valid_coords.append(coords)
    
    if not valid_pois:
        return pois
    
    n = len(valid_pois)
    # 构建距离矩阵
    distance_matrix = np.zeros((n, n))
    
    # 利用batch_calculate_distances减少API调用次数
    for i in range(n):
        # 当前POI作为终点
        destination_poi = valid_coords[i]
        
        # 收集除了当前POI外的所有其他POI坐标作为起点
        origin_coords = []
        origin_indices = []
        for j in range(n):
            if i != j:  # 排除自身
                origin_coords.append(valid_coords[j])
                origin_indices.append(j)
        
        if origin_coords:
            # 批量计算从其他所有POI到当前POI的距离
            batch_results = batch_calculate_distances(origin_coords, destination_poi)
            
            # 填充距离矩阵
            for idx, result in enumerate(batch_results):
                if idx < len(origin_indices):
                    j = origin_indices[idx]
                    dist = int(result.get("distance", 0))
                    distance_matrix[j][i] = dist  # 注意：这里是从j到i的距离
    
    # 贪心算法求解TSP
    current = 0  # 从第一个POI开始
    unvisited = set(range(1, n))
    path = [current]
    
    while unvisited:
        next_point = min(unvisited, key=lambda x: distance_matrix[current][x])
        path.append(next_point)
        unvisited.remove(next_point)
        current = next_point
    
    # 按照优化后的顺序重排POI
    optimized_pois = [valid_pois[i] for i in path]
    
    # 添加回没有坐标的POI（放在最后）
    for poi in pois:
        if not get_coordinates(poi) and poi not in optimized_pois:
            optimized_pois.append(poi)
    
    return optimized_pois

def build_optimized_itinerary(clusters: List[List[Dict]], centers: List[np.ndarray]) -> List[Dict]:
    """
    构建优化后的完整行程
    
    Args:
        clusters: 聚类后的POI分组列表
        centers: 每个聚类的中心点坐标
        
    Returns:
        List[Dict]: 优化后的每日行程列表
    """
    daily_itinerary = []
    
    for day, (cluster, center) in enumerate(zip(clusters, centers), 1):
        if not cluster:
            continue
            
        # 优化当天POI访问顺序
        optimized_pois = optimize_daily_route(cluster)
        
        # 构建当天行程
        schedule = []
        for poi in optimized_pois:
            schedule.append({
                "poi_type": "景点",
                "name": poi["name"],
                "coordinates": poi["coordinates"],
                "visit_time": poi.get("visit_time", "2小时"),
                "description": poi.get("description", "")
            })
        
        daily_itinerary.append({
            "day": day,
            "schedule": schedule,
            "center": center.tolist() if center is not None else None
        })
    
    return daily_itinerary