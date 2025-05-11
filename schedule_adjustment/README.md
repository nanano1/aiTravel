# 行程调整子图

这个项目实现了一个行程调整子图（Schedule Adjustment Flow），用于处理用户对行程单的整体结构调整请求。

## 功能特点

- 意图识别：能够识别多种调整意图
  - 压缩天数
  - 调整行程节奏
  - 改变景点风格
  - 优化访问路线

- POI推荐：根据用户偏好推荐新的景点
- 行程重组：根据不同调整类型重新安排行程
- 地理聚类：使用DBSCAN算法优化景点访问顺序

## 安装依赖

```bash
pip install -r requirements.txt
```

## 使用示例

```python
from agent.subgraphs.schedule_adjustment import ScheduleAdjustmentFlow

# 创建行程调整流程实例
flow = ScheduleAdjustmentFlow()

# 示例行程数据
schedule = {
    "days": [
        {
            "day": 1,
            "spots": [
                {"name": "王府井", "type": "购物", "duration": 120},
                {"name": "故宫", "type": "文化", "duration": 180}
            ]
        },
        {
            "day": 2,
            "spots": [
                {"name": "天安门", "type": "文化", "duration": 60},
                {"name": "颐和园", "type": "自然", "duration": 240}
            ]
        }
    ]
}

# 解析用户意图
user_input = "能否把行程压缩到一天完成？"
intent = flow.parse_intent(user_input)

# 调整行程
adjusted_schedule = flow.adjust_schedule(schedule, intent)
```

## 运行测试

```bash
python -m unittest tests/test_schedule_adjustment_graph.py
```

## 项目结构

```
schedule_adjustment/
├── README.md
├── requirements.txt
├── agent/
│   └── subgraphs/
│       └── schedule_adjustment.py
└── tests/
    └── test_schedule_adjustment_graph.py
```

## 主要类和方法

### ScheduleAdjustmentFlow

- `parse_intent(user_input: str) -> Dict[str, str]`：解析用户输入，识别调整意图
- `adjust_schedule(schedule: Dict[str, Any], intent: Dict[str, str]) -> Dict[str, Any]`：根据意图调整行程
- `recommend_pois(preferences: Dict[str, str]) -> List[Dict[str, Any]]`：根据偏好推荐POI
- `cluster_by_location(spots: List[Dict[str, Any]]) -> List[List[Dict[str, Any]]]`：使用地理位置对景点进行聚类
- `optimize_route(schedule: Dict[str, Any]) -> Dict[str, Any]`：优化行程路线 