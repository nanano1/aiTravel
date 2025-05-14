import requests

def get_all_pois_baidu():

    ak = 'UIOBPAhprxZdUqc2lufTIKy7nYMNh3mv'
    url = 'https://api.map.baidu.com/place/v2/search'
    params = {
        'query': '天坛公园,旅游景点',  # 检索关键字，可修改为其他内容
        'region': '北京',
        'output': 'json',
        'city_limit': 'true',# 输出格式为json
        'ak': ak,
        'tag': '旅游景点',
        'page_size': 10,
        'scope': '2',

        }

    try:
        response = requests.get(url, params=params)
        if response.status_code == 200:
            result = response.json()
            if result['status'] == 0:
                pois = result['results']
                for poi in pois:
                    # 提取所需信息
                    name = poi.get('name', '无名称')
                    
                    detail_info=poi.get('detail_info', '无详情')
                    overall_rating = detail_info.get('overall_rating', '无评分')
                    tag = detail_info.get('tag', '无标签')
                    label = detail_info.get('label', '无标签解释')
                    price = detail_info.get('price', '无价格')
                    classified_poi_tag=detail_info.get('classified_poi_tag', '无分类')
                    print(f"名称: {name}")
                    print(f"价格: {price}")
                    print(f"评分: {overall_rating}")
                    print(f"标签: {tag}")
                    print(f"标签解释: {label}")
                    print(f"分类: {classified_poi_tag}")
                    print("-" * 50)
            else:
                print(f"请求失败，错误信息: {result['message']}")
        else:
            print(f"请求失败，状态码: {response.status_code}")
    except requests.RequestException as e:
        print(f"请求发生异常: {e}")
def get_surrounding_pois_baidu():
    center=[39.915,116.404]
    location_string=f"{center[0]},{center[1]}"
    preferences={'budget': [100, 200], 'cuisine_preference': '外国餐厅', 'special_requirement': None, 'rating': 'overall_rating'}
    budget = preferences.get("budget")
    cuisine_preference = preferences.get("cuisine_preference")

    if budget:  # 检查 budget 是否为空
        filter_string = f'industry_type:cater|sort_name:{preferences.get("rating")}|sort_type:1|price_section:{preferences.get("budget")[0]},{preferences.get("budget")[1]}'
    else:
        filter_string = f'industry_type:cater|sort_name:{preferences.get("rating")}|sort_type:1'
    print(filter_string)
    
    
    # 开发者的访问密钥，需替换为真实的AK
    ak = 'UIOBPAhprxZdUqc2lufTIKy7nYMNh3mv'
    url = 'https://api.map.baidu.com/place/v2/search'
    params = {
        'query': '美食',  # 检索关键字，可修改为其他内容
        'location': '30.569248,114.309851',  # 圆形区域检索中心点，可修改
        'radius': 2.98*1000,  # 圆形区域检索半径，单位为米，可修改
        'output': 'json',  # 输出格式为json
        'ak': ak,
        'scope': '2',
        'filter': 'industry_type:cater|sort_name:default|sort_rule:0'
    }

    try:
        response = requests.get(url, params=params)
        if response.status_code == 200:
            result = response.json()
            if result['status'] == 0:
                pois = result['results']
                for poi in pois:
                    # 提取所需信息
                    location=poi.get('location', '无纬度')
                    latitude=location.get('lat')
                    longitude=location.get('lng')
                    name = poi.get('name', '无名称')
                    address=poi.get('address','无地址')
                    detail_info=poi.get('detail_info', '无详情')
                    content_tag=detail_info.get('content_tag', '无内容标签')
                    comment_num=detail_info.get('comment_num', '无评论数')
                    discount_num=detail_info.get('discount_num', '无折扣数')
                    favorite_num=detail_info.get('favorite_num', '无收藏数')
                    print(f"地址: {address}")
                    print(f"纬度: {latitude}")
                    print(f"经度: {longitude}")
                    print(f"内容标签: {content_tag}")
                    print(f"评论数: {comment_num}")
                    print(f"折扣数: {discount_num}")
                    print(f"收藏数: {favorite_num}")
                    print(detail_info)
                    overall_rating = detail_info.get('overall_rating', '无评分')
                    tag = detail_info.get('tag', '无标签')
                    label = detail_info.get('label', '无标签解释')
                    price = detail_info.get('price', '无价格')
                    classified_poi_tag=detail_info.get('classified_poi_tag', '无分类')
                    print(f"名称: {name}")
                    print(f"价格: {price}")
                    print(f"评分: {overall_rating}")
                    print(f"标签: {tag}")
                    print(f"标签解释: {label}")
                    print(f"分类: {classified_poi_tag}")
                    print("-" * 50)
            else:
                print(f"请求失败，错误信息: {result['message']}")
        else:
            print(f"请求失败，状态码: {response.status_code}")
    except requests.RequestException as e:
        print(f"请求发生异常: {e}")

