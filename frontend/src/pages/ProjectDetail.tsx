import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { fetchApi, getAuthToken } from '../lib/api';
import { Button } from '../components/ui/Button';
import { Card } from '../components/ui/Card';
import { ArrowLeft, ExternalLink, Copy } from 'lucide-react';
import { fetchEventSource } from '@microsoft/fetch-event-source';
import './ProjectDetail.css';

interface Project {
  id: string;
  name: string;
  slug: string;
  githubRepoUrl: string;
  githubWebhookSecret: string;
  branch: string;
  subdomain: string;
  customDomain: string | null;
  createdAt: string;
}

interface Build {
  id: string;
  status: string;
  commitHash: string;
  startedAt: string;
  finishedAt: string | null;
}

export const ProjectDetail: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const [project, setProject] = useState<Project | null>(null);
  const [builds, setBuilds] = useState<Build[]>([]);
  const [logs, setLogs] = useState<string[]>([]);
  const [copied, setCopied] = useState<string | null>(null);
  const [isDeploying, setIsDeploying] = useState(false);
  const logsEndRef = useRef<HTMLDivElement>(null);
  const sseControllerRef = useRef<AbortController | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    loadProject();
    loadBuilds();
  }, [id]);

  useEffect(() => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [logs]);

  useEffect(() => {
    if (builds.length > 0) {
      const latestBuild = builds[0];
      // Start streaming logs if it's active
      if (['QUEUED', 'STARTING', 'RUNNING'].includes(latestBuild.status)) {
        startLogStream(latestBuild.id);
      } else {
        // If it's SUCCESS or FAILED, we might just show a static message or fetch historic logs.
        // For MVP, we'll just clear the active stream if it's not running.
        if (sseControllerRef.current) {
          sseControllerRef.current.abort();
          sseControllerRef.current = null;
        }
        if (latestBuild.status === 'SUCCESS') setLogs(['Build completed successfully.']);
        else if (latestBuild.status === 'FAILED') setLogs(['Build failed.']);
      }
    }
    
    return () => {
      if (sseControllerRef.current) {
        sseControllerRef.current.abort();
      }
    };
  }, [builds]);

  const loadProject = async () => {
    try {
      const data = await fetchApi(`/projects/${id}`);
      setProject(data);
    } catch (err) {
      console.error(err);
    }
  };

  const loadBuilds = async () => {
    try {
      const data = await fetchApi(`/projects/${id}/builds`);
      setBuilds(data);
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeploy = async () => {
    setIsDeploying(true);
    setLogs(['Triggering manual deployment...']);
    try {
      await fetchApi(`/projects/${id}/deploy`, { method: 'POST' });
      // Refresh builds, which will trigger the SSE stream in useEffect
      await loadBuilds();
    } catch (err) {
      console.error(err);
      setLogs(['Failed to trigger deployment.']);
    } finally {
      setIsDeploying(false);
    }
  };

  const startLogStream = (buildId: string) => {
    if (sseControllerRef.current) {
      sseControllerRef.current.abort();
    }
    sseControllerRef.current = new AbortController();
    setLogs([]); // clear logs for new stream

    const token = getAuthToken();
    
    fetchEventSource(`/api/builds/${buildId}/logs/stream?token=${token}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      signal: sseControllerRef.current.signal,
      onmessage(msg) {
        if (msg.data) {
          setLogs(prev => [...prev, msg.data]);
        }
      },
      onclose() {
        // SSE closed
        loadBuilds(); // reload to get updated status
      },
      onerror(err) {
        console.error('SSE Error:', err);
        sseControllerRef.current?.abort();
        throw err; // throw to stop retrying
      }
    });
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    setCopied(label);
    setTimeout(() => setCopied(null), 2000);
  };

  if (!project) {
    return <div className="loading-state">Loading project...</div>;
  }

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
        <div className="section-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Button variant="ghost" onClick={() => navigate('/dashboard')} style={{ paddingLeft: 0 }}>
            <ArrowLeft size={16} /> Back to Projects
          </Button>
          <Button 
            onClick={handleDeploy} 
            disabled={isDeploying || (builds.length > 0 && ['QUEUED', 'STARTING', 'RUNNING'].includes(builds[0].status))}
          >
            Trigger Build
          </Button>
        </div>

        <div className="project-detail-grid">
          <div className="project-info-column">
            <h1 className="detail-title">{project.name}</h1>
            
            <Card className="info-card">
              <h3>Deployment</h3>
              <div className="info-row">
                <span className="info-label">Domain</span>
                <a 
                  href={`http://${project.subdomain}.localhost`} 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="info-value link"
                >
                  {project.subdomain}.localhost <ExternalLink size={12} />
                </a>
              </div>
              {project.customDomain && (
                <div className="info-row">
                  <span className="info-label">Custom Domain</span>
                  <span className="info-value">{project.customDomain}</span>
                </div>
              )}
              <div className="info-row">
                <span className="info-label">Branch</span>
                <span className="info-value code">{project.branch}</span>
              </div>
            </Card>

            <Card className="info-card">
              <h3>Git Integration</h3>
              <div className="info-row">
                <span className="info-label">Repository</span>
                <a href={project.githubRepoUrl} target="_blank" rel="noopener noreferrer" className="info-value link">
                  {project.githubRepoUrl.replace('https://github.com/', '').replace('.git', '')} <ExternalLink size={12} />
                </a>
              </div>
              <div className="info-row">
                <span className="info-label">Webhook URL</span>
                <span 
                  className="info-value code secret" 
                  onClick={() => copyToClipboard('http://localhost:8080/api/webhook/github', 'webhook')}
                >
                  http://localhost:8080/api/webhook/github <Copy size={12} />
                  {copied === 'webhook' && <span className="copied-badge">Copied!</span>}
                </span>
              </div>
              <div className="info-row">
                <span className="info-label">Webhook Secret</span>
                <span 
                  className="info-value code secret" 
                  onClick={() => copyToClipboard(project.githubWebhookSecret, 'secret')}
                >
                  {project.githubWebhookSecret} <Copy size={12} />
                  {copied === 'secret' && <span className="copied-badge">Copied!</span>}
                </span>
              </div>
            </Card>
          </div>

          <div className="project-logs-column">
            <Card className="logs-card">
              <div className="logs-header">
                <h3>Build Logs</h3>
                <span className="logs-hint">Logs will appear here during active builds</span>
              </div>
              <div className="logs-terminal">
                {builds.length === 0 ? (
                  <div className="empty-logs">
                    <p>No active build.</p>
                    <p className="empty-logs-hint">Push to your GitHub repo or click Trigger Build.</p>
                  </div>
                ) : (
                  logs.map((log, i) => (
                    <div key={i} className="log-line">{log}</div>
                  ))
                )}
                <div ref={logsEndRef} />
              </div>
            </Card>
          </div>
        </div>
      </main>
    </div>
  );
};
