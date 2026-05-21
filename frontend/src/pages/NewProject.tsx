import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchApi } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { Card } from '../components/ui/Card';
import { ArrowLeft } from 'lucide-react';

export const NewProject: React.FC = () => {
  const [name, setName] = useState('');
  const [githubRepoUrl, setGithubRepoUrl] = useState('');
  const [subdomain, setSubdomain] = useState('');
  const [branch, setBranch] = useState('main');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');
  const navigate = useNavigate();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError('');

    try {
      const project = await fetchApi('/projects', {
        method: 'POST',
        body: JSON.stringify({ name, githubRepoUrl, subdomain, branch: branch || 'main' }),
      });
      navigate(`/projects/${project.id}`);
    } catch (err: any) {
      setError(err.message || 'Failed to create project');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="dashboard">
      <header className="header glass">
        <div className="container header-content">
          <div className="logo">
            <span className="logo-icon">🚀</span>
            LiftOff
          </div>
        </div>
      </header>

      <main className="container main-content">
        <div className="section-header">
          <Button variant="ghost" onClick={() => navigate('/dashboard')} style={{ paddingLeft: 0 }}>
            <ArrowLeft size={16} /> Back to Projects
          </Button>
        </div>

        <div style={{ maxWidth: 600, margin: '0 auto' }}>
        <Card>
          <h2 style={{ marginBottom: 24 }}>Import Git Repository</h2>
          
          {error && <div className="auth-error">{error}</div>}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Input 
              label="Project Name" 
              placeholder="My Awesome App"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required 
            />
            <Input 
              label="GitHub Repository URL" 
              placeholder="https://github.com/username/repo.git"
              value={githubRepoUrl}
              onChange={(e) => setGithubRepoUrl(e.target.value)}
              required 
            />
            <Input 
              label="Branch" 
              placeholder="main"
              value={branch}
              onChange={(e) => setBranch(e.target.value)}
            />
            <Input 
              label="Subdomain (must be unique)" 
              placeholder="my-awesome-app"
              value={subdomain}
              onChange={(e) => setSubdomain(e.target.value.toLowerCase().replace(/[^a-z0-9-]/g, ''))}
              required 
            />
            
            <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end' }}>
              <Button type="submit" isLoading={isLoading}>
                Create &amp; Deploy
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </main>
  </div>
  );
};
