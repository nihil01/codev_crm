.PHONY: up down restart rebuild logs build-backend build-frontend clean

# Start all services
up:
	docker compose up -d

# Stop all services
down:
	docker compose down

# Restart all services
restart:
	docker compose restart

# Rebuild and start all services
rebuild:
	docker compose up -d --build

# Rebuild specific service
rebuild-backend:
	docker compose up -d --build backend

rebuild-frontend:
	docker compose up -d --build frontend

# View logs
logs:
	docker compose logs -f

logs-backend:
	docker compose logs -f backend

logs-frontend:
	docker compose logs -f frontend

logs-db:
	docker compose logs -f db

# Build without running
build-backend:
	docker compose build backend

build-frontend:
	docker compose build frontend

# Clean everything (containers, volumes, images)
clean:
	docker compose down -v --rmi all

# Database access
db-shell:
	docker compose exec db psql -U postgres -d tedris_crm

# Status
status:
	docker compose ps
