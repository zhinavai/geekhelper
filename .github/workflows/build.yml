name: Build Release APK

on:
  push:
    tags:
      - 'v*'

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
    - name: Checkout Repository
      uses: actions/checkout@v4

    - name: set up JDK 17
      uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: '17'

    # 🌟 修正點 1：升級至 v4，並加入 accept-licenses 參數以避免 sdkmanager 授權卡住報錯
    - name: Setup Android SDK
      uses: android-actions/setup-android@v3
      with:
        accept-licenses: true

    - name: Grant execute permission for gradlew
      run: chmod +x gradlew

    - name: Build Release APK
      env:
        SIGNING_KEY_STORE_BASE64: ${{ secrets.SIGNING_KEY_STORE_BASE64 }}
        KEY_STORE_PASSWORD: ${{ secrets.KEY_STORE_PASSWORD }}
        KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
        KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
      run: ./gradlew assembleRelease

    - name: Upload APK Artifact
      uses: actions/upload-artifact@v4
      with:
        name: GeekHelper-TV-Release
        path: app/build/outputs/apk/release/app-release.apk
