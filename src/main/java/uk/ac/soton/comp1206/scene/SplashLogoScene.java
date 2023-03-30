package uk.ac.soton.comp1206.scene;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import uk.ac.soton.comp1206.ui.GamePane;
import uk.ac.soton.comp1206.ui.GameWindow;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Displays logo at very beginning of game start, then leads directly into menu scene.
 */
public class SplashLogoScene extends BaseScene {

    private final Logger logger = LogManager.getLogger(SplashLogoScene.class);

    /**
     * Create a new splashscreen  scene.
     * @param gameWindow the Game Window
     */
    public SplashLogoScene(GameWindow gameWindow) {
        super(gameWindow);

        logger.info("Creating new Splash Scene");
    }

    @Override
    public void initialise() {
        Timer timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(gameWindow::startMenu);
            }
        };

        // Delay main menu start for 4.6 seconds
        timer.schedule(timerTask, 4600);
    }

    /**
     * Build the Challenge window and bind any event handlers to necessary components.
     */
    @Override
    public void build() {
        logger.info("Building " + this.getClass().getName());

        root = new GamePane(gameWindow.getWidth(), gameWindow.getHeight());

        var splashScreenPane = new StackPane();
        splashScreenPane.setMaxWidth(gameWindow.getWidth());
        splashScreenPane.setMaxHeight(gameWindow.getHeight());
        splashScreenPane.getStyleClass().add("intro");

        root.getChildren().add(splashScreenPane);

        // Animated simple fade in for image
        var logo = new ImageView(Objects.requireNonNull(this.getClass().getResource("/images/ECSGames.png")).toExternalForm());
        logo.setFitHeight(gameWindow.getHeight()/2.2);
        logo.setFitWidth(gameWindow.getWidth()/1.8);
        FadeTransition fadeTransition = new FadeTransition(Duration.millis(4500), logo);
        fadeTransition.setFromValue(0.0);
        fadeTransition.setToValue(1.0);
        fadeTransition.play();

        Multimedia.playSound("intro.mp3");

        var mainPane = new BorderPane();
        splashScreenPane.getChildren().add(mainPane);

        mainPane.setCenter(logo);
    }
}
