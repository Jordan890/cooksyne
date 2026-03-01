rootProject.name = "cart_and_cook"

include("core")
include("runtime:self-hosted")
include("adapters:persistence-jpa")
include("adapters:auth-local")
include("adapters:ai-local")
include("adapters:ai-remote")
