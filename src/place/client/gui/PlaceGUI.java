package place.client.gui;

import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;


import javafx.stage.Stage;
import place.PlaceBoard;
import place.PlaceColor;
import place.PlaceTile;
import place.client.ptui.Listener;
import place.model.ClientModel;
import place.model.Observer;
import place.network.PlaceRequest;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;


public class PlaceGUI extends Application implements Observer<ClientModel, PlaceTile> {
    private static String username; //username
    public Button[][] Button_Grid; //needs to be referenced from other methods
    public PlaceTile[][] Tile_Grid;//temp model for this case: to be removed
    private PlaceTile[] Tile_Line;// the tiles used in the tool bar, at the bottom
    private PlaceColor Current_Color; //current selected color

    private int Dim;// to be filled in by model

    //Only used to get board data
    PlaceBoard board;

    private Socket ClientSocket;
    private ObjectOutputStream objectOutputStream;
    private ObjectInputStream objectInputStream;
    private ListenerGUI serverListener;


    /**
     * Setup network, login and receive board
     */
    public void init(){
        // Get host info from command line
        List<String> args = getParameters().getRaw();

        // get host name, port and username from command line
        String host = args.get(0);
        int port = Integer.parseInt(args.get(1));
        username = args.get(2);

        try {
            //connect socket with host and port
            ClientSocket = new Socket(host, port);
            //Create new output stream
            objectOutputStream = new ObjectOutputStream(ClientSocket.getOutputStream());
            objectOutputStream.flush();

            //Create new login request
            PlaceRequest<String> sendUser = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
            //Send request to user
            objectOutputStream.writeUnshared(sendUser);
            objectOutputStream.flush();

            objectInputStream = new ObjectInputStream(ClientSocket.getInputStream());
            PlaceRequest<?> confirmConnection = (PlaceRequest<?>) objectInputStream.readUnshared();

            if (confirmConnection.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS){
                System.out.println("Login Success");
                //Create board request
                PlaceRequest<?> boardRequest = (PlaceRequest<?>) objectInputStream.readUnshared();

                if (boardRequest.getType() == PlaceRequest.RequestType.BOARD){
                    //Get board as PlaceTile[][]
                    board = (PlaceBoard) boardRequest.getData();
                    Tile_Grid = board.getBoard();

                    //Get board dimension
                    Dim = Tile_Grid.length;

                    //Start the gui listener - looks for tile changed request type
                    serverListener = new ListenerGUI(objectInputStream, this);
                    serverListener.start();

                }else if (confirmConnection.getType() == PlaceRequest.RequestType.ERROR){
                    System.out.println("Login unsuccessful");
                    close();
                }

            }

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void start(Stage primaryStage) {
        //Create board and show
        primaryStage = initial();
        primaryStage.show();
    }


    //creates the primary stage
    public Stage initial() {
        //Create panes
        BorderPane pane = new BorderPane();
        GridPane pane2 = new GridPane();

        //Using dim create tiles for grid
        Tile_Grid = new PlaceTile[Dim][Dim];
        for (int row=0;row<Dim;row++){
            for (int col=0;col<Dim;col++){
                Tile_Grid[row][col]=new PlaceTile(row,col,"Server", PlaceColor.WHITE);
            }
        }//initialization for the temp model

        //Add gridpanes to border pane
        makeGrid(pane2);
        GridPane pane3 = new GridPane();
        pane3.add(Toggler(),0,0);
        pane.setCenter(pane2);
        pane.setBottom(pane3);

        //Create stage and add border pane to it
        Stage primaryStage =new Stage();
        Scene scene =new Scene(pane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Place_GUI");

        return primaryStage;
    }

    /**
     * Creates the grid of buttons that make up the main body of the program
     * @param pane
     */
    public void makeGrid(GridPane pane) {
        Button_Grid = new Button[Dim][Dim];
        //creates a button for each section of the grid
        for (int row=0;row<Dim;row++) {
            for (int col = 0; col < Dim; col++) {
                Button temp_Button = new Button();
                // action event
                int finalRow = row;
                int finalCol = col;
                //plop into here the function you want done
                EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
                    public void handle(ActionEvent e) {
                        //Change board color
                        ChangeColor(new Coordinate(finalRow, finalCol));
                        //Create change tile request
                        changeTile(username, finalRow, finalCol);
                    }

                };

                //creation of the tooltip (The hover over part)
                Tooltip t = new Tooltip();
                t.setText("(" + Integer.toString(col) + "," + Integer.toString(row) + ")"
                        + "\n" + Tile_Grid[finalCol][finalRow].getOwner() + "\n" +
                        Tile_Grid[finalCol][finalRow].getTime());
                Tooltip.install(temp_Button, t);


                temp_Button.setOnAction(event);
                //colors and sizes the button
                temp_Button.setStyle("-fx-base: rgb(255,255,255);");
                Button_Grid[finalCol][finalRow] = temp_Button;
                if (Dim > 8) {
                    temp_Button.setMinSize(50 - 5 * (Dim - 8), 50 - 5 * (Dim - 8));
                }else {
                    temp_Button.setMinSize(50 + 5 * (8 - Dim), 50 + 5 * (8 - Dim));
                }

                pane.add(temp_Button, finalCol, finalRow);

            }
        }
    }

    /**
     * Creates the color picker button section of the gui
     * @return A Flow pane of the buttons
     */
    private Node Toggler() {
        ToggleGroup toggler = new ToggleGroup();
        FlowPane flow = new FlowPane();
        Tile_Line = new PlaceTile[16];//creation and filling of the tile_line functions
        Tile_Line[0] = new PlaceTile(0,0,"",PlaceColor.BLACK);
        Tile_Line[1] = new PlaceTile(0,1,"",PlaceColor.GRAY);
        Tile_Line[2] = new PlaceTile(0,2,"",PlaceColor.SILVER);
        Tile_Line[3] = new PlaceTile(0,3,"",PlaceColor.WHITE);
        Tile_Line[4] = new PlaceTile(0,4,"",PlaceColor.MAROON);
        Tile_Line[5] = new PlaceTile(0,5,"",PlaceColor.RED);
        Tile_Line[6] = new PlaceTile(0,6,"",PlaceColor.OLIVE);
        Tile_Line[7] = new PlaceTile(0,7,"",PlaceColor.YELLOW);
        Tile_Line[8] = new PlaceTile(0,8,"",PlaceColor.GREEN);
        Tile_Line[9] = new PlaceTile(0,9,"",PlaceColor.LIME);
        Tile_Line[10] = new PlaceTile(0,10,"",PlaceColor.TEAL);
        Tile_Line[11] = new PlaceTile(0,11,"",PlaceColor.AQUA);
        Tile_Line[12] = new PlaceTile(0,12,"",PlaceColor.NAVY);
        Tile_Line[13] = new PlaceTile(0,13,"",PlaceColor.BLUE);
        Tile_Line[14] = new PlaceTile(0,14,"",PlaceColor.PURPLE);
        Tile_Line[15] = new PlaceTile(0,15,"",PlaceColor.FUCHSIA);

        for (int x = 0; x < 16; x++) {
            ToggleButton temp_Button = new ToggleButton();
            int finalNum = x;
            //what the
            EventHandler<ActionEvent> event = new EventHandler<ActionEvent>() {
                public void handle(ActionEvent e) {
                    //Current color selected
                    Current_Color = Tile_Line[finalNum].getColor();
                }
            };

            temp_Button.setOnAction(event);

            //styles the button
            temp_Button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
            if (Dim > 8) {
                temp_Button.setMinSize(23 + 2 * (Dim - 8), 23 + 2 * (Dim - 8));
            } else {
                temp_Button.setMinSize(23 - 2 * (8 - Dim), 23 - 2 * (8 - Dim));
            }
            temp_Button.setStyle("-fx-base: rgb(" + Integer.toString(Tile_Line[finalNum].getColor().getRed()) + "," + Integer.toString(Tile_Line[finalNum].getColor().getGreen())
                    + "," + Integer.toString(Tile_Line[finalNum].getColor().getBlue()) + ");");

            temp_Button.setToggleGroup(toggler);//adds it to the toggle bar
            flow.getChildren().add(temp_Button); //adds the button

        }

        return flow ;
    }

    /**
     * Current function for changing the tile color, can be changed
     * simply is the function called upon when a button is clicked from the grid
     * @param - coordinate of the button on the grid
     */
    private void ChangeColor(Coordinate a) {
        //tiles settings get changed, likely this is where you will modify the model
        Tile_Grid [a.getCol()][a.getRow()].setTime(0L);
        Tile_Grid [a.getCol()][a.getRow()].setColor(Current_Color);
        Tile_Grid [a.getCol()][a.getRow()].setOwner(username);
        PlaceTile temp =  Tile_Grid [a.getCol()][a.getRow()];

        Button_Grid[a.getCol()][a.getRow()].setStyle("-fx-base: rgb("+Integer.toString(temp.getColor().getRed())+","+Integer.toString(temp.getColor().getGreen())
                +","+Integer.toString(temp.getColor().getBlue())+");");//IMPORTANT FOR COLOR SWAP DON"T MESS WITH IT
    }

    /**
     * this section is the part heavily involved with the server, where you'll likely spend most of your time
     * @param model
     * @param tile
     */


    /**
     * Stop all connections and close the program
     * @throws IOException
     */
    private void close() throws IOException {
        this.objectOutputStream.close();
        this.objectInputStream.close();
        this.ClientSocket.close();
        serverListener.close();

        System.out.println("The program has stopped");
        System.exit(1);
    }

    /**
     * Creates a change tile request
     * @param username
     * @param row - current tile row
     * @param col - current tile column
     */
    private void changeTile(String username, int row, int col){
        //Change place color to int
        PlaceColor[] colors = PlaceColor.values();
        PlaceColor currentColor = colors[Current_Color.getNumber()];
        //Get time
        long time = System.currentTimeMillis();
        //Create new placetile
        PlaceTile tile = new PlaceTile(row, col, username, currentColor, time);

        //Create new request to change tile
        PlaceRequest<PlaceTile> tilePlaceRequest = new PlaceRequest<>(PlaceRequest.RequestType.CHANGE_TILE, tile);

        try{
            //Send the tile change request
            objectOutputStream.writeUnshared(tilePlaceRequest);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void update(ClientModel model, PlaceTile tile) {

    }


    /**
     * Pretty standard, has the args and launches the program
     * @param args
     */
    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("Usage: java PlaceGUI host port username");
            System.exit(-1);
        } else {
            Application.launch(args);
        }
    }
}
