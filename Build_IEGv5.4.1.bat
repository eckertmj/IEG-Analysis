@echo off
del *.class
rem javac -cp ij.jar *.java

javac -g:none -cp  ij.jar;jcuda.jar;jcudaUtils.jar;jna.jar;win32-amd64.jar;win32-x86.jar;platform.jar;jxl.jar *.java
//javac -cp ij.jar;jcuda.jar;jcudaUtils.jar *.java
jar cf IEG_AnalysisV5.4.3.jar *.class
del *.class
pause
