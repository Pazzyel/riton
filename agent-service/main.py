from __future__ import annotations

import uvicorn

from config.settings import get_agent_port


if __name__ == "__main__":
    uvicorn.run("api.app:app", host="0.0.0.0", port=get_agent_port(), reload=False)
