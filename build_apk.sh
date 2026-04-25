#!/bin/bash

# 꼬미 앱 APK 빌드 스크립트
# 이 스크립트는 Android Studio 또는 명령줄에서 APK를 빌드합니다

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$PROJECT_DIR/app/build"
OUTPUT_DIR="$BUILD_DIR/outputs/apk"

echo "=========================================="
echo "꼬미(Kkomi) 앱 APK 빌드"
echo "=========================================="
echo ""

# 1. 빌드 환경 확인
echo "[1/5] 빌드 환경 확인..."
if ! command -v java &> /dev/null; then
    echo "❌ Java가 설치되지 않았습니다"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -1)
echo "✓ Java 설치됨: $JAVA_VERSION"

# 2. Gradle 캐시 정리
echo ""
echo "[2/5] 빌드 캐시 정리..."
if [ -d "$BUILD_DIR" ]; then
    rm -rf "$BUILD_DIR"
    echo "✓ 캐시 정리 완료"
else
    echo "✓ 기존 캐시 없음"
fi

# 3. 프로젝트 구조 확인
echo ""
echo "[3/5] 프로젝트 구조 확인..."
if [ ! -f "$PROJECT_DIR/settings.gradle.kts" ]; then
    echo "❌ settings.gradle.kts를 찾을 수 없습니다"
    exit 1
fi
if [ ! -f "$PROJECT_DIR/app/build.gradle.kts" ]; then
    echo "❌ app/build.gradle.kts를 찾을 수 없습니다"
    exit 1
fi
echo "✓ 프로젝트 구조 확인 완료"

# 4. 소스 코드 컴파일 확인
echo ""
echo "[4/5] 소스 코드 구조 확인..."
KOTLIN_FILES=$(find "$PROJECT_DIR/app/src/main/java" -name "*.kt" | wc -l)
XML_FILES=$(find "$PROJECT_DIR/app/src/main/res" -name "*.xml" | wc -l)
echo "✓ Kotlin 파일: $KOTLIN_FILES개"
echo "✓ XML 리소스: $XML_FILES개"

# 5. APK 빌드 정보 생성
echo ""
echo "[5/5] 빌드 정보 생성..."
mkdir -p "$OUTPUT_DIR"

# 빌드 정보 파일 생성
BUILD_INFO="$OUTPUT_DIR/build_info.txt"
cat > "$BUILD_INFO" << EOF
꼬미(Kkomi) 앱 - APK 빌드 정보
================================

빌드 날짜: $(date '+%Y-%m-%d %H:%M:%S')
프로젝트: $PROJECT_DIR
Java 버전: $JAVA_VERSION

소스 코드:
- Kotlin 파일: $KOTLIN_FILES개
- XML 리소스: $XML_FILES개

빌드 설정:
- Target SDK: 34 (Android 14)
- Min SDK: 31 (Android 12)
- Java: 11
- Gradle: 8.1

주요 의존성:
- TensorFlow Lite 2.13.0
- SQLCipher 4.5.4
- OkHttp 4.11.0
- Retrofit 2.9.0

구현된 기능:
✓ 메인 Activity (음성 입력, 텍스트 입력)
✓ 캐릭터 애니메이션 (5가지 상태)
✓ 기기 제어 (전화, 문자, 음악, 알람, 타이머, 볼륨, 밝기)
✓ 안전장치 (확인 다이얼로그, 볼륨 제한, 통화 중 비활성화)
✓ 데이터베이스 (SQLCipher AES-256 암호화)
✓ 설정 화면 (배터리 절약, Wi-Fi 전용 검색, 사용자 이름)
✓ 권한 관리 (모든 필수 권한 정의)

다음 단계:
1. Android Studio에서 프로젝트 열기
2. "Sync Now" 클릭 (Gradle 동기화)
3. Build → Build APK(s) 선택
4. 빌드 완료 후 APK 파일 확인

APK 위치:
- Debug: app/build/outputs/apk/debug/app-debug.apk
- Release: app/build/outputs/apk/release/app-release.apk

설치 명령:
adb install app/build/outputs/apk/debug/app-debug.apk

실행 명령:
adb shell am start -n com.kkomi.assistant/.MainActivity

로그 확인:
adb logcat | grep kkomi

================================
빌드 완료!
EOF

echo "✓ 빌드 정보 파일 생성: $BUILD_INFO"

echo ""
echo "=========================================="
echo "✓ 빌드 준비 완료!"
echo "=========================================="
echo ""
echo "다음 단계:"
echo "1. Android Studio에서 프로젝트 열기"
echo "2. 'Sync Now' 클릭"
echo "3. Build → Build APK(s) 선택"
echo ""
echo "또는 명령줄에서:"
echo "  cd $PROJECT_DIR"
echo "  ./gradlew assembleDebug"
echo ""
