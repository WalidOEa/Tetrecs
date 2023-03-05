# Tetrecs
## About
A small project based off the hit game Tetris. In this version, the player places blocks onto a 5 by 5 grid, trying to fill columns or rows to gain points. For each successive column or row clear a multiplier increases the points gained for each subsequent clear. Furthermore, with enough points, the player can purchase power ups including a nuke (clears the entire board), a 15x multiplier and a one UP. 

In addition, the game contains a musicplayer and a folder is created allowing players to play their own music and skip, forward or even pause the current track playing.

Once the game is over, if the player beat any of the local scores, they can add their name to the leaderboard. Consequently, a text file is written detailing local scores on that machine.



## Requirements
- JDK 17+
- Maven 3.2+

## How to Run
1. In cmd or terminal, navigate to the root directory of Tetrecs, containing file Pom.xml
2. Run command,

    ```mvn javafx:run```

## Screenshots
![Screenshot (2)](https://user-images.githubusercontent.com/126994619/222938347-a768fafd-cf73-41c7-a58d-458e4cff04b4.png)



![Screenshot (4)](https://user-images.githubusercontent.com/126994619/222938359-5e1746e7-8cf2-48a8-a5c8-9cdfa3ee03fb.png)



![Screenshot (8)](https://user-images.githubusercontent.com/126994619/222938503-47987eda-4eb2-43fe-8528-20fff0104004.png)



![Screenshot (9)](https://user-images.githubusercontent.com/126994619/222938512-2e98fbe8-67c7-4c93-bddb-eb732bffaf06.png)

## Future Updates
- There currently exists a functional multiplayer contained within the game's code, however as of recent, the server no longer functions and as a result the multiplayer component is down. This will eventually be fixed in the near future.
- Minor bug fixes
