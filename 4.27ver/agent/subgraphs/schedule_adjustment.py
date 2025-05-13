from langgraph.graph import StateGraph, END, START
from agent.state import TripState
from agent.app_context import AppContext
from langchain.prompts import PromptTemplate
import json
from typing import Dict, List, Any
import math
from langchain_core.prompts import PromptTemplate
from travel_tools.schedule import calculate_poi_change, get_total_attractions, search_pois_by_center, calculate_distances_to_pois, rank_pois_by_utility, extract_intent_from_user_input, generate_recommendation_reason,  cluster_pois_kmeans, calculate_cluster_centers, build_optimized_itinerary,optimize_daily_route

app_context = AppContext.get_instance()


def create_schedule_adjustment_graph_test() -> StateGraph:
    """创建整体调整子图"""
    graph = StateGraph(TripState)

    # 添加节点
    graph.add_node("process_input", process_input_node)
    graph.add_node("intent_recognition", intent_recognition_function)
    graph.add_node("calculate_poi_change", calculate_poi_change_node)
    graph.add_node("add_poi", add_poi_node)
    graph.add_node("remove_poi", remove_poi_node)
    graph.add_node("optimize_route", optimize_route_node)
    graph.add_node("classify_followup", classify_followup_node)
    graph.add_node("handle_selection", handle_selection_node)
    graph.add_node("update_preferences", update_preferences_node)
    graph.add_node("change_style", change_style_node)
    graph.add_node("invalid", invalid_node)

    # 设置入口节点
    graph.set_entry_point("process_input")

    # process_input 路由
    graph.add_conditional_edges(
        "process_input",
        routing_function
    )

    # 意图识别后路由到相应的节点
    graph.add_conditional_edges(
        "intent_recognition",
        lambda state: state["flow_state"]["adjustment_type"],
        {
            "change_days": "calculate_poi_change",
            "adjust_pace": "calculate_poi_change",
            "change_style": "change_style",
            "optimize_route": "optimize_route",
            "invalid": "invalid"
        }
    )
    
    # 从calculate_poi_change节点开始的条件边，根据poi_change的正负值路由
    graph.add_conditional_edges(
        "calculate_poi_change",
        lambda state: "add_poi" if state["flow_state"].get("poi_change", 0) > 0 else 
                      "remove_poi" if state["flow_state"].get("poi_change", 0) < 0 else 
                      "optimize_route",  # 无需增减POI时直接优化路线
        {
            "add_poi": "add_poi",
            "remove_poi": "remove_poi",
            "optimize_route": "optimize_route"
        }
    )
    
    # 添加和删除POI的节点直接到END，等待用户下一次输入
    graph.add_edge("add_poi", END)
    graph.add_edge("remove_poi", END)
    
    # 用户跟进输入处理
    graph.add_conditional_edges(
        "classify_followup",
        lambda state: state["flow_state"]["followup_action"],
        {
            "选择": "handle_selection",
            "偏好": "update_preferences",
            "无效": "invalid"
        }
    )
    
    # 处理用户选择后，进入路线优化
    graph.add_edge("handle_selection", "optimize_route")
    
    # 更新偏好后，根据先前的操作类型重新进入添加或删除POI节点
    graph.add_conditional_edges(
        "update_preferences",
        lambda state: state["flow_state"].get("action_type", "add_poi"),
        {
            "add_poi": "add_poi",
            "remove_poi": "remove_poi"
        }
    )
    
    # 终止节点连接
    graph.add_edge("optimize_route", END)
    graph.add_edge("change_style", END)
    graph.add_edge("invalid", END)

    return graph.compile()

def routing_function(state: TripState) -> str:
    """根据当前状态路由到适当的节点"""
    print(f"当前状态: on_progress={state['flow_state'].get('on_progress', False)}, should_continue={state.get('should_continue', True)}")
    
    # 检查是否有效输入或用户要求退出流程
    if not state.get("should_continue", True):
        print("用户要求退出流程或输入无效，路由到 invalid")
        return "invalid"
    
    # 检查是否在进行中的推荐流程
    if state["flow_state"].get("on_progress", False):
        print("路由到 classify_followup")
        return "classify_followup"
    else:
        print("路由到 intent_recognition")
        return "intent_recognition"

