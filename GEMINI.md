# Project Instructions: Olive Young Tracker

## Overview
This project is a Spring Boot application that tracks product prices from Olive Young. It uses a Python-based crawler for data collection due to performance and anti-bot requirements.

## Crawling Strategy (Python-based)
- **Engine**: Python 3.x with `requests` and `BeautifulSoup4`.
- **Script**: `crawler.py` handles all scraping logic.
- **Integration**: `OliveyoungCrawler.java` invokes the Python script and parses its JSON output.
- **Endpoints**:
  - `POST /api/crawler/run`: Triggers a full ranking crawl.
  - `POST /api/crawler/import`: Accepts JSON data from external crawlers.

## Development Rules
- **NEVER** revert to Java Selenium for crawling unless explicitly requested.
- **Maintain** the Python script as the primary data collection tool.
- **Update** `requirements.txt` when adding Python dependencies.
- **Ensure** the Java DTOs (`CrawledProduct`) stay in sync with Python output.

## Python Dependencies
- `requests`
- `beautifulsoup4`
- `lxml`
