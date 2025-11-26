Task Management System

A full-featured task management web application built with:

Java 17+ using the Spark Framework

SQLite for persistent local storage

Maven for building/running

HTML / CSS / JavaScript frontend

The system supports multi-role user access, recurring tasks, threaded comments, notifications, and a modern interactive dashboard with list and calendar views.

TEAM MEMBERS:
MAKISM GUGIN (TEAM LEADER)
JAMIE FLYNN 
CHANCE DEWITT
BENJAMIN MONAHAN

📌 Table of Contents

Features

Project Structure

Architecture

Installation (Windows)

Running the App

IDE Setup

Development Workflow

API Overview

Screenshots (Optional Placeholder)

License

🚀 Features
👤 Authentication & Roles
Role	Permissions
Admin	Full access, manage users + tasks
Manager	Create/edit/assign tasks, approve/deny, delete comments
Staff	Only tasks assigned to them, submit for review, comment
Authentication

Login & registration

Password hashing

Session-based login (cookie-backed)

📌 Task System
Task Attributes

Title

Description

Category: one-time, daily, weekly, monthly

Priority: 1–5

Due date & time

Status:

TODO

IN_PROGRESS

DONE

PENDING_REVIEW

Manager/Admin Capabilities

Create tasks

Assign tasks to multiple users

Edit tasks

Delete tasks

Mark complete / in progress

Approve/deny submissions from staff

Staff Capabilities

See only assigned tasks

Submit tasks for approval

Comment / reply

Edit + delete own comments

💬 Comments & Replies

Nested reply system

Edit/delete your own comments

Manager/Admin can delete any comments

Comments modal with live refresh

🔔 Notifications

Unread counter

Dropdown list

Auto-mark as read

Triggered on:

Task assignment

Comment on your task

Reply to your comment

Task submission

Approval/denial

📊 Dashboard
Summary Tiles

Total tasks

Due today

In progress

Done

Overdue

List View

Sorting + filtering:

Status

Category

Priority

Assigned to me

Created by me

Calendar View

Recurring tasks shown accurately

“+X more” indicator

Today highlighting

📁 Project Structure
Task-Management-App/
│
├── src/
│   └── main/java/com/example/tms/
│       ├── Main.java
│       ├── Database.java
│       ├── dao/
│       ├── model/
│       └── util/
│
│   └── main/resources/public/
│       ├── login.html
│       ├── dashboard.html
│       ├── css/
│       ├── js/
│       └── favicon.svg
│
├── database/
│   ├── schema.sql
│   └── app.db (created automatically)
│
└── pom.xml

🏗 Architecture Overview
Server-Side (Java + Spark)
Client (Browser)
      ↓ HTTP REST
Spark Java Server
      ↓ JDBC
SQLite Database

Layers
Controller (Main.java)  
    ↳ DAO Layer (TaskDAO, UserDAO, CommentDAO…)  
          ↳ Database.java (SQLite connection)

Frontend Flow
HTML/CSS/JS → Fetch API → Spark Routes → JSON → UI Rendering

🛠 Installation (Windows)

All teammates must follow these steps.
Use Command Prompt (cmd), NOT PowerShell.

1. Install Git

Download: https://git-scm.com/download/win

Install with defaults.

Verify:

git --version

2. Install Java JDK (17 or 21)

Download from Oracle / Eclipse Temurin / Microsoft OpenJDK.

Set environment variables:

setx JAVA_HOME "C:\Program Files\Java\jdk-17"
setx PATH "%JAVA_HOME%\bin;%PATH%"


Verify:

java -version
javac -version

3. Install Maven

Download ZIP: https://maven.apache.org/download.cgi

Extract to:

C:\apache-maven-3.x.x


Set environment:

setx MAVEN_HOME "C:\apache-maven-3.x.x"
setx PATH "%MAVEN_HOME%\bin;%PATH%"


Verify:

mvn -version

📥 Project Setup
4. Clone the Repository
cd C:\Projects
git clone <REPO_URL> Task-Management-App
cd Task-Management-App


Replace <REPO_URL> with the GitHub link.

▶ Running the Application

Inside the project folder:

mvn clean package
mvn exec:java -Dexec.mainClass=com.example.tms.Main


The server starts at:

http://localhost:4567/


Open in browser:

http://localhost:4567/login.html

🧭 IDE Setup
✔ VS Code

Install Java Extension Pack

Install Maven for Java

Open the project folder

Optional: Run from Maven sidebar

exec:java

👉 cmd remains the recommended method.

✔ NetBeans

Open NetBeans

File → Open Project…

Select folder with pom.xml

Run the project

🧪 Development Workflow

Whenever code changes:

mvn clean package
mvn exec:java -Dexec.mainClass=com.example.tms.Main


Refresh your browser.

📘 API Overview (Summary)
Method	Endpoint	Description
POST	/api/auth/register	Create user
POST	/api/auth/login	Authenticate
GET	/api/auth/me	Current user
GET	/api/users	List all users
GET	/api/tasks	List tasks
POST	/api/tasks	Create task
PUT	/api/tasks/:id	Edit task
DELETE	/api/tasks/:id	Delete task
PATCH	/api/tasks/:id/status	Update status
PATCH	/api/tasks/:id/submit	Submit for review
PATCH	/api/tasks/:id/approve	Approve
PATCH	/api/tasks/:id/deny	Deny
GET	/api/tasks/:id/comments	Get comments
POST	/api/tasks/:id/comments	Add comment
PATCH	/api/comments/:id	Edit comment
DELETE	/api/comments/:id	Delete comment
GET	/api/notifications	User notifications
PATCH	/api/notifications/:id/read	Mark as read
📸 Screenshots


<img src="screenshots/dashboard.png" width="700">
<img src="screenshots/calendar.png" width="700">

📄 License

This project is for educational use only and may be freely used by all project contributors.

🎯 Quick Start (Print This)
1. Install Git
2. Install Java (17+)
3. Install Maven
4. Clone the repo
5. Run in CMD:

   mvn clean package
   mvn exec:java -Dexec.mainClass=com.example.tms.Main

6. Open: http://localhost:4567/login.html