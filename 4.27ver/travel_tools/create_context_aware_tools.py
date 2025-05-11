# travel_tools/create_context_aware_tools.py

from langchain.tools import Tool
from travel_tools.set_trip_id_tool import set_trip_id

def create_context_aware_tools(context_manager):
    return [
        Tool(
            name="设置当前行程",
            func=lambda trip_id: set_trip_id(trip_id, context_manager),
            description="根据 ID 设置当前正在操作的行程"
        )
    ]
