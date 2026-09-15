# Checklist de sortie du MVP

Dernière revue : 2026-09-15.

## 1. Preuve logicielle

- [ ] la pull request vise `main` et `git diff --check` est vide ;
- [ ] les tests Maven passent ;
- [ ] `npm ci`, le lint, les tests et le build passent ;
- [ ] `npm run test:usability` passe contre la stack de production : clavier,
      structure accessible, parcours débutant et redistribution à 320 px ;
- [ ] `Production — smoke test` valide toute la stack ;
- [ ] aucun secret, `.env.production`, backup ou donnée réelle n’est commité ;
- [ ] les PR Dependabot sont examinées ;
- [ ] CodeQL ne contient aucune alerte critique ou élevée non évaluée.

## 2. Parcours manuel

- [ ] créer un compte et se reconnecter ;
- [ ] créer, modifier et supprimer le profil ;
- [ ] importer un GPX valide et refuser un fichier invalide ;
- [ ] vérifier distance, dénivelés, altitude et pentes ;
- [ ] planifier avec et sans consentement météo ;
- [ ] vérifier preuves, limites et statut prototype de l’analyse ;
- [ ] créer, modifier et supprimer le retour post-sortie ;
- [ ] créer, renouveler et révoquer un partage ;
- [ ] ouvrir le partage en navigation privée ;
- [ ] exporter et ouvrir l’archive ZIP ;
- [ ] supprimer le compte et confirmer la disparition de la session.

## 3. Utilisabilité et accessibilité

- [ ] le protocole manuel de `docs/frontend-usability.md` est exécuté sans
      souris sur ordinateur ;
- [ ] le même protocole est exécuté avec NVDA ou VoiceOver, puis avec TalkBack
      ou VoiceOver sur téléphone ;
- [ ] à 320 px de large et à 400 % de zoom, aucune tâche essentielle ne demande
      un défilement horizontal ;
- [ ] une personne qui ne connaît pas Itinéclair prépare une sortie depuis un
      GPX sans consigne orale et sait expliquer ce que le rapport ne garantit
      pas ;
- [ ] les anomalies, le navigateur, l’aide technique et la date du test sont
      consignés avant décision de diffusion.

## 4. Vie privée et sécurité

- [ ] le secret de partage reste uniquement dans le fragment `#` ;
- [ ] le rapport public ne contient ni identité, fichier, coordonnées, GPX,
      profil ni retour post-sortie ;
- [ ] les cookies CSRF et session portent `Secure` ;
- [ ] aucune route API ou PostgreSQL n’est exposée directement ;
- [ ] les journaux ne contiennent aucune donnée sensible ;
- [ ] les mentions de confidentialité sont complètes ;
- [ ] une personne est responsable des demandes d’accès et de suppression.

## 5. Exploitation

- [ ] HTTPS et le renouvellement du certificat sont testés ;
- [ ] HSTS est ajouté par le frontal TLS ;
- [ ] le healthcheck est surveillé depuis l’extérieur ;
- [ ] une sauvegarde chiffrée existe hors du serveur ;
- [ ] une restauration complète a réussi ;
- [ ] disque, journaux et certificat disposent d’une alerte ;
- [ ] la procédure de mise à jour est documentée ;
- [ ] un responsable et un contact d’incident existent.

## 6. Sécurité physique

- [ ] aucun écran ne parle de parcours « sûr » ou d’autorisation de partir ;
- [ ] les limites restent visibles ;
- [ ] les règles ont été relues par plusieurs professionnels qualifiés ;
- [ ] alertes, fermetures, avalanche et état du terrain utilisent des sources
      officielles, fraîches et attribuées ;
- [ ] une règle ou source douteuse peut être retirée immédiatement.

## 7. Décision

### Démonstration locale

Possible lorsque la section 1 est verte, avec des données fictives ou
explicitement consenties.

### Bêta privée encadrée

Possible lorsque les sections 1 à 5 sont cochées et que les participants
connaissent le statut prototype.

### Bêta publique

Interdite tant que toutes les sections ne sont pas cochées. Les risques
`PHY-002` et `PHY-005` restent bloquants.

## 8. Procès-verbal

```text
Version / commit :
Date UTC :
Environnement :
CI :
Sauvegarde :
Test de restauration :
Risques encore ouverts :
Niveau autorisé : démonstration / bêta privée / bêta publique
Décision prise par :
Motif et durée de validité :
```
