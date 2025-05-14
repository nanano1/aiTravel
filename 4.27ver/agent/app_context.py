from agent.context_manager import TripContextManager
from langchain_openai import ChatOpenAI
from utils.file_utils import load_itinerary
import os

class AppContext:
    """统一管理应用程序共享资源的类"""
    _instance = None
    
    @classmethod
    def get_instance(cls):
        """获取单例实例"""
        if cls._instance is None:
            cls._instance = cls()
        return cls._instance
    
    def __init__(self):
        """初始化应用上下文"""
        # 创建一个空的行程数据作为默认值
        self.itinerary_data = {"travel_itineraries": []}
        self.context_manager = TripContextManager(self.itinerary_data)
        
        # 只在命令行模式下加载样例数据
        if os.environ.get("RUN_MODE") == "cli":
            try:
                # 构建行程文件绝对路径（仅用于命令行模式）
                base_dir = os.path.dirname(os.path.dirname(__file__))
                json_path = os.path.join(base_dir, 'data', 'sample_trip.json')
                
                self.itinerary_data = load_itinerary(json_path)
                # 重新初始化上下文管理器
                self.context_manager = TripContextManager(self.itinerary_data)
            except Exception as e:
                print(f"无法加载样例行程数据：{e}")
        
        # 初始化LLM
        self.llm = ChatOpenAI(
            api_key=os.getenv('DASHSCOPE_API_KEY'),
            base_url="https://dashscope.aliyuncs.com/compatible-mode/v1",
            model="qwen2.5-7b-instruct-1m",
            temperature=0.3,
        )
        
        # 其他工具和资源可以在这里添加
        self.tools = {}
    
    def update_context_manager(self, new_context_manager: TripContextManager):
        """更新上下文管理器
        
        在API模式下，我们需要能够替换上下文管理器，以便使用从API请求中接收的数据
        """
        self.context_manager = new_context_manager
        return True

