# Captures de l'epreuve — seance 3

L'enonce en demande quatre dans la description de la Pull Request.

| Fichier attendu | Ce qu'il doit montrer | Comment l'obtenir |
|---|---|---|
| `chargement.png` | L'indicateur de chargement a la place du tableau | DevTools → Network → throttling « Slow 3G », puis recharger `/reservations` |
| `liste-remplie.png` | Le tableau, ses six colonnes, les badges de statut | Charger les fixtures, creer deux ou trois reservations |
| `liste-vide.png` | « Aucune reservation » et le message qui explique qu'un filtre est actif | Filtrer sur **Honoree** |
| `refus-409.png` | Le message du serveur sous le formulaire, precede de sa regle en gras | Reserver **L1 - Le Comte de Monte-Cristo**, qui a 3 exemplaires en rayon → RG-01 |

## Attention en collant la description

Ces images ne s'afficheront **pas** via un chemin relatif dans le corps de la
Pull Request : GitHub n'y resout pas les chemins du depot. Il faut les
glisser-deposer dans l'editeur de la PR, ce qui les heberge et insere une URL
absolue.

## Reproduire l'environnement

```powershell
docker compose up --build -d
Get-Content epreuve\seance-2\fixtures.sql | docker exec -i bibliotheque-db mysql -u root -pmysql bibliotheque
```

Puis <http://localhost:4200>, connexion `admin` / `admin123`, menu
**Reservations**.
