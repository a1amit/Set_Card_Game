package bguspl.set.ex;

import bguspl.set.Env;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    /**
     * a boolean var that tells if a player can perform an action or not
     */
    protected volatile boolean canPlayerModifyTable;

    /**
     * represents the table
     * false - a token is not placed on slot
     * true - a token is placed on that slot
     * it's size is number of players x table size
     */
    protected volatile boolean[][] tokenTable;

    /**
     * a queue that holds all the players that finished placing their tokens and awaiting a check from the dealer
     */
    protected ConcurrentLinkedQueue<Integer> playersToCheck;

    /**
     * for smartAI only for testing
     */
    protected ArrayList<Integer> answers;

    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        this.canPlayerModifyTable = true;
        tokenTable = new boolean[env.config.players][env.config.tableSize];
        playersToCheck = new ConcurrentLinkedQueue<>();
        answers = new ArrayList<>();
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     *
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {
        }

        canPlayerModifyTable = false; // if the dealer puts cards on board, the player is not allowed to put cards on board
        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card, slot);
        env.logger.info("card: " + card + " was placed on slot " + slot + " successfully");
    }

    /**
     * Removes a card from a grid slot on the table.
     *
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {
        }
        // TODO implement
        canPlayerModifyTable = false; // if the dealer removes a card the players are not allowed to modify the table
        Integer card = slotToCard[slot];
        if(card == null){
            return;
        }
        slotToCard[slot] = null;
        cardToSlot[card] = null;
        for (int i = 0; i < tokenTable.length; i++) {
            if (tokenTable[i][slot]) {
                tokenTable[i][slot] = false;
                env.logger.info("player: " + i + " token on slot: " + slot + " was removed.");
                env.ui.removeToken(i, slot);
            }
        }
    }

    /**
     * Places a player token on a grid slot.
     *
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        // TODO implement
        if (canPlayerModifyTable) { // if we can place token
            if (tokenTable[player][slot]) { // if player already has a token on this spot
                removeToken(player, slot);
            } else {
                tokenTable[player][slot] = true;
                env.logger.info("player: " + player + " put a token on slot: " + slot);
                env.logger.info("token was saved at main token table at: " + "[Player][Slot]" + "[" + player + "]" + "[" + slot + "]");
                env.ui.placeToken(player, slot);
            }
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     *
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        // TODO implement
        if (canPlayerModifyTable) {
            if (tokenTable[player][slot]) { // in case there's a token on table
                tokenTable[player][slot] = false;
                env.ui.removeToken(player, slot);
                env.logger.info("player: " + player + " removed a token from slot: " + slot);
                env.logger.info("token was removed from table at: " + "[Player][Slot]" + "[" + player + "]" + "[" + slot + "]");
                return true;
            } else {
                env.logger.info("token already exists, should remove it instead");
                placeToken(player, slot);
            }
        }
        return false;
    }


    /**
     * this function clears all tokens from the table
     */
    public void clearAllTokens() {
        canPlayerModifyTable = false; // first we tell all the players that the board can't be modified
        for (int row = 0; row < tokenTable.length; row++) {
            for (int col = 0; col < tokenTable[row].length; col++) {
                if (tokenTable[row][col]) {
                    tokenTable[row][col] = false;
                    env.ui.removeToken(row, col);
                }
            }
            env.logger.info("all tokens were removed from board successfully");
        }
    }

}
