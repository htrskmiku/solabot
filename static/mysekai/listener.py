import os
import re
from datetime import datetime
from fastapi import FastAPI, Request, HTTPException, Header
from fastapi.responses import PlainTextResponse, Response
from config import IN_PORT, OUT_PORT, LOCAL_IP, MYSEKAI_RAW_DIR, GAME_SERVER_MAP

app = FastAPI(
    title="MySekai Listener",
    description="Asynchronous server for receiving and saving API data",
    version="1.0.0"
)

def extract_api_type(url: str) -> str:
    if re.search(r'/mysekai(\?|$)', url):
        return 'mysekai'
    if re.search(r'/suite/', url):
        return 'suite'
    return 'unknown'

def extract_game_server(url: str) -> str:
    domain_match = re.search(r'https?://([^/]+)', url)
    if not domain_match:
        return "unknown"
    
    domain = domain_match.group(1)
    
    for server_domain, server_code in GAME_SERVER_MAP.items():
        if domain == server_domain or domain.endswith('.' + server_domain):
            return server_code
    
    return "unknown"

def generate_filename(api_type: str, original_url: str) -> str:
    # timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    user_id_match = re.search(r'/user/(\d+)', original_url)
    user_id = user_id_match.group(1) if user_id_match else "unknown"
    game_server = extract_game_server(original_url)
    return f"{game_server}_{api_type}_{user_id}.bin"

def create_app(queue=None):
    app = FastAPI(
        title="MySekai Listener",
        description="Asynchronous server for receiving and saving API data",
        version="1.0.0"
    )

    @app.get("/upload.js")
    async def get_upload_js():
        js_content = f"""
        const upload = () => {{
            $httpClient.post({{
                url: "https://{LOCAL_IP}:{OUT_PORT}/upload",
                headers: {{ 
                    "X-Original-Url": $request.url,
                    "X-Request-Path": $request.path
                }},
                body: $response.body
            }}, (error) => $done({{}}));
        }};
        upload();
        """.strip()
        
        return Response(
            content=js_content,
            media_type="application/javascript; charset=utf-8",
            headers={
                "Cache-Control": "no-store, no-cache, must-revalidate",
                "Pragma": "no-cache",
                "Expires": "0"
            }
        )

    @app.post("/upload")
    async def upload_data(request: Request, x_original_url: str = Header(None, alias="X-Original-Url")):
        if not x_original_url:
            raise HTTPException(status_code=400, detail="Missing X-Original-Url header")
        
        api_type = extract_api_type(x_original_url)
        filename = generate_filename(api_type, x_original_url)
        os.makedirs(MYSEKAI_RAW_DIR, exist_ok=True)
        filepath = os.path.join(MYSEKAI_RAW_DIR, filename)

        data = await request.body()
        with open(filepath, "wb") as f:
            f.write(data)

        print(f"Saved [{api_type.upper()}]: {filepath}")
        print(f"Source URL: {x_original_url[:100]}{'...' if len(x_original_url) > 100 else ''}")
        print(f"File Size: {len(data)/1024:.2f} KB\n")

        if queue:
            await queue.put(filepath)

        return PlainTextResponse("OK")

    @app.exception_handler(404)
    async def not_found_exception_handler(request: Request, exc: HTTPException):
        return PlainTextResponse("Not Found", status_code=404)
    
    return app


if __name__ == "__main__":
    import uvicorn
    
    print(f"Universal Data Receiver running at http://0.0.0.0:{IN_PORT}")
    print("File naming format: [api_type]_[user].bin\n")
    
    try:
        uvicorn.run(
            app, 
            host="0.0.0.0", 
            port=IN_PORT,
            log_level="info"
        )
    except KeyboardInterrupt:
        print("\nServer stopped by user")