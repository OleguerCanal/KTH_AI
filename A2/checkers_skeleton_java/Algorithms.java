import java.util.*;

public class Algorithms {
    public Hashtable<String, Double> max_states = new Hashtable<String, Double>();
    public Hashtable<String, Double> min_states = new Hashtable<String, Double>();

    private final int WHITE_KING = Constants.CELL_WHITE | Constants.CELL_KING;
    private final int RED_KING = Constants.CELL_RED | Constants.CELL_KING;

    /* Constants for evaluation function */
    private final int PIECE_VALUE = 1;
    private final int KING_VALUE = 3;

    /* Store player */
    public int max_player;
    public int min_player;
    
    public Integer iterativeDeepening(int max_depth, GameState gamestate, Deadline deadline, long time_limit) {
        Pair<Integer, Double> action = null;
        for (int depth = 1; depth < max_depth; depth++) {
            action = alphabeta(gamestate, depth, -Double.MAX_VALUE, Double.MAX_VALUE, max_player);
            if (deadline.timeUntil() < time_limit) {
                return action.first;
            }
        }
        return action.first;
    }

    public Pair<Integer, Double> alphabeta(GameState gameState, int depth, double alpha, double beta, int player) {

        // Early return conditions
        if (gameState.isEOG()) {
            if (max_player == Constants.CELL_RED){ // MAX is Red player
                if (gameState.isRedWin()) {
                    max_states.put(gameState.toMessage(), Double.MAX_VALUE/2);
                    return new Pair<Integer, Double>(0, Double.MAX_VALUE/2);
                }
                if (gameState.isWhiteWin()) {
                    max_states.put(gameState.toMessage(), -Double.MAX_VALUE/2);
                    return new Pair<Integer, Double>(0, -Double.MAX_VALUE/2);
                }
            } else { // MIN is Red player
                if (gameState.isRedWin()) {
                    min_states.put(gameState.toMessage(), -Double.MAX_VALUE/2);
                    return new Pair<Integer, Double>(0, -Double.MAX_VALUE/2);
                }
                if (gameState.isWhiteWin()) {
                    min_states.put(gameState.toMessage(), Double.MAX_VALUE/2);
                    return new Pair<Integer, Double>(0, Double.MAX_VALUE/2);
                }
            }
            max_states.put(gameState.toMessage(), 0.);
            min_states.put(gameState.toMessage(), 0.);
            return new Pair<Integer, Double>(0, 0.); // Else is draw
        }

        if (depth == 0)
            return new Pair<Integer, Double>(0, evaluate(gameState));
    

        // Otherwise Explore other possible moves
        Vector<GameState> nextStates = new Vector<GameState>();
        gameState.findPossibleMoves(nextStates);

        // If tree values have been hashed, sort them
        int n_childs = nextStates.size();
        Vector<SortableGameState> states = new Vector<SortableGameState>();
        if (depth > 2) {
            GameState nextState = null;
            String code = "";
            double score;
            double max_score = -Double.MAX_VALUE;
            int max_pos = 0;
            for (int i = 0; i < n_childs; i++) {
                nextState = nextStates.elementAt(i);
                code = nextState.toMessage();
                score = -Double.MAX_VALUE/3;
                if (player == max_player) // Means your children are min
                    if (max_states.containsKey(code)) {
                        score = max_states.get(code);
                    }
                if (player == min_player)  // Means your children are max
                    score = Double.MAX_VALUE/3;
                    if (min_states.containsKey(code))
                        score = min_states.get(code);

                if (score > max_score) {  // Get best score (to put at beginning)
                    max_score = score;
                    max_pos = i;
                }
                states.add(new SortableGameState(nextState, score, i));
            }
            // Swap it for element at 0
            Collections.swap(states, 0, max_pos);
            // Collections.sort(states);
        }
        else {  // Do not sort (could only use heuristics)
            GameState nextState;
            for (int i = 0; i < n_childs; i++) {
                nextState = nextStates.elementAt(i);
                states.add(new SortableGameState(nextState, 0, i));
            }
        }

        // Develop Tree
        if (player == max_player) {  // MAX player
            int best_child_pos = 0;
            double v = -Double.MAX_VALUE;
            SortableGameState best_child = null;
            SortableGameState state = null;
            Pair<Integer, Double> state_info = null;

            for (int i = 0; i < n_childs; i++) {
                state = states.elementAt(i);
                state_info = alphabeta(state.gameState, depth - 1, alpha, beta, min_player);
                if (state_info.second > v) {
                    v = state_info.second;
                    best_child_pos = state.pos; // Assign pos previous to sorting
                    best_child = state;
                }
                alpha = Math.max(alpha, v);
                if (beta <= alpha)
                    break;
            }
            // max_states.put(gameState.toMessage(), v);
            max_states.put(best_child.gameState.toMessage(), v);
            return new Pair<Integer, Double>(best_child_pos, v); // move, val
        } else {  // MIN player
            
            int best_child_pos = 0;
            double v = Double.MAX_VALUE;
            SortableGameState best_child = null;
            SortableGameState state = null;
            Pair<Integer, Double> state_info = null;

            for (int i = n_childs - 1; i > - 1; i--) {
            // for (int i = 0; i < n_childs; i++) {
                state = states.elementAt(i);
                state_info = alphabeta(state.gameState, depth - 1, alpha, beta, max_player);
                if (state_info.second < v) {
                    v = state_info.second;
                    best_child_pos = state.pos;
                    best_child = state;
                }
                beta = Math.min(beta, v);
                if (beta <= alpha)
                    break;
            }
            // min_states.put(gameState.toMessage(), v);
            min_states.put(best_child.gameState.toMessage(), v);
            return new Pair<Integer, Double>(best_child_pos, v); // move, val
        }
    }

    public double evaluate(GameState gamestate) {
        double number_of_whites = 0;
        double number_of_reds = 0;

        double white_kings = 0;
        double red_kings = 0;

        double aux = 0;
        for (int i = 0; i < GameState.NUMBER_OF_SQUARES; i++) {
            aux = gamestate.get(i);
            if (aux == Constants.CELL_EMPTY){
                continue;
            }
            else if (aux == Constants.CELL_WHITE){
                number_of_whites++;
                continue;
            }
            else if (aux == WHITE_KING){
                white_kings++;
                continue;
            }
            else if (aux == Constants.CELL_RED){
                number_of_reds++;
                continue;
            }
            else if (aux == RED_KING){
                red_kings++;
                continue;
            }
        }

        double whites = (number_of_whites * PIECE_VALUE + white_kings * KING_VALUE);
        double reds = number_of_reds * PIECE_VALUE + red_kings * KING_VALUE;

        if (max_player == Constants.CELL_RED){
            return (reds - whites);///(reds + whites);
        }            

        return (whites - reds);///(reds + whites);
    }
}