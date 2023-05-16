package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

class DealerTest {
    Dealer dealer;

    @Mock
    Player[] players;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Logger logger;
    @Mock
    private Table table;

    @Mock
    Player player;


    private Integer[] slotToCard;
    private Integer[] cardToSlot;

    void assertInvariants() {
        assertTrue(dealer.getFrozenPlayers().size() <= dealer.getPlayers().length);
    }

    @BeforeEach
    void setUp() {
        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        TableTest.MockLogger logger = new TableTest.MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];

        Env env = new Env(logger, config, new TableTest.MockUserInterface(), new TableTest.MockUtil());
        table = new Table(env, slotToCard, cardToSlot);

        Player player1 = new Player(env, dealer, table, 0, false);
        Player player2 = new Player(env, dealer, table, 1, false);
        players = new Player[2];
        players[0] = player1;
        players[1] = player2;
        fillAllSlots();

        table = new Table(env, slotToCard, cardToSlot);
        dealer = new Dealer(env,table,players);
    }


    private int fillSomeSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
            cardToSlot[i] = i;
        }
    }

    @Test
    void terminate() {
        dealer.terminate();
        assertTrue(dealer.isTerminate());
    }

    @Test
    void removeAllCardsFromTable() {
       dealer.restTimeTrueTest();
       assertTrue(dealer.isNewRound());
    }
}
