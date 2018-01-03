@echo off
echo 正在编译 SimpleYacc 项目...
cd src
javac -encoding utf-8 -d ../bin/ *.java
cd ../bin
echo 编译完毕，开始测试运行
echo;

choice /t 1 /d y /n >nul

set curdir=%~dp0
cd /d %curdir%/bin

set A=1 2 3 4 5 6 7 8 9 10
set B=1 2 3 4 5 6 7
for %%b in (%B%) do (
  for %%a in (%A%) do (
    echo testcase %%b, tokenstream %%a
    java SimpleYacc ../testcases/testcase%%b/input.bnf ../testcases/testcase%%b/tokenstream%%a.tok
  )
  echo;
  pause
  echo;
)

for %%c in (8 9 10) do (
  echo testcase %%c
  java SimpleYacc ../testcases/testcase%%c/input.bnf ../testcases/testcase%%c/tokenstream1.tok
  echo;
  pause;
  echo;
)
cd ..
echo;
echo 测试完毕，按任意键退出
pause
