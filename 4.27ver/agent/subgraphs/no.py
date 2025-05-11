from langgraph.graph import StateGraph, END, START
from agent.state import TripState
from agent.app_context import AppContext
from langchain.prompts import PromptTemplate
import json
from typing import Dict, List, Any
import math
from langchain_core.prompts import PromptTemplate

app_context = AppContext.get_instance()


def create_schedule_adjustment_graph_test() -> StateGraph:
    """创建整体调整子图"""
    graph = StateGraph(TripState)

    # 添加意图识别节点
    graph.add_node("intent_recognition", intent_recognition_function)

    # 添加具体调整类型的节点
    graph.add_node("change_days_node", change_days_node)
    graph.add_node("adjust_pace_node", adjust_pace_node)
    graph.add_node("change_style_node", change_style_node)
    graph.add_node("optimize_route_node", optimize_route_node)
    graph.add_node("invalid_node", invalid_node)

    # 设置边
    graph.add_edge(START, "intent_recognition")

    # 添加条件边，根据 adjustment_type 路由到相应的节点
    graph.add_conditional_edges(
        "intent_recognition",
        lambda state: state["flow_state"]["adjustment_type"],
        path_map={
            "change_days": "change_days_node",  # 假设 compress_days 对应 change_days
            "adjust_pace": "adjust_pace_node",
            "change_style": "change_style_node",
            "optimize_route": "optimize_route_node",
            "invalid": "invalid_node"
        }
    )
    graph.add_edge("change_days_node", END)
    graph.add_edge("adjust_pace_node", END)
    graph.add_edge("change_style_node", END)
    graph.add_edge("optimize_route_node", END)
    graph.add_edge("invalid_node", END)
    return graph.compile()

def intent_recognition_function(state: TripState) -> TripState:
    """意图识别节点：提取用户的调整意图"""
    user_input = state["user_input"]
    
    # 使用 LLM 提取意图
    intent_prompt = PromptTemplate.from_template("""
    # 角色：智能行程调整解析专家
    ## 背景知识
    当前行程基础数据：
    - 原计划天数：{base_days} 天
    - 原每日景点数：{daily_pois} 个/天
    - 现有标签分布：{current_tags}

    ## 任务
    将用户自然语言请求转换为结构化调整指令，注意以下要点：
    1. 识别用户要的操作是change_days,adjust_pace,change_style,optimize_route中的哪一个
    2. 优先处理明确数字指令（如天数/数量）
    3. 风格替换需保持同类目转换（历史→文化，自然→户外）
    4. 强度描述需量化（"轻松"=减少30%景点）
    5. 如果用户没有提到每天景点的调整,就设置为原本的{daily_pois}.如果用户提到总天数调整,就设置为原本的{base_days}

    ## 输入示例
    用户输入 | 期望输出
    ---|---
    "我不喜欢历史景点" | {{"style_replace": {{"历史": ["文化", "艺术"]}} }}
    "每天别超过2个景点" | {{"daily_limit": 2}}
    "只玩三天" | {{"adjust_type": "compress_days", "target_days": 3}}
    "我想轻松点" | {{"daily_limit": 原来的景点数{daily_pois}-1, "prefer_tags": ["自然", "休闲"]}} 
    "我还想去798艺术区" | {{"add_pois": ["798艺术区"], "prefer_tags": ["艺术"]}}

    ## 当前请求处理
    用户输入: "{user_input}"

    请输出严格遵循此结构的 JSON（保留未提及字段为 null）：不要添加任何解释
    {{
    "adjust_type": "change_days/adjust_pace/change_style/optimize_route/invalid", 
    "target_days": 数值/null,
    "daily_limit": 数值,      
    "style_replace": {{"旧标签": ["新标签1", "新标签2"]}}/null,
    "add_pois": [POI名称]/null,
    "prefer_tags": [标签]/null
    }}
    """)

    # 调用 LLM 进行意图提取
    llm_response = app_context.llm.predict(intent_prompt.format(user_input=user_input,base_days=3,daily_pois=2,current_tags=["历史","文化"])).strip()
    
    # 解析 LLM 返回的 JSON
    try:
        extracted_info = json.loads(llm_response)
    except json.JSONDecodeError:
        print("解析 LLM 返回的 JSON 失败",llm_response)
        extracted_info = {
            "adjust_type": "invalid",
            "target_days": None,
            "daily_limit": None,
            "style_replace": None,
            "add_pois": None,
            "prefer_tags": None,
            "error": "解析失败"
        }

    # 打印提取的结果
    print("提取的意图:", extracted_info)

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
            "error": extracted_info.get("error")
        },
        "completed": True
    }

# 测试函数
def change_days_node(state: TripState) -> TripState:
    """调整天数节点：确认已进入该节点"""
    print("已成功进入调整天数节点")
    return {
        **state,
        "completed": True
    }

