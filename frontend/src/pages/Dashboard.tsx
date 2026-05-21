import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchApi } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { useAuth } from '../context/AuthContext';
import { Plus, ExternalLink, LogOut } from 'lucide-react';
import './Dashboard.css';

interface Project {
  id: string;
  name: string;
  slug: string;
  githubRepoUrl: string;
  subdomain: string;
  branch: string;
  createdAt: string;
}

export const Dashboard: React.FC = () => {
  const [projects, setProjects] = useState<Project[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const navigate = useNavigate();
  const { logout } = useAuth();

  useEffect(() => {
    const loadProjects = async () => {
      try {
        const data = await fetchApi('/projects');
        setProjects(data);
      } catch (err) {
        console.error('Failed to load projects', err);
      } finally {
        setIsLoading(false);
      }
    };
    loadProjects();
  }, []);

  return (
    <div className="dashboard">
      <header className="header glass">
        <div className="container header-content">
          <div className="logo">
            <span className="logo-icon">🚀</span>
            LiftOff
          </div>
          <div className="header-actions">
            <Button variant="ghost" onClick={logout}><LogOut size={16} /> Logout</Button>
          </div>
        </div>
      </header>

      <main className="container main-content">
        <div className="section-header">
          <h2>Projects</h2>
          <Button onClick={() => navigate('/projects/new')}>
            <Plus size={16} /> Create New Project
          </Button>
        </div>

        {isLoading ? (
          <div className="loading-state">Loading projects...</div>
        ) : projects.length === 0 ? (
          <Card className="empty-state">
            <p>You don't have any projects yet.</p>
            <Button onClick={() => navigate('/projects/new')} style={{ marginTop: 16 }}>
              Deploy your first project
            </Button>
          </Card>
        ) : (
          <div className="project-grid">
            {projects.map((project) => (
              <Card 
                key={project.id} 
                className="project-card" 
                onClick={() => navigate(`/projects/${project.id}`)}
              >
                <div className="project-card-header">
                  <h3 className="project-name">{project.name}</h3>
                  <span className="branch-tag">{project.branch || 'main'}</span>
                </div>
                <div className="project-url">
                  <a 
                    href={`http://${project.subdomain}.localhost`} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    onClick={(e) => e.stopPropagation()}
                  >
                    {project.subdomain}.localhost <ExternalLink size={12} />
                  </a>
                </div>
                <div className="project-meta">
                  <span>{project.githubRepoUrl.replace('https://github.com/', '').replace('.git', '')}</span>
                  <span>{new Date(project.createdAt).toLocaleDateString()}</span>
                </div>
              </Card>
            ))}
          </div>
        )}
      </main>
    </div>
  );
};
