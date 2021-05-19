package uk.ac.bris.cs.scotlandyard.ui.ai;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;


import com.google.common.collect.ImmutableList;
import io.atlassian.fugue.Pair;
import uk.ac.bris.cs.scotlandyard.model.*;

public class AquaAI implements Ai {

	@Nonnull @Override public String name() { return "AquaAI"; }

	@Nonnull @Override public Move pickMove(
			@Nonnull Board board,
			Pair<Long, TimeUnit> timeoutPair) {
		var moves = board.getAvailableMoves().asList();
		Move bestMove = pickBestMove(board, moves);
		System.out.println(bestMove);
		return bestMove;
	}

	public Move pickBestMove(Board board, ImmutableList<Move> moves) {
		Move bestMove = null;
		int bestScore = -100;
		for (Move moveMade : moves) { // get the move with the best score (higher = better)
			int score = scoreBoard(board, moveMade);
			if (score > bestScore) { // check if better than best
				bestScore = score;
				bestMove = moveMade;
			}
		}

		return bestMove;
	}

	public int scoreBoard(Board board, Move moveMade) {
		int mrXSourceLocation = moveMade.source();
		int newMrXLocation;
		if (moveMade instanceof Move.SingleMove) { // get destination if single move made
			newMrXLocation = ((Move.SingleMove) moveMade).destination;
		}
		else { // get destination if double move made
			newMrXLocation = ((Move.DoubleMove) moveMade).destination2;
		}

		boolean isDetectiveNearby = false; // Save doublemove when there's no detective nearby at the original place

		Set <Integer> adjacentNodes = new HashSet<>();
		adjacentNodes.addAll(board.getSetup().graph.adjacentNodes(newMrXLocation)); // adjacentNodes after move
		int score = 0;

		var players = board.getPlayers();
		HashMap<Piece, Optional<Integer>> detectiveLocations = new HashMap<>();
		Optional<Integer> detectiveLocation;
		int averageDistanceFromDetectives = 0;



		for (Piece player : players) {
			if (!player.isDetective()) continue; // exclude mrX
			detectiveLocation = board.getDetectiveLocation((Piece.Detective) player); // get location of detective
			if (detectiveLocation.isEmpty()) { // check to be sure we have a location, this shouldn't happen
				throw new IllegalArgumentException("No detective location found.");
			}
			if(board.getSetup().graph.adjacentNodes(detectiveLocation.get()).contains(mrXSourceLocation)){
				isDetectiveNearby = true; // check if there's detective nearby
			}
			if(board.getSetup().graph.adjacentNodes(detectiveLocation.get()).contains(newMrXLocation)){
				score = score - 1; // check how many detectives are close to mrX after the move
			}
			detectiveLocations.put(player, detectiveLocation); // add location of this detective
			averageDistanceFromDetectives += Math.abs(newMrXLocation - detectiveLocation.get()); // get absolute value of naive distance from mrX (after move made) to this detective
		}


		System.out.println(isDetectiveNearby);
		System.out.println(averageDistanceFromDetectives / (board.getPlayers().size() - 1));

		if ((moveMade instanceof Move.DoubleMove) && !isDetectiveNearby) return 0; // Save doublemove when there's no detective nearby
		else if(score < 0) return score; // this move would be negative if there's detective nearby after move
		else return averageDistanceFromDetectives / (board.getPlayers().size() - 1); // divide by number of detectives
	}
}
