package com.prime.util.cardio;

import javax.smartcardio.CardTerminal;
import java.util.List;

public class Test {
    public static void main(String[] args) throws Exception{
        CardIO cardio = CardIO.getInstance();
        cardio.addListener(new CardIO.ICardListener() {
            @Override
            public void onCardInserted(CardTerminal cardTerminal) {
                System.out.println(cardTerminal + "  - a card inserted");
            }

            @Override
            public void onCardEjected(CardTerminal cardTerminal) {
                System.out.println(cardTerminal + "  - a card ejected");
            }

            @Override
            public void onDeviceDetected(List<CardTerminal> cardTerminals) {
                cardTerminals.forEach(l-> System.out.println("device " + l + " connected"));
            }

            @Override
            public void onDeviceDetached(CardTerminal cardTerminal) {
                System.out.println("device "+ cardTerminal + "  - detached");
            }
        });
    }
}