def process_input_node(state: TripState) -> TripState:
    """处理输入节点：验证用户输入并设置初始状态"""
    user_input = state["user_input"]
    history = state.get("conversation_history", [])

    # 验证用户输入
    if not user_input.strip():
        msg = "❗️请输入您的行程调整需求，如把行程压缩到3天或每天安排少一点景点"
        history.append({"role": "assistant", "content": msg})
        return {
            **state,
            "response": msg,
            "conversation_history": history,
            "should_continue": False,
            "flow_state": {**state.get("flow_state", {}), "on_progress": False}
        }
    
    return {
        **state,
        "conversation_history": history,
        "should_continue": True
    }

def intent_recognition_function(state: TripState) -> TripState:
    """意图识别节点：提取用户的调整意图"""
    user_input = state["user_input"]
    
    context_manager = app_context.context_manager

    # 直接使用context_manager中的current_trip_id
    print(f"当前context_manager使用的trip_id: {context_manager.current_trip_id}")
    
    # 获取行程数据
    trip_json = context_manager.get_current_trip()
    print(f"获取到的行程数据: {trip_json}")
    
    # 如果没有获取到行程数据，输出警告
    if not trip_json:
        print(f"⚠️ 警告：无法获取行程数据，使用context_manager中的默认行程ID: {context_manager.current_trip_id}")

    # 调用封装的函数提取意图
    extracted_info = extract_intent_from_user_input(user_input, trip_json, app_context)

    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "adjustment_type": extracted_info.get("adjust_type"),
            "target_days": extracted_info.get("target_days"),
            "daily_limit": extracted_info.get("daily_limit"),
            "style_replace": extracted_info.get("style_replace"),
            "add_pois": extracted_info.get("add_pois"),
            "prefer_tags": extracted_info.get("prefer_tags"),
            "error": extracted_info.get("error"),
            "on_progress": False  # 初始设置为false，在add_poi或remove_poi节点中设为true
        }
    }

def calculate_poi_change_node(state: TripState) -> TripState:
    """计算POI变化节点：处理天数或节奏变更请求，计算需要增减的POI数量"""
    print("进入计算POI变化节点")
    context_manager = app_context.context_manager
    # 获取当前行程数据
    trip_json = context_manager.get_current_trip()
    current_days = trip_json["metadata"]["total_days"]
    target_days = state["flow_state"]["target_days"]
    daily_limit = state["flow_state"]["daily_limit"]
    print(f"当前天数: {current_days}, 目标天数: {target_days}, 每日限制: {daily_limit}")
    
    # 如果没有指定daily_limit，使用当前的每日平均景点数
    if not daily_limit:
        total_attractions = get_total_attractions(trip_json)
        daily_limit = total_attractions / current_days
        
    # 获取当前所有景点
    existing_pois = []
    for day in trip_json["daily_itinerary"]:
        existing_pois.extend([item for item in day["schedule"] if item["poi_type"] == "景点"])
    print(f"现有POI数量: {len(existing_pois)}")
    
    # 计算需要改变的景点数量
    poi_change = calculate_poi_change(existing_pois, current_days, target_days, daily_limit)
    
    if poi_change < 0:
        print(f"需要减少{abs(poi_change)}个景点")
    elif poi_change > 0:
        print(f"需要增加{poi_change}个景点")
    else:
        print("景点数量无需调整，将进行路线优化")
    
    # 查找行程单中的最后一个POI作为搜索中心点
    last_poi = None
    last_day = trip_json["daily_itinerary"][-1]  # 最后一天
    for item in reversed(last_day["schedule"]):
        if item["poi_type"] == "景点":
            last_poi = item
            break
    
    # 将需要改变的景点数量保存到状态中
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "poi_change": poi_change,
            "last_poi": last_poi,
            "existing_pois": existing_pois
        }
    }

