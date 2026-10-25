package ci.inphb.election.persistance;

import ci.inphb.election.modele.Identifiable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dépôt stocké dans un fichier texte CSV (séparateur ';', encodage UTF-8,
 * première ligne = en-tête). Les sous-classes décrivent seulement la conversion
 * entre un objet et les champs d'une ligne.
 */
public abstract class DepotFichierCsv<T extends Identifiable> implements Depot<T> {

    protected static final String SEPARATEUR = ";";

    private final Path fichier;

    protected DepotFichierCsv(Path fichier) {
        this.fichier = fichier;
    }

    public Path getFichier() {
        return fichier;
    }

    protected abstract String getEntete();

    protected abstract String[] versChamps(T element);

    protected abstract T depuisChamps(String[] champs);

    /** Lit le fichier ; un fichier absent donne une liste vide. */
    @Override
    public List<T> chargerTout() throws PersistanceException {
        List<T> elements = new ArrayList<>();
        if (!Files.exists(fichier)) {
            return elements;
        }
        List<String> lignes;
        try {
            lignes = Files.readAllLines(fichier, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistanceException("Lecture impossible de " + fichier + " : " + e.getMessage(), e);
        }
        for (int i = 1; i < lignes.size(); i++) {
            String ligne = lignes.get(i);
            if (ligne.isBlank()) {
                continue;
            }
            try {
                elements.add(depuisChamps(ligne.split(SEPARATEUR, -1)));
            } catch (RuntimeException e) {
                throw new PersistanceException(fichier.getFileName() + ", ligne " + (i + 1) + " invalide : " + e.getMessage(), e);
            }
        }
        return elements;
    }

    @Override
    public void sauvegarderTout(Collection<? extends T> elements) throws PersistanceException {
        List<String> lignes = new ArrayList<>();
        lignes.add(getEntete());
        for (T element : elements) {
            lignes.add(String.join(SEPARATEUR, versChamps(element)));
        }
        try {
            Path parent = fichier.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(fichier, lignes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistanceException("Écriture impossible de " + fichier + " : " + e.getMessage(), e);
        }
    }

    protected static void verifierNombreChamps(String[] champs, int attendu) {
        if (champs.length != attendu) {
            throw new IllegalArgumentException(attendu + " champs attendus, " + champs.length + " trouvés");
        }
    }
}
