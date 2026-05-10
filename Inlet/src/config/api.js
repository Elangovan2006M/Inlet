const TRAINING_BASE_URL = import.meta.env.VITE_TRAINING_BASE_URL || "http://localhost:8080/api/v1/training";
const TICKETS_BASE_URL = import.meta.env.VITE_TICKETS_BASE_URL || "http://localhost:8080/api/v1/tickets";

export const API_ENDPOINTS = {
  // --- KNOWLEDGE BASE ENDPOINTS ---
  HISTORY: (uid) => `${TRAINING_BASE_URL}/history?uid=${uid}`,
  CHAT: `${TRAINING_BASE_URL}/chat`,
  UPLOAD_PDF: `${TRAINING_BASE_URL}/upload-pdf`,
  DELETE_FILE: (fileName, uid) => `${TRAINING_BASE_URL}/file/${fileName}?uid=${uid}`,
  CLEAR_MEMORY: (uid) => `${TRAINING_BASE_URL}/clear?uid=${uid}`,

  // --- INBOX / TICKETS ENDPOINTS ---
  SCAN_INBOX: `${TICKETS_BASE_URL}/scan`,
  GENERATE_REPLY: `${TICKETS_BASE_URL}/generate-reply`,
  PROCESS_ALL: `${TICKETS_BASE_URL}/process`,
  MARK_READ: (id) => `${TICKETS_BASE_URL}/mark-read/${id}`, 
  TICKET_DETAILS: (id) => `${TICKETS_BASE_URL}/${id}/details`,
  SEND_REPLY: `${TICKETS_BASE_URL}/send-reply`,
};