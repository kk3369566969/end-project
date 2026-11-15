@echo off
rem 双击运行：自动切到脚本所在目录（也就是项目根），再启动程序
cd /d "%~dp0"

if not exist "target\peer-review-tool.jar" (
    echo [错误] 未找到 target\peer-review-tool.jar
    echo 请先在项目根目录执行：mvn package -DskipTests
    pause
    exit /b 1
)

echo 工作目录：%CD%
rem 显式告诉程序应用根目录，配置、数据、日志都以它为基准
java -Dpeerreview.home="%~dp0." -jar target\peer-review-tool.jar
if errorlevel 1 pause
