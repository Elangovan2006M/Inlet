import React, { useState, useEffect } from 'react';
import { FiSend, FiZap, FiPaperclip, FiDownload, FiCornerUpLeft, FiRefreshCw, FiLoader, FiEdit2, FiEye, FiCheckCircle, FiInbox, FiMessageSquare, FiSearch } from 'react-icons/fi';
import { toast } from 'react-hot-toast';
import ReactMarkdown from 'react-markdown';
import { Panel, Group as PanelGroup, Separator as PanelResizeHandle } from 'react-resizable-panels';
import { useOutletContext } from 'react-router-dom';
import { API_ENDPOINTS } from '../config/api';

export default function InboxPage() {
  const { globalTickets, setGlobalTickets } = useOutletContext();
  
  const [tickets, setTickets] = useState(globalTickets || []);
  const [activeTicket, setActiveTicket] = useState(null);
  const [replyText, setReplyText] = useState('');
  
  const [isGenerating, setIsGenerating] = useState(false);
  const [isSending, setIsSending] = useState(false);
  const [isInitialLoad, setIsInitialLoad] = useState(!globalTickets);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isPreviewMode, setIsPreviewMode] = useState(false);
  
  const [filterStatus, setFilterStatus] = useState('all');
  const [searchQuery, setSearchQuery] = useState('');

  useEffect(() => {
    const controller = new AbortController();
    fetchLiveInbox(false, controller.signal);
    
    const liveSyncInterval = setInterval(() => fetchLiveInbox(true, controller.signal), 15000);
    return () => {
      clearInterval(liveSyncInterval);
      controller.abort();
    };
  }, []);

  useEffect(() => {
    if (tickets.length > 0) {
      setGlobalTickets(tickets);
    }
  }, [tickets, setGlobalTickets]);

  const fetchLiveInbox = async (isBackground = false, signal = null) => {
    if (!isBackground && !isInitialLoad) setIsRefreshing(true);
    try {
      const res = await fetch(API_ENDPOINTS.SCAN_INBOX, { signal });
      const incomingData = await res.json();
      if (Array.isArray(incomingData)) {
        setTickets(incomingData);
      }
    } catch (err) {
      if (err.name !== 'AbortError') console.error(err);
    } finally {
      if (!signal || !signal.aborted) {
        setIsInitialLoad(false);
        setIsRefreshing(false);
      }
    }
  };

  const handleTicketClick = async (ticket) => {
    setActiveTicket({ ...ticket, isLoadingDetails: true });
    setReplyText('');
    setIsPreviewMode(false);
    
    if (ticket.isUnread) {
      setTickets(prev => prev.map(t => t.id === ticket.id ? { ...t, isUnread: false } : t));
      try {
        await fetch(API_ENDPOINTS.MARK_READ(ticket.id), { method: 'POST' });
      } catch (err) {
        console.error(err);
      }
    }

    try {
      const res = await fetch(API_ENDPOINTS.TICKET_DETAILS(ticket.id));
      const details = await res.json();
      
      setActiveTicket(prev => {
        if (prev?.id === ticket.id) {
          return { 
            ...prev, 
            fullBody: details.fullBody, 
            attachments: details.attachments,
            isLoadingDetails: false 
          };
        }
        return prev;
      });
    } catch (err) {
      setActiveTicket(prev => ({ ...prev, isLoadingDetails: false, fullBody: "Error loading email content." }));
    }
  };

  const handleGenerateReply = async () => {
    if (!activeTicket) return;
    setIsGenerating(true);
    setReplyText('Querying knowledge base and drafting reply...'); 
    setIsPreviewMode(false);
    
    try {
      const response = await fetch(API_ENDPOINTS.GENERATE_REPLY, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          customerMessage: activeTicket.fullBody,
          customerName: activeTicket.sender
        })
      });
      const data = await response.json();
      setReplyText(data.draft);
      setIsPreviewMode(true);
    } catch (error) {
      setReplyText("Error connecting to the AI brain.");
    } finally {
      setIsGenerating(false);
    }
  };

  const handleSendReply = async () => {
    if (!activeTicket || !replyText.trim()) return;
    setIsSending(true);
    
    try {
      await fetch(API_ENDPOINTS.SEND_REPLY, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          to: activeTicket.senderEmail || activeTicket.sender,
          subject: activeTicket.subject,
          body: replyText,
          messageId: activeTicket.id
        })
      });

      setTickets(prev => prev.filter(t => t.id !== activeTicket.id));
      setActiveTicket(null);
      setReplyText('');
      setIsPreviewMode(false);
      
      toast.success('Email sent successfully');
      
    } catch (err) {
      console.error("Failed to send email:", err);
      toast.error("Failed to send email. Check backend console.");
    } finally {
      setIsSending(false);
    }
  };

  const filteredTickets = tickets.filter(ticket => {
    const matchesStatus = filterStatus === 'all' 
      ? true 
      : filterStatus === 'unread' ? ticket.isUnread : !ticket.isUnread;
      
    const searchLower = searchQuery.toLowerCase();
    const matchesSearch = !searchQuery 
      ? true 
      : ticket.sender.toLowerCase().includes(searchLower) || ticket.subject.toLowerCase().includes(searchLower);
      
    return matchesStatus && matchesSearch;
  });

  return (
    <div className="flex flex-col h-full relative bg-[#0a0a0a]">
      <PanelGroup direction="horizontal" className="h-full w-full">
        <Panel id="inbox-panel" order={1} defaultSize={25} minSize={20} className="bg-[#121212] flex flex-col h-full border-r border-white/5 transition-all">
          <div className="p-5 border-b border-white/5 flex flex-col gap-4 bg-[#121212] shrink-0">
            <div className="flex justify-between items-center">
              <h2 className="text-lg font-bold text-white tracking-tight">Inbox</h2>
              <button 
                onClick={() => fetchLiveInbox(false)}
                className="w-8 h-8 flex items-center justify-center rounded-lg text-slate-400 hover:text-white hover:bg-white/5 transition-colors"
              >
                <FiRefreshCw size={14} className={isRefreshing ? "animate-spin text-white" : ""} />
              </button>
            </div>
            
            <div className="relative">
               <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" size={14} />
               <input 
                 type="text" 
                 placeholder="Search emails..."
                 value={searchQuery}
                 onChange={(e) => setSearchQuery(e.target.value)}
                 className="w-full bg-[#0a0a0a] border border-white/5 rounded-xl pl-9 pr-4 py-2 text-sm font-medium text-white placeholder:text-slate-600 focus:outline-none focus:border-white/20 transition-all shadow-sm"
               />
            </div>

            <div className="flex bg-[#0a0a0a] p-1 rounded-xl border border-white/5 shadow-sm">
              {['all', 'unread', 'read'].map(status => (
                <button
                  key={status}
                  onClick={() => setFilterStatus(status)}
                  className={`flex-1 text-xs font-bold py-1.5 rounded-lg capitalize transition-all ${
                    filterStatus === status 
                      ? 'bg-white text-black shadow-sm scale-[0.98]' 
                      : 'text-slate-500 hover:text-slate-300'
                  }`}
                >
                  {status}
                </button>
              ))}
            </div>
          </div>
          
          <div className="custom-scrollbar flex-1 overflow-y-auto">
            {isInitialLoad ? (
              <div className="flex flex-col gap-4 justify-center items-center h-48 text-slate-500">
                <FiLoader className="animate-spin" size={24} />
                <span className="text-sm font-semibold">Loading...</span>
              </div>
            ) : filteredTickets.length === 0 ? (
              <div className="flex flex-col items-center justify-center h-64 text-center px-6">
                <div className="w-16 h-16 bg-[#1a1a1a] border border-white/5 text-slate-400 rounded-full flex items-center justify-center mb-4 shadow-sm">
                  <FiInbox size={28} />
                </div>
                <h3 className="text-white font-bold mb-1">No matching emails</h3>
                <p className="text-slate-500 text-sm font-medium">Try adjusting your filters.</p>
              </div>
            ) : (
              <div className="flex flex-col">
                {filteredTickets.map((ticket) => (
                  <div 
                    key={ticket.id} 
                    onClick={() => handleTicketClick(ticket)}
                    className={`p-4 cursor-pointer border-b border-white/5 flex flex-col gap-2 transition-all ${
                      activeTicket?.id === ticket.id 
                        ? 'bg-[#1a1a1a] shadow-[inset_4px_0_0_0_#ffffff]' 
                        : 'bg-transparent hover:bg-white/5'
                    }`}
                  >
                    <div className="flex justify-between items-center">
                      <div className="flex items-center gap-2 overflow-hidden">
                        {ticket.isUnread ? (
                          <div className="w-2 h-2 rounded-full bg-indigo-500 shrink-0" />
                        ) : (
                          <div className="w-2 h-2 shrink-0" /> 
                        )}
                        <span className={`text-sm truncate ${ticket.isUnread ? 'font-bold text-white' : 'font-semibold text-slate-400'}`}>
                          {ticket.sender}
                        </span>
                      </div>
                      <span className={`text-xs shrink-0 ml-2 font-semibold ${ticket.isUnread ? 'text-indigo-400' : 'text-slate-500'}`}>
                        {ticket.time}
                      </span>
                    </div>
                    <div className={`text-sm pl-4 truncate ${ticket.isUnread ? 'font-bold text-white' : 'font-medium text-slate-400'}`}>
                      {ticket.subject}
                    </div>
                    <div className="text-xs text-slate-500 pl-4 truncate font-medium">
                      {ticket.preview}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Panel>
        
        <PanelResizeHandle className="w-4 relative flex justify-center items-center group cursor-col-resize z-20 outline-none hover:bg-white/5 transition-colors">
          <div className="w-[1px] h-full bg-white/5 group-hover:bg-white/30 transition-colors" />
        </PanelResizeHandle>

        <Panel id="viewer-panel" order={2} defaultSize={45} minSize={30} className="bg-[#0a0a0a] flex flex-col h-full">
          
          {activeTicket ? (
            <div className="custom-scrollbar flex-1 p-10 overflow-y-auto">
              <h1 className="text-3xl font-extrabold text-white mb-8 tracking-tight leading-tight">{activeTicket.subject}</h1>
              
              <div className="flex gap-4 mb-10 pb-8 border-b border-white/5">
                <div className="w-12 h-12 rounded-full bg-[#1a1a1a] border border-white/10 text-white flex items-center justify-center font-bold text-xl shrink-0">
                  {activeTicket.sender.charAt(0).toUpperCase()}
                </div>
                <div className="flex-1 flex justify-between items-start pt-1">
                  <div className="flex flex-col">
                    <strong className="text-white font-bold text-lg">{activeTicket.sender}</strong>
                    <span className="text-slate-400 text-sm font-medium">
                      {activeTicket.senderEmail || 'customer@email.com'}
                    </span>
                  </div>
                  <div className="text-slate-500 text-sm font-semibold">
                    {activeTicket.time}
                  </div>
                </div>
              </div>

              {activeTicket.isLoadingDetails ? (
                <div className="flex items-center gap-3 text-slate-500 mb-8 font-bold text-lg">
                  <FiLoader className="animate-spin" /> Loading email...
                </div>
              ) : (
                <div className="text-slate-300 leading-loose whitespace-pre-wrap mb-10 text-[15px] font-medium">
                  {activeTicket.fullBody}
                </div>
              )}

              {!activeTicket.isLoadingDetails && activeTicket.attachments && activeTicket.attachments.length > 0 && (
                <div className="mt-8">
                  <span className="text-xs font-bold text-slate-500 uppercase tracking-widest">{activeTicket.attachments.length} Attachments</span>
                  
                  <div className="flex flex-wrap gap-4 mt-4">
                    {activeTicket.attachments.map((file, idx) => (
                      <a 
                        key={idx} 
                        href={file.dataUri || file.url} 
                        download={file.filename} 
                        className="flex items-center justify-between w-64 p-3 bg-[#121212] border border-white/5 hover:border-white/20 hover:shadow-sm rounded-xl transition-all group" 
                      >
                        <div className="flex items-center gap-3 overflow-hidden">
                          <div className="p-2 bg-white/5 rounded-lg shrink-0 text-slate-400">
                            <FiPaperclip size={16} />
                          </div>
                          <span className="text-sm font-bold text-slate-300 truncate group-hover:text-white transition-colors">{file.filename}</span>
                        </div>
                        <div className="w-8 h-8 flex items-center justify-center rounded-full bg-white/5 text-slate-400 group-hover:text-white group-hover:bg-white/10 shrink-0 ml-2 transition-colors">
                          <FiDownload size={14} />
                        </div>
                      </a>
                    ))}
                  </div>
                </div>
              )}
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-center px-6">
              <div className="w-20 h-20 bg-[#121212] border border-white/5 text-slate-500 rounded-full flex items-center justify-center mb-6 shadow-sm">
                <FiMessageSquare size={32} />
              </div>
              <h3 className="text-white text-lg font-bold mb-2">No email selected</h3>
              <p className="text-slate-500 font-medium text-sm">Select an email from the list to read.</p>
            </div>
          )}
        </Panel>

        <PanelResizeHandle className="w-4 relative flex justify-center items-center group cursor-col-resize z-20 outline-none hover:bg-white/5 transition-colors">
          <div className="w-[1px] h-full bg-white/5 group-hover:bg-white/30 transition-colors" />
        </PanelResizeHandle>

        <Panel id="composer-panel" order={3} defaultSize={30} minSize={25} className="bg-[#121212] flex flex-col h-full border-l border-white/5">
          {activeTicket ? (
            <>
              <div className="p-5 border-b border-white/5 bg-[#121212] flex justify-between items-center shrink-0">
                <div className="flex items-center gap-2 text-sm font-bold text-white">
                  <FiCornerUpLeft size={18} className="text-slate-500" /> Reply
                </div>
                <div className="flex items-center gap-2">
                  <div className="bg-[#0a0a0a] rounded-xl p-1 flex">
                    <button 
                      onClick={() => setIsPreviewMode(false)}
                      title="Edit"
                      className={`p-2 rounded-lg font-bold transition-all text-sm ${!isPreviewMode ? 'bg-[#1a1a1a] text-white shadow-sm border border-white/10' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                      Write
                    </button>
                    <button 
                      onClick={() => setIsPreviewMode(true)}
                      title="Preview"
                      className={`p-2 rounded-lg font-bold transition-all text-sm ${isPreviewMode ? 'bg-[#1a1a1a] text-white shadow-sm border border-white/10' : 'text-slate-500 hover:text-slate-300'}`}
                    >
                      Preview
                    </button>
                  </div>
                  
                  <div className="relative ml-2">
                    <button 
                      onClick={handleGenerateReply} 
                      disabled={isGenerating || activeTicket.isLoadingDetails}
                      className={`relative flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm font-bold transition-all border shadow-sm ${
                        isGenerating 
                          ? 'bg-[#1a1a1a] text-slate-500 border-white/5 cursor-not-allowed' 
                          : 'bg-white text-black border-transparent hover:bg-slate-200 active:scale-95'
                      } ${(activeTicket.isLoadingDetails) ? 'opacity-50 cursor-not-allowed' : ''}`}
                    >
                      {isGenerating ? <FiLoader className="animate-spin text-slate-500" size={14} /> : <FiZap size={14} className="text-black" />}
                      <span className="relative z-10">{isGenerating ? 'Drafting...' : 'AI Draft'}</span>
                    </button>
                  </div>
                </div>
              </div>

              <div className="flex-1 flex flex-col relative min-h-0 bg-[#0a0a0a] m-4 rounded-2xl border border-white/5 shadow-sm overflow-hidden">
                {isPreviewMode ? (
                  <div className="custom-scrollbar flex-1 p-6 overflow-y-auto prose prose-invert prose-slate prose-sm max-w-none prose-p:leading-loose prose-a:text-white">
                    <ReactMarkdown>{replyText || '*No text to preview.*'}</ReactMarkdown>
                  </div>
                ) : (
                  <textarea
                    className="custom-scrollbar flex-1 w-full bg-transparent border-none p-6 text-slate-200 text-[15px] leading-loose resize-none focus:outline-none placeholder:text-slate-600 font-medium"
                    value={replyText} 
                    onChange={(e) => setReplyText(e.target.value)} 
                    placeholder="Write your reply..."
                  />
                )}
                
                <div className="p-4 flex justify-between items-center shrink-0 border-t border-white/5 bg-[#0a0a0a]">
                  <div className="text-xs font-semibold text-slate-600">
                    Markdown supported
                  </div>
                  <button 
                    disabled={!replyText.trim() || isSending} 
                    onClick={handleSendReply}
                    className={`flex items-center gap-2 px-6 py-2.5 rounded-xl font-bold text-sm transition-all ${
                      replyText.trim() && !isSending 
                        ? 'bg-white hover:bg-slate-200 text-[#0a0a0a] shadow-md active:scale-95' 
                        : 'bg-white/5 text-slate-600 cursor-not-allowed'
                    }`}
                  >
                    {isSending ? <FiLoader className="animate-spin" size={16} /> : <>Send <FiSend size={16} /></>}
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center text-slate-500 px-6 text-center">
              <div className="w-16 h-16 bg-[#1a1a1a] shadow-sm border border-white/5 text-slate-600 rounded-full flex items-center justify-center mb-4">
                <FiEdit2 size={24} />
              </div>
              <h3 className="text-white font-bold mb-1">Composer</h3>
              <p className="font-medium text-sm">Select an email to reply.</p>
            </div>
          )}
        </Panel>
      </PanelGroup>
    </div>
  );
}