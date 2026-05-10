import React, { useState } from 'react';
import { Outlet, Link, useLocation, useNavigate } from 'react-router-dom';
import { FiZap, FiGithub, FiLinkedin, FiMail, FiLogOut } from 'react-icons/fi';

export default function MainLayout() {
  const location = useLocation();
  const navigate = useNavigate();

  const [globalTickets, setGlobalTickets] = useState(null);

  const navItems = [
    { name: 'Inbox', path: '/inbox' },
    { name: 'Console', path: '/knowledge' },
  ];

  const handleLogout = () => {
    if (window.confirm("Are you sure you want to logout?")) {
      localStorage.removeItem('userRole');
      localStorage.removeItem('userId');
      navigate('/');
    }
  };

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-[#0a0a0a] text-slate-200 flex flex-col">
      <header className="h-16 flex justify-between items-center px-8 border-b border-white/5 bg-[#0f0f0f]/80 backdrop-blur-md z-10 shrink-0">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-xl bg-white/10 border border-white/5 flex items-center justify-center text-white shadow-sm">
            <FiZap size={16} />
          </div>
          <h2 className="text-xl font-bold tracking-tight text-white">INLET</h2>
        </div>

        <nav className="flex items-center gap-8">
          {navItems.map((item) => {
            const isActive = location.pathname === item.path || (item.name === 'Console' && location.pathname.includes('knowledge'));
            return (
              <Link
                key={item.name}
                to={item.path}
                className={`text-sm font-semibold transition-colors ${isActive ? 'text-white' : 'text-slate-500 hover:text-slate-300'}`}
              >
                {item.name}
              </Link>
            );
          })}
        </nav>

        <button
          onClick={handleLogout}
          className="flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-semibold text-slate-500 hover:text-red-400 hover:bg-white/5 transition-colors"
        >
          <FiLogOut size={16} />
          Logout
        </button>
      </header>

      <main className="flex-1 overflow-hidden relative z-10 flex flex-col">
        <Outlet context={{ globalTickets, setGlobalTickets }} />
      </main>

      <footer className="h-12 flex justify-between items-center px-8 border-t border-white/5 bg-[#0a0a0a] shrink-0">
        <p className="text-xs font-medium text-slate-500">
          &copy; {new Date().getFullYear()} Inlet AI. All rights reserved.
        </p>
        <div className="flex items-center gap-4 text-slate-500">
          <a href="mailto:elangovan.ai.m@gmail.com" className="hover:text-white transition-colors"><FiMail size={16} /></a>
          <a href="https://linkedin.com/in/elangovanai" target="_blank" rel="noreferrer" className="hover:text-white transition-colors"><FiLinkedin size={16} /></a>
          <a href="https://github.com/Elangovan2006M" target="_blank" rel="noreferrer" className="hover:text-white transition-colors"><FiGithub size={16} /></a>
        </div>
      </footer>
    </div>
  );
}