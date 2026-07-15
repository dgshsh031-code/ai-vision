import base64, time, httpx, os

GEMINI_API_KEY = os.getenv("GEMINI_API_KEY", "")
GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

PROMPTS = {
    "describe": "Что ты видишь на фото? Ответь коротко по-русски, 1-2 предложения.",
    "identify": "Что это за предмет или место? Назови кратко по-русски.",
    "read": "Прочитай весь текст на изображении. Только текст, без пояснений.",
    "help": "Посмотри на фото и дай практический совет по-русски. Что здесь происходит и что делать?",
}

async def analyze_image(image_bytes: bytes, mode: str = "describe") -> str:
    if not GEMINI_API_KEY:
        return "Ошибка: не задан GEMINI_API_KEY в .env"
    prompt = PROMPTS.get(mode, PROMPTS["describe"])
    b64 = base64.b64encode(image_bytes).decode()
    payload = {
        "contents": [{"parts": [
            {"text": prompt},
            {"inline_data": {"mime_type": "image/jpeg", "data": b64}}
        ]}],
        "generationConfig": {"maxOutputTokens": 300, "temperature": 0.4}
    }
    t0 = time.time()
    async with httpx.AsyncClient(timeout=30) as client:
        r = await client.post(GEMINI_URL, json=payload, params={"key": GEMINI_API_KEY})
        r.raise_for_status()
    text = r.json()["candidates"][0]["content"]["parts"][0]["text"].strip()
    print(f"[vision] mode={mode} latency={time.time()-t0:.2f}s")
    return text
