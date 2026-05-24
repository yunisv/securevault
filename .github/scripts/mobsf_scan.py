#!/usr/bin/env python3
"""
MobSF REST API клиент для CI/CD-пайплайна.
Загружает APK, запускает статический анализ, проверяет оценку.

API-ключ получается АВТОМАТИЧЕСКИ из запущенного контейнера MobSF —
никаких GitHub Secrets не требуется.

Диплом: «Обеспечение ИБ в мобильных приложениях», Глава 3.2
"""

import os
import sys
import time
import json
import requests
from bs4 import BeautifulSoup

MOBSF_URL = os.environ.get("MOBSF_URL", "http://localhost:8000")
APK_PATH  = os.environ.get("APK_PATH",  "")
MIN_SCORE = int(os.environ.get("MIN_SCORE", "70"))


def get_api_key(retries: int = 10, delay: int = 10) -> str:
    """
    Автоматически получает REST API ключ из запущенного MobSF-контейнера.
    Парсит страницу /api_docs/ — ключ отображается прямо там.
    Повторяет попытки пока контейнер не поднимется полностью.
    """
    print(f"[MobSF] Getting API key from {MOBSF_URL}/api_docs/ ...")

    for attempt in range(1, retries + 1):
        try:
            response = requests.get(
                f"{MOBSF_URL}/api_docs/",
                timeout=10
            )
            if response.status_code == 200:
                # Ключ находится в тексте страницы в формате:
                # REST API KEY: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
                text = response.text

                # Метод 1: прямой поиск в тексте
                if "REST API KEY" in text:
                    for line in text.splitlines():
                        if "REST API KEY" in line:
                            # Убираем HTML-теги если есть
                            clean = BeautifulSoup(line, "html.parser").get_text()
                            key = clean.split("REST API KEY")[-1].strip().strip(":").strip()
                            if key and len(key) >= 32:
                                print(f"[MobSF] API key obtained (length: {len(key)})")
                                return key

                # Метод 2: через BeautifulSoup
                soup = BeautifulSoup(text, "html.parser")
                for tag in soup.find_all(string=lambda t: t and "REST API KEY" in t):
                    key = tag.split("REST API KEY")[-1].strip().strip(":").strip()
                    if key and len(key) >= 32:
                        print(f"[MobSF] API key obtained via BS4 (length: {len(key)})")
                        return key

                print(f"[MobSF] Attempt {attempt}/{retries}: page loaded but key not found yet...")

        except requests.ConnectionError:
            print(f"[MobSF] Attempt {attempt}/{retries}: MobSF not ready yet...")
        except Exception as e:
            print(f"[MobSF] Attempt {attempt}/{retries}: {e}")

        if attempt < retries:
            time.sleep(delay)

    raise RuntimeError(
        f"Could not get MobSF API key after {retries} attempts. "
        "Make sure MobSF container is running on port 8000."
    )


def find_apk() -> str:
    """Найти APK-файл если путь не задан явно."""
    if APK_PATH and os.path.exists(APK_PATH):
        return APK_PATH

    import glob
    apks = glob.glob("**/*.apk", recursive=True)
    apks = [a for a in apks if "debug" in a.lower() or "release" in a.lower()]
    if not apks:
        raise FileNotFoundError(
            "APK file not found. Build it first:\n"
            "  ./gradlew assembleDebug"
        )
    print(f"[MobSF] APK found: {apks[0]}")
    return apks[0]


def upload_apk(apk_path: str, headers: dict) -> str:
    """Загрузить APK в MobSF, вернуть hash файла."""
    print(f"[MobSF] Uploading {os.path.basename(apk_path)} ...")
    with open(apk_path, "rb") as f:
        response = requests.post(
            f"{MOBSF_URL}/api/v1/upload",
            files={"file": (os.path.basename(apk_path), f, "application/octet-stream")},
            headers=headers,
            timeout=120
        )
    response.raise_for_status()
    data = response.json()
    file_hash = data.get("hash")
    print(f"[MobSF] Uploaded. Hash: {file_hash}")
    return file_hash


