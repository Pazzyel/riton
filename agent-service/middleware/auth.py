from __future__ import annotations

from fastapi import HTTPException, Request

from config.settings import get_redis_host, get_redis_password, get_redis_port


LOGIN_USER_KEY = "login:token:"


def register_auth_middleware(app) -> None:
    """注册基于 Redis token 的全局认证中间件。"""
    try:
        import redis
    except Exception:
        return

    redis_client = redis.Redis(
        host=get_redis_host(),
        port=get_redis_port(),
        password=get_redis_password() or None,
        db=0,
    )

    @app.middleware("http")
    async def authentication(request: Request, call_next):
        """校验请求头中的授权 token，并将其透传给后续处理。"""
        auth_header = request.headers.get("authorization")
        if not auth_header:
            raise HTTPException(status_code=401, detail="Unauthorized")

        token = auth_header.split(" ", 1)[-1]
        if not redis_client.exists(LOGIN_USER_KEY + token):
            raise HTTPException(status_code=401, detail="Invalid token")

        response = await call_next(request)
        response.headers["X-Received-Authorization"] = token
        return response