def add_poi_node(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    """添加POI节点：查找并推荐新POI"""
    print("进入添加POI节点")
    
    # 获取状态中的数据
    poi_change = state["flow_state"]["poi_change"]
    last_poi = state["flow_state"]["last_poi"]
    existing_pois = state["flow_state"]["existing_pois"]
    prefer_tags = state["flow_state"]["prefer_tags"]
    
    # 确保poi_change是整数
    try:
        poi_change = int(poi_change)
    except (ValueError, TypeError):
        # 如果无法转换为整数，默认为1
        print(f"警告: poi_change值({poi_change})无法转换为整数，使用默认值1")
        poi_change = 1
    
    # 如果找到了最后一个POI，以它为中心搜索周边景点
    recommended_pois = []
    if last_poi:
        print(f"以最后一个POI '{last_poi['name']}' 为中心搜索周边景点...")
        center_coordinates = last_poi["coordinates"]
        nearby_pois = search_pois_by_center(center_coordinates)
        print("nearby_pois",nearby_pois)
        print(f"搜索到 {len(nearby_pois)} 个周边景点")
        
        # 使用效用评分对POI进行排序
        if nearby_pois:
            ranked_pois = rank_pois_by_utility(
                nearby_pois, 
                existing_pois, 
                prefer_tags=prefer_tags,
                llm_client=app_context.llm
            )
            
            # 获取前N个推荐景点（N=需要增加的景点数量）
            recommended_count = min(poi_change, len(ranked_pois))
            recommended_pois = ranked_pois[:recommended_count]
            
            recommended_pois_with_reason=[]
            for poi in recommended_pois:
                recommendation_reason = generate_recommendation_reason(
                    poi.get("name","") + poi.get("type", ""), 
                    poi['score_details'],
                    prefer_tags, 
                    app_context.llm,
                    is_removal=False  # 指定为推荐场景
                )
                
                recommended_pois_with_reason.append({
                    "name": poi['name'],
                    "type": poi['type'],
                    "rating": poi['rating'],
                    "utility_score": poi['utility_score'],
                    "recommendation_reason": recommendation_reason,
                    "coordinates": poi.get('coordinates', [0, 0]),
                    "address": poi.get('address', ''),
                    "image_url": poi.get('image_url', ''),
                    "distance": poi.get('distance', '未知')
                })
            
            print("\n推荐的新景点:")
            for poi in recommended_pois_with_reason:
                print(f"{poi['name']} - {poi['rating']} - {poi['recommendation_reason']}")
            
            # 构建响应文本
            response_text = "我为您找到了以下几个景点作为添加选择：\n\n"
            
            for i, rec in enumerate(recommended_pois_with_reason[:5], 1):  # 限制最多5个推荐
                response_text += f"{i}. {rec['name']} - {rec['type']}\n"
                response_text += f"   评分：{rec['rating']}\n"
                response_text += f"   推荐理由：{rec['recommendation_reason']}\n\n"
            
            response_text += "您可以选择其中一个或多个添加到行程中，或者告诉我您的其他偏好，我可以重新推荐。"
            
            # 构建结构化JSON数据
            json_data = {
                "data_type": "poi_recommendations",
                "recommendations": recommended_pois_with_reason[:5]
            }
            
            # 在响应中嵌入JSON数据
            response_with_json = response_text + f"\n<!--JSON_DATA:{json.dumps(json_data, ensure_ascii=False)}-->"
            
            # 更新状态
            history = state.get("conversation_history", [])
            history.append({"role": "assistant", "content": response_with_json})
            
            # 保存推荐列表到上下文管理器
            context_manager.update_recommendations(recommended_pois_with_reason)
            
            return {
                **state,
                "flow_state": {
                    **state["flow_state"],
                    "recommended_pois": recommended_pois_with_reason,
                    "action_type": "add_poi",
                    "on_progress": True,  # 设置为true，表示流程在进行中，等待用户输入
                    "data_type": "poi_recommendations",
                    "data": recommended_pois_with_reason[:5]
                },
                "response": response_with_json,
                "conversation_history": history,
                "data_type": "poi_recommendations",
                "data": recommended_pois_with_reason[:5]
            }
    else:
        print("未找到可用的POI作为搜索中心点")
        response_text = "抱歉，未能找到适合添加的POI。请提供更多偏好信息。"
        
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": response_text})
        
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "recommended_pois": [],
                "action_type": "add_poi",
                "on_progress": False  # 无法推荐，流程结束
            },
            "response": response_text,
            "conversation_history": history
        }

