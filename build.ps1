# build.ps1

param([switch]$Installer)

Push-Location "K:\Programm\IDEA Project\KoverG\KoverG"

# Удаляем старые образы из прошлых jpackage?запусков
if (Test-Path .\KoverG) {
  Remove-Item .\KoverG -Recurse -Force
}

# 1) Собираем JAR (очищает target/)
mvn clean package

# 2) Пакуем образ
$type = if ($Installer) { 'exe' } else { 'app-image' }
& "C:\Program Files\Java\jdk-23\bin\jpackage.exe" `
  --type $type `
  --name KoverG `
  --input target `
  --main-jar KoverG-1.0-SNAPSHOT-with-dependencies.jar `
  --main-class app.Main `
  --module-path "C:\Program Files\Java\jdk-23\jmods;K:\Programm\javafx-jmods-24.0.2" `
  --add-modules javafx.controls,javafx.fxml,java.logging,java.net.http `
  --icon "src\main\resources\icon.ico"

Pop-Location
