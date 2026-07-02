import { useEffect, useRef, useState } from 'react';

// Khai báo tối thiểu cho YouTube IFrame API
interface YTPlayer {
  seekTo(seconds: number, allowSeekAhead: boolean): void;
  getCurrentTime(): number;
  destroy(): void;
}

declare global {
  interface Window {
    YT?: {
      Player: new (
        el: HTMLElement,
        opts: {
          videoId: string;
          playerVars?: Record<string, string | number>;
          events?: { onReady?: () => void };
        },
      ) => YTPlayer;
    };
    onYouTubeIframeAPIReady?: () => void;
  }
}

let apiPromise: Promise<void> | null = null;

function loadIframeApi(): Promise<void> {
  if (window.YT?.Player) return Promise.resolve();
  apiPromise ??= new Promise((resolve) => {
    window.onYouTubeIframeAPIReady = () => resolve();
    const tag = document.createElement('script');
    tag.src = 'https://www.youtube.com/iframe_api';
    document.head.appendChild(tag);
  });
  return apiPromise;
}

/**
 * Nhúng YouTube player vào containerRef và trả về thời gian phát hiện tại (ms).
 */
export function useYouTubePlayer(youtubeId: string | undefined) {
  const containerRef = useRef<HTMLDivElement>(null);
  const playerRef = useRef<YTPlayer | null>(null);
  const [ready, setReady] = useState(false);
  const [currentMs, setCurrentMs] = useState(0);

  useEffect(() => {
    if (!youtubeId || !containerRef.current) return;
    let cancelled = false;

    loadIframeApi().then(() => {
      if (cancelled || !containerRef.current || !window.YT) return;
      playerRef.current = new window.YT.Player(containerRef.current, {
        videoId: youtubeId,
        playerVars: { rel: 0, modestbranding: 1 },
        events: { onReady: () => setReady(true) },
      });
    });

    return () => {
      cancelled = true;
      playerRef.current?.destroy();
      playerRef.current = null;
      setReady(false);
    };
  }, [youtubeId]);

  useEffect(() => {
    if (!ready) return;
    const timer = setInterval(() => {
      const t = playerRef.current?.getCurrentTime?.();
      if (typeof t === 'number') setCurrentMs(Math.round(t * 1000));
    }, 300);
    return () => clearInterval(timer);
  }, [ready]);

  function seekTo(ms: number) {
    playerRef.current?.seekTo(ms / 1000, true);
    setCurrentMs(ms);
  }

  return { containerRef, currentMs, seekTo, ready };
}