def remove_poi_node(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    """删除POI节点：根据效用评分删除最不合适的POI"""
    print("进入删除POI节点")
    
    # 获取状态中的数据
    poi_change = state["flow_state"]["poi_change"]
    existing_pois = state["flow_state"]["existing_pois"]
    prefer_tags = state["flow_state"]["prefer_tags"]
    
    # 确保poi_change是整数并转为正数（因为它本来是负数）
    try:
        # 取绝对值，因为我们需要知道要删除多少个POI
        poi_change = abs(int(poi_change))
    except (ValueError, TypeError):
        print(f"警告: poi_change值({poi_change})无法转换为整数，使用默认值1")
        poi_change = 1
    
    print(f"需要删除 {poi_change} 个POI")
    if not existing_pois:
        print("没有现有的POI可供删除")
        response_text = "当前行程中没有景点可供删除。"
        
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": response_text})
        
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "removed_pois": [],
                "action_type": "remove_poi",
                "on_progress": False  # 无法删除，流程结束
            },
            "response": response_text,
            "conversation_history": history
        }
    
    # 使用效用评分对现有POI进行排序
    ranked_pois = rank_pois_by_utility(
        existing_pois, 
        existing_pois, 
        prefer_tags=prefer_tags,
        llm_client=app_context.llm,
        is_reversed=True,  # 升序，效用分低的在前面
        exclude_self=True  # 计算兼容性时排除自身
    )
    
    # 获取效用评分最低的POI作为要删除的POI
    to_remove_count = min(poi_change, len(ranked_pois))
    to_remove_pois = ranked_pois[:to_remove_count]
    
    # 为要删除的POI生成删除理由
    removed_pois_with_reason = []
    for poi in to_remove_pois:
        score_details = poi["score_details"]
        
        # 生成删除理由
        removal_reason = generate_recommendation_reason(
            poi.get("name", "") + poi.get("type", ""), 
            score_details,
            prefer_tags, 
            app_context.llm,
            is_removal=True  # 指定为删除场景
        )
        
        removed_pois_with_reason.append({
            "name": poi.get("name", ""),
            "type": poi.get("type", ""),
            "rating": poi.get("rating", "无评分"),
            "utility_score": poi["utility_score"],
            "removal_reason": removal_reason,
            "coordinates": poi.get('coordinates', [0, 0]),
            "address": poi.get('address', ''),
            "image_url": poi.get('image_url', ''),
            "distance": poi.get('distance', '未知')
        })
    
    print("\n建议删除的POI:")
    for poi in removed_pois_with_reason:
        print(f"{poi['name']} - {poi['rating']} - {poi['removal_reason']}")
    
    # 构建响应文本
    response_text = "建议从行程中删除以下景点：\n\n"
    
    for i, rec in enumerate(removed_pois_with_reason[:5], 1):  # 限制最多5个推荐
        response_text += f"{i}. {rec['name']} - {rec['type']}\n"
        response_text += f"   评分：{rec['rating']}\n"
        response_text += f"   删除理由：{rec['removal_reason']}\n\n"
    
    response_text += "您是否同意删除这些景点？或者您可以告诉我您的其他偏好，我可以重新推荐。"
    
    # 更新状态
    history = state.get("conversation_history", [])
    history.append({"role": "assistant", "content": response_text})
    
    # 保存推荐列表到上下文管理器
    context_manager.update_recommendations(removed_pois_with_reason)
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "removed_pois": removed_pois_with_reason,
            "action_type": "remove_poi",
            "on_progress": True  # 设置为true，表示流程在进行中，等待用户输入
        },
        "response": response_text,
        "conversation_history": history
    }

