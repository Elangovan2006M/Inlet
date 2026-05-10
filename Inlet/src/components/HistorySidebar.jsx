import React from 'react';
import { FiX, FiAlertCircle, FiTrash2, FiFileText } from 'react-icons/fi';
import ReactMarkdown from 'react-markdown';

export default function HistorySidebar({
  history, activePreview, setActivePreview, 
  handleWipeMemory, handleDeleteFile
}) {
  const formatContent = (rawContent) => {
    if (!rawContent) return '';
    let cleaned = rawContent
      .replace(/CRITICAL OVERRIDE: --- DOCUMENT ---/g, '')
      .replace(/Rule Text:/g, '')
      .replace(/CRITICAL OVERRIDE:/g, '')
      .trim();
    
    // Filter out backend metadata UUIDs
    cleaned = cleaned.split('\n')
      .filter(line => !line.includes('Metadata_LastUpdater') && !line.match(/^[0-9a-fA-F\-]{36}$/))
      .join('\n');
    
    return cleaned;
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '';
    // If it looks like just a time (e.g. 01:38 pm) without a year or month, append 'Today, '
    if (dateStr.toLowerCase().includes('pm') || dateStr.toLowerCase().includes('am')) {
       if (!dateStr.includes('202') && !dateStr.includes('/')) {
         return `Today, ${dateStr}`;
       }
    }
    return dateStr;
  };

  return (
    <section className="w-full h-full bg-[#121212] flex flex-col relative min-h-0 overflow-hidden shrink-0 border-l border-white/5">
      {activePreview ? (
        <div className="flex flex-col h-full min-h-0 animate-in slide-in-from-right-4 duration-300">
          <div className="p-6 flex justify-between items-center shrink-0 bg-[#121212]">
            <span className="text-xs font-bold text-slate-500 uppercase tracking-widest">
              {activePreview.type === 'PDF' ? 'Training File' : 'Custom Rule'}
            </span>
            <button 
              onClick={() => setActivePreview(null)} 
              className="p-2 rounded-xl text-slate-500 hover:text-white hover:bg-white/10 transition-colors shadow-sm"
            >
              <FiX size={18}/>
            </button>
          </div>
          
          <div className={`custom-scrollbar flex-1 min-h-0 overflow-y-auto ${activePreview.type === 'PDF' ? 'p-0' : 'p-6 bg-[#0a0a0a]/50'}`}>
            {activePreview.type === 'PDF' ? (
              <iframe src={activePreview.url} className="w-full h-full border-none bg-[#1a1a1a]" title="PDF Preview" />
            ) : (
              <div className="p-6 rounded-3xl bg-[#121212] border border-white/5 shadow-sm">
                <div className="flex items-center gap-2 text-white mb-4 font-bold text-sm">
                  <FiAlertCircle size={18} className="text-slate-400" /> Custom Rule Active
                </div>
                <div className="prose prose-invert prose-sm prose-p:leading-loose prose-strong:text-indigo-400 max-w-none">
                  <ReactMarkdown>{formatContent(activePreview.content)}</ReactMarkdown>
                </div>
                <div className="mt-6 pt-5 border-t border-white/5 text-xs text-slate-600 font-bold uppercase tracking-widest">
                   Saved to AI Memory
                </div>
              </div>
            )}
          </div>
        </div>
      ) : (
        <div className="flex flex-col h-full min-h-0 bg-[#121212]">
          <div className="px-8 py-7 flex justify-between items-center shrink-0 bg-[#121212]">
            <div>
              <h3 className="text-2xl font-black text-white tracking-tight">Your Files</h3>
              <p className="mt-1 text-xs font-bold text-slate-500 uppercase tracking-widest">Things you've taught the AI</p>
            </div>
            <button 
              onClick={handleWipeMemory} 
              title="Delete all files" 
              className="p-3 rounded-2xl bg-[#1a1a1a] text-slate-500 hover:text-red-400 hover:bg-red-500/10 transition-all shadow-sm border border-white/5"
            >
              <FiTrash2 size={18} />
            </button>
          </div>
          
          <div className="custom-scrollbar flex-1 min-h-0 p-6 overflow-y-auto bg-[#0a0a0a]/50">
            {history.length === 0 ? (
              <div className="text-center mt-12 flex flex-col items-center px-4">
                <div className="w-16 h-16 rounded-2xl bg-[#1a1a1a] border border-white/5 flex items-center justify-center mb-4 shadow-sm">
                  <FiFileText size={28} className="text-slate-600" />
                </div>
                <p className="text-sm font-bold text-slate-500">You haven't uploaded any files yet.</p>
              </div>
            ) : (
              history.map((doc, i) => (
                <div 
                  key={i} 
                  onClick={() => setActivePreview(doc)} 
                  className="p-4 rounded-2xl mb-4 flex items-center gap-4 cursor-pointer transition-all bg-[#121212] border border-white/5 hover:border-white/20 hover:shadow-sm group"
                >
                  <div className="w-12 h-12 rounded-xl flex items-center justify-center shrink-0 shadow-sm bg-[#1a1a1a] border border-white/5 text-slate-400">
                    {doc.type === 'PDF' ? <FiFileText size={20} /> : <FiAlertCircle size={20} />}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-[15px] font-bold text-white truncate group-hover:text-slate-200 transition-colors">{doc.name}</p>
                    <p className="mt-1 text-xs font-semibold text-slate-500">{formatDate(doc.date)}</p>
                  </div>
                  <button 
                    onClick={(e) => { e.stopPropagation(); handleDeleteFile(doc.name, e); }} 
                    className="p-2.5 text-slate-500 hover:text-red-400 hover:bg-red-500/10 rounded-xl transition-all opacity-0 group-hover:opacity-100"
                  >
                    <FiTrash2 size={16} />
                  </button>
                </div>
              ))
            )}
          </div>
          <div className="p-5 border-t border-white/5 bg-[#121212] shrink-0">
             <p className="text-xs text-slate-500 text-center font-bold">
                The AI reads these files to learn how to reply.
             </p>
          </div>
        </div>
      )}
    </section>
  );
}