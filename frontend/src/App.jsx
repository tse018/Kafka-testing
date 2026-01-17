import { useState, useEffect } from 'react'
import './App.css'

function App() {
  const [messages, setMessages] = useState([])
  const [inputValue, setInputValue] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [stats, setStats] = useState({ total: 0, sent: 0, consumed: 0 })

  const API_URL = 'http://localhost:8080/api/messages'

  useEffect(() => {
    fetchMessages()
    const interval = setInterval(fetchMessages, 2000)
    return () => clearInterval(interval)
  }, [])

  const fetchMessages = async () => {
    try {
      const response = await fetch(`${API_URL}`)
      const data = await response.json()
      if (data.success) {
        setMessages(data.data || [])
      }
    } catch (err) {
      console.error('Error fetching messages:', err)
    }
  }

  const sendMessage = async (e) => {
    e.preventDefault()
    if (!inputValue.trim()) {
      setError('Please enter a message')
      return
    }

    setLoading(true)
    setError(null)

    try {
      const response = await fetch(`${API_URL}/send?message=${encodeURIComponent(inputValue)}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
      })
      const data = await response.json()

      if (data.success) {
        setInputValue('')
        fetchMessages()
      } else {
        setError(data.message || 'Failed to send message')
      }
    } catch (err) {
      setError('Error sending message: ' + err.message)
      console.error('Error:', err)
    } finally {
      setLoading(false)
    }
  }

  const clearMessages = async () => {
    if (!window.confirm('Are you sure you want to clear all messages?')) return

    try {
      const response = await fetch(API_URL, { method: 'DELETE' })
      const data = await response.json()
      if (data.success) {
        setMessages([])
      }
    } catch (err) {
      setError('Error clearing messages: ' + err.message)
    }
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Kafka Message Hub</h1>
        <p className="subtitle">Real-time messaging with Spring Boot & Kafka</p>
      </header>

      <div className="container">
        <div className="form-section">
          <form onSubmit={sendMessage} className="message-form">
            <div className="form-group">
              <label htmlFor="message">Send a Message</label>
              <div className="input-wrapper">
                <input
                  id="message"
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder="Type your message here..."
                  disabled={loading}
                  className="message-input"
                />
                <button
                  type="submit"
                  disabled={loading}
                  className={`submit-btn ${loading ? 'loading' : ''}`}
                >
                  {loading ? 'Sending...' : 'Send'}
                </button>
              </div>
            </div>
          </form>

          {error && <div className="error-message">{error}</div>}
        </div>

        <div className="stats-section">
          <div className="stats-card">
            <h3>Total Messages</h3>
            <p className="stats-number">{messages.length}</p>
          </div>
          <div className="stats-card">
            <h3>Status</h3>
            <p className="stats-status">Connected</p>
          </div>
        </div>

        <div className="messages-section">
          <div className="messages-header">
            <h2>Messages ({messages.length})</h2>
            {messages.length > 0 && (
              <button onClick={clearMessages} className="clear-btn">
                Clear All
              </button>
            )}
          </div>

          <div className="messages-list">
            {messages.length === 0 ? (
              <div className="no-messages">
                <p>No messages yet. Send one to get started!</p>
              </div>
            ) : (
              messages.map((msg) => (
                <div key={msg.id} className="message-item">
                  <div className="message-header">
                    <span className="message-id">{msg.id.substring(0, 8)}...</span>
                    <span className="message-status">{msg.status}</span>
                  </div>
                  <div className="message-content">{msg.content}</div>
                  <div className="message-footer">
                    <span className="message-time">
                      {new Date(msg.timestamp).toLocaleTimeString()}
                    </span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default App

