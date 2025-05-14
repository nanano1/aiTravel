import mysql.connector
import requests
import pandas as pd
import time
from typing import List
import os


# 配置
AMAP_KEY = os.getenv('AMAP_KEY')  # 从环境变量中获取API密钥
MYSQL_HOST = 'localhost'
MYSQL_USER = 'root'
MYSQL_PASSWORD = '8384888'
MYSQL_DATABASE = 'userdb'

def get_poi_ids_from_db() -> List[str]:
    """从MySQL数据库获取POI IDs"""
    try:
        conn = mysql.connector.connect(
            host=MYSQL_HOST,
            user=MYSQL_USER,
            password=MYSQL_PASSWORD,
            database=MYSQL_DATABASE
        )
        cursor = conn.cursor()
        
        # 根据实际的表名和字段名修改SQL查询
        cursor.execute("SELECT poiid FROM tpt_data_jingdian WHERE areaid LIKE '110%'")
        poi_ids = [row[0] for row in cursor.fetchall()]
        
        cursor.close()
        conn.close()
        return poi_ids
    except Exception as e:
        print(f"数据库错误: {e}")
        return []

def get_poi_details(poi_ids: List[str]) -> List[dict]:
    """调用高德API获取POI详细信息"""
    base_url = "https://restapi.amap.com/v5/place/detail"
    
    # 将POI IDs转换为"|"分隔的字符串
    poi_ids_str = "|".join(poi_ids)
    
    params = {
        "key": AMAP_KEY,
        "id": poi_ids_str,
        "show_fields": "business"  # 包含额外信息
    }
    
    try:
        response = requests.get(base_url, params=params)
        data = response.json()
        
        if data["status"] == "1":
            return data["pois"]
        else:
            print(f"API错误: {data['info']}")
            return []
    except Exception as e:
        print(f"请求错误: {e}")
        return []

def process_poi_data(pois: List[dict]) -> List[dict]:
    """处理POI数据，提取所需字段"""
    processed_data = []
    
    for poi in pois:
        processed_poi = {
            'name': poi.get('name', ''),
            'id': poi.get('id', ''),
            'address': poi.get('address', ''),
            'city': poi.get('cityname', ''),
            'district': poi.get('adname', ''),
            'type': poi.get('type', ''),
            'location': poi.get('location', ''),
            'tel': poi.get('business', {}).get('tel', ''),
            'rating': poi.get('business', {}).get('rating', ''),
            'cost': poi.get('business', {}).get('cost', ''),
            'business_area': poi.get('business', {}).get('business_area', '')
        }
        processed_data.append(processed_poi)
    
    return processed_data

def main():
    # 获取所有POI IDs
    poi_ids = get_poi_ids_from_db()
    if not poi_ids:
        print("未找到POI IDs")
        return
    
    all_poi_data = []
    
    # 每10个ID为一组处理
    for i in range(0, len(poi_ids), 10):
        batch_ids = poi_ids[i:i+10]
        print(f"正在处理第 {i//10 + 1} 组POI...")
        
        poi_details = get_poi_details(batch_ids)
        processed_data = process_poi_data(poi_details)
        all_poi_data.extend(processed_data)
        
        # 添加延时避免API限制
        time.sleep(0.5)
    
    # 将数据保存为CSV
    if all_poi_data:
        df = pd.DataFrame(all_poi_data)
        df.to_csv('poi_details.csv', index=False, encoding='utf-8-sig')
        print(f"数据已保存到 poi_details.csv，共处理 {len(all_poi_data)} 条记录")
    else:
        print("未获取到POI数据")

if __name__ == "__main__":
    main() 