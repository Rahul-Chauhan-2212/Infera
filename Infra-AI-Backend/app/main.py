from fastapi import FastAPI
from app.ollama_client import generate_completion
from pydantic import BaseModel

app = FastAPI()

class CodeRequest(BaseModel):
    language: str
    code: str
    cursor_line: int

@app.post("/suggest")
def suggest_code(req: CodeRequest):
    prompt = f"Complete this {req.language} code near line {req.cursor_line}:\n\n{req.code}"
    suggestion = generate_completion(prompt)
    return {"suggestion": suggestion}