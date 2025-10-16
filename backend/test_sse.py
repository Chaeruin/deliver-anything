import requests
import json
import time

BASE_URL = "https://api.deliver-anything.shop"
LOGIN_ENDPOINT = f"{BASE_URL}/api/v1/auth/login"
SSE_ENDPOINT = f"{BASE_URL}/api/v1/notifications/stream"

# 1. 로그인 정보
email = "test2@test.com"
password = "password2!"
# 초기 device_id는 임의로 설정하거나, 서버에서 새로 발급받을 수 있음
initial_device_id = "test_device_123"

# 2. 로그인 요청
print("로그인 요청 중...")
login_payload = {
    "email": email,
    "password": password
}
login_headers = {
    "Content-Type": "application/json",
    "X-Device-ID": initial_device_id # 로그인 시 X-Device-ID를 보냄
}

try:
    login_response = requests.post(LOGIN_ENDPOINT, json=login_payload, headers=login_headers)
    login_response.raise_for_status() # HTTP 오류 발생 시 예외 발생

    access_token = login_response.headers.get("Authorization")
    received_device_id_raw = login_response.headers.get("X-Device-ID")

    if not access_token:
        print("로그인 응답에서 Authorization 헤더를 찾을 수 없습니다.")
        print(f"응답 헤더: {login_response.headers}")
        print(f"응답 본문: {login_response.text}")
        exit()

    final_device_id = initial_device_id # 기본값은 초기 device_id
    if received_device_id_raw:
        final_device_id = received_device_id_raw.split(',')[0].strip()
        print(f"로그인 응답에서 X-Device-ID를 추출했습니다: {final_device_id}")
    else:
        print("로그인 응답에서 X-Device-ID 헤더를 찾을 수 없습니다. 초기 device_id를 사용합니다.")

    print("로그인 성공! (이전 단계에서 얻은 토큰 사용)")
    print(f"Access Token: {access_token}")
    print(f"Final X-Device-ID: {final_device_id}")

    # 3. SSE 연결
    print("\nSSE 연결 시도 중...")
    sse_headers = {
        "Accept": "text/event-stream",
        "Authorization": access_token,
        "X-Device-ID": final_device_id
    }

    # stream=True를 사용하여 응답을 스트리밍 방식으로 처리
    with requests.get(SSE_ENDPOINT, headers=sse_headers, stream=True) as sse_response:
        sse_response.raise_for_status() # HTTP 오류 발생 시 예외 발생

        print("SSE 연결 성공! 이벤트를 수신 중...")
        for line in sse_response.iter_lines():
            if line:
                decoded_line = line.decode('utf-8')
                print(decoded_line)
            # 연결이 끊어지면 루프 종료
            if not line and sse_response.raw.closed:
                print("SSE 연결이 종료되었습니다.")
                break
            time.sleep(0.1) # 너무 빠르게 읽지 않도록 잠시 대기

except requests.exceptions.RequestException as e:
    print(f"요청 중 오류 발생: {e}")
    if hasattr(e, 'response') and e.response is not None:
        print(f"응답 상태 코드: {e.response.status_code}")
        print(f"응답 본문: {e.response.text}")
except Exception as e:
    print(f"예상치 못한 오류 발생: {e}")