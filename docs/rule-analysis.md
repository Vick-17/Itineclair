# Moteur de règles explicable — version 1

Cette note est le contrat de la version 1 du moteur d’analyse d’Itinéclair.
Le moteur aide à trier les points à examiner ; il ne produit ni score global,
ni classement de difficulté, ni autorisation de partir.

## Statut et portée

- statut : `PROTOTYPE_AWAITING_EXPERT_REVIEW` ;
- portée : randonnée pédestre à la journée, hors alpinisme et glacier ;
- entrée : faits GPX versionnés, fenêtre planifiée, lumière calculée et,
  lorsqu’elle existe, prévision ponctuelle au départ ;
- sortie : signaux séparés, preuve numérique, seuil déclenché, explication,
  action proposée, limites et checklist ;
- relecture indispensable : les seuils doivent être examinés par plusieurs
  professionnels qualifiés avant toute exposition publique.

Les seuils ci-dessous sont des **heuristiques internes de tri du MVP**. Ils ne
sont ni des normes officielles ni des limites universelles. Ils sont
volontairement versionnés pour pouvoir être corrigés sans masquer un
changement de comportement.

## Niveaux

| Niveau | Sens dans l’interface |
|---|---|
| `NOTICE` | information manquante ou action à compléter |
| `CAUTION` | premier seuil interne déclenché, point à examiner |
| `STRONG_CAUTION` | second seuil interne déclenché, attention renforcée |

Ces niveaux ne sont jamais agrégés en un feu vert, un feu rouge ou une note.

## Seuils version 1

| Code | Mesure | `CAUTION` | `STRONG_CAUTION` |
|---|---|---:|---:|
| `DISTANCE_LOAD` | distance | ≥ 20 km | ≥ 35 km |
| `ELEVATION_GAIN_LOAD` | D+ | ≥ 1 000 m | ≥ 1 600 m |
| `HIGH_ALTITUDE` | altitude maximale GPX | ≥ 2 500 m | ≥ 3 000 m |
| `STEEP_GRADE` | pente absolue maximale sur fenêtre ≥ 25 m | ≥ 30 % | ≥ 50 % |
| `LONG_PLANNED_DURATION` | durée planifiée | ≥ 8 h | ≥ 12 h |
| `EXPECTED_DARKNESS` | temps hors crépuscule civil | > 0 min | ≥ 120 min |
| `WEATHER_FORECAST_STALE` | âge de la prévision | ≥ 6 h | ≥ 12 h |
| `WEATHER_ELEVATION_GAP` | point haut GPX − altitude du modèle | ≥ 500 m | ≥ 1 000 m |
| `STRONG_GUSTS_AT_START` | rafale maximale au départ | ≥ 50 km/h | ≥ 70 km/h |
| `PRECIPITATION_AT_START` | probabilité maximale | ≥ 50 % | ≥ 80 % |
| `PRECIPITATION_AT_START` | cumul sur la fenêtre | ≥ 5 mm | ≥ 15 mm |
| `SNOWFALL_AT_START` | neige sur la fenêtre | > 0 cm | ≥ 5 cm |
| `LOW_APPARENT_TEMPERATURE` | ressenti minimal | ≤ 5 °C | ≤ 0 °C |
| `HIGH_TEMPERATURE_AT_START` | température maximale | ≥ 30 °C | ≥ 35 °C |

Pour une règle météo avec plusieurs mesures, le niveau le plus prudent
déclenché est retenu et toutes les preuves ayant franchi au moins le premier
seuil sont exposées. Une absence de mesure ne vaut jamais zéro.

## Comportements dégradés

- altitude incomplète : signal de qualité ; les valeurs altimétriques restent
  présentées comme partielles ;
- horaire absent : aucune règle de lumière ou de météo n’est exécutée ;
- météo non demandée ou hors horizon : état explicite, sans valeur inventée ;
- fournisseur indisponible : `CAUTION`, sans bloquer les faits GPX et la
  lumière locale ;
- prévision présente : la checklist reste `PARTIAL`, car il ne s’agit que du
  départ et non du parcours complet.

## Contrôles toujours humains

Le moteur conserve systématiquement dans la checklist :

- alertes officielles, fermetures et réglementation ;
- état du terrain, exposition et passages techniques ;
- niveau du membre le moins expérimenté, santé, matériel et plan de repli ;
- bulletin montagne, neige ou avalanche lorsqu’il est pertinent.

## Sources de cadrage

- Le ministère chargé des Sports rappelle que la montagne cumule météo
  changeante, variations de température, dénivelé, fatigue et isolement, et
  recommande une activité adaptée au membre le plus faible, la consultation
  régulière de la météo, un itinéraire de remplacement et la possibilité de
  renoncer : https://www.sports.gouv.fr/prevention-montagne-ete
- La Vigilance Météo-France complète les prévisions pour signaler les
  phénomènes dangereux et leurs précautions :
  https://meteofrance.com/vigilance-et-securite
- La FFRandonnée rappelle que matériel, vêtements, orientation, hydratation
  et préparation physique dépendent du parcours et du pratiquant :
  https://www.ffrandonnee.fr/randonner/conseils/bien-preparer-sa-randonnee

Ces sources cadrent les dimensions et les vérifications à conserver. Elles ne
valident pas les seuils numériques internes, qui restent à faire relire.

## Preuves de validation

- tests paramétrés aux frontières de seuil ;
- tests de météo absente, indisponible et vieillissante ;
- test d’altitude partielle ;
- test d’isolation par propriétaire ;
- test HTTP sans authentification ;
- lint, tests et build du client ;
- CI Java 25 et Node.js 24 avant fusion.
