from agent.agent_config import setup_agent
from utils.file_utils import load_itinerary
import pandas as pd
from agent.langgraph_runtime import LangGraphApp
from agent.app_context import AppContext
from agent.context_manager import TripContextManager
import logging
import datetime

# 配置日志
logger = logging.getLogger(__name__)

# 添加全局会话管理
active_sessions = {}  # 存储活跃的会话，键为trip_id，值为LangGraphApp实例

def process_message(message, itinerary_info: dict) -> str:
    """
    Flask 调用的消息处理接口，
    将单次用户输入和行程信息转成 TripContextManager 需要的格式并驱动 LangGraphApp.
    
    参数:
    - message: 用户输入的消息
    - itinerary_info: 行程信息，可能来自Android客户端或数据库
    
    返回:
    - 如果是普通响应，返回字符串
    - 如果涉及数据更新，返回字典，包含消息和更新的数据
    """
    logger.info(f"处理消息: {message}")
    
    # 提取行程ID
    trip_id = str(itinerary_info.get('itinerary_id'))
    logger.info(f"提取的行程ID: {trip_id}")
    
    # 检查是否已有该行程ID的会话实例
    if trip_id in active_sessions:
        logger.info(f"使用现有会话实例，行程ID: {trip_id}")
        app = active_sessions[trip_id]
        
        # 重要：确保context_manager中使用正确的行程ID
        # 注意：只在context_manager中设置，不再设置state['current_trip_id']
        result = app.context_manager.set_current_trip(trip_id)
        logger.info(f"设置context_manager行程ID结果: {result}")
        
        # 打印当前行程数据，检查是否能正确获取
        trip_data = app.context_manager.get_current_trip()
        if trip_data:
            logger.info(f"成功获取行程数据: {trip_data.get('metadata', {}).get('title', '未知')}")
        else:
            logger.warning(f"⚠️ 无法获取行程数据，请检查行程ID: {trip_id}")
        
        # 处理用户输入
        response = app.process_input(message)
        print(f"处理后的响应: {response}")
        # 检查是否有更新的行程数据或其他类型的数据
        if isinstance(response, dict):
            # 检查是否有行程更新数据
            if 'flow_state' in response and 'updated_trip' in response['flow_state']:
                flow_state = response['flow_state']
                # 更新行程数据
                update_result = app.context_manager.update_current_trip(flow_state['updated_trip'])
                
                # 返回更新结果
                return {
                    'success': update_result['success'],
                    'message': response.get('response', update_result['message']),
                    'data': update_result['data'],
                    'data_type': update_result['data_type']
                }
            
            # 检查是否直接包含data和data_type字段（POI推荐等）
            elif 'data_type' in response and 'data' in response:
                return {
                    'success': True,
                    'message': response.get('response', '推荐完成'),
                    'data': response['data'],
                    'data_type': response['data_type']
                }
            
            # 其他格式的字典响应，返回response字段
            return response.get('response', '处理完成')
        
        # 如果是普通字符串响应，直接返回
        return response
    
    # 没有现有会话，创建新的会话
    logger.info(f"创建新会话实例，行程ID: {trip_id}")
    
    # 获取当前日期，作为默认开始日期
    today = datetime.datetime.now().strftime("%Y-%m-%d")
    
    # 1. 把单条行程信息包装成 cacheable 格式
    payload = {
        'travel_itineraries': [
            {
                'metadata': {
                    'trip_id': trip_id,
                    'title': itinerary_info.get('title', '未命名行程'),
                    'destination': itinerary_info.get('location', '未指定目的地'),
                    'total_days': itinerary_info.get('days', 1),
                    'start_date': itinerary_info.get('start_date', today),
                    'target_audience': itinerary_info.get('target_audience', '通用')
                },
                'daily_itinerary': []
            }
        ]
    }
    
    # 将attractions列表按天数分组
    attractions = itinerary_info.get('attractions', [])
    daily_attractions = {}
    
    for attraction in attractions:
        day = attraction.get('day', 1)
        if day not in daily_attractions:
            daily_attractions[day] = []
        if "餐饮服务" in attraction.get('type', ''):
            poi_type='餐厅'
        else: 
            poi_type='景点'
        # 处理景点详细信息
        attraction_data = {
            'name': attraction.get('name', ''),
            'poi_type': attraction.get('type', ''),
            'uid': attraction.get('poi_id', ''),  # 默认为景点类型
            'time': {
                'start': '09:00',
                'end': '10:30'
            },
            'duration_hours': 1.5,
            'notes': [],
            'price': 0,
            'type': attraction.get('type_desc', ''),
            'tel': attraction.get('tel', '')
        }
        
        # 如果有经纬度信息，添加
        if 'latitude' in attraction and 'longitude' in attraction:
            attraction_data['coordinates'] = [float(attraction.get('latitude')), float(attraction.get('longitude'))]
            
        # 添加地址信息(如果存在)
        if 'address' in attraction and attraction['address']:
            attraction_data['address'] = attraction['address']
        
        # 如果有交通方式，添加到备注
        if 'transport' in attraction and attraction['transport']:
            attraction_data['transport'] = attraction['transport']
            attraction_data['notes'].append(f"交通方式: {attraction['transport']}")
            
        daily_attractions[day].append(attraction_data)
    
    # 为每一天创建日程
    for day, schedule in daily_attractions.items():
        # 计算该天的日期
        day_date = datetime.datetime.strptime(today, "%Y-%m-%d")
        day_date += datetime.timedelta(days=day-1)
        day_date_str = day_date.strftime("%Y-%m-%d")
        
        # 创建该天的数据结构，包含默认酒店信息
        day_data = {
            'day': day,
            'date': day_date_str,
            'hotel': {
                'name': '默认酒店',
                'price': 300,
                'numbed': 2,
                'coordinates': [0, 0]  # 添加默认坐标
            },
            'schedule': schedule
        }
        
        payload['travel_itineraries'][0]['daily_itinerary'].append(day_data)
    
    # 如果没有分组日程，至少创建一个默认日程
    if not payload['travel_itineraries'][0]['daily_itinerary']:
        payload['travel_itineraries'][0]['daily_itinerary'].append({
            'day': 1,
            'date': today,
            'hotel': {
                'name': '默认酒店',
                'price': 300,
                'numbed': 2,
                'coordinates': [0, 0]  # 添加默认坐标
            },
            'schedule': []
        })
    
    # 排序日程，确保按天数顺序
    payload['travel_itineraries'][0]['daily_itinerary'].sort(key=lambda x: x['day'])
    
    print("处理后行程",payload)
    logger.info("已准备行程数据作为AI输入")
    
    # 2. 用新的 payload 创建上下文管理器和应用实例
    cm = TripContextManager(payload)
    
    # 重要：使用set_current_trip方法设置当前行程ID
    logger.info(f"行程ID: {trip_id}")
    # 确保通过set_current_trip方法设置行程ID
    result = cm.set_current_trip(trip_id)
    logger.info(f"设置行程ID结果: {result}")

    # 更新AppContext中的context_manager实例，确保其他模块使用最新的context_manager
    app_context = AppContext.get_instance()
    app_context.context_manager = cm

    # 3. 创建LangGraphApp实例，使用修改后的context_manager
    app = LangGraphApp(session_id='flask_session_'+trip_id, context_manager=cm)
    
    # 更新状态，初始化应用，但不再设置current_trip_id
    app.initialized = True
    
    # 保存会话实例
    active_sessions[trip_id] = app
    
    # 4. 处理用户输入
    response = app.process_input(message)
    
    # 检查是否有更新的行程数据或其他类型的数据
    if isinstance(response, dict):
        # 检查是否有行程更新数据
        if 'flow_state' in response and 'updated_trip' in response['flow_state']:
            flow_state = response['flow_state']
            # 更新行程数据
            update_result = app.context_manager.update_current_trip(flow_state['updated_trip'])
            
            # 返回更新结果
            return {
                'success': update_result['success'],
                'message': response.get('response', update_result['message']),
                'data': update_result['data'],
                'data_type': update_result['data_type']
            }
        
        # 检查是否直接包含data和data_type字段（POI推荐等）
        elif 'data_type' in response and 'data' in response:
            return {
                'success': True,
                'message': response.get('response', '推荐完成'),
                'data': response['data'],
                'data_type': response['data_type']
            }
        
        # 其他格式的字典响应，返回response字段
        return response.get('response', '处理完成')
    
    # 如果是普通字符串响应，直接返回
    return response

