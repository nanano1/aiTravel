from typing import Dict, List, Any, Optional, TypedDict, Literal
from pydantic import BaseModel, Field

class UserPreference(BaseModel):
    """用户偏好数据模型"""
    budget: Optional[float] = None
    poi_type: Optional[str] = None
    rating: Optional[float] = None
    tags: List[str] = Field(default_factory=list)

class TripState(TypedDict):
    """旅行系统的全局状态"""
    # 基本信息
    user_input: str  # 用户当前输入
    
    # 上下文和历史
    conversation_history: List[Dict]  # 对话历史
    response: str  # 当前响应
    error: Optional[str]  # 错误信息
    
    # 控制流
    should_continue: bool  # 是否继续子流程
    completed: bool  # 当前流程是否完成
    awaiting_selection: bool  # 是否等待用户选择

    flow_state: Dict[str, Any] = {
        "active_flow": None,  # 当前激活的子流程
        "on_progress": False,   # 是否在该流程中流转
        "followup_action": None,  # 后续动作
    }