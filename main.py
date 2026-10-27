"""Mick AI secure server. Set MICK_ACCESS_TOKEN and OPENAI_API_KEY in environment."""
import os
import secrets
from fastapi import FastAPI, Header, HTTPException
from pydantic import BaseModel, Field
import httpx

app = FastAPI(title="Mick AI backend", docs_url=None, redoc_url=None)

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=4000)

@app.post("/chat")
async def chat(payload: ChatRequest, authorization: str = Header(default="")):
    expected = os.environ.get("MICK_ACCESS_TOKEN")
    if not expected:
        raise HTTPException(503, "Server authentication is not configured")
    if not secrets.compare_digest(authorization, f"Bearer {expected}"):
        raise HTTPException(401, "Unauthorized")
    key = os.environ.get("OPENAI_API_KEY")
    if not key:
        raise HTTPException(503, "AI provider is not configured")
    async with httpx.AsyncClient(timeout=55) as client:
        try:
            response = await client.post(
                "https://api.openai.com/v1/responses",
                headers={"Authorization": f"Bearer {key}"},
                json={
                    "model": os.getenv("OPENAI_MODEL", "gpt-4.1-mini"),
                    "instructions": "You are Mick, a friendly, cheeky but respectful Australian personal AI companion. Be helpful, clear and safe.",
                    "input": payload.message,
                    "max_output_tokens": 600,
                },
            )
            response.raise_for_status()
        except httpx.HTTPError:
            raise HTTPException(502, "AI provider unavailable")
    data = response.json()
    text = "".join(
        block.get("text", "")
        for item in data.get("output", [])
        for block in item.get("content", [])
        if block.get("type") == "output_text"
    )
    if not text:
        raise HTTPException(502, "AI provider returned no text")
    return {"reply": text}
