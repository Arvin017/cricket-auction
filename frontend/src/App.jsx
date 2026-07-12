import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import TopNav from './components/TopNav';
import Login from './pages/Login';
import Signup from './pages/Signup';
import AuctionRoom from './pages/AuctionRoom';
import TeamDashboard from './pages/TeamDashboard';
import AdminPanel from './pages/AdminPanel';

function Shell() {
  const { user } = useAuth();
  return (
    <div className="app-shell">
      {user && <TopNav />}
      <div className="page-body">
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route path="/room" element={<ProtectedRoute><AuctionRoom /></ProtectedRoute>} />
          <Route path="/dashboard/:teamId" element={<ProtectedRoute requireRole="TEAM_REP"><TeamDashboard /></ProtectedRoute>} />
          <Route path="/admin" element={<ProtectedRoute requireRole="AUCTIONEER"><AdminPanel /></ProtectedRoute>} />
          <Route path="*" element={<Navigate to="/room" replace />} />
        </Routes>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Shell />
      </AuthProvider>
    </BrowserRouter>
  );
}
