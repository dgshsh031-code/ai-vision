from fastapi import FastAPI, File, UploadFile, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
import uvicorn, os
from vision import analyze_image

app = FastAPI(title="AI Vision Backend")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
AUTH_TOKEN = os.getenv("AI_VISION_TOKEN", "ai-vision-2026")

@app.get("/health")
async def health(): return {"status": "ok"}

@app.post("/analyze")
async def analyze(image: UploadFile = File(...), x_auth_token: str = Header(default=""), mode: str = "describe"):
    if x_auth_token != AUTH_TOKEN:
        raise HTTPException(status_code=401, detail="Unauthorized")
    data = await image.read()
    if len(data) > 3 * 1024 * 1024:
        raise HTTPException(status_code=413, detail="Image too large (max 3MB)")
    text = await analyze_image(data, mode)
    return JSONResponse({"text": text})

if __name__ == "__main__":
    uvicorn.run(app, host="0.0.0.0", port=8765)