def clear_session(trip_id: str) -> str:
    """
    清除指定行程ID的会话
    
    参数:
    - trip_id: 行程ID
    
    返回:
    - 清除结果信息
    """
    global active_sessions
    
    trip_id = str(trip_id)
    
    # 记录当前所有活跃会话
    session_ids = list(active_sessions.keys())
    logger.info(f"当前活跃会话: {session_ids}")
    
    if trip_id in active_sessions:
        # 删除会话
        del active_sessions[trip_id]
        logger.info(f"已清除行程ID: {trip_id} 的会话")
        
        # 记录清除后的活跃会话
        remaining_sessions = list(active_sessions.keys())
        logger.info(f"清除后的活跃会话: {remaining_sessions}")
        
        return f"已清除行程ID: {trip_id} 的会话"
    else:
        logger.info(f"未找到行程ID: {trip_id} 的会话，当前活跃会话: {session_ids}")
        return f"未找到行程ID: {trip_id} 的会话"

def clear_all_sessions() -> str:
    """
    清除所有会话
    
    返回:
    - 清除结果信息
    """
    global active_sessions
    
    session_count = len(active_sessions)
    session_ids = list(active_sessions.keys())
    logger.info(f"清除前的活跃会话: {session_ids}")
    
    active_sessions.clear()
    logger.info(f"已清除所有 {session_count} 个会话")
    return f"已清除所有 {session_count} 个会话"

def main():
    # 初始化应用

    app_context = AppContext.get_instance()  # 确保获取到 AppContext 实例
    app = LangGraphApp(session_id="user123",context_manager=app_context.context_manager)
    
    # 行程设置阶段
    while True:
        trip_id = input("请输入你想要查看或编辑的行程编号（例如 CD-FOOD-003）：")
        result = app.set_trip_id(trip_id)
        print(result)
        if result.startswith("✅"):
            break
    
    # 交互循环
    print("欢迎使用旅行助手！输入'退出'结束对话。")
    
    while True:
        user_input = input("\n🗣️ 你说：")
        
        if user_input.lower() == "exit" or user_input == "退出":
            print("谢谢使用，再见！")
            break
        
        # 处理输入并输出响应
        response = app.process_input(user_input)
        print(f"\n🤖 助手: {response}")
        
        # 打印上下文状态用于调试
        print("\n📊 当前状态摘要:")
        print(app.context_manager.summary())
        if app.state.get("should_continue") is False:
            print("should_continue_in_main",app.state.get("should_continue"))
            continue  # 退出循环等待新输入

if __name__ == "__main__":
    main()
