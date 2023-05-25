# Set Game

Welcome to the Set Game! This is a simple implementation of the popular card game "Set" with some modifications. The game involves finding sets of three cards that meet specific criteria. 

## Introduction

To understand the game's objective and rules, you can refer to the original Set Card Game description (link provided in the project description). Keep in mind that this implementation uses slightly different rules, as outlined in the project specifications.
- https://en.wikipedia.org/wiki/Set_(card_game) 

## My Version of the Game

### Short Demonstration Video:



https://github.com/a1amit/Set_Card_Game/assets/66822340/f295d40c-a46c-4c6a-82fc-f2486573b926


### Features and Cards

The game consists of a deck of 81 cards. Each card represents a drawing with four features: color, number, shape, and shading. The values for these features vary within specific options.

### Table and Players

The game starts with 12 cards drawn from the deck, which are placed on a 3x4 grid on the table. The players' goal is to find a combination of three cards on the table that form a "legal set."

A "legal set" is defined by the following criteria: for each of the four features (color, number, shape, and shading), the three cards must display that feature as either all the same or all different.

The players interact with the game by placing tokens on the cards using specific keys on the keyboard. Each player controls 12 unique keys on the keyboard corresponding to the table's card slots. The players can place or remove tokens from the cards using their designated keys.

The game supports two player types: human and non-human. Human players provide input from the physical keyboard, while non-human players are simulated by threads that generate random key presses.

### The Dealer

The dealer is responsible for managing the game flow. It creates and runs player threads, deals cards to the table, shuffles the cards, collects cards from the table, checks if the placed tokens form a legal set, awards points, and penalizes players accordingly. The dealer also tracks the countdown timer and determines if any legal sets can be formed from the remaining cards in the deck.

### Graphic User Interface & Keyboard Input

This Project Has a GUI

## Getting Started

To run the Set Game, follow these steps:

1. Ensure that you have Java installed on your machine.

2. Clone the project repository to your local machine.

3. Open a terminal or command prompt and navigate to the project directory.

4. Compile the source code by running the following command:
   ```
   javac Main.java
   ```

5. Run the game by executing the following command:
   ```
   java Main
   ```

6. Enjoy playing the Set Game!

## Configuration

The game configuration is stored in the `config.properties` file. You can modify this file to adjust various settings, such as the number of features, options per feature, and more. Refer to the comments in the `config.properties` file for more information on each configurable option.

## License

This project is licensed under the [MIT License](LICENSE).

## More Info
For a full understanding of the project, open the instructions folder and read the instructions
