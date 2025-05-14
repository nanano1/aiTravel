import json
from typing import List
from langchain_core.prompts import ChatPromptTemplate
from langchain_openai import ChatOpenAI
from langchain_core.output_parsers import JsonOutputParser

# 定义数据结构模型（Pydantic模型更规范，这里先用简单dict示例）
parser = JsonOutputParser(pydantic_object=None)  # 也可以自定义pydantic模型

# 初始化LLM
llm = ChatOpenAI(
        api_key="sk-",
        base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
        model="qwen-turbo",
        temperature=0.7
    )
# 构建提示模板
template = """
你是一个专业的旅行数据生成器。请为{city}生成{num_poi}个真实的,受欢迎的旅游景点数据，
严格遵循以下JSON格式要求：

要求：
1. 包含中文名称(name)、类型(type)、坐标(coordinates)、推荐时长(duration)
2. 类型必须从列表中选择：["历史古迹", "自然景观", "主题公园", "博物馆", "购物中心", "特色街区"]
3. 坐标使用合理的地理位置（参考真实城市坐标）
4. 推荐时长需符合景点类型特征（单位：分钟）
5. 用中文生成描述(description)，包含2-3个特色点

示例格式：
{{
  "poi_list": [
    {{
      "name": "上海未来科技馆",
      "type": "博物馆",
      "coordinates": [121.5012, 31.2245],
      "duration": 120,
      "description": "展示前沿科技应用的沉浸式体验馆，特色：1. 人工智能互动展区 2. 虚拟现实太空探索 3. 机器人咖啡厅"
    }}
  ]
}}

请为{city}生成数据：
"""

prompt = ChatPromptTemplate.from_template(template)

# 构建完整流程链
chain = prompt | llm | parser


def generate_poi_data(city: str, num_poi: int = 5) -> List[dict]:
    """生成指定城市的POI数据"""
    # 获取格式指令（自动生成JSON结构描述）
    format_instructions = parser.get_format_instructions()

    try:
        response = chain.invoke({
            "city": city,
            "num_poi": num_poi,
            "format_instructions": format_instructions
        })

        # 确保返回的是列表结构
        if isinstance(response, dict) and "poi_list" in response:
            return response["poi_list"]
        return response

    except Exception as e:
        print(f"生成失败: {str(e)}")
        return []


# 示例使用
if __name__ == "__main__":
    cities = ["上海", "成都", "西安"]

    all_data = {}
    for city in cities:
        print(f"正在生成{city}数据...")
        poi_list = generate_poi_data(city, 10)  # 每个城市生成3个

        if poi_list:
            all_data[city] = poi_list
            print(f"成功生成{len(poi_list)}个{city}景点")
            print(json.dumps(poi_list, ensure_ascii=False, indent=2))
        else:
            print(f"{city}数据生成失败")

    # 保存到文件
    with open("poi_data.json", "w", encoding="utf-8") as f:
        json.dump(all_data, f, ensure_ascii=False, indent=2)