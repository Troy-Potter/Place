package place.client.gui;

import javafx.scene.control.Button;
import place.PlaceTile;
import place.client.ptui.PlacePTUI;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;

public class ListenerGUI extends Thread {

    private ObjectInputStream inputStream;
    private PlaceGUI gui;
    private boolean runListener;

    public ListenerGUI(ObjectInputStream input, PlaceGUI gui){
        this.inputStream = input;
        this.gui = gui;
        this.runListener = true;
    }

    public void close(){
        runListener = false;
    }


    public void run() {
        while (runListener) {
            try {
                PlaceRequest<?> updateServer = (PlaceRequest<?>) inputStream.readUnshared();

                if (updateServer.getType() == PlaceRequest.RequestType.TILE_CHANGED) {
                    PlaceTile tile = (PlaceTile) updateServer.getData();
                    //set new tile on board
                    int row = tile.getRow();
                    int col = tile.getCol();
                    //Update Tile Grid
                    gui.Tile_Grid[col][row] = tile;

                    //Update Button Grid
                    PlaceTile temp =  gui.Tile_Grid [col][row];
                    gui.Button_Grid[col][row].setStyle("-fx-base: rgb("+Integer.toString(temp.getColor().getRed())+","+Integer.toString(temp.getColor().getGreen())
                            +","+Integer.toString(temp.getColor().getBlue())+");");

                    //print the new board
                    System.out.println(gui.Tile_Grid);

                    //Add delay to boards being printed
                    Thread.sleep(3000);

                } else if (updateServer.getType() == PlaceRequest.RequestType.ERROR) {
                    System.out.println("Error in game");
                }


            } catch (IOException | ClassNotFoundException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

}
