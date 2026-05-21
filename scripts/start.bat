@echo off
REM skills-SpringAI-agent 启动脚本 (Windows)

echo =========================================
echo   skills-SpringAI-agent 启动中...
echo =========================================

REM 加载环境变量
if exist .env (
    for /f "tokens=*" %%a in (.env) do set %%a
)

echo   端口: %SERVER_PORT%
echo   应用: skills-SpringAI-agent
echo =========================================

REM 下载依赖并启动
call mvnw.cmd spring-boot:run
