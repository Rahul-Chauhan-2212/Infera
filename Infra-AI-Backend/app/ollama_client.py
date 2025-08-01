import requests

OLLAMA_URL = "http://localhost:11434/api/generate"

def generate_completion(prompt: str, model: str = "tinyllama") -> str:
    payload = {
        "model": model,
        "prompt": prompt,
        "stream": False
    }

    response = requests.post(OLLAMA_URL, json=payload)
    response.raise_for_status()
    return response.json()["response"].strip()