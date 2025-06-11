import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:3001/api';
const SESSION_COOKIE_NAME = 'chat_session_id';

export interface ChatMessage {
  message: string;
  sender: 'user' | 'bot';
  timestamp: string;
  sessionId?: string;
}

let currentSessionId: string | null = null;

// Helper function to manage cookies
const cookieManager = {
  setCookie: (name: string, value: string, days: number) => {
    const expires = new Date();
    expires.setTime(expires.getTime() + days * 24 * 60 * 60 * 1000);
    document.cookie = `${name}=${value};expires=${expires.toUTCString()};path=/`;
  },
  getCookie: (name: string): string | null => {
    const nameEQ = name + "=";
    const ca = document.cookie.split(';');
    for (let i = 0; i < ca.length; i++) {
      let c = ca[i];
      while (c.charAt(0) === ' ') c = c.substring(1, c.length);
      if (c.indexOf(nameEQ) === 0) return c.substring(nameEQ.length, c.length);
    }
    return null;
  },
  deleteCookie: (name: string) => {
    document.cookie = `${name}=;expires=Thu, 01 Jan 1970 00:00:00 GMT;path=/`;
  }
};

export const initializeSession = async (): Promise<string> => {
  try {
    // Check if we have a session ID in cookies
    const existingSessionId = cookieManager.getCookie(SESSION_COOKIE_NAME);
    if (existingSessionId) {
      currentSessionId = existingSessionId;
      return existingSessionId;
    }

    const response = await axios.post(`${API_URL}/session`);
    const newSessionId = response.data.sessionId;
    if (!newSessionId) {
      throw new Error('Failed to initialize session');
    }
    
    currentSessionId = newSessionId;
    // Store session ID in cookie for 30 days
    cookieManager.setCookie(SESSION_COOKIE_NAME, newSessionId, 30);
    
    return newSessionId;
  } catch (error) {
    console.error('Error initializing session:', error);
    throw error;
  }
};

export const sendMessage = async (message: string): Promise<ChatMessage> => {
  try {
    // Check if we have a session ID in cookies
    if (!currentSessionId) {
      const cookieSessionId = cookieManager.getCookie(SESSION_COOKIE_NAME);
      if (cookieSessionId) {
        currentSessionId = cookieSessionId;
      }
    }

    // If still no session ID, initialize a new session
    if (!currentSessionId) {
      currentSessionId = await initializeSession();
    }

    if (!currentSessionId) {
      throw new Error('Failed to get or create session');
    }

    const response = await axios.post(
      `${API_URL}/message`,
      { message },
      {
        headers: {
          'X-Session-ID': currentSessionId
        }
      }
    );

    return {
      message: response.data.message,
      sender: 'bot',
      timestamp: response.data.timestamp,
      sessionId: currentSessionId
    };
  } catch (error) {
    console.error('Error sending message:', error);
    throw error;
  }
};

export const signOut = async (): Promise<void> => {
  try {
    if (currentSessionId) {
      await axios.get(`${API_URL}/session/invalidate/${currentSessionId}`);
      currentSessionId = null;
      cookieManager.deleteCookie(SESSION_COOKIE_NAME);
    }
  } catch (error) {
    console.error('Error signing out:', error);
    throw error;
  }
};

// Function to clear the session (useful for logout or session reset)
export const clearSession = () => {
  currentSessionId = null;
  cookieManager.deleteCookie(SESSION_COOKIE_NAME);
}; 