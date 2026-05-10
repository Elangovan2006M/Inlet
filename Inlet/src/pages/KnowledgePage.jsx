import React, { useState, useRef, useEffect } from 'react';
import { Panel, Group as PanelGroup, Separator as PanelResizeHandle } from 'react-resizable-panels';
import { toast } from 'react-hot-toast';
import ChatArea from '../components/ChatArea';
import HistorySidebar from '../components/HistorySidebar';
import { API_ENDPOINTS } from '../config/api';

export default function KnowledgePage() {
  const userId = localStorage.getItem('userId') || 'guest';

  const [messages, setMessages] = useState(() => {
    const saved = localStorage.getItem(`inlet_chat_${userId}`);
    return saved ? JSON.parse(saved) : [{ 
        role: 'ai', 
        text: `Knowledge Manager active for user: ${userId}. Ask questions or upload PDFs.` 
    }];
  });

  const [history, setHistory] = useState([]);
  const [inputText, setInputText] = useState("");
  const [attachedFile, setAttachedFile] = useState(null);
  const [isTyping, setIsTyping] = useState(false);
  const [activePreview, setActivePreview] = useState(null);

  const chatScrollRef = useRef(null); 

  const scrollToBottom = () => {
    setTimeout(() => {
      if (chatScrollRef.current) {
        chatScrollRef.current.scrollTo({
          top: chatScrollRef.current.scrollHeight,
          behavior: 'smooth'
        });
      }
    }, 50);
  };
  
  useEffect(() => {
    localStorage.setItem(`inlet_chat_${userId}`, JSON.stringify(messages));
  }, [messages, userId]);
  
  useEffect(() => scrollToBottom(), [messages, isTyping]);

  const fetchHistory = async () => {
    try {
      const res = await fetch(API_ENDPOINTS.HISTORY(userId));
      const data = await res.json();
      setHistory(data);
    } catch (e) { 
      console.error("Failed to load history", e); 
    }
  };

  useEffect(() => { fetchHistory(); }, [userId]);

  const handleFileAttach = (e) => {
    const file = e.target.files[0];
    if (file?.type === "application/pdf") setAttachedFile(file);
    else toast.error("Strict PDF format only.");
  };

  const handleClearChat = () => {
    if(window.confirm("Clear this chat screen? (Does not delete AI memory)")) {
      setMessages([{ role: 'ai', text: 'Chat history cleared.' }]);
    }
  };

  const handleDeleteFile = async (fileName, e) => {
    e.stopPropagation(); 
    if(!window.confirm(`Delete "${fileName}" from your Knowledge Base?`)) return;
    try {
      await fetch(API_ENDPOINTS.DELETE_FILE(fileName, userId), { method: "DELETE" });
      setMessages(prev => [...prev, { role: 'ai', text: `🗑️ Removed "${fileName}" from memory.` }]);
      setActivePreview(null);
      await fetchHistory(); 
    } catch (error) { 
      toast.error("Failed to connect to backend."); 
    }
  };

  const handleWipeMemory = async () => {
    if(!window.confirm("CRITICAL: Wipe YOUR personal vector memory? This cannot be undone.")) return;
    try {
      await fetch(API_ENDPOINTS.CLEAR_MEMORY(userId), { method: "DELETE" });
      setMessages([{ role: 'ai', text: '🚨 Your Vector Store has been wiped.' }]);
      setActivePreview(null);
      await fetchHistory();
    } catch (error) { 
      toast.error("Failed to connect to backend."); 
    }
  };

  const handleSendMessage = async () => {
    if (!inputText.trim() && !attachedFile) return;
    const currentText = inputText;
    const currentFile = attachedFile;

    setMessages(prev => [...prev, { role: 'user', text: currentText, fileName: currentFile?.name }]);
    setInputText(""); setAttachedFile(null); setIsTyping(true);

    try {
      if (currentFile) {
        const formData = new FormData(); 
        formData.append("file", currentFile);
        formData.append("uid", userId); 
        await fetch(API_ENDPOINTS.UPLOAD_PDF, { method: "POST", body: formData });
        await fetchHistory(); 
      } 

      if (currentText.trim()) {
        const res = await fetch(API_ENDPOINTS.CHAT, {
          method: "POST", 
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ message: currentText, uid: userId })
        });
        const data = await res.json();
        setMessages(prev => [...prev, { role: 'ai', text: data.reply }]);
        
        if (data.reply.includes("SUCCESS")) {
          setTimeout(async () => {
            await fetchHistory();
          }, 1000); 
        } else {
          await fetchHistory(); 
        }
      }
    } catch (e) {
      setMessages(prev => [...prev, { role: 'ai', text: "❌ Connection to AI Server failed." }]);
    } finally { 
      setIsTyping(false); 
    }
  };

  return (
    <div className="w-full h-full overflow-hidden min-h-0 bg-[#0a0a0a]">
      <PanelGroup direction="horizontal" className="h-full w-full">
        
        <Panel id="chat-panel" order={1} defaultSize={70} minSize={40} className="h-full border-r border-white/5">
          <ChatArea 
            messages={messages} isTyping={isTyping}
            inputText={inputText} setInputText={setInputText}
            attachedFile={attachedFile} setAttachedFile={setAttachedFile}
            handleFileAttach={handleFileAttach} handleSendMessage={handleSendMessage}
            handleClearChat={handleClearChat} 
            chatScrollRef={chatScrollRef}
          />
        </Panel>

        <PanelResizeHandle className="w-4 relative flex justify-center items-center group cursor-col-resize z-20 outline-none hover:bg-white/5 transition-colors">
          <div className="w-[1px] h-full bg-white/5 group-hover:bg-white/30 transition-colors" />
        </PanelResizeHandle>

        <Panel id="history-panel" order={2} defaultSize={30} minSize={20} className="h-full">
          <HistorySidebar 
            history={history} activePreview={activePreview} setActivePreview={setActivePreview}
            handleWipeMemory={handleWipeMemory} handleDeleteFile={handleDeleteFile}
          />
        </Panel>

      </PanelGroup>
    </div>
  );
}