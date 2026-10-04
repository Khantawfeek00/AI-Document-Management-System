import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { Layout } from './components/Layout';
import { ProtectedRoute } from './components/ProtectedRoute';
import { LandingPage } from './pages/LandingPage';
import { DashboardPage } from './pages/DashboardPage';
import { FilesPage } from './pages/FilesPage';
import { FileDetailPage } from './pages/FileDetailPage';
import { DocumentSearchPage } from './pages/DocumentSearchPage';
import { UploadPage } from './pages/UploadPage';
import { TeamFilesPage } from './pages/TeamFilesPage';
import { UserRole } from './types';
import { AdminDashboard } from './pages/AdminDashboard';

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<LandingPage />} />

          <Route path="/ui" element={<Navigate to="/" replace />} />

          <Route element={<Layout />}>
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <DashboardPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/files"
              element={
                <ProtectedRoute>
                  <FilesPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/files/:fileId"
              element={
                <ProtectedRoute>
                  <FileDetailPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/search"
              element={
                <ProtectedRoute>
                  <DocumentSearchPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/upload"
              element={
                <ProtectedRoute>
                  <UploadPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/team-files"
              element={
                <ProtectedRoute requiredRoles={[UserRole.MANAGER, UserRole.ADMIN]}>
                  <TeamFilesPage />
                </ProtectedRoute>
              }
            />

            <Route
              path="/admin"
              element={
                <ProtectedRoute requiredRoles={[UserRole.ADMIN]}>
                  <AdminDashboard />
                </ProtectedRoute>
              }
            />
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
};

export default App;
