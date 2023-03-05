package uk.ac.soton.comp1206.scene;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Objects;

/**
 * Multimedia is responsible for playing sound effects during specific interactions with the UI as well as playing
 * music during scenes on a loop and playing music through the music player.
 */
public class Multimedia {

  private static final Logger logger = LogManager.getLogger(Multimedia.class);

  /**
   * Checks if audio is enabled or not.
   */
  private static boolean audioEnabled = true;

  /**
   * Handles music that plays during runtime. MediaPlayer are declared as field variables to prevent premature
   * garbage collection.
   */
  private static MediaPlayer backgroundMusicPlayer;

  /**
   * Handles music that plays from the musicplayer component in certain scenes.
   */
  private static MediaPlayer musicPlayer;

  /**
   * Handles sounds played during certain interactions with UI.
   */
  private static MediaPlayer soundPlayer;

  /**
   * Play a sound file during runtime.
   * @param filename name of file to play
   */
  public static void playSound(String filename) {
    if (!audioEnabled) {
      return;
    }

    // Convert URL to string
    String playSound = Objects.requireNonNull(Multimedia.class.getResource("/sounds/" + filename)).toExternalForm();
    logger.info("Playing sound {} ", playSound);

    try {
      Media play = new Media(playSound);
      soundPlayer = new MediaPlayer(play);
      // Play sound
      soundPlayer.play();
    } catch (Exception e) {
      audioEnabled = false;
      logger.error("Unable to play {}", playSound);
    }
  }

  /**
   * Play looping music during runtime.
   * @param filename name of file to play
   */
  public static void playBackgroundMusic(String filename) {
    if (!audioEnabled) {
      return;
    }

    String playMusic = Objects.requireNonNull(Multimedia.class.getResource("/music/" + filename)).toExternalForm();
    logger.info("Playing music {}", playMusic);

    try {
      Media play = new Media(playMusic);
      backgroundMusicPlayer = new MediaPlayer(play);
      backgroundMusicPlayer.play();
    } catch (Exception e) {
      audioEnabled = false;
      logger.error("Unable to play {}", playMusic);
    }
  }

  /**
   * Play music during certain scenes from the music player.
   * @param fileToPlay name of file to play
   */
  public static void playMusic(String fileToPlay) {
    if (!audioEnabled) {
      return;
    }

    logger.info("Playing music {}", fileToPlay);
    try {
      Media play = new Media(fileToPlay);
      musicPlayer = new MediaPlayer(play);
      musicPlayer.setOnEndOfMedia(() -> {
        try {
          playMusic(MenuScene.musicToPlay.get(MenuScene.musicIndex));
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
          musicPlayer.stop();
          return;
        }
      });
      musicPlayer.play();
    } catch (Exception e) {
      e.printStackTrace();
      audioEnabled = false;
      logger.error("Unable to play {}", fileToPlay);

    }
  }

  /**
   * Stops any playing music from playing during scene transitions.
   */
  public static void musicCleanUp() {
    try {
      musicPlayer.stop();
    } catch (NullPointerException nullPointerException1) {
      try {
        backgroundMusicPlayer.stop();
      } catch (NullPointerException nullPointerException2) {
        assert true;
      }
    }
    try {
      backgroundMusicPlayer.stop();
    } catch (NullPointerException nullPointerException1) {
      try {
        musicPlayer.stop();
      } catch (NullPointerException nullPointerException2) {
        assert true;
      }
    }
  }

  /**
   * Pause currently playing music. Used in the song player.
   */
  public static void pauseMusic() {
    musicPlayer.pause();
  }

  /**
   * Resumes playing music.
   */
  public static void resumeMusic() {
    musicPlayer.play();
  }
}