import requests
import json

def test_chat_endpoint():
    url = 'http://127.0.0.1:5001/chat'
    
    # 测试数据
    test_data = {
        "message": "你好，我想了解一下行程",
        "itinerary_id": "北京-8372"  # 使用你数据库中存在的 itinerary_id
    }
    
    try:
        # 发送 POST 请求
        response = requests.post(url, json=test_data)
        
        # 打印状态码
        print(f"状态码: {response.status_code}")
        
        # 打印响应内容
        print(f"响应内容: {response.text}")
        
        if response.status_code == 200:
            print("✅ API 测试成功!")
        else:
            print("❌ API 测试失败!")
            
    except requests.exceptions.ConnectionError:
        print("❌ 连接错误: 确保 Flask 服务器正在运行")
    except Exception as e:
        print(f"❌ 测试过程中出现错误: {str(e)}")

if __name__ == "__main__":
    test_chat_endpoint() 