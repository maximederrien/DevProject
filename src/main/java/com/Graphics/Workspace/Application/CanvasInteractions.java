package com.Graphics.Workspace.Application;

import com.Graphics.GraphicsManager;
import com.Graphics.Workspace.Component.ComponentInstance;
import com.Graphics.Workspace.Component.IOComponent;
import com.Graphics.Workspace.Node.GraphicNode;
import com.Graphics.Workspace.Node.InputNode;
import com.Graphics.Workspace.Node.OutputNode;
import com.Graphics.Workspace.Sheet.Sheet;
import com.Graphics.Workspace.Wire.WireInstance;
import com.Graphics.Workspace.Wire.WireInteraction;
import com.Physics.Wire;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;

import java.util.ArrayList;
import java.util.Objects;

import static com.Graphics.GraphicsManager.currentSheet;
import static com.Graphics.GraphicsManager.physicSheet;

/**
 * This class allows the user to interact with the canvas
 */
public class CanvasInteractions {
    /**
     * The sheet on which the user is working
     */
    private final Sheet sheet;
    /**
     * The scale of the sheet on the canvas
     */
    private final double scale;

    /**
     * The action the user is currently performing
     */
    private CurrentAction currentAction;

    /**
     * The graphic node the user is currently interacting with
     */
    public GraphicNode selectedNode;
    /**
     * The output node the user is currently interacting with
     */
    public OutputNode startNode;
    /**
     * The input node the user is currently interacting with
     */
    public InputNode endNode;
    /**
     * The component the user is currently interacting with
     */
    private ComponentInstance selectedComponent;
    /**
     * The wire the user is currently interacting with
     */
    private WireInstance selectedWire;

    /**
     * The x coordinate of the mouse relative to the selected object
     */
    private double xFromCenter;
    /**
     * The y coordinate of the mouse relative to the selected object
     */
    private double yFromCenter;

    /**
     * The x coordinate of the component before the user moved it
     */
    private double firstXPosition;
    /**
     * The y coordinate of the component before the user moved it
     */
    private double firstYPosition;

    /**
     * Constructor of the class CanvasInteractions
     * @param sheet the sheet on which the user is working
     * @param canvas the canvas on which the user is working
     * @param scale the scale of the sheet on the canvas
     */
    public CanvasInteractions(Sheet sheet, Canvas canvas, double scale) {
        this.sheet = sheet;
        this.scale = scale;
        canvas.setOnMousePressed(this::OnMousePressed);
        canvas.setOnMouseReleased(this::OnMouseReleased);
        canvas.setOnMouseDragged(this::OnMouseDragged);
        currentAction = CurrentAction.none;
    }

