import React from 'react';
import ReactMarkdown from 'react-markdown';
import { 
  FiRefreshCcw, FiFileText, FiUser, FiLoader, 
  FiPaperclip, FiX, FiPlus, FiSend, FiTerminal
} from 'react-icons/fi';

export default function ChatArea({
  messages, isTyping, inputText, setInputText, 
  attachedFile, setAttachedFile, handleFileAttach, 
  handleSendMessage, handleClearChat, 
  chatScrollRef 
}) {

  return (
    <section className="flex-1 w-full h-full bg-[#0a0a0a] flex flex-col relative min-h-0 overflow-hidden">
      {/* HEADER */}
      <div className="px-8 py-6 flex justify-between items-center shrink-0 bg-[#0a0a0a]">
        <div>
          <h1 className="text-3xl font-black tracking-tight text-white">
            Developer Console
          </h1>
          <p className="mt-1 text-xs font-bold tracking-wide text-slate-500 uppercase">
            AI is ready to learn
          </p>
        </div>
        <button 
          onClick={handleClearChat} 
          className="p-3 rounded-2xl bg-[#1a1a1a] text-slate-500 hover:text-white hover:bg-white/10 transition-colors shadow-sm border border-white/5"
          title="Clear chat"
        >
          <FiRefreshCcw size={18} />
        </button>
      </div>

      {/* CHAT MESSAGES */}
      <div ref={chatScrollRef} className="custom-scrollbar flex-1 p-8 flex flex-col gap-6 overflow-y-auto bg-[#0a0a0a]/50">
        {messages.map((msg, idx) => (
          <div key={idx} className={`flex gap-4 ${msg.role === 'user' ? 'justify-end' : 'justify-start'}`}>
            {msg.role === 'ai' && (
              <div className="w-12 h-12 rounded-2xl bg-[#1a1a1a] border border-white/5 flex items-center justify-center shrink-0 shadow-sm">
                <FiTerminal size={20} className="text-white" />
              </div>
            )}
            
            <div className={`max-w-[75%] px-6 py-5 text-[15px] leading-relaxed border ${
              msg.role === 'user' 
                ? 'bg-white/10 text-white border-white/5 rounded-3xl rounded-tr-md shadow-md shadow-black/20 font-medium' 
                : 'bg-transparent text-slate-300 border-white/5 rounded-3xl rounded-tl-md shadow-sm font-medium'
            }`}>
              {msg.fileName && (
                <div className={`flex items-center gap-2 text-xs mb-3 px-4 py-2.5 rounded-xl font-bold ${
                  msg.role === 'user' ? 'bg-black/20 text-slate-200' : 'bg-white/5 text-slate-400 border border-white/5'
                }`}>
                  <FiFileText size={16} className={msg.role === 'user' ? 'text-slate-400' : 'text-slate-500'}/> {msg.fileName}
                </div>
              )}
              <div className="prose prose-invert prose-slate prose-sm max-w-none prose-p:leading-loose prose-pre:bg-[#0f0f0f] prose-pre:border prose-pre:border-white/5 whitespace-pre-wrap">
                  <ReactMarkdown>{msg.text}</ReactMarkdown>
              </div>
            </div>

            {msg.role === 'user' && (
              <div className="w-12 h-12 rounded-2xl bg-white/5 border border-white/10 flex items-center justify-center shrink-0 shadow-sm">
                <FiUser size={20} className="text-white" />
              </div>
            )}
          </div>
        ))}
        {isTyping && (
           <div className="flex items-center gap-3 text-slate-500 text-sm font-bold pl-2">
             <FiLoader className="animate-spin" size={18}/> AI is thinking...
           </div>
        )}
      </div>

      {/* INPUT AREA */}
      <div className="p-6 bg-[#0a0a0a] shrink-0">
        {attachedFile && (
          <div className="mb-4 inline-flex items-center gap-2 text-xs font-bold px-4 py-2 rounded-xl bg-[#1a1a1a] border border-white/5 text-slate-300">
            <FiPaperclip size={14} /> {attachedFile.name}
            <button onClick={() => setAttachedFile(null)} className="hover:text-red-400 transition-colors ml-2 bg-white/5 p-0.5 rounded-full shadow-sm border border-white/5"><FiX size={14} /></button>
          </div>
        )}
        <div className="flex gap-4 items-end">
          <input type="file" id="f-up" className="hidden" accept=".pdf" onChange={handleFileAttach} />
          <label 
            htmlFor="f-up" 
            className="w-14 h-14 rounded-2xl bg-[#1a1a1a] hover:bg-white/10 border border-white/5 flex items-center justify-center cursor-pointer shrink-0 transition-all text-slate-500 hover:text-white shadow-sm"
          >
            <FiPlus size={24} />
          </label>
          <textarea 
            value={inputText} onChange={(e) => setInputText(e.target.value)} 
            onKeyDown={(e) => e.key === 'Enter' && !e.shiftKey && (e.preventDefault(), handleSendMessage())}
            placeholder="Tell the AI a new rule or ask a question..." 
            className="custom-scrollbar flex-1 min-h-[56px] max-h-32 px-6 py-4 bg-[#0a0a0a] border border-white/5 rounded-2xl text-slate-200 text-base resize-none focus:outline-none focus:border-white/20 focus:ring-4 focus:ring-white/5 transition-all placeholder:text-slate-600 font-medium"
            rows={1}
          />
          <button 
            onClick={handleSendMessage} 
            disabled={!inputText.trim() && !attachedFile} 
            className={`w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 transition-all shadow-md ${
              (!inputText.trim() && !attachedFile)
                ? 'bg-[#1a1a1a] text-slate-600 cursor-not-allowed border border-white/5 shadow-none'
                : 'bg-white hover:bg-slate-200 text-[#0a0a0a] shadow-white/10 active:scale-95'
            }`}
          >
            <FiSend size={20} className={inputText.trim() || attachedFile ? 'translate-x-0.5' : ''} />
          </button>
        </div>
      </div>
    </section>
  );
}