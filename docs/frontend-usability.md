# Protocole d’utilisabilité du frontal

Dernière révision : 2026-09-15.

Ce protocole complète les tests Playwright. Un contrôle automatique repère des
défauts techniques fréquents, mais ne prouve ni la conformité WCAG complète ni
la facilité d’usage réelle. La validation finale exige donc les essais humains
ci-dessous.

## Exécuter les contrôles automatiques

La CI exécute ces contrôles contre la même stack conteneurisée que le smoke
test de production. Pour les reproduire localement, démarrer d’abord cette
stack en suivant `docs/production-runbook.md`, puis lancer depuis `web` :

```bash
npm ci
npx playwright install chromium
ITINECLAIR_E2E_BASE_URL=http://localhost:8080 npm run test:usability
```

Le rapport HTML et les traces d’échec sont écrits dans `playwright-report` et
`test-results`. GitHub Actions les conserve automatiquement lorsque le test
navigateur échoue.

## Préparer la session

Utiliser un compte et un GPX fictifs. Tester la version exacte qui sera
diffusée, sans extension de navigateur susceptible de modifier la page.

Consigner avant de commencer :

```text
Commit :
Date UTC :
Adresse testée :
Ordinateur / téléphone :
Navigateur et version :
Aide technique et version :
Personne qui teste :
```

## 1. Parcours sans souris

Sur ordinateur, ranger la souris puis utiliser uniquement `Tab`, `Maj+Tab`, les
flèches, `Entrée`, `Espace` et `Échap`.

- [ ] le premier appui sur `Tab` affiche « Aller au contenu principal » ;
- [ ] l’activation de ce lien place le focus au début du contenu ;
- [ ] le focus est toujours visible, y compris sur fond clair et foncé ;
- [ ] les flèches changent l’onglet « Se connecter / Créer un compte » ;
- [ ] tous les champs, cases, boutons, résumés et liens sont atteignables dans
      un ordre logique ;
- [ ] aucun composant ne piège le focus ;
- [ ] créer un compte, ouvrir « Mes sorties », importer un GPX, renseigner le
      départ et revenir à la liste est possible sans pointeur.

## 2. Lecteur d’écran sur ordinateur

Faire au moins un passage avec NVDA et Firefox ou Chrome sous Windows, ou avec
VoiceOver et Safari sous macOS.

- [ ] la voix française est sélectionnée automatiquement ;
- [ ] le titre de page annonce clairement Accueil, Mes sorties, Mon compte ou
      l’étape de préparation ;
- [ ] un seul contenu principal est annoncé ;
- [ ] la navigation annonce la page courante ;
- [ ] les niveaux de titres permettent de parcourir la page sans lire tout le
      contenu ;
- [ ] l’onglet de compte actif et l’étape de préparation courante sont annoncés ;
- [ ] chaque champ possède un nom, et son aide ou son erreur est lue avec lui ;
- [ ] les messages d’erreur et de réussite sont annoncés sans déplacer le
      focus de manière inattendue ;
- [ ] le statut d’une sortie et l’action suivante sont compréhensibles sans
      couleur ni symbole ;
- [ ] dans le rapport, « À regarder d’abord » arrive avant les détails et la
      limite « pas un feu vert » est entendue.

## 3. Téléphone et redistribution

Faire un passage avec TalkBack et Chrome sous Android, ou VoiceOver et Safari
sous iOS. Tester aussi une fenêtre de 320 pixels de large et un zoom navigateur
à 400 % sur une fenêtre de 1280 pixels.

- [ ] aucun défilement horizontal n’est nécessaire pour finir une tâche ;
- [ ] la barre Accueil / Mes sorties / Compte reste lisible et ne masque pas le
      contrôle qui reçoit le focus ;
- [ ] les trois destinations et l’action principale offrent une cible d’au
      moins 44 × 44 pixels ;
- [ ] l’affichage reste utilisable en orientation portrait ;
- [ ] le clavier virtuel ne masque pas le champ, l’erreur ou le bouton suivant ;
- [ ] le dépôt de fichier possède une alternative « Choisir un fichier » ;
- [ ] aucun geste complexe n’est indispensable.

## 4. Parcours d’une personne débutante

Donner uniquement un fichier GPX fictif à une personne qui ne connaît ni
Itinéclair ni les applications de randonnée. Dire : « Prépare cette sortie et
explique-moi ce que tu ferais ensuite. » Ne pas la guider pendant l’essai.

Le parcours est validé si la personne :

- [ ] trouve comment commencer depuis l’accueil ;
- [ ] importe le bon fichier et comprend les quatre repères de la trace ;
- [ ] distingue la date de départ du consentement météo facultatif ;
- [ ] atteint le rapport sans revenir en arrière par erreur ;
- [ ] trouve les points prioritaires avant les détails ;
- [ ] comprend que le rapport aide à préparer mais n’autorise pas à partir ;
- [ ] retrouve ensuite la sortie et identifie l’action suivante dans la liste.

Noter chaque hésitation, retour arrière, demande d’aide et mot incompris. Une
tâche terminée après une explication du facilitateur n’est pas considérée comme
autonome.

## 5. Compte rendu

```text
Parcours terminé sans aide : oui / non
Temps total :
Étape du premier blocage :
Nombre de retours arrière :
Mots ou libellés incompris :
Problèmes clavier :
Problèmes lecteur d’écran :
Problèmes mobile / zoom :
Corrections bloquantes avant diffusion :
Décision : validé / à corriger / à retester
```

Ne pas écrire « conforme WCAG » sur la seule base de ces contrôles. Une telle
affirmation demande un audit complet du périmètre publié et de ses contenus.
