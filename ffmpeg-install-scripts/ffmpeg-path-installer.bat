ECHO OFF

IF NOT EXIST bin\ffmpeg.exe (
  CLS
  ECHO bin\ffmpeg.exe could not be found.
  GOTO:error
)

CD bin || GOTO:error
PROMPT $G
CLS
SETX PATH "%CD%;%PATH%"
ECHO.
ECHO Successfully set ffmpeg's PATH variable.
ECHO If you move this folder, make sure to run this script again.
ECHO.
PAUSE
GOTO:EOF

:error
  ECHO.
  ECHO Press any key to exit.
  PAUSE >nul
  GOTO:EOF
