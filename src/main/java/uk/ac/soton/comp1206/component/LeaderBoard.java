package uk.ac.soton.comp1206.component;

import javafx.collections.ObservableList;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Pair;

import java.util.Random;

/**
 * Custom UI component that lays out text containing name and score vertically. Used in displaying scores against
 * other competing players.
 */
public class LeaderBoard extends ScoresList {

    Random rand = new Random();

    /**
     * Construct the leaderboard
     * @param scores scores to update the leaderboard
     */
    public LeaderBoard(ObservableList<Pair<String, Integer>> scores) {
        super(scores);
    }

    /**
     * During an event firing, clear all existing nodes in the component and reconstruct it using based off the observable list.
     * @param scores scores to reflect in UI
     * @param userName username of player who has lost or quit
     * @param userScore score of player who has lost or quit
     */
    public void build(ObservableList<Pair<String, Integer>> scores, String userName, String userScore) {
        this.getChildren().clear();
        var scoreVBox = new VBox(5);

        for (Pair<String, Integer> pair : scores) {
            String name = pair.getKey();
            String score = pair.getValue().toString();

            String nameScore;
            if (userName.equals(name) && userScore.equals(score)) {
                nameScore = userName + ": " + userScore;
                scoreText = new Text(nameScore);
                scoreText.setFill(GameBlock.COLOURS[rand.nextInt(GameBlock.COLOURS.length)]);
                scoreText.getStyleClass().add("scorelist");
                scoreText.setStrikethrough(true);

            } else {
                nameScore = name + ": " + score;

                scoreText = new Text(nameScore);
                scoreText.setFill(GameBlock.COLOURS[rand.nextInt(GameBlock.COLOURS.length)]);
                scoreText.getStyleClass().add("scorelist");

            }
            scoreVBox.getChildren().add(scoreText);
        }
        this.getChildren().add(scoreVBox);
    }
}
