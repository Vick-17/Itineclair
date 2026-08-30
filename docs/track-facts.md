# Calcul des faits d’une trace

Cette note décrit la version 1 des calculs effectués par Itinéclair après
l’import d’un fichier GPX.

## Référentiel géographique

Le format GPX exprime les coordonnées dans le référentiel WGS84 et les
mesures en unités métriques. La distance horizontale entre deux points est
calculée sur l’ellipsoïde WGS84 avec l’algorithme inverse de GeographicLib,
puis additionnée à l’intérieur de chaque segment.

- GPX 1.1 : https://www.topografix.com/gpx/1/1/
- GeographicLib Java :
  https://geographiclib.sourceforge.io/html/java/net/sf/geographiclib/Geodesic.html

Deux segments distincts ne sont jamais reliés par une distance artificielle.

## Altitude et dénivelé

Les altitudes présentes dans le GPX sont exprimées en mètres.

- le D+ additionne les différences positives entre deux altitudes
  consécutives disponibles ;
- le D− additionne la valeur absolue des différences négatives ;
- les altitudes minimale et maximale utilisent tous les échantillons
  disponibles ;
- une altitude manquante coupe la continuité du calcul : Itinéclair
  n’interpole pas une valeur qu’il ne connaît pas.

Le résultat peut donc être partiel si le fichier ne fournit pas l’altitude
de tous ses points. La qualité dépend aussi du capteur et de l’export GPX.
La version 1 n’applique pas de lissage ni de modèle numérique de terrain.

## Pente

La pente est le rapport entre la variation d’altitude et la distance
horizontale, exprimé en pourcentage. Pour réduire les pics produits par des
coordonnées presque superposées, les maxima montant et descendant sont
calculés sur la fenêtre glissante la plus proche couvrant au moins 25 mètres.

Une rupture de segment ou une altitude manquante réinitialise cette fenêtre.

## Évolution et recalcul

Chaque résultat stocke sa version de calcul. Une trace importée avant
l’ajout des faits, ou calculée avec une ancienne version, est recalculée à
partir de ses points lors de sa prochaine consultation. Ce mécanisme permet
de faire évoluer la méthode sans rendre les anciennes traces incohérentes.
