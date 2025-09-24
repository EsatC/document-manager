# Document Manager

## Project Overview
Document Manager is a comprehensive full-stack web application designed to digitize, organize, and manage physical archives efficiently. 
By bridging the gap between traditional paper-based recordkeeping and modern digital solutions, this system streamlines document storage, retrieval, and search operations. 
The backend is powered by **Spring Boot (Java)**, the frontend is developed with **React**, and **PostgreSQL** serves as the relational database management system.

The application integrates **Tesseract OCR** to extract textual information from uploaded PDFs, including scanned or image-based documents, enabling users to access and search content that would otherwise be locked in physical forms.

## Key Features

### User Authentication
- Secure login system with role-based access control.
- Prevents unauthorized access and protects sensitive document data.

### Document Upload and Management
- Upload documents in various formats (PDFs supported).
- Attach metadata to each file, including:
  - Title
  - Page count
  - Date of creation
  - Description or tags
- Documents and metadata are stored in PostgreSQL for reliable persistence.

### Optical Character Recognition (OCR)
- Tesseract OCR engine automatically extracts text from uploaded PDFs.
- Supports scanned and image-based documents.
- OCR results are indexed for advanced search and keyword highlighting.

### Advanced Search Functionality
- Search documents by metadata fields or OCR content.
- Keyword highlighting inside documents to quickly locate relevant information.
- Copyable OCR results enable easy extraction and use of document text.

### API Validation and Testing
- All REST API endpoints tested and validated using **Postman**.
- Ensures robustness, reliability, and error handling.

## Technology Stack
- **Backend**: Spring Boot (Java), Spring Data JPA
- **Frontend**: React, HTML5, CSS3, JavaScript
- **Database**: PostgreSQL
- **OCR Engine**: Tesseract OCR
- **Testing**: Postman

## Architecture and Design
- **Frontend-Backend Separation**: React frontend communicates with Spring Boot backend via REST APIs.
- **Modular Design**: The backend follows a layered architecture (Controller, Service, Repository).
- **Database Schema**: Relational design with tables for users, documents, and metadata.
- **OCR Integration**: Text extraction is performed asynchronously during file upload to improve performance.
- **Search Indexing**: Metadata and OCR content are indexed to provide fast and efficient search capabilities.

## Purpose and Benefits
- Digitizes physical archives, reducing dependency on paper storage.
- Enables quick and accurate search of historical documents.
- Supports organizational compliance and auditing processes.
- Improves accessibility by providing searchable, copyable digital documents.
- Enhances productivity by automating OCR extraction and metadata management.

## Potential Extensions
- Implement role-based document access for different user groups.
- Add version control for documents.
- Integrate with cloud storage services for scalability.
- Implement full-text search with Elasticsearch for even faster search performance.
- Include automated notifications for newly uploaded or modified documents.

---

