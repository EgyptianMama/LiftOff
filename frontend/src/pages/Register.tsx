import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchApi } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card } from '../components/ui/Card';
import './Auth.css';

export const Register: React.FC = () => {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const { login } = useAuth();
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const data = await fetchApi('/auth/register', {
        method: 'POST',
        body: JSON.stringify({ email, username, password }),
      });
      // Backend returns TokenResponse on registration — auto-login
      login(data.accessToken);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Failed to register');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <Card className="auth-card glass">
        <h1 className="auth-title">Create your account</h1>
        <p className="auth-subtitle">Join LiftOff today</p>
        
        {error && <div className="auth-error">{error}</div>}

        <form onSubmit={handleSubmit} className="auth-form">
          <Input 
            label="Email Address" 
            type="email" 
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            required 
          />
          <Input 
            label="Username" 
            type="text" 
            placeholder="Choose a username (3-50 chars)"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required 
          />
          <Input 
            label="Password" 
            type="password" 
            placeholder="At least 8 characters"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required 
          />
          
          <Button type="submit" isLoading={isLoading} className="auth-submit">
            Sign Up
          </Button>
        </form>
        
        <div className="auth-footer">
          Already have an account? <Link to="/login">Log In</Link>
        </div>
      </Card>
    </div>
  );
};
