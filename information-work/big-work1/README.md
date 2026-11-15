# 网络爬虫实验

这是一个可以真实访问互联网的网页爬虫，使用 Python 标准库实现，不依赖
`requests`、`BeautifulSoup` 等第三方库。程序会发送真实 HTTP/HTTPS 请求，
解析网页标题、正文、摘要和链接，并保存抓取结果以及原始 HTML 文件。

## 文件说明

- `crawler.py`：爬虫核心和命令行入口，适合批量运行或实验脚本。
- `interactive_crawler.py`：中文交互入口，只需要输入一个网页地址。
- `data/crawl/`：默认输出目录，运行后自动创建。

## 最简单的运行方式

在当前目录打开 PowerShell，执行：

```powershell
python .\interactive_crawler.py
```

程序只会询问一次：

```text
请输入要抓取的网页地址：
```

输入网址后直接按回车，程序就会开始抓取。例如以下两种写法都可以：

```text
https://example.com/
www.iana.org/domains/example
```

交互模式使用固定默认配置：最多抓取 20 页、最大链接深度为 1、4 个并发线程、
20 秒请求超时、同一域名请求间隔 0.2 秒、遵守 `robots.txt`、只跟踪种子域名，
结果保存到 `data/crawl/results.jsonl`，原始 HTML 保存到 `data/crawl/raw/`。

抓取结束后会显示成功数量、失败数量、总字节数、耗时和结果文件路径。如果某个
网页失败，还会直接显示对应网址和失败原因。

## 命令行运行

直接指定一个网址：

```powershell
python .\crawler.py https://example.com/ --max-pages 5 --max-depth 1
```

指定多个网址和实验参数：

```powershell
python .\crawler.py https://www.iana.org/domains/example https://example.com/ `
  --max-pages 20 --max-depth 1 --workers 4 --timeout 15 --delay 0.5 `
  --output data\crawl\results.jsonl --raw-dir data\crawl\raw
```

也可以从种子文件读取网址，每行一个网址，空行和 `#` 开头的行会被忽略：

```powershell
python .\crawler.py --seed-file .\seeds.txt --max-pages 50
```

## 命令行参数

| 参数 | 说明 | 默认值 |
| --- | --- | --- |
| `--max-pages` | 最多抓取的网页数量，包含失败请求 | `20` |
| `--max-depth` | 从种子网址开始的最大链接深度 | `1` |
| `--workers` | 并发抓取线程数 | `4` |
| `--timeout` | 单个请求超时时间，单位为秒 | `15` |
| `--delay` | 同一域名两次请求的最小间隔，单位为秒 | `0` |
| `--max-bytes` | 单个响应允许读取的最大字节数 | `5242880` |
| `--output` | 结果文件路径 | `data/crawl/results.jsonl` |
| `--format` | 输出格式，可选 `jsonl` 或 `csv` | 根据扩展名判断 |
| `--raw-dir` | 原始 HTML 保存目录 | `data/crawl/raw` |
| `--seed-file` | 种子网址文件 | 无 |
| `--allow-external` | 允许跟踪到种子域名以外的链接 | 默认关闭 |
| `--no-robots` | 不读取或遵守 `robots.txt` | 默认遵守 |
| `--verbose` | 显示详细调试信息 | 默认关闭 |

## 输出内容

JSONL 每行对应一个抓取结果，包含：

- 请求网址、最终网址、抓取深度和 HTTP 状态码；
- 内容类型、响应字节数和请求耗时；
- 网页标题、`meta description`、可见正文和发现的链接；
- 抓取时间、错误信息和原始 HTML 文件路径。

CSV 适合用 Excel 或表格软件打开，用于统计每个网站的抓取速度、页面大小和
失败情况。原始 HTML 可以用于后续网页清洗、噪声比例和正文提取实验。

## 抓取规则

程序默认遵守 `robots.txt`，并且只跟踪种子网址所在的域名。默认会过滤常见的
跟踪参数，去掉 URL 片段，限制响应大小，并使用 HTTPS 证书校验。请只抓取自己
有权访问和分析的网站，并设置合理的页数、并发数和请求间隔。

运行环境要求 Python 3.10 或更高版本。程序在没有 Python 加入系统 PATH 时，
也可以使用 Python 可执行文件的完整路径启动。
