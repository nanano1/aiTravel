import json
import os
from langgraph.graph import END, StateGraph
import pandas as pd
from agent.state import TripState
from agent.app_context import AppContext
from langchain.prompts import PromptTemplate
from travel_tools.schedule import search_pois_by_center, calculate_distance
from langgraph.graph import StateGraph, END
from agent.state import TripState
from agent.app_context import AppContext
import numpy as np

app_context = AppContext.get_instance()

def create_poi_replacement_graph():
    graph = StateGraph(TripState)

    # 添加节点
    graph.add_node("process_input", process_user_input)  # 处理用户输入
    graph.add_node("init_recommend", initial_user_input)  # 初始化替换和获取POI信息
    graph.add_node("classify_input", classify_followup_input)  # 分析用户后续输入
    graph.add_node("generate_recommendations", generate_recommendations)  # 生成推荐
    graph.add_node("handle_selection", handle_selection)  # 处理用户选择
    graph.add_node("update_preferences", update_preferences)  # 更新用户偏好
    graph.add_node("handle_invalid", handle_invalid_input)  # 处理无效输入

    # 入口：统一由 process_input 开始
    graph.set_entry_point("process_input")

    # process_input 之后，判断 flow_state
    graph.add_conditional_edges(
        "process_input",
        routing_function
    )

    # 初始化推荐后 → 生成推荐
    graph.add_edge("init_recommend", "generate_recommendations")

    # 推荐完成 → END
    graph.add_edge("generate_recommendations", END)

    # classify_input 后续分支
    graph.add_conditional_edges(
        "classify_input",
        lambda state: state["flow_state"]["followup_action"],
        path_map={
            "选择": "handle_selection",
            "偏好": "update_preferences",
            "退出": "handle_invalid",
            "无效": "handle_invalid"
        }
    )

    # 用户选择 → 结束流程
    graph.add_edge("handle_selection", END)

    # 补充偏好 → 重新生成推荐
    graph.add_edge("update_preferences", "generate_recommendations")

    # 无效输入 → 结束
    graph.add_edge("handle_invalid", END)

    return graph.compile()

def routing_function(state: TripState) -> str:
    print(f"当前状态: {state['flow_state']['on_progress']}")
    if not state["should_continue"]:
        print("路由到 handle_invalid")
        return "handle_invalid"
    elif state["flow_state"]["on_progress"]:
        print("路由到 classify_input")
        return "classify_input"
    elif not state["flow_state"]["on_progress"]:
        print("路由到 init_recommend")
        return "init_recommend"

def process_user_input(state: TripState) -> TripState:
    user_input = state["user_input"]
    history = state["conversation_history"]

    if not user_input.strip():
        msg = "❗️请输入您想替换的景点，如\"帮我换掉第一天的故宫\"。"
        history.append({"role": "assistant", "content": msg})
        return {
            **state,
            "response": msg,
            "conversation_history": history,
            "should_continue": False,
            "flow_state": {"on_progress": False}
        }
    return {
        **state,
        "conversation_history": history,
        "should_continue": True,
    }

def classify_followup_input(state: TripState) -> TripState:
    llm = app_context.llm
    user_input = state["user_input"]
    all_fitPOI = app_context.context_manager.get_recommendations()
    last_recommendations = all_fitPOI[:5]
    print("last_recommendations", last_recommendations)
    pois_str = "\n".join([f"{i+1}. {p['name']}" for i, p in enumerate(last_recommendations)])

    prompt = PromptTemplate.from_template("""
    # 背景信息
    你是一个旅行助手，刚刚给用户推荐了几个景点，你需要解析下一步用户要做什么。
                                          
    用户输入："{user_input}"
    上次给他推荐的景点:
    {pois}

    请根据输入内容分类：
    - 如果用户表示想要更换为{pois}里的其中一个景点，请回复 "选择"
    - 如果用户提供了新的要求偏好，比如景点类型、评分等新的要求,请回复 "偏好"
    - 如果用户表示不想继续这个推荐过程，或者想做其他事情，请回复 "退出"
    - 如果输入无效或模糊，但似乎还与景点选择有关，请回复 "无效"

    # 输出格式
    请严格按"无效"、"选择"、"偏好"、"退出"中的一种回答，不要添加任何其他内容，也不要解释和注释：
    """)

    classification = llm.predict(prompt.format(user_input=user_input, pois=pois_str)).strip()
    print("classification", classification)
    
    # 如果用户想退出推荐流程
    if classification == "退出":
        return {
            **state,
            "flow_state": {
                "on_progress": False,  # 将on_progress设置为False，退出当前推荐流程
                "followup_action": "退出"  # 依然使用"无效"作为路由的值，但会在handle_invalid_input处理
            },
            "response": "好的，我们可以稍后再继续景点推荐。您有什么其他需要帮助的吗？",
            "should_continue": False  # 标记不应该继续当前流程
        }
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "followup_action": classification
        }
    }

