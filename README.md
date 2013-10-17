TimeSever
=========

Following task was implemented in this project:

**Time Server**

- Users can connect to server with two roles: guest and admin

- Number of existing guest connections should be limited in the server properties.Only one admin can connect to the server at the same time.

- Server should record “\<Date Time\> \<Role\>(\<session\_ID\> \<client\_IP\>) connected. Number of existing connections is \<number\>” entry to the log file every time when user 
  connected to the server.

- Server should record “\<Date Time\> \<Role\> (\<session\_ID\> \<client\_IP\>) disconnected. Number of existing connections is \<number\>” entry to the log every time when user disconnected from the server.

- Guest after connection should receive welcome message and can type command “time” and “exit”.

- Admin after connection should receive welcome message and can type commands “time”, “status”, “stop server”, “exit”. The output of command “status” should be:

   Server started:        \<Date Time\>.
   
   Existing connections:  \<Number\>.
   
   All connections:       \<Number\>.
   
   Log file:              \<Path\> \<Size\>.
   

- Admin should be able to drop existing admin connection during log in.


Usage
-----
*Requirements:*
To build code it's required to intall SBT version 0.13 or later.

Download repository to <local_rep_dir> directory and start CMD or Shell session here

- type *sbt run* to start TimeServer with default settings (admin credential admin\admin and guest limit is 10) or
- type *sbt "run \<admin_name\> \<admin_password\> \<N\>"* where:

*\<admin_name\>* is TimeSever admin login name for this session

*\<admin_password\>* is TimeServer admin password for this session

*\<N\>* is a number of max on-line guests' sessions 


