@echo off
chcp 65001 >nul
echo ========================================
echo SmartRead 数据同步工具
echo 从 Android 设备拉取 JSON 数据到 assets/
echo ========================================
echo.

REM 检查 adb
adb --version >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未找到 adb，请确保 Android SDK 已正确安装并添加到 PATH
    pause
    exit /b 1
)

REM 检查设备连接
adb devices | findstr /r "device$" >nul 2>&1
if %errorlevel% neq 0 (
    echo [错误] 未检测到 Android 设备，请连接设备或启动模拟器
    pause
    exit /b 1
)

set PACKAGE=com.aistudio.smartread.gkrqz
set REMOTE_DIR=/sdcard/Android/data/%PACKAGE%/files/Documents/SmartRead
set LOCAL_DIR=app\src\main\assets\data

echo 包名: %PACKAGE%
echo 远程路径: %REMOTE_DIR%
echo 本地路径: %LOCAL_DIR%
echo.

REM 创建本地目录
if not exist "%LOCAL_DIR%" mkdir "%LOCAL_DIR%"

set FILENAMES=books highlights notes chat_messages knowledge_nodes knowledge_edges reading_reports embeddings
set SUCCESS=0
set FAILED=0

for %%f in (%FILENAMES%) do (
    echo 拉取 %%f.json ...
    adb pull "%REMOTE_DIR%/%%f.json" "%LOCAL_DIR%\%%f.json" >nul 2>&1
    if %errorlevel% equ 0 (
        echo   [OK] %%f.json
        set /a SUCCESS+=1
    ) else (
        echo   [SKIP] %%f.json (文件不存在)
        set /a FAILED+=1
    )
)

echo.
echo ========================================
echo 同步完成: %SUCCESS% 个文件成功
echo ========================================
echo.
echo 请检查 %LOCAL_DIR% 目录下的文件，然后:
echo   git add app\src\main\assets\data\
echo   git commit -m "sync data"
echo   git push
echo.
pause