@echo off
set CLASSPATH=.;dist\*
java -Dwzpath=Data\ tools.QuestDropParser false
pause