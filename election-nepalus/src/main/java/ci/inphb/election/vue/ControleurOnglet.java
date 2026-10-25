package ci.inphb.election.vue;

import ci.inphb.election.modele.Election;
import ci.inphb.election.modele.PartiPolitique;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableRow;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.DropShadow;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Alert;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Contrôleur d'un onglet de la fenêtre principale. Il reçoit le contexte partagé
 * et se rafraîchit automatiquement à chaque modification de l'élection.
 */
public abstract class ControleurOnglet {

    protected static final NumberFormat FORMAT_POURCENTAGE = NumberFormat.getPercentInstance(Locale.FRANCE);
    protected static final NumberFormat FORMAT_ENTIER = NumberFormat.getIntegerInstance(Locale.FRANCE);

    static {
        FORMAT_POURCENTAGE.setMinimumFractionDigits(2);
        FORMAT_POURCENTAGE.setMaximumFractionDigits(2);
    }

    protected ContexteApplication contexte;

    // Titre défilant de l'onglet (facultatif : seulement si la vue FXML déclare ces fx:id)
    @FXML
    private Pane zoneTitre;
    @FXML
    private Region titreDefilant;

    public void initialiserContexte(ContexteApplication contexte) {
        this.contexte = contexte;
        if (zoneTitre != null) {
            Animations.defilement(zoneTitre, titreDefilant);
        }
        contexte.revisionProperty().addListener((obs, ancienne, nouvelle) -> rafraichir());
        rafraichir();
    }

    protected Election election() {
        return contexte.getElection();
    }

    /** Recharge l'affichage à partir du modèle. */
    protected abstract void rafraichir();

    // ------------------------------------------------------------------ outils communs

