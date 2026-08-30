# ADR 0001 — Fournisseur météo hybride et lumière locale

- Statut : accepté
- Date : 2026-08-30

## Contexte

Le MVP doit enrichir une trace avec une date, un horaire, la lumière et une
prévision météo. Les coordonnées d’une randonnée sont sensibles, une panne
d’un tiers ne doit pas rendre le rapport inutilisable et l’endpoint gratuit
d’Open-Meteo ne couvre pas un produit commercial en production.

## Décision

L’application utilise un port `WeatherForecastProvider` et un adaptateur
Open-Meteo remplaçable.

- En développement, l’endpoint libre sert au prototypage non commercial.
- En production, la météo est désactivée par défaut. L’activation nécessite
  l’endpoint client, une clé et un abonnement commercial.
- Le contrat HTTP Open-Meteo étant identique entre les deux offres, seule la
  configuration change.
- La coordonnée du départ n’est transmise que lors d’un enregistrement avec
  consentement coché. Retirer ce choix efface la prévision et le consentement.
- Les lever, coucher et crépuscule sont calculés localement avec Commons
  Suncalc, donc restent disponibles sans réseau.
- Les réponses météo sont réduites à des agrégats factuels sur la fenêtre de
  sortie. La réponse brute et les coordonnées ne sont pas exposées au front.
- Une erreur fournisseur devient un état explicite `UNAVAILABLE` ; elle ne
  bloque pas le plan et ne produit aucun verdict de sécurité.

## Conséquences

Cette approche minimise les données partagées, évite un verrouillage fort au
fournisseur et permet un passage en production sans réécrire le métier. Elle
impose de maintenir l’attribution, le registre fournisseur, les paramètres de
contrat et les tests d’adaptateur.
