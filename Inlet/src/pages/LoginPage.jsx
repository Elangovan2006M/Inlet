import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { FiLock, FiMail, FiZap } from 'react-icons/fi';
import { supabase } from '../config/supabaseClient';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError('');

    try {
      // ACTUAL SUPABASE AUTH CALL
      const { data, error: authError } = await supabase.auth.signInWithPassword({
        email: email.trim(),
        password: password,
      });

      if (authError) throw authError;

      if (data.user) {
        localStorage.setItem('userId', data.user.id);

        const role = data.user.email.includes('admin') ? 'developer' : 'support';
        localStorage.setItem('userRole', role);

        navigate('/inbox');
      }
    } catch (err) {
      setError(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full flex bg-[#0a0a0a] text-slate-200 overflow-hidden font-sans">

      {/* LEFT SIDE - BRANDING */}
      <div className="hidden lg:flex w-1/2 relative bg-[#050505] flex-col justify-center items-center overflow-hidden border-r border-white/5">
        {/* Subtle Background Effects */}
        <div className="absolute top-1/4 -left-20 w-96 h-96 bg-indigo-500/10 rounded-full blur-[120px] pointer-events-none" />
        <div className="absolute bottom-1/4 -right-20 w-96 h-96 bg-blue-500/10 rounded-full blur-[120px] pointer-events-none" />

        {/* Abstract Creative Text Effect */}
        <div className="relative z-10 flex flex-col items-center">
          {/* Layered Text for 3D/Glowing Effect */}
          <div className="relative">
            <h1
              className="text-[12rem] font-black leading-none tracking-tighter"
              style={{
                WebkitTextStroke: '2px rgba(255, 255, 255, 0.1)',
                color: 'transparent',
                textShadow: '0 0 40px rgba(255, 255, 255, 0.05)'
              }}
            >
              INLET
            </h1>
            <h1
              className="text-[12rem] font-black leading-none tracking-tighter absolute top-2 left-2 opacity-50"
              style={{
                WebkitTextStroke: '1px rgba(99, 102, 241, 0.3)',
                color: 'transparent',
                filter: 'blur(4px)'
              }}
            >
              INLET
            </h1>
          </div>

          <div className="mt-8 text-center max-w-sm">
            <p className="text-lg font-medium text-slate-400 tracking-wide">
              The next-generation autonomous support platform.
            </p>
          </div>
        </div>
      </div>

      {/* RIGHT SIDE - LOGIN FORM */}
      <div className="w-full lg:w-1/2 flex items-center justify-center relative">
        <div className="w-full max-w-md p-8 sm:p-12">

          <div className="flex flex-col mb-10">
            <div className="w-12 h-12 rounded-xl bg-white/5 border border-white/10 flex items-center justify-center text-white mb-6 shadow-sm">
              <FiZap size={24} />
            </div>
            <h2 className="text-3xl font-bold tracking-tight text-white mb-2">Sign in to Inlet</h2>
            <p className="text-slate-400 font-medium">Welcome back. Please enter your details.</p>

            {error && (
              <div className="mt-6 w-full p-4 bg-red-500/10 border border-red-500/20 rounded-xl text-red-400 text-sm font-semibold flex items-center gap-2">
                {error}
              </div>
            )}
          </div>

          <form onSubmit={handleLogin} className="flex flex-col gap-6">
            <div className="relative">
              <label className="block text-sm font-semibold text-slate-400 mb-2">Email</label>
              <div className="relative">
                <FiMail className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                <input
                  type="email"
                  placeholder="name@company.com"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full pl-12 pr-4 py-3.5 bg-[#121212] border border-white/10 rounded-xl text-white placeholder:text-slate-600 focus:outline-none focus:border-white/30 focus:ring-4 focus:ring-white/5 transition-all font-medium"
                />
              </div>
            </div>

            <div className="relative">
              <label className="block text-sm font-semibold text-slate-400 mb-2">Password</label>
              <div className="relative">
                <FiLock className="absolute left-4 top-1/2 -translate-y-1/2 text-slate-500" size={18} />
                <input
                  type="password"
                  placeholder="••••••••"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className="w-full pl-12 pr-4 py-3.5 bg-[#121212] border border-white/10 rounded-xl text-white placeholder:text-slate-600 focus:outline-none focus:border-white/30 focus:ring-4 focus:ring-white/5 transition-all font-medium"
                />
              </div>
            </div>

            <div className="flex items-center justify-between text-sm mt-2">
              <label className="flex items-center gap-2 cursor-pointer group">
                <input type="checkbox" className="w-4 h-4 rounded border-white/10 bg-[#121212] text-white focus:ring-white/20 focus:ring-offset-[#0a0a0a]" />
                <span className="text-slate-400 font-medium group-hover:text-slate-300 transition-colors">Remember me</span>
              </label>
              <a href="#" className="font-semibold text-slate-400 hover:text-white transition-colors">Forgot password?</a>
            </div>

            <button
              disabled={loading}
              className="w-full py-3.5 mt-2 bg-white hover:bg-slate-200 text-black rounded-xl font-bold text-base transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-md shadow-white/5 active:scale-[0.98]"
            >
              {loading ? 'Authenticating...' : 'Sign In'}
            </button>

            <p className="text-center text-sm text-slate-500 font-medium mt-6">
              Don't have an account? <a href="#" className="text-white hover:underline font-semibold">Contact sales</a>
            </p>
          </form>

        </div>
      </div>
    </div>
  );
}