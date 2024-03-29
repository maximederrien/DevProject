package com.Graphics.Workspace.Component;

import com.Config;
import com.Graphics.Workspace.Application.SheetObject;
import com.Graphics.Workspace.Node.GraphicNode;
import com.Graphics.Workspace.Node.InputNode;
import com.Graphics.Workspace.Node.OutputNode;
import com.Physics.Component;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ComponentInstance extends ObjectInstance {
    public InputNode[] inputs;
    public OutputNode[] outputs;

    public boolean isPlaced = false;
    private boolean isSelected = false;

    public ComponentInstance(SheetObject object, Component physicComponent) {
        instanceOf = object;
        inputs = new InputNode[instanceOf.inputs];
        outputs = new OutputNode[instanceOf.outputs];
        for (int i = 0; i < object.inputs; i++) {
            inputs[i] = new InputNode(this, 0, instanceOf.inputNodeHeights[i], i);
        }
        for (int i = 0; i < object.outputs; i++) {
            outputs[i] = new OutputNode(this, 0, instanceOf.outputNodeHeights[i], i);
        }
        this.physicComponent = physicComponent;
    }

    public ComponentInstance(SheetObject object, double originX, double originY, Component physicComponent) {
        this(object, physicComponent);
        setOriginX(originX);
        setOriginY(originY);
    }

    /**
     * Gets the X coordinate of the component instance modified by the selection state
     * @return the X coordinate of the component instance
     */
    public double getOriginX() {
        return super.getOriginX();
    }
    /**
     * Gets the Y coordinate of the component instance modified by the selection state
     * @return the Y coordinate of the component instance
     */
    public double getOriginY() {
        return super.getOriginY();
    }

    /**
     * Gets the width of the component instance modified by the selection state
     * @return the width of the component instance
     */
    public double getWidth() {
        return super.getWidth() + Config.WSCompoSelectedSize * growthAnimation.getSize();
    }
    /**
     * Gets the height of the component instance modified by the selection state
     * @return the height of the component instance
     */
    public double getHeight() {
        return super.getHeight() + Config.WSCompoSelectedSize * growthAnimation.getSize();
    }

    /**
     * Get all the nodes of this component instance
     * @return the ArrayList of all the nodes
     */
    public ArrayList<GraphicNode> getAllNodes() {
        ArrayList<GraphicNode> nodes = new ArrayList<>(List.of(inputs));
        nodes.addAll(List.of(outputs));
        return nodes;
    }
    /**
     * Get all the ON nodes of this component instance
     * @return the ArrayList of all the ON nodes
     */
    public ArrayList<GraphicNode> getOnNodes() {
        ArrayList<GraphicNode> onNodes = new ArrayList<>();
        for (GraphicNode node: inputs) {
            if (node.getState()) {
                onNodes.add(node);
            }
        }
        for (GraphicNode node: outputs) {
            if (node.getState()) {
                onNodes.add(node);
            }
        }
        return onNodes;
    }

    /**
     * Get all the OFF nodes of this component instance
     * @return the ArrayList of all the OFF nodes
     */
    public ArrayList<GraphicNode> getOffNodes() {
        ArrayList<GraphicNode> offNodes = new ArrayList<>();
        for (GraphicNode node: inputs) {
            if (!node.getState()) {
                offNodes.add(node);
            }
        }
        for (GraphicNode node: outputs) {
            if (!node.getState()) {
                offNodes.add(node);
            }
        }
        return offNodes;
    }

    /**
     * Draws the component instance
     * @param context the graphics context where to draw
     * @param scale the scale of the workspace
     */
    public void drawComponent(GraphicsContext context, double scale) {
        double centerX = getCenterX();
        double centerY = getCenterY();
        growthAnimation.animate();
        setCenter(centerX, centerY);
        updateNodesPosition();

        moveAnimation.animate();
        if (isMoving) {
            //System.out.println("moving");
            animateMoveTo();
        }

        // Determines the color of the component
        colorAnimation.animate();
        context.setFill(getActualColor());

        // Draws the component
        context.fillRoundRect(
                getOriginX() * scale, getOriginY() * scale,
                getWidth() * scale, getHeight() * scale,
                Config.WSComponentRoundSize * scale, Config.WSComponentRoundSize * scale
        );
        // Draws the name of the component
        context.setFill(Config.WSTextColor);
        context.fillText(instanceOf.name, getCenterX() * scale, getCenterY() * scale);

        // Draws the nodes
        for (GraphicNode node: inputs) {
            node.drawNode(context, scale);
        }
        for (GraphicNode node: outputs) {
            node.drawNode(context, scale);
        }
    }

    /**
     * Checks if the wires connected to this component instance are long enough
     * @return true if the wires are long enough, false otherwise
     */
    public boolean areWiresFacing() {
        return areInputWiresLongEnough() && areOutputWiresLongEnough();
    }

    /**
     * Checks if the input wires connected to this component instance are long enough
     * @return true if the input wires are long enough, false otherwise
     */
    private boolean areInputWiresLongEnough() {
        //On regarde chaque input
        for (InputNode node: inputs) {
            //Si l'input est relié à un fil, mais que ce fil n'est pas assez long
            if (node.wireConnected.isReal && !node.wireConnected.isWidthLarge()) {
                return false;
            }
        }
        return true;
    }
    /**
     * Checks if the output wires connected to this component instance are long enough
     * @return true if the output wires are long enough, false otherwise
     */
    private boolean areOutputWiresLongEnough() {
        //On regarde chaque ouput
        for (OutputNode node: outputs) {
            //Pour tous les fils reliés à l'output
            for (int i = 1; i < node.wiresConnected.size(); i++) {
                //Si le fil n'est pas assez long, on renvoit false
                if (!node.wiresConnected.get(i).isWidthLarge()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Checks if the component instance will be placed inside the workspace
     * @param width the width of the workspace
     * @param height the height of the workspace
     * @return true if the component instance will be placed inside the workspace, false otherwise
     */
    public boolean isOnSheet(double width, double height) {
        return getOriginX() > Config.WSDistBtwCompo + 0.5 && getOriginY() > Config.WSDistBtwCompo &&
                getOriginX() + this.getWidth() < width - Config.WSDistBtwCompo - 0.5 &&
                getOriginY() + this.getHeight() < height - Config.WSDistBtwCompo;
    }

    /**
     * Sets the component selection state
     * @param selected the new selection state
     */
    public void selectComponent(boolean selected) {
        growthAnimation.setState(selected ? AnimationState.p2 : AnimationState.p4);
        this.isSelected = selected;
    }

    /**
     * Gets the component selection state
     * @return true if the component is selected, false otherwise
     */
    public boolean isSelected() {
        return isSelected;
    }

    private void updateNodesPosition() {
        double space = Config.WSNodeSpace; //* (animation.getSize() + 1); //* (Config.WSCompoSelectedSize * animation.getSize() / 2 + 1);

        int i = 0;
        for (GraphicNode node: inputs) {
            node.selectNode(isSelected);
            node.setCenter(0, space * i + (getHeight() - space * (inputs.length - 1)) / 2d);
            i++;
        }
        i = 0;
        for (GraphicNode node: outputs) {
            node.selectNode(isSelected);
            node.setCenter(0, space * i + (getHeight() - space * (outputs.length - 1)) / 2d);
            i++;
        }
    }

    private boolean isMoving = false;

    private double startX;
    private double startY;

    private double goToX;
    private double goToY;

    public void startAnimateMoveTo(double x, double y) {
        isMoving = true;
        moveAnimation.setState(AnimationState.p2);
        startX = getCenterX();
        startY = getCenterY();
        goToX = x;
        goToY = y;
    }

    void animateMoveTo() {
        if (moveAnimation.state == AnimationState.p2) {
            setCenterX((goToX - startX) * moveAnimation.getSize() + startX);
            setCenterY((goToY - startY) * moveAnimation.getSize() + startY);
        }
        else {
            setCenterX(goToX);
            setCenterY(goToY);
            isMoving = false;
            isPlaced = true;
            setPlaced(true);
        }
    }

    /**
     * The actual color of the component instance
     * @return the actual color of the component instance
     */
    private Color getActualColor() {
        return Config.lerpColor(instanceOf.color, Config.WSDisabledColor, colorAnimation.getSize());
    }

    public boolean isComplete() {
        for (InputNode node: inputs) {
            if (!node.hasWire()) return false;
        }
        for (OutputNode node: outputs) {
            if (!node.hasWire()) return false;
        }
        return true;
    }
}
