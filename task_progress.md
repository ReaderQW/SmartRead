# SmartRead 悬浮窗 OCR 截图识别 + 自动创建书籍 改造计划

## 任务清单

- [x] 分析现有项目结构和代码
- [ ] 1. 添加 ML Kit OCR 依赖到 build.gradle.kts 和 libs.versions.toml
- [ ] 2. 创建 ML Kit OCR 识别工具类 (OcrTextRecognizer.kt)
- [ ] 3. 创建悬浮窗截图 + OCR 识别服务 (FloatOcrService.kt) - 替代原 SmartReadFloatingService
- [ ] 4. 创建书籍编辑弹窗 (BookEditDialog.kt) - 截图识别后弹出编辑书名
- [ ] 5. 创建书籍列表页面 (BookListScreen.kt) - 展示所有通过截图创建的书籍
- [ ] 6. 更新 AndroidManifest.xml - 添加权限和服务声明
- [ ] 7. 更新 MainActivity.kt - 集成 MediaProjection 授权和悬浮窗启动
- [ ] 8. 更新 SmartReadViewModel.kt - 添加 OCR 截图创建书籍相关方法
- [ ] 9. 更新 SmartReadApp.kt - 添加书籍列表导航
- [ ] 10. 更新 DashboardScreen.kt - 添加悬浮窗截图入口
- [ ] 11. 创建悬浮窗布局文件 (layout_float_window.xml)
- [ ] 12. 更新 strings.xml - 添加新字符串资源
