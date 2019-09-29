package com.prime.util.cardio;

import javax.smartcardio.*;
import java.util.Arrays;
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
                try{run(cardTerminals.get(0));}catch (Exception e){e.printStackTrace();}
            }

            @Override
            public void onDeviceDetached(CardTerminal cardTerminal) {
                System.out.println("device "+ cardTerminal + "  - detached");
            }
        });


    }

    private static void run(CardTerminal myTerminal) throws Exception{
        Card card = myTerminal.connect("*"); // could be "T=0" or "T=1", "*" means any protocol
        CardChannel channel = card.getBasicChannel();

        byte[] verify_command = new byte[]{(byte)0xFF, (byte)0x20, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0xFF, (byte)0xFF };
        ResponseAPDU responseAPDU = channel.transmit(new CommandAPDU(verify_command));
        System.out.printf("response: %d, %d\n", responseAPDU.getSW1(), responseAPDU.getSW2());


        byte[] write_command = new byte[]{(byte)0xFF, (byte)0xD0, (byte)0x01, (byte)0x04, (byte)0x0A};
        byte[] commandData = "I am Prime".getBytes();
        byte[] t = new byte[write_command.length + commandData.length];
        for (int i = 0; i < t.length; i++)
            if(i<write_command.length) t[i] = write_command[i];
            else t[i] = commandData[i-write_command.length];
        ResponseAPDU resp = channel.transmit(new CommandAPDU(t));
        System.out.printf("writing: %s = %s\n", new String(commandData), Arrays.toString(commandData));
        System.out.printf("response: %d, %d\n", resp.getSW1(), resp.getSW2());

        if(isNewCard(channel)) System.out.println("new card");
        else System.out.println("not new");


        byte[] read_command = new byte[]{(byte)0xFF, (byte)0xB0, (byte)0x01, (byte)0x04, (byte)0x0A};
        ResponseAPDU resp2 = channel.transmit(new CommandAPDU(read_command));
        System.out.printf("read response: %s, %s\n", resp2.getSW1(), resp2.getSW2());
        System.out.printf("read: %s = %s \n", Arrays.toString(resp2.getData()), new String(resp2.getBytes()));


        System.out.println("equal = " + new String(commandData).equals(new String(resp2.getData())));
        System.out.println(new String(commandData));

        card.disconnect(false);
    }

    private static boolean isNewCard(CardChannel channel) throws CardException{
        byte[] read_command = new byte[]{(byte)0xFF, (byte)0xB0, (byte)0x01, (byte)0x04, (byte)0x0A};
        ResponseAPDU response = channel.transmit(new CommandAPDU(read_command));
        if(new String(response.getData()).equals("I am Prime")) return false;
        else return true;
    }
}
