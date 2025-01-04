# E-commerce Backend Docker Setup

This guide provides instructions to set up the necessary services for the e-commerce backend using Docker.

## PostgreSQL Database Setup

The backend relies on a PostgreSQL database. Use one of the following commands to run the database container:

### Option 1: Basic Setup (No Backup)

This will start the database without enabling backup functionality.

```bash
docker run --name etherealdb -p 5433:5432 -e POSTGRES_PASSWORD=root -d postgres
```

### Option 2: Backup Enabled

This option sets up the database with a backup directory mapped to your local machine.
Replace `D:\Dev\ecommerce-be\postgres_backup` with the desired directory on your computer.

```bash
docker run --name sklepdb -p 5433:5432 -v D:\Dev\ecommerce-be\postgres_backup:/var/lib/postgresql/data -e POSTGRES_PASSWORD=postgres -d postgres
```

## FTP Server Setup

To configure an FTP server for file management, use the following command:

```bash
docker run -d --name ftp2 -p 8002:21 -p 21011-21020:21011-21020 -e USERS="ethereal|ethereal_pass|/home/ethereal|10000" delfer/alpine-ftp-server
```