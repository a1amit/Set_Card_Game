package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * the list of the slots the player placed his tokens on
     */
    public volatile ArrayList<Integer> selectedSlots;

    /**
     * the number of tokens the player placed
     */
    private AtomicInteger tokensPlaced;

    /**
     * ACTIVE =
     * PENALIZED =
     */
    public enum State {
        ACTIVE,
        WAITING,
        FROZEN
    }

    /**
     *
     */
    private final int ADD_TOKEN = 1;


    /**
     * represents the state of the player
     */
    private State state;

    /**
     * the freezeTime in case of penalty or point
     */
    private volatile long freeze;

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.selectedSlots = new ArrayList<>();
        this.tokensPlaced = new AtomicInteger(0);
        state = State.ACTIVE;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
        }
        if (!human) try {
            aiThread.join();
        } catch (InterruptedException ignored) {
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    Thread.sleep(env.config.tableDelayMillis);

                    //  smart AI for testing
//                    List<Integer> deck = Arrays.stream(table.slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
//                    List<int[]> set = env.util.findSets(deck,0);
//                    for(int[] setfound : set){
//                        for(int i = 0; i < setfound.length;i++){
//                            try {
//                                keyPressed(table.cardToSlot[setfound[i]]);
//                            } catch (NullPointerException ignored){}
//                        }
//                    }
//                    // // the real dumb AI
                    Random random = new Random();
                    int slot = random.nextInt(env.config.tableSize);
                    keyPressed(slot);
                } catch (InterruptedException ignored) {
                }
            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        if (table.canPlayerModifyTable) {
            boolean exists = false;
            for (Integer num : selectedSlots) { // checks whether the token already exists
                if (num == slot) {
                    exists = true;
                }
            }
            if (tokensPlaced.get() < 3 && state == State.ACTIVE) { // if
                env.logger.info("Player: " + id + " has enough tokens to put");
                if (exists) { // if a token already exists
                    removeToken(slot);
                } else { //player hadn't placed a token
                    addToken(slot);
                }
            } else if (tokensPlaced.get() >= 3 && state == State.ACTIVE) { //players state after a penalty
                if (exists) {
                    removeToken(slot);
                }
            }
        } else {
            env.logger.info("player tried to modify table even though he can't");
        }
    }

    /**
     * removes token from slot
     *
     * @param slot = slot to add token to
     */
    public void removeToken(int slot) {
        Integer slotToObject = slot;
        selectedSlots.remove(slotToObject);
        env.logger.info("player: " + id + " number of tokens before removal" + tokensPlaced.get());
        tokensPlaced.decrementAndGet();
        env.logger.info("player: " + id + " number of tokens after removal" + tokensPlaced.get());
        table.removeToken(id, slot);
    }

    /**
     * adds token to table if possible
     *
     * @param slot = slot to remove token from
     */
    public void addToken(int slot) {
        selectedSlots.add(slot);
        env.logger.info("player: " + id + " number of tokens before addition: " + tokensPlaced.get());
        tokensPlaced.addAndGet(ADD_TOKEN); // adds one token
        env.logger.info("player: " + id + " number of tokens after addition: " + tokensPlaced.get());
        table.placeToken(id, slot);
        if (tokensPlaced.get() == env.config.featureSize) { // if a player placed 3 tokens, he changes is state to WAITING
            state = State.WAITING;
            table.playersToCheck.add(new Integer(id));
            env.logger.info("player placed 3 tokens state changed to : " + state);
            env.logger.info("player : " + id + " added to table playersToCheck Queue"
                    + " num of players to check : " + table.playersToCheck.size());
        }
    }


    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement

        //set it's state back to active
        //clear players tokens
        score++;
        freeze = System.currentTimeMillis() + env.config.pointFreezeMillis;
        env.ui.setFreeze(id, env.config.penaltyFreezeMillis);
        env.ui.setScore(id, score);
        while (System.currentTimeMillis() < freeze) {
            env.ui.setFreeze(id, freeze - System.currentTimeMillis());
            try {
                Thread.sleep(env.config.tableDelayMillis);
            } catch (InterruptedException ignored) {
            }

        }
        clearPlayerToken();
        env.logger.info("player tokens were cleared after freeze");
        env.ui.setFreeze(id, env.config.pointFreezeMillis * - ADD_TOKEN );

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        // TODO implement
        state = State.FROZEN;
        env.logger.info("player : " + id + " got a penalty" + " set his state to: " + state);

    }

    public void clearPlayerToken() {
        selectedSlots.clear();
        env.logger.info("player: " + id + " pickedSlots in Player class were cleared");
        tokensPlaced = new AtomicInteger(0);
        env.logger.info("player: " + id + " number of tokens were rest to " + tokensPlaced.get());
        state = State.ACTIVE;
        env.logger.info("player: " + id + " State after clearPlayerToken " + state);
    }

    public int score() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setState(State state) {
        this.state = state;
    }

    public AtomicInteger getTokensPlaced() {
        return tokensPlaced;
    }

    public State getState() {
        return state;
    }
}
