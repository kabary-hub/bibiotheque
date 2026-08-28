@echo off
setlocal enabledelayedexpansion

set BASE=http://localhost:8080
set TOKEN=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImV4cCI6MTc4NzkzMTUwNywiaWF0IjoxNzg3OTEzNTA3fQ.V37X-fy_3ASMLqwnIjvQnIgjBBEQcGSEWivctaiFy9C-xQGYThVQnC6clx_7Z6_OMAs68cQ7hjPnD3ziW3sZZQ
set AUTH=Authorization: Bearer %TOKEN%

echo.
echo ============================================================
echo  1. AUTHENTIFICATION
echo ============================================================

echo.
echo --- POST /authenticate (admin) ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/authenticate ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"admin123\"}"

echo.
echo --- POST /authenticate (mauvais mot de passe) ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/authenticate ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"admin\",\"password\":\"mauvais\"}"

echo.
echo ============================================================
echo  2. LIVRES  /admin/books
echo ============================================================

echo.
echo --- GET /admin/books (liste) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/books ^
  -H "%AUTH%"

echo.
echo --- GET /admin/books/201 (un livre) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/books/201 ^
  -H "%AUTH%"

echo.
echo --- GET /admin/books/9999 (404) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/books/9999 ^
  -H "%AUTH%"

echo.
echo --- POST /admin/books (creer) ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/admin/books ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"bookName\":\"Test curl\",\"bookAuthor\":\"Auteur Test\",\"bookGenre\":\"Test\",\"noOfCopies\":2}"

echo.
echo --- PUT /admin/books/201 (modifier) ---
curl -s -w "\nHTTP %%{http_code}\n" -X PUT %BASE%/admin/books/201 ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"bookName\":\"L1 - Le Comte de Monte-Cristo\",\"bookAuthor\":\"Alexandre Dumas\",\"bookGenre\":\"Roman\",\"noOfCopies\":3}"

echo.
echo ============================================================
echo  3. UTILISATEURS  /admin/users
echo ============================================================

echo.
echo --- GET /admin/users (liste) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/users ^
  -H "%AUTH%"

echo.
echo --- GET /admin/users/1 (admin) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/users/1 ^
  -H "%AUTH%"

echo.
echo --- POST /admin/users (creer lecteur) ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/admin/users ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"username\":\"testcurl\",\"name\":\"Test Curl\",\"password\":\"pass123\",\"role\":[{\"roleName\":\"User\"}]}"

echo.
echo ============================================================
echo  4. EMPRUNTS  /borrow
echo ============================================================

echo.
echo --- GET /borrow (tous les emprunts) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/borrow ^
  -H "%AUTH%"

echo.
echo --- GET /borrow/user/303 (emprunts de A3) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/borrow/user/303 ^
  -H "%AUTH%"

echo.
echo --- GET /borrow/book/202 (historique L2) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/borrow/book/202 ^
  -H "%AUTH%"

echo.
echo ============================================================
echo  5. RESERVATIONS  /api/reservations
echo ============================================================

echo.
echo --- GET /api/reservations (liste vide au depart) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/api/reservations ^
  -H "%AUTH%"

echo.
echo --- POST /api/reservations sur L2 (0 exemplaire) - doit reussir 201 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":202,\"adherentId\":301}"

echo.
echo --- POST /api/reservations sur L3 pour A1 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":203,\"adherentId\":301}"

echo.
echo --- POST /api/reservations sur L4 pour A1 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":204,\"adherentId\":301}"

echo.
echo --- RG-01 : reserver L1 (3 exemplaires en rayon) - doit echouer 409 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":201,\"adherentId\":301}"

echo.
echo --- RG-02 : reserver L2 une 2e fois pour A1 - doit echouer 409 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":202,\"adherentId\":301}"

echo.
echo --- RG-03 : 4e reservation pour A1 (quota 3) - doit echouer 409 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":205,\"adherentId\":301}"

echo.
echo --- RG-04 : reserver livre inexistant - doit echouer 404 ---
curl -s -w "\nHTTP %%{http_code}\n" -X POST %BASE%/api/reservations ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json" ^
  -d "{\"livreId\":9999,\"adherentId\":301}"

echo.
echo --- GET /api/reservations (liste apres creations) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/api/reservations ^
  -H "%AUTH%"

echo.
echo --- GET /api/reservations?statut=EN_ATTENTE ---
curl -s -w "\nHTTP %%{http_code}\n" "%BASE%/api/reservations?statut=EN_ATTENTE" ^
  -H "%AUTH%"

echo.
echo --- GET /api/reservations?statut=HONOREE (liste vide) ---
curl -s -w "\nHTTP %%{http_code}\n" "%BASE%/api/reservations?statut=HONOREE" ^
  -H "%AUTH%"

echo.
echo --- GET /api/reservations/1 (consulter reservation 1) ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/api/reservations/1 ^
  -H "%AUTH%"

echo.
echo --- PATCH /api/reservations/1/annuler ---
curl -s -w "\nHTTP %%{http_code}\n" -X PATCH %BASE%/api/reservations/1/annuler ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json"

echo.
echo --- RG-05 : annuler une 2e fois la reservation 1 - doit echouer 409 ---
curl -s -w "\nHTTP %%{http_code}\n" -X PATCH %BASE%/api/reservations/1/annuler ^
  -H "%AUTH%" ^
  -H "Content-Type: application/json"

echo.
echo --- GET /api/reservations/expirees ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/api/reservations/expirees ^
  -H "%AUTH%"

echo.
echo --- DELETE /api/reservations/2 ---
curl -s -w "\nHTTP %%{http_code}\n" -X DELETE %BASE%/api/reservations/2 ^
  -H "%AUTH%"

echo.
echo --- GET /api/reservations/2 apres suppression - doit echouer 404 ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/api/reservations/2 ^
  -H "%AUTH%"

echo.
echo ============================================================
echo  6. SECURITE
echo ============================================================

echo.
echo --- GET /admin/books sans token - doit echouer 401 ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/books

echo.
echo --- GET /admin/books avec token invalide - doit echouer 401 ---
curl -s -w "\nHTTP %%{http_code}\n" %BASE%/admin/books ^
  -H "Authorization: Bearer tokenbidon"

echo.
echo ============================================================
echo  TERMINE
echo ============================================================
