TimeSever
=========

Following task was implemented in this project:

Time Server

∙ Users can connect to server with two roles: guest and admin

∙ Number of existing guest connections should be limited in the server properties.Only one admin can connect to the server at the same time.

∙ Server should record “<Date Time> <Role>(<session_ID> <client_IP>) connected. Number of existing connections is <number>” entry to the log file every time when user 
  connected to the server.

∙ Server should record “<Date Time> <Role> (<session_ID> <client_IP>) disconnected. Number of existing connections is <number>” entry to the log every time when user disconnected from the server.

∙ Guest after connection should receive welcome message and can type command “time” and “exit”.

∙ Admin after connection should receive welcome message and can type commands “time”, “status”, “stop server”, “exit”. The output of command “status” should be:

   Server started:        <Date Time>.
   Existing connections:  <Number>.
   All connections:       <Number>.
   Log file:              <Path> <Size>.

∙ Admin should be able to drop existing admin connection during log in.


