# agent/agent_config.py

import os
from langchain.agents import initialize_agent, AgentType, Tool
from langchain_openai import ChatOpenAI
from langchain.memory import ConversationBufferWindowMemory
from langchain.prompts import MessagesPlaceholder
from langchain.schema import SystemMessage

from agent.context_manager import TripContextManager
from travel_tools.create_context_aware_tools import create_context_aware_tools

from travel_tools.view_itinerary import view_itinerary
from utils.file_utils import load_itinerary


def setup_agent(session_id: str):
    # 🔹 1. 加载行程数据 + 初始化上下文管理器
    file_path = 'data/sample_trip.json'
    itinerary_data = load_itinerary(file_path)
    context_manager = TripContextManager(itinerary_data)

    # 🔹 2. 构建支持上下文的工具
    tools = [

        Tool(
            name="查看行程",
            func=lambda *_args, **_kwargs: view_itinerary(context_manager),
            description="查看当前指定的行程单"
        )
        
    ]

    # 🔹 3. 初始化 LangChain Memory（只做消息历史）
    memory = ConversationBufferWindowMemory(
        k=5,
        return_messages=True,
        memory_key="chat_history"
    )

    # 🔹 4. 系统提示与 Agent 配置
    system_message = SystemMessage(
        content="你是一位旅行助手。请先设置行程ID，一旦设置成功，后续默认操作都针对该行程。"
    )
    agent_kwargs = {
        "extra_prompt_messages": [MessagesPlaceholder(variable_name="chat_history")],
        "system_message": system_message
    }

    # 🔹 5. 初始化 LLM Agent
    agent = initialize_agent(
        tools=tools,
        llm=ChatOpenAI(
            api_key="sk-",
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
            model="qwen2.5-14b-instruct-1m",
            temperature=0.3,
        ),
        agent=AgentType.ZERO_SHOT_REACT_DESCRIPTION,
        memory=memory,
        verbose=True,
        agent_kwargs=agent_kwargs
    )

    # ✅ 返回 Agent + 上下文管理器
    return agent, context_manager