def classify_followup_node(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    """分类用户后续输入"""
    user_input = state["user_input"]
    action_type = state["flow_state"].get("action_type", "")
    recommended_pois = context_manager.get_recommendations()
    
    # 使用LLM分析用户输入
    prompt = PromptTemplate.from_template("""
    # 背景信息
    你是一个旅行助手，刚刚给用户推荐了{action_type}行程中的景点。
    
    用户输入："{user_input}"
    
    推荐的景点列表:
    {pois_list}
    
    请根据输入内容分类：
    - 如果用户表示同意修改，请回复 "选择"
    - 如果用户表示想要选择某个或多个推荐的景点，请回复 "选择"
    - 如果用户表示不同意修改，请回复 "偏好"                                      
    - 如果用户提供了新的偏好或要求，请回复 "偏好"
    - 如果用户表示不想继续这个推荐过程，或想做其他事情，请回复 "退出"
    - 如果输入无效或模糊，但似乎与景点选择有关，请回复 "无效"
    
    # 输出格式
    请严格按"选择"、"偏好"、"退出"或"无效"中的一种回答，不要添加任何其他内容：
    """)
    
    # 构造POI列表文本
    pois_text = ""
    for i, poi in enumerate(recommended_pois, 1):
        if action_type == "add_poi":
            pois_text += f"{i}. {poi['name']} - 推荐理由: {poi.get('recommendation_reason', '未知')}\n"
        else:
            pois_text += f"{i}. {poi['name']} - 删除理由: {poi.get('removal_reason', '未知')}\n"
    
    # 使用LLM分析用户输入
    action_type_text = "添加" if action_type == "add_poi" else "删除"
    followup_action = app_context.llm.predict(
        prompt.format(action_type=action_type_text, user_input=user_input, pois_list=pois_text)
    ).strip()
    
    print(f"用户跟进动作: {followup_action}")
    
    # 如果用户想退出推荐流程
    if followup_action == "退出":
        history = state.get("conversation_history", [])
        response_text = "好的，我们可以稍后再继续景点推荐。您有什么其他需要帮助的吗？"
        history.append({"role": "assistant", "content": response_text})
        
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "on_progress": False,  # 将on_progress设置为False，退出当前推荐流程
                "followup_action": "无效"  # 使用"无效"路由到invalid节点
            },
            "response": response_text,
            "conversation_history": history,
            "should_continue": False  # 标记不应该继续当前流程
        }
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "followup_action": followup_action
        }
    }

