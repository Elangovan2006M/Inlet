import React, { useState, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { supabase } from './config/supabaseClient';
import MainLayout from './layouts/MainLayout';
import KnowledgePage from './pages/KnowledgePage';
import LoginPage from './pages/LoginPage';
import InboxPage from './pages/InboxPage';

const ProtectedRoute = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(null);

  useEffect(() => {
    supabase.auth.getSession().then(({ data: { session } }) => {
      setIsAuthenticated(!!session);
    });

    const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, session) => {
      setIsAuthenticated(!!session);
    });

    return () => subscription.unsubscribe();
  }, []);

  if (isAuthenticated === null) {
    return (
      <div className="flex h-screen w-screen items-center justify-center bg-[#0a0a0a]">
         <div className="w-8 h-8 border-4 border-white/10 border-t-white rounded-full animate-spin"></div>
      </div>
    );
  }

  return isAuthenticated ? <Outlet /> : <Navigate to="/" replace />;
};

function App() {
  return (
    <div className="h-screen w-screen overflow-hidden bg-[#0a0a0a] text-slate-200 antialiased">
      <Toaster 
        position="bottom-right"
        toastOptions={{
          style: {
            background: '#1a1a1a',
            color: '#fff',
            border: '1px solid rgba(255,255,255,0.05)',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.5)',
            borderRadius: '12px',
            fontSize: '14px',
            fontWeight: '600'
          },
        }}
      />
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<LoginPage />} />

          <Route element={<ProtectedRoute />}>
            <Route element={<MainLayout />}>
              <Route path="/inbox" element={<InboxPage />} />
              <Route path="/knowledge" element={<KnowledgePage />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </div>
  );
}

export default App;