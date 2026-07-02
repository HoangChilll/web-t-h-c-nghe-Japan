/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_URL?: string;
  readonly VITE_YOUTUBE_IFRAME_ORIGIN?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
