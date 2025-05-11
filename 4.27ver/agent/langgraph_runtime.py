from typing import Dict, Any
from agent.main_graph import create_main_graph
from agent.state import TripState
from agent.context_manager import TripContextManager

class LangGraphApp:
    """LangGraph应用程序运行时"""
    
    def __init__(self, session_id: str, context_manager: TripContextManager):
        self.session_id = session_id
        self.graph = create_main_graph()
        self.state = self._initialize_state()
        self.context_manager = context_manager
        self.initialized = False  # 添加初始化标志
    
    def _initialize_state(self) -> TripState:
        """初始化状态"""
        return {
            "user_input": "",
            "active_flow": None,
            "current_trip_id": None,
            "target_poi": None,
            "candidates": [],
            "user_preferences": {
                "budget": None,
                "poi_type": None,
                "rating": None,
                "tags": []
            },
            "conversation_history": [],
            "response": "",
            "error": None,
            "should_continue": True,
            "completed": False,
            "awaiting_selection": False,
            "flow_state": {  
                "active_flow": None,
                "on_progress": False,
                "followup_action": None,
            }
        }
    
    def set_trip_id(self, trip_id: str) -> str:
        """设置当前行程ID"""
        result = self.context_manager.set_current_trip(trip_id)
        
        if result.startswith("✅"):
            # 更新状态中的行程ID
            self.state = {
                **self.state,
                "current_trip_id": trip_id
            }
            self.initialized = True
            
        return result
    
    def process_input(self, user_input: str) -> str:
        """处理用户输入并返回响应"""
        # 检查是否已初始化行程
        if not self.initialized:
            return "请先设置行程ID，再进行其他操作。"
        
        # 更新状态
        self.state = {
            **self.state,
            "user_input": user_input,
            "completed": False
        }
        
        # 添加到上下文管理器
        self.context_manager.add_message("user", user_input)
        # 调用图
        result = self.graph.invoke(self.state)
        
        # 更新状态
        self.state = result
        
        # 返回响应
        return result["response"]
