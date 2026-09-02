# Export et suppression des données du compte

Dernière revue : 2026-09-01.

## Périmètre MVP

Itinéclair permet à une personne authentifiée de récupérer une copie portable
de ses données et de supprimer définitivement son compte depuis l’interface.
Ces deux actions sensibles redemandent le mot de passe courant, appliquent la
protection CSRF de la PWA et partagent la limitation de tentatives de la
connexion.

| Action | Route | Confirmation | Résultat |
|---|---|---|---|
| Export | `POST /account/export` | mot de passe courant | ZIP privé, `no-store`, téléchargé comme pièce jointe |
| Suppression | `DELETE /account` | mot de passe courant + adresse e-mail exacte | `204`, session invalidée et cookies expirés |

Les réponses d’erreur ne recopient jamais le mot de passe ni l’adresse reçue.
Les coordonnées, GPX, adresses, mots de passe et jetons ne doivent pas être
écrits dans les journaux applicatifs.

## Contenu de l’archive

Le fichier `manifest.json` utilise le format versionné
`itineclair-account-export`, version de schéma `1`. Il contient :

- l’identifiant, l’adresse e-mail et la date de création du compte ;
- les métadonnées et faits calculés de chaque trace ;
- les plans, consentements et instantanés météo conservés ;
- les retours post-sortie structurés et leurs catégories ;
- les dates de création et d’expiration d’un partage privé, sans son secret ;
- le chemin du GPX associé dans l’archive.

Chaque `tracks/{uuid}.gpx` est reconstruit à partir des points validés et
conservés. Il préserve les segments, coordonnées, altitudes et horodatages
disponibles. Ce n’est pas une copie octet pour octet du fichier importé : les
extensions et métadonnées non retenues lors de l’import ne peuvent pas être
restituées.

Sont volontairement absents : hash du mot de passe, hash ou jeton de partage,
état de session et analyses calculées à la demande qui ne sont pas persistées.

## Suppression et garanties

La suppression retire la ligne `user_accounts` dans une transaction. Les clés
étrangères PostgreSQL avec `ON DELETE CASCADE` effacent ensuite les traces,
points, faits associés, contextes extérieurs, retours et partages. Aucune
migration supplémentaire n’est nécessaire.

Après succès, le serveur invalide toutes les sessions actives connues pour le
compte, puis expire explicitement dans la réponse courante :

- `ITINECLAIR_SESSION` sur `/api`, `HttpOnly`, `SameSite=Lax` ;
- `XSRF-TOKEN` sur `/`, `SameSite=Lax`.

Le registre de sessions est cohérent avec le déploiement MVP mono-instance et
ses sessions Servlet en mémoire. Avant un déploiement horizontal, les sessions
et leur révocation devront être déplacées vers un stockage partagé, par exemple
avec Spring Session.

Ce flux efface les données opérationnelles actuellement présentes dans
Itinéclair. Si de futures obligations légales imposent de conserver certaines
catégories (facturation, contentieux, etc.), elles devront être séparées,
documentées avec leur durée et exclues explicitement de cette cascade.

## Vérification avant fusion

1. exécuter `bash ./mvnw verify` dans `api` ;
2. exécuter `npm run lint`, `npm test` et `npm run build` dans `web` ;
3. exporter un compte contenant une trace, un plan, un retour et un partage ;
4. ouvrir le ZIP et contrôler le manifeste et le GPX reconstruit ;
5. chercher l’absence de `password`, `tokenHash` et du mot de passe de test ;
6. supprimer le compte puis vérifier `GET /auth/me` → `401` ;
7. vérifier en base qu’aucune ligne liée au compte n’existe encore.
