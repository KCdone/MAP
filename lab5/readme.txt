lab5 - social network (MAP 2026-2027)
continua lab4: aceeasi aplicatie (domain / service / ui neschimbate), dar datele pot fi
tinute si intr-o baza de date PostgreSQL (UserDbRepository, FriendshipDbRepository).

how to run
- baza de date: in pgAdmin, Query Tool, ruleaza intai "CREATE DATABASE socialnetwork;",
  apoi deschide o conexiune noua la socialnetwork si ruleaza restul din data/schema.sql
  (creeaza tabelele users si friendships).
- copiaza src/main/resources/config.example.properties -> config.properties si pune
  parola reala la db.password (config.properties e in .gitignore, nu se urca pe git).
- File -> Open -> folderul lab5 (IntelliJ il recunoaste ca proiect Gradle)
- run Main (working dir = lab5, ca sa gaseasca data/users.txt si data/friendships.txt
  cand repository.type=FILE)
- tipul de persistenta se alege din config.properties (repository.type: MEMORY, FILE
  sau DATABASE); implicit e DATABASE.

1. schema bazei de date
- users(id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, username VARCHAR NOT NULL
  UNIQUE, first_name VARCHAR NOT NULL, last_name VARCHAR NOT NULL, email VARCHAR NOT NULL).
  Enuntul cere doar id/first_name/last_name; am pastrat si username si email pentru ca
  domain.User le are inca din lab2 si functionalitatile (addFriendship dupa username,
  email in UserValidator) nu s-au schimbat la lab5 - tabela trebuia sa le poata stoca.
- friendships(id1 BIGINT, id2 BIGINT, friends_from TIMESTAMP NOT NULL), cheie primara
  (id1, id2), CHECK (id1 < id2), exact cum cere enuntul; id1/id2 chei straine catre
  users(id).

2. ON DELETE CASCADE vs stergere explicita in service
- am pastrat ambele, dar nu se bat cap in cap:
  NetworkService.removeUser() sterge explicit prieteniile userului inainte de a sterge
  userul - asta exista neschimbat din lab2, pentru ca MEMORY si FILE nu au nicio
  constrangere de baza de date care sa faca asta singura.
  ON DELETE CASCADE e pus in plus pe foreign key, ca plasa de siguranta doar pentru
  DATABASE: daca cineva sterge un user direct din pgAdmin (nu prin aplicatie), prieteniile
  lui nu raman orfane in tabela friendships.
- in practica, cand stergerea vine din aplicatie, service-ul goleste deja prieteniile,
  asa ca CASCADE nu mai are ce sa stearga; el conteaza doar pentru stergerile facute
  direct in baza de date.

3. id-ul utilizatorilor
- NetworkService.addUser() calculeaza in continuare un id provizoriu (nextUserId(),
  ca la lab2-4), ca sa treaca de UserValidator (care cere id pozitiv) fara sa-l modific.
- UserDbRepository.save() insereaza user-ul fara coloana id (id e GENERATED ALWAYS AS
  IDENTITY, deci oricum nu poate fi setat de aplicatie), citeste id-ul generat cu
  getGeneratedKeys() si il pune pe entitate cu setId(); id-ul provizoriu calculat de
  service e astfel suprascris cu cel din baza de date inainte sa fie afisat in UI.
  Pentru MEMORY/FILE id-ul ramane cel calculat de service (nu exista alta sursa).

4. tratarea exceptiilor
- toate PreparedStatement/ResultSet/Connection se deschid in try-with-resources, cate
  o conexiune noua per metoda (nu se tine o conexiune deschisa intre apeluri).
- orice SQLException (baza oprita, constrangere incalcata - username duplicat, CHECK
  id1<id2, foreign key inexistenta) e impachetata in RepositoryException cu un mesaj
  descriptiv; ConsoleUI.run() prinde SocialNetworkException si afiseaza doar mesajul,
  fara stack trace (neschimbat din lab2).

5. SQL injection
- interogarile UserDbRepository/FriendshipDbRepository folosesc exclusiv PreparedStatement
  cu "?", nicio valoare nu e concatenata in textul SQL, deci un nume ca
  "'); DROP TABLE users; --" ar fi trimis ca parametru si stocat ca text obisnuit, nu
  executat.
- in plus, UserValidator (neschimbat din lab2) respinge deja astfel de caractere in
  first_name/last_name/username inainte sa ajunga la baza de date (prima linie de
  aparare); pentru demonstratia la predare, acest exemplu se insereaza direct in
  pgAdmin (ocolind validarea din UI) si se arata ca aplicatia il citeste si il afiseaza
  ca text simplu, fara niciun efect asupra tabelei.

6. ce NU s-a schimbat fata de lab4
- domain, domain.validators, exceptions, service, ui - identice; RepositoryFactory avea
  deja intrarile pentru DATABASE din lab4 (skeleton), aici doar s-au implementat
  UserDbRepository si FriendshipDbRepository.
- test data pentru FILE/MEMORY (data/users.txt, data/friendships.txt) neschimbate.