    protected static <S, T> void colonne(TableColumn<S, T> colonne, Function<S, T> valeur) {
        colonne.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(valeur.apply(c.getValue())));
    }

    /** Colonne d'entiers affichés avec séparateur de milliers (le tri reste numérique). */
    protected static <S> void colonneEntier(TableColumn<S, Integer> colonne, Function<S, Integer> valeur) {
        colonne(colonne, valeur);
        colonne.setCellFactory(c -> cellule(FORMAT_ENTIER::format));
    }

    /** Colonne de taux (entre 0 et 1) affichés en pourcentage. */
    protected static <S> void colonnePourcentage(TableColumn<S, Double> colonne, Function<S, Double> valeur) {
        colonne(colonne, valeur);
        colonne.setCellFactory(c -> cellule(FORMAT_POURCENTAGE::format));
    }

    private static <S, T> TableCell<S, T> cellule(Function<T, String> format) {
        return new TableCell<>() {
            @Override
            protected void updateItem(T valeur, boolean vide) {
                super.updateItem(valeur, vide);
                setText(vide || valeur == null ? null : format.apply(valeur));
            }
        };
    }

    // ------------------------------------------------------------------ couleurs des partis

    /** Couleur attribuée au parti (selon sa position parmi les partis enrôlés). */
    protected String couleur(PartiPolitique parti) {
        if (contexte == null || parti == null) {
            return Palette.NEUTRE;
        }
        return Palette.couleur(election().getPartis().indexOf(parti));
    }

    /** Pastille colorée portant le sigle du parti. */
    protected Label pastille(PartiPolitique parti) {
        Label badge = new Label(parti.getSigle());
        badge.getStyleClass().add("pastille-parti");
        badge.setStyle("-fx-background-color: " + couleur(parti) + ";");
        return badge;
    }

    /** Colonne affichant un parti sous forme de pastille, suivie de son nom si demandé. */
    protected <S> void colonneParti(TableColumn<S, PartiPolitique> colonne, Function<S, PartiPolitique> parti,
                                    boolean avecNom) {
        colonne(colonne, parti);
        colonne.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(PartiPolitique p, boolean vide) {
                super.updateItem(p, vide);
                if (vide || p == null) {
                    setGraphic(null);
                    setText(vide ? null : "—");
                } else {
                    HBox contenu = new HBox(8, pastille(p));
                    if (avecNom) {
                        contenu.getChildren().add(new Label(p.getNom()));
                    }
                    contenu.setAlignment(Pos.CENTER_LEFT);
                    setGraphic(contenu);
                    setText(null);
                }
            }
        });
    }

    // ------------------------------------------------------------------ graphiques interactifs

    /**
     * Rend interactif un élément de graphique (part de camembert, barre) : couleur, infobulle,
     * surbrillance au survol et action au clic. Le nœud graphique pouvant être créé après
     * l'ajout de la donnée au graphique, la configuration est appliquée dès qu'il existe.
     */
    protected static void rendreInteractif(ObservableValue<Node> noeud, String style, String infobulle, Runnable clic) {
        Consumer<Node> configurer = n -> {
            n.setStyle(style);
            Tooltip bulle = new Tooltip(infobulle);
            bulle.setShowDelay(Duration.millis(80));
            Tooltip.install(n, bulle);
            ColorAdjust eclaircir = new ColorAdjust();
            eclaircir.setBrightness(0.15);
            DropShadow ombre = new DropShadow(12, Color.rgb(0, 0, 0, 0.35));
            ombre.setInput(eclaircir);
            n.setOnMouseEntered(e -> n.setEffect(ombre));
            n.setOnMouseExited(e -> n.setEffect(null));
            if (clic != null) {
                n.setCursor(Cursor.HAND);
                n.setOnMouseClicked(e -> clic.run());
            }
        };
        if (noeud.getValue() != null) {
            configurer.accept(noeud.getValue());
        }
        noeud.addListener((obs, ancien, nouveau) -> {
            if (nouveau != null) {
                configurer.accept(nouveau);
            }
        });
    }

    /** Lignes de tableau réagissant au double-clic. */
    protected static <T> void doubleClicSurLigne(TableView<T> table, Consumer<T> action) {
        table.setRowFactory(t -> {
            TableRow<T> ligne = new TableRow<>();
            ligne.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !ligne.isEmpty()) {
                    action.accept(ligne.getItem());
                }
            });
            return ligne;
        });
    }

    /** « 1 département », « 3 départements » (en français, 0 s'écrit au singulier). */
    protected static String nombre(long n, String singulier, String pluriel) {
        return FORMAT_ENTIER.format(n) + " " + (n <= 1 ? singulier : pluriel);
    }

    // ------------------------------------------------------------------ recherche

    /** Texte sans accents ni majuscules : « Pélorie » et « pelorie » se correspondent. */
    protected static String normaliser(String texte) {
        return Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Vue filtrée de {@code source}, mise à jour à chaque frappe dans le champ de recherche. */
    protected static <T> FilteredList<T> filtrer(ObservableList<T> source, TextField recherche, Function<T, String> texte) {
        FilteredList<T> filtre = new FilteredList<>(source);
        recherche.textProperty().addListener((obs, ancien, nouveau) -> {
            String cherche = normaliser(nouveau.trim());
            filtre.setPredicate(cherche.isEmpty() ? null : element -> normaliser(texte.apply(element)).contains(cherche));
        });
        return filtre;
    }

    /** Affiche {@code source} dans la table, filtrée par la recherche et triable par clic sur les en-têtes. */
    protected static <T> void lierTableFiltree(TableView<T> table, ObservableList<T> source,
                                               TextField recherche, Function<T, String> texte) {
        SortedList<T> trie = new SortedList<>(filtrer(source, recherche, texte));
        trie.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(trie);
    }

    protected static void afficherErreur(String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR, message);
        alerte.setHeaderText("Opération impossible");
        alerte.showAndWait();
    }

    protected static void afficherInformation(String message) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION, message);
        alerte.setHeaderText(null);
        alerte.showAndWait();
    }
}
