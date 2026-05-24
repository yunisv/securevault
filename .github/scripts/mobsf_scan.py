#!/usr/bin/env python3
"""
MobSF REST API клиент для CI/CD-пайплайна.
Загружает APK, запускает статический анализ, проверяет оценку.

Диплом: «Обеспечение ИБ в мобильных приложениях», Глава 3.2
"""

import os
import sys
import time
import json
import requests

MOBSF_URL   = os.environ.get("MOBSF_URL",   "http://localhost:8000")
MOBSF_KEY   = os.environ.get("MOBSF_API_KEY", "")
APK_PATH    = os.environ.get("APK_PATH",    "")
MIN_SCORE   = int(os.environ.get("MIN_SCORE", "70"))

HEADERS = {"Authorization": MOBSF_KEY}


def find_apk() -> str:
    """Найти APK-файл если путь не задан."""
    if APK_PATH and os.path.exists(APK_PATH):
        return APK_PATH
    import glob
    apks = glob.glob("**/*.apk", recursive=True)
    apks = [a for a in apks if "debug" in a.lower()]
    if not apks:
        raise FileNotFoundError("APK file not found. Build it first with ./gradlew assembleDebug")
    print(f"[MobSF] APK found: {apks[0]}")
    return apks[0]


def upload_apk(apk_path: str) -> str:
    """Загрузить APK в MobSF, вернуть hash файла."""
    print(f"[MobSF] Uploading {apk_path}...")
    with open(apk_path, "rb") as f:
        response = requests.post(
            f"{MOBSF_URL}/api/v1/upload",
            files={"file": (os.path.basename(apk_path), f, "application/octet-stream")},
            headers=HEADERS,
            timeout=120
        )
    response.raise_for_status()
    data = response.json()
    file_hash = data.get("hash")
    print(f"[MobSF] Uploaded. Hash: {file_hash}")
    return file_hash


def start_scan(file_hash: str) -> None:
    """Запустить статический анализ."""
    print(f"[MobSF] Starting scan for hash: {file_hash}")
    response = requests.post(
        f"{MOBSF_URL}/api/v1/scan",
        data={"hash": file_hash, "scan_type": "apk"},
        headers=HEADERS,
        timeout=300
    )
    response.raise_for_status()
    print("[MobSF] Scan initiated.")


def wait_for_scan(file_hash: str, timeout: int = 300) -> dict:
    """Ожидать завершения сканирования."""
    print("[MobSF] Waiting for scan to complete...")
    start = time.time()
    while time.time() - start < timeout:
        try:
            response = requests.post(
                f"{MOBSF_URL}/api/v1/report_json",
                data={"hash": file_hash},
                headers=HEADERS,
                timeout=30
            )
            if response.status_code == 200:
                data = response.json()
                if "security_score" in data:
                    print("[MobSF] Scan complete!")
                    return data
        except requests.RequestException:
            pass
        time.sleep(15)
        print(f"[MobSF] Still scanning... ({int(time.time()-start)}s)")

    raise TimeoutError(f"MobSF scan did not complete within {timeout} seconds")


def download_report(file_hash: str) -> None:
    """Скачать PDF-отчёт."""
    print("[MobSF] Downloading PDF report...")
    response = requests.post(
        f"{MOBSF_URL}/api/v1/download_pdf",
        data={"hash": file_hash},
        headers=HEADERS,
        timeout=60,
        stream=True
    )
    if response.status_code == 200:
        with open("mobsf_report.pdf", "wb") as f:
            for chunk in response.iter_content(chunk_size=8192):
                f.write(chunk)
        print("[MobSF] PDF report saved as mobsf_report.pdf")
    else:
        print(f"[MobSF] WARNING: Could not download PDF (status {response.status_code})")


def print_summary(report: dict) -> None:
    """Вывести краткий отчёт."""
    score      = report.get("security_score", 0)
    grade      = report.get("security_grade", "?")
    app_name   = report.get("app_name", "unknown")
    version    = report.get("version", "unknown")

    # Подсчёт находок
    findings = report.get("findings", {})
    critical = sum(1 for f in findings.values() if isinstance(f, dict) and f.get("level") == "critical")
    high     = sum(1 for f in findings.values() if isinstance(f, dict) and f.get("level") == "high")
    medium   = sum(1 for f in findings.values() if isinstance(f, dict) and f.get("level") == "medium")

    print("\n" + "="*60)
    print("  MobSF Security Analysis Results")
    print("="*60)
    print(f"  App:      {app_name} v{version}")
    print(f"  Score:    {score}/100 (Grade: {grade})")
    print(f"  Critical: {critical}")
    print(f"  High:     {high}")
    print(f"  Medium:   {medium}")
    print("="*60 + "\n")


def main():
    if not MOBSF_KEY:
        print("[MobSF] ERROR: MOBSF_API_KEY environment variable not set")
        sys.exit(1)

    try:
        apk_path  = find_apk()
        file_hash = upload_apk(apk_path)
        start_scan(file_hash)
        report    = wait_for_scan(file_hash)

        print_summary(report)
        download_report(file_hash)

        score = report.get("security_score", 0)
        if score < MIN_SCORE:
            print(f"[MobSF] FAIL: Security score {score} is below minimum threshold {MIN_SCORE}")
            print("[MobSF] Check mobsf_report.pdf for details")
            sys.exit(1)
        else:
            print(f"[MobSF] PASS: Security score {score} meets minimum threshold {MIN_SCORE}")
            sys.exit(0)

    except Exception as e:
        print(f"[MobSF] ERROR: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()
