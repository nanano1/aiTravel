import json
import os
from langgraph.graph import END,StateGraph
import pandas as pd
from agent.state import TripState
from agent.app_context import AppContext
from langchain.prompts import PromptTemplate
from travel_tools.recommendRestaurant import extract_res_intent,generate_res_recommendation,get_res_preference
from langgraph.graph import StateGraph, END
from agent.state import TripState
from agent.app_context import AppContext

app_context = AppContext.get_instance()

def create_restaurant_graph():
    graph = StateGraph(TripState)

    # 添加节点
    graph.add_node("process_input", process_user_input)  # A
    graph.add_node("init_recommend", initial_user_input)  # B
    graph.add_node("classify_input", classify_followup_input)  # C
    graph.add_node("generate_recommendations", generate_recommendations)  # D
    graph.add_node("handle_selection", handle_selection)  # E
    graph.add_node("update_preferences", update_preferences)  # F
    graph.add_node("handle_invalid", handle_invalid_input)  # G

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
    print(f"当前状态: {state["flow_state"]["on_progress"]}")
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
        msg = "❗️请输入您想替换的餐厅，如“帮我换掉第一天的午餐餐厅”。"
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
    all_fitPOi = app_context.context_manager.get_recommendations()
    last_recommendations = all_fitPOi[:3]
    print("last_recommendations",last_recommendations)
    pois_str = "\n".join([f"{i+1}. {p['name']}" for i, p in enumerate(last_recommendations)])

    prompt = PromptTemplate.from_template("""
    # 背景信息
    你是一个旅行助手，刚刚给用户推荐了几个餐厅，你需要解析下一步用户要做什么。
                                          
    用户输入："{user_input}"
    上次给他推荐的餐厅:
    {pois}

    请根据输入内容分类：
    - 如果用户表示想要更换为{pois}里的其中一个餐厅，请回复 "选择"
    - 如果用户提供了新的要求偏好，比如预算,餐厅类型,评分维度等新的要求,请回复 "偏好"
    - 如果用户表示不想继续这个推荐过程，或者想做其他事情，请回复 "退出"
    - 如果输入无效或模糊，但似乎还与餐厅选择有关，请回复 "无效"

    # 输出格式
    请严格按"无效"、"选择"、"偏好"、"退出"中的一种回答，不要添加任何其他内容，也不要解释和注释：
    """)

    classification = llm.predict(prompt.format(user_input=user_input, pois=pois_str)).strip()
    print("classification",classification)
    
    # 如果用户想退出推荐流程
    if classification == "退出":
        return {
            **state,
            "flow_state": {
                "on_progress": False,  # 将on_progress设置为False，退出当前推荐流程
                "followup_action": "退出"  # 依然使用"无效"作为路由的值，但会在handle_invalid_input处理
            },
            "response": "好的，我们可以稍后再继续餐厅推荐。您有什么其他需要帮助的吗？",
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
    print("处理用户输入...")
    if not state.get("user_input", "").strip():
        message = "❗️请输入您想替换的餐厅，例如“帮我换掉第一天的午餐餐厅”。"
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False,
            "flow_state": {**state["flow_state"], "active_flow": "餐厅替换"}
        }

    print("进入提取意图节点...")
    intent = extract_res_intent(state["user_input"], context_manager.get_current_trip())

    missing_info = intent.get("missing_info", False)
    city = intent.get("city", None)
    print("missing_info",missing_info)
    print("state.get('should_continue')",state.get("should_shoucontinue"))
    if missing_info:
        message = "❗️未能识别要替换的餐厅，请说明，例如“第二天的故宫”。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False
        }
    target_restaurant = intent.get("target_restaurant", {})
    preference_analysis = intent.get("preference_analysis", {})
    print("进入initial_user_input的target_restaurant",target_restaurant)
    print("进入initial_user_input的preference_analysis",preference_analysis)


    context_manager.update_poi(target_restaurant)
    context_manager.update_preferences(preference_analysis)
    context_manager.update_city(city)
    history = state.get("conversation_history", [])
    
    candidates = context_manager.get_recommendations()
    print("candidates",candidates)

    return {
        **state,
        "target_poi": target_restaurant,
        "preferences": preference_analysis,
        "city": city,
        "candidates": candidates,
        "user_selected": False,
        "should_continue": True,
        "conversation_history": history,
        "flow_state": {"active_flow": "餐厅替换", "on_progress": True}
    }

def generate_recommendations(state: TripState) -> TripState:
    """生成餐厅推荐"""
    # 使用AppContext中的context_manager
    context_manager = app_context.context_manager
    print("进入generate_recommendations节点")
    # 获取当前的目标POI和用户偏好
    target_poi = context_manager.get_poi()
    preferences = context_manager.get_preferences()
    print("进入generate_recommendations的preferences",preferences)
    print("进入generate_recommendations的target_poi",target_poi)
    # 调用 filter_poi_candidates 函数
    recommended_pois = generate_res_recommendation(target_poi, preferences)
    # 更新上下文管理器中的推荐列表
    context_manager.update_recommendations(recommended_pois)

    if not recommended_pois or len(recommended_pois) == 0:
        message = "共找到0个符合条件的POI"
        print("message",message)
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False,
            "flow_state": {"active_flow": "餐厅替换","on_progress": False}
        }

    # 构建响应文本
    response_text = "我为您找到了以下几家餐厅作为替代选择：\n\n"
    
