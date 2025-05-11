import json

def load_itinerary(file_path):
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"错误：未找到文件 {file_path}")
        return []

def save_itinerary(file_path, itinerary_data):
    try:
        with open(file_path, 'w', encoding='utf-8') as f:
            json.dump(itinerary_data, f, ensure_ascii=False, indent=4)
        print(f"已成功将行程数据保存到 {file_path}")
    except Exception as e:
        print(f"保存文件时出错：{e}")