def initial_user_input(state: TripState) -> TripState:
    print("进入initial_user_input节点")
    context_manager = app_context.context_manager
    user_input = state.get("user_input", "")
    print("处理用户输入...", user_input)
    if not user_input.strip():
        message = "❗️请输入您想替换的景点，例如帮我换掉第一天的故宫。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False,
            "flow_state": {**state["flow_state"], "active_flow": "景点替换"}
        }

    print("进入提取意图节点...")
    # 提取替换景点的意图
    extract_prompt = PromptTemplate.from_template("""
    # 角色：智能旅行助手信息提取专家

    # 任务
    请从用户输入中提取出用户想要替换行程中哪个景点的信息。

    # 用户原始输入
    "{user_input}"

    # 当前行程概览
    {itinerary_summary}

    # 提取要求
    请提取以下信息：
    1. 要替换的景点名称
    2. 替换景点所在的天数
    3. 替换景点的顺序
    
    # 输出格式
    请严格按以下JSON格式回答，不添加任何其他内容:
    {{
      "replace_poi_info": {{
        "target_poi": {{
          "name": "被替换POI的名称", 
          "coordinates": [经度, 纬度],
          "day": 第几天,
          "order": 访问顺序,
        }},
        "prev_poi": {{
          "name": "前一个景点名称",
          "coordinates": [经度, 纬度]
        }},
        "next_poi": {{
          "name": "后一个景点名称",
          "coordinates": [经度, 纬度]
        }}
      }},
      "missing_info": false
    }}

    如果无法提取完整信息，请将missing_info设为true。
    """)
    
    # 获取当前行程
    trip_json = context_manager.get_current_trip()
    print("initial_user_input中trip_json", trip_json)
    # 生成行程概览
    itinerary_summary = ""
    if trip_json:
        for day in trip_json["daily_itinerary"]:
            itinerary_summary += f"\n第{day['day']}天：\n"
            for i, poi in enumerate(day["schedule"]):
                if poi["poi_type"] == "景点":
                    coords = poi.get("coordinates", [0, 0])
                    itinerary_summary += f"  {i+1}. {poi['name']} - 坐标: [{coords[0]}, {coords[1]}]\n"
    
    # 提取意图
    extracted_text = app_context.llm.predict(extract_prompt.format(
        user_input=user_input,
        itinerary_summary=itinerary_summary
    ))
    try:
        # 提取JSON部分
        import re
        json_match = re.search(r'({.*})', extracted_text, re.DOTALL)
        if json_match:
            extracted_json = json.loads(json_match.group(1))
        else:
            extracted_json = json.loads(extracted_text)
        
        missing_info = extracted_json.get("missing_info", True)
        replace_poi_info = extracted_json.get("replace_poi_info", {})
        print("initial_user_input中replace_poi_info", replace_poi_info)
        if missing_info:
            message = "❗️未能识别要替换的景点，请详细说明要替换的景点名称和天数。"
            history = state.get("conversation_history", [])
            history.append({"role": "assistant", "content": message})
            return {
                **state,
                "response": message,
                "conversation_history": history,
                "should_continue": False,
                "user_selected": False
            }
        
        # 更新上下文管理器
        context_manager.update_poi_replace_info(replace_poi_info)
        
        return {
            **state,
            "replace_poi_info": replace_poi_info,
            "user_selected": False,
            "should_continue": True,
            "flow_state": {"active_flow": "景点替换", "on_progress": True}
        }
        
    except Exception as e:
        print(f"解析意图失败: {e}")
        message = "❗️解析您的请求时出现问题，请重新描述要替换的景点。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False,
            
        }