#    for i, rec in enumerate(recommended_pois[:5], 1):  # 限制最多5个推荐
#       response_text += f"{i}. {rec['name']} - {rec['label']}\n"
#       response_text += f"   价格：{rec['price']}元/人左右\n"
#       response_text += f"   推荐理由：{rec['reason']}\n\n"
    
    response_text += "您可以选择其中一家作为替代，或者告诉我您的其他偏好，我可以重新推荐。"
    print(response_text)
    # 构建结构化JSON数据
    json_data = {
        "data_type": "restaurant_recommendations",
        "res_info":target_poi.get("res_info",None),
        "day_info":target_poi.get("day_info",None),
        "recommendations": recommended_pois
    }
    
    # 在响应中嵌入JSON数据
    response_with_json = response_text + f"\n<!--JSON_DATA:{json.dumps(json_data, ensure_ascii=False)}-->"

    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_with_json})
    print("response_text",response_with_json)
    
    # 返回推荐结果
    return {
        **state,
        "recommended_pois": recommended_pois,
        "response": response_with_json,
        "conversation_history": history,
        "should_continue": False,  # 返回主图
        "awaiting_selection": True,
        "flow_state": {
            **state["flow_state"],
            "on_progress": True
        }
    }

def update_preferences(state: TripState) -> TripState:

    """更新用户偏好"""
    user_input = state["user_input"]
    preferences = app_context.context_manager.get_preferences()
    print("original preferences",preferences)
    update_preferences = get_res_preference(user_input,preferences).get("preference_analysis",{})

    print("updated_preferences",update_preferences)
    # 更新偏好
    app_context.context_manager.update_preferences(update_preferences) 
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": "偏好已更新"})
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"]
        },
        "response": "偏好已更新",
        "conversation_history": history,
        
    }
            

def check_user_selection(state: TripState) -> TripState:
    """检查用户是否选择了推荐的POI"""
    user_input = state["user_input"].lower()
    recommended_pois = app_context.context_manager.get_recommendations()
    
    # 使用AppContext中的llm来分析用户选择
    prompt = PromptTemplate.from_template("""
    分析用户输入，确定用户是否选择了推荐的餐厅：    
    
    用户输入: "{user_input}"
    
    推荐餐厅列表:
    {pois}
    
    如果用户明确选择了某个餐厅，请输出 "已选择: [餐厅名称]"
    如果用户提供了新的偏好但没有选择，请输出 "未选择: 新偏好"
    如果用户拒绝了所有推荐，请输出 "拒绝推荐"
    
    请直接输出结果（不要解释）：
    """)
    
    # 构造餐厅列表文本      
    pois_text = ""
    for i, poi in enumerate(recommended_pois, 1):
        pois_text += f"{i}. {poi['name']} - {poi.get('reason', '未知')}\n"
    
    # 使用LLM分析用户选择
    selection_response = app_context.llm.predict(
        prompt.format(user_input=user_input, pois=pois_text)
    ).strip()
    
    # 解析LLM响应
    if "已选择" in selection_response:
        # 用户选择了推荐的POI
        confirmation_text = f"您已选择: {selection_response.split('已选择:')[1].strip()}"
        state["flow_state"]["on_progress"] = False
    else:
        # 用户未选择，继续收集偏好
        if "拒绝推荐" in selection_response:
            confirmation_text = "您拒绝了所有推荐，请提供更多偏好。"
            state["flow_state"]["on_progress"] = True
        else:
            confirmation_text = "请选择一个推荐的餐厅或提供更多偏好。"
            state["flow_state"]["on_progress"] = True

    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": confirmation_text})
    
    return {
        **state,
        "response": confirmation_text,
        "conversation_history": history,
        "flow_state": state["flow_state"]
    }

def handle_selection(state: TripState) -> TripState:
    msg = f"已将原餐厅替换，已更新行程 ✅"
    app_context.context_manager.reset_attraction_flow()
    return {
        **state,
        
        "response": msg,
        "flow_state": {"on_progress": False,"should_continue": False}
    }


def handle_invalid_input(state: TripState) -> TripState:
    flow_state = state.get("flow_state", {})
    # 检查是否是从退出推荐流程过来的
    if flow_state.get("on_progress") is False and "should_continue" in state and state["should_continue"] is False:
        active_flow = None
        msg = "已退出餐厅推荐。您可以继续浏览行程或提出其他需求。"
    else:
        active_flow = flow_state.get("active_flow", None)
        msg = "⚠️ 我没理解您的意思。您可以选择一个推荐的餐厅，或告诉我更多偏好，比如「预算200以内」、「评分4.5以上」。"
    
    history = state.get("conversation_history", [])
    history.append({"role": "assistant", "content": msg})
    # 确保保留原有active_flow值，或者设置为None


    return {
        **state,
        "response": msg,
        "conversation_history": history,
        "flow_state": {
            "on_progress": True,
            "active_flow": active_flow  # 保留原有active_flow值
        }
    }
