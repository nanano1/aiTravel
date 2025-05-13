import json
import os
from langgraph.graph import END,StateGraph
import pandas as pd
from agent.state import TripState
from travel_tools.recommendAttraction import filter_poi_candidates,extract_modification_intent
from agent.app_context import AppContext
from langchain.prompts import PromptTemplate

app_context=AppContext.get_instance()
from langgraph.graph import StateGraph, END
from agent.state import TripState
from agent.app_context import AppContext

app_context = AppContext.get_instance()

def create_attraction_graph():
    graph = StateGraph(TripState)

    # 添加节点
    graph.add_node("process_input", process_user_input)  # A
    graph.add_node("init_recommend", initial_user_input)  # C
    graph.add_node("classify_input", classify_followup_input)  # D
    graph.add_node("generate_recommendations", generate_recommendations)  # E
    graph.add_node("handle_selection", handle_selection)  # G
    graph.add_node("update_preferences", update_preferences)  # I
    graph.add_node("handle_invalid", handle_invalid_input)  # J

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
        msg = "❗️请输入您想替换的景点，如“第三天的故宫”。"
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
    你是一个旅行助手，刚刚给用户推荐了几个景点,你需要解析下一步用户需要干嘛
                                          
    用户输入："{user_input}"
    上次给他推荐的三个poi:
    {pois}

    请根据输入内容分类：
    - 如果用户表示想要更换为{pois}里的其中一个景点,请回复 "选择"
    - 如果用户提供了新的偏好，请回复 "偏好"
    - 如果输入无效或模糊，请回复 "无效"

    # 输出格式
    请严格按"无效","选择","偏好"中的一种回答，不要添加任何其他内容,也不要解释和注释：
    """)

    classification = llm.predict(prompt.format(user_input=user_input, pois=pois_str)).strip()
    print("classification",classification)
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "followup_action": classification}
    }

def initial_user_input(state: TripState) -> TripState:
    context_manager = app_context.context_manager
    print("处理用户输入...")
    if not state.get("user_input", "").strip():
        message = "❗️请输入您想替换的景点，例如“第二天的故宫”。"
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False,
            "flow_state": {**state["flow_state"], "active_flow": "景点替换"}
        }

    # 直接使用context_manager
    print("提取意图...")
    intent = extract_modification_intent(state["user_input"], context_manager.get_current_trip())

    missing_info = intent.get("missing_info", False)
    city = intent.get("city", None)
    print("missing_info",missing_info)
    print("state.get('should_continue')",state.get("should_shoucontinue"))
    if missing_info:
        message = "❗️未能识别要替换的景点，请说明第几天和景点名称，例如“第二天的故宫”。"
        history = state.get("conversation_history", [])
        history.append({"role": "assistant", "content": message})
        return {
            **state,
            "response": message,
            "conversation_history": history,
            "should_continue": False,
            "user_selected": False
        }
    target_poi = intent.get("target_poi", {})
    preferences = intent.get("preferences", {})

    context_manager.update_poi(target_poi)
    context_manager.update_preferences(preferences)
    context_manager.update_city(city)
    history = state.get("conversation_history", [])
    
    candidates = context_manager.get_recommendations()
    print("candidates",candidates[:3])

    return {
        **state,
        "target_poi": target_poi,
        "preferences": preferences,
        "city": city,
        "candidates": candidates,
        "user_selected": False,
        "should_continue": True,
        "conversation_history": history,
        "flow_state": {"active_flow": "景点替换", "on_progress": True}
    }

def generate_recommendations(state: TripState) -> TripState:
    """生成景点推荐"""
    # 使用AppContext中的context_manager
    context_manager = app_context.context_manager
    
    # 获取当前的目标POI和用户偏好
    target_poi = context_manager.get_poi()
    preferences = context_manager.get_preferences()
    #candidates = context_manager.get_recommendations()
    #print("存在context_manager的candidates",candidates[:3])
    #if not candidates or len(candidates) == 0:
    #print("仍未初始化列表")
    city = context_manager.get_city()
    print("仍未初始化列表时获得的city",city)
    file_path = os.path.join('data', 'database', 'attractions', city, 'attractions.csv')
    poi_data = pd.read_csv(file_path)
    candidates = list(poi_data.itertuples(index=False))
    print("读入的poidata",candidates[:3])
        
    # 调用 filter_poi_candidates 函数
    recommended_pois = filter_poi_candidates(target_poi, candidates, preferences)
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
            "flow_state": {"active_flow": "景点替换","on_progress": False}
        }

        
    # 构造推荐响应文本
    response_text = "根据您的偏好，以下是推荐的景点：\n"
    for i, poi in enumerate(recommended_pois[:3], 1):  # 仅显示前3个
        response_text += f"{i}. {poi['name']} - {poi.get('poi_type', '未知类型')}\n"
    response_text += "\n请选择一个景点或提供更多偏好。"
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response_text})
    print("response_text",response_text)
    
    # 返回推荐结果
    return {
        **state,
        "recommended_pois": recommended_pois,
        "response": response_text,
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

    prompt = PromptTemplate.from_template("""
    # Role: 智能旅行助手信息提取专家

    # 背景信息:这里本来有一些偏好,但是用户进行了更新,你也要更新偏好

    # 任务
    请从用户输入中提取用户的新偏好要求。
    可用POI类型列表：
    [
            "游乐园/体育娱乐", "人文景观", "文化旅游区", "博物馆/纪念馆",
            "商业街区", "其它", "历史古迹", "公园",
            "美术馆/艺术馆", "红色景点", "自然风光", "大学校园"
    ]

    # 用户原始输入
    "{user_input}"
    # 用户原始偏好
    "{preferences}"

    # 提取要求
    ## 新偏好要求
    - 预算（budget）：提取明确的数字金额，没有新增的信息的话还是保持不变
    - 类型偏好（type）：必须匹配可用类型列表中的选项,比如用户说适合小孩玩的,你可以把偏好设置为"游乐园/体育娱乐,没有新增的信息的话还是保持不变"

    # 输出格式
    请严格按以下JSON格式回答，不要添加任何其他内容,也不要解释和注释：
    "preferences": {{
        "budget": 500,       # 提取的预算
        "type": "博物馆",    # 提取的类型偏好
    }}
""")
    context_manager = app_context.context_manager
    
    # 使用LLM分析用户输入

    try:
        response = app_context.llm.predict(prompt.format(user_input=user_input, preferences=preferences)).strip()
        # 尝试从响应中提取 JSON 部分
        json_start = response.find('{')
        json_end = response.rfind('}') + 1
        if json_start != -1 and json_end != -1:
            json_str = response[json_start:json_end]
            parsed = json.loads(json_str)
            
    except Exception as e:
        print("更新偏好失败",e)
        

    print("updated_preferences",parsed)
    # 更新偏好
    context_manager.update_preferences(parsed) 
    
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
    分析用户输入，确定用户是否选择了推荐的景点：
    
    用户输入: "{user_input}"
    
    推荐景点列表:
    {pois}
    
    如果用户明确选择了某个景点，请输出 "已选择: [景点名称]"
    如果用户提供了新的偏好但没有选择，请输出 "未选择: 新偏好"
    如果用户拒绝了所有推荐，请输出 "拒绝推荐"
    
    请直接输出结果（不要解释）：
    """)
    
    # 构造景点列表文本
    pois_text = ""
    for i, poi in enumerate(recommended_pois[:3], 1):
        pois_text += f"{i}. {poi['name']} - {poi.get('poi_type', '未知类型')}\n"
    
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
            confirmation_text = "请选择一个推荐的景点或提供更多偏好。"
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
    msg = f"已将原景点替换为：，已更新行程 ✅"
    app_context.context_manager.reset_attraction_flow()
    return {
        **state,
        
        "response": msg,
        "flow_state": {"on_progress": False,"should_continue": False}
    }


def handle_invalid_input(state: TripState) -> TripState:
    msg = "⚠️ 我没理解您的意思。您可以选择一个推荐的景点，或告诉我更多偏好，比如“预算200以内”、“评分4.5以上”。"
    history = state.get("conversation_history", [])
    history.append({"role": "assistant", "content": msg})

    return {
        **state,
        "response": msg,
        "conversation_history": history
    }