def search_restaurants_by_center_radius() -> list:
    """
    根据中心点和半径搜索周边餐厅
    
    Args:
        center: 中心点坐标 [纬度, 经度]
        radius: 搜索半径（公里）
    
    Returns:
        list: 餐厅列表，每个餐厅包含名称、评分、价格等信息
    """
    url = "https://restapi.amap.com/v3/place/around"
    
    params = {
        "key": "d477f25785fee6455f468f4702ff7bd5",
        "location": '30.569248,114.309851',
        "radius": 4*1000,
        "keywords": "美食",
        "types": "110000",
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
                        type=poi.get('type', '无类型')
                        biz_ext = poi.get('biz_ext', {})
                        rating = biz_ext.get('rating', '无评分')
                        cost = biz_ext.get('cost', '无价格')
                        print(f"名称: {poi['name']}, 类型: {type}, 评分: {rating},cost: {cost}")
                        
                else:
                    print(f"请求成功，但返回结果状态错误: {result}")
            else:
                print(f"请求失败，状态码: {response.status_code}")
    except requests.RequestException as e:
        print(f"请求异常: {e}")


def search_restaurants_by_keyword() -> list:

    url = "https://restapi.amap.com/v5/place/text"
    
    params = {
        "key": "d477f25785fee6455f468f4702ff7bd5",
        "types": "110000",
        "region": "北京",
        "city_limit": "true",
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
                        type=poi.get('type', '无类型')
                        biz_ext = poi.get('biz_ext', {})
                        rating = biz_ext.get('rating', '无评分')
                        cost = biz_ext.get('cost', '无价格')
                        print(f"名称: {poi['name']}, 类型: {type}, 评分: {rating},cost: {cost}")
                        
                else:
                    print(f"请求成功，但返回结果状态错误: {result}")
            else:
                print(f"请求失败，状态码: {response.status_code}")
    except requests.RequestException as e:
        print(f"请求异常: {e}")

def search_pois_by_center_view(center_poi, radius=4, limit=20):
    """
    根据中心POI坐标搜索周边景点
    
    Args:
        center_poi: 中心点坐标 [纬度, 经度]
        radius: 搜索半径（公里），默认4公里
        limit: 返回景点数量限制，默认20个
    
    Returns:
        list: 景点列表
    """
    url = "https://restapi.amap.com/v3/place/around"
    
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
                    biz_ext = poi.get('biz_ext', {})
                    rating = biz_ext.get('rating', '无评分')
                    
                    # 提取经纬度
                    location = poi.get('location', '')
                    coordinates = None
                    if location:
                        lng, lat = location.split(',')
                        coordinates = [float(lat), float(lng)]
                    
                    poi_info = {
                        "name": poi.get('name'),
                        "type": poi_type,
                        "rating": rating,
                        "coordinates": coordinates,
                        "address": poi.get('address', '无地址'),
                        "distance": poi.get('distance', '未知')
                    }
                    
                    result_pois.append(poi_info)
                    
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

if __name__ == "__main__":
 #   print(search_pois_by_center_view([30.569248,114.309851]))
 #   print(calculate_distance([
 #                 40.362639,
 ##                 116.024067
 # #                ],[
 # #                  39.9237932,
 # #                  116.4101746
 # #                ],))
    get_surrounding_pois_baidu()