import java.util.Arrays;

class Player {
    private Species[] species;

    private int timestep;
    private int[] sent_guesses;

    public static final int STATES = 1;
    public static final double START_SHOOTING_TIMESTEP = 60;
    public static final double START_SHOOTING_ROUND = 4;
    public static final double SHOOT_THRESHOLD = 0.60;
    public static final Action cDontShoot = new Action(-1, -1);

    public Player() {
        species = new Species[6];
        for (int i = 0; i < species.length; i++) {
            species[i] = new Species(STATES, 9);
        }
    }

    private int[] getBirdObservations(Bird bird) {
        int[] bird_observations = new int[bird.getSeqLength()];
        for (int i = 0; i < bird.getSeqLength(); i++) {
            bird_observations[i] = bird.getObservation(i);
            if (bird_observations[i] == -1) {
                return Arrays.copyOfRange(bird_observations, 0, i - 1);
            }
        }
        return bird_observations;
    }

    private int mostLikelySpecies(int[] obss) {
        double max = Double.NEGATIVE_INFINITY;
        int max_pos = -1;
        for (int j = 0; j < species.length; j++) {
            double value = species[j].getProb(obss);
            if (value > max) {
                max = value;
                max_pos = j;
            }
        }
        return max_pos;
    }

    public Action shoot(GameState pState, Deadline pDue) {
        timestep += pState.getNumNewTurns(); // Update timestep count

        if (timestep > START_SHOOTING_TIMESTEP && pState.getRound() > START_SHOOTING_ROUND) {
            int num_birds = pState.getNumBirds();
            double max_prob = -1;
            int movement = -1;
            int bird_to_shoot = -1;
            for (int i = 0; i < num_birds; i++) {

                if (pState.getBird(i).isAlive()) {

                    // 1. Identify black stork
                    int[] obss = getBirdObservations(pState.getBird(i));
                    int bird_species = mostLikelySpecies(obss);
                    if (bird_species == Constants.SPECIES_BLACK_STORK) {
                        continue;
                    }

                    // 2. For the rest, compute most likely next movement
                    Pair<Integer, Double> move_info = species[bird_species].nextMovement(obss);
                    if (move_info.second > max_prob) {
                        max_prob = move_info.second;
                        movement = move_info.first;
                        bird_to_shoot = i;
                    }
                }

                // 3. Shoot most certain
                // System.err.print("Shooting: ");
                // System.err.println(max_prob);
                if (max_prob > SHOOT_THRESHOLD) {
                    System.err.print("FIRE! Prob: ");
                    System.err.print(Math.round(max_prob * 100));
                    System.err.print(", Bird:");
                    System.err.println(bird_to_shoot);
                    return new Action(bird_to_shoot, movement);
                }

            }
        }

        return cDontShoot;
    }

    public int[] guess(GameState pState, Deadline pDue) {
        int birds_num = pState.getNumBirds();
        sent_guesses = new int[birds_num];

        for (int i = 0; i < birds_num; i++) {
            Bird bird = pState.getBird(i);
            int[] obss = getBirdObservations(bird);
            sent_guesses[i] = mostLikelySpecies(obss); // Find closest species
        }
        return sent_guesses;
    }

    public void hit(GameState pState, int pBird, Deadline pDue) {
        System.err.println("HIT BIRD!!!");
    }

    private void guessingStatistics(int[] real_vals) {
        int correct = 0;
        int error = 0;
        int unknown = 0;
        for (int i = 0; i < real_vals.length; i++) {
            if (sent_guesses[i] == -1)
                unknown++;
            else if (sent_guesses[i] == real_vals[i])
                correct++;
            else
                error++;
        }
        System.err.print("Sent: ");
        for (int i = 0; i < real_vals.length; i++) {
            System.err.print(sent_guesses[i]);
            System.err.print(" ");
        }
        System.err.println();
        System.err.print("Gott: ");
        for (int i = 0; i < real_vals.length; i++) {
            System.err.print(real_vals[i]);
            System.err.print(" ");
        }
        System.err.println();
        System.err.print("Correct: ");
        System.err.print(correct);
        System.err.print(", Errors: ");
        System.err.print(error);
        System.err.print(", Unknown: ");
        System.err.println(unknown);
    }

    /**
     * If you made any guesses, you will find out the true species of those birds
     * through this function.
     *
     * @param pState   the GameState object with observations etc
     * @param pSpecies the vector with species
     * @param pDue     time before which we must have returned
     */
    public void reveal(GameState pState, int[] pSpecies, Deadline pDue) {
        guessingStatistics(pSpecies);

        // Assign Birds
        int birds_num = pState.getNumBirds();
        for (int i = 0; i < birds_num; i++) {
            Bird bird = pState.getBird(i);
            int[] obss = getBirdObservations(bird);
            int real_species = pSpecies[i];
            species[real_species].appendObs(obss);
        }
        // Update species
        for (int i = 0; i < species.length; i++) {
            species[i].updateModel();
            // System.err.print("---------------SPECIES: ");
            // System.err.println(i);
            // species[i].printSpecies();
        }
        // System.exit(0);
    }

}