def generate_recommendations(state: TripState) -> TripState:
    """生成景点推荐"""
    # 使用AppContext中的context_manager
    context_manager = app_context.context_manager
    print("进入generate_recommendations节点")
    
    # 获取当前的替换景点信息
    replace_poi_info = context_manager.get_poi_replace_info()
    preferences = state.get("preferences", {})
    replace_poi_info_fromstate = state.get("replace_poi_info", {})
    print("替换景点信息:", replace_poi_info)
    print("替换景点信息从state:", replace_poi_info_fromstate)
    print("state:", state)
    target_poi = replace_poi_info.get("target_poi", {})
    prev_poi = replace_poi_info.get("prev_poi", {})
    next_poi = replace_poi_info.get("next_poi", {})
    
    # 如果有前后景点，计算中点作为搜索中心
    center_poi = None
    if prev_poi.get("coordinates") and next_poi.get("coordinates"):
        # 计算两点的中点
        prev_coords = prev_poi["coordinates"]
        next_coords = next_poi["coordinates"]
        center_poi = [
            (prev_coords[0] + next_coords[0]) / 2,
            (prev_coords[1] + next_coords[1]) / 2
        ]
        print(f"使用前后景点中点作为搜索中心: {center_poi}")
    else:
        # 使用目标景点坐标作为搜索中心
        center_poi = target_poi.get("coordinates")
        print(f"使用目标景点坐标作为搜索中心: {center_poi}")
    
    # 如果没有有效的搜索中心，返回错误
    if not center_poi:
        message = "❗️无法确定搜索中心，无法推荐替换景点。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "flow_state": {"active_flow": "景点替换", "on_progress": False}
        }
    
    # 搜索周边景点
    searched_pois = search_pois_by_center(center_poi, radius=5)  # 5km范围内搜索
    
    if not searched_pois or len(searched_pois) == 0:
        message = "❗️未找到符合条件的替换景点。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "flow_state": {"active_flow": "景点替换", "on_progress": False}
        }
    
    # 使用LLM筛选出最符合需求的5个景点
    filter_prompt = PromptTemplate.from_template("""
    # 角色：景点推荐专家

    # 任务
    你需要从提供的景点列表中选择最适合替换目标景点的5个景点。

    # 数据
    目标景点: {target_poi}

    # 候选景点列表
    {pois_list}

    # 筛选标准
    1. 景点类型与原景点相似度
    2. 景点评分和受欢迎程度
    3. 景点的游览价值
    4. 景点的知名度和特色

    # 输出格式
    返回一个JSON数组，包含5个最合适的景点索引（从0开始）和推荐理由：
    [
      {{"index": 3, "reason": "这是一个与原景点类型相似的历史遗迹，评分高，具有很高的游览价值"}},
      {{"index": 7, "reason": "这个景点提供了不同的体验，但与原行程主题一致，且知名度较高"}},
      ...
    ]
    """)
    
    # 格式化POI列表
    pois_list_text = ""
    for i, poi in enumerate(searched_pois):
        rating = poi.get("rating", "无评分")
        poi_type = poi.get("type", "无类型")
        address = poi.get("address", "无地址")
        
        pois_list_text += f"{i}. 名称: {poi['name']}\n"
        pois_list_text += f"   类型: {poi_type}\n"
        pois_list_text += f"   评分: {rating}\n"
        pois_list_text += f"   地址: {address}\n\n"
    
    # 调用LLM筛选
    filter_result = app_context.llm.predict(
        filter_prompt.format(
            target_poi=json.dumps(target_poi, ensure_ascii=False),
            pois_list=pois_list_text
        )
    )
    
    try:
        # 提取JSON数组
        import re
        json_match = re.search(r'(\[.*\])', filter_result, re.DOTALL)
        if json_match:
            selected_indices = json.loads(json_match.group(1))
        else:
            selected_indices = json.loads(filter_result)
        
        # 获取推荐的POI
        recommended_pois = []
        for item in selected_indices:
            index = item.get("index")
            if 0 <= index < len(searched_pois):
                poi = searched_pois[index].copy()
                poi["recommendation_reason"] = item.get("reason", "推荐理由未提供")
                recommended_pois.append(poi)
        
        if not recommended_pois:
            message = "❗️筛选推荐景点时出现问题。"
            history = state.get("conversation_history", [])
            history.append({"role": "assistant", "content": message})
            return {
                **state,
                "response": message,
                "conversation_history": history,
                "should_continue": False,
                "flow_state": {"active_flow": "景点替换", "on_progress": False}
            }
        
        # 构建响应文本
        response_text = f"为您找到以下可替换\"{target_poi['name']}\"的景点：\n\n"
        
        for i, poi in enumerate(recommended_pois, 1):
            response_text += f"{i}. {poi['name']} - {poi.get('type', '无类型')}\n"
            response_text += f"   评分：{poi.get('rating', '无评分')}\n"
            response_text += f"   推荐理由：{poi['recommendation_reason']}\n\n"
        
        response_text += "您可以选择其中一个作为替代景点，或告诉我您的其他偏好，我可以重新推荐。"
        
        # 构建结构化JSON数据
        json_data = {
            "data_type": "poi_replace",
            "replace_poi_info": replace_poi_info,
            "recommendations": [
                {
                    "uid": poi.get("poi_id", ""),
                    "tel": poi.get("tel", "无电话"),
                    "opentime_week": poi.get("opentime_week", "无营业时间"),
                    "name": poi.get("name", ""),
                    "type": poi.get("type", ""),
                    "rating": poi.get("rating", ""),
                    "recommendation_reason": poi.get("recommendation_reason", ""),
                    "lat": poi.get("coordinates", [0, 0])[0],
                    "lng": poi.get("coordinates", [0, 0])[1],
                    "address": poi.get("address", ""),
                    "day": target_poi.get("day", 1),
                    "order": target_poi.get("order", 1)
                } for poi in recommended_pois
            ]
        }
        
        # 在响应中嵌入JSON数据
        response_with_json = response_text + f"\n<!--JSON_DATA:{json.dumps(json_data, ensure_ascii=False)}-->"
        
        # 更新上下文管理器中的推荐列表
        context_manager.update_recommendations(recommended_pois)
        
        # 更新历史
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": response_with_json})
        
        return {
            **state,
            "recommended_pois": recommended_pois,
            "response": response_with_json,
            "conversation_history": history,
            "should_continue": True,  
            "flow_state": {"active_flow": "景点替换", "on_progress": True}
        }
        
    except Exception as e:
        print(f"筛选推荐景点时出错: {e}")
        message = "❗️筛选推荐景点时出现问题。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "flow_state": {"active_flow": "景点替换", "on_progress": False}
        }

