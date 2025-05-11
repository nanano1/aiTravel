from agent.nodes import decide_if_done, route_to_subgraph, view_itinerary_node

from langgraph.graph import StateGraph, END,START
from agent.state import TripState
from agent.subgraphs.attraction import create_attraction_graph
from agent.app_context import AppContext
from agent.subgraphs.restaurant import create_restaurant_graph
from agent.subgraphs.schedule_adjustment import create_schedule_adjustment_graph_test  # 导入整体调整子图的创建函数


def create_main_graph():
    """创建主图结构"""
    # 确保AppContext已初始化
    app_context = AppContext.get_instance()
    
    graph = StateGraph(TripState)

    graph.add_node("router", route_to_subgraph)
    graph.add_node("view_itinerary", view_itinerary_node)

    
    attraction_graph = create_attraction_graph()
    # 替换你原来的 graph.add_subgraph 这行
    graph.add_node("attraction_flow", attraction_graph)  # 使用 add_node 添加子图
    restaurant_graph = create_restaurant_graph()
    graph.add_node("restaurant_flow", restaurant_graph)  # 使用 add_node 添加子图

    # 添加整体调整子图
    schedule_adjustment_graph = create_schedule_adjustment_graph_test()  # 创建整体调整子图
    graph.add_node("schedule_adjustment_flow", schedule_adjustment_graph)  # 添加整体调整子图

    print("设置边：定义节点之间的连接关系...")
    graph.add_edge(START, "router")
    
    print("设置条件边：根据当前状态决定路由...")
    graph.add_conditional_edges(
        "router",
        routing_function
    )
    
    graph.add_edge("attraction_flow", END)
    graph.add_edge("schedule_adjustment_flow", END)  # 添加整体调整子图的结束边

    print("设置条件边：根据是否完成决定是结束还是继续...")
    graph.add_conditional_edges(
        "router",
        decide_if_done,
    )
    
    print("编译并返回图的最终结构...")
    return graph.compile()

def routing_function(state: TripState) -> str:
    print(f"当前状态: {state['flow_state']['active_flow']}")

    if state["flow_state"]["active_flow"] == "景点替换":
        print("路由到 attraction_flow")
        return "attraction_flow"
    elif state["flow_state"]["active_flow"] == "查看行程":
        print("路由到 view_itinerary")
        return "view_itinerary"
    elif state["flow_state"]["active_flow"] == "餐厅替换":
        print("路由到 restaurant_flow")
        return "restaurant_flow"
    elif state["flow_state"]["active_flow"] == "整体调整":
        print("路由到整体调整子图")
        return "schedule_adjustment_flow"
    else:
        print("结束")
        return END

