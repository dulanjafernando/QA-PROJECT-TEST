import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './Home.css';

const Home = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    // Get user data from localStorage
    const userData = localStorage.getItem('user');
    if (userData) {
      setUser(JSON.parse(userData));
    } else {
      // If no user data, redirect to login
      navigate('/login');
    }
    setLoading(false);
  }, [navigate]);

  const handleLogout = async () => {
    try {
      // Call backend logout endpoint
      const response = await fetch('http://localhost:8080/api/auth/logout', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (response.ok) {
        // Clear user data from localStorage
        localStorage.removeItem('user');
        // Redirect to login
        navigate('/login');
      }
    } catch (error) {
      console.error('Logout error:', error);
      // Even if backend fails, clear local storage and redirect
      localStorage.removeItem('user');
      navigate('/login');
    }
  };

  if (loading) {
    return (
      <div className="loading-container">
        <div className="loading-spinner"></div>
        <p>Loading...</p>
      </div>
    );
  }

  if (!user) {
    return null; // Will redirect to login in useEffect
  }

  return (
    <div className="home-container">
      <header className="home-header">
        <div className="header-content">
          <h1 className="welcome-title">Welcome to QA Project</h1>
          <button onClick={handleLogout} className="logout-button">
            Logout
          </button>
        </div>
      </header>

      <main className="home-main">
        <div className="user-card">
          <div className="user-avatar">
            <span className="avatar-text">
              {user.username ? user.username.charAt(0).toUpperCase() : 'U'}
            </span>
          </div>
          <div className="user-info">
            <h2 className="user-name">Hello, {user.username}!</h2>
            <p className="user-email">{user.email}</p>
            <p className="user-id">User ID: {user.userId}</p>
          </div>
        </div>

        <div className="dashboard-grid">
          <div className="dashboard-card">
            <div className="card-icon">📊</div>
            <h3>Dashboard</h3>
            <p>View your analytics and statistics</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon">⚙️</div>
            <h3>Settings</h3>
            <p>Manage your account preferences</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon">📝</div>
            <h3>Projects</h3>
            <p>Manage your QA projects and tasks</p>
          </div>

          <div className="dashboard-card">
            <div className="card-icon">📈</div>
            <h3>Reports</h3>
            <p>Generate and view detailed reports</p>
          </div>
        </div>

        <div className="activity-section">
          <h3 className="section-title">Recent Activity</h3>
          <div className="activity-list">
            <div className="activity-item">
              <div className="activity-icon">✅</div>
              <div className="activity-content">
                <p className="activity-text">Successfully logged in</p>
                <span className="activity-time">Just now</span>
              </div>
            </div>
            <div className="activity-item">
              <div className="activity-icon">👤</div>
              <div className="activity-content">
                <p className="activity-text">Account created</p>
                <span className="activity-time">Today</span>
              </div>
            </div>
          </div>
        </div>
      </main>

      <footer className="home-footer">
        <p>&copy; 2025 QA Project. All rights reserved.</p>
      </footer>
    </div>
  );
};

export default Home;