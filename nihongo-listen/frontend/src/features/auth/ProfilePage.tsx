import { useQuery } from '@tanstack/react-query';
import { fetchProfile } from './api';

export default function ProfilePage() {
  const { data, isLoading, error } = useQuery({
    queryKey: ['me', 'profile'],
    queryFn: fetchProfile,
  });

  if (isLoading) return <p className="mt-10 text-center text-slate-500">Đang tải…</p>;
  if (error || !data)
    return <p className="mt-10 text-center text-red-600">Không tải được hồ sơ.</p>;

  return (
    <div className="mx-auto mt-10 max-w-lg rounded-xl bg-white p-8 shadow">
      <h1 className="mb-6 text-2xl font-bold">Hồ sơ của tôi</h1>
      <dl className="space-y-3 text-sm">
        <Row label="Tên hiển thị" value={data.displayName ?? '—'} />
        <Row label="Email" value={data.email} />
        <Row label="Gói" value={data.role} />
        <Row label="Mục tiêu JLPT" value={data.jlptTarget ?? 'Chưa đặt'} />
        <Row label="Mục tiêu mỗi ngày" value={`${data.dailyGoalMinutes} phút`} />
      </dl>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex justify-between border-b border-slate-100 pb-2">
      <dt className="text-slate-500">{label}</dt>
      <dd className="font-medium">{value}</dd>
    </div>
  );
}
