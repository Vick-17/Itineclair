# Profil de pratique privé

Dernière revue : 2026-09-04.

## Objectif du MVP

Le profil conserve quelques repères habituels de randonnée. Il est
facultatif, privé, auto-déclaré, modifiable et supprimable
indépendamment du compte.

Il ne constitue pas une certification, un avis médical ou une
autorisation de partir. Il ne modifie ni le moteur de règles, ni le
rapport d’une trace, ni un rapport partagé.

## Données conservées

| Champ | Obligatoire | Limites |
|---|---:|---|
| Niveau auto-déclaré | oui si le profil existe | quatre valeurs fermées |
| Durée habituelle | non | 15 à 1 440 minutes |
| Distance habituelle | non | 500 à 100 000 mètres |
| Dénivelé positif habituel | non | 0 à 10 000 mètres |
| Dates de création et modification | serveur | instants UTC |

Le MVP ne demande ni âge, poids, état de santé, handicap,
certification, terrain maîtrisé ou commentaire libre.

## Contrat HTTP

| Méthode | Route | Résultat |
|---|---|---|
| GET | `/profile` | profil ou `configured: false` |
| PUT | `/profile` | création ou remplacement |
| DELETE | `/profile` | suppression, réponse 204 |

L’identifiant du compte vient exclusivement de la session.

## Confidentialité

Le profil est inclus dans l’export du compte à partir du schéma 2.
Il est supprimé avec le compte grâce à `ON DELETE CASCADE`.
Il n’est jamais inclus dans un rapport partagé.
