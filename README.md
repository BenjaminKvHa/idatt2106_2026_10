# idatt2106_2026_10

## Useful docker commands:

### Start every service
```bash
docker compose up -d --build
```

### Stop every service

```bash
docker compose down
```

### Stop and remove service volumes

Use this if you want to remove all local data like files & db records.

```bash
docker compose down -v
```

### Start only one service

Example:

```bash
docker compose up -d seaweedfs
```

### Restart only one service

Example using the frontend service
```bash
docker compose up -d --force-recreate frontend
```

## Frontend development

### Run frontend locally from `/frontend`:

```bash
cd frontend
npm i
npm run dev
```

### Other useful `/frontend` commands:

```bash
npm run build
npm run test:unit -- --run
npm run lint
```

### e2e `/frontend` commands:

```bash
npm run test:e2e
npm run test:e2e:dev
```

## Troubleshooting

See logs:
```bash
docker compose logs db
docker compose logs frontend_app
docker compose logs backend_app
```

See containers running:
```bash
docker compose ps
```

## Created using:

curl https://start.spring.io/starter.zip \
  -d type=maven-project \
  -d language=java \
  -d bootVersion=3.5.13 \
  -d baseDir=backend \
  -d groupId=com.nettdet \
  -d artifactId=backend \
  -d name=backend \
  -d packageName=com.nettdet.backend \
  -d packaging=jar \
  -d javaVersion=21 \
  -d dependencies=web \
  -o backend.zip
