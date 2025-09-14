# P-MAGIC Physics Admin Portal

## Overview
Simplified admin portal for managing the P-MAGIC Physics application with essential features only.

## Files Structure
```
admin/
├── index.html          # Login page
├── dashboard.html      # Main dashboard with statistics
├── users.html         # User management
├── all_records.html   # Comprehensive records view
├── admin-styles.css   # Shared styles
├── firebase.json      # Firebase hosting config
├── server.py          # Local development server
└── README.md          # This documentation
```

## Features

### 🏠 Dashboard (`dashboard.html`)
- **User Statistics**: Total Users, Active Users, Admin Users
- **Quick Actions**: Direct link to User Management
- **Clean Interface**: Simplified navigation with only essential features

### 👥 User Management (`users.html`)
- **User List**: View all users with Name, Email, Role, and Status
- **User Actions**: Edit and Delete users
- **Role Management**: Admin/User roles
- **Status Control**: Active/Inactive user status
- **Add Users**: Create new users manually
- **CSV Upload**: Bulk user creation via CSV file

### 🎨 Styling (`admin-styles.css`)
- **Bootstrap 5**: Modern UI framework
- **Responsive Design**: Works on all devices
- **Consistent Theme**: Professional purple/blue color scheme

### 🗄️ All Records (`all_records.html`)
- **Comprehensive Data View**: All records from both 'record' and 'questions' collections
- **Advanced Filtering**: Filter by category, status, date range, and user search
- **Data Visualization**: Activity timeline and category distribution charts
- **Statistics Dashboard**: Total records, Out Class, In Class, and detection counts
- **Record Management**: View details, delete records, export data
- **Image Gallery**: Preview all types of images (detection, canvas, documentation)
- **Export Functionality**: Export filtered data to Excel/CSV

## Navigation
The admin portal includes:
- **Dashboard** - Main overview page with user statistics
- **Users** - User management page
- **All Records** - Comprehensive records and data analysis

## User Management Features

### User Status
- **Active**: User can login and use the application
- **Inactive**: User is blocked from accessing the application

### User Roles
- **Admin**: Full access to admin portal
- **User**: Regular application user

### CSV Upload Format
```csv
password,nama,email
userpass123,John Doe,john@example.com
userpass456,Jane Smith,jane@example.com
```

## Development

### Local Server
```bash
python server.py
```
Then visit: `http://localhost:8000`

### Firebase Hosting
```bash
firebase deploy --only hosting
```

## Security
- Firebase Authentication required
- Admin-only access to portal
- User status control for application access
- Secure password requirements (minimum 6 characters)

## Removed Features
To simplify the admin portal, the following features have been removed:
- ❌ Questions management
- ❌ User utilities and fix tools
- ❌ Recent activity tracking
- ❌ Admin creation utilities

The portal now focuses on essential user management, comprehensive data viewing, and analytics.