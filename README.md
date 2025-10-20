# Banking Application Reliability Testing Dashboard

A comprehensive web-based dashboard for testing and monitoring the reliability of banking applications. This system allows you to send multiple concurrent requests to your banking service and analyze the results through an intuitive interface with real-time statistics and visualizations.

## 📹 Demo Video

Watch the full demonstration here: [https://youtu.be/JPdRoaR5B7I](https://youtu.be/JPdRoaR5B7I)

It runs in: http://localhost:8085

## ✨ Features

### 🎯 Request Control Panel
- Send configurable number of test requests (1-10,000)
- Concurrent request processing using ExecutorService
- Real-time loading indicators

### 📊 Real-time Statistics Dashboard
- **Success Count**: Total number of successful requests
- **Error Count**: Total number of failed requests
- **Total Logs**: Combined count of all logged events
- **Error Rate**: Percentage of failed requests

### 📈 Error Distribution Visualization
- Interactive pie chart showing error categories
- Main error categories:
  - Database Errors (connection pool exhaustion, timeouts, SQL syntax)
  - Server Errors (null pointer, illegal state, internal server errors)
  - Performance Warnings (high latency)
  - Network Timeouts
  - Other errors
- Sub-category breakdowns with percentages

### 📋 Log Viewer
Three tabbed views for comprehensive log analysis:
- **All Logs**: Combined view of all requests sorted by timestamp
- **Success Logs**: Only successful request logs
- **Error Logs**: Only error and warning logs

### 🎨 Modern UI/UX
- Responsive design for all screen sizes
- Smooth animations and hover effects
- Color-coded log entries (green for success, red for errors)
- Gradient background design
- Loading overlays for better user feedback

## 🏗️ Architecture

### Backend (Spring Boot)

#### Controller: `LogController.java`
```
Endpoints:
- POST /logs/sendRequestsAndSave?count={number}
  Sends specified number of requests and saves logs
  
- GET /logs/error
  Returns all error logs
  
- GET /logs/success
  Returns all successful logs
  
- GET /logs/error-distribution
  Returns error distribution with percentages and sub-categories
```

#### Service: `LogServiceImpl.java`
Key components:
- **Concurrent Request Processing**: Uses ExecutorService with 15 threads
- **Log Fetching**: Retrieves logs from external API
- **Log Parsing**: Parses timestamp and message format (yyyy/MM/dd HH:mm:ss)
- **Error Classification**: Categorizes errors into main and sub-categories
- **Database Operations**: Saves and retrieves logs using ServerLogRepo

#### Data Flow
1. User initiates request from dashboard
2. Service sends concurrent requests to balance check API
3. Fetches logs from log server API
4. Parses and classifies logs
5. Saves to database
6. Frontend retrieves and displays data

### Frontend (HTML/CSS/JavaScript)

#### Technologies Used
- **Bootstrap 5.3.0**: Responsive layout and components
- **Chart.js 4.4.0**: Interactive pie chart visualization
- **Font Awesome 6.4.0**: Icon library
- **Vanilla JavaScript**: API interactions and DOM manipulation

#### Key Functions
- `sendRequests()`: Sends POST request to initiate test
- `loadAllData()`: Fetches all log types and statistics
- `displayErrorDistribution()`: Renders pie chart with error data
- `displayLogs()`: Renders log entries in tabbed interface
- `updateStats()`: Updates statistics cards