def adjust_pace_node(state: TripState) -> TripState:
    """调整节奏节点：确认已进入该节点"""
    print("已成功进入调整节奏节点")
    return {
        **state,
        "completed": True
    }

def change_style_node(state: TripState) -> TripState:
    """风格替换节点：确认已进入该节点"""
    print("已成功进入风格替换节点")
    return {
        **state,
        "completed": True
    }

def optimize_route_node(state: TripState) -> TripState:
    """优化路线节点：确认已进入该节点"""
    print("已成功进入优化路线节点")
    return {
        **state,
        "completed": True
    }

def invalid_node(state: TripState) -> TripState:
    """无效请求节点：确认已进入该节点"""
    print("已成功进入无效请求节点")
    return {
        **state,
        "completed": True
    }

def create_schedule_adjustment_graph():
    """创建行程调整子图"""
    graph = StateGraph(TripState)
    
    # 添加节点
    graph.add_node("process_input", process_user_input)
    graph.add_node("parse_intent", parse_adjustment_intent)
    graph.add_node("generate_recommendations", generate_poi_recommendations)
    graph.add_node("reorganize_schedule", reorganize_schedule)
    graph.add_node("optimize_route", optimize_route)
    graph.add_node("handle_invalid", handle_invalid_input)
    
    # 设置入口点
    graph.set_entry_point("process_input")
    
    # 添加条件边
    graph.add_conditional_edges(
        "process_input",
        lambda state: "parse_intent" if state["should_continue"] else "handle_invalid"
    )
    
    graph.add_conditional_edges(
        "parse_intent",
        lambda state: state["flow_state"]["adjustment_type"],
        path_map={
            "compress_days": "reorganize_schedule",
            "adjust_pace": "reorganize_schedule",
            "change_style": "generate_recommendations",
            "optimize_route": "optimize_route",
            "invalid": "handle_invalid"
        }
    )
    
    # 添加普通边
    graph.add_edge("generate_recommendations", "reorganize_schedule")
    graph.add_edge("reorganize_schedule", END)
    graph.add_edge("optimize_route", END)
    graph.add_edge("handle_invalid", END)
    
    return graph.compile()

def process_user_input(state: TripState) -> TripState:
    """处理用户输入"""
    user_input = state["user_input"]
    history = state["conversation_history"]
    
    if not user_input.strip():
        msg = "❗️请输入您想要如何调整行程，例如：把5天行程压缩到3天，或每天少去两个景点。"
        history.append({"role": "assistant", "content": msg})
        return {
            **state,
            "response": msg,
            "conversation_history": history,
            "should_continue": False
        }
    
    return {
        **state,
        "conversation_history": history,
        "should_continue": True
    }

def parse_adjustment_intent(state: TripState) -> TripState:
    """解析调整意图"""
    user_input = state["user_input"]
    
    # 简单的规则匹配
    if "压缩" in user_input and "天" in user_input:
        # 提取目标天数
        target_days = 3  # 默认值
        for word in user_input.split():
            if word.isdigit():
                target_days = int(word)
                break
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "adjustment_type": "compress_days",
                "target_days": target_days
            }
        }
    elif "少去" in user_input and "景点" in user_input:
        # 提取每日限制
        daily_limit = 2  # 默认值
        for word in user_input.split():
            if word.isdigit():
                daily_limit = int(word)
                break
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "adjustment_type": "adjust_pace",
                "daily_limit": daily_limit
            }
        }
    elif "换成" in user_input and "景点" in user_input:
        # 提取目标风格
        target_style = "自然风光" if "自然" in user_input else "历史" if "历史" in user_input else "文化"
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "adjustment_type": "change_style",
                "target_style": target_style
            }
        }
    elif "优化" in user_input and "路线" in user_input:
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "adjustment_type": "optimize_route"
            }
        }
    else:
        return {
            **state,
            "flow_state": {
                **state["flow_state"],
                "adjustment_type": "invalid"
            }
        }

def generate_poi_recommendations(state: TripState) -> TripState:
    """生成POI推荐"""
    context_manager = app_context.context_manager
    target_style = state["flow_state"].get("target_style")
    
    # 获取当前行程
    current_trip = context_manager.get_current_trip()
    if not current_trip:
        return {
            **state,
            "response": "❌ 未找到当前行程",
            "should_continue": False
        }
    
    # 根据目标风格生成推荐
    recommended_pois = []
    for day in current_trip["daily_itinerary"]:
        for poi in day["schedule"]:
            if target_style in poi.get("tags", []):
                recommended_pois.append(poi)
    
    # 如果没有找到匹配的POI，添加一些示例POI
    if not recommended_pois:
        recommended_pois = [
            {
                "name": "示例景点1",
                "type": "景点",
                "poi_type": "景点",
                "time": {"start": "09:00", "end": "11:00"},
                "duration_hours": 2,
                "notes": [],
                "price": 50,
                "coordinates": [39.9, 116.4],
                "address": "示例地址1",
                "tags": [target_style]
            },
            {
                "name": "示例景点2",
                "type": "景点",
                "poi_type": "景点",
                "time": {"start": "13:00", "end": "16:00"},
                "duration_hours": 3,
                "notes": [],
                "price": 80,
                "coordinates": [39.91, 116.41],
                "address": "示例地址2",
                "tags": [target_style]
            }
        ]
    
    # 更新状态
    return {
        **state,
        "recommended_pois": recommended_pois,
        "should_continue": True,
        "flow_state": {
            **state["flow_state"],
            "recommended_pois": recommended_pois
        }
    }

