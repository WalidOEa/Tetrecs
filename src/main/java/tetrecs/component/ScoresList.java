package tetrecs.component;

import javafx.animation.FadeTransition;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.Pair;

/**
 * Creates a custom UI component that displays all scores, both local and online.
 */
public class ScoresList extends VBox {

    /**
     * Score text containing information received from local files and online server
     */
    public Text scoreText;

    /**
     * Constructs component
     * @param scores scores to display
     */
    public ScoresList(ObservableList<Pair<String, Integer>> scores) {
        scores.addListener((ListChangeListener<? super Pair<String, Integer>>) change -> build(scores));
    }

    /**
     * Build custom UI component
     * @param scores scores to display
     */
    public void build(ObservableList<Pair<String, Integer>> scores) {
        int millis = 500;
        int i = 1;

        this.getChildren().clear();
        var scoreVBox = new VBox(5);

        for (Pair<String, Integer> pair : scores) {
            String name = pair.getKey();
            String score = pair.getValue().toString();

            String nameScore = name + ": " + score;

            // Add simple fade transition and change each individual colour
            scoreText = new Text(nameScore);
            scoreText.getStyleClass().add("scorelist");
            scoreText.setFill(GameBlock.COLOURS[i]);

            FadeTransition fadeTransition = new FadeTransition(Duration.millis(millis), scoreText);
            fadeTransition.setFromValue(0.0);
            fadeTransition.setToValue(1.0);
            fadeTransition.play();

            scoreVBox.getChildren().add(scoreText);

            millis += 950;
            i++;
        }

        this.getChildren().add(scoreVBox);
    }
}
