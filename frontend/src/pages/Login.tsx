import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { fetchApi } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card } from '../components/ui/Card';
import './Auth.css';

export const Login: React.FC = () => {
  const [email, setEmail] = useState('');
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
      const data = await fetchApi('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      });
      login(data.accessToken);
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Failed to login');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <Card className="auth-card glass">
        <h1 className="auth-title">Welcome back</h1>
        <p className="auth-subtitle">Log in to your LiftOff account</p>
        
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
            label="Password" 
            type="password" 
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required 
          />
          
          <Button type="submit" isLoading={isLoading} className="auth-submit">
            Log In
          </Button>
        </form>
        
        <div className="auth-footer">
          Don't have an account? <Link to="/register">Sign Up</Link>
        </div>
      </Card>
    </div>
  );
};
