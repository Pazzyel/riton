# Agent Service

`agent-service` is the standalone Python module migrated from the original `agent` directory.

## Structure

- `api/`: FastAPI app and request schemas
- `agent/`: Agent runtime (MCP server, dto, service)
- `config/`: configuration loader and `application.yaml`
- `clients/`: backend HTTP client wrappers
- `middleware/`: optional auth middleware
- `main.py`: service startup entry

## Run

1. Install deps:

```bash
pip install -r requirements.txt
```

2. Configure `config/application.yaml`:

```yaml
server:
  port: 8046
tool:
  address: localhost:8045
model: qwen-plus
llm:
  baseUrl: https://dashscope.aliyuncs.com/compatible-mode/v1
  apiKey: your_api_key
```

3. Start:

```bash
python main.py
```

4. API:
- `POST /search/agent`
- header: `authorization: <token>`
- body: `{ "query": "..." }`

Query Example

```json
{
    "query": "为我推荐平台上可以吃水煮活鱼的地方"
}
```

Response Example

```json
[
    {
        "content": "炉鱼(拱墅万达广场店)提供水煮活鱼，位于北部新城杭行路666号万达商业中心，人均85元，评分4.7，24小时营业。",
        "shopDoc": {
            "id": 7,
            "name": "炉鱼(拱墅万达广场店)",
            "typeId": 1,
            "images": "https://img.meituan.net/msmerchant/909434939a49b36f340523232924402166854.jpg,https://img.meituan.net/msmerchant/32fd2425f12e27db0160e837461c10303700032.jpg,https://img.meituan.net/msmerchant/f7022258ccb8dabef62a0514d3129562871160.jpg",
            "area": "北部新城",
            "address": "杭行路666号万达商业中心4幢2单元409室(铺位号4005)",
            "x": 120.124691,
            "y": 30.336819,
            "avgPrice": 85,
            "sold": 2631,
            "score": 47,
            "openHours": "00:00-24:00"
        }
    },
    {
        "content": "海底捞火锅(水晶城购物中心店）也提供水煮活鱼，位于大关上塘路458号水晶城购物中心F6，人均104元，评分4.9，营业时间10:00-07:00。",
        "shopDoc": {
            "id": 5,
            "name": "海底捞火锅(水晶城购物中心店）",
            "typeId": 1,
            "images": "https://img.meituan.net/msmerchant/054b5de0ba0b50c18a620cc37482129a45739.jpg,https://img.meituan.net/msmerchant/59b7eff9b60908d52bd4aea9ff356e6d145920.jpg,https://qcloud.dpfile.com/pc/Qe2PTEuvtJ5skpUXKKoW9OQ20qc7nIpHYEqJGBStJx0mpoyeBPQOJE4vOdYZwm9AuzFvxlbkWx5uwqY2qcjixFEuLYk00OmSS1IdNpm8K8sG4JN9RIm2mTKcbLtc2o2vmIU_8ZGOT1OjpJmLxG6urQ.jpg",
            "area": "大关",
            "address": "上塘路458号水晶城购物中心F6",
            "x": 120.15778,
            "y": 30.310633,
            "avgPrice": 104,
            "sold": 4125,
            "score": 49,
            "openHours": "10:00-07:00"
        }
    }
]
```
