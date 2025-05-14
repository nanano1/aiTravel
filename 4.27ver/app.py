from flask import Flask, request, jsonify
from main import process_message, clear_session, clear_all_sessions
import sqlite3
import logging
import json
import traceback
from flask_cors import CORS

app = Flask(__name__)
CORS(app)  # 启用CORS支持

# 配置日志
logging.basicConfig(level=logging.INFO, 
                   format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
logger = logging.getLogger(__name__)

@app.route('/chat', methods=['POST'])
def chat():
    try:
        logger.info("收到新的聊天请求")
        
        # 验证请求内容是否为JSON
        if not request.is_json:
            logger.error("请求内容不是JSON格式")
            return jsonify({'error': '请求必须是JSON格式'}), 400
        
        data = request.get_json()
        
        # 验证必要参数
        if not data:
            logger.error("JSON解析失败或为空")
            return jsonify({'error': 'JSON解析失败或为空'}), 400
        
        message = data.get('message')
        if not message:
            logger.warning("消息内容为空")
            return jsonify({'error': '消息内容不能为空'}), 400
        
        itinerary_data = data.get('itinerary_data')
        
        logger.info(f"请求参数: message={message}")
        if itinerary_data:
            logger.info(f"行程数据: {json.dumps(itinerary_data, ensure_ascii=False)[:200]}...")
        else:
            logger.warning("既没有提供行程数据也没有提供itinerary_id")
            return jsonify({'response': '抱歉，请提供行程信息或行程ID'}), 400

        logger.info("调用 AI 处理函数")
        # 将itinerary_data传递给AI处理函数
        response = process_message(message, itinerary_data)
        
        # 检查response是否是字典类型（新格式）
        if isinstance(response, dict):
            logger.info(f"AI 返回了数据: {str(response)[:100]}...")
            return jsonify({
                'success': True,
                'response': response.get('message', '处理完成'),
                'data': response.get('data'),
                'data_type': response.get('data_type')
            })
            
        else:
            # 处理旧格式（字符串）响应
            logger.info(f"AI 响应: {response[:100]}...")
            return jsonify({
                'success': True,
                'response': response,
                'data': None,
                'data_type': None
            })
            
    except json.JSONDecodeError as e:
        logger.error(f"JSON解析错误: {str(e)}")
        return jsonify({'error': f'JSON格式错误: {str(e)}'}), 400
    except sqlite3.Error as e:
        logger.error(f"数据库错误: {str(e)}")
        return jsonify({'error': f'数据库错误: {str(e)}'}), 500
    except Exception as e:
        logger.error(f"处理请求时出错: {str(e)}", exc_info=True)
        error_traceback = traceback.format_exc()
        logger.error(f"错误详情: {error_traceback}")
        return jsonify({'error': str(e)}), 500

# 新增API：餐厅推荐
@app.route('/recommend_restaurants', methods=['POST'])
def recommend_restaurants():
    try:
        logger.info("收到餐厅推荐请求")
        
        # 验证请求格式
        if not request.is_json:
            logger.error("请求内容不是JSON格式")
            return jsonify({'success': False, 'error': '请求必须是JSON格式'}), 400
        
        data = request.get_json()
        
        # 验证必要参数
        if not data:
            logger.error("JSON解析失败或为空")
            return jsonify({'success': False, 'error': 'JSON解析失败或为空'}), 400
        
        itinerary_id = data.get('itinerary_id')
        day_number = data.get('day_number')
        meal_type = data.get('meal_type')
        
        if not itinerary_id:
            logger.warning("缺少必要参数itinerary_id")
            return jsonify({'success': False, 'error': '缺少必要参数itinerary_id'}), 400
        
        # 这里调用餐厅推荐函数
        from travel_tools.recommendRestaurant import search_restaurants_by_center_radius
        
        # 连接数据库获取行程信息
        conn = sqlite3.connect('../trave_v6-showmap/app/travel.db')
        cursor = conn.cursor()
        
        # 查询当天的景点
        cursor.execute('''
            SELECT a.name, a.day_number, a.visit_order, 
                   s.latitude, s.longitude, s.address, s.type_desc
            FROM attractions a
            LEFT JOIN sites s ON a.site_id = s.id
            WHERE a.itinerary_id = ? AND a.day_number = ?
            ORDER BY a.visit_order
        ''', (itinerary_id, day_number))
        
        poi_list = []
        for row in cursor.fetchall():
            if row[3] is not None and row[4] is not None:  # 如果有经纬度
                poi_list.append({
                    'name': row[0],
                    'day': row[1],
                    'order': row[2],
                    'coordinates': [row[3], row[4]],
                    'address': row[5],
                    'type': row[6]
                })
        
        # 关闭数据库连接
        conn.close()
        
        if not poi_list:
            logger.warning(f"未找到行程{itinerary_id}第{day_number}天的景点信息")
            return jsonify({'success': False, 'error': f'未找到行程第{day_number}天的景点信息'}), 404
        
        # 计算当天景点的中心位置
        center = [0, 0]
        for poi in poi_list:
            center[0] += poi['coordinates'][0]
            center[1] += poi['coordinates'][1]
        
        center[0] /= len(poi_list)
        center[1] /= len(poi_list)
        
        # 获取推荐餐厅
        restaurants = search_restaurants_by_center_radius(center, 1.5)  # 半径1.5公里
        
        if not restaurants:
            logger.warning("未找到推荐餐厅")
            return jsonify({'success': False, 'error': '未找到符合条件的餐厅'}), 404
        
        # 构建推荐列表
        recommendations = []
        for i, restaurant in enumerate(restaurants[:5]):  # 最多返回5个推荐
            recommendations.append({
                'uid': restaurant.get('uid', f'id_{i}'),
                'name': restaurant.get('name', '未知餐厅'),
                'rating': restaurant.get('overall_rating', 4.5),
                'distance': '1km',  # 示例距离
                'reason': f"这是一家很受欢迎的{restaurant.get('tag', '餐厅')}，距离您的行程景点很近，适合作为{meal_type}。",
                'label': restaurant.get('tag', '未知菜系'),
                'image_url': '',
                'address': restaurant.get('address', '未知地址'),
                'price': restaurant.get('price', 0),
                'coordinates': restaurant.get('location', {}).get('lat_lng', [0, 0])
            })
        
        return jsonify({
            'success': True,
            'recommendations': recommendations
        })
        
    except Exception as e:
        logger.error(f"处理餐厅推荐请求时出错: {str(e)}", exc_info=True)
        error_traceback = traceback.format_exc()
        logger.error(f"错误详情: {error_traceback}")
        return jsonify({'success': False, 'error': str(e)}), 500

# 新增API：刷新餐厅推荐
@app.route('/refresh_recommendations', methods=['POST'])
def refresh_recommendations():
    try:
        logger.info("收到刷新餐厅推荐请求")
        
        # 验证请求格式
        if not request.is_json:
            logger.error("请求内容不是JSON格式")
            return jsonify({'success': False, 'error': '请求必须是JSON格式'}), 400
        
        data = request.get_json()
        
        # 验证必要参数
        if not data:
            logger.error("JSON解析失败或为空")
            return jsonify({'success': False, 'error': 'JSON解析失败或为空'}), 400
        
        # 简单地调用推荐餐厅的API，但使用不同的搜索半径
        itinerary_id = data.get('itinerary_id')
        day_number = data.get('day_number')
        meal_type = data.get('meal_type')
        
        if not itinerary_id:
            logger.warning("缺少必要参数itinerary_id")
            return jsonify({'success': False, 'error': '缺少必要参数itinerary_id'}), 400
        
        # 这里调用餐厅推荐函数，使用更大的搜索半径
        from travel_tools.recommendRestaurant import search_restaurants_by_center_radius
        
        # 连接数据库获取行程信息
        conn = sqlite3.connect('../trave_v6-showmap/app/travel.db')
        cursor = conn.cursor()
        
        # 查询当天的景点
        cursor.execute('''
            SELECT a.name, a.day_number, a.visit_order, 
                   s.latitude, s.longitude, s.address, s.type_desc
            FROM attractions a
            LEFT JOIN sites s ON a.site_id = s.id
            WHERE a.itinerary_id = ? AND a.day_number = ?
            ORDER BY a.visit_order
        ''', (itinerary_id, day_number))
        
        poi_list = []
        for row in cursor.fetchall():
            if row[3] is not None and row[4] is not None:  # 如果有经纬度
                poi_list.append({
                    'name': row[0],
                    'day': row[1],
                    'order': row[2],
                    'coordinates': [row[3], row[4]],
                    'address': row[5],
                    'type': row[6]
                })
        
        # 关闭数据库连接
        conn.close()
        
        if not poi_list:
            logger.warning(f"未找到行程{itinerary_id}第{day_number}天的景点信息")
            return jsonify({'success': False, 'error': f'未找到行程第{day_number}天的景点信息'}), 404
        
        # 计算当天景点的中心位置
        center = [0, 0]
        for poi in poi_list:
            center[0] += poi['coordinates'][0]
            center[1] += poi['coordinates'][1]
        
        center[0] /= len(poi_list)
        center[1] /= len(poi_list)
        
        # 获取推荐餐厅，使用更大的半径
        restaurants = search_restaurants_by_center_radius(center, 3.0)  # 半径3公里
        
        if not restaurants:
            logger.warning("未找到推荐餐厅")
            return jsonify({'success': False, 'error': '未找到符合条件的餐厅'}), 404
        
        # 构建推荐列表，确保与之前不同
        recommendations = []
        for i, restaurant in enumerate(restaurants[5:10]):  # 取之前没有返回的餐厅
            recommendations.append({
                'uid': restaurant.get('uid', f'id_{i+5}'),
                'name': restaurant.get('name', '未知餐厅'),
                'rating': restaurant.get('overall_rating', 4.5),
                'distance': '2km',  # 示例距离
                'reason': f"这是一家很受欢迎的{restaurant.get('tag', '餐厅')}，虽然距离较远，但口碑很好，适合作为{meal_type}。",
                'label': restaurant.get('tag', '未知菜系'),
                'image_url': '',
                'address': restaurant.get('address', '未知地址'),
                'price': restaurant.get('price', 0),
                'coordinates': restaurant.get('location', {}).get('lat_lng', [0, 0])
            })
        
        return jsonify({
            'success': True,
            'recommendations': recommendations
        })
        
    except Exception as e:
        logger.error(f"处理刷新餐厅推荐请求时出错: {str(e)}", exc_info=True)
        error_traceback = traceback.format_exc()
        logger.error(f"错误详情: {error_traceback}")
        return jsonify({'success': False, 'error': str(e)}), 500

# 修改确认餐厅推荐API
@app.route('/confirm_recommendation', methods=['POST'])
def confirm_recommendation():
    try:
        logger.info("收到确认餐厅推荐请求")
        
        # 验证请求格式
        if not request.is_json:
            logger.error("请求内容不是JSON格式")
            return jsonify({'success': False, 'error': '请求必须是JSON格式'}), 400
        
        data = request.get_json()
        
        # 验证必要参数
        if not data:
            logger.error("JSON解析失败或为空")
            return jsonify({'success': False, 'error': 'JSON解析失败或为空'}), 400
        
        itinerary_id = data.get('itinerary_id')
        day = data.get('day')
        meal_type = data.get('meal_type')
        restaurant = data.get('restaurant')
        
        if not itinerary_id or not restaurant:
            logger.warning("缺少必要参数")
            return jsonify({'success': False, 'error': '缺少必要参数(itinerary_id, restaurant)'}), 400
        
        logger.info(f"行程ID: {itinerary_id}, 天数: {day}, 餐食类型: {meal_type}")
        logger.info(f"选择的餐厅: {json.dumps(restaurant, ensure_ascii=False)}")
        
        # 连接数据库
        conn = sqlite3.connect('../trave_v6-showmap/app/travel.db')
        cursor = conn.cursor()
        
        # 确定该餐厅应该在行程中的顺序
        cursor.execute('''
            SELECT MAX(visit_order)
            FROM attractions
            WHERE itinerary_id = ? AND day_number = ?
        ''', (itinerary_id, day))
        
        max_order = cursor.fetchone()[0]
        if max_order is None:
            max_order = 0
        
        # 餐厅访问顺序根据餐食类型确定
        visit_order = 0
        if meal_type == '早餐':
            visit_order = 1  # 早餐放在第一个
        elif meal_type == '午餐':
            visit_order = max(max_order // 2, 2)  # 午餐放在中间
        elif meal_type == '晚餐':
            visit_order = max_order + 1  # 晚餐放在最后
        else:
            visit_order = max_order + 1  # 默认放在最后
        
        # 检查是否已有相同类型的餐厅
            cursor.execute('''
            SELECT id, name, site_id, visit_order
                FROM attractions
            WHERE itinerary_id = ? AND day_number = ? AND notes LIKE ?
        ''', (itinerary_id, day, f"%{meal_type}%"))
            
        existing_meal = cursor.fetchone()
            
        if existing_meal:
            # 更新现有餐厅
            attraction_id = existing_meal[0]
            logger.info(f"找到现有{meal_type}: ID={attraction_id}")
                
            # 更新餐厅信息
            cursor.execute('''
                UPDATE attractions
                SET name = ?, notes = ?, ai_optimized = 1
                WHERE id = ?
            ''', (
            restaurant.get('name', '未知餐厅'),
            f"{meal_type} - AI推荐理由: {restaurant.get('reason', '无')}",
                attraction_id
            ))
        else:
            # 插入新的餐厅
            # 先检查是否需要调整其他景点的顺序
            cursor.execute('''
                UPDATE attractions
                SET visit_order = visit_order + 1
                WHERE itinerary_id = ? AND day_number = ? AND visit_order >= ?
            ''', (itinerary_id, day, visit_order))
            
            # 先添加地点信息
            site_id = None
            if restaurant.get('latitude') and restaurant.get('longitude'):
                cursor.execute('''
                    INSERT INTO sites (name, latitude, longitude, address, type_desc)
                    VALUES (?, ?, ?, ?, ?)
                ''', (
                    restaurant.get('name', '未知餐厅'),
                    restaurant.get('latitude', 0),
                    restaurant.get('longitude', 0),
                    restaurant.get('address', '未知地址'),
                    restaurant.get('cuisine', '餐厅')
                ))
                site_id = cursor.lastrowid
            
            # 插入餐厅记录
            cursor.execute('''
                INSERT INTO attractions (itinerary_id, day_number, name, visit_order, transport, notes, ai_optimized, site_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ''', (
                itinerary_id,
                day,
                restaurant.get('name', '未知餐厅'),
                visit_order,
                '步行',  # 默认交通方式
                f"{meal_type} - AI推荐理由: {restaurant.get('reason', '无')}\n价格: {restaurant.get('price', '未知')}元/人",
                1,  # 标记为AI优化
                site_id
            ))
                
            conn.commit()
            logger.info("餐厅更新成功")
            
            # 获取更新后的行程信息
            cursor.execute('''
                SELECT a.name, a.day_number, a.visit_order, a.transport, a.notes, a.ai_optimized,
                        s.latitude, s.longitude, s.address, s.type_desc
                FROM attractions a
                LEFT JOIN sites s ON a.site_id = s.id
        WHERE a.itinerary_id = ? AND a.day_number = ?
        ORDER BY a.visit_order
    ''', (itinerary_id, day))
                
            attractions = []
            for row in cursor.fetchall():
                attractions.append({
                    'name': row[0],
                    'day': row[1],
                    'order': row[2],
                    'transport': row[3],
                    'notes': row[4],
                    'ai_optimized': bool(row[5]),
                    'coordinates': [row[6], row[7]] if row[6] and row[7] else None,
                    'address': row[8],
                    'type': row[9]
                })
            
            # 关闭数据库连接
            conn.close()
                
            return jsonify({
                'success': True,
                'message': '餐厅已成功更新',
                'updated_attractions': attractions
            })
        
    except sqlite3.Error as e:
        logger.error(f"数据库操作失败: {str(e)}")
        return jsonify({'success': False, 'error': f'数据库操作失败: {str(e)}'}), 500
    except Exception as e:
        logger.error(f"处理请求时出错: {str(e)}", exc_info=True)
        error_traceback = traceback.format_exc()
        logger.error(f"错误详情: {error_traceback}")
        return jsonify({'success': False, 'error': str(e)}), 500

# 添加一个测试路由
@app.route('/test', methods=['GET'])
def test():
    return jsonify({'status': 'ok', 'message': '后端服务器正在运行'})

# 添加一个健康检查路由
@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy', 'version': '1.0.0'})

# 添加清除会话的API接口，在test路由之前
@app.route('/clear_session', methods=['POST'])
def api_clear_session():
    try:
        data = request.get_json()
        if not data or 'trip_id' not in data:
            return jsonify({'error': '必须提供trip_id参数'}), 400
        
        trip_id = data['trip_id']
        result = clear_session(trip_id)
        logger.info(f"清除会话结果: {result}")
        return jsonify({'result': result})
    except Exception as e:
        logger.error(f"清除会话时出错: {str(e)}")
        return jsonify({'error': str(e)}), 500

@app.route('/clear_all_sessions', methods=['POST'])
def api_clear_all_sessions():
    try:
        result = clear_all_sessions()
        logger.info(f"清除所有会话结果: {result}")
        return jsonify({'result': result})
    except Exception as e:
        logger.error(f"清除所有会话时出错: {str(e)}")
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    try:
        # 尝试在 5002 端口启动
        logger.info("尝试在端口5002启动服务器")
        app.run(host='127.0.0.1', port=5002)
    except Exception as e:
        logger.error(f"启动服务器时出错: {str(e)}")
        # 如果 5002 失败，尝试其他端口
        try:
            logger.info("尝试使用备用端口 5003...")
            app.run(host='127.0.0.1', port=5003)
        except Exception as e:
            logger.error(f"备用端口也失败: {str(e)}")