def update_preferences(state: TripState) -> TripState:
    """更新用户偏好"""
    user_input = state["user_input"]
    
    # 使用LLM提取用户的新偏好
    prompt = PromptTemplate.from_template("""
    # 背景信息
    用户输入: "{user_input}"
    
    # 任务
    请从用户输入中提取关于景点的新偏好，可能包括：景点类型、评分要求、距离要求等。
    
    # 输出格式
    请以JSON格式返回提取的偏好：
    {{
      "type": "用户提到的景点类型偏好，如博物馆、公园等",
      "rating_min": "最低评分要求",
      "distance_max": "最大距离要求(公里)",
      "other": "其他特殊要求"
    }}
    """)
    
    # 使用LLM提取偏好
    preference_text = app_context.llm.predict(
        prompt.format(user_input=user_input)
    ).strip()
    
    # 解析偏好
    try:
        # 提取JSON部分
        import re
        json_match = re.search(r'({.*})', preference_text, re.DOTALL)
        if json_match:
            new_preferences = json.loads(json_match.group(1))
        else:
            new_preferences = json.loads(preference_text)
    except:
        new_preferences = {}
    
    # 合并新旧偏好
    current_preferences = state.get("preferences", {})
    updated_preferences = {**current_preferences, **new_preferences}
    
    # 构建响应
    response_text = "已更新您的偏好："
    for key, value in new_preferences.items():
        if value:
            if key == "type":
                response_text += f" 景点类型：{value},"
            elif key == "rating_min":
                response_text += f" 最低评分：{value},"
            elif key == "distance_max":
                response_text += f" 最大距离：{value}公里,"
            elif key == "other" and value != "无":
                response_text += f" {value},"
    
    response_text = response_text.rstrip(",") + "。将为您重新推荐景点。"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_text})
    
    return {
        **state,
        "preferences": updated_preferences,
        "response": response_text,
        "conversation_history": history
    }

def handle_selection(state: TripState) -> TripState:
    """处理用户选择"""
    context_manager = app_context.context_manager
    user_input = state["user_input"]
    recommended_pois = context_manager.get_recommendations()
    
    # 使用LLM分析用户选择了哪个景点
    prompt = PromptTemplate.from_template("""
    # 背景信息
    用户输入: "{user_input}"
    
    # 推荐的景点列表
    {pois_list}
    
    # 任务
    请分析用户是否明确选择了推荐景点列表中的某个景点。
    
    # 输出格式
    如果用户明确选择了某个景点，请输出: "已选择: [景点编号]"
    如果用户没有明确选择，请输出: "未选择"
    仅输出以上两种格式之一，不要添加任何其他内容。
    """)
    
    # 构建POI列表文本
    pois_text = ""
    for i, poi in enumerate(recommended_pois, 1):
        pois_text += f"{i}. {poi['name']} - {poi.get('type', '无类型')}\n"
        pois_text += f"   推荐理由: {poi.get('recommendation_reason', '未知')}\n"
    
    # 使用LLM分析用户选择
    selection_result = app_context.llm.predict(
        prompt.format(user_input=user_input, pois_list=pois_text)
    ).strip()
    
    print("用户选择分析结果:", selection_result)
    
    # 处理选择结果
    selected_poi = None
    if selection_result.startswith("已选择:"):
        try:
            selected_index = int(selection_result.split("已选择:")[1].strip()) - 1
            if 0 <= selected_index < len(recommended_pois):
                selected_poi = recommended_pois[selected_index]
        except:
            pass
    
    if not selected_poi:
        response_text = "请明确选择一个推荐的景点，如我选择第2个景点。"
        history = state["conversation_history"]
        history.append({"role": "assistant", "content": response_text})
        return {
            **state,
            "response": response_text,
            "conversation_history": history,
            "flow_state": {**state["flow_state"], "on_progress": True}
        }
    
    # 获取替换POI信息
    replace_poi_info = state.get("replace_poi_info", {})
    target_poi = replace_poi_info.get("target_poi", {})
    
    # 构建替换后的POI信息
    replacement_poi = {
        "uid": selected_poi.get("poi_id", ""),
        "tel": selected_poi.get("tel", "无电话"),
        "opentime_week": selected_poi.get("opentime_week", "无营业时间"),
        "name": selected_poi.get("name", ""),
        "type": selected_poi.get("type", ""),
        "rating": selected_poi.get("rating", ""),
        "recommendation_reason": selected_poi.get("recommendation_reason", ""),
        "lat": selected_poi.get("coordinates", [0, 0])[0],
        "lng": selected_poi.get("coordinates", [0, 0])[1],
        "address": selected_poi.get("address", ""),
        "day": target_poi.get("day", 1),
        "order": target_poi.get("order", 1)
    }
    
    # 构建响应文本
    response_text = f"已将{target_poi.get('name', '')}替换为{selected_poi.get('name', '')}，已更新行程 ✅"
    
    # 构建结构化JSON数据
    json_data = {
        "data_type": "poi_replace",
        "replacement": replacement_poi
    }
    
    # 在响应中嵌入JSON数据
    response_with_json = response_text + f"\n<!--JSON_DATA:{json.dumps(json_data, ensure_ascii=False)}-->"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_with_json})
    
    return {
        **state,
        "response": response_with_json,
        "conversation_history": history,
        "flow_state": {"on_progress": False, "should_continue": False, "active_flow": None},
        "replacement_poi": replacement_poi
    }

def handle_invalid_input(state: TripState) -> TripState:
    flow_state = state.get("flow_state", {})
    # 检查是否是从退出推荐流程过来的
    if flow_state.get("on_progress") is False and "should_continue" in state and state["should_continue"] is False:
        active_flow = None
        msg = "已退出景点替换。您可以继续浏览行程或提出其他需求。"
    else:
        active_flow = flow_state.get("active_flow", None)
        msg = "⚠️ 我没理解您的意思。您可以选择一个推荐的景点，或告诉我更多偏好，比如我想要评分4.5以上的博物馆。"
    
    history = state.get("conversation_history", [])
    history.append({"role": "assistant", "content": msg})

    return {
        **state,
        "response": msg,
        "conversation_history": history,
        "flow_state": {
            "on_progress": False,
            "active_flow": active_flow  # 保留原有active_flow值
        }
    } 