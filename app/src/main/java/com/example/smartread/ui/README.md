
```
ui/
├── app/                    # 主应用界面模块
│   ├── components/        # 主应用专用UI组件(不共享)
│   │   └── AppTopBar.kt   # 应用顶部标题栏组件
│   │                      # 用途:统一的页面标题、返回按钮、操作按钮
│   │
│   ├── navigation/        # 导航相关组件
│   │   ├── AppNavGraph.kt # 应用导航图配置
│   │   │                  # 用途:定义所有页面的路由规则和跳转逻辑
│   │   │
│   │   └── BottomNavBar.kt # 底部导航栏UI组件
│   │                      # 用途:渲染底部Tab栏,处理选中状态和点击事件
│   │
│   ├── screens/           # 主要页面视图
│   │   ├── PageHome.kt    # 首页
│   │   │
│   │   ├── PageSearch.kt  # 搜索页
│   │   │
│   │   ├── PageBookShelf.kt # 书架页
│   │   │
│   │   └── PageMine.kt    # 我的页面
│   │
│   └── MainAppScaffold.kt # 主应用框架容器
│                          # 用途:整合底部导航 + 页面路由,应用级布局 scaffold
│
├── common/                # 通用组件模块(待开发)
│   └── 通用               # (空文件)
│
├── floating/              # 悬浮窗功能模块(待开发)
│   └── 悬浮窗             # (空文件)
│
├── theme/                 # 全局主题配置 ✓
│   ├── Color.kt           # 颜色定义
│   │                      # 用途:定义应用主色、辅助色、语义色
│   │
│   ├── Theme.kt           # 主题配置
│   │                      # 用途:Material3 主题、明暗模式切换
│   │
│   └── Type.kt            # 字体排版
│                          # 用途:定义标题、正文等文字样式
│
└── README.md              # UI模块说明文档

```
