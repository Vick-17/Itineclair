# Registre des fournisseurs de données externes

Dernière vérification : 2026-08-30.

Ce registre doit être relu avant toute mise en production et à chaque
changement de fournisseur, de contrat ou de finalité.

## Open-Meteo

| Élément | Décision Itinéclair |
|---|---|
| Finalité | Prévision horaire au premier point du GPX pendant la durée planifiée |
| Donnée transmise | Latitude et longitude du seul point de départ, date de début et de fin |
| Base légale produit | Appel uniquement après un consentement explicite et révocable dans le rapport |
| Données conservées | Agrégats utiles, source, horodatage, fenêtre de validité et altitude du modèle ; jamais la réponse brute |
| Endpoint de développement | `https://api.open-meteo.com` |
| Endpoint de production | `https://customer-api.open-meteo.com` avec clé et abonnement commercial |
| Licence des données | CC BY 4.0, attribution affichée dans chaque rapport |
| Limites techniques | 2 s de connexion, 5 s de lecture, horizon configuré à 16 jours |
| Défaillance | L’horaire et la lumière restent disponibles ; le rapport affiche une indisponibilité sans conclure sur la sécurité |
| Documentation | <https://open-meteo.com/en/docs> |
| Offre commerciale | <https://open-meteo.com/en/pricing> |

### Configuration

```text
ITINECLAIR_WEATHER_ENABLED=true
ITINECLAIR_WEATHER_BASE_URL=https://customer-api.open-meteo.com
ITINECLAIR_WEATHER_API_KEY=clé-fournie-par-open-meteo
```

Le profil `prod` laisse la météo désactivée par défaut. Il ne faut l’activer
qu’après souscription et validation juridique du contrat en vigueur.

## Commons Suncalc

| Élément | Décision Itinéclair |
|---|---|
| Finalité | Lever, coucher et crépuscule civil au point de départ |
| Traitement | Calcul entièrement local dans l’API ; aucun appel réseau |
| Version | `org.shredzone.commons:commons-suncalc:3.11` |
| Licence | Apache License 2.0 |
| Limite | Le relief, la végétation et l’horizon réel ne sont pas modélisés |
| Documentation | <https://shredzone.org/maven/commons-suncalc/> |
