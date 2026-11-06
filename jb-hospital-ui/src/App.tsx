import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';
import { AuthProvider, HOME, useAuth } from './context/AuthContext';
import { AppShell } from './components/AppShell';
import { RequireRole } from './components/RequireRole';
import { Card, EmptyState } from './components/ui';
import Login from './pages/Login';
import Registration from './pages/Registration';
import TriageQueue from './pages/Triage';
import VitalsPage from './pages/Triage/VitalsPage';
import DoctorWorkspace from './pages/Doctor';
import LabWorkspace from './pages/Lab';
import Staff from './pages/Staff';

function Home() {
  const { user } = useAuth();
  return <Navigate to={user ? HOME[user.role] : '/login'} replace />;
}

function Forbidden() {
  return (
    <Card className="max-w-lg mx-auto mt-10">
      <EmptyState icon={<ShieldAlert className="size-5" />} title="You don't have access to this area">
        Your role can't open this workspace. Use the menu to go to your own area.
      </EmptyState>
    </Card>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route element={<RequireRole />}>
            <Route element={<AppShell />}>
              <Route element={<RequireRole roles={['NURSE', 'ADMIN']} />}>
                <Route path="/registration" element={<Registration />} />
                <Route path="/triage" element={<TriageQueue />} />
                <Route path="/triage/:visitId" element={<VitalsPage />} />
              </Route>
              <Route element={<RequireRole roles={['DOCTOR']} />}>
                <Route path="/doctor" element={<DoctorWorkspace />} />
              </Route>
              <Route element={<RequireRole roles={['LABTECH', 'ADMIN']} />}>
                <Route path="/lab" element={<LabWorkspace />} />
              </Route>
              <Route element={<RequireRole roles={['ADMIN']} />}>
                <Route path="/staff" element={<Staff />} />
              </Route>
              <Route path="/forbidden" element={<Forbidden />} />
            </Route>
          </Route>
          <Route path="*" element={<Home />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}
