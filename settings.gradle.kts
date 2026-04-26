rootProject.name = "cooksyne"

include("core")
include("runtime:self-hosted")
include("adapters:persistence-jpa")
include("adapters:auth-local")
include("adapters:ai-ollama")
include("adapters:ai-openai")
include("adapters:ai-bedrock")
include("adapters:ai-huggingface")
