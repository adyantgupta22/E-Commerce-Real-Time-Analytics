import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
  const [analytics, setAnalytics] = useState(null);
  const [lastUpdated, setLastUpdated] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 20;

  useEffect(() => {
    const fetchData = async () => {
      try {
        setError(null);
        const response = await fetch('/api/analytics');
        if (!response.ok) throw new Error('Failed to fetch');
        const data = await response.json();
        setAnalytics(data);
        setLastUpdated(new Date());
        setLoading(false);
      } catch (err) {
        setError('Unable to connect to analytics service');
        console.error("Error fetching analytics:", err);
      }
    };

    fetchData();
    const interval = setInterval(fetchData, 30000);
    return () => clearInterval(interval);
  }, []);

  const formatTime = (date) => {
    if (!date) return '';
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  };

  // Pagination logic for sessions
  const getSessionsPage = () => {
    if (!analytics?.activeSessions) return { items: [], totalPages: 0, totalItems: 0 };
    
    const sortedSessions = Object.entries(analytics.activeSessions)
      .sort(([,a], [,b]) => b - a);
    
    const totalItems = sortedSessions.length;
    const totalPages = Math.ceil(totalItems / ITEMS_PER_PAGE);
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const items = sortedSessions.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    
    return { items, totalPages, totalItems };
  };

  const { items: sessionItems, totalPages, totalItems } = getSessionsPage();

  if (loading && !analytics) {
    return (
      <div className="app">
        <div className="loading-container">
          <div className="spinner"></div>
          <p>Loading analytics...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="header">
        <div className="header-content">
          <h1>📊 Real-time Analytics Dashboard</h1>
          <p className="subtitle">E-commerce User Behavior Tracking</p>
        </div>
        <div className="header-meta">
          {lastUpdated && (
            <span className="last-updated">
              Last updated: {formatTime(lastUpdated)}
            </span>
          )}
          <span className="refresh-info">Auto-refresh: 30s</span>
        </div>
      </header>

      {error && (
        <div className="error-banner">
          ⚠️ {error}
        </div>
      )}

      <main className="main-content">
        <div className="metrics-grid">
          {/* Active Users Card */}
          <div className="metric-card active-users">
            <div className="card-header">
              <span className="card-icon">👥</span>
              <h2>Active Users</h2>
              <span className="time-window">Last 5 minutes</span>
            </div>
            <div className="card-body">
              <div className="big-number">{analytics?.activeUsers || 0}</div>
              <p className="metric-label">unique users</p>
            </div>
          </div>

          {/* Top Pages Card */}
          <div className="metric-card top-pages">
            <div className="card-header">
              <span className="card-icon">📄</span>
              <h2>Top 5 Pages</h2>
              <span className="time-window">Last 15 minutes</span>
            </div>
            <div className="card-body">
              {analytics?.topPages && analytics.topPages.length > 0 ? (
                <div className="pages-list">
                  {analytics.topPages.map((page, index) => (
                    <div key={page.url} className="page-item">
                      <span className="rank">#{index + 1}</span>
                      <span className="page-url">{page.url}</span>
                      <span className="page-views">{page.views} views</span>
                      <div 
                        className="progress-bar" 
                        style={{ 
                          width: `${(page.views / analytics.topPages[0].views) * 100}%` 
                        }}
                      />
                    </div>
                  ))}
                </div>
              ) : (
                <p className="no-data">No page views yet</p>
              )}
            </div>
          </div>

          {/* Active Sessions Card */}
          <div className="metric-card active-sessions">
            <div className="card-header">
              <span className="card-icon">🔗</span>
              <h2>Active Sessions per User</h2>
              <span className="time-window">Last 5 minutes</span>
            </div>
            <div className="card-body">
              {totalItems > 0 ? (
                <>
                  <div className="sessions-summary">
                    <span className="total-users">{totalItems} users</span>
                    <span className="total-sessions">
                      {Object.values(analytics.activeSessions).reduce((a, b) => a + b, 0)} total sessions
                    </span>
                  </div>
                  <div className="sessions-grid">
                    {sessionItems.map(([userId, sessionCount]) => (
                      <div key={userId} className="session-item">
                        <span className="user-id">{userId}</span>
                        <span className={`session-count ${sessionCount > 5 ? 'high' : ''}`}>
                          {sessionCount}
                        </span>
                      </div>
                    ))}
                  </div>
                  
                  {/* Pagination Controls */}
                  {totalPages > 1 && (
                    <div className="pagination">
                      <button 
                        className="pagination-btn"
                        onClick={() => setCurrentPage(1)}
                        disabled={currentPage === 1}
                      >
                        ⟪
                      </button>
                      <button 
                        className="pagination-btn"
                        onClick={() => setCurrentPage(p => Math.max(1, p - 1))}
                        disabled={currentPage === 1}
                      >
                        ◀
                      </button>
                      <span className="pagination-info">
                        Page {currentPage} of {totalPages}
                      </span>
                      <button 
                        className="pagination-btn"
                        onClick={() => setCurrentPage(p => Math.min(totalPages, p + 1))}
                        disabled={currentPage === totalPages}
                      >
                        ▶
                      </button>
                      <button 
                        className="pagination-btn"
                        onClick={() => setCurrentPage(totalPages)}
                        disabled={currentPage === totalPages}
                      >
                        ⟫
                      </button>
                    </div>
                  )}
                </>
              ) : (
                <p className="no-data">No active sessions</p>
              )}
            </div>
          </div>
        </div>
      </main>

      <footer className="footer">
        <p>E-commerce Analytics Platform • Rolling metrics powered by Redis</p>
      </footer>
    </div>
  );
}

export default App;