def reorganize_schedule(state: TripState) -> TripState:
    """重组行程"""
    context_manager = app_context.context_manager
    adjustment_type = state["flow_state"]["adjustment_type"]
    
    # 获取当前行程
    current_trip = context_manager.get_current_trip()
    if not current_trip:
        return {
            **state,
            "response": "❌ 未找到当前行程",
            "should_continue": False
        }
    
    # 根据调整类型重组行程
    if adjustment_type == "compress_days":
        target_days = state["flow_state"]["target_days"]
        reorganized = compress_schedule(current_trip, target_days)
    elif adjustment_type == "adjust_pace":
        daily_limit = state["flow_state"]["daily_limit"]
        reorganized = adjust_pace(current_trip, daily_limit)
    elif adjustment_type == "change_style":
        # 使用推荐的POI重组行程
        recommended_pois = state["flow_state"].get("recommended_pois", [])
        reorganized = reorganize_with_pois(current_trip, recommended_pois)
    else:
        reorganized = current_trip["daily_itinerary"]
    
    # 更新状态
    return {
        **state,
        "reorganized_schedule": reorganized,
        "should_continue": True,
        "flow_state": {
            **state["flow_state"],
            "reorganized_schedule": reorganized
        }
    }

def optimize_route(state: TripState) -> TripState:
    """优化路线"""
    context_manager = app_context.context_manager
    
    # 获取当前行程
    current_trip = context_manager.get_current_trip()
    if not current_trip:
        return {
            **state,
            "response": "❌ 未找到当前行程",
            "should_continue": False
        }
    
    # 按地理位置聚类
    clustered = cluster_by_location(current_trip["daily_itinerary"])
    
    # 更新状态
    return {
        **state,
        "clustered_schedule": clustered,
        "should_continue": True,
        "flow_state": {
            **state["flow_state"],
            "clustered_schedule": clustered
        }
    }

def handle_invalid_input(state: TripState) -> TripState:
    """处理无效输入"""
    msg = "⚠️ 请明确说明您想要如何调整行程，例如：\n" + \
          "- 把5天行程压缩到3天\n" + \
          "- 每天少去两个景点\n" + \
          "- 把历史景点换成自然风光"
    
    history = state.get("conversation_history", [])
    history.append({"role": "assistant", "content": msg})
    
    return {
        **state,
        "response": msg,
        "conversation_history": history,
        "should_continue": False
    }

# 辅助函数
def compress_schedule(trip: Dict, target_days: int) -> List[Dict]:
    """压缩行程天数"""
    # 实现压缩逻辑
    daily_itinerary = trip["daily_itinerary"]
    if len(daily_itinerary) <= target_days:
        return daily_itinerary
    
    # 简单的压缩策略：保留前target_days天
    compressed = daily_itinerary[:target_days]
    return compressed

def adjust_pace(trip: Dict, daily_limit: int) -> List[Dict]:
    """调整每日景点数量"""
    # 实现调整节奏逻辑
    adjusted = []
    for day in trip["daily_itinerary"]:
        adjusted_day = {
            **day,
            "schedule": day["schedule"][:daily_limit]
        }
        adjusted.append(adjusted_day)
    return adjusted

def reorganize_with_pois(trip: Dict, recommended_pois: List[Dict]) -> List[Dict]:
    """使用推荐的POI重组行程"""
    # 获取原始行程的天数
    daily_itinerary = trip["daily_itinerary"]
    num_days = len(daily_itinerary)
    
    # 将推荐的POI平均分配到各天
    pois_per_day = math.ceil(len(recommended_pois) / num_days)
    reorganized = []
    
    for i in range(num_days):
        start_idx = i * pois_per_day
        end_idx = min((i + 1) * pois_per_day, len(recommended_pois))
        day_pois = recommended_pois[start_idx:end_idx]
        
        # 创建新的一天
        new_day = {
            **daily_itinerary[i],
            "schedule": day_pois
        }
        reorganized.append(new_day)
    
    return reorganized

def cluster_by_location(daily_itinerary: List[Dict]) -> List[List[Dict]]:
    """按地理位置聚类"""
    # 实现地理聚类逻辑
    clusters = []
    for day in daily_itinerary:
        # 按照经纬度排序
        pois = sorted(day["schedule"], 
                     key=lambda x: (x["coordinates"][0], x["coordinates"][1]))
        clusters.append(pois)
    return clusters 