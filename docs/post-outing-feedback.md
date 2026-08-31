# Retour post-sortie

## Objectif

Le retour post-sortie compare rapidement le plan enregistré avec ce qui s’est
réellement passé sur le terrain. Il doit pouvoir être rempli en moins de
30 secondes depuis le rapport privé d’une trace.

Ce retour ne recalcule pas la sécurité d’une sortie et n’est jamais considéré
comme une observation objective ou vérifiée.

## Données conservées

Une trace possède au maximum un retour modifiable : résultat, durée réelle et
effort perçu facultatifs, comparaison des conditions, difficultés structurées,
ainsi que les dates de création et de modification. Le MVP ne collecte aucun
commentaire libre.

## Contrat HTTP

| Méthode | Route | Résultat |
|---|---|---|
| `GET` | `/tracks/{trackId}/feedback` | Retour ou `recorded: false` |
| `PUT` | `/tracks/{trackId}/feedback` | Création ou remplacement |
| `DELETE` | `/tracks/{trackId}/feedback` | Suppression, réponse `204` |

Toutes les routes sont privées. Les mutations exigent CSRF.

## Limites

- un retour seulement par trace ;
- données déclaratives, non vérifiées ;
- aucune utilisation automatique dans le moteur de règles ou en ML ;
- aucune sollicitation par e-mail dans le MVP.
