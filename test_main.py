import os
from fastapi.testclient import TestClient
from main import app

client = TestClient(app)

def test_auth_required(monkeypatch):
    monkeypatch.setenv("MICK_ACCESS_TOKEN", "test-secret")
    response = client.post("/chat", json={"message": "hello"})
    assert response.status_code == 401

def test_empty_message_rejected(monkeypatch):
    monkeypatch.setenv("MICK_ACCESS_TOKEN", "test-secret")
    response = client.post("/chat", headers={"Authorization": "Bearer test-secret"}, json={"message": ""})
    assert response.status_code == 422

def test_missing_provider_key(monkeypatch):
    monkeypatch.setenv("MICK_ACCESS_TOKEN", "test-secret")
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    response = client.post("/chat", headers={"Authorization": "Bearer test-secret"}, json={"message": "hi"})
    assert response.status_code == 503
