# travel_tools/set_trip_id_tool.py

from agent.context_manager import TripContextManager

def set_trip_id(trip_id: str, context_manager: TripContextManager) -> str:
    """
    设置当前激活的行程单 ID
    :param trip_id: 用户输入的行程单 ID（如 SH-URBAN-003）
    :param context_manager: TripContextManager 实例
    """
    result = context_manager.set_current_trip(trip_id)
    return result
