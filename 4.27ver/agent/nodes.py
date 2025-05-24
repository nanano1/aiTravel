from langchain_core.prompts import PromptTemplate
from agent.state import TripState
from agent.app_context import AppContext

# 获取AppContext实例
app_context = AppContext.get_instance()
# 使用AppContext中的LLM
llm = app_context.llm



def route_to_subgraph(state: TripState) -> TripState:
    """路由节点：根据活动流程路由到相应的子图"""
    print(state)

    user_input = state["user_input"]
    flow_state = state["flow_state"]
    if flow_state["on_progress"]:
        print(f"当前活动流程: {flow_state['active_flow']}，继续处理")
        return {
            **state,
            "active_flow": flow_state["active_flow"]  # 保持当前活动流程
        }
    
    # 使用LLM提取用户意图
    intent_prompt = PromptTemplate.from_template("""
    分析用户输入，确定用户想要执行的操作：
    
    用户输入: "{user_input}"
    
    可能的操作有：
    1. 查看行程
    2. 替换景点
    3. 替换餐厅
    4. 推荐餐厅
    5. 结束对话
    6. 调整行程天数（延长/缩短）:比如说新增一天行程，安排多一天行程，我有多一天时间等
    7. 调整每日节奏（增减景点密度）
    8. 整体风格替换（如历史→自然）
    9. 路线优化（不增减只重排）
    
    请直接输出最匹配的操作名称（不要解释）：
    """)
    
    intent_response = llm.predict(intent_prompt.format(user_input=user_input)).strip()
    
    # 映射意图到流程
    intent_mapping = {
        "查看行程": "查看行程",
        "替换景点": "POI替换",
        "替换餐厅": "餐厅替换", 
        "推荐餐厅": "餐厅替换",
        "结束对话": None,
        "调整行程天数": "整体调整",
        "调整每日节奏": "整体调整",
        "整体风格替换": "整体调整",
        "路线优化": "整体调整"
    }
    
    # 确定活动流程
    active_flow = None
    for key, value in intent_mapping.items():
        if key in intent_response:
            active_flow = value
            print(f"识别到的活动流程: {active_flow}")
            break
    
    # 当没有识别到有效的活动流程时，路由到通用问答节点
    if not active_flow:
        print("未识别到具体意图，路由到通用问答节点")
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "active_flow": "general_qa"
            }
        }
    
    return {
        **state,
        "flow_state": {
            **state["flow_state"],
            "active_flow": active_flow
        }
    }
    


def view_itinerary_node(state: TripState) -> TripState:
    """查看行程节点：返回当前行程信息"""
    from travel_tools.view_itinerary import view_itinerary
    
    # 获取最新的AppContext实例以确保使用最新的context_manager
    current_app_context = AppContext.get_instance()
    
    # 使用AppContext中最新的context_manager
    itinerary_info = view_itinerary(current_app_context.context_manager)
    
    # 调试信息
    print(f"当前状态: 查看行程")
    print(f"路由到 view_itinerary")
    print(f"使用的行程ID: {current_app_context.context_manager.get_current_trip_id()}")
    
    # 更新历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": itinerary_info})
    
    return {
        **state,
        "response": itinerary_info,
        "conversation_history": history,
        "completed": True
    }

def decide_if_done(state: TripState) -> str:
    """决定是否结束会话"""
    if "再见" in state["user_input"].lower() or "结束" in state["user_input"].lower():
        return "end"
    return "continue"

def general_qa_node(state: TripState) -> TripState:
    """通用问答节点：使用LLM直接回答用户问题"""
    # 获取最新的AppContext实例
    current_app_context = AppContext.get_instance()
    llm = current_app_context.llm
    
    # 构建提示模板
    qa_prompt = PromptTemplate.from_template(
        """
        你是一个专业的旅行规划助手，请根据用户的问题提供专业、友好的回答。
        如果问题涉及具体的旅行建议，请尽可能给出详细和实用的建议。
        如果问题不清楚，可以礼貌地请求用户提供更多信息。

        用户问题: {user_input}

        请提供你的回答：
        """
    )
    
    # 生成回答
    response = llm.predict(qa_prompt.format(user_input=state["user_input"])).strip()
    
    # 更新对话历史
    history = state["conversation_history"]
    history.append({"role": "assistant", "content": response})
    
    return {
        **state,
        "response": response,
        "conversation_history": history,
        "completed": True
    }
