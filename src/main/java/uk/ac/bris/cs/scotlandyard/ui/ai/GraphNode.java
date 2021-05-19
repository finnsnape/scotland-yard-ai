package uk.ac.bris.cs.scotlandyard.ui.ai;

import uk.ac.bris.cs.scotlandyard.model.Board;
import uk.ac.bris.cs.scotlandyard.model.Move;

public final class GraphNode {
    private final Board.GameState gameState;
    private final Move moveMade;

    GraphNode(Board.GameState gameState, Move moveMade) {
        this.gameState = gameState;
        this.moveMade = moveMade;
    }

    public Board.GameState gameState() {
        return gameState;
    }

    public Move moveMade() {
        return moveMade;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof GraphNode) {
            GraphNode that = (GraphNode) other;
            return this.gameState.equals(that.gameState) && this.moveMade.equals(that.moveMade);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return gameState.hashCode() + moveMade.hashCode();
    }

    @Override
    public String toString() {
        return gameState.toString() + moveMade.toString();
    }
}