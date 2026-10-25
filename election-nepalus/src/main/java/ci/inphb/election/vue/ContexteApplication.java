package ci.inphb.election.vue;

import ci.inphb.election.modele.Election;
import ci.inphb.election.persistance.ConfigurationStockage;
import ci.inphb.election.persistance.StockageElection;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * État partagé par tous les contrôleurs : l'élection en cours, le stockage actif
 * (fichiers CSV ou base MySQL), et des propriétés observables :
 * <ul>
 *   <li>{@code revision} est incrémentée à chaque modification ; les onglets l'écoutent pour se rafraîchir ;</li>
 *   <li>{@code modifie} indique des changements non enregistrés ;</li>
 *   <li>{@code descriptionStockage} décrit le stockage actif (affichée dans la barre d'état).</li>
 * </ul>
 */
public class ContexteApplication {

    private Election election;
    private StockageElection stockage;
    private final ConfigurationStockage configuration;
    private Navigation navigation;
    private final IntegerProperty revision = new SimpleIntegerProperty(0);
    private final BooleanProperty modifie = new SimpleBooleanProperty(false);
    private final StringProperty descriptionStockage = new SimpleStringProperty();

    public ContexteApplication(Election election, ConfigurationStockage configuration, StockageElection stockage) {
        this.election = election;
        this.configuration = configuration;
        setStockage(stockage);
    }

    public Election getElection() {
        return election;
    }

    public StockageElection getStockage() {
        return stockage;
    }

    public final void setStockage(StockageElection stockage) {
        this.stockage = stockage;
        descriptionStockage.set(stockage.getDescription());
    }

    public ConfigurationStockage getConfiguration() {
        return configuration;
    }

    public Navigation getNavigation() {
        return navigation;
    }

    public void setNavigation(Navigation navigation) {
        this.navigation = navigation;
    }

    /** Remplace l'élection (rechargement, données de démonstration, ...). */
    public void remplacerElection(Election nouvelle, boolean marquerModifie) {
        this.election = nouvelle;
        modifie.set(marquerModifie);
        revision.set(revision.get() + 1);
    }

    /** À appeler après toute modification de l'élection. */
    public void notifierModification() {
        modifie.set(true);
        revision.set(revision.get() + 1);
    }

    public ReadOnlyIntegerProperty revisionProperty() {
        return revision;
    }

    public BooleanProperty modifieProperty() {
        return modifie;
    }

    public boolean isModifie() {
        return modifie.get();
    }

    public ReadOnlyStringProperty descriptionStockageProperty() {
        return descriptionStockage;
    }
}
