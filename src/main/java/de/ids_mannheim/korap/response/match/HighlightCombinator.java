package de.ids_mannheim.korap.response.match;

import de.ids_mannheim.korap.response.Match;
import de.ids_mannheim.korap.response.match.HighlightCombinatorElement;
import java.util.*;
import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
  Public class for combining highlighting elements
*/
public class HighlightCombinator {

    // Logger (use the Match class)
    private final static Logger log = LoggerFactory.getLogger(Match.class);

    // This advices the java compiler to ignore all loggings
    public static final boolean DEBUG = false;

    private LinkedList<HighlightCombinatorElement> combine;
    private Stack<Integer> balanceStack = new Stack<>();
    private Stack<Integer> tempStack = new Stack<>();


    // Empty constructor
    public HighlightCombinator () {
        this.combine = new LinkedList<>();
    };


    // Return the combination list
    public LinkedList<HighlightCombinatorElement> list () {
        return this.combine;
    };


    // get the first element (without removing)
    public HighlightCombinatorElement getFirst () {
        return this.combine.getFirst();
    };


    // get the last element (without removing)
    public HighlightCombinatorElement getLast () {
        return this.combine.getLast();
    };


    // get an element by index (without removing)
    public HighlightCombinatorElement get (int index) {
        return this.combine.get(index);
    };


    // Get the size of the combinator stack
    public short size () {
        return (short) this.combine.size();
    };


    // Add primary data to the stack
    public void addString (String characters) {
        this.combine.add(new HighlightCombinatorElement(characters));
		if (DEBUG) {
			log.trace("Add string \"{}\" to stack", characters);
		};
    };


    // Add opening highlight combinator to the stack
    public void addOpen (int number) {
        this.combine.add(new HighlightCombinatorElement((byte) 1, number));
        this.balanceStack.push(number);
		if (DEBUG)
			log.trace("Add opening {} to stack", number);
    };

	// Add empty highlight to the stack
    public void addEmpty (int pagenumber) {
        this.combine.add(new HighlightCombinatorElement((byte) 3, pagenumber));
    };

    // Add closing highlight combinator to the stack
    public void addClose (int number) {
        HighlightCombinatorElement lastComb;

        // Clean up temporary stack
        this.tempStack.clear();

        // Check if there is an opening tag at least
        if (this.balanceStack.empty()) {
            if (DEBUG)
                log.trace("The balance stack is empty");
            return;
        };

        // Just some debug information
        if (DEBUG) {
            StringBuilder sb = new StringBuilder(
                    "Stack for checking with class ");
            sb.append(number).append(" is ");
            for (int s : this.balanceStack) {
                sb.append('[').append(s).append(']');
            };
            log.trace(sb.toString());
        };

        // class number of the last element
        // It's already ensured the stack is not empty
        int eold = this.balanceStack.pop();

        // the closing element is not balanced, i.e. the last element differs
        while (eold != number) {

            // Retrieve last combinator on stack
            lastComb = this.combine.peekLast();

            if (DEBUG)
                log.trace(
                        "Closing element is unbalanced - {} "
                                + "!= {} with lastComb {}|{}|{}",
                        eold,
						number,
						lastComb.type,
						lastComb.number,
                        lastComb.characters);

            // combinator is opening and the number is not equal to the last
            // element on the balanceStack
            if (lastComb.type == 1 && lastComb.number == eold) {

                // Remove the last element - it's empty and uninteresting!
                this.combine.removeLast();
            }

			// Last element is empty
			else if (lastComb.type == 3) {
				log.error(
					"The last element was empty -- and this is not correctly handled yet"
					);
			}

            // combinator is either closing (??) or another opener
            else {

                if (DEBUG)
                    log.trace("close element a) {}", eold);

                // Add a closer for the old element (this has following elements)
                this.combine.add(
                        new HighlightCombinatorElement((byte) 2, eold, false));
            };

            // add this element number temporarily on the stack
            tempStack.push(eold);

            // Check next element
            eold = this.balanceStack.pop();
        };

        // Get last combinator on the stack
        lastComb = this.combine.peekLast();

        if (DEBUG) {
            log.trace("Stack for checking 2: {}|{}|{}|{}", lastComb.type,
                    lastComb.number, lastComb.characters, number);
        };

        if (lastComb.type == 1 && lastComb.number == number) {
            while (lastComb.type == 1 && lastComb.number == number) {
                // Remove the damn thing - It's empty and uninteresting!
                this.combine.removeLast();
                lastComb = this.combine.peekLast();
            };
        }
		else if (lastComb.type == 3) {
			System.err.println("öööööööööööööööööööööööö");
		}
        else {
            if (DEBUG)
                log.trace("close element b) {}", number);

            // Add a closer
            this.combine.add(new HighlightCombinatorElement((byte) 2, number));
        };

        // Fetch everything from the tempstack and reopen it
        for (int e : tempStack) {
            if (DEBUG)
                log.trace("Reopen element {}", e);
            combine.add(new HighlightCombinatorElement((byte) 1, e));
            balanceStack.push(e);
        };
    };


    // Get all combined elements as a string
    public String toString () {
        StringBuilder sb = new StringBuilder();
        for (HighlightCombinatorElement e : combine) {
            sb.append(e.toString()).append("\n");
        };
        return sb.toString();
    };
};
