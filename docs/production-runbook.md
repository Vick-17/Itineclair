# Exploitation de la bêta privée

Dernière revue : 2026-09-09.

Ce guide décrit le déploiement mono-serveur du MVP. Il ne transforme pas
Itinéclair en service apte à une bêta publique : les critères bloquants restent
dans `mvp-release-checklist.md` et `risk-register.md`.

## Architecture retenue

- un frontal HTTPS installé sur l’hôte reçoit le trafic public ;
- le conteneur Nginx d’Itinéclair écoute uniquement sur `127.0.0.1` ;
- l’API n’est joignable que depuis le réseau Docker `app` ;
- PostgreSQL n’est joignable que depuis le réseau Docker interne `data` ;
- les sessions sont conservées en mémoire : une seule instance d’API est
  supportée pour le MVP.

Le port `8080` ne doit jamais être publié directement sur Internet. Le frontal
TLS est responsable du certificat, de la redirection HTTP vers HTTPS et de
`Strict-Transport-Security`, une fois HTTPS vérifié.

## Préparer le serveur

```bash
cp .env.production.example .env.production
chmod 600 .env.production
openssl rand -hex 32
```

Copier la valeur produite dans `POSTGRES_PASSWORD`. Conserver :

```dotenv
BIND_ADDRESS=127.0.0.1
ITINECLAIR_WEATHER_ENABLED=false
```

Le fichier `.env.production` ne doit jamais être commité.

## Configurer HTTPS

Exemple avec Caddy installé sur le même serveur :

```caddyfile
itineclair.example.fr {
    encode zstd gzip
    reverse_proxy 127.0.0.1:8080
    header Strict-Transport-Security "max-age=31536000"
}
```

Remplacer le domaine. Ne pas ajouter `includeSubDomains` ou `preload` sans
avoir vérifié tous les sous-domaines.

## Démarrer

```bash
docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  config --quiet

docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  up --build --detach --wait --wait-timeout 240
```

## Contrôler

```bash
curl --fail --silent --show-error http://127.0.0.1:8080/health
curl --fail --silent --show-error \
  http://127.0.0.1:8080/api/actuator/health
```

Après activation du domaine :

```bash
curl --fail --silent --show-error \
  https://itineclair.example.fr/api/actuator/health

curl --head https://itineclair.example.fr/
```

La réponse publique doit utiliser HTTPS et contenir CSP,
X-Content-Type-Options, X-Frame-Options, Referrer-Policy,
Permissions-Policy et HSTS.

## Journaux

```bash
docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  ps

docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  logs --since 30m --no-color api web postgres
```

Ne jamais publier des journaux sans les relire. Ils ne doivent pas contenir de
mot de passe, jeton, cookie, GPX ou coordonnée.

## Sauvegarder

```bash
./scripts/backup-postgres.sh
```

Ou avec des chemins explicites :

```bash
./scripts/backup-postgres.sh \
  /srv/itineclair/.env.production \
  /srv/backups/itineclair
```

Politique minimale :

- sauvegarde quotidienne conservée 7 jours ;
- sauvegarde hebdomadaire conservée 4 semaines ;
- sauvegarde mensuelle conservée 6 mois ;
- restauration testée après une modification de schéma puis au moins une fois
  par trimestre.

Les sauvegardes contiennent des données personnelles. Elles doivent être
chiffrées hors du serveur et accessibles uniquement aux personnes autorisées.

## Tester une restauration

Effectuer le test uniquement dans une base jetable nommée
`itineclair_restore_check`.

```bash
cd /srv/backups/itineclair
sha256sum --check \
  itineclair-YYYYMMDDTHHMMSSZ.dump.sha256

cd /srv/itineclair

docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  exec -T postgres sh -ceu '
    test "$POSTGRES_DB" != "itineclair_restore_check"
    dropdb --if-exists --force \
      --username="$POSTGRES_USER" \
      itineclair_restore_check
    createdb \
      --username="$POSTGRES_USER" \
      itineclair_restore_check
  '

docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  exec -T postgres sh -ceu '
    exec pg_restore \
      --exit-on-error \
      --no-owner \
      --no-privileges \
      --username="$POSTGRES_USER" \
      --dbname=itineclair_restore_check
  ' \
  < /srv/backups/itineclair/itineclair-YYYYMMDDTHHMMSSZ.dump

docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  exec -T postgres sh -ceu '
    exec psql \
      --username="$POSTGRES_USER" \
      --dbname=itineclair_restore_check \
      --command="SELECT COUNT(*) FROM flyway_schema_history;"
  '
```

Après contrôle :

```bash
docker compose \
  --env-file .env.production \
  --file compose.prod.yaml \
  exec -T postgres sh -ceu '
    exec dropdb \
      --if-exists \
      --force \
      --username="$POSTGRES_USER" \
      itineclair_restore_check
  '
```

## Mettre à jour

- vérifier que la CI du commit cible est verte ;
- externaliser une sauvegarde ;
- relever le commit actuellement déployé ;
- récupérer la version cible ;
- reconstruire avec `up --build --detach --wait` ;
- exécuter la checklist fonctionnelle ;
- surveiller les journaux et le healthcheck.

Une migration Flyway avance le schéma. Ne jamais remettre une ancienne image
applicative face à un schéma devenu incompatible.

## Incident minimum

- indisponibilité : conserver les journaux et arrêter le trafic public ;
- suspicion de fuite : révoquer les partages et faire tourner les secrets ;
- corruption : arrêter les écritures et restaurer dans un environnement séparé ;
- information montagne erronée : retirer immédiatement l’exposition publique
  et faire revoir la règle par une personne qualifiée.

Le frontal Caddy relaie automatiquement les requêtes et leurs informations de
proxy. [Documentation Caddy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy).
