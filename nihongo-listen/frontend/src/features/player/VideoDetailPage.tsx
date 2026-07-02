import { useQuery } from '@tanstack/react-query';
import { useEffect, useRef, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../../shared/api/client';
import { useYouTubePlayer } from './useYouTubePlayer';

interface VideoDetail {
  id: number;
  youtubeId: string;
  title: string;
  channelName: string | null;
  jlptLevel: string | null;
}

interface Segment {
  id: number;
  startMs: number;
  endMs: number;
  text: string;
}

interface Transcript {
  transcriptId: number;
  language: string;
  source: string;
  segments: Segment[];
}

export default function VideoDetailPage() {
  const { id } = useParams<{ id: string }>();

  const { data: video, error: videoError } = useQuery({
    queryKey: ['video', id],
    queryFn: async () => (await api.get<VideoDetail>(`/videos/${id}`)).data,
    enabled: !!id,
  });

  const { data: transcript } = useQuery({
    queryKey: ['transcript', id],
    queryFn: async () => (await api.get<Transcript>(`/videos/${id}/transcript`)).data,
    enabled: !!id,
  });

  const { containerRef, currentMs, seekTo } = useYouTubePlayer(video?.youtubeId);
  const [showTranscript, setShowTranscript] = useState(true);

  if (videoError) {
    return (
      <div className="mt-10 text-center">
        <p className="text-red-600">Không tìm thấy video.</p>
        <Link to="/" className="text-indigo-600 hover:underline">
          ← Về danh sách
        </Link>
      </div>
    );
  }

  return (
    <div>
      <Link to="/" className="text-sm text-indigo-600 hover:underline">
        ← Về danh sách
      </Link>
      <div className="mt-2 grid grid-cols-1 gap-6 lg:grid-cols-5">
        {/* Player */}
        <div className="lg:col-span-3">
          <div className="overflow-hidden rounded-xl bg-black shadow">
            <div className="aspect-video w-full">
              <div ref={containerRef} className="h-full w-full" />
            </div>
          </div>
          <div className="mt-3 flex items-start justify-between gap-3">
            <div>
              <h1 className="text-lg font-bold">{video?.title ?? 'Đang tải…'}</h1>
              {video?.channelName && (
                <p className="text-sm text-slate-500">{video.channelName}</p>
              )}
            </div>
            {video?.jlptLevel && (
              <span className="rounded bg-indigo-100 px-2 py-1 text-sm font-semibold text-indigo-700">
                {video.jlptLevel}
              </span>
            )}
          </div>
        </div>

        {/* Transcript */}
        <div className="lg:col-span-2">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="font-semibold">
              Transcript{' '}
              {transcript && (
                <span className="text-xs font-normal text-slate-400">
                  ({transcript.source === 'manual' ? 'phụ đề chuẩn' : 'tự động'})
                </span>
              )}
            </h2>
            <button
              onClick={() => setShowTranscript((v) => !v)}
              className="rounded-lg border border-slate-300 px-2 py-1 text-xs hover:bg-slate-100"
            >
              {showTranscript ? 'Ẩn' : 'Hiện'}
            </button>
          </div>
          {showTranscript &&
            (transcript ? (
              <TranscriptPanel
                segments={transcript.segments}
                currentMs={currentMs}
                onSeek={seekTo}
              />
            ) : (
              <p className="text-sm text-slate-500">Video này chưa có transcript.</p>
            ))}
        </div>
      </div>
    </div>
  );
}

function TranscriptPanel({
  segments,
  currentMs,
  onSeek,
}: {
  segments: Segment[];
  currentMs: number;
  onSeek: (ms: number) => void;
}) {
  const activeIndex = segments.findIndex(
    (s) => currentMs >= s.startMs && currentMs < s.endMs,
  );
  const activeRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
  }, [activeIndex]);

  return (
    <div className="max-h-[70vh] space-y-1 overflow-y-auto rounded-xl bg-white p-3 shadow">
      {segments.map((seg, i) => (
        <button
          key={seg.id}
          ref={i === activeIndex ? activeRef : undefined}
          onClick={() => onSeek(seg.startMs)}
          className={`block w-full rounded-lg px-3 py-2 text-left text-[15px] leading-relaxed transition ${
            i === activeIndex
              ? 'bg-indigo-600 text-white'
              : 'text-slate-700 hover:bg-indigo-50'
          }`}
        >
          <span
            className={`mr-2 text-xs tabular-nums ${
              i === activeIndex ? 'text-indigo-200' : 'text-slate-400'
            }`}
          >
            {formatTime(seg.startMs)}
          </span>
          {seg.text}
        </button>
      ))}
    </div>
  );
}

function formatTime(ms: number): string {
  const totalSec = Math.floor(ms / 1000);
  const m = Math.floor(totalSec / 60);
  const s = totalSec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}
