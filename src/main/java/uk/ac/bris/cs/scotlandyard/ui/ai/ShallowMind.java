package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class ShallowMind implements Ai {

	@Nonnull @Override public String name() { return "ShallowMind"; }

	@Nonnull @Override public Move pickMove(@Nonnull Board board, Pair<Long, TimeUnit> timeoutPair) {
		MutableValueGraph stateGraph = generateGraph(board);
		Move bestMove = pickBestMove(stateGraph, board);
		return bestMove;
	}

	public Move pickBestMove(MutableValueGraph stateGraph, Board board) {
		/*
		idea is to go through the graph and find the path that results in the highest overall total score (sum of the edges)
		we can then retrieve the move we need to make with node.moveMade

		TODO: currently only takes the first node, but it needs to work recursively and then return the move that leads us to the best outcome
		 */
		GraphNode topNode = new GraphNode((Board.GameState) board, null);
		Set<GraphNode> nextNodes = stateGraph.successors(topNode);
		Move bestMove = null;
		int bestScore = -100;
		for (GraphNode node : nextNodes) { // find the node which has the highest score
			Integer nodeScore = (Integer) stateGraph.edgeValue(topNode, node).get();
			if (nodeScore > bestScore) {
				bestMove = node.moveMade();
				bestScore = nodeScore;
			}
		}

		return bestMove;
	}

	public int scoreState(Board.GameState gameState, Move moveMade) {
		/*
		TODO: use a less naive approach
		 */
		int mrXSourceLocation = moveMade.source(); //mrx starting location
		boolean isDetectiveNearby = false; // Save doublemove when there's no detective nearby at the original place

		int newMrXLocation;
		int score = 0;

		if (moveMade instanceof Move.SingleMove) { // get destination if single move made
			newMrXLocation = ((Move.SingleMove) moveMade).destination;
		}
		else { // get destination if double move made
			newMrXLocation = ((Move.DoubleMove) moveMade).destination2;
		}

		Set <Integer> adjacentNodes = new HashSet<>();
		adjacentNodes.addAll(gameState.getSetup().graph.adjacentNodes(newMrXLocation)); // adjacentNodes after move


		ImmutableSet<Piece> players = gameState.getPlayers();
		HashMap<Piece, Optional<Integer>> detectiveLocations = new HashMap<>();
		Optional<Integer> detectiveLocation;
		int DistanceFromDetectives = 0;

		for (Piece player : players) {
			if (!player.isDetective()) continue; // exclude mrX
			detectiveLocation = gameState.getDetectiveLocation((Piece.Detective) player); // get location of detective
			if (detectiveLocation.isEmpty()) { // check to be sure we have a location, this shouldn't happen
				throw new IllegalArgumentException("No detective location found.");
			}
			if(gameState.getSetup().graph.adjacentNodes(detectiveLocation.get()).contains(mrXSourceLocation)){
				isDetectiveNearby = true; // check if there's detective nearby before moving
			}
			if(gameState.getSetup().graph.adjacentNodes(detectiveLocation.get()).contains(newMrXLocation)){
				score = score - 1; // check how many detectives are close to mrX after the move
			}

			detectiveLocations.put(player, detectiveLocation); // add location of this detective
			DistanceFromDetectives += Math.abs(newMrXLocation - detectiveLocation.get()); // get absolute value of naive distance from mrX (after move made) to this detective
		}
		int averageDistanceFromDetectives = DistanceFromDetectives / (gameState.getPlayers().size() - 1);// divide by number of detectives
		//choose a move that has more freedom
		averageDistanceFromDetectives += (gameState.getSetup().graph.adjacentNodes(newMrXLocation).size() * 10);



		if(score < 0) return score; // this move would be negative if there's detective nearby after move
		else if ((moveMade instanceof Move.DoubleMove) && !isDetectiveNearby) return 0; // Save doublemove when there's no detective nearby
		else return averageDistanceFromDetectives;

	}

	public MutableValueGraph generateGraph(Board board) {
		/*
		this should work (recursively?) for a given number of layers
		e.g. evaluate MrX's possible moves and then all possible detective moves as a result, then all possible detective moves again
		 */
		Board.GameState gameState = (Board.GameState) board;
		Board.GameState gameStateTemp;
		MutableValueGraph<GraphNode, Integer> stateGraph = ValueGraphBuilder.directed().build();
		ImmutableList<Move> moves = gameState.getAvailableMoves().asList();

		GraphNode topNode = new GraphNode(gameState, null);
		GraphNode tempNode;
		int score;
		for (Move move : moves) {
			score = scoreState(gameState, move); // score the move we want to make
			gameStateTemp = gameState.advance(move); // establish the new gamestate after move has been made
			tempNode = new GraphNode(gameStateTemp, move); // add new gamestate and movemade to node
			stateGraph.putEdgeValue(topNode, tempNode, score); // add node and score to the graph
		}
		return stateGraph;

	}
}
