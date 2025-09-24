import React, { useState, useEffect, useRef } from 'react';
import { Plus, FileText, Search, Edit2, Trash2, Save, X, Upload, Download, LogOut, Eye, EyeOff, XCircle, FileSearch } from 'lucide-react'; // Import XCircle and FileSearch

// Base URL for the Spring Boot API
const API_BASE_URL = 'http://localhost:8080/api';

/**
 * Main component for the Document Manager Application.
 * Handles state management, API calls, and renders the UI.
 */
const DocumentManager = () => {
  // State variables
  const [user, setUser] = useState(null);
  const [documents, setDocuments] = useState([]);
  const [isAddingOrEditing, setIsAddingOrEditing] = useState(false);
  const [editingId, setEditingId] = useState(null);

  // Search bar states
  const [searchTerm, setSearchTerm] = useState(''); // Value currently in the input field
  const [activeSearchTerm, setActiveSearchTerm] = useState(''); // Value used for actual API search
  const [searchInOcr, setSearchInOcr] = useState(false); // New state for OCR search

  const [isLoading, setIsLoading] = useState(true);
  const [showAuthModal, setShowAuthModal] = useState(false);
  const [authMode, setAuthMode] = useState('login'); // 'login' or 'register'
  const [showPassword, setShowPassword] = useState(false);
  const fileInputRef = useRef(null);
  const fileInputRefModal = useRef(null); // Ref for the file input in the modal

  // OCR content modal states
  const [showOcrModal, setShowOcrModal] = useState(false);
  const [currentOcrText, setCurrentOcrText] = useState('');
  const [currentOcrDocumentTitle, setCurrentOcrDocumentTitle] = useState('');
  const [isOcrLoading, setIsOcrLoading] = useState(false);
  const [ocrSearchKeyword, setOcrSearchKeyword] = useState(''); // State for OCR highlighting
  const [cachedOcrTexts, setCachedOcrTexts] = useState({}); // New state to cache OCR content by document ID


  // Form data states
  const [documentFormData, setDocumentFormData] = useState({
    title: '',
    number: '',
    date: '',
    description: ''
  });
  const [selectedFile, setSelectedFile] = useState(null); // New state for selected file in modal

  const [authFormData, setAuthFormData] = useState({
    username: '',
    password: '',
    email: '',
    firstName: '',
    lastName: ''
  });

  // Effect to validate token on initial load
  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      validateToken(token);
    } else {
      setIsLoading(false);
      setShowAuthModal(true);
    }
  }, []);

  // Effect to fetch documents when user changes or activeSearchTerm changes (only on Enter/Clear)
  useEffect(() => {
    if (user) {
      fetchDocuments();
    }
  }, [user, activeSearchTerm, searchInOcr]); // NOW DEPENDS ON activeSearchTerm AND searchInOcr

  // API utility function
  const callApi = async (endpoint, method = 'GET', body = null, isFormData = false) => {
    const token = localStorage.getItem('token');
    const headers = {};
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    let requestBody = body;

    if (!isFormData) {
      headers['Content-Type'] = 'application/json';
      requestBody = body ? JSON.stringify(body) : null;
    }

    try {
      const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        method,
        headers,
        body: requestBody,
      });

      if (response.status === 401) {
        // Token expired or invalid, log out the user
        handleLogout();
        return null;
      }

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Something went wrong');
      }

      // Check if the response has content
      if (response.headers.get('content-length') === '0' || response.status === 204) {
        return null; // No content
      }

      return await response.json();

    } catch (error) {
      console.error('API call error:', error);
      throw error;
    }
  };

  // Auth functions
  const validateToken = async (token) => {
    try {
      const data = await callApi('/auth/validate', 'GET');
      setUser(data);
    } catch (error) {
      console.error('Token validation failed:', error);
      handleLogout();
    } finally {
      setIsLoading(false);
    }
  };

  const handleAuthSubmit = async (e) => {
    e.preventDefault();
    try {
      let data;
      if (authMode === 'login') {
        data = await callApi('/auth/login', 'POST', {
          username: authFormData.username,
          password: authFormData.password
        });
      } else {
        data = await callApi('/auth/register', 'POST', authFormData);
      }
      localStorage.setItem('token', data.token);
      setUser(data);
      setShowAuthModal(false);
      setAuthFormData({ username: '', password: '', email: '', firstName: '', lastName: '' }); // Clear form
    } catch (error) {
      // Use a custom modal or message box instead of alert
      console.error(error.message);
      // Example of a simple message box (you'd implement a proper modal)
      alert(error.message);
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setUser(null);
    setDocuments([]);
    setShowAuthModal(true);
    setAuthMode('login'); // Reset to login mode
  };

  // Document functions
  const fetchDocuments = async () => {
    setIsLoading(true);
    try {
      // Use activeSearchTerm for the API call, and conditionally include OCR search
      const endpoint = searchInOcr ? `/documents/ocr/search?query=${activeSearchTerm}` : `/documents?search=${activeSearchTerm}`;
      const data = await callApi(endpoint);
      setDocuments(data.content || []); // Assuming the API returns a Page object
    } catch (error) {
      console.error('Error fetching documents:', error);
      setDocuments([]); // Clear documents on error
    } finally {
      setIsLoading(false);
    }
  };

  const handleInputChange = (e) => {
    setDocumentFormData({ ...documentFormData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e) => {
    setSelectedFile(e.target.files[0]);
  };

  const handleNewDocumentClick = () => {
    setIsAddingOrEditing(true);
    setEditingId(null);
    setDocumentFormData({
      title: '',
      number: '',
      date: '',
      description: ''
    });
    setSelectedFile(null); // Clear selected file when opening new document form
    if (fileInputRefModal.current) {
      fileInputRefModal.current.value = ""; // Clear file input visually
    }
  };

  const handleCreateDocument = async () => {
    try {
      const formData = new FormData();
      formData.append('document', new Blob([JSON.stringify(documentFormData)], {
        type: 'application/json'
      }));
      if (selectedFile) {
        formData.append('file', selectedFile);
      }

      await callApi('/documents', 'POST', formData, true); // Use isFormData = true
      fetchDocuments();
      setIsAddingOrEditing(false);
      setSelectedFile(null); // Clear selected file after upload
    } catch (error) {
      // Use a custom modal or message box instead of alert
      console.error(`Error creating document: ${error.message}`);
      alert(`Error creating document: ${error.message}`);
    }
  };

  const handleEdit = (doc) => {
    setIsAddingOrEditing(true);
    setEditingId(doc.id);
    setDocumentFormData({
      title: doc.title,
      number: doc.number,
      date: doc.date,
      description: doc.description || ''
    });
    setSelectedFile(null); // Clear selected file when opening edit form
    if (fileInputRefModal.current) {
      fileInputRefModal.current.value = ""; // Clear file input visually
    }
  };

  const handleUpdateDocument = async () => {
    try {
      const formData = new FormData();
      formData.append('document', new Blob([JSON.stringify(documentFormData)], {
        type: 'application/json'
      }));
      if (selectedFile) {
        formData.append('file', selectedFile);
      }

      await callApi(`/documents/${editingId}`, 'PUT', formData, true); // Use isFormData = true
      fetchDocuments();
      setIsAddingOrEditing(false);
      setEditingId(null);
      setSelectedFile(null); // Clear selected file after upload
      // Clear cached OCR for the updated document as its content might have changed
      setCachedOcrTexts(prev => {
        const newCache = { ...prev };
        delete newCache[editingId];
        return newCache;
      });
    } catch (error) {
      // Use a custom modal or message box instead of alert
      console.error(`Error updating document: ${error.message}`);
      alert(`Error updating document: ${error.message}`);
    }
  };

  const handleDelete = async (id) => {
    // Use a custom modal for confirmation instead of window.confirm
    if (window.confirm('Are you sure you want to delete this document?')) {
      try {
        await callApi(`/documents/${id}`, 'DELETE');
        fetchDocuments();
        // Clear cached OCR for the deleted document
        setCachedOcrTexts(prev => {
          const newCache = { ...prev };
          delete newCache[id];
          return newCache;
        });
      } catch (error) {
        // Use a custom modal or message box instead of alert
        console.error(`Error deleting document: ${error.message}`);
        alert(`Error deleting document: ${error.message}`);
      }
    }
  };

  const handleFileUpload = async (documentId, file) => {
    try {
      const formData = new FormData();
      formData.append('file', file);
      await callApi(`/documents/${documentId}/upload`, 'POST', formData, true);
      fetchDocuments();
      // Use a custom modal or message box instead of alert
      alert('File uploaded successfully!');
      // Clear cached OCR for this document as new file means new OCR
      setCachedOcrTexts(prev => {
        const newCache = { ...prev };
        delete newCache[documentId];
        return newCache;
      });
    } catch (error) {
      // Use a custom modal or message box instead of alert
      console.error(`Error uploading file: ${error.message}`);
      alert(`Error uploading file: ${error.message}`);
    }
  };

  const handleFileDownload = async (documentId, filename) => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${API_BASE_URL}/documents/${documentId}/download`, {
        method: 'GET',
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });

      if (response.status === 401) {
        handleLogout();
        return;
      }

      if (!response.ok) {
        throw new Error('File download failed');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (error) {
      // Use a custom modal or message box instead of alert
      console.error(`Error downloading file: ${error.message}`);
      alert(`Error downloading file: ${error.message}`);
    }
  };

  const handleDeleteFile = async (documentId) => {
    // Use a custom modal for confirmation instead of window.confirm
    if (window.confirm('Are you sure you want to delete the attached file?')) {
      try {
        await callApi(`/documents/${documentId}/file`, 'DELETE');
        fetchDocuments();
        // Use a custom modal or message box instead of alert
        alert('File deleted successfully!');
        // Clear cached OCR for this document since the file is deleted
        setCachedOcrTexts(prev => {
          const newCache = { ...prev };
          delete newCache[documentId];
          return newCache;
        });
      } catch (error) {
        // Use a custom modal or message box instead of alert
        console.error(`Error deleting file: ${error.message}`);
        alert(`Error deleting file: ${error.message}`);
      }
    }
  };

  // OCR Functions
  const handleShowOcr = async (documentId, documentTitle) => {
    setIsOcrLoading(true);
    setShowOcrModal(true);
    setCurrentOcrDocumentTitle(documentTitle);
    setOcrSearchKeyword(activeSearchTerm); 

    // Check if OCR content is already cached for this document
   /* if (cachedOcrTexts[documentId]) {
      setCurrentOcrText(cachedOcrTexts[documentId]);
      setIsOcrLoading(false);
      return; // Exit early as content is already available
    }*/
    setIsOcrLoading(true);
    // If not cached, fetch from API
    try {
      const data = await callApi(`/documents/${documentId}/ocr/text`);
      const ocrText = data.ocrText || 'No OCR text available for this document.';
      setCurrentOcrText(ocrText);
      // Cache the fetched OCR text
      setCachedOcrTexts(prev => ({
        ...prev,
        [documentId]: ocrText
      }));
    } catch (error) {
      console.error('Error fetching OCR text:', error);
      setCurrentOcrText('Failed to load OCR text.');
    } finally {
      setIsOcrLoading(false);
    }
  };

  const handleCloseOcrModal = () => {
    setShowOcrModal(false);
    setCurrentOcrText('');
    setCurrentOcrDocumentTitle('');
    setOcrSearchKeyword(''); 
    
  };

  // Search Bar Handlers
  const handleSearchInputChange = (e) => {
    setSearchTerm(e.target.value);
  };

  const handleSearchKeyPress = (e) => {
    if (e.key === 'Enter') {
      setActiveSearchTerm(searchTerm); // Trigger search only on Enter
    }
  };

  const handleClearSearch = () => {
    setSearchTerm(''); // Clear input field
    setActiveSearchTerm(''); // Trigger search with empty term
  };

  const handleToggleSearchInOcr = () => {
    setSearchInOcr(!searchInOcr);
    // If toggling, re-fetch documents with the new search mode
    setActiveSearchTerm(searchTerm);
  };

  // Helper function to highlight text
  const highlightText = (text, keyword) => {
    if (!keyword || keyword.trim() === '') {
      return text;
    }
    // Escape special characters in the keyword for RegExp
    const escapedKeyword = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const parts = text.split(new RegExp(`(${escapedKeyword})`, 'gi'));
    return (
      <>
        {parts.map((part, index) =>
          part.toLowerCase() === keyword.toLowerCase() ? (
            <span key={index} className="bg-yellow-200">{part}</span>
          ) : (
            part
          )
        )}
      </>
    );
  };


  if (isLoading) {
    return (
      <div className="flex justify-center items-center h-screen bg-gray-100">
        <div className="text-blue-600 text-xl font-semibold">Loading...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      {/* Auth Modal */}
      {showAuthModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-75 flex items-center justify-center z-50">
          <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-md">
            <h2 className="text-2xl font-bold mb-6 text-center text-gray-800">
              {authMode === 'login' ? 'Login' : 'Register'}
            </h2>
            <form onSubmit={handleAuthSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700">Username</label>
                <input
                  type="text"
                  name="username"
                  value={authFormData.username}
                  onChange={(e) => setAuthFormData({ ...authFormData, username: e.target.value })}
                  className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  required
                />
              </div>
              {authMode === 'register' && (
                <>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">Email</label>
                    <input
                      type="email"
                      name="email"
                      value={authFormData.email}
                      onChange={(e) => setAuthFormData({ ...authFormData, email: e.target.value })}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">First Name</label>
                    <input
                      type="text"
                      name="firstName"
                      value={authFormData.firstName}
                      onChange={(e) => setAuthFormData({ ...authFormData, firstName: e.target.value })}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700">Last Name</label>
                    <input
                      type="text"
                      name="lastName"
                      value={authFormData.lastName}
                      onChange={(e) => setAuthFormData({ ...authFormData, lastName: e.target.value })}
                      className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                      required
                    />
                  </div>
                </>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700">Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    name="password"
                    value={authFormData.password}
                    onChange={(e) => setAuthFormData({ ...authFormData, password: e.target.value })}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                  <span
                    className="absolute inset-y-0 right-0 pr-3 flex items-center cursor-pointer"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-5 w-5 text-gray-400" />
                    ) : (
                      <Eye className="h-5 w-5 text-gray-400" />
                    )}
                  </span>
                </div>
              </div>
              <button
                type="submit"
                className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500"
              >
                {authMode === 'login' ? 'Login' : 'Register'}
              </button>
            </form>
            <div className="mt-6 text-center">
              <button
                onClick={() => setAuthMode(authMode === 'login' ? 'register' : 'login')}
                className="text-sm text-blue-600 hover:text-blue-800"
              >
                {authMode === 'login' ? 'Need an account? Register' : 'Already have an account? Login'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* OCR Content Modal */}
      {showOcrModal && (
        <div className="fixed inset-0 bg-gray-600 bg-opacity-75 flex items-center justify-center z-50 p-4">
          <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-3xl max-h-[90vh] overflow-y-auto relative">
            <button
              onClick={handleCloseOcrModal}
              className="absolute top-4 right-4 text-gray-500 hover:text-gray-700 p-2 rounded-full hover:bg-gray-100 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
            <h2 className="text-2xl font-bold mb-4 text-gray-800">OCR Content for: {currentOcrDocumentTitle}</h2>
            {isOcrLoading ? (
              <div className="flex justify-center items-center h-40">
                <div className="text-blue-600 text-lg">Loading OCR text...</div>
              </div>
            ) : (
              <div className="bg-gray-50 p-4 rounded-md border border-gray-200">
                <pre className="whitespace-pre-wrap font-mono text-sm text-gray-800">
                  {highlightText(currentOcrText, ocrSearchKeyword)}
                </pre>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Main Content */}
      <header className="bg-white shadow-sm p-4 flex justify-between items-center sticky top-0 z-10">
        <h1 className="text-2xl font-bold text-gray-800">Document Manager</h1>
        {user && (
          <div className="flex items-center space-x-4">
            <span className="text-gray-700 text-md">Welcome, {user.firstName} {user.lastName}!</span>
            <button
              onClick={handleLogout}
              className="flex items-center space-x-2 px-4 py-2 bg-red-500 text-white rounded-md hover:bg-red-600 transition-colors"
            >
              <LogOut className="w-4 h-4" />
              <span>Logout</span>
            </button>
          </div>
        )}
      </header>

      <div className="flex-grow container mx-auto px-4 py-8">
        <div className="flex flex-col sm:flex-row justify-between items-center mb-6 space-y-4 sm:space-y-0">
          <button
            onClick={handleNewDocumentClick}
            className="flex items-center space-x-2 px-6 py-3 bg-blue-600 text-white rounded-md shadow-md hover:bg-blue-700 transition-colors text-lg"
          >
            <Plus className="w-5 h-5" />
            <span>New Document</span>
          </button>
          {/* Search bar with "search on enter" and "clear" button */}
          <div className="relative w-full sm:w-auto flex flex-col sm:flex-row items-center space-y-2 sm:space-y-0 sm:space-x-4">
            <div className="relative w-full sm:w-80">
              <input
                type="text"
                placeholder="Search documents (Press Enter to search)..."
                value={searchTerm}
                onChange={handleSearchInputChange}
                onKeyPress={handleSearchKeyPress}
                className="w-full pl-10 pr-10 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              />
              <Search className="w-5 h-5 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
              {searchTerm && ( // Show clear button only if there's text in the search bar
                <button
                  onClick={handleClearSearch}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-700"
                  title="Clear search"
                >
                  <XCircle className="w-5 h-5" />
                </button>
              )}
            </div>
            <label className="flex items-center space-x-2 cursor-pointer text-gray-700">
              <input
                type="checkbox"
                checked={searchInOcr}
                onChange={handleToggleSearchInOcr}
                className="form-checkbox h-5 w-5 text-blue-600 rounded focus:ring-blue-500"
              />
              <span>Search in OCR</span>
            </label>
          </div>
        </div>

        {/* Add/Edit Document Modal */}
        {isAddingOrEditing && (
          <div className="fixed inset-0 bg-gray-600 bg-opacity-75 flex items-center justify-center z-50 p-4">
            <div className="bg-white p-8 rounded-lg shadow-xl w-full max-w-2xl max-h-[90vh] overflow-y-auto">
              <h2 className="text-2xl font-bold mb-6 text-gray-800">
                {editingId ? 'Edit Document' : 'New Document'}
              </h2>
              <form onSubmit={(e) => {
                e.preventDefault();
                if (editingId) {
                  handleUpdateDocument();
                } else {
                  handleCreateDocument();
                }
              }} className="space-y-4">
                <div>
                  <label htmlFor="title" className="block text-sm font-medium text-gray-700">Title</label>
                  <input
                    type="text"
                    id="title"
                    name="title"
                    value={documentFormData.title}
                    onChange={handleInputChange}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="number" className="block text-sm font-medium text-gray-700">Number</label>
                  <input
                    type="text"
                    id="number"
                    name="number"
                    value={documentFormData.number}
                    onChange={handleInputChange}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="date" className="block text-sm font-medium text-gray-700">Date</label>
                  <input
                    type="date"
                    id="date"
                    name="date"
                    value={documentFormData.date}
                    onChange={handleInputChange}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    required
                  />
                </div>
                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700">Description</label>
                  <textarea
                    id="description"
                    name="description"
                    value={documentFormData.description}
                    onChange={handleInputChange}
                    rows="3"
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                  ></textarea>
                </div>

                {/* File Upload Input in Modal */}
                <div>
                  <label htmlFor="file-upload" className="block text-sm font-medium text-gray-700">
                    {editingId && documents.find(doc => doc.id === editingId)?.hasFile ? "Replace File" : "Upload File"}
                  </label>
                  <input
                    type="file"
                    id="file-upload"
                    ref={fileInputRefModal} // Assign ref here
                    onChange={handleFileChange}
                    className="mt-1 block w-full text-sm text-gray-500
                               file:mr-4 file:py-2 file:px-4
                               file:rounded-md file:border-0
                               file:text-sm file:font-semibold
                               file:bg-blue-50 file:text-blue-700
                               hover:file:bg-blue-100"
                    onClick={(e) => e.target.value = null} // Clear input value on click
                  />
                  {selectedFile && <p className="mt-2 text-sm text-gray-500">Selected file: {selectedFile.name}</p>}
                  {editingId && documents.find(doc => doc.id === editingId)?.hasFile && !selectedFile && (
                    <p className="mt-2 text-sm text-gray-500">Current file: {documents.find(doc => doc.id === editingId).originalFilename}</p>
                  )}
                </div>


                <div className="flex justify-end space-x-3 mt-6">
                  <button
                    type="button"
                    onClick={() => setIsAddingOrEditing(false)}
                    className="flex items-center space-x-2 px-4 py-2 bg-gray-300 text-gray-800 rounded-md hover:bg-gray-400 transition-colors"
                  >
                    <X className="w-4 h-4" />
                    <span>Cancel</span>
                  </button>
                  <button
                    type="submit"
                    className="flex items-center space-x-2 px-4 py-2 bg-blue-600 text-white rounded-md shadow-md hover:bg-blue-700 transition-colors"
                  >
                    <Save className="w-4 h-4" />
                    <span>{editingId ? 'Update Document' : 'Create Document'}</span>
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}

        <main className="bg-white p-6 rounded-lg shadow-md">
          <div className="grid grid-cols-1 gap-6">
            {documents.length > 0 ? (
              documents.map((doc) => (
                <div key={doc.id} className="flex flex-col sm:flex-row items-start sm:items-center justify-between border-b border-gray-200 pb-4 last:border-b-0 last:pb-0">
                    <div className="flex-grow pr-4">
                        <h3 className="text-xl font-semibold text-gray-800">{doc.title} (<span className="text-blue-600">{doc.number}</span>)</h3>
                        <p className="text-gray-600 text-sm mb-1">Date: {doc.date}</p>
                        {doc.description && <p className="text-gray-700 text-sm mb-2">{doc.description}</p>}
                        <div className="flex items-center space-x-3 text-sm">
                            {doc.hasFile ? (
                                <>
                                    <button
                                        onClick={() => handleFileDownload(doc.id, doc.originalFilename)}
                                        className="text-blue-600 hover:text-blue-800 flex items-center space-x-1 font-semibold"
                                    >
                                        <Download className="w-4 h-4" />
                                        <span>{doc.originalFilename} ({Math.round(doc.fileSize / 1024)} KB)</span>
                                    </button>
                                    <button
                                        onClick={() => handleDeleteFile(doc.id)}
                                        className="text-red-600 hover:text-red-800 flex items-center space-x-1 font-semibold"
                                    >
                                        <Trash2 className="w-4 h-4" />
                                        <span>Delete File</span>
                                    </button>
                                    <button
                                        onClick={() => handleShowOcr(doc.id, doc.title)}
                                        className="text-purple-600 hover:text-purple-800 flex items-center space-x-1 font-semibold"
                                    >
                                        <FileSearch className="w-4 h-4" />
                                        <span>View OCR</span>
                                    </button>
                                </>
                            ) : (
                                <label className="text-green-600 hover:text-green-800 font-semibold flex items-center space-x-1 cursor-pointer">
                                    <Upload className="w-4 h-4" />
                                    <span>Upload File</span>
                                    <input type="file" ref={fileInputRef} className="hidden" onChange={(e) => e.target.files && handleFileUpload(doc.id, e.target.files[0])} />
                                </label>
                            )}
                        </div>
                    </div>
                    <div className="flex items-center space-x-2 self-start sm:self-center">
                        <button onClick={() => handleEdit(doc)} className="text-gray-600 hover:text-blue-600 p-2 rounded-full hover:bg-blue-50 transition-colors"><Edit2 className="w-5 h-5" /></button>
                        <button onClick={() => handleDelete(doc.id)} className="text-gray-600 hover:text-red-600 p-2 rounded-full hover:bg-red-50 transition-colors"><Trash2 className="w-5 h-5" /></button>
                    </div>
                </div>
                ))
            ) : (
                <div className="text-center py-16 text-gray-500">
                    <FileText className="w-16 h-16 mx-auto mb-4 text-gray-300" />
                    <p className="font-semibold">No documents found.</p>
                    <p>Click "New Document" to get started.</p>
                </div>
            )}
            </div>
        </main>
      </div>
    </div>
  );
};

export default DocumentManager;