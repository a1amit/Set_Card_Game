package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * symbolizes if the clock was reset or not
     */
    private volatile boolean newRound;

    /**
     * The list of random numbers used put cards in slots randomly
     */
    private List<Integer> range = IntStream.range(0, 12).boxed().collect(Collectors.toList());

    /**
     * list of frozen players
     */
    private ConcurrentLinkedQueue<FrozenPlayer> frozenPlayers;




    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        newRound = true;
        frozenPlayers = new ConcurrentLinkedQueue<>();
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        for (Player player : players) {
            Thread newPlayer = new Thread(player);
            newPlayer.start();
        }
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(newRound);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(newRound);
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        for (Player player : players) {
            player.terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
        if (!table.playersToCheck.isEmpty()) {
            int playerId = table.playersToCheck.remove();
            Player player = players[playerId];
            int[] cards = new int[env.config.featureSize]; // create an array from the player list
            int index = 0;
            for (Integer slot : player.selectedSlots) {
                try {
                    cards[index] = table.slotToCard[slot];
                    index++;
                } catch (NullPointerException ignored){}
            }
            if (env.util.testSet(cards)) {
                table.canPlayerModifyTable = false; // a set was found,therefore we need to lock the board
                env.logger.info("a set was found. locking board from modifications");
                player.point();
                for (int i = 0; i < cards.length; i++) {// removes the found cards from deck
                    Integer slot = table.cardToSlot[cards[i]];
                    if (slot != null) {
                        try {
                            Thread.sleep(env.config.tableDelayMillis);
                            deck.remove(table.slotToCard[slot]);
                        } catch (InterruptedException ignored) {
                        }

                        env.logger.info("card: " + table.slotToCard[slot] + " was removed from the deck");
                        table.removeCard(slot);
                        env.logger.info("current number of cards remain in deck: " + deck.size());
                        env.logger.info("deck after change: " + deck);
                    }
                }
                newRound = true;
                env.logger.info("player found set, setting newRound = true");
            } else {
                frozenPlayers.add(new FrozenPlayer(playerId, System.currentTimeMillis() + env.config.penaltyFreezeMillis));
                env.ui.setFreeze(playerId, env.config.penaltyFreezeMillis);
                player.penalty();
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        if (!deck.isEmpty() && newRound) {
            updateTimerDisplay(newRound);
            table.canPlayerModifyTable = false; // notifies the players that the table can't be modified
            env.logger.info("players are not allowed to place tokens now");
            Collections.shuffle(deck);
            Collections.shuffle(range);
            for (int i = 0; i < env.config.tableSize; i++) {
                if (table.slotToCard[range.get(i)] == null && i < deck.size()) {
                    table.placeCard(deck.get(i), range.get(i));
                }
            }
            newRound = false; // after cards were placed, it's not a new round anymore
            env.logger.info("newRound = false");
            env.logger.info("cards were placed on table");
        }
        table.canPlayerModifyTable = true; //allow the players to edit the board again
        env.logger.info("finished placing cards, players can place tokens again");
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if (reset) {
            env.logger.info("timer was reset");
            newRound = true; // set the new round to true
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
//            reshuffleTime = System.currentTimeMillis() + 5000; //for testing
            env.ui.setElapsed(env.config.turnTimeoutMillis);
        } else {
            if (reshuffleTime < System.currentTimeMillis()) { // in case we need to start a new round
                return;
            }
            if (!frozenPlayers.isEmpty()) { // handles frozen players
                for (FrozenPlayer frozenPlayer : frozenPlayers) {
                    env.ui.setFreeze(frozenPlayer.getId(), frozenPlayer.getFreezeTime() - System.currentTimeMillis());
                    env.logger.info("Penalty current time: " + System.currentTimeMillis());
                    env.logger.info("Recorded frozen player freeze time: " + frozenPlayer.getFreezeTime());
                    if (System.currentTimeMillis() - frozenPlayer.getFreezeTime() > frozenPlayer.getFreezeTime() - System.currentTimeMillis()) {
                        env.logger.info("player: " + frozenPlayer.getId() + " penalty ended");
                        frozenPlayers.remove(frozenPlayer); // remove the frozen player from list
                        players[frozenPlayer.getId()].setState(Player.State.ACTIVE);
                    }
                }
            }
        }
        try {
            env.ui.setElapsed(reshuffleTime - System.currentTimeMillis());
            Thread.sleep(1);
            env.logger.info("a time tick has occurred");
        } catch (InterruptedException ignored) {
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        // TODO implement
        env.logger.info("wants to remove all cards, thus locking the table from modification");
        table.canPlayerModifyTable = false;
        for (int i = 0; i < env.config.tableSize; i++) {
            table.removeCard(range.get(i));//removes a card from the table in a random order
            env.ui.removeCard(range.get(i));
        }
        for (Player player : players) { // clears all the players picks
            player.clearPlayerToken();
        }
        table.clearAllTokens();
        newRound = true;
        env.logger.info("finished clearing the board and all the players picks, starting a new round");
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        int maxScore = Integer.MIN_VALUE;
        for (Player player : players) {
            if (player.score() > maxScore) {
                maxScore = player.score();
            }
        }
        int count = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                count++;
            }
        }
        int[] winners = new int[count];
        int index = 0;
        for (Player player : players) {
            if (player.score() == maxScore) {
                winners[index] = player.id;
                index++;
            }
        }
        env.ui.announceWinner(winners);
    }


    public Player[] getPlayers() {
        return players;
    }

    public ConcurrentLinkedQueue<FrozenPlayer> getFrozenPlayers() {
        return frozenPlayers;
    }

    public boolean isTerminate() {
        return terminate;
    }

    public void restTimeTrueTest(){
        updateTimerDisplay(true);
    }

    public boolean isNewRound() {
        return newRound;
    }
}
