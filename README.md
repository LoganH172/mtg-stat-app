Created **Dockerfile** and **Docker Compose** for containerizing server elements, as well as, a database initialization script. 

The **Dockerfile** uses the rootProject.name of the server found in the settings.gradle file of the server. If you are intent on changing that please make sure it is reflected in the **Dockerfile**.

The **Docker Compose** file, names its primary service as commander-ledger and serves to also mount the database initialization script to the volumes. The compose file also shards the database.

The database initialization script creates 7 tables with identifiers and some other data for querying. I know that playgroup's were technically optional, but I believe with this database schema it would be simplistic to implement. Each column has a comment associated with it to explain what it represents or any further operations intended in the server-side implementation. 

