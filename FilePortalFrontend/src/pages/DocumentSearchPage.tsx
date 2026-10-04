import React, { useState } from 'react';
import { fileApi } from '../services/api';
import type { DocumentQueryRequest, DocumentQueryResponse, DocumentSource } from '../types';
import { useNavigate } from 'react-router-dom';

export const DocumentSearchPage: React.FC = () => {
  const navigate = useNavigate();
  const [question, setQuestion] = useState('');
  const [loading, setLoading] = useState(false);
  const [response, setResponse] = useState<DocumentQueryResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [searchHistory, setSearchHistory] = useState<{ question: string; answer: string }[]>([]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!question.trim()) return;

    setLoading(true);
    setError(null);

    try {
      const request: DocumentQueryRequest = {
        question: question.trim(),
        topK: 5,
      };
      const result = await fileApi.queryDocuments(request);
      setResponse(result);

      // Add to search history
      setSearchHistory((prev) => [
        { question: question.trim(), answer: result.answer },
        ...prev.slice(0, 9), // Keep last 10 searches
      ]);
    } catch (err: any) {
      if (err.message === 'UNAUTHORIZED') {
        setError('Your session has expired. Please login again.');
      } else if (err.message === 'FORBIDDEN') {
        setError('You do not have permission to search documents.');
      } else {
        setError(err.message || 'Failed to query documents. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  const handleSourceClick = (source: DocumentSource) => {
    // Navigate to file detail page
    // Extract file ID from fileVersionId if needed
    navigate(`/files/${source.fileVersionId}`);
  };

  const clearHistory = () => {
    setSearchHistory([]);
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>🔍 Document Search</h1>
        <p style={styles.subtitle}>
          Ask questions about your documents using AI-powered search
        </p>
      </div>

      {/* Search Form */}
      <div style={styles.searchCard}>
        <form onSubmit={handleSubmit} style={styles.searchForm}>
          <div style={styles.inputWrapper}>
            <input
              type="text"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="Ask anything about your documents..."
              style={styles.searchInput}
              disabled={loading}
            />
            <button
              type="submit"
              style={styles.searchButton}
              disabled={loading || !question.trim()}
            >
              {loading ? (
                <span style={styles.loadingText}>
                  <span style={styles.spinnerInline}></span> Searching...
                </span>
              ) : (
                '🔍 Search'
              )}
            </button>
          </div>
        </form>

        {/* Quick suggestions */}
        <div style={styles.suggestions}>
          <span style={styles.suggestionsLabel}>Try asking:</span>
          <button
            style={styles.suggestionChip}
            onClick={() => setQuestion('What are the main topics in my documents?')}
          >
            Main topics
          </button>
          <button
            style={styles.suggestionChip}
            onClick={() => setQuestion('Summarize the confidential documents')}
          >
            Summarize confidential
          </button>
          <button
            style={styles.suggestionChip}
            onClick={() => setQuestion('What are the key findings?')}
          >
            Key findings
          </button>
        </div>
      </div>

      {/* Error Display */}
      {error && (
        <div style={styles.errorCard}>
          <span style={styles.errorIcon}>❌</span>
          <span>{error}</span>
        </div>
      )}

      {/* Results */}
      {response && (
        <div style={styles.resultsCard}>
          <div style={styles.answerSection}>
            <h2 style={styles.answerTitle}>💡 Answer</h2>
            <p style={styles.answerText}>{response.answer}</p>
          </div>

          {response.sources && response.sources.length > 0 && (
            <div style={styles.sourcesSection}>
              <h3 style={styles.sourcesTitle}>📚 Sources ({response.sources.length})</h3>
              <div style={styles.sourcesList}>
                {response.sources.map((source, index) => (
                  <div
                    key={index}
                    style={styles.sourceCard}
                    onClick={() => handleSourceClick(source)}
                    role="button"
                    tabIndex={0}
                    onKeyDown={(e) => e.key === 'Enter' && handleSourceClick(source)}
                  >
                    <div style={styles.sourceHeader}>
                      <span style={styles.sourceIndex}>#{index + 1}</span>
                      <span style={styles.sourceFilename}>📄 {source.filename}</span>
                      <span style={styles.sourceSimilarity}>
                        {(source.similarity * 100).toFixed(1)}% relevance
                      </span>
                    </div>
                    <blockquote style={styles.sourceQuote}>
                      "{source.chunkContent}"
                    </blockquote>
                    <span style={styles.sourceLink}>Click to view file →</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Search History */}
      {searchHistory.length > 0 && (
        <div style={styles.historyCard}>
          <div style={styles.historyHeader}>
            <h3 style={styles.historyTitle}>🕒 Recent Searches</h3>
            <button onClick={clearHistory} style={styles.clearButton}>
              Clear
            </button>
          </div>
          <div style={styles.historyList}>
            {searchHistory.map((item, index) => (
              <div
                key={index}
                style={styles.historyItem}
                onClick={() => setQuestion(item.question)}
                role="button"
                tabIndex={0}
              >
                <span style={styles.historyQuestion}>Q: {item.question}</span>
                <span style={styles.historyAnswer}>
                  A: {item.answer.length > 100 ? item.answer.substring(0, 100) + '...' : item.answer}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {!response && !error && !loading && (
        <div style={styles.emptyState}>
          <div style={styles.emptyIcon}>🤖</div>
          <h3 style={styles.emptyTitle}>Ready to help!</h3>
          <p style={styles.emptyText}>
            Ask a question about your documents and I'll search through them to find the answer.
            I can understand natural language and will show you the most relevant sources.
          </p>
        </div>
      )}
    </div>
  );
};

const styles: { [key: string]: React.CSSProperties } = {
  container: {
    maxWidth: '900px',
    margin: '0 auto',
    padding: '2rem',
  },
  header: {
    textAlign: 'center',
    marginBottom: '2rem',
  },
  title: {
    fontSize: '2rem',
    fontWeight: 700,
    color: '#333',
    marginBottom: '0.5rem',
  },
  subtitle: {
    fontSize: '1.1rem',
    color: '#666',
  },
  searchCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 4px 20px rgba(0,0,0,0.1)',
    marginBottom: '1.5rem',
  },
  searchForm: {
    marginBottom: '1rem',
  },
  inputWrapper: {
    display: 'flex',
    gap: '0.75rem',
  },
  searchInput: {
    flex: 1,
    padding: '1rem 1.25rem',
    fontSize: '1.1rem',
    border: '2px solid #e0e0e0',
    borderRadius: '8px',
    outline: 'none',
    transition: 'border-color 0.2s',
  },
  searchButton: {
    padding: '1rem 2rem',
    backgroundColor: '#1976d2',
    color: 'white',
    border: 'none',
    borderRadius: '8px',
    cursor: 'pointer',
    fontSize: '1rem',
    fontWeight: 600,
    whiteSpace: 'nowrap',
    transition: 'background-color 0.2s',
  },
  loadingText: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
  },
  spinnerInline: {
    width: '16px',
    height: '16px',
    border: '2px solid rgba(255,255,255,0.3)',
    borderTop: '2px solid white',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  suggestions: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.5rem',
    flexWrap: 'wrap',
  },
  suggestionsLabel: {
    fontSize: '0.9rem',
    color: '#666',
  },
  suggestionChip: {
    padding: '0.4rem 0.75rem',
    backgroundColor: '#e3f2fd',
    color: '#1976d2',
    border: 'none',
    borderRadius: '16px',
    cursor: 'pointer',
    fontSize: '0.85rem',
    transition: 'background-color 0.2s',
  },
  errorCard: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    padding: '1rem 1.5rem',
    backgroundColor: '#ffebee',
    borderRadius: '8px',
    marginBottom: '1.5rem',
    color: '#c62828',
  },
  errorIcon: {
    fontSize: '1.2rem',
  },
  resultsCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
    marginBottom: '1.5rem',
  },
  answerSection: {
    marginBottom: '1.5rem',
    paddingBottom: '1.5rem',
    borderBottom: '1px solid #eee',
  },
  answerTitle: {
    fontSize: '1.2rem',
    fontWeight: 600,
    color: '#333',
    marginBottom: '0.75rem',
  },
  answerText: {
    fontSize: '1.05rem',
    lineHeight: 1.7,
    color: '#444',
    whiteSpace: 'pre-wrap',
  },
  sourcesSection: {},
  sourcesTitle: {
    fontSize: '1.1rem',
    fontWeight: 600,
    color: '#555',
    marginBottom: '1rem',
  },
  sourcesList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '1rem',
  },
  sourceCard: {
    padding: '1rem',
    backgroundColor: '#fafafa',
    borderRadius: '8px',
    border: '1px solid #e0e0e0',
    cursor: 'pointer',
    transition: 'box-shadow 0.2s, border-color 0.2s',
  },
  sourceHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: '0.75rem',
    marginBottom: '0.75rem',
  },
  sourceIndex: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '24px',
    height: '24px',
    backgroundColor: '#1976d2',
    color: 'white',
    borderRadius: '50%',
    fontSize: '0.75rem',
    fontWeight: 600,
  },
  sourceFilename: {
    fontWeight: 600,
    color: '#1976d2',
    flex: 1,
  },
  sourceSimilarity: {
    fontSize: '0.85rem',
    color: '#4caf50',
    fontWeight: 500,
    backgroundColor: '#e8f5e9',
    padding: '0.25rem 0.5rem',
    borderRadius: '4px',
  },
  sourceQuote: {
    margin: '0 0 0.5rem 0',
    padding: '0.75rem',
    backgroundColor: 'white',
    borderLeft: '3px solid #1976d2',
    fontStyle: 'italic',
    color: '#555',
    fontSize: '0.95rem',
    lineHeight: 1.5,
  },
  sourceLink: {
    fontSize: '0.85rem',
    color: '#1976d2',
    fontWeight: 500,
  },
  historyCard: {
    backgroundColor: 'white',
    borderRadius: '12px',
    padding: '1.5rem',
    boxShadow: '0 2px 8px rgba(0,0,0,0.05)',
  },
  historyHeader: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '1rem',
  },
  historyTitle: {
    fontSize: '1rem',
    fontWeight: 600,
    color: '#555',
    margin: 0,
  },
  clearButton: {
    padding: '0.35rem 0.75rem',
    backgroundColor: 'transparent',
    color: '#f44336',
    border: '1px solid #f44336',
    borderRadius: '4px',
    cursor: 'pointer',
    fontSize: '0.85rem',
  },
  historyList: {
    display: 'flex',
    flexDirection: 'column',
    gap: '0.75rem',
  },
  historyItem: {
    padding: '0.75rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '6px',
    cursor: 'pointer',
    transition: 'background-color 0.2s',
  },
  historyQuestion: {
    display: 'block',
    fontWeight: 500,
    color: '#333',
    marginBottom: '0.25rem',
  },
  historyAnswer: {
    display: 'block',
    fontSize: '0.9rem',
    color: '#666',
  },
  emptyState: {
    textAlign: 'center',
    padding: '3rem 2rem',
    backgroundColor: '#f9f9f9',
    borderRadius: '12px',
  },
  emptyIcon: {
    fontSize: '4rem',
    marginBottom: '1rem',
  },
  emptyTitle: {
    fontSize: '1.3rem',
    fontWeight: 600,
    color: '#333',
    marginBottom: '0.75rem',
  },
  emptyText: {
    fontSize: '1rem',
    color: '#666',
    lineHeight: 1.6,
    maxWidth: '500px',
    margin: '0 auto',
  },
};

export default DocumentSearchPage;

