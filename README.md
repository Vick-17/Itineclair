# Itineclair

L'environnement de développement complet fonctionne dans Docker : Java 17,
Maven, Node.js 24, npm et PostgreSQL. Seuls Docker et Docker Compose sont requis
sur la machine hôte.

## Démarrer l'environnement

```bash
docker compose up --build -d
docker compose exec --user dev dev bash
```

Le dépôt est monté dans `/workspace`. Les modifications faites sur l'hôte ou
depuis le conteneur sont donc immédiatement synchronisées.

## Lancer les applications

Dans le conteneur de développement, ouvrir deux terminaux :

```bash
# Terminal 1 : API Spring Boot
cd /workspace/api
./mvnw spring-boot:run
```

```bash
# Terminal 2 : front Vite
cd /workspace/web
npm install
npm run dev -- --host 0.0.0.0
```

L'API est accessible sur <http://localhost:8080/api> et le front sur
<http://localhost:5173>. PostgreSQL est disponible depuis le conteneur avec :

```bash
psql -h postgres -U itineclair -d itineclair
```

Le mot de passe de développement par défaut est `itineclair`. Pour modifier les
ports ou les identifiants, copier `.env.example` vers `.env`, puis adapter les
valeurs.

## Arrêter ou réinitialiser

```bash
docker compose down
```

Les dépendances et les données PostgreSQL restent dans des volumes Docker. Pour
réinitialiser aussi ces données :

```bash
docker compose down --volumes
```
