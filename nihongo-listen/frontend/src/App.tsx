import { Route, Routes } from 'react-router-dom';
import LoginPage from './features/auth/LoginPage';
import ProfilePage from './features/auth/ProfilePage';
import RegisterPage from './features/auth/RegisterPage';
import VideoDetailPage from './features/player/VideoDetailPage';
import VideoListPage from './features/videos/VideoListPage';
import Layout from './shared/ui/Layout';
import ProtectedRoute from './shared/ui/ProtectedRoute';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<VideoListPage />} />
        <Route path="/videos/:id" element={<VideoDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/profile" element={<ProfilePage />} />
        </Route>
      </Route>
    </Routes>
  );
}
