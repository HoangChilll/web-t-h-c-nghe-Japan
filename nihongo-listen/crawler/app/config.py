from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    backend_internal_url: str = "http://localhost:8080"
    crawler_internal_secret: str = "change-me-shared-secret"
    redis_url: str = "redis://localhost:6379"

    class Config:
        env_file = ".env"
        extra = "ignore"


settings = Settings()
