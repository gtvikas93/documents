import React, { useState, useEffect } from 'react';
import './App.css';
import { sendMessage, ChatMessage, initializeSession, signOut } from './services/chatService';

function App() {
  const [isChatOpen, setIsChatOpen] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);

  useEffect(() => {
    const initSession = async () => {
      try {
        await initializeSession();
      } catch (error) {
        console.error('Failed to initialize chat session:', error);
      }
    };
    initSession();
  }, []);

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    // Add user message
    const userMessage: ChatMessage = {
      message: inputMessage,
      sender: 'user',
      timestamp: new Date().toISOString(),
    };
    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      // Send message to backend and get response
      const botResponse = await sendMessage(inputMessage);
      setMessages(prev => [...prev, botResponse]);
    } catch (error) {
      console.error('Error in chat:', error);
      const errorMessage: ChatMessage = {
        message: 'Sorry, I encountered an error. Please try again later.',
        sender: 'bot',
        timestamp: new Date().toISOString(),
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSignOut = async () => {
    try {
      await signOut();
      setMessages([]);
      // Reinitialize session
      await initializeSession();
    } catch (error) {
      console.error('Error signing out:', error);
    }
  };

  return (
    <div className="app">
      <header className="app-header">
        <div className="header-left">
          <h1>Wells Fargo Online Banking</h1>
        </div>
        <button className="sign-out-button" onClick={handleSignOut}>
          Sign Out
        </button>
      </header>

      <main className="app-main">
        <div className="dashboard">
          <div className="balance-card">
            <h2>Available Balance</h2>
            <div className="balance-amount">$12,345.67</div>
            <div className="balance-details">
              <div className="detail-item">
                <span>Checking Account</span>
                <span>$8,765.43</span>
              </div>
              <div className="detail-item">
                <span>Savings Account</span>
                <span>$3,580.24</span>
              </div>
            </div>
          </div>

          <div className="recent-transactions">
            <h2>Recent Transactions</h2>
            <div className="transaction-list">
              <div className="transaction-item">
                <div className="transaction-info">
                  <div className="transaction-title">Grocery Store</div>
                  <div className="transaction-date">Today, 2:30 PM</div>
                </div>
                <div className="transaction-amount" data-prefix="-$">45.67</div>
              </div>
              <div className="transaction-item">
                <div className="transaction-info">
                  <div className="transaction-title">Salary Deposit</div>
                  <div className="transaction-date">Yesterday, 9:00 AM</div>
                </div>
                <div className="transaction-amount" data-prefix="+$">3,500.00</div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <div className={`chat-container ${isChatOpen ? 'open' : ''}`}>
        <div className="chat-header" onClick={() => setIsChatOpen(!isChatOpen)}>
          <h3>Chat Support</h3>
          <button className="toggle-chat">
            {isChatOpen ? '▼' : '▲'}
          </button>
        </div>
        
        {isChatOpen && (
          <div className="chat-content">
            <div className="messages">
              {messages.map((message, index) => (
                <div key={index} className={`message ${message.sender}`}>
                  <div className="message-content">{message.message}</div>
                  <div className="message-timestamp">
                    {new Date(message.timestamp).toLocaleTimeString()}
                  </div>
                </div>
              ))}
              {isLoading && (
                <div className="message bot">
                  <div className="message-content">Typing...</div>
                </div>
              )}
            </div>
            <div className="chat-input">
              <input
                type="text"
                value={inputMessage}
                onChange={(e) => setInputMessage(e.target.value)}
                placeholder="Type your message..."
                onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                disabled={isLoading}
              />
              <button 
                onClick={handleSendMessage}
                disabled={isLoading}
              >
                Send
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
