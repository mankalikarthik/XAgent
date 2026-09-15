# XAgent

XAgent is a local-first AI-assisted content research, generation, verification, approval, and publishing system for X.

It can:

- Fetch public web articles
- Extract useful article content
- Build grounded research briefs
- Generate posts using a local LLM
- Score generated content for factual accuracy and quality
- Require human approval before publishing
- Connect to an X account using OAuth 2.0
- Publish approved posts to X
- Persist OAuth tokens securely using AES-256-GCM encryption
- Store research, candidates, provenance, and publishing metadata in PostgreSQL

The system is designed so that AI-generated content is reviewed before anything is published.

---

## Architecture

```text
Public Article / URL
        |
        v
Research Source Ingestion
        |
        v
HTML Extraction
        |
        v
Research Brief
        |
        v
Local LLM
        |
        v
Generated Candidate
        |
        v
Fact / Quality Critic
        |
        v
Human Approval
        |
        v
X Publisher
        |
        v
X.com