def start_scan(file_hash: str, headers: dict) -> None:
    """Запустить статический анализ."""
    print(f"[MobSF] Starting static analysis ...")
    response = requests.post(
        f"{MOBSF_URL}/api/v1/scan",
        data={"hash": file_hash, "scan_type": "apk"},
        headers=headers,
        timeout=300
    )
    response.raise_for_status()
    print("[MobSF] Scan initiated.")


def wait_for_report(file_hash: str, headers: dict, timeout: int = 360) -> dict:
    """Ожидать завершения сканирования и получить отчёт."""
    print("[MobSF] Waiting for scan to complete...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            response = requests.post(
                f"{MOBSF_URL}/api/v1/report_json",
                data={"hash": file_hash},
                headers=headers,
                timeout=30
            )
            if response.status_code == 200:
                data = response.json()
                if "security_score" in data:
                    print("[MobSF] Scan complete!")
                    return data
        except requests.RequestException:
            pass

        elapsed = int(time.time() - start)
        print(f"[MobSF] Scanning... ({elapsed}s elapsed)")
        time.sleep(15)

    raise TimeoutError(
        f"MobSF scan did not complete within {timeout} seconds."
    )


def download_pdf(file_hash: str, headers: dict) -> None:
    """Скачать PDF-отчёт."""
    print("[MobSF] Downloading PDF report...")
    try:
        response = requests.post(
            f"{MOBSF_URL}/api/v1/download_pdf",
            data={"hash": file_hash},
            headers=headers,
            timeout=60,
            stream=True
        )
        if response.status_code == 200:
            with open("mobsf_report.pdf", "wb") as f:
                for chunk in response.iter_content(chunk_size=8192):
                    f.write(chunk)
            size_kb = os.path.getsize("mobsf_report.pdf") // 1024
            print(f"[MobSF] PDF saved: mobsf_report.pdf ({size_kb} KB)")
        else:
            print(f"[MobSF] WARNING: PDF download failed (HTTP {response.status_code})")
    except Exception as e:
        print(f"[MobSF] WARNING: Could not download PDF: {e}")


def print_summary(report: dict) -> None:
    """Красивый вывод итогов."""
    score    = report.get("security_score", 0)
    grade    = report.get("security_grade", "?")
    app_name = report.get("app_name", "unknown")
    version  = report.get("version", "?")

    # Подсчёт находок по уровням
    high   = len([f for f in report.get("high", [])   if f])
    medium = len([f for f in report.get("warning", []) if f])
    info   = len([f for f in report.get("info", [])   if f])

    print()
    print("=" * 56)
    print("   MobSF Security Analysis Results")
    print("=" * 56)
    print(f"   App:       {app_name} v{version}")
    print(f"   Score:     {score} / 100  (Grade: {grade})")
    print(f"   High:      {high}")
    print(f"   Medium:    {medium}")
    print(f"   Info:      {info}")
    print("=" * 56)
    print()


def main():
    # Установить BeautifulSoup если нет
    try:
        from bs4 import BeautifulSoup
    except ImportError:
        print("[MobSF] Installing beautifulsoup4...")
        os.system("pip install beautifulsoup4 -q")

    try:
        # 1. Получить API-ключ автоматически
        api_key = get_api_key(retries=12, delay=10)
        headers = {"Authorization": api_key}

        # 2. Найти APK
        apk_path = find_apk()

        # 3. Загрузить и сканировать
        file_hash = upload_apk(apk_path, headers)
        start_scan(file_hash, headers)
        report    = wait_for_report(file_hash, headers)

        # 4. Вывести итоги и скачать отчёт
        print_summary(report)
        download_pdf(file_hash, headers)

        # 5. Проверить порог оценки
        score = report.get("security_score", 0)
        if score < MIN_SCORE:
            print(f"[MobSF] ❌ FAIL: score {score} < threshold {MIN_SCORE}")
            print("[MobSF]    See mobsf_report.pdf for details.")
            sys.exit(1)
        else:
            print(f"[MobSF] ✅ PASS: score {score} >= threshold {MIN_SCORE}")
            sys.exit(0)

    except Exception as e:
        print(f"[MobSF] ERROR: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