    private void OnMousePressed(MouseEvent event) {
        // On essaye de récupérer un node sous le curseur
        GraphicNode node = sheet.getNodeAt(event.getX() / scale, event.getY() / scale);
        if (Objects.nonNull(node)) {
            // Dans ce cas, on sélectionne le node
            selectedNode = node;
            //System.out.println(selectedNode.id);

            // On teste quel type de node c'est
            if (selectedNode instanceof OutputNode) {
                //On note qu'un node est sélectionné
                currentAction = CurrentAction.pressOnOutputNode;
            }
            else if (selectedNode instanceof InputNode) {
                //On note qu'un node est sélectionné
                currentAction = CurrentAction.pressOnInputNode;
            }
            //System.out.println("Click on node");
            return;
        }
        // Si ce n'est pas un node, on tente avec un composant
        ComponentInstance component = sheet.getComponentAt(event.getX() / scale, event.getY() / scale);
        if (Objects.nonNull(component)) {
            //On note qu'un composant est sélectionné
            currentAction = CurrentAction.pressOnComponent;

            // On récupère le composant sélectionné
            selectedComponent = component;

            // On récupère les coordonnées du composant
            firstXPosition = selectedComponent.getCenterX();
            firstYPosition = selectedComponent.getCenterY();

            // On récupère les coordonnées du curseur par rapport au composant
            xFromCenter = event.getX() / scale - selectedComponent.getCenterX();
            yFromCenter = event.getY() / scale - selectedComponent.getCenterY();

            selectedComponent.selectComponent(true);
            selectedComponent.setCenter(firstXPosition, firstYPosition);
            return;
        }
        WireInteraction wireInteraction = sheet.getWireAt(event.getX() / scale, event.getY() / scale);
        if (Objects.nonNull(wireInteraction)) {
            //On note qu'un wire est sélectionné
            currentAction = CurrentAction.pressOnWire;

            // On récupère le wire sélectionné
            selectedWire = wireInteraction.getWire();
        }
    }
    private void OnMouseReleased(MouseEvent event) {
        switch (currentAction) {
            // Si un fil est sélectionné, on ouvre le menu de contexte
            case pressOnWire:
                System.out.println("Wire selected");
                break;

            // Si un output node est sélectionné, on vérifie si ce node est global pour changer l'état de son fil
            case pressOnOutputNode:
                if (Objects.equals(sheet.getNodeAt(event.getX() / scale, event.getY() / scale), selectedNode) && selectedNode.isGlobal && selectedNode instanceof OutputNode) {
                    selectedNode.setState(!selectedNode.getState());
                    ArrayList<WireInstance> node = ((OutputNode) selectedNode).wiresConnected;
                    node.get(0).getPhysicWire().setState(!selectedNode.getState());

                    if (sheet.isSheetComplete()) {
                        for (Wire wire : physicSheet.getWires()) {
                            wire.setState(null);
                        }
                        physicSheet.refresh();
                        for (Wire wire : physicSheet.getWires()) {
                            System.out.println(wire.getState());
                        }
                        currentSheet.refresh();
                    }
                }
                break;

            // Si un input node est sélectionné, ???
            case pressOnInputNode:
                break;

            // Si un composant est sélectionné, on
            case pressOnComponent:
                double centerX1 = selectedComponent.getCenterX();
                double centerY1 = selectedComponent.getCenterY();
                selectedComponent.selectComponent(false);
                selectedComponent.setCenter(centerX1, centerY1);
                break;

            // Si un fil est en train d'être déplacé depuis un input ou un output
            case wireDragFromInput:
            case wireDragFromOutput:
                if (!selectedWire.canBePlaced) {
                    sheet.removeWire(selectedWire);
                }
                if (sheet.isSheetComplete()) {
                    for (Wire wire : physicSheet.getWires()) {
                        wire.setState(null);
                    }
                    physicSheet.refresh();
                    for (Wire wire : physicSheet.getWires()) {
                        System.out.println(wire.getState());
                    }
                    currentSheet.refresh();
                }
                break;

            // Si un composant est en train d'être déplacé
            case componentDrag:

                if (!selectedComponent.canBePlaced()) {
                    selectedComponent.selectComponent(false);
                    selectedComponent.startAnimateMoveTo(firstXPosition, firstYPosition);
                }
                else {
                    double centerX2 = selectedComponent.getCenterX();
                    double centerY2 = selectedComponent.getCenterY();
                    selectedComponent.selectComponent(false);
                    selectedComponent.setCenter(centerX2, centerY2);
                    selectedComponent.isPlaced = true;
                }
                break;

            default:
                break;
        }
        currentAction = CurrentAction.none;
        startNode = null;
        endNode = null;

        GraphicsManager.controller.setSimulateState(sheet.isSheetComplete());
    }
    private void OnMouseDragged(MouseEvent event) {
        double posX = event.getX() / scale;
        double posY = event.getY() / scale;
        if (posX >= 0 && posY >= 0 && posX <= sheet.getWidth() && posY <= sheet.getHeight()) {
            switch (currentAction) {
                // Si un composant a été sélectionné mais pas déplacé
                case pressOnComponent: {
                    currentAction = CurrentAction.componentDrag;
                    selectedComponent.isPlaced = false;
                }

                // Si un composant est en train d'être déplacé, on essaye de le bouger
                case componentDrag: {
                    tryAndMoveComponent(posX, posY);
                    break;
                }

                // Si on a cliqué sur un output node, mais que le fil n'est pas encore créé
                case pressOnOutputNode: {
                    // On caste le node en tant que start node
                    startNode = (OutputNode) selectedNode;

                    // On créé un nouveau fil
                    selectedWire = new WireInstance();
                    sheet.addWire(selectedWire);

                    selectedWire.setStart(startNode);
                    if (!(startNode.relativeTo instanceof IOComponent)) {
                        startNode.relativeTo.getPhysicComponent().addWireOutput(selectedWire.getPhysicWire(), startNode.id);
                        physicSheet.addWire(selectedWire.getPhysicWire());
                    } else {
                        selectedWire.getPhysicWire().setState(startNode.getState());
                    }
                    startNode.setState(startNode.getState());
                    // On passe en wireDrag
                    currentAction = CurrentAction.wireDragFromOutput;
                }
                // Si un fil est déjà créé, depuis un node d'output
                case wireDragFromOutput: {
                    // On essaye de récupérer un fil
                    GraphicNode possibleEndNode = sheet.getNodeAt(posX, posY);

                    // Cas 1 : on reste sur le même node
                    if (Objects.nonNull(endNode) && Objects.equals(endNode, possibleEndNode)) {
                        break;
                    }

                    // Cas 2 : on entre sur un node input
                    if (Objects.isNull(endNode) && possibleEndNode instanceof InputNode) {
                        endNode = (InputNode) possibleEndNode;

                        // Si le node de fin est vide
                        if (!endNode.hasWire()) {
                            // On connecte le fil
                            //endNode.setWire(selectedWire);
                            selectedWire.setEnd(endNode);

                            selectedWire.canBePlaced = !sheet.isWireOverriding(selectedWire);
                        }
                        // Si le node de fin est déjà occupé
                        else {
                            // On coupe les nodes pour passer au cas 6
                            possibleEndNode = null;
                            endNode = null;
                        }
                    }

                    // Cas 3 : on entre sur un node output
                    if (Objects.isNull(endNode) && possibleEndNode instanceof OutputNode) {
                        // On casse possible end node pour passer au cas 6
                        possibleEndNode = null;
                    }

                    // Cas 4 : on sort du node input
                    if (Objects.nonNull(endNode) && Objects.isNull(possibleEndNode)) {
                        // On passe au cas 6 en cassant les nodes
                        endNode = null;
                    }

                    // Cas 5 (peu probable) : on change instantanément de node
                    if (Objects.nonNull(endNode) && !Objects.equals(endNode, possibleEndNode)) {
                        System.out.println("Warning ! Case happened, end node and possible end node not null");

                        // On suit d'abord le cas 3
                        selectedWire.setEnd(posX, posY);

                        // Puis on suit le cas 1
                        if (!endNode.hasWire()) {
                            selectedWire.setEnd(endNode);
                            selectedWire.canBePlaced = true;
                        }
                        else {
                            possibleEndNode = null;
                            endNode = null;
                        }
                    }

                    // Cas 6 : on est tjs sans node
                    if (Objects.isNull(endNode) && Objects.isNull(possibleEndNode)) {
                        // On fait suivre le fil par le curseur
                        selectedWire.setEnd(posX, posY);

                        //testWirePlacing();
                        selectedWire.canBePlaced = false;
                    }
                    break;
                }

                // Si on a cliqué sur un input node, mais qu'on a pas créé de fil, on créé un fil et on le déplace
                case pressOnInputNode: {
                    // On cast le node en tant que endNode
                    endNode = (InputNode) selectedNode;

                    if (endNode.hasWire()) {
                        currentAction = CurrentAction.none;
                        break;
                    }
                    else {
                        selectedWire = new WireInstance();
                        sheet.addWire(selectedWire);

                        if (endNode.relativeTo instanceof  IOComponent) {
                            physicSheet.addWire(selectedWire.getPhysicWire());
                            endNode.relativeTo.getPhysicComponent().addWireInput(selectedWire.getPhysicWire(), endNode.id);
                        } else {
                            selectedWire.getPhysicWire().setState(endNode.getState());
                        }
                        selectedWire.setEnd(endNode);

                        endNode.setState(endNode.getState());

                        currentAction = CurrentAction.wireDragFromInput;
                    }
                }
                // Si un fil est déjà créé depuis un input node, on essaye de le bouger
                case wireDragFromInput: {
                    // On essaye de récupérer un fil
                    GraphicNode possibleStartNode = sheet.getNodeAt(posX, posY);

                    // Cas 1 : on reste sur le même node
                    if (Objects.nonNull(startNode) && Objects.equals(startNode, possibleStartNode)) {
                        break;
                    }

                    // Cas 2 : on entre sur un node output
                    if (Objects.isNull(startNode) && possibleStartNode instanceof OutputNode) {
                        startNode = (OutputNode) possibleStartNode;

                        // On connecte le fil
                        selectedWire.setStart(startNode);

                        selectedWire.canBePlaced = true;
                    }

                    // Cas 3 : on entre sur un node input
                    if (Objects.isNull(startNode) && possibleStartNode instanceof InputNode) {
                        // On casse possible end node pour passer au cas 6
                        possibleStartNode = null;
                    }

                    // Cas 4 : on sort du node input
                    if (Objects.nonNull(startNode) && Objects.isNull(possibleStartNode)) {
                        // Puis on passe au cas 6 en cassant les nodes
                        startNode = null;
                    }

                    // Cas 5 (peu probable) : on change instantanément de node
                    if (Objects.nonNull(startNode) && !Objects.equals(startNode, possibleStartNode)) {
                        System.out.println("Warning ! Case happened, end node and possible end node not null");

                        // On suit d'abord le cas 3
                        selectedWire.setStart(posX, posY);

                        // Puis on suit le cas 1
                        selectedWire.setStart(startNode);
                    }

                    // Cas 6 : on est tjs sans node
                    if (Objects.isNull(startNode) && Objects.isNull(possibleStartNode)) {
                        // On fait suivre le fil par le curseur
                        selectedWire.setStart(posX, posY);

                        //testWirePlacing();
                        selectedWire.canBePlaced = false;
                    }
                    break;
                }

                // Si on avait sélectionné un fil, et que l'on essaie de déplacer
                case pressOnWire:
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Teste si le fil sélectionné peut être placé
     */
    private void testWirePlacing() {
        boolean wireExists = false;
        if (Objects.nonNull(selectedWire.getStart()) && Objects.nonNull(selectedWire.getEnd())) {
            wireExists = sheet.isThereAWire(selectedWire.getStart(), selectedWire.getEnd());
        }
        selectedWire.canBePlaced = selectedWire.isWidthLarge() && !sheet.isWireOverriding(selectedWire) && !wireExists;
    }

    /**
     * Test if the selected component can be moved at (centerX, centerY), which are the center of the component
     * @param centerX the X coordinate in NU
     * @param centerY the Y coordinate in NU
     */
    private void tryAndMoveComponent(double centerX, double centerY) {
        if (!selectedComponent.isOnSheet(sheet.getWidth(), sheet.getHeight())) {
            selectedComponent.setPlaced(false);
            //System.out.println("Not on sheet");
        }
        else {
            ComponentInstance override = sheet.isOverriding(selectedComponent);
            //System.out.println("Wire overriding");
            if (Objects.nonNull(override) || !selectedComponent.areWiresFacing()) {
                selectedComponent.setPlaced(false);
                //System.out.println("Wire inverted");
            }
            else selectedComponent.setPlaced(!sheet.areWiresOverriding());
        }
        selectedComponent.setCenter(centerX - xFromCenter, centerY - yFromCenter);
    }
}