def handle_selection_node(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    """处理用户选择"""
    user_input = state["user_input"]
    action_type = state["flow_state"].get("action_type", "")
    recommended_pois = context_manager.get_recommendations()
    
    # 使用LLM提取用户选择的POI
    prompt = PromptTemplate.from_template("""
    # 背景信息
    用户输入: "{user_input}"
    
    推荐的景点列表:
    {pois_list}
    
    # 任务
    请分析用户的输入，确定用户选择了哪些景点。
    
    # 输出格式
    请仅返回用户选择的景点索引列表（例如 [1, 3]），如果无法确定，则返回 []：
    """)
    
    # 构造POI列表文本
    pois_text = ""
    for i, poi in enumerate(recommended_pois, 1):
        pois_text += f"{i}. {poi['name']}\n"
    
    # 使用LLM提取用户选择
    selection_text = app_context.llm.predict(
        prompt.format(user_input=user_input, pois_list=pois_text)
    ).strip()
    
    # 解析选择结果
    try:
        selected_indices = json.loads(selection_text)
        selected_pois = [recommended_pois[i-1] for i in selected_indices if 0 < i <= len(recommended_pois)]
    except:
        selected_pois = []
    
    # 构建响应
    if selected_pois:
        selected_names = [poi["name"] for poi in selected_pois]
        action_word = "添加" if action_type == "add_poi" else "删除"
        response_text = f"已确认{action_word}以下景点：{', '.join(selected_names)}。接下来将为您优化行程路线。"
        
        # 构建结构化JSON数据
        json_data = {
            "data_type": "selected_pois",
            "selections": [
                {
                    "name": poi["name"],
                    "type": poi.get("type", ""),
                    "rating": poi.get("rating", ""),
                    "coordinates": poi.get("coordinates", [0, 0]),
                    "address": poi.get("address", ""),
                } for poi in selected_pois
            ]
        }
        
        # 在响应中嵌入JSON数据
        response_with_json = response_text + f"\n<!--JSON_DATA:{json.dumps(json_data, ensure_ascii=False)}-->"
    else:
        response_text = "未能理解您的选择，请重新指定您想要的景点。"
        response_with_json = response_text
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_with_json})
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "selected_pois": selected_pois,
            "on_progress": False,  # 用户已做出选择，流程进入下一阶段
            "data_type": "selected_pois" if selected_pois else None,
            "data": selected_pois if selected_pois else None
        },
        "response": response_with_json,
        "conversation_history": history,
        "data_type": "selected_pois" if selected_pois else None,
        "data": selected_pois if selected_pois else None
    }

def update_preferences_node(state: TripState) -> TripState:
    """更新用户偏好"""
    user_input = state["user_input"]
    
    # 使用LLM提取用户的新偏好
    prompt = PromptTemplate.from_template("""
    # 背景信息
    用户输入: "{user_input}"
    
    # 任务
    请从用户输入中提取新的偏好标签。这些标签将用于重新推荐景点。
    
    # 输出格式
    请以JSON格式返回提取的偏好标签列表：
    ["标签1", "标签2", ...]
    """)
    
    # 使用LLM提取偏好
    preference_text = app_context.llm.predict(
        prompt.format(user_input=user_input)
    ).strip()
    
    # 解析偏好
    try:
        new_preferences = json.loads(preference_text)
    except:
        new_preferences = []
    
    # 构建响应
    if new_preferences:
        response_text = f"已更新您的偏好：{', '.join(new_preferences)}。将为您重新推荐景点。"
    else:
        response_text = "未能提取到新的偏好，将使用原有偏好重新推荐。"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_text})
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "prefer_tags": new_preferences if new_preferences else state["flow_state"].get("prefer_tags", [])
        },
        "response": response_text,
        "conversation_history": history
    }

