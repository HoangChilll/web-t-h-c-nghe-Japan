import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../shared/api/client';

interface Video {
  id: number;
  youtubeId: string;
  title: string;
  channelName: string | null;
  durationSec: number | null;
  thumbnailUrl: string | null;
  jlptLevel: string | null;
}

interface VideoPage {
  content: Video[];
  totalElements: number;
}

const LEVELS = ['N5', 'N4', 'N3', 'N2', 'N1'] as const;

async function fetchVideos(level: string | null): Promise<VideoPage> {
  const { data } = await api.get<VideoPage>('/videos', {
    params: { level: level ?? undefined, page: 0, size: 20 },
  });
  return data;
}

function formatDuration(sec: number | null): string {
  if (!sec) return '—';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

export default function VideoListPage() {
  const [level, setLevel] = useState<string | null>(null);
  const { data, isLoading, error } = useQuery({
    queryKey: ['videos', level],
    queryFn: () => fetchVideos(level),
  });

  return (
    <div>
      <h1 className="mb-4 text-2xl font-bold">Luyện nghe theo trình độ</h1>

      <div className="mb-6 flex flex-wrap gap-2">
        <FilterChip active={level === null} onClick={() => setLevel(null)}>
          Tất cả
        </FilterChip>
        {LEVELS.map((l) => (
          <FilterChip key={l} active={level === l} onClick={() => setLevel(l)}>
            {l}
          </FilterChip>
        ))}
      </div>

      {isLoading && <p className="text-slate-500">Đang tải video…</p>}
      {error != null && (
        <p className="text-red-600">Không tải được danh sách video. Backend đã chạy chưa?</p>
      )}
      {data && data.content.length === 0 && (
        <p className="text-slate-500">
          Chưa có video nào{level ? ` cho trình độ ${level}` : ''}. Admin hãy crawl và duyệt video.
        </p>
      )}

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {data?.content.map((v) => (
          <VideoCard key={v.id} video={v} />
        ))}
      </div>
    </div>
  );
}

function FilterChip({
  active,
  onClick,
  children,
}: {
  active: boolean;
  onClick: () => void;
  children: React.ReactNode;
}) {
  return (
    <button
      onClick={onClick}
      className={`rounded-full px-4 py-1.5 text-sm font-medium transition ${
        active
          ? 'bg-indigo-600 text-white'
          : 'bg-white text-slate-600 ring-1 ring-slate-300 hover:bg-slate-100'
      }`}
    >
      {children}
    </button>
  );
}

function VideoCard({ video }: { video: Video }) {
  return (
    <Link
      to={`/videos/${video.id}`}
      className="block overflow-hidden rounded-xl bg-white shadow transition hover:shadow-md"
    >
      {video.thumbnailUrl ? (
        <img src={video.thumbnailUrl} alt={video.title} className="aspect-video w-full object-cover" />
      ) : (
        <div className="flex aspect-video w-full items-center justify-center bg-slate-200 text-4xl">
          🎬
        </div>
      )}
      <div className="p-4">
        <div className="mb-1 flex items-center gap-2 text-xs">
          {video.jlptLevel && (
            <span className="rounded bg-indigo-100 px-2 py-0.5 font-semibold text-indigo-700">
              {video.jlptLevel}
            </span>
          )}
          <span className="text-slate-500">{formatDuration(video.durationSec)}</span>
        </div>
        <h2 className="line-clamp-2 font-semibold">{video.title}</h2>
        {video.channelName && <p className="mt-1 text-sm text-slate-500">{video.channelName}</p>}
      </div>
    </Link>
  );
}
