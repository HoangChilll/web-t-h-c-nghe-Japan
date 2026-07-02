import { type FormEvent, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { register } from './api';
import { useAuthStore } from './authStore';

export default function RegisterPage() {
  const navigate = useNavigate();
  const setAuth = useAuthStore((s) => s.setAuth);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    if (password.length < 8) {
      setError('Mật khẩu phải có ít nhất 8 ký tự');
      return;
    }
    setLoading(true);
    try {
      const resp = await register(email, password, displayName || undefined);
      setAuth(resp);
      navigate('/');
    } catch (err: unknown) {
      const message =
        (err as { response?: { data?: { error?: string } } }).response?.data?.error ??
        'Đăng ký thất bại';
      setError(message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="mx-auto mt-16 max-w-md rounded-xl bg-white p-8 shadow">
      <h1 className="mb-6 text-2xl font-bold">Đăng ký</h1>
      <form onSubmit={handleSubmit} className="space-y-4">
        <div>
          <label className="mb-1 block text-sm font-medium">Tên hiển thị</label>
          <input
            type="text"
            value={displayName}
            onChange={(e) => setDisplayName(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Email</label>
          <input
            type="email"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
        </div>
        <div>
          <label className="mb-1 block text-sm font-medium">Mật khẩu (≥ 8 ký tự)</label>
          <input
            type="password"
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full rounded-lg border border-slate-300 px-3 py-2 focus:border-indigo-500 focus:outline-none"
          />
        </div>
        {error && <p className="text-sm text-red-600">{error}</p>}
        <button
          type="submit"
          disabled={loading}
          className="w-full rounded-lg bg-indigo-600 py-2 font-semibold text-white hover:bg-indigo-700 disabled:opacity-50"
        >
          {loading ? 'Đang đăng ký…' : 'Tạo tài khoản'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Đã có tài khoản?{' '}
        <Link to="/login" className="font-medium text-indigo-600 hover:underline">
          Đăng nhập
        </Link>
      </p>
    </div>
  );
}