def optimize_route_node(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    """优化路线节点"""
    print("进入优化路线节点")
    
    # 获取当前行程数据
    trip_json = context_manager.get_current_trip()
    history = state.get("conversation_history", [])
    
    # 获取所有POI
    all_pois = []
    for day in trip_json["daily_itinerary"]:
        for item in day["schedule"]:
            if item["poi_type"] == "景点":
                all_pois.append(item)
    
    if not all_pois:
        msg = "当前行程中没有景点，无需优化。"
        history.append({"role": "assistant", "content": msg})
        return {
            **state,
            "response": msg,
            "conversation_history": history,
            "flow_state": {**state["flow_state"], "on_progress": False}
        }
    
    # 获取总天数
    total_days = trip_json["metadata"]["total_days"]
    
    # 获取每天最大POI数量
    avg_pois_per_day = math.ceil(len(all_pois) / total_days)
    max_pois_per_day = max([
        sum(1 for item in day["schedule"] if item["poi_type"] == "景点")
        for day in trip_json["daily_itinerary"]
    ])
    pois_per_day = max(avg_pois_per_day, max_pois_per_day)
    
    try:
        # 1. 使用KMeans进行聚类，限制每天POI数量
        print(f"开始KMeans聚类... 每天最多{pois_per_day}个POI")
        clusters = cluster_pois_kmeans(all_pois, total_days, pois_per_day)
        
        # 2. 构建新的行程，保持原始行程格式
        print("构建优化后的行程...")
        new_daily_itinerary = []
        
        # 遍历原始行程，替换POI部分但保留其他内容
        for day_idx, day_data in enumerate(trip_json["daily_itinerary"]):
            if day_idx < len(clusters):
                # 提取当天的非POI项目
                non_poi_items = [item for item in day_data.get("schedule", []) 
                              if item.get("poi_type") != "景点"]
                
                # 获取聚类后的POI并优化访问顺序
                day_pois = clusters[day_idx]
                optimized_pois = []
                
                if day_pois:
                    # 优化当天POI的访问顺序
                    optimized_pois = optimize_daily_route(day_pois)
                
                # 构建新的行程项目列表，保留原POI的所有属性
                poi_items = []
                for poi in optimized_pois:
                    # 创建新的POI项，但保留原有所有属性
                    poi_item = {}
                    for key, value in poi.items():
                        poi_item[key] = value
                    
                    poi_items.append(poi_item)
                
                # 合并POI和非POI项目
                new_schedule = non_poi_items + poi_items
                
                # 创建新的天数据，完全复制原始数据结构
                new_day = day_data.copy()
                new_day["schedule"] = new_schedule
                
                new_daily_itinerary.append(new_day)
            else:
                # 如果聚类数少于原始天数，保持剩余天原样
                new_daily_itinerary.append(day_data)
        
        # 更新行程JSON
        trip_json["daily_itinerary"] = new_daily_itinerary
        print(f"优化后的行程: {trip_json}")
        # 生成优化说明
        msg = "✅ 已完成行程优化：\n"
        msg += "1. 将地理位置相近的景点安排在同一天\n"
        msg += "2. 优化了每天景点的游览顺序，减少往返时间\n"
        msg += "3. 严格保持了原有行程格式和所有景点属性"
        
    except Exception as e:
        print(f"优化过程中出现错误: {e}")
        msg = "❌ 优化行程时出现错误，请稍后重试。"
        trip_json = None
    
    # 更新历史
    history.append({"role": "assistant", "content": msg})
    
    return {
        **state,
        "response": msg,
        "conversation_history": history,
        "flow_state": {
            **state["flow_state"],
            "on_progress": False,
            "updated_trip": trip_json,  # 添加更新后的行程数据
            "data_type": "trip"  # 标识返回数据类型为完整行程
        }
    }

def change_style_node(state: TripState) -> TripState:
    """风格替换节点"""
    print("进入风格替换节点")
    
    style_replace = state["flow_state"].get("style_replace", {})
    
    if not style_replace:
        response_text = "未能识别要替换的风格，请提供更具体的风格替换要求。"
    else:
        old_styles = list(style_replace.keys())
        new_styles = [item for sublist in style_replace.values() for item in sublist]
        
        response_text = f"已将行程风格从 {', '.join(old_styles)} 调整为 {', '.join(new_styles)}。"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_text})
    
    return {
        **state,
        "response": response_text,
        "conversation_history": history,
        "flow_state": {
            **state["flow_state"],
            "on_progress": False  # 标记流程结束
        }
    }

def invalid_node(state: TripState) -> TripState:
    """无效请求节点"""
    print("进入无效请求节点")
    flow_state = state.get("flow_state", {})
    # 检查是否是从退出推荐流程过来的
    if "should_continue" in state and state["should_continue"] is False:
        active_flow = None
        response_text = "已退出景点推荐。您可以继续浏览行程或提出其他需求。"
    else:
        active_flow = flow_state.get("active_flow", None)
        error = state["flow_state"].get("error", "未能理解您的请求")
        response_text = f"抱歉，{error}。请尝试更明确的表达，例如'把行程压缩到3天'或'每天不超过2个景点'。"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_text})
    
    return {
        **state,
        "response": response_text,
        "conversation_history": history,
        "flow_state": {
            **state["flow_state"],  
            "on_progress": False,
            "active_flow": active_flow  # 保留原有active_flow值
        }
    }

