import { Link, Outlet, useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/authStore';

export default function Layout() {
  const navigate = useNavigate();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  function handleLogout() {
    logout();
    navigate('/');
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-6xl items-center justify-between px-4 py-3">
          <Link to="/" className="text-xl font-bold text-indigo-600">
            🎧 Nihongo Listen
          </Link>
          <nav className="flex items-center gap-4 text-sm">
            <Link to="/" className="text-slate-600 hover:text-indigo-600">
              Video
            </Link>
            {user ? (
              <>
                <Link to="/profile" className="text-slate-600 hover:text-indigo-600">
                  {user.email}
                </Link>
                <button
                  onClick={handleLogout}
                  className="rounded-lg border border-slate-300 px-3 py-1.5 hover:bg-slate-100"
                >
                  Đăng xuất
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="text-slate-600 hover:text-indigo-600">
                  Đăng nhập
                </Link>
                <Link
                  to="/register"
                  className="rounded-lg bg-indigo-600 px-3 py-1.5 font-medium text-white hover:bg-indigo-700"
                >
                  Đăng ký
                </Link>
              </>
            )}
          </nav>
        </div>
      </header>
      <main className="mx-auto max-w-6xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  );
}
