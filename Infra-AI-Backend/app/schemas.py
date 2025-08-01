from pydantic import BaseModel

class CodeRequest(BaseModel):
    language: str
    code: str
    cursor_line: int

class CodeResponse(BaseModel):
    suggestion: str