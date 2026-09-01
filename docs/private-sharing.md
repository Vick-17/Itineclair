# Partage privé d’un rapport

Dernière revue : 2026-09-01.

## Périmètre MVP

Le partage est une capacité de lecture temporaire. Il ne transforme jamais
une trace en ressource publique indexable et ne donne accès ni au compte du
propriétaire ni au GPX.

Le parcours impose l’ordre suivant :

1. le propriétaire ouvre un aperçu calculé avec le contrat public exact ;
2. il choisit 7 jours par défaut ou 30 jours au maximum ;
3. une action explicite crée ou renouvelle le lien ;
4. le secret n’est affiché qu’à cette création ;
5. le propriétaire peut révoquer le partage immédiatement.

Un renouvellement remplace le secret précédent. Il n’existe donc qu’un seul
lien actif par trace dans le MVP.

## Données incluses et exclues

| Inclus dans la vue partagée | Toujours exclu |
|---|---|
| Faits agrégés : distance, dénivelés, altitude, pente et couverture | Identité, e-mail et identifiant du compte |
| Horaire planifié, lumière et agrégats météo déjà consentis | Fichier source, points GPX, coordonnées et géométrie |
| Signaux explicables, preuves numériques, checklist et limites | Nom du fichier, identifiants techniques et date d’import |
| Source et fraîcheur de la météo | Retour personnel post-sortie |

Le titre issu du GPX est également exclu : il pourrait contenir un nom de lieu
privé ou une mention comme « départ maison ».

## Secret de partage

- génération : 32 octets de `SecureRandom`, soit 256 bits ;
- transport dans le lien : Base64 URL sans remplissage, 43 caractères ;
- lien navigateur : `/#/share/{token}` afin que le fragment ne soit pas envoyé
  au serveur lors du chargement de la PWA ;
- appel API : chemin fixe `GET /shared-report` et secret dans
  `X-Itineclair-Share-Token` ;
- stockage : SHA-256 hexadécimal uniquement dans `token_hash` ;
- réponse : `Cache-Control: no-store`, `Referrer-Policy: no-referrer` et
  `X-Robots-Tag: noindex, nofollow` ;
- erreur publique uniforme pour un secret absent, invalide, expiré ou révoqué.

SHA-256 convient ici parce que l’entrée n’est pas un mot de passe humain mais
un secret aléatoire de 256 bits. Le hachage évite qu’une lecture seule de la
base permette d’utiliser directement les liens actifs.

## Cycle de vie et suppression

La table `track_shares` lie `track_id` et `owner_id` par une clé étrangère
composite. Une suppression du compte ou de la trace supprime donc le partage
en cascade. Une tâche UTC retire quotidiennement les lignes expirées ; le
propriétaire peut les supprimer plus tôt avec `DELETE /tracks/{id}/share`.

Le jeton brut n’est jamais relu : après rechargement, l’interface montre le
statut et l’expiration mais exige une rotation pour obtenir un nouveau lien.

## Contrats HTTP

| Méthode | Route | Accès | Résultat |
|---|---|---|---|
| `GET` | `/tracks/{id}/share` | propriétaire | statut et expiration |
| `GET` | `/tracks/{id}/share/preview` | propriétaire | contrat public exact, sans secret |
| `POST` | `/tracks/{id}/share` | propriétaire + CSRF | nouveau secret affiché une fois |
| `DELETE` | `/tracks/{id}/share` | propriétaire + CSRF | révocation immédiate |
| `GET` | `/shared-report` | détenteur du secret | rapport réduit en lecture seule |

## Preuves attendues avant fusion

- tests unitaires de forme, unicité et hachage du secret ;
- tests de propriété, rotation, expiration et révocation ;
- tests MVC anonymes et authentifiés, notamment CSRF négatif ;
- assertions d’absence du nom, du fichier, du secret et du feedback ;
- vérification des en-têtes anti-cache et anti-indexation ;
- tests du client confirmant que le secret n’entre jamais dans le chemin API ;
- scénario manuel dans une fenêtre privée avant puis après révocation.
