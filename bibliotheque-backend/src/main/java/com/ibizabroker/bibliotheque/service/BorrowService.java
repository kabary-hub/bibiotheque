package com.ibizabroker.bibliotheque.service;

import com.ibizabroker.bibliotheque.dao.BooksRepository;
import com.ibizabroker.bibliotheque.dao.BorrowRepository;
import com.ibizabroker.bibliotheque.dao.UsersRepository;
import com.ibizabroker.bibliotheque.dto.BorrowRequestDto;
import com.ibizabroker.bibliotheque.dto.BorrowResponseDto;
import com.ibizabroker.bibliotheque.entity.Books;
import com.ibizabroker.bibliotheque.entity.Borrow;
import com.ibizabroker.bibliotheque.entity.Users;
import com.ibizabroker.bibliotheque.exceptions.BusinessRuleException;
import com.ibizabroker.bibliotheque.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regles de gestion des emprunts.
 *
 * Le controleur portait toute cette logique et souffrait de trois defauts que
 * cette couche corrige :
 *
 *   1. Un livre epuise renvoyait la phrase « The book ... is out of stock! »
 *      avec un code HTTP 200. Un echec annonce comme un succes : le client ne
 *      pouvait le detecter qu'en comparant du texte anglais.
 *   2. Optional.get() sans controle : un identifiant inconnu produisait une 500
 *      au lieu d'un 404.
 *   3. Aucune transaction. Le decompte d'exemplaire etait enregistre avant la
 *      ligne d'emprunt ; un echec entre les deux retirait un exemplaire du
 *      catalogue sans trace de qui le detenait.
 */
@Service
public class BorrowService {

    /** Duree d'un pret, en jours. Etait un « 7 » nu au milieu du controleur. */
    public static final int DUREE_PRET_JOURS = 7;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private BooksRepository booksRepository;

    @Transactional(readOnly = true)
    public List<BorrowResponseDto> lister() {
        return enrichir(borrowRepository.findAll());
    }

    @Transactional(readOnly = true)
    public List<BorrowResponseDto> listerParAdherent(Integer userId) {
        return enrichir(borrowRepository.findByUserId(userId));
    }

    @Transactional(readOnly = true)
    public List<BorrowResponseDto> listerParLivre(Integer bookId) {
        return enrichir(borrowRepository.findByBookId(bookId));
    }

    /**
     * Enregistre un emprunt et retire un exemplaire du rayon.
     *
     * @Transactional : les deux ecritures reussissent ensemble ou echouent
     * ensemble. C'est la seule facon de garantir que le compteur d'exemplaires
     * reste le reflet exact des emprunts en cours — ce dont RG-01 depend
     * entierement pour decider si un livre est reservable.
     */
    @Transactional
    public BorrowResponseDto emprunter(BorrowRequestDto demande) {
        Users emprunteur = trouverAdherent(demande.getUserId());
        Books livre = trouverLivre(demande.getBookId());

        if (livre.getNoOfCopies() == null || livre.getNoOfCopies() < 1) {
            // 409 et non 200 : la demande est legitime, c'est l'etat du stock
            // qui s'y oppose. La meme demande reussira apres une restitution.
            throw new BusinessRuleException(String.format(
                    "Le livre « %s » n'a plus d'exemplaire disponible. "
                            + "Il peut en revanche etre reserve.",
                    livre.getBookName()));
        }

        livre.borrowBook();
        booksRepository.save(livre);

        Date maintenant = new Date();
        Calendar echeance = Calendar.getInstance();
        echeance.setTime(maintenant);
        echeance.add(Calendar.DATE, DUREE_PRET_JOURS);

        Borrow emprunt = new Borrow();
        emprunt.setBookId(livre.getBookId());
        emprunt.setUserId(emprunteur.getUserId());
        emprunt.setIssueDate(maintenant);
        emprunt.setDueDate(echeance.getTime());
        emprunt.setReturnDate(null);

        return BorrowResponseDto.de(borrowRepository.save(emprunt), livre, emprunteur);
    }

    /**
     * Enregistre la restitution et remet l'exemplaire en rayon.
     *
     * Le controle « deja rendu » manquait : rejouer la meme restitution
     * incrementait le compteur d'exemplaires a chaque appel. Le catalogue
     * finissait par annoncer plus d'exemplaires que la bibliotheque n'en possede.
     */
    @Transactional
    public BorrowResponseDto restituer(Integer borrowId) {
        Borrow emprunt = borrowRepository.findById(borrowId).orElseThrow(
                () -> new NotFoundException(
                        String.format("L'emprunt d'identifiant %d n'existe pas.", borrowId)));

        if (emprunt.getReturnDate() != null) {
            throw new BusinessRuleException(
                    "Cet emprunt a deja ete restitue : le rendre une seconde fois "
                            + "ajouterait un exemplaire inexistant au catalogue.");
        }

        Books livre = trouverLivre(emprunt.getBookId());
        livre.returnBook();
        booksRepository.save(livre);

        emprunt.setReturnDate(new Date());
        Users emprunteur = usersRepository.findById(emprunt.getUserId()).orElse(null);

        return BorrowResponseDto.de(borrowRepository.save(emprunt), livre, emprunteur);
    }

    /**
     * Resout les titres et les noms en deux requetes, pas en deux par ligne.
     *
     * Ecrire la resolution ligne par ligne serait plus court et produirait
     * 2N+1 requetes sur une liste de N emprunts.
     */
    private List<BorrowResponseDto> enrichir(List<Borrow> emprunts) {
        Map<Integer, Books> livres = booksRepository.findAll().stream()
                .collect(Collectors.toMap(Books::getBookId, Function.identity()));
        Map<Integer, Users> adherents = usersRepository.findAll().stream()
                .collect(Collectors.toMap(Users::getUserId, Function.identity()));

        return emprunts.stream()
                .map(e -> BorrowResponseDto.de(e,
                        livres.get(e.getBookId()),
                        adherents.get(e.getUserId())))
                .collect(Collectors.toList());
    }

    private Users trouverAdherent(Integer id) {
        return usersRepository.findById(id).orElseThrow(() -> new NotFoundException(
                String.format("L'adherent d'identifiant %d n'existe pas.", id)));
    }

    private Books trouverLivre(Integer id) {
        return booksRepository.findById(id).orElseThrow(() -> new NotFoundException(
                String.format("Le livre d'identifiant %d n'existe pas.", id)));
    }
